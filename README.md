# Reading Habit Coach

**Read more consistently. Remember what matters.**

A native, offline-first reading journal and book tracker for compatible Amazon Fire tablets and Android devices. Publisher: **O. Bankole** · [Support](mailto:banksmi@mail.com).

This is **0.2.0 early access**. It is a working Android application, with important production requirements still outstanding. See [release status](docs/RELEASE-STATUS.md) before distribution.

## Build and run

Requires JDK 17 and Android SDK 36. Set `sdk.dir` in a local, ignored `local.properties`, or set `ANDROID_HOME`.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.readinghabitcoach.app/.MainActivity
```

Minimum API 26. Target API 35. No Google Play Services dependency. No account required. No release-signing key is included.

## Try the app

Skip or complete the optional daily/weekly goal setup. Add a book from Library, choose its format and shelf, then start a reading session. Pause/resume, finish and save your reached page. Use Journal to keep notes. Goals and Insights use stored reading records.

Import a personal PDF or DRM-free EPUB from Library. A small original test EPUB can be generated with `powershell -File tools/create-samples.ps1`. Do not import files you are not authorised to open. Kindle libraries and protected Kindle books are not supported.

Voice commands are available as text on every device. Spoken commands and read-aloud depend on installed Android engines. Voice recognition may use a network service. No continuous background recording occurs.

Settings offers JSON export/import, session CSV, journal Markdown, text and basic PDF, dark appearance, optional reminders and confirmed deletion. Optional encrypted local backups require a passphrase of at least 12 characters, with no password recovery. Records and notes are included; imported files are excluded. Ordinary exports are unencrypted; choose their destination carefully.

## Structure

| Path | Responsibility |
|---|---|
| `app/.../data` | Room records/DAO, DataStore preferences, transfer formats, optional metadata lookup |
| `app/.../domain` | Time interval allocation, calendar-week consistency, deterministic command parsing |
| `app/.../reader` | Private document import/preflight, PDF renderer and Readium EPUB integration |
| `app/.../voice` | One-shot recognition and Android text-to-speech controls |
| `app/.../reminders` | Battery-friendly optional reminder worker |
| `app/.../ui` | Adaptive Compose screens and editing flows |
| `tools` | Localisation, sample creation and static website build |
| `website/dist` | Crawlable companion site deployed by GitHub Pages |
| `docs` | Release status, privacy, support and verification evidence |

## Website and localisation

```powershell
node tools/build-website.mjs
node tools/extract-localization.mjs
node tools/localize.mjs
node tools/localize.mjs --strict
```

The Android app includes 400 resource messages in 21 locale configurations covering Amazon Appstore/Fire languages, plus Arabic and Hindi. Choose a language in onboarding or Settings, or follow the device. See [localization notes](docs/LOCALIZATION.md) for the exact language list, regional reuse, provenance and review limits. Thirteen Amazon listing drafts are in `store-listings`. Native-speaker review and translated screenshot/device visual checks remain pending.

`tools/extract-localization.mjs` generates a candidate list for review. `tools/build-locales.mjs` and `tools/build-store-locales.mjs` regenerate checked-in resources/listings without network access. The strict check rejects missing messages, placeholder changes, leaked temporary markers and entries lacking independent-request or agent-review provenance. Optional development-only translation uses `python tools/translate-locales.py --fetch --independent`; it sends only public application copy, stops on rate limits and never runs in the APK.

For website verification and editable screenshot/icon rendering, run `npm ci`, `npx playwright install chromium`, then `npm run site:serve` in one terminal and `npm run site:verify` in another. `npm run assets:render` uses actual captures in `screenshots/captures`. `CHROMIUM_PATH` can select an already-installed compatible Chromium executable. These are development-only dependencies; the website itself is buildless HTML/CSS.

The companion website is designed for [GitHub Pages](https://lanray07.github.io/Reading-Habit-Coach/). Publication status is recorded in the verification report. [GitHub issues](https://github.com/lanray07/Reading-Habit-Coach/issues) and [email](mailto:banksmi@mail.com) are the support routes.

## Licensing

Third-party dependencies retain their own licences. Readium uses BSD-3-Clause. See [third-party notices](docs/THIRD-PARTY-NOTICES.md). The generated banner is original artwork; the icon and typographic book tiles are original project vector/code designs. Sample EPUB prose is original test content, not a publisher book or cover. No publisher cover images or reviews are bundled.
