/*
 * Copyright (c) 2020-2022 Martin Denham, Tuomas Airaksinen and the AndBible contributors.
 *
 * This file is part of AndBible: Bible Study (http://github.com/AndBible/and-bible).
 *
 * AndBible is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * AndBible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with AndBible.
 * If not, see http://www.gnu.org/licenses/.
 */

package net.bible.android.view.activity.installzip

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import net.bible.android.activity.R

import org.crosswire.common.util.NetUtil
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.sword.SwordBookDriver
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.book.sword.SwordConstants

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bible.android.SharedConstants
import net.bible.android.activity.databinding.ActivityInstallZipBinding
import net.bible.android.control.event.ABEventBus
import net.bible.android.control.event.ToastEvent
import net.bible.android.view.activity.base.ActivityBase
import net.bible.android.view.activity.page.MainBibleActivity
import net.bible.service.sword.mybible.addManuallyInstalledMyBibleBooks
import net.bible.service.sword.mybible.addMyBibleBook
import net.bible.service.sword.mysword.addManuallyInstalledMySwordBooks
import net.bible.service.sword.mysword.addMySwordBook
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

/**
 * Install SWORD module from a zip file
 *
 * @author Tuomas Airaksinen [tuomas.airaksinen at gmail dot com]
 */

class ModulesExists(val files: List<String>) : Exception()
class CantOverwrite(val files: List<String>) : Exception()

class InvalidModule : Exception()

const val TAG = "InstallZip"

