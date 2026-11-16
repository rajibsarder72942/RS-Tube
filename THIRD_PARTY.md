# Third-party software

RS Tube uses these unmodified dependencies, fetched by Gradle from Maven Central:

| Component | Version | License / source |
| --- | --- | --- |
| youtubedl-android library | 0.18.1 | GPL-3.0: https://github.com/yausername/youtubedl-android/tree/0.18.1 |
| youtubedl-android FFmpeg package | 0.18.1 | See source and bundled notices: https://github.com/yausername/youtubedl-android/tree/0.18.1/ffmpeg |
| yt-dlp | Bundled by the library; user-updatable | Unlicense and dependency notices: https://github.com/yt-dlp/yt-dlp |
| FFmpeg and bundled Python/native dependencies | Bundled by the packages above | Upstream build recipes, source references and licenses: https://github.com/yausername/youtubedl-android/tree/0.18.1 |
| AndroidX and transitive JVM libraries | Resolved by Gradle | Respect their included notices; inspect the dependency report with `gradle app:dependencies` |

The exact app source corresponding to each CI APK is shipped in the same artifact as `RS-Tube-source.zip`. Build configuration pins the native package version. No bundled native binaries were modified by RS Tube. Consult the upstream version's build scripts and notices when rebuilding native components. Engine updates replace yt-dlp on the device; the updated version's source and license are provided by its GitHub release.

Before production redistribution, archive the complete corresponding native dependency sources, build recipes and license notices with the release. A repository URL alone is not a substitute for fulfilling any corresponding-source obligations that apply to a distributed native binary.
