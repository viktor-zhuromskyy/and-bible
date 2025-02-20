/*
 * Copyright (c) 2021-2022 Martin Denham, Tuomas Airaksinen and the AndBible contributors.
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

import {reactive} from "vue";
import {setupEventBusListener} from "@/eventbus";
import {Bookmark, BookmarkToLabel, Label, StudyPadTextItem} from "@/types/client-objects";

export function useJournal(label: Label) {
    const journalTextEntries = reactive(new Map());
    const bookmarkToLabels = reactive(new Map());

    setupEventBusListener("add_or_update_bookmarks", (bookmarks: Bookmark[]) => {
        for (const b of bookmarks) {
            if (b.bookmarkToLabels) {
                updateBookmarkToLabels(...b.bookmarkToLabels);
            }
        }
    });

    function updateJournalTextEntries(...entries: StudyPadTextItem[]) {
        for (const e of entries)
            if (e.labelId === label.id)
                journalTextEntries.set(e.id, e);
    }

    function updateBookmarkToLabels(...entries: BookmarkToLabel[]) {
        for (const e of entries)
            if (e.labelId === label.id)
                bookmarkToLabels.set(e.bookmarkId, e);
    }

    function updateJournalOrdering(...entries: StudyPadTextItem[]) {
        for (const e of entries) {
            journalTextEntries.get(e.id).orderNumber = e.orderNumber;
        }
    }

    function deleteJournal(journalId: number) {
        journalTextEntries.delete(journalId)
    }

    return {
        journalTextEntries,
        updateJournalTextEntries,
        updateJournalOrdering,
        updateBookmarkToLabels,
        bookmarkToLabels,
        deleteJournal
    };
}
