# Reading Habit Coach 0.2.0 — localized early access

The Android interface now includes **21 locale configurations**, covering Amazon's documented Appstore and Fire language union, with Arabic and Hindi retained from the original brief.

English (UK/US/AU/CA/IN), German (DE/AT), Spanish (ES/MX), French (FR/CA), Italian, Japanese, Portuguese (BR/PT), Dutch, Polish, Swedish, Simplified Chinese, Arabic and Hindi.

- 400 resource messages covering onboarding, navigation, book/session/journal/goals/insights/settings dialogs, readers, speech status and notifications.
- Language choice in onboarding and Settings; follow the device or choose an app-only language. API 33+ integrates with Android's app-language settings.
- Reading-specific terminology, localized command examples and numeric log commands, locale-aware dates/numbers, quantity resources and Arabic RTL configuration.
- 13 Amazon store-listing draft localizations, source catalogs, cache/provenance checks and reproducible generation tools. Builds need no translation service or API key.
- Book data and private notes retain their original language. Internal IDs and backup/CSV fields remain compatible.

Validation: debug build and Android lint; 19 unit tests; four Fire API28 instrumentation tests, including every configured locale, placeholder formatting, persisted language choice, reading terminology, localized example/numeric commands, Arabic layout direction, Polish/Arabic quantities, Room/JSON and actual document import. Final localized visual/interaction checks remain pending tablet unlock; no translated device screenshots are claimed.

Translations are machine-generated drafts with agent corrections. Native-speaker review remains pending. This is a debug-signed preview, with Amazon billing and other production requirements unfinished. See [release status](https://github.com/lanray07/Reading-Habit-Coach/blob/main/docs/RELEASE-STATUS.md) and [localization notes](https://github.com/lanray07/Reading-Habit-Coach/blob/main/docs/LOCALIZATION.md).

Publisher: **O. Bankole** · Support: [banksmi@mail.com](mailto:banksmi@mail.com).