class ZipHandler(
        private val newInputStream: () -> InputStream?,
        private val updateProgress: (progress: Int) -> Unit,
        private val finish: (finishResult: Int) -> Unit,
        private val activity: Activity
) {
    private var totalEntries = 0

    @Throws(IOException::class, ModulesExists::class, InvalidModule::class)
    private suspend fun checkZipFile() = withContext(Dispatchers.IO) {
        var modsDirFound = false
        var modulesFound = false
        var entry: ZipEntry?

        val targetDirectory = SwordBookPath.getSwordDownloadDir()
        val zin = ZipInputStream(newInputStream())
        try {
            entry = zin.nextEntry
        } catch (e: IllegalArgumentException) {
            throw InvalidModule()
        }
        val existingFiles = mutableListOf<String>()

        while (entry != null) {
            totalEntries++
            val name = entry.name.replace('\\', '/')
            val targetFile = File(targetDirectory, name)
            if (!entry.isDirectory && targetFile.exists()) {
                existingFiles.add(targetFile.relativeTo(targetDirectory).canonicalPath)
            }
            if (name.startsWith(SwordConstants.DIR_CONF + "/") && name.endsWith(SwordConstants.EXTENSION_CONF))
                modsDirFound = true
            else if (name.startsWith(SwordConstants.DIR_CONF + "/")) {
                // Ignore directory
            } else if (name.startsWith(SwordConstants.DIR_DATA + "/")) {
                modulesFound = true
            } else if (name.startsWith("mysword/") || name.startsWith("mybible/")) {
                modulesFound = true
                modsDirFound = true
            } else {
                zin.close()
                throw InvalidModule()
            }
            entry = zin.nextEntry
        }

        if (!(modsDirFound && modulesFound)) {
            zin.close()
            throw InvalidModule()
        }
        zin.close()
        if(existingFiles.isNotEmpty())
            throw ModulesExists(existingFiles)
    }


    @Throws(IOException::class, BookException::class)
    private suspend fun installZipFile() = withContext(Dispatchers.IO) {
        val confFiles = ArrayList<File>()
        val targetDirectory = SwordBookPath.getSwordDownloadDir()
        val errors: MutableList<String> = mutableListOf()
        ZipInputStream(newInputStream()).use { zIn ->
            var ze: ZipEntry?
            var count: Int
            var entryNum = 0
            val buffer = ByteArray(8192)
            ze = zIn.nextEntry
            while (ze != null) {
                val name = ze.name.replace('\\', '/')

                val file = File(targetDirectory, name)
                if (name.startsWith(SwordConstants.DIR_CONF) && name.endsWith(SwordConstants.EXTENSION_CONF))
                    confFiles.add(file)

                val dir = if (ze.isDirectory) file else file.parentFile

                if (!dir.isDirectory && !(dir.mkdirs() || dir.isDirectory))
                    throw IOException()

                if (ze.isDirectory) {
                    ze = zIn.nextEntry
                    continue
                }
                try {
                    FileOutputStream(file).use { fOut ->
                        count = zIn.read(buffer)
                        while (count != -1) {
                            fOut.write(buffer, 0, count)
                            count = zIn.read(buffer)
                        }
                    }
                } catch (e: IOException) {
                    errors.add(file.name)
                    Log.e(TAG, "Error in writing ${file.name}", e);
                }
                onProgressUpdate(++entryNum)
                ze = zIn.nextEntry
            }
            if(errors.isNotEmpty()) {
                throw CantOverwrite(errors)
            }
        }
        // Load configuration files & register books
        val bookDriver = SwordBookDriver.instance()
        for (confFile in confFiles) {
            val me = SwordBookMetaData(confFile, NetUtil.getURI(targetDirectory))
            me.driver = bookDriver
            SwordBookDriver.registerNewBook(me)
        }
        addManuallyInstalledMyBibleBooks()
        addManuallyInstalledMySwordBooks()
    }

    suspend fun execute() = withContext(Dispatchers.Main) {
        var finishResult = Activity.RESULT_CANCELED
        var doInstall = false

        var result = try {
            checkZipFile()
            doInstall = true
            InstallResult.OK
        } catch (e: IOException) {
            Log.e(TAG, "Error occurred", e)
            InstallResult.ERROR
        } catch (e: InvalidModule) {
            InstallResult.INVALID_MODULE
        } catch (e: ModulesExists) {
            doInstall = suspendCoroutine {
                AlertDialog.Builder(activity)
                    .setTitle(R.string.overwrite_files_title)
                    .setMessage(activity.getString(R.string.overwrite_files, "\n" + e.files.joinToString("\n")))
                    .setPositiveButton(R.string.yes) {_, _ -> it.resume(true)}
                    .setNeutralButton(R.string.cancel) {_, _ -> it.resume(false)}
                    .setOnCancelListener {_ -> it.resume(false)}
                    .show()
            }
            if(doInstall) InstallResult.OK else InstallResult.CANCEL
        }

        if(doInstall) {
            result = try {
                installZipFile()
                finishResult = Activity.RESULT_OK
                InstallResult.OK
            } catch (e: BookException) {
                Log.e(TAG, "Error occurred", e)
                InstallResult.ERROR
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred", e)
                InstallResult.ERROR
            } catch (e: CantOverwrite) {
                suspendCoroutine {
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.error_occurred)
                        .setMessage(
                            activity.getString(
                                R.string.could_not_overwrite_files,
                                "\n" + e.files.joinToString("\n")
                            )
                        )
                        .setPositiveButton(R.string.okay) {_, _ -> it.resume(InstallResult.IGNORE)}
                        .setOnCancelListener {_ -> it.resume(InstallResult.IGNORE)}
                        .show()
                }
            }
        }

        val bus = ABEventBus
        when (result) {
            InstallResult.ERROR -> bus.post(ToastEvent(R.string.error_occurred))
            InstallResult.CANCEL -> bus.post(ToastEvent(R.string.install_zip_canceled))
            InstallResult.INVALID_MODULE -> bus.post(ToastEvent(R.string.invalid_module))
            InstallResult.OK -> bus.post(ToastEvent(R.string.install_zip_successfull))
            InstallResult.IGNORE -> {}
        }
        finish(finishResult)

    }

    private suspend fun onProgressUpdate(value: Int)  = withContext(Dispatchers.Main) {
        val progressNow = (value.toFloat() / totalEntries.toFloat() * 100).roundToInt()
        updateProgress(progressNow/totalEntries)
    }
    
    enum class InstallResult {ERROR, INVALID_MODULE, CANCEL, OK, IGNORE}
}

open class SqliteInstallError: Error()
class CantRead: SqliteInstallError()
class InvalidFile: SqliteInstallError()
class CantWrite: SqliteInstallError()

