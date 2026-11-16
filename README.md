# RS Tube

An Android downloader for **MP4 video** and **MP3 audio**. No built-in player.

## Install the first test build

1. Open [Actions → Build Android APK](https://github.com/rajibsarder72942/RS-Tube/actions/workflows/android.yml).
2. Open the latest successful run and download **RS-Tube-Android** under Artifacts (sign in to GitHub).
3. Unzip it on your Android phone and install the versioned RS Tube APK. Official signed APKs are also available from [Releases](https://github.com/rajibsarder72942/RS-Tube/releases).
4. Allow installation from your browser/file manager if Android asks. Android 10 or newer is required.

The artifact also includes the exact source and SHA-256 checksums. Without configured stable signing secrets, CI builds a test APK; it is not published as an in-app update. Official update releases must share one private stable signing key. See [UPDATES.md](UPDATES.md) for the one-time setup and release process.

## Features

- Paste a link or use another app's **Share → RS Tube**.
- MP4: 360p, 720p, 1080p or best available; resolution selection is an upper limit.
- MP3: 128, 192 or 320 kbps, with real audio conversion through FFmpeg.
- Background download with progress notification, cancel, and the latest 50 history records (12 shown in the UI).
- Save through Android MediaStore to **Downloads/RS Tube** without broad storage access.
- Share a saved file with another app. RS Tube has no playback screen or player.
- Update the bundled yt-dlp engine from within the app.
- **App updates:** detect newer published releases, download their APK inside RS Tube, verify it and open Android installation confirmation. No browser redirect. See [UPDATES.md](UPDATES.md).
- Dark interface, no ads, login, analytics, remote processing server or API key.

## Source support and limits

Facebook, YouTube, direct media URLs and other websites are handled by yt-dlp's supported extractors. This is not a promise that every link works. Public videos can still be restricted, throttled or blocked; website changes can break extraction. Try **Update download engine** if extraction stops working. Modern YouTube extraction can require additional runtime support unavailable in this first build. Do not treat an installed APK as evidence that live YouTube or Facebook downloads have passed testing.

No DRM bypass, account-cookie import, login-wall bypass, private-video access or certificate-verification bypass is implemented. Use only content you own or have permission to download, and follow the source's rules. RS Tube is not affiliated with YouTube, Facebook or Meta.

MP4 is a container; codec compatibility depends on the source and FFmpeg conversion. Conversion can be slow and use battery. Increasing MP3 bitrate does not improve the source audio quality. Temporary input and the exported output may coexist, so leave enough free storage. Only one task runs at a time; no queue or pause/resume is included in v0.1.0. Android can stop background work; interrupted downloads require retrying. Cancellation of engine updates may wait for the network operation to return.

## Build

Use JDK 17, Android SDK 35 and Gradle 8.13. Open this folder in Android Studio or install Gradle and run:

```sh
gradle testDebugUnitTest lintDebug assembleDebug
```

This initial repository uses the pinned Gradle version from `gradle/actions/setup-gradle` in CI; no Gradle wrapper binary is checked in. To add a wrapper locally with Gradle installed: `gradle wrapper --gradle-version 8.13`.

Every push to `main` builds an APK. A matching `v*` tag publishes a stable signed update release only after checks pass and stable signing secrets have been configured. Test builds without those secrets are never advertised as automatic updates.

## Verification on a phone

Before calling a release production-ready, test on physical Android devices:

- Paste and share an authorized direct media link, a YouTube link and a Facebook link; report the exact failure if the source blocks access.
- Download each format and confirm the actual media format using a media inspector, not just the filename.
- Confirm audio/video duration, MP3 bitrate, video quality cap and exported file visibility in Files.
- Rotate the phone and background/reopen the app during download.
- Cancel during extraction, downloading, conversion and final export; check for partial public files.
- Test notification denial, offline/network loss, insufficient disk space, a deleted history file, Android timeout and process termination.
- Verify Android 10, Android 14 and Android 15+; native executable/16 KB page support needs device verification, particularly newer devices.

Local unit tests cover URL validation and media conversion request policy. CI compilation and lint do not verify live site extraction or native FFmpeg operation.

## Privacy

Processing happens on the phone. Source websites receive the URLs and network requests needed for downloads. Engine updates contact GitHub. Download history is stored locally; app backup is disabled. No credentials are requested, and no backend is used. Cleartext HTTP is enabled for user-entered HTTP media URLs; prefer HTTPS links.

## License and third-party software

RS Tube source is licensed under GNU GPL v3. See [LICENSE](LICENSE) and [THIRD_PARTY.md](THIRD_PARTY.md). No warranty is provided. The About screen links to the source and license notices.
