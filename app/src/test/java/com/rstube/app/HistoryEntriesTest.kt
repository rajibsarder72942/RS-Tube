package com.rstube.app

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class HistoryEntriesTest {
    @Test fun oldHistoryReceivesPersistentUniqueIds() {
        val items = JSONArray("""[{"name":"old.mp4"},{"name":"old.mp4"}]""")
        assertTrue(HistoryEntries.ensureIds(items))
        val id = items.getJSONObject(0).getString("id")
        assertNotEquals(id, items.getJSONObject(1).getString("id"))
        assertFalse(HistoryEntries.ensureIds(items))
        assertEquals(id, items.getJSONObject(0).getString("id"))
        assertEquals("old.mp4", items.getJSONObject(0).getString("name"))
    }

    @Test fun aNewDownloadDoesNotChangeWhichEntryIsDeleted() {
        val history = JSONArray("""[{"id":"new","name":"new.mp4"},{"id":"selected","name":"same.mp4"},{"id":"keep","name":"same.mp4"}]""")
        val kept = HistoryEntries.without(history, "selected")
        assertEquals(2, kept.length())
        assertEquals("new", kept.getJSONObject(0).getString("id"))
        assertEquals("keep", kept.getJSONObject(1).getString("id"))
        assertEquals(3, history.length())
    }

    @Test fun deletingAgainDoesNotRemoveAnotherEntry() {
        val history = JSONArray("""[{"id":"keep","uri":"content://media/external/downloads/7"}]""")
        val kept = HistoryEntries.without(history, "already-removed")
        assertEquals(1, kept.length())
        assertEquals("content://media/external/downloads/7", kept.getJSONObject(0).getString("uri"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankIdsCannotDeleteLegacyRows() {
        HistoryEntries.without(JSONArray("""[{"name":"keep"}]"""), "")
    }
}
