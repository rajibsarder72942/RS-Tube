package com.rstube.app

import org.junit.Assert.*
import org.junit.Test

class DownloadOptionsTest {
    @Test fun acceptsSharedTextAndPreservesQuery() {
        assertEquals("https://youtu.be/abc?t=30", DownloadOptions.extractUrl("Watch this https://youtu.be/abc?t=30"))
        assertEquals("https://example.org/movie.mp4", DownloadOptions.extractUrl("(https://example.org/movie.mp4)"))
    }
    @Test fun rejectsUnsupportedSchemesCredentialsAndMalformedUrls() {
        for (url in listOf("file:///sdcard/private", "javascript:alert(1)", "https://user:pass@example.org/a", "https:///broken", "not a link"))
            assertNull(url, DownloadOptions.extractUrl(url))
    }
    @Test fun audioIsActuallyConvertedToMp3() {
        val options = DownloadOptions.arguments(true, "192").toMap()
        assertTrue(options.containsKey("-x"))
        assertEquals("mp3", options["--audio-format"])
        assertEquals("192K", options["--audio-quality"])
        assertFalse(options.containsKey("--recode-video"))
    }
    @Test fun videoIsConvertedAndResolutionIsCapped() {
        val options = DownloadOptions.arguments(false, "720").toMap()
        assertEquals("mp4", options["--recode-video"])
        assertEquals("mp4", options["--merge-output-format"])
        assertTrue(options["-f"]!!.contains("height<=720"))
        assertTrue(options.containsKey("--no-playlist"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInjectedQualityOptions() { DownloadOptions.arguments(false, "720 --exec anything") }
}
