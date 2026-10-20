package com.readinghabitcoach.app.domain

import java.time.*
import java.time.temporal.TemporalAdjusters

data class Span(val start: Long, val end: Long) { val duration get() = (end - start).coerceAtLeast(0) }
fun encodeSpans(spans: List<Span>) = spans.joinToString(";") { "${it.start},${it.end}" }
fun decodeSpans(value: String): List<Span> = value.split(';').mapNotNull { part ->
    val pair = part.split(','); if (pair.size != 2) null else {
        val a = pair[0].toLongOrNull(); val b = pair[1].toLongOrNull()
        if (a == null || b == null || b < a) null else Span(a, b)
    }
}
fun dailyDurations(spans: List<Span>, zone: ZoneId): Map<LocalDate, Long> {
    val result = mutableMapOf<LocalDate, Long>()
    spans.forEach { span ->
        var cursor = Instant.ofEpochMilli(span.start)
        val end = Instant.ofEpochMilli(span.end)
        while (cursor < end) {
            val date = cursor.atZone(zone).toLocalDate()
            val boundary = date.plusDays(1).atStartOfDay(zone).toInstant()
            val stop = minOf(boundary, end)
            result[date] = (result[date] ?: 0) + Duration.between(cursor, stop).toMillis()
            cursor = stop
        }
    }
    return result
}
fun weekOf(date: LocalDate, firstDay: DayOfWeek = DayOfWeek.MONDAY): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(firstDay))
data class WeeklyRule(val effectiveWeek: LocalDate, val days: Int, val paused: Boolean = false)
data class Streak(val current: Int, val best: Int, val currentWeekDays: Int, val target: Int)

/** The current partial week is never treated as a failed or confirmed week. Pauses preserve history. */
fun consistency(readingDays: Set<LocalDate>, rules: List<WeeklyRule>, today: LocalDate, firstDay: DayOfWeek = DayOfWeek.MONDAY): Streak {
    val currentWeek = weekOf(today, firstDay)
    val ordered = rules.sortedBy { it.effectiveWeek }
    fun rule(w: LocalDate) = ordered.lastOrNull { it.effectiveWeek <= w }
    val target = rule(currentWeek)?.days ?: 4
    val thisWeek = readingDays.count { it >= currentWeek && it <= today }
    val first = ordered.firstOrNull()?.effectiveWeek ?: return Streak(0, 0, thisWeek, target)
    var w = weekOf(first, firstDay); var current = 0; var best = 0
    while (w < currentWeek) {
        val r = rule(w)
        if (r != null && !r.paused) {
            val finish = w.plusWeeks(1)
            val days = readingDays.count { it >= w && it < finish }
            current = if (days >= r.days) current + 1 else 0
            best = maxOf(best, current)
        }
        w = w.plusWeeks(1)
    }
    return Streak(current, best, thisWeek, target)
}

sealed interface VoiceCommand {
    data object Start : VoiceCommand; data object Pause : VoiceCommand; data object Resume : VoiceCommand
    data object Finish : VoiceCommand; data object Note : VoiceCommand; data object Week : VoiceCommand
    data object Goal : VoiceCommand; data object Open : VoiceCommand; data object LatestNote : VoiceCommand
    data object Next : VoiceCommand; data object Bookmark : VoiceCommand
    data class Log(val minutes: Int) : VoiceCommand
    data object Unknown : VoiceCommand
}
fun parseCommand(raw: String): VoiceCommand {
    val s = raw.lowercase(java.util.Locale.ROOT).replace(Regex("[.!?]"), "").trim()
    val dictionary = mapOf("start reading" to VoiceCommand.Start, "pause my reading session" to VoiceCommand.Pause,
        "pause reading" to VoiceCommand.Pause, "resume reading" to VoiceCommand.Resume,
        "finish my session" to VoiceCommand.Finish, "finish reading" to VoiceCommand.Finish,
        "add a note to this book" to VoiceCommand.Note, "how much have i read this week" to VoiceCommand.Week,
        "what's my reading goal" to VoiceCommand.Goal, "open my current book" to VoiceCommand.Open,
        "read my latest note" to VoiceCommand.LatestNote, "next page" to VoiceCommand.Next, "add a bookmark" to VoiceCommand.Bookmark)
    dictionary[s]?.let { return it }
    val m = Regex("log (.+) minutes?( of reading)?").matchEntire(s)
    if (m != null) {
        val words = mapOf("one" to 1, "five" to 5, "ten" to 10, "fifteen" to 15, "twenty" to 20, "thirty" to 30, "forty five" to 45, "sixty" to 60)
        val n = m.groupValues[1].toIntOrNull() ?: words[m.groupValues[1]]
        if (n != null && n in 1..1440) return VoiceCommand.Log(n)
    }
    return VoiceCommand.Unknown
}
