package com.rstube.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DownloadStore {
    private fun prefs(context: Context) = context.getSharedPreferences("downloads", Context.MODE_PRIVATE)
    fun status(context: Context): String = prefs(context).getString("status", "Ready to download")!!
    fun progress(context: Context): Int = prefs(context).getInt("progress", -1)
    fun state(context: Context, status: String, progress: Int = -1) {
        prefs(context).edit().putString("status", status).putInt("progress", progress).apply()
    }
    @Synchronized fun history(context: Context): JSONArray = try {
        JSONArray(prefs(context).getString("history", "[]")).also { entries ->
            if (HistoryEntries.ensureIds(entries)) prefs(context).edit().putString("history", entries.toString()).commit()
        }
    } catch (_: Exception) { JSONArray() }
    @Synchronized fun record(context: Context, name: String, uri: String, audio: Boolean, result: String) {
        val old = history(context)
        val next = JSONArray().put(JSONObject().put("id", UUID.randomUUID().toString()).put("name", name).put("uri", uri)
            .put("audio", audio).put("result", result).put("time", System.currentTimeMillis()))
        for (i in 0 until minOf(old.length(), 49)) next.put(old.getJSONObject(i))
        prefs(context).edit().putString("history", next.toString()).commit()
    }
    @Synchronized fun remove(context: Context, id: String): Boolean {
        val remaining = HistoryEntries.without(history(context), id)
        return prefs(context).edit().putString("history", remaining.toString()).commit()
    }
}
