# In-app updates

Starting with **0.2.2**, RS Tube silently checks this repository's public stable GitHub Releases whenever the app opens or returns to the foreground, and every five minutes while it remains open. A 30-second throttle avoids repeated checks on screen rotation. There is no check-for-updates button or idle updates panel. A published build artifact or a source commit alone is not an app release.

When a newer `versionCode` is published, the app displays **New version available**. Tapping **Update now** downloads inside the app with progress/cancel. After validating the APK's size, SHA-256, package ID, version and signing certificate, RS Tube opens Android's package installer automatically while the app is in the foreground. No browser or third-party website is opened by the updater. Android may first require **Allow from this source** for RS Tube. The user must confirm installation; silent installation is not implemented.

The Android DownloadManager keeps transferring if the Activity rotates or backgrounds. Opening RS Tube again resumes progress display, verification and a pending installer handoff. Installer cancellation leaves the verified APK ready to retry without repeatedly reopening the installer. A retry Install button appears only for an already downloaded update. Downloads require the user's Update now tap and may then continue on mobile data. Network check errors stay silent and retry automatically.

## One-time stable signing setup

All official builds must use the same private signing key. Do not use a new random debug keystore for each release. Do not put signing keys or passwords in the public repository or release assets.

In [repository Actions secrets](https://github.com/rajibsarder72942/RS-Tube/settings/secrets/actions), add:

| Secret | Value |
| --- | --- |
| `RSTUBE_KEYSTORE_BASE64` | Base64 of the private stable keystore |
| `RSTUBE_KEYSTORE_PASSWORD` | Keystore password |
| `RSTUBE_KEY_ALIAS` | Stable signing alias |
| `RSTUBE_KEY_PASSWORD` | Password for the signing key |

The build creates a private temporary keystore only for trusted branch/tag builds, never uploads it, and deletes it at the end. No long-lived GitHub token is embedded in the app. Fork pull requests receive no repository secrets.

The original **0.1.0 test APK** used an ephemeral debug key that was not retained by CI. It cannot receive updates signed with a different stable key. Moving from that test APK to a stable-key build may require **one uninstall/reinstall**. This removes app-local history; exported Downloads stay on the phone. Once the stable-key build is installed, keep its key for every subsequent version. RS Tube refuses any in-app APK with a different signer instead of asking the user to uninstall.

## Publish every new version

1. Increase both `versionName` and the integer `versionCode` in `app/build.gradle.kts` (for example, 0.2.0/code 2 → 0.2.1/code 3).
2. Push the code and run/test the signed build.
3. Push the matching version tag, for example `git tag v0.2.1` then `git push origin v0.2.1`.
4. The workflow tests/lints, builds with the stable private key, packages the APK and matching source, generates `update.json`, and publishes a stable GitHub Release. Tag/version mismatch, missing signing secrets, build failure or missing update metadata prevents publication.
5. Existing installations discover the newer version at their next check. They download directly from this repository's release assets inside the app.

Keep the repository public so users can check and download without embedding credentials. Do not replace an already published version with unrelated bytes or reuse its versionCode. Preview/prerelease releases are intentionally excluded from automatic updates. Key rotation is not supported in this first implementation.

## Required phone verification before calling the updater production-ready

- Install a stable-key 0.2.0 build, then publish a higher-code build signed with that same key.
- Confirm automatic discovery on app open/return and while open, Later, no idle check button, progress/cancel, background/reopen and automatic installer handoff after verification.
- Allow/deny installation permission, cancel the installer, retry, then install successfully and confirm app history remains.
- Confirm a changed signer, checksum mismatch, wrong package/version, truncated file and failed network never open the installer.
- Confirm every updater path stays in the app or Android system settings/installer, with no browser redirect.

Unit tests validate the update trust/version policy; compilation and lint do not replace this physical-device test.
