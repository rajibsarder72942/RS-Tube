# Build status — 2026-11-16

Prepared version: 0.2.1 (code 3), Android 10+, player-free MP4/MP3 downloader with in-app updates and the approved tilted red arrow on yellow logo.

- XML resource and manifest parsing: passed.
- GitHub Actions workflow YAML parsing: passed.
- Previous 0.1.0 compilation, JUnit and lint: passed in GitHub Actions run [43832763970](https://github.com/rajibsarder72942/RS-Tube/actions/runs/43832763970).
- 0.2.0 compilation, JUnit and lint: passed in [GitHub Actions run 43835700107](https://github.com/rajibsarder72942/RS-Tube/actions/runs/43835700107), testing commit `7e7c46de5dd3503f03b85c6a4c8af0d87ad3e35c`.
- The new APK was re-signed locally with the private stable RS Tube key and cryptographically verified using Android SDK apksigner. The certificate was checked against the saved keystore. No private key was committed or published.
- The owner has configured the signing secrets. The 0.2.1 build will validate them before publishing this release.
- Update release manifest generation, test-build exclusion and XML/YAML checks: passed locally.
- Physical-device tests and live YouTube/Facebook/direct-link downloads: not run.
- Repository access: verified. The Raja GitHub connection now includes `RS-Tube`.
- Source publication and APK build: completed. Download the `RS-Tube-Android` artifact from the successful run above.
- Downloaded artifact and packaged APK/source SHA-256 checksums: verified.
- APK manifest, native ABI entries and APK v2 signing-block presence: inspected. This is not a cryptographic signature verification or an on-device installation test.

In-app update installation must still be exercised on a physical Android device with two successively versioned APKs signed by the same key. See UPDATES.md. A successful compile alone does not verify live extraction or an Android installer round trip.
