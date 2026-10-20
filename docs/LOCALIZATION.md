# Localization — 0.2.0

The Android app includes the union of Amazon's documented Appstore listing and Fire device languages, plus Arabic and Hindi from the original brief. Source: [Amazon international distribution and localization](https://developer.amazon.com/docs/app-submission/distribute-your-app-internationally.html), checked 20 October 2026.

## Coverage

21 explicit locale configurations:

- English: UK, US, Australia, Canada, India
- German: Germany, Austria
- Spanish: Spain, Mexico
- French: France, Canada
- Italian, Japanese, Dutch, Polish, Swedish
- Portuguese: Brazil, Portugal
- Simplified Chinese, Arabic, Hindi

UK English is the default resource fallback. Regional English, French, German and Spanish configurations share their language's UI copy while using the selected locale for numbers and dates. Brazilian and European Portuguese have separate catalogs and terminology. Chinese uses the Android `zh-Hans` script qualifier.

Amazon's store-listing choices differ from device languages. The 13 listing drafts in `store-listings` cover Dutch, English (AU/IN/US), French, German, Italian, Japanese, Polish, Portuguese (BR/PT), Spanish and Swedish. Chinese, Arabic and Hindi app resources do not imply that Amazon offers matching store-listing fields.

## Runtime behavior

Choose a language during onboarding or in Settings, or follow the device language. The choice survives restarting the app. API 33+ uses Android's per-app LocaleManager, including the system app-language setting. Earlier Android/Fire versions keep an app-only preference and recreate the activity with the matching configuration. The EPUB activity, notifications, status messages, controls and accessibility labels use the same locale boundary.

Room shelves, format values, note types, navigation IDs, JSON keys and CSV protocol fields remain stable. Their displayed labels are localized explicitly. User book titles, authors, notes, imported documents and typed content are not sent for translation or automatically translated. Locale changes do not migrate or rewrite reading records.

Dates and numeric summaries use the selected locale. Android plural resources cover book, minute, page, session and star counts, including Arabic zero/one/two/few/many and Polish one/few/many. Arabic uses Android/Compose right-to-left layout. Native language names remain in the language picker so users can recover from an accidental selection.

Localized command examples map back to stable command IDs; the example for logging twenty minutes works in every language. Localized numeric log-command templates accept numbers through NumberFormat. This is a deterministic phrase parser, not open-ended natural-language understanding. Device recognition and offline TTS voice availability still vary.

## Reproducible development workflow

```powershell
node tools/build-locales.mjs
node tools/build-store-locales.mjs
node tools/localize.mjs --strict
```

Normal builds read checked-in catalogs and never access a translation provider. The resource generator checks completeness, independent-request or agent-review provenance, placeholders and leaked temporary markers. CI regenerates resources and rejects drift. `tools/extract-localization.mjs` produces a candidate list for review; stable internal keys must not be translated. It refuses to reapply an extraction selection to already-extracted source.

To generate missing draft translations of public application copy during development:

```powershell
python tools/translate-locales.py --fetch --independent
python tools/translate-locales.py --fetch --independent --store
```

This optional tool uses Google's public web translation endpoint, a best-effort endpoint without a contracted Cloud API SLA. It sends only checked-in application/listing copy. It does not process user books or notes, requires no API key and is not included in the APK. Failures stop generation; no English replacement is silently substituted. HTTP 429 stops the run rather than rotating endpoints. New requests contain one source message; bounded request groups retain successful results on failure.

Bulk drafts contained shifted labels. German, Spanish, French and Italian were independently requested; the remaining catalogs received a local source/translation review with explicit corrections after the endpoint rate-limited the independent pass. `agent-review.json` binds inspected source/catalog pairs to SHA-256 values and is explicitly agent review, not native-speaker approval. The reading terminology glossary and plural table are applied separately. Production linguistic review is still required. Listing files remain draft machine translations.

## Human review

Translations are drafts, not certified native-speaker translations. `localization/terminology.json` supplies reading-specific navigation, shelves, formats and note types; `plural-forms.json` supplies context-specific plural forms. Review the remaining copy with a native speaker before production submission, especially deletion/backup instructions, accessibility labels, speech phrases, Portuguese regional usage and store listing text.

For each locale, review onboarding, library/book editor, session/finish/manual log, goals, journal/note editor, insights, settings, backup/deletion dialogs and both reader surfaces. Check long text, maximum font scale, RTL navigation and actual device voices. Record reviewer/date/changes in a pull request. Localized screenshots must be captured from the real localized build; English captures must not be presented as translated screenshots.
