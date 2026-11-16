# RS Tube 0.2.3 verification

- Application version 0.2.3, versionCode 5.
- Tested source commit: a569cf429172d2c5b6d8f2e3d96c149781676bb1.
- Build, unit tests and debug/release lint passed: https://github.com/rajibsarder72942/RS-Tube/actions/runs/43962867738.
- Signed artifact checksum, stable signing certificate, public update metadata and uploaded APK digest verified: https://github.com/rajibsarder72942/RS-Tube/actions/runs/43963631076.
- Official release: https://github.com/rajibsarder72942/RS-Tube/releases/tag/v0.2.3.
- Every history item has Delete. History only leaves the file untouched; File + history asks for permanent-deletion confirmation.
- Android deletion permission is requested when required. Cancellation or failed file deletion keeps the history entry.
- Legacy history receives unique stable IDs; tests verify that concurrent new downloads and duplicate filenames do not change the row selected for removal.
- Existing automatic in-app updates, no check button, approved logo and signing key are retained.
- Physical-device deletion permission, rotation and full update-install testing remain pending.
- The temporary publisher has been removed after successful publication. Existing version-tag release publishing remains unchanged.
