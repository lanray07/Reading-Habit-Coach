# Amazon submission draft — 20 October 2026

The publisher explicitly requested final submission on 20 October 2026. Amazon confirmed **Submitted** and **App Submission Successful** on that date. The app is awaiting review, not confirmed approved or live. The Console estimates 25 October 2026 at 08:30 AM PDT (15:30 Europe/London); this is an estimate, not a guarantee.

- App ID: `amzn1.devportal.mobileapp.477419a9726d4d4fb4775f40b25f81ce`
- Release ID: `amzn1.devportal.apprelease.47351ee029e04e2ca79302e232fb20f0`
- Title: Reading Habit Coach - Book Tracker (Amazon rejected the en dash).
- SKU: `reading-habit-coach`; Books & Comics > Readers & Players.
- Support: banksmi@mail.com; GitHub Pages support and privacy URLs.
- Pricing: free; all available countries/regions.
- All 15 description locales offered in this Console were populated and marked Complete: en-US, de-DE, en-AU, en-GB, en-IN, es-ES, es-MX, fr-FR, it-IT, ja-JP, nl-NL, pl-PL, pt-BR, pt-PT, sv-SE. UK English reuses English source copy; Mexican Spanish reuses Spanish source copy. Native review remains pending.
- Both transparent PNG icons (512 and 114) and seven real English screenshot compositions were uploaded. Amazon marks English multimedia Complete and uses it as the fallback for other locales.
- Screenshot sequence: Home, Library, Sessions, Goals, Journal, Reader, Voice. Sources are `screenshots/compositions/01-home.png` through `07-voice.png` (Sessions filename is `03-sessions.png`). These accurately retain the 0.1.0 early-access capture label and original QA sample data. They are not localized 0.2.0 device captures.
- The existing `08-insights.png` composition was excluded: its underlying capture shows Goals, not Insights. Do not upload it until recaptured.
- Description, bullets and keywords use concrete feature names, with no ranking promises, fake ratings or unimplemented feature claims.
- Privacy questionnaire saved for review: optional in-app search and system speech transfer to third parties for app functionality; publisher does not collect those records. No account creation. Privacy/deletion instructions point to the published privacy page.
- Content questionnaire marked Complete: no bundled violence, drugs, nudity, sex, intolerance, profanity, advertisements, gambling or location detection. Academic attenuation: no. User-generated content: yes (local notes and imported documents); there is no user-to-user messaging.
- Practical testing instructions from `AMAZON-REVIEWER-INSTRUCTIONS.txt` were populated again immediately before final submission; no credentials are required. The final review form does not retain this optional field across navigation before submission.
- Production-signed `reading-habit-coach-0.2.0-release.apk` (version code 2) uploaded and accepted by Amazon validation. See `PRODUCTION-APK.json` for its verified digest, signer and local checks. Amazon binary ID: `MV3T8AZKQZCR5`.
- Amazon DRM wrapping: No (free, offline app with no active purchases or DRM SDK).
- Device selection saved: 12 compatible Fire tablets; zero Fire TV or automotive devices. The selected tablet list includes the tested Fire HD 10 (2021) KFTRWI. The Console excludes older unsupported models and newer Alexa/Amazon Kids tablets. This build targets API 35; Fire OS 16 requires target API 36, so support for those newer devices needs a future target-36 build and platform testing. Source: https://developer.amazon.com/docs/app-submission/device-filtering-and-compatibility.html#fire-os-and-android-api-levels

## Remaining before release

1. Publisher approved ages 16 and above; Amazon target audience saved as 16–17 and 18+ (younger groups excluded).
2. APK upload and compatible tablet targeting are complete. Publisher-owned key custody and portable backup instructions are in `RELEASE-SIGNING.md`; the private key/password were not uploaded or committed. No production APK was installed over the existing debug-signed tablet app.
3. Publisher explicitly confirmed the export-compliance declaration, including encryption controls, on 20 October 2026. The Amazon checkbox was selected and the Console displayed "Last saved". No final submission was authorized or performed in that confirmation.
4. Complete the production gaps in `RELEASE-STATUS.md`, including privacy/legal review and native/localized visual QA. Localized screenshots and optional promotional artwork/video are not uploaded.

The export-compliance declaration was checked with publisher confirmation. Final submission was explicitly authorized and completed. The Console's submission history states "App Submission Successful" and shows no new updates requiring attention. Approval and publication remain Amazon's decision.
