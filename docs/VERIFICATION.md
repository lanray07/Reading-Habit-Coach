# Verification record

20 October 2026 · English UK · early-access 0.1.0

## 0.2.0 localization update

- 400 messages across 21 explicit Android locale configurations; complete key/placeholder checks and provenance checks pass. Thirteen Amazon listing drafts pass field completeness and length checks. Native-speaker review remains pending.
- Final 0.2.0 debug build, instrumentation APK build, 19 JVM tests and Android lint pass locally (zero lint errors; 80 dependency/API/style warnings). No MissingTranslation, quantity or string-format warnings were reported.
- Four tests passed on the connected **KFTRWI/API28** tablet: resource resolution and argument formatting for every configured locale, persistent app-language selection, expected reading terminology, localized example and numeric log commands, Arabic RTL and Arabic/Polish quantity cases, plus the two Room/JSON and PDF/EPUB tests.
- Device UI interaction and visual screenshots of the final localized build remain pending PIN unlock. The tests above do not require bypassing or interacting with the lock screen. English screenshots below are earlier 0.1.0 evidence, not proof of localized 0.2.0 layout or visual acceptance.
- Translation provenance distinguishes independent single-message requests and local agent review/corrections. Rate limiting stopped the online pass; it was not bypassed. All remaining source/catalog pairs were inspected locally and corrected, with digest records. No native-speaker approval is claimed.

## Build and automated checks

- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest` and `:app:lintDebug` passed locally. Lint retains informational dependency/version/style warnings; no lint errors are waived with a baseline.
- Nineteen JVM tests cover midnight/DST/timezone allocation, excluded paused time, completed-week streaks, goal changes, pauses, backdating, week starts, structured voice commands, archive path/encryption/compression checks and authenticated backup encryption.
- Three instrumentation tests passed on **Amazon KFTRWI, API 28**: Room/JSON merge, actual PDF/EPUB import and rendering preflight, and the complete interactive workflow.
- Final-build rerun: the Room/JSON and PDF/EPUB tests passed. The interactive rerun reached the tablet's PIN lock screen before the home assertion; it remains pending user unlock. The earlier complete workflow pass and captures are retained as earlier-build evidence, not a claimed final-build UI pass. The latest test runner adds failure diagnostics and keeps its own resumed activities awake without changing global device settings.
- Manual logs carry an explicit source flag and date-only semantics, preserved by JSON/CSV export and restore. A Room 1→2 migration adds the flag without deleting existing records. Timer sessions retain measured active intervals.
- Interactive path: start → pause → resume → finish/save → library → create/save journal entry → goals → insights → voice fallback → reopen → EPUB render → bookmark → search → portrait home/library. Test-owned original sample content is used. The test runner requires `allowPhysicalUi=true` before interacting with a physical device.
- Real microphone recognition, installed TTS voices/audio output, notification delivery timing, encrypted export through a third-party document provider, TalkBack, large font scales and RTL remain acceptance-test work.

## Actual screen captures

Raw Fire captures are in `screenshots/captures`: eight 1920×1200 landscape views and 1200×1920 portrait home/library views. Editable HTML compositions in `screenshots/compositions` use these actual images. Icon SVG and 114/512/1024 PNG exports are in `branding`.

The final capture helper lets closing dialogs settle before taking a screenshot. Earlier captures caught the journal dialog's dimming animation; those were replaced. EPUB metadata import was improved after visual review to use the publication's title and authors rather than its file name.

No phone-device screenshot or translated screenshot is claimed. No AI-generated UI is presented as an application screenshot.

## Visual inspection

Generated source artwork, desktop/mobile website renders, native Fire home/session/journal/reader/portrait captures and a composed screenshot were inspected with `view_image`.

| Comparison | Evidence and result |
|---|---|
| Palette | Warm ivory/parchment surfaces, forest primary controls and muted bronze accents follow the supplied brief. |
| Typography | Serif reading headings and readable sans-serif navigation; exact primary action is “Start Reading Session”. |
| Layout | Landscape navigation panel and spacious columns; portrait bottom navigation. Primary content remains scrollable without horizontal clipping in checked screens. |
| Assets | Original generated reading banner and original vector book mark. Book tiles and sample prose are project-authored, not publisher covers. |
| Touch/feedback | Material controls use at least 48dp primary target sizing; saved sessions/notes update real Room-backed screens. Formal accessibility audit remains pending. |
| Reader | Real Readium-rendered original EPUB text, chapter controls, saved location and successful search/bookmark path. PDF limitations are explicitly labelled. |

## Website

Thirteen crawlable pages are built from `tools/build-website.mjs`. All routes respond successfully. Browser checks at 1440×1000 and 390×844 found no tested-page horizontal overflow or page errors. The reader page explicitly discloses PDF limitations. A mobile bookshelf overflow was repaired.

The in-app browser was used first. Its resized captures were clipped and click dispatch timed out, so Playwright Chromium was used for repeatable responsive checks and exported screenshots. Results are in `screenshots/website-verification.json`.

GitHub Pages is public at <https://lanray07.github.io/Reading-Habit-Coach/> and returned HTTP 200 with the expected content. The initial Pages run preceded repository Pages enablement; rerunning after enabling it succeeded. Android CI initially requested the obsolete SDK `tools` package; selecting explicit current SDK packages fixed the workflow. Both workflows passed after those setup corrections.

## Remaining release scope

See `RELEASE-STATUS.md`. Passing these checks does not establish full production readiness, completed localisation, configured Amazon billing, trademark availability or store acceptance.

Temporary emulator files remain in the ignored `.qa` directory and the task-created AVD location because automatic approval review rejected cleanup with “blocked by policy”. They are excluded from the repository and release assets.
