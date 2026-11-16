package com.rstube.app

import android.app.*
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import android.os.*
import android.provider.MediaStore
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** One download at a time. Work lives outside the Activity and survives rotation. */
class DownloadService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private val cancelled = AtomicBoolean(false)
    private var processId = ""
    private val main = Handler(Looper.getMainLooper())
    private var lastNotice = 0L
    private val notices by lazy { getSystemService(NotificationManager::class.java) }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        notices.createNotificationChannel(NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == CANCEL) {
            cancel()
            if (!busy) stopSelf()
            return START_NOT_STICKY
        }
        if (busy) return START_NOT_STICKY
        val update = intent?.action == UPDATE
        val url = DownloadOptions.extractUrl(intent?.getStringExtra("url") ?: "")
        if (!update && url == null) { stopSelf(); return START_NOT_STICKY }
        busy = true
        cancelled.set(false)
        processId = UUID.randomUUID().toString()
        val initial = if (update) "Updating download engine…" else "Preparing download…"
        DownloadStore.state(this, initial)
        try {
            startForeground(NOTICE, notification(initial), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (e: Exception) {
            busy = false
            DownloadStore.state(this, "Could not start: ${e.localizedMessage}")
            stopSelf()
            return START_NOT_STICKY
        }
        val audio = intent?.getBooleanExtra("audio", false) ?: false
        val quality = intent?.getStringExtra("quality") ?: if (audio) "192" else "720"
        executor.execute { runTask(update, url, audio, quality) }
        return START_NOT_STICKY
    }

    private fun runTask(update: Boolean, url: String?, audio: Boolean, quality: String) {
        var folder: File? = null
        try {
            YoutubeDL.init(applicationContext)
            FFmpeg.init(applicationContext)
            checkCancelled()
            if (update) {
                YoutubeDL.updateYoutubeDL(applicationContext)
                checkCancelled()
                report("Download engine is up to date", 100)
            } else {
                // Remove only abandoned private work directories, never public downloads.
                File(cacheDir, "jobs").listFiles()?.forEach { it.deleteRecursively() }
                folder = File(cacheDir, "jobs/$processId").apply { mkdirs() }
                val request = YoutubeDLRequest(requireNotNull(url))
                DownloadOptions.arguments(audio, quality).forEach { (option, value) ->
                    if (value == null) request.addOption(option) else request.addOption(option, value)
                }
                request.addOption("-o", File(folder, "%(title).100B-%(id)s.%(ext)s").absolutePath)
                checkCancelled()
                YoutubeDL.execute(request, processId, false, callback = { percent, _, line ->
                    if (cancelled.get()) YoutubeDL.destroyProcessById(processId)
                    val progress = percent.toInt().coerceIn(0, 100)
                    val phase = if (line.contains("[ExtractAudio]") || line.contains("[VideoConvertor]") ||
                        line.contains("[Merger]")) "Converting to ${if (audio) "MP3" else "MP4"}…"
                        else "Downloading ${if (audio) "audio" else "video"} · $progress%"
                    report(phase, progress)
                })
                checkCancelled()
                val extension = if (audio) "mp3" else "mp4"
                val output = folder.listFiles()?.filter { it.extension.equals(extension, true) && it.length() > 0 }
                    ?.singleOrNull() ?: error("No $extension file was produced. This link or format may not be supported.")
                report("Saving to Downloads/RS Tube…")
                val uri = export(output, audio)
                DownloadStore.record(this, output.name, uri, audio, "Saved")
                report("Saved to Downloads/RS Tube", 100)
            }
        } catch (e: Exception) {
            val message = if (cancelled.get() || e is YoutubeDL.CanceledException || e is InterruptedException)
                "Cancelled" else "Failed: ${friendlyError(e)}"
            if (!update) DownloadStore.record(this, url.orEmpty(), "", audio, message)
            report(message)
        } finally {
            folder?.deleteRecursively()
            main.post {
                busy = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun export(file: File, audio: Boolean): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, if (audio) "audio/mpeg" else "video/mp4")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/RS Tube")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create a file in Downloads")
        try {
            val stream = contentResolver.openOutputStream(uri) ?: error("Unable to open Downloads file")
            stream.use { out ->
                file.inputStream().use { input ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        checkCancelled()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                    }
                }
            }
            checkCancelled()
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            check(contentResolver.update(uri, values, null, null) == 1) { "Unable to finish saving file" }
            return uri.toString()
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun report(message: String, progress: Int = -1) {
        DownloadStore.state(this, message, progress)
        val now = SystemClock.elapsedRealtime()
        if (now - lastNotice > 800 || progress == 100) {
            lastNotice = now
            if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                notices.notify(NOTICE, notification(message, progress))
        }
    }

    private fun notification(message: String, progress: Int = -1): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val cancel = PendingIntent.getService(this, 1, Intent(this, DownloadService::class.java).setAction(CANCEL), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_download)
            .setContentTitle("RS Tube").setContentText(message).setContentIntent(open)
            .setOnlyAlertOnce(true).setOngoing(true)
            .setProgress(100, progress.coerceAtLeast(0), progress < 0)
            .addAction(Notification.Action.Builder(null, "Cancel", cancel).build()).build()
    }

    private fun friendlyError(e: Exception): String {
        val message = e.localizedMessage.orEmpty()
        return when {
            message.contains("Unsupported URL", true) -> "This website or link is not supported. Try a direct media link."
            message.contains("sign in", true) || message.contains("login", true) -> "This source requires sign-in or has restricted access. Try a publicly accessible link."
            message.contains("No space", true) -> "Not enough free storage. Free space and try again."
            else -> message.lineSequence().lastOrNull { it.isNotBlank() }?.take(350) ?: "Please check the link and connection."
        }
    }

    private fun checkCancelled() {
        if (cancelled.get() || Thread.currentThread().isInterrupted) throw InterruptedException()
    }

    private fun cancel() {
        cancelled.set(true)
        // Repeat briefly to cover cancellation between process launch and registration.
        repeat(8) { attempt -> main.postDelayed({ YoutubeDL.destroyProcessById(processId) }, attempt * 200L) }
        DownloadStore.state(this, "Cancelling…")
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        cancel()
        DownloadStore.state(this, "Stopped by Android time limit. Open RS Tube and retry.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cancelled.set(true)
        YoutubeDL.destroyProcessById(processId)
        executor.shutdownNow()
        busy = false
        super.onDestroy()
    }

    companion object {
        const val CANCEL = "com.rstube.app.CANCEL"
        const val UPDATE = "com.rstube.app.UPDATE"
        private const val CHANNEL = "downloads"
        private const val NOTICE = 41
        @Volatile var busy = false
            private set
    }
}
