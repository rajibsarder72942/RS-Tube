package com.rstube.app

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

/** Public release discovery, Android-managed in-app download and a system installer handoff. */
class AppUpdater(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("app-updates", Context.MODE_PRIVATE)
    private val manager = app.getSystemService(DownloadManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val checking = AtomicBoolean(false)
    private val verifying = AtomicBoolean(false)
    private val pm get() = app.packageManager
    private val installed get() = pm.getPackageInfo(app.packageName, 0).longVersionCode
    private fun apk(code: Long) = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates/RS-Tube-$code.apk")

    data class State(val message: String, val action: String, val progress: Int = -1, val cancellable: Boolean = false,
        val availableVersion: String? = null, val visible: Boolean = true, val readyToInstall: Boolean = false)

    private fun release(): AppRelease? = try {
        val json = JSONObject(prefs.getString("release", "")!!)
        UpdatePolicy.validate(AppRelease(json.getLong("versionCode"), json.getString("versionName"),
            json.getString("url"), json.getString("sha256"), json.getLong("size")))
    } catch (_: Exception) { null }

    private fun json(release: AppRelease) = JSONObject().put("versionCode", release.code)
        .put("versionName", release.version).put("url", release.url).put("sha256", release.sha256).put("size", release.bytes)

    fun check() {
        val now = System.currentTimeMillis()
        if (!UpdateFlow.shouldCheck(now, prefs.getLong("lastAttempt", 0))) return
        if (prefs.getLong("downloadId", -1) >= 0 || !checking.compareAndSet(false, true)) return
        prefs.edit().putLong("lastAttempt", now).apply()
        executor.execute {
            try {
                val releases = JSONArray(readJson("https://api.github.com/repos/${UpdatePolicy.REPOSITORY}/releases?per_page=20"))
                var candidate: AppRelease? = null
                // Release assets, not build artifacts: a build alone is never advertised as an update.
                for (i in 0 until releases.length()) {
                    val entry = releases.getJSONObject(i)
                    if (entry.optBoolean("draft") || entry.optBoolean("prerelease")) continue
                    val assets = entry.getJSONArray("assets")
                    val manifest = (0 until assets.length()).map { assets.getJSONObject(it) }
                        .firstOrNull { it.optString("name") == "update.json" } ?: continue
                    val tag = entry.getString("tag_name")
                    val manifestUrl = manifest.getString("browser_download_url")
                    require(manifestUrl == "https://github.com/${UpdatePolicy.REPOSITORY}/releases/download/$tag/update.json") { "Untrusted release metadata" }
                    val info = JSONObject(readJson(manifestUrl))
                    val found = UpdatePolicy.validate(AppRelease(info.getLong("versionCode"), info.getString("versionName"),
                        info.getString("url"), info.getString("sha256"), info.getLong("size")))
                    require(tag == "v${found.version}") { "Release version mismatch" }
                    val matchingAsset = (0 until assets.length()).map { assets.getJSONObject(it) }
                        .any { it.optString("browser_download_url") == found.url && it.optLong("size") == found.bytes }
                    require(matchingAsset) { "Release APK is missing" }
                    if (candidate == null || found.code > candidate.code) candidate = found
                }
                val newer = candidate?.takeIf { UpdatePolicy.isNewer(it, installed) }
                val edit = prefs.edit().putLong("lastCheck", System.currentTimeMillis())
                if (newer != null) edit.putString("release", json(newer).toString())
                    .putString("message", "New version available · ${newer.version}")
                else edit.remove("release").putString("message", "")
                edit.apply()
            } catch (_: Exception) {
                // Keep a previously discovered update. Network failures stay silent and retry automatically.
            } finally { checking.set(false) }
        }
    }

    fun state(): State {
        val release = release()
        if (release == null || !UpdatePolicy.isNewer(release, installed)) {
            if (release != null) cancel(clearRelease = true)
            return State("", "", visible = false)
        }
        val id = prefs.getLong("downloadId", -1)
        if (id < 0) return State(prefs.getString("message", "New version available · ${release.version}")!!, "Update now",
            availableVersion = release.version)
        try {
            manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                if (!cursor.moveToFirst()) { fail("Update download interrupted. Tap to retry."); return state() }
                fun number(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
                when (number(DownloadManager.COLUMN_STATUS).toInt()) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        if (prefs.getLong("verified", -1) == release.code && apk(release.code).isFile)
                            return State("Update ${release.version} ready to install", "Install update", 100, readyToInstall = true)
                        verify(release)
                        return State("Verifying update…", "Verifying…", -1, true)
                    }
                    DownloadManager.STATUS_FAILED -> { fail("Update download failed. Check connection and storage, then retry."); return state() }
                    else -> {
                        val total = number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val done = number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val percent = if (total > 0) (done * 100 / total).toInt().coerceIn(0, 100) else -1
                        val paused = number(DownloadManager.COLUMN_STATUS).toInt() == DownloadManager.STATUS_PAUSED
                        return State(if (paused) "Update waiting for network…" else "Downloading update · ${percent.coerceAtLeast(0)}%", "Downloading…", percent, true)
                    }
                }
            }
        } catch (_: Exception) { fail("Could not read update download. Tap to retry."); return state() }
    }

    fun download() {
        val release = release() ?: return
        if (!UpdatePolicy.isNewer(release, installed)) return
        if (prefs.getLong("downloadId", -1) >= 0) return
        try {
            val file = apk(release.code)
            file.parentFile?.mkdirs()
            file.delete()
            val request = DownloadManager.Request(Uri.parse(release.url))
                .setTitle("RS Tube ${release.version}")
                .setDescription("Downloading app update inside RS Tube")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, "updates/${file.name}")
                .setAllowedOverMetered(true).setAllowedOverRoaming(false)
            val id = manager.enqueue(request)
            prefs.edit().putLong("downloadId", id).putBoolean("offerInstall", true).remove("verified").apply()
        } catch (_: Exception) { fail("Could not start update download. Check storage and retry.") }
    }

    fun cancel(clearRelease: Boolean = false) {
        val id = prefs.getLong("downloadId", -1)
        if (id >= 0) manager.remove(id)
        release()?.let { apk(it.code).delete() }
        prefs.edit().remove("downloadId").remove("verified").remove("offerInstall").putString("message", "Update download cancelled")
            .apply { if (clearRelease) remove("release") }.apply()
    }

    private fun fail(message: String) { cancel(); prefs.edit().putString("message", message).apply() }

    fun takeInstallOffer(foreground: Boolean): Boolean {
        val release = release() ?: return false
        val shouldOffer = UpdateFlow.shouldOfferInstaller(
            !verifying.get() && prefs.getLong("verified", -1) == release.code && apk(release.code).isFile,
            prefs.getBoolean("offerInstall", false), foreground)
        if (shouldOffer) prefs.edit().putBoolean("offerInstall", false).commit()
        return shouldOffer
    }

    private fun verify(release: AppRelease) {
        if (!verifying.compareAndSet(false, true)) return
        val id = prefs.getLong("downloadId", -1)
        executor.execute {
            try {
                verifyApk(release)
                if (prefs.getLong("downloadId", -1) == id && id >= 0)
                    prefs.edit().putLong("verified", release.code).apply()
            } catch (e: Exception) {
                if (prefs.getLong("downloadId", -1) == id) fail(e.message ?: "Update verification failed")
            } finally { verifying.set(false) }
        }
    }

    private fun verifyApk(release: AppRelease) {
        val file = apk(release.code)
        check(file.isFile && file.length() == release.bytes) { "Update file is incomplete. Download it again." }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        check(hex(digest.digest()).equals(release.sha256, true)) { "Update checksum failed. Download it again." }
        val archive = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("Invalid update APK")
        val current = pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        check(archive.packageName == app.packageName && archive.longVersionCode == release.code &&
            archive.versionName == release.version && archive.longVersionCode > current.longVersionCode) { "Update package/version mismatch" }
        fun signers(info: android.content.pm.PackageInfo) = info.signingInfo?.apkContentsSigners
            ?.map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }?.toSet().orEmpty()
        check(UpdatePolicy.sameSigners(signers(current), signers(archive))) {
            "Update signing key does not match this installation. Contact the publisher."
        }
    }

    @Suppress("DEPRECATION")
    fun install(activity: Activity, onError: (String) -> Unit) {
        val release = release() ?: return
        if (prefs.getLong("verified", -1) != release.code) return
        if (!pm.canRequestPackageInstalls()) {
            try { activity.startActivityForResult(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${app.packageName}")), INSTALL_PERMISSION) }
            catch (_: Exception) { onError("Allow RS Tube to install updates in Android Settings, then tap Install update.") }
            return
        }
        if (!verifying.compareAndSet(false, true)) return
        executor.execute {
            try {
                verifyApk(release)
                main.post {
                    if (!activity.isFinishing && !activity.isDestroyed && activity.hasWindowFocus()) {
                        try {
                            val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", apk(release.code))
                            activity.startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        } catch (_: Exception) { onError("Android could not open the installer. Tap Install update to retry.") }
                    } else prefs.edit().putBoolean("offerInstall", true).apply()
                }
            } catch (e: Exception) { main.post { fail(e.message ?: "Update verification failed"); onError(e.message.orEmpty()) } }
            finally { verifying.set(false) }
        }
    }

    private fun readJson(url: String): String {
        var target = URL(url)
        repeat(5) {
            require(target.protocol == "https" && target.userInfo == null && (target.port == -1 || target.port == 443) &&
                target.host in setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")) { "Untrusted update redirect" }
            val connection = target.openConnection() as HttpsURLConnection
            connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "RS-Tube/${BuildConfig.VERSION_NAME}")
            try {
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    target = URL(target, connection.getHeaderField("Location") ?: error("Missing redirect"))
                } else {
                    check(code == 200) { "Update check HTTP $code" }
                    return connection.inputStream.use {
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = it.read(buffer)
                            if (n < 0) break
                            check(output.size() + n <= 512 * 1024) { "Update response too large" }
                            output.write(buffer, 0, n)
                        }
                        output.toString("UTF-8")
                    }
                }
            } finally { connection.disconnect() }
        }
        error("Too many update redirects")
    }

    fun close() { executor.shutdown() }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    companion object { const val INSTALL_PERMISSION = 812 }
}
