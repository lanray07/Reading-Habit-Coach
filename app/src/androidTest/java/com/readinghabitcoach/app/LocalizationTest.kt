package com.readinghabitcoach.app

import android.content.res.Configuration
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.readinghabitcoach.app.domain.VoiceCommand
import com.readinghabitcoach.app.domain.parseCommand
import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.i18n.SourceMessages
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun everyLocaleResolvesAllMessagesAndFormatsArguments() {
        for (tag in L10n.languages.keys.filter { it.isNotBlank() }) {
            val locale = Locale.forLanguageTag(tag)
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale); setLayoutDirection(locale) })
            for ((id, source) in SourceMessages.english) {
                val text = localized.getString(id)
                assertTrue("$tag has a blank translation for $source", text.isNotBlank())
                assertFalse("$tag leaked a translation marker", text.contains("ZXQ"))
                if (source.contains("%1\$s")) assertTrue(localized.getString(id, "7", "8", "9", "10", "11").isNotBlank())
            }
            if (tag == "ar") assertEquals(View.LAYOUT_DIRECTION_RTL, localized.resources.configuration.layoutDirection)
            val books = localized.resources.getQuantityString(R.plurals.book_count, 2, "2")
            if (locale.language == "pl") { assertEquals("2 książki", books); assertEquals("5 książek", localized.resources.getQuantityString(R.plurals.book_count, 5, "5")) }
            if (locale.language == "ar") assertEquals("كتابان", books)
        }
    }
    @Test fun languageChoicePersistsAndLocalizedCommandsKeepStableBehavior() {
        val original = L10n.selected(context); val originalDefault = Locale.getDefault()
        val libraryLabels = mapOf("de" to "Bibliothek", "es" to "Biblioteca", "fr" to "Bibliothèque", "it" to "Libreria", "ja" to "ライブラリ", "pt" to "Biblioteca", "nl" to "Bibliotheek", "pl" to "Biblioteka", "sv" to "Bibliotek", "zh" to "书库", "ar" to "المكتبة", "hi" to "पुस्तकालय")
        try {
            for (tag in L10n.languages.keys.filter { it.isNotBlank() }) {
                L10n.select(context, tag)
                assertEquals(tag, L10n.selected(context))
                assertEquals(Locale.forLanguageTag(tag).language, L10n.wrap(context).resources.configuration.locales[0].language)
                libraryLabels[Locale.forLanguageTag(tag).language]?.let { assertEquals("$tag reading terminology", it, L10n.label("Library")) }
                assertEquals("$tag start command", VoiceCommand.Start, parseCommand(L10n.canonicalCommand(L10n.label("Start reading"))))
                assertEquals("$tag log example", VoiceCommand.Log(20), parseCommand(L10n.canonicalCommand(L10n.label("Log twenty minutes of reading"))))
                assertEquals("$tag goal command", VoiceCommand.Goal, parseCommand(L10n.canonicalCommand(L10n.label("What’s my reading goal?"))))
                val numericCommand = L10n.label("Log %1\$s minutes of reading").replace("%1\$s", L10n.number(37))
                assertEquals("$tag numeric log command", VoiceCommand.Log(37), parseCommand(L10n.canonicalCommand(numericCommand)))
            }
        } finally { L10n.select(context, original); Locale.setDefault(originalDefault) }
    }
}
