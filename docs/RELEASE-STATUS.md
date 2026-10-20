# Release status — 0.1.0 early access

This is a working native Android early-access build, not a complete implementation of the supplied production brief. Do not submit it to the Amazon Appstore as production-ready.

Publisher: **O. Bankole**. Support: **banksmi@mail.com**.

## Available

- Kotlin, Compose Material 3, Room, DataStore, coroutines and Flow; no account or Google Play Services dependency.
- Manual book entry/editing, five shelves, formats, genres/tags, ISBN, personal ratings, search/filter, optional Open Library metadata lookup.
- Start/pause/resume/finish sessions; process-persistent active session, manual logs, notes, page progress; audiobook minutes remain time.
- Daily minute goals, weekly reading-day intentions, annual book goal; completed-calendar-week streaks, best streak, paused policies and next-week goal changes.
- Private notes with seven types, book/location links, tags, favourites, search and editing.
- Real stored-data insights: reading time/pages, sessions, average session, logged pace, weekly bars, 12-week calendar, format time and completed books.
- Private import of PDF and EPUB up to 100 MB, bounded archive preflight, safe EPUB paths, encrypted EPUB rejection.
- Readium 3.1 EPUB navigation, font size/spacing, themes, chapters, saved positions, location bookmarks, selected-text quotes with highlight decorations, text search (up to 100 results) and selected-text/chapter speech (up to 50,000 characters per request).
- PdfRenderer page view, navigation/jump, zoom, page-position persistence and journal bookmarks. PDF image renderer has no text extraction, search, text highlights, OCR or read-aloud.
- Runtime speech-recognition capability/permission checks, structured English commands and touch/text fallback; note speech, sentence-level pause/resume, rate and installed voice selection.
- Approximate daily WorkManager reminders, notification permission handling and fixed 22:00–07:00 quiet hours.
- JSON backup/merge, session CSV, journal Markdown/text/basic PDF; explicit deletion confirmation. Documents are excluded from exports. Optional passphrase-protected local backup uses AES-256-GCM with PBKDF2-HMAC-SHA256 (210,000 iterations), random salt/IV and authenticated headers. Ordinary exports remain unencrypted.
- Original generated banner and original vector icon source/adaptive/monochrome layers. Typographic book tiles are not publisher covers.
- Thirteen-page static marketing site, canonical URLs, sitemap, robots, English language annotations, accurate SoftwareApplication data, privacy/support pages and GitHub Pages workflow.

## Still required for the full brief / store release

1. Real Amazon monthly/annual/lifetime product IDs, billing SDK integration, server receipt verification, signed entitlement cache, restore/expiry/cancellation/failure testing. This build takes no payment and grants no fabricated purchases or trials.
2. Finish string extraction, supply an approved development-time translation provider and review all target locales, plural rules, RTL layouts, voice dictionaries, listing copy and screenshots. Current interface is English; localization/report.json reports gaps.
3. Full EPUB interaction/accessibility verification, automatic narration across chapters, narrated-position synchronisation and richer text-linked annotation UX. Chapter speech is bounded and starts at the current locator; it does not automatically advance chapters.
4. Optional PDF text extraction/search/highlight engine; PDF OCR is not implemented or claimed.
5. User cover-image picker, import of third-party metadata files, tested opt-in cloud provider (none currently configured) and independent cryptographic review of local backup handling.
6. Daily page goals, monthly goals/custom challenges, reminder windows/snooze/weekly schedules, date-range analytics, genre chart and yearly recap.
7. Expanded onboarding formats/main goal/language selection and language switching; full TalkBack, font scale, reduced motion, RTL and device-matrix accessibility verification.
8. Signed production APK, release signing ownership, shrinker validation, dependency/security review and Fire OS/device compatibility matrix. Current minimum SDK is 26, excluding older Fire OS 5/API22 devices.
9. Name/trademark availability, final legal/privacy review and Amazon Console submission metadata review. No name reservation or store acceptance is implied.
10. Phone variants and translations of the eight English Fire-tablet screenshot compositions. Raw 1920×1200 Fire captures and editable marketing compositions are included. Screenshots are based on original QA sample content; only implemented features may be advertised.
11. The complete illustration pack requested in the master brief: distinct empty-library, notes, achievements, goals, Premium, promotional banner and light/dark artwork variants. Current original assets are the book-mark icon family and reading-moment illustration used in the app and website.

## External references checked 20 October 2026

- [Amazon submission overview](https://developer.amazon.com/docs/app-submission/understanding-submission.html)
- [Amazon asset guidelines](https://developer.amazon.com/docs/app-submission/asset-guidelines.html)
- [Amazon submission FAQ](https://developer.amazon.com/docs/app-submission/faq-submission.html)
- [Amazon IAP implementation](https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html)
- [Readium 3.1.0 source and integration guidance](https://github.com/readium/kotlin-toolkit/tree/3.1.0)

Keep marketing aligned with this document. A future feature in the master brief is not a released feature.