class InstallZip : ActivityBase() {
    private lateinit var binding: ActivityInstallZipBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Install from Zip starting")
        binding = ActivityInstallZipBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.buildActivityComponent().inject(this)
        lifecycleScope.launch {
            when (intent?.action) {
                Intent.ACTION_VIEW -> installZip(intent!!.data!!)
                Intent.ACTION_SEND -> installZip(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)
                Intent.ACTION_SEND_MULTIPLE -> {
                    for (uri in intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!) {
                        installZip(uri)
                    }
                }
                else -> {
                    getFileFromUserAndInstall()
                }
            }
        }
    }

    private suspend fun getFileFromUserAndInstall() {
        val proceed = suspendCoroutine {
            val zip = getString(R.string.format_zip)
            val myBible = getString(R.string.format_mybible)
            val mySword = getString(R.string.format_mysword)
            val formats = getString(R.string.choose_file) + " \n\n" + getString(R.string.supported_formats, "$zip, $myBible, $mySword")

            AlertDialog.Builder(this@InstallZip)
                .setTitle(R.string.install_zip)
                .setMessage(formats)
                .setPositiveButton(R.string.proceed) { dialog, which ->
                    it.resume(true)
                }
                .setNeutralButton(R.string.cancel){ _, _ ->
                    it.resume(false)
                }
                .setOnCancelListener {_ -> it.resume(false)}
                .show()
        }

        if(!proceed) {
            finish()
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "application/*"

        val result = awaitIntent(intent)
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                installFromFile(result.resultData!!.data!!)
            } catch (e: SqliteInstallError) {
                Log.e(TAG, "Error occurred in installing module", e)
                val msgId = when(e) {
                    is CantRead -> R.string.sqlite_cant_read
                    is InvalidFile -> R.string.sqlite_invalid_file
                    is CantWrite -> R.string.sqlite_cant_write
                    else -> throw RuntimeException(e)
                }
                suspendCoroutine<Boolean> {
                    AlertDialog.Builder(this@InstallZip)
                        .setTitle(R.string.error_occurred)
                        .setMessage(getString(R.string.install_failed_reason, getString(msgId)))
                        .setPositiveButton(R.string.okay) { dialog, which ->
                            it.resume(true)
                        }
                        .setOnCancelListener {_ -> it.resume(false)}
                        .show()
                }
            }
        }
        finish()
    }

    private suspend fun installFromFile(uri: Uri): Boolean {
        val displayName = contentResolver.query(uri, null, null, null, null)?.use {
            it.moveToFirst()
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.getString(idx)
        }?: throw CantRead()

        if (displayName.lowercase().endsWith(".zip"))
            return installZip(uri)

        val filetype = when {
            displayName.lowercase().endsWith(".sqlite3") -> "mybible"
            displayName.lowercase().endsWith(".mybible") -> "mysword"
            else -> throw InvalidFile()
        }

        binding.loadingIndicator.visibility = View.VISIBLE
        contentResolver.openInputStream(uri).use { fIn ->
            fIn ?: throw CantRead()
            val outDir = File(SharedConstants.MODULE_DIR, filetype)
            outDir.mkdirs()
            val outFile = File(outDir, displayName)
            if(outFile.exists()) {
                val doInstall = suspendCoroutine {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.overwrite_files_title)
                        .setMessage(getString(R.string.overwrite_files, "$filetype/$displayName"))
                        .setPositiveButton(R.string.yes) {_, _ -> it.resume(true)}
                        .setNeutralButton(R.string.cancel) {_, _ -> it.resume(false)}
                        .setOnCancelListener {_ -> it.resume(false)}
                        .show()
                }
                if(!doInstall) {
                    ABEventBus.post(ToastEvent(R.string.install_zip_canceled))
                    return false
                }
            }

            if ((outFile.exists() && !outFile.canWrite()) || (!outFile.exists() && !outDir.canWrite())) {
                throw CantWrite()
            }

            withContext(Dispatchers.IO) {
                val header = ByteArray(16)
                fIn.read(header)
                if (String(header) == "SQLite format 3\u0000") {
                    val out = FileOutputStream(outFile)
                    withContext(Dispatchers.IO) {
                        out.write(header)
                        fIn.copyTo(out)
                        out.close()
                    }
                    val book = when(filetype) {
                        "mybible" -> addMyBibleBook(outFile)
                        "mysword" -> addMySwordBook(outFile)
                        else -> throw RuntimeException()
                    }
                    if(book == null) {
                        outFile.delete()
                        throw InvalidFile()
                    }
                }
                else {
                    throw InvalidFile()
                }
            }
        }
        binding.loadingIndicator.visibility = View.GONE
        ABEventBus.post(ToastEvent(R.string.install_zip_successfull))
        ABEventBus.post(MainBibleActivity.UpdateMainBibleActivityDocuments())
        setResult(Activity.RESULT_OK)
        finish()
        return true
    }


    private suspend fun installZip(uri: Uri): Boolean {
        var result = false
        binding.installZipLabel.text = getString(R.string.checking_zip_file)
        val zh = ZipHandler(
                {contentResolver.openInputStream(uri)},
                {percent -> updateProgress(percent)},
                {finishResult ->
                    result = finishResult == Activity.RESULT_OK
                    setResult(finishResult);
                    ABEventBus.post(MainBibleActivity.UpdateMainBibleActivityDocuments())
                    finish()
                },
            this
        )
        binding.loadingIndicator.visibility = View.VISIBLE
        zh.execute()
        binding.loadingIndicator.visibility = View.GONE
        return result
    }

    override fun onBackPressed() {}

    private fun updateProgress(percentValue: Int) {
        if (percentValue == 1)
            binding.installZipLabel.text = getString(R.string.extracting_zip_file)

        binding.progressBar.progress = percentValue
    }
}
