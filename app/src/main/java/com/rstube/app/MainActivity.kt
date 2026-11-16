package com.rstube.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import org.json.JSONArray
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private val mint = Color.rgb(99, 230, 190)
    private val muted = Color.rgb(156, 177, 185)
    private val bg = Color.rgb(13, 21, 25)
    private val surface = Color.rgb(24, 36, 42)
    private lateinit var link: EditText
    private lateinit var format: RadioGroup
    private lateinit var quality: Spinner
    private lateinit var download: Button
    private lateinit var cancel: Button
    private lateinit var update: Button
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var history: LinearLayout
    private lateinit var appUpdater: AppUpdater
    private lateinit var appUpdateStatus: TextView
    private lateinit var appUpdateButton: Button
    private lateinit var appUpdateCancel: Button
    private lateinit var appUpdateProgress: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var lastHistory = ""
    private val poll = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 600) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(22), dp(22), dp(24)) }
        root.addView(scroll)
        scroll.addView(body)
        setContentView(root)
        root.requestApplyInsets()

        body.addView(label("↓  RS TUBE", 24, mint, true))
        body.addView(label("Your link. Your download.", 32, Color.WHITE, true).apply { setPadding(0, dp(30), 0, dp(8)) })
        body.addView(label("Save video as MP4 or audio as MP3.", 15, muted))
        body.addView(label("FACEBOOK  ·  YOUTUBE  ·  WEB LINKS", 11, mint, true).apply { setPadding(0, dp(18), 0, dp(24)) })

        appUpdater = AppUpdater(this)
        val updateCard = columnCard()
        body.addView(updateCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        updateCard.addView(label("APP UPDATES", 12, mint, true))
        appUpdateStatus = label("Version ${BuildConfig.VERSION_NAME}", 14, Color.WHITE)
        updateCard.addView(appUpdateStatus)
        appUpdateProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = ColorStateList.valueOf(mint)
            indeterminateTintList = ColorStateList.valueOf(mint)
        }
        updateCard.addView(appUpdateProgress, LinearLayout.LayoutParams(-1, dp(20)))
        appUpdateButton = button("Check for app updates", false).apply {
            setOnClickListener {
                val state = appUpdater.state()
                when {
                    state.action == "Install update" -> appUpdater.install(this@MainActivity) { toast(it) }
                    state.availableVersion != null -> appUpdater.download()
                    state.action == "Check for app updates" -> appUpdater.check()
                }
            }
        }
        updateCard.addView(appUpdateButton)
        appUpdateCancel = button("Cancel update download", false).apply { setOnClickListener { appUpdater.cancel() } }
        updateCard.addView(appUpdateCancel)

        val card = columnCard()
        body.addView(card)
        card.addView(label("PASTE A LINK", 12, muted, true))
        link = EditText(this).apply {
            hint = "https://…"
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 16f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            backgroundTintList = ColorStateList.valueOf(mint)
            contentDescription = "Video or audio link"
        }
        card.addView(link, LinearLayout.LayoutParams(-1, dp(58)))
        card.addView(button("Paste from clipboard", false).apply {
            setOnClickListener {
                val clipboard = getSystemService(ClipboardManager::class.java)
                val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this@MainActivity)?.toString().orEmpty()
                val url = DownloadOptions.extractUrl(text)
                if (url == null) toast("No valid web link found") else link.setText(url)
            }
        })
        card.addView(label("DOWNLOAD FORMAT", 12, muted, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        format = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        format.addView(RadioButton(this).apply {
            id = VIDEO; text = "Video · MP4"; setTextColor(Color.WHITE); buttonTintList = ColorStateList.valueOf(mint)
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        format.addView(RadioButton(this).apply {
            id = AUDIO; text = "Audio · MP3"; setTextColor(Color.WHITE); buttonTintList = ColorStateList.valueOf(mint)
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        card.addView(format)
        quality = Spinner(this).apply { contentDescription = "Download quality" }
        card.addView(quality, LinearLayout.LayoutParams(-1, dp(52)))
        format.setOnCheckedChangeListener { _, _ -> setQuality() }
        val prefs = getPreferences(MODE_PRIVATE)
        format.check(if (prefs.getBoolean("audio", false)) AUDIO else VIDEO)
        quality.setSelection(prefs.getInt("quality", 1).coerceIn(0, if (isAudio()) 2 else 3))
        download = button("↓  Download", true).apply { setOnClickListener { startDownload() } }
        card.addView(download, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(14) })
        card.addView(label("Saved in Downloads / RS Tube", 12, muted).apply { setPadding(0, dp(12), 0, 0) })

        val stateCard = columnCard()
        body.addView(stateCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        status = label("Ready to download", 15, Color.WHITE)
        stateCard.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = ColorStateList.valueOf(mint)
            indeterminateTintList = ColorStateList.valueOf(mint)
        }
        stateCard.addView(progress, LinearLayout.LayoutParams(-1, dp(20)).apply { topMargin = dp(8) })
        cancel = button("Cancel download", false).apply {
            setOnClickListener { startService(Intent(this@MainActivity, DownloadService::class.java).setAction(DownloadService.CANCEL)) }
        }
        stateCard.addView(cancel)
        body.addView(label("Recent downloads", 21, Color.WHITE, true).apply { setPadding(0, dp(28), 0, dp(12)) })
        history = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(history)
        update = button("Update download engine", false).apply {
            setOnClickListener { launch(Intent(this@MainActivity, DownloadService::class.java).setAction(DownloadService.UPDATE)) }
        }
        body.addView(update, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(18) })
        body.addView(button("About & source code", false).apply { setOnClickListener { about() } })
        body.addView(label("Download only content you own or have permission to save. Link availability depends on the source.", 12, muted).apply { setPadding(0, dp(16), 0, 0) })

        if (savedInstanceState != null) link.setText(savedInstanceState.getString("link", ""))
        else receive(intent)
        if (!DownloadService.busy && DownloadStore.progress(this) in 0..99) {
            DownloadStore.state(this, "Previous download stopped. Paste the link to retry.")
        }
    }

    private fun setQuality() {
        val labels = if (isAudio()) listOf("128 kbps · Small file", "192 kbps · Balanced", "320 kbps · High bitrate")
            else listOf("360p · Small file", "720p · Balanced", "1080p · High quality", "Best available")
        quality.adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).apply { (this as TextView).setTextColor(Color.WHITE) }
        }.apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        quality.setSelection(1)
    }

    private fun isAudio() = format.checkedRadioButtonId == AUDIO

    private fun startDownload() {
        val url = DownloadOptions.extractUrl(link.text.toString())
        if (url == null) { link.error = "Enter a valid HTTP or HTTPS link"; return }
        val audio = isAudio()
        val selected = quality.selectedItemPosition.coerceAtLeast(0)
        val qualityValue = if (audio) listOf("128", "192", "320")[selected] else listOf("360", "720", "1080", "best")[selected]
        getPreferences(MODE_PRIVATE).edit().putBoolean("audio", audio).putInt("quality", selected).apply()
        launch(Intent(this, DownloadService::class.java).putExtra("url", url).putExtra("audio", audio).putExtra("quality", qualityValue))
    }

    private fun launch(intent: Intent) {
        if (DownloadService.busy) { toast("Wait for the current task to finish"); return }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        try { startForegroundService(intent) } catch (e: Exception) { toast("Could not start: ${e.localizedMessage}") }
    }

    private fun refresh() {
        refreshAppUpdate()
        val busy = DownloadService.busy
        status.text = DownloadStore.status(this)
        val current = DownloadStore.progress(this)
        progress.visibility = if (busy || current == 100) View.VISIBLE else View.GONE
        progress.isIndeterminate = current < 0
        progress.progress = current.coerceAtLeast(0)
        cancel.visibility = if (busy) View.VISIBLE else View.GONE
        download.isEnabled = !busy
        update.isEnabled = !busy
        download.alpha = if (busy) 0.45f else 1f
        val items = DownloadStore.history(this)
        val signature = items.toString()
        if (signature != lastHistory) { lastHistory = signature; renderHistory(items) }
    }

    private fun refreshAppUpdate() {
        val state = appUpdater.state()
        appUpdateStatus.text = state.message
        appUpdateButton.text = state.action
        appUpdateButton.isEnabled = state.action == "Check for app updates" ||
            state.action == "Install update" || state.availableVersion != null
        appUpdateProgress.visibility = if (state.cancellable || state.progress == 100) View.VISIBLE else View.GONE
        appUpdateProgress.isIndeterminate = state.progress < 0
        appUpdateProgress.progress = state.progress.coerceAtLeast(0)
        appUpdateCancel.visibility = if (state.cancellable) View.VISIBLE else View.GONE
        val version = state.availableVersion
        val prefs = getPreferences(MODE_PRIVATE)
        if (version != null && hasWindowFocus() && prefs.getString("updatePrompt", "") != version) {
            prefs.edit().putString("updatePrompt", version).apply()
            AlertDialog.Builder(this).setTitle("Update available · $version")
                .setMessage("Download the new RS Tube version here. Android will ask you to confirm installation.")
                .setPositiveButton("Download update") { _, _ -> appUpdater.download() }
                .setNegativeButton("Later", null).show()
        }
    }

    private fun renderHistory(items: JSONArray) {
        history.removeAllViews()
        if (items.length() == 0) history.addView(label("Your downloads will appear here.", 14, muted))
        for (i in 0 until minOf(items.length(), 12)) {
            val item = items.getJSONObject(i)
            val card = columnCard()
            val audio = item.optBoolean("audio")
            card.addView(label(if (audio) "MP3  ·  AUDIO" else "MP4  ·  VIDEO", 11, mint, true))
            card.addView(label(item.optString("name"), 14, Color.WHITE, true).apply { maxLines = 3; setPadding(0, dp(6), 0, dp(4)) })
            card.addView(label(item.optString("result"), 12, muted))
            card.addView(label(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.optLong("time"))), 11, muted))
            val uri = item.optString("uri")
            if (uri.isNotBlank()) card.addView(button("Share file", false).apply {
                setOnClickListener {
                    try {
                        val contentUri = Uri.parse(uri)
                        contentResolver.openFileDescriptor(contentUri, "r")?.use { } ?: error("File missing")
                        val share = Intent(Intent.ACTION_SEND).setType(if (audio) "audio/mpeg" else "video/mp4")
                            .putExtra(Intent.EXTRA_STREAM, contentUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        share.clipData = ClipData.newRawUri("Download", contentUri)
                        startActivity(Intent.createChooser(share, "Share download"))
                    } catch (_: Exception) { toast("File is unavailable. It may have been moved or deleted.") }
                }
            })
            history.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
    }

    private fun receive(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val url = DownloadOptions.extractUrl(text)
            if (url != null) link.setText(url) else toast("The shared text contains no valid web link")
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    override fun onResume() { super.onResume(); appUpdater.check(automatic = true); handler.post(poll) }
    override fun onPause() { handler.removeCallbacks(poll); super.onPause() }
    override fun onDestroy() { appUpdater.close(); super.onDestroy() }
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppUpdater.INSTALL_PERMISSION) {
            if (packageManager.canRequestPackageInstalls()) appUpdater.install(this) { toast(it) }
            else toast("Installation permission was not granted. Tap Install update to try again.")
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("link", link.text.toString())
        getPreferences(MODE_PRIVATE).edit().putBoolean("audio", isAudio()).putInt("quality", quality.selectedItemPosition).apply()
        super.onSaveInstanceState(outState)
    }

    private fun about() {
        AlertDialog.Builder(this).setTitle("RS Tube · ${BuildConfig.VERSION_NAME}")
            .setMessage("MP4 video and MP3 audio downloads. No built-in player.\n\nBuilt with yt-dlp, youtubedl-android and FFmpeg. Free software under GNU GPL v3; no warranty. You may redistribute it under that license.\n\nProcessing happens on your device. Links go to their source websites; the engine updater contacts GitHub. No ads or analytics.\n\nDownloads may fail when a website changes or restricts access. Higher MP3 bitrate does not improve the original recording.\n\nSource code, licenses and build instructions are available in the repository.")
            .setPositiveButton("Source & licenses") { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rajibsarder72942/RS-Tube"))) }
            .setNegativeButton("Close", null).show()
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun columnCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(surface)
    }
    private fun rounded(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(18).toFloat() }
    private fun button(value: String, primary: Boolean) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 15f; minHeight = dp(48)
        setTextColor(if (primary) bg else mint)
        background = rounded(if (primary) mint else Color.TRANSPARENT)
        setPadding(dp(12), 0, dp(12), 0)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    companion object { private const val VIDEO = 1001; private const val AUDIO = 1002 }
}
