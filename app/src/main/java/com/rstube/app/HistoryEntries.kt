package com.rstube.app

import org.json.JSONArray
import java.util.UUID

/** Stable identity prevents a new download shifting the row selected for deletion. */
object HistoryEntries {
    fun ensureIds(entries: JSONArray): Boolean {
        var changed = false
        val seen = mutableSetOf<String>()
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            var id = entry.optString("id")
            if (id.isBlank() || !seen.add(id)) {
                id = UUID.randomUUID().toString()
                entry.put("id", id)
                seen.add(id)
                changed = true
            }
        }
        return changed
    }

    fun without(entries: JSONArray, id: String): JSONArray {
        require(id.isNotBlank())
        val kept = JSONArray()
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            if (entry.optString("id") != id) kept.put(entry)
        }
        return kept
    }
}
