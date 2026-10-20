package com.readinghabitcoach.app

import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.R

import android.app.Application
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.readinghabitcoach.app.data.*
import com.readinghabitcoach.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*

class CoachViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CoachApplication
    val dao = app.database.dao()
    val books = dao.books().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val sessions = dao.sessions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val notes = dao.notes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val policies = dao.policies().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val active = dao.active().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings = app.preferences.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val ready = MutableStateFlow(false)
    val tick = MutableStateFlow(System.currentTimeMillis())
    val message = MutableStateFlow<String?>(null)
    private val timerLock = Mutex()
    private fun boot() = AndroidSettings.Global.getInt(app.contentResolver, AndroidSettings.Global.BOOT_COUNT, 0)
    init {
        viewModelScope.launch { app.preferences.flow.first(); ready.value = true }
        viewModelScope.launch {
            dao.active().collect { a ->
                if (a?.anchorEpoch != null && (a.bootCount != boot() || a.anchorElapsed > SystemClock.elapsedRealtime()))
                    dao.saveActive(a.copy(anchorEpoch = null)) // Never count powered-off time as reading.
            }
        }
        viewModelScope.launch { while (isActive) { tick.value = System.currentTimeMillis(); delay(1000) } }
    }
    fun saveBook(b: Book) = viewModelScope.launch { dao.saveBook(b) }
    fun saveNote(n: JournalNote) = viewModelScope.launch { dao.saveNote(n.copy(updatedAt = System.currentTimeMillis())) }
    fun deleteNote(n: JournalNote) = viewModelScope.launch { dao.deleteNote(n.id) }
    fun updateSettings(s: Settings) = viewModelScope.launch { app.preferences.save(s) }
    fun onboarding(s: Settings) = viewModelScope.launch {
        val week = weekOf(LocalDate.now(), DayOfWeek.of(s.weekStart))
        dao.savePolicy(GoalPolicy(week.toEpochDay(), s.weeklyDays))
        app.preferences.save(s.copy(onboarded = true))
    }
    fun changeGoal(days: Int, paused: Boolean) = viewModelScope.launch {
        // Changes apply next week, leaving this week's agreement and all history intact.
        val week = weekOf(LocalDate.now(), DayOfWeek.of(settings.value.weekStart))
        if (policies.value.isEmpty()) dao.savePolicy(GoalPolicy(week.toEpochDay(), days, paused))
        else dao.savePolicy(GoalPolicy(week.plusWeeks(1).toEpochDay(), days, paused))
        app.preferences.save(settings.value.copy(weeklyDays = days))
        message.value = L10n.text(R.string.msg_a047752300e1)
    }
    fun start(book: Book) = viewModelScope.launch { timerLock.withLock {
        if (dao.active().first() != null) { message.value = L10n.text(R.string.msg_bdcc9e46bff5); return@withLock }
        dao.saveActive(ActiveSession(bookId = book.id, startedAt = System.currentTimeMillis(), startPage = book.page,
            anchorEpoch = System.currentTimeMillis(), anchorElapsed = SystemClock.elapsedRealtime(), bootCount = boot()))
        if (book.shelf != "Reading") dao.saveBook(book.copy(shelf = "Reading", finishedAt = null))
    } }
    private fun settled(a: ActiveSession): ActiveSession {
        val anchor = a.anchorEpoch ?: return a
        val duration = if (a.bootCount == boot()) (SystemClock.elapsedRealtime() - a.anchorElapsed).coerceAtLeast(0) else 0
        return a.copy(spans = encodeSpans(decodeSpans(a.spans) + Span(anchor, anchor + duration)), anchorEpoch = null)
    }
    fun pause() = viewModelScope.launch { timerLock.withLock { dao.active().first()?.let { dao.saveActive(settled(it)) } } }
    fun resume() = viewModelScope.launch { timerLock.withLock {
        dao.active().first()?.takeIf { it.anchorEpoch == null }?.let {
            dao.saveActive(it.copy(anchorEpoch = System.currentTimeMillis(), anchorElapsed = SystemClock.elapsedRealtime(), bootCount = boot()))
        }
    } }
    fun elapsed(a: ActiveSession): Long = decodeSpans(a.spans).sumOf { it.duration } +
        if (a.anchorEpoch != null && a.bootCount == boot()) (SystemClock.elapsedRealtime() - a.anchorElapsed).coerceAtLeast(0) else 0
    fun finish(page: Int, note: String) = viewModelScope.launch { timerLock.withLock {
        val a = dao.active().first()?.let(::settled) ?: return@withLock
        val b = dao.book(a.bookId) ?: return@withLock
        val spans = decodeSpans(a.spans)
        val endPage = if (b.format == "Audiobook") b.page else page.coerceIn(a.startPage, if (b.totalPages > 0) b.totalPages else Int.MAX_VALUE)
        app.database.withTransaction {
            dao.saveSession(ReadingSession(bookId = b.id, startedAt = a.startedAt, endedAt = maxOf(System.currentTimeMillis(), spans.maxOfOrNull { it.end } ?: a.startedAt),
                durationMs = spans.sumOf { it.duration }, startPage = a.startPage, endPage = endPage,
                zone = a.zone, note = note, spans = a.spans))
            dao.saveBook(b.copy(page = endPage, shelf = if (b.totalPages > 0 && endPage == b.totalPages) "Finished" else b.shelf,
                finishedAt = if (b.totalPages > 0 && endPage == b.totalPages) System.currentTimeMillis() else b.finishedAt))
            dao.clearActive()
        }
        message.value = L10n.text(R.string.msg_5638ac4ab325)
    } }
    fun log(book: Book, minutes: Int, date: LocalDate, startPage: Int, endPage: Int, note: String, mood: String) = viewModelScope.launch {
        val zone = ZoneId.systemDefault()
        val dayMarker = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val duration = minutes.toLong() * 60_000
        app.database.withTransaction {
            dao.saveSession(ReadingSession(bookId = book.id, startedAt = dayMarker, endedAt = dayMarker,
                durationMs = duration, startPage = if (book.format == "Audiobook") 0 else startPage,
                endPage = if (book.format == "Audiobook") 0 else endPage, zone = zone.id, note = note, mood = mood,
                spans = "", manual = true))
            // Backdated logs must not rewind a book's current progress.
            if (book.format != "Audiobook" && endPage > book.page) dao.saveBook(book.copy(page = endPage))
        }
        message.value = L10n.text(R.string.msg_dda6c4da1fad)
    }
    fun daily(source: List<ReadingSession> = sessions.value): Map<LocalDate, Long> {
        val out = mutableMapOf<LocalDate, Long>()
        source.forEach { s ->
            if (s.manual) {
                val day = Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.of(s.zone)).toLocalDate()
                out[day] = (out[day] ?: 0) + s.durationMs
                return@forEach
            }
            val spans = decodeSpans(s.spans).ifEmpty { listOf(Span(s.startedAt, s.startedAt + s.durationMs)) }
            dailyDurations(spans, runCatching { ZoneId.of(s.zone) }.getOrDefault(ZoneId.systemDefault())).forEach { (day, value) ->
                out[day] = (out[day] ?: 0) + value
            }
        }
        return out
    }
    fun streak() = consistency(daily().filterValues { it >= 60_000 }.keys,
        policies.value.map { WeeklyRule(LocalDate.ofEpochDay(it.effectiveWeek), it.daysPerWeek, it.paused) },
        LocalDate.now(), DayOfWeek.of(settings.value.weekStart))
    fun deleteAll() = viewModelScope.launch { timerLock.withLock {
        app.database.withTransaction { dao.clearActive(); dao.clearSessions(); dao.clearNotes(); dao.clearBooks(); dao.clearPolicies() }
        java.io.File(app.filesDir, "documents").deleteRecursively()
        app.preferences.clear()
        message.value = L10n.text(R.string.msg_8647cbe24ed5)
    } }
}
