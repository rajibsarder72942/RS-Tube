package com.rstube.app

import java.net.URI

data class AppRelease(val code: Long, val version: String, val url: String, val sha256: String, val bytes: Long)

object UpdatePolicy {
    const val REPOSITORY = "rajibsarder72942/RS-Tube"
    const val MAX_BYTES = 512L * 1024 * 1024
    fun isNewer(release: AppRelease, installed: Long) = release.code > installed
    fun validate(release: AppRelease): AppRelease {
        require(release.code > 0 && release.version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) { "Invalid update version" }
        require(release.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Update has no valid checksum" }
        require(release.bytes in 1..MAX_BYTES) { "Invalid update file size" }
        val uri = URI(release.url)
        require(uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 && uri.userInfo == null &&
            uri.path.startsWith("/$REPOSITORY/releases/download/v${release.version}/") &&
            uri.path.endsWith(".apk") && uri.rawQuery == null && uri.fragment == null) { "Untrusted update location" }
        return release
    }
    fun sameSigners(installed: Set<String>, downloaded: Set<String>) =
        installed.isNotEmpty() && installed == downloaded
}
