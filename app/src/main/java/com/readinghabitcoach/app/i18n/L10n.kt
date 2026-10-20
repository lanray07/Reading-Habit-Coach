package com.readinghabitcoach.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import java.text.NumberFormat
import java.util.Locale

/** Locale boundary shared by Compose, readers, notifications and error messages.
 * Database values and user content never pass through label().
 */
object L10n {
    private var application: Context? = null
    private var cachedTag = ""
    private var cachedContext: Context? = null
    val languages = linkedMapOf(
        "" to "System", "en-GB" to "English (UK)", "en-US" to "English (US)",
        "en-AU" to "English (Australia)", "en-CA" to "English (Canada)", "en-IN" to "English (India)",
        "de-DE" to "Deutsch (Deutschland)", "de-AT" to "Deutsch (Österreich)",
        "es-ES" to "Español (España)", "es-MX" to "Español (México)",
        "fr-FR" to "Français (France)", "fr-CA" to "Français (Canada)", "it-IT" to "Italiano",
        "ja-JP" to "日本語", "pt-BR" to "Português (Brasil)", "pt-PT" to "Português (Portugal)",
        "nl-NL" to "Nederlands", "pl-PL" to "Polski", "sv-SE" to "Svenska",
        "zh-CN" to "简体中文", "ar" to "العربية", "hi-IN" to "हिन्दी"
    )
    fun bind(context: Context) { application = context.applicationContext }
    fun selected(context: Context): String = if (android.os.Build.VERSION.SDK_INT >= 33)
        context.getSystemService(android.app.LocaleManager::class.java).applicationLocales.let { if (it.isEmpty) "" else it[0].toLanguageTag() }
        else context.getSharedPreferences("app_locale", Context.MODE_PRIVATE).getString("tag", "") ?: ""
    fun locale(): Locale {
        val tag = application?.let(::selected).orEmpty()
        return if (tag.isBlank()) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(tag)
    }
    fun wrap(context: Context): Context {
        val tag = selected(context)
        val locale = if (tag.isBlank()) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(tag)
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale); setLayoutDirection(locale) })
    }
    fun select(context: Context, tag: String) {
        require(languages.containsKey(tag))
        context.getSharedPreferences("app_locale", Context.MODE_PRIVATE).edit().putString("tag", tag).commit()
        if (android.os.Build.VERSION.SDK_INT >= 33) context.getSystemService(android.app.LocaleManager::class.java).applicationLocales = android.os.LocaleList.forLanguageTags(tag)
        cachedContext = null
        Locale.setDefault(locale())
    }
    private fun context(): Context? {
        val base = application ?: return null
        val tag = locale().toLanguageTag()
        if (cachedContext == null || cachedTag != tag) { cachedContext = wrap(base); cachedTag = tag }
        return cachedContext
    }
    fun text(@StringRes id: Int, vararg args: Any): String {
        val values = args.map { if (it is Number) NumberFormat.getInstance(if(application == null) Locale.UK else locale()).format(it) else it }.toTypedArray()
        val resources = context()?.resources
        return if (values.isEmpty()) resources?.getString(id) ?: SourceMessages.english.getValue(id)
        else resources?.getString(id, *values) ?: String.format(Locale.UK, SourceMessages.english.getValue(id), *values)
    }
    fun number(value: Number): String = NumberFormat.getInstance(if (application == null) Locale.UK else locale()).format(value)
    fun label(stableId: String): String = SourceMessages.labels[stableId]?.let { text(it) } ?: stableId.toLongOrNull()?.let(::number) ?: stableId
    fun languageName(tag: String): String = languages[tag] ?: Locale.forLanguageTag(tag).getDisplayName(Locale.forLanguageTag(tag))
    fun quantity(name: String, count: Int): String {
        val context = context() ?: return "$count $name"
        val id = when (name) {
            "book_count" -> com.readinghabitcoach.app.R.plurals.book_count
            "minute_count" -> com.readinghabitcoach.app.R.plurals.minute_count
            "page_count" -> com.readinghabitcoach.app.R.plurals.page_count
            "session_count" -> com.readinghabitcoach.app.R.plurals.session_count
            "star_count" -> com.readinghabitcoach.app.R.plurals.star_count
            else -> error("Unknown quantity resource: $name")
        }
        return context.resources.getQuantityString(id, count, NumberFormat.getInstance(locale()).format(count))
    }
    /** Accept localized example commands, preserving the English parser's stable command IDs. */
    fun canonicalCommand(input: String): String {
        fun normal(s: String) = s.trim().lowercase(locale()).replace('’', '\'').trimEnd('?', '.', '؟', '。')
        val commands = listOf("Start reading", "Pause my reading session", "Resume reading", "Finish my session", "Log twenty minutes of reading", "Add a note to this book", "How much have I read this week?", "What’s my reading goal?", "Open my current book", "Read my latest note", "Next page", "Add a bookmark")
        commands.firstOrNull { normal(label(it)) == normal(input) }?.let { return it.replace('’', '\'') }
        val template = label("Log %1\$s minutes of reading").split("%1\$s")
        if (template.size == 2) {
            val match = Regex(Regex.escape(normal(template[0])) + "\\s*(.+?)\\s*" + Regex.escape(normal(template[1])), RegexOption.IGNORE_CASE).matchEntire(normal(input))
            val number = match?.groupValues?.get(1)?.let { value ->
                val position = java.text.ParsePosition(0)
                NumberFormat.getIntegerInstance(locale()).parse(value, position)?.takeIf { position.index == value.length }?.toInt()
            }
            if (number != null && number in 1..1440) return "log $number minutes of reading"
        }
        return input
    }
}
