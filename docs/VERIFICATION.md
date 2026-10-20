# Verification record

20 October 2026 · English UK · early-access 0.1.0

## Build and automated checks

- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest` and `:app:lintDebug` passed locally. Lint retains informational dependency/version/style warnings; no lint errors are waived with a baseline.
- Nineteen JVM tests cover midnight/DST/timezone allocation, excluded paused time, completed-week streaks, goal changes, pauses, backdating, week starts, structured voice commands, archive path/encryption/compression checks and authenticated backup encryption.
- Three instrumentation tests passed on **Amazon KFTRWI, API 28**: Room/JSON merge, actual PDF/EPUB import and rendering preflight, and the complete interactive workflow.
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
