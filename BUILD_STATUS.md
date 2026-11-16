# RS Tube 0.2.2 verification

- Application version: 0.2.2, versionCode 4.
- Tested application source: 90268921db67db29da63319c739391741f20d442.
- Android build, unit tests and debug/release lint passed: https://github.com/rajibsarder72942/RS-Tube/actions/runs/43933611042.
- Stable-key APK certificate, artifact checksum and update metadata verified during publishing: https://github.com/rajibsarder72942/RS-Tube/actions/runs/43934428030.
- Anonymous public update.json and GitHub-uploaded APK digest verified by the publisher.
- Official release: https://github.com/rajibsarder72942/RS-Tube/releases/tag/v0.2.2.
- No manual check button or idle update panel. Checks run on app open/return and periodically while the app remains open.
- New version available → Update now → in-app download and verification → Android installation confirmation.
- The installer opens automatically only after a user-requested, verified update and while the app is in the foreground. Android installation permission/confirmation is still required.
- The approved yellow/dark-red download logo and stable signing key are retained.
- A complete physical-device update/install test and live source-site download tests remain pending.
- Temporary one-version publishing workflows have been removed. Existing tag-based release publishing remains unchanged.
