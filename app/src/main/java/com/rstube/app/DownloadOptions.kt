package com.rstube.app

import java.net.URI

/** Pure request policy; arguments are passed directly, never through a shell. */
object DownloadOptions {
    fun extractUrl(text: String): String? {
        val candidate = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(text.trim())
            ?.value?.trimEnd('.', ',', ')', ']', '}', '"', '\'') ?: return null
        return try {
            val uri = URI(candidate)
            if (uri.scheme.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null)
                uri.toASCIIString() else null
        } catch (_: Exception) { null }
    }

    fun arguments(audio: Boolean, quality: String): List<Pair<String, String?>> {
        val common = listOf(
            "--no-playlist" to null, "--no-overwrites" to null,
            "--newline" to null, "--no-mtime" to null,
            "--socket-timeout" to "30", "--retries" to "3",
            "--fragment-retries" to "3", "--restrict-filenames" to null,
            "--trim-filenames" to "140"
        )
        return common + if (audio) {
            require(quality in listOf("128", "192", "320"))
            listOf("-f" to "bestaudio/best", "-x" to null,
                "--audio-format" to "mp3", "--audio-quality" to "${quality}K")
        } else {
            require(quality in listOf("360", "720", "1080", "best"))
            val cap = if (quality == "best") "" else "[height<=$quality]"
            listOf("-f" to "bv*$cap+ba/b$cap", "--merge-output-format" to "mp4",
                "--recode-video" to "mp4")
        }
    }
}
