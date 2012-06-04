package net.bible.service.format.osistohtml;

import java.net.URI;
import java.util.List;

import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.KeyUtil;
import org.crosswire.jsword.passage.Verse;

public class OsisToHtmlParameters {
    private String languageCode = "en";
    private boolean isLeftToRight = true;
    private boolean isShowTitles = true;
    private boolean isShowVerseNumbers = false;
    private boolean isVersePerline = false;
    private boolean isShowMyNotes = false;
    private boolean isShowBookmarks = false;
    private boolean isShowNotes = false;
    private boolean isAutoWrapUnwrappedRefsInNote = false;
    // used as a basis if a reference has only chapter and no book
    private Verse basisRef;
    private String font;
    private String cssClassForCustomFont;

    private boolean isShowStrongs = false;
    private boolean isShowMorphology = false;
    private boolean isRedLetter = false;
    private String extraStylesheet;
    private String extraFooter;
    private boolean convertStrongsRefsToLinks;
    private List<Key> keysWithNotes;
    private List<Key> keysWithBookmarks;
    private URI moduleBasePath;

	public String getLanguageCode() {
		return languageCode;
	}
	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}
	public boolean isLeftToRight() {
		return isLeftToRight;
	}
	public void setLeftToRight(boolean isLeftToRight) {
		this.isLeftToRight = isLeftToRight;
	}
	public boolean isShowTitles() {
		return isShowTitles;
	}
	public void setShowTitles(boolean isShowTitles) {
		this.isShowTitles = isShowTitles;
	}
	public boolean isShowVerseNumbers() {
		return isShowVerseNumbers;
	}
	public void setShowVerseNumbers(boolean isShowVerseNumbers) {
		this.isShowVerseNumbers = isShowVerseNumbers;
	}
	public boolean isVersePerline() {
		return isVersePerline;
	}
	public void setVersePerline(boolean isVersePerline) {
		this.isVersePerline = isVersePerline;
	}
	public boolean isShowMyNotes() {
		return isShowMyNotes;
	}
	public void setShowMyNotes(boolean isShowMyNotes) {
		this.isShowMyNotes = isShowMyNotes;
	}
	public boolean isShowBookmarks() {
		return isShowBookmarks;
	}
	public void setShowBookmarks(boolean isShowBookmarks) {
		this.isShowBookmarks = isShowBookmarks;
	}
	public boolean isShowNotes() {
		return isShowNotes;
	}
	public void setShowNotes(boolean isShowNotes) {
		this.isShowNotes = isShowNotes;
	}
	public boolean isAutoWrapUnwrappedRefsInNote() {
		return isAutoWrapUnwrappedRefsInNote;
	}
	public void setAutoWrapUnwrappedRefsInNote(boolean isAutoWrapUnwrappedRefsInNote) {
		this.isAutoWrapUnwrappedRefsInNote = isAutoWrapUnwrappedRefsInNote;
	}
	public boolean isShowStrongs() {
		return isShowStrongs;
	}
	public void setShowStrongs(boolean isShowStrongs) {
		this.isShowStrongs = isShowStrongs;
	}
	public boolean isShowMorphology() {
		return isShowMorphology;
	}
	public void setShowMorphology(boolean isShowMorphology) {
		this.isShowMorphology = isShowMorphology;
	}
	public String getExtraStylesheet() {
		return extraStylesheet;
	}
	public void setExtraStylesheet(String extraStylesheet) {
		this.extraStylesheet = extraStylesheet;
	}
	public String getExtraFooter() {
		return extraFooter;
	}
	public void setExtraFooter(String extraFooter) {
		this.extraFooter = extraFooter;
	}
	public Verse getBasisRef() {
		return basisRef;
	}
	public void setBasisRef(Key basisRef) {
		// KeyUtil always returns a Verse even if it is only Gen 1:1
		this.basisRef = KeyUtil.getVerse(basisRef);
	}
	public boolean isRedLetter() {
		return isRedLetter;
	}
	public void setRedLetter(boolean isRedLetter) {
		this.isRedLetter = isRedLetter;
	}
	public String getFont() {
		return font;
	}
	public void setFont(String font) {
		this.font = font;
	}
	public String getCssClassForCustomFont() {
		return cssClassForCustomFont;
	}
	public void setCssClassForCustomFont(String cssClassForCustomFont) {
		this.cssClassForCustomFont = cssClassForCustomFont;
	}
	public boolean isConvertStrongsRefsToLinks() {
		return convertStrongsRefsToLinks;
	}
	public void setConvertStrongsRefsToLinks(boolean convertStrongsRefsToLinks) {
		this.convertStrongsRefsToLinks = convertStrongsRefsToLinks;
	}
	public List<Key> getKeysWithNotes() {
		return keysWithNotes;
	}
	public void setKeysWithNotes(List<Key> keysWithNotes) {
		this.keysWithNotes = keysWithNotes;
	}
	public List<Key> getKeysWithBookmarks() {
		return keysWithBookmarks;
	}
	public void setKeysWithBookmarks(List<Key> keysWithBookmarks) {
		this.keysWithBookmarks = keysWithBookmarks;
	}
	public URI getModuleBasePath() {
		return moduleBasePath;
	}
	public void setModuleBasePath(URI moduleBasePath) {
		this.moduleBasePath = moduleBasePath;
	}
}
