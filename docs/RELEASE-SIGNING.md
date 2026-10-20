# Publisher release signing

Run `powershell -NoProfile -File tools/build-release.ps1` from the repository on Windows. It builds the release variant, runs release unit tests and lint, aligns the APK, signs it, then verifies its signatures and alignment. The release variant remains unminified for this early-access build; shrinking is not claimed as verified.

Output: `.release/production/reading-habit-coach-0.2.0-release.apk` and `SHA256SUMS.txt` (ignored by Git).

The dedicated RSA-4096 publisher key uses SHA256withRSA and alias `reading-habit-coach`, separate from Android's debug key. It is retained outside the repository at `%LOCALAPPDATA%\ReadingHabitCoach\signing\reading-habit-coach-release.p12`. Its random password is stored in `password.dpapi`, encrypted for the current Windows user using Windows DPAPI. The script restricts directory access to that user and SYSTEM, never writes the password to build output or command-line arguments, and removes its temporary password environment variable after signing. The script reuses an existing key and refuses an incomplete key/password pair.

Keep the private key and password out of GitHub, release assets and Amazon uploads. Only the signed APK is uploaded. Preserve the key for future updates. Before public publication, the publisher should securely escrow the keystore and its password in their own protected backup/password manager. Copying `password.dpapi` alone to another Windows account or computer does not provide a portable recovery method.

The release APK cannot replace a debug-signed installation with the same package ID. Do not uninstall or clear a user's existing installation to force a signing change: export their records and documents first, and have the user choose migration. No such uninstall is part of the release build/upload.

A release signature identifies the build publisher; it does not establish native translation approval, complete the original feature brief, or certify legal/export compliance. See `RELEASE-STATUS.md` and the Amazon submission record for those limits.
