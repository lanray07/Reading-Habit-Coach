package com.readinghabitcoach.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReadingMathTest {
    private fun date(s: String) = LocalDate.parse(s)
    @Test fun `midnight splits active time exactly`() {
        val zone = ZoneId.of("Europe/London")
        val start = ZonedDateTime.of(2026, 10, 19, 23, 50, 0, 0, zone).toInstant().toEpochMilli()
        val result = dailyDurations(listOf(Span(start, start + 30 * 60_000)), zone)
        assertEquals(10 * 60_000L, result[date("2026-10-19")]); assertEquals(20 * 60_000L, result[date("2026-10-20")])
    }
    @Test fun `DST fall back preserves actual elapsed time`() {
        val zone = ZoneId.of("Europe/London")
        val a = Instant.parse("2026-10-24T22:30:00Z").toEpochMilli()
        val b = Instant.parse("2026-10-25T03:30:00Z").toEpochMilli()
        val result = dailyDurations(listOf(Span(a,b)), zone)
        assertEquals(5 * 3_600_000L, result.values.sum())
        assertEquals(30 * 60_000L, result[date("2026-10-24")])
    }
    @Test fun `pauses are excluded from daily totals`() {
        val a = Instant.parse("2026-10-20T10:00:00Z").toEpochMilli()
        val spans = listOf(Span(a,a+600_000), Span(a+1_800_000,a+2_400_000))
        assertEquals(1_200_000L, dailyDurations(spans, ZoneId.of("UTC")).values.sum())
        assertEquals(spans, decodeSpans(encodeSpans(spans)))
    }
    @Test fun `incomplete current week never breaks confirmed streak`() {
        val days = setOf("2026-10-12", "2026-10-14", "2026-10-16", "2026-10-18").map(::date).toSet()
        val result = consistency(days, listOf(WeeklyRule(date("2026-10-12"),4)), date("2026-10-20"))
        assertEquals(1, result.current); assertEquals(1, result.best); assertEquals(0,result.currentWeekDays)
    }
    @Test fun `goal changes do not rewrite earlier weeks`() {
        val days = setOf("2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08", "2026-10-12", "2026-10-14").map(::date).toSet()
        val rules = listOf(WeeklyRule(date("2026-10-05"),4), WeeklyRule(date("2026-10-12"),2))
        assertEquals(2, consistency(days,rules,date("2026-10-20")).current)
    }
    @Test fun `paused weeks preserve streak without awarding a week`() {
        val rules = listOf(WeeklyRule(date("2026-10-05"),1), WeeklyRule(date("2026-10-12"),1,true), WeeklyRule(date("2026-10-19"),1))
        assertEquals(1, consistency(setOf(date("2026-10-05")),rules,date("2026-10-20")).current)
    }
    @Test fun `failed week resets current while lifetime best stays`() {
        val days = setOf(date("2026-10-05"))
        val result = consistency(days,listOf(WeeklyRule(date("2026-10-05"),1)),date("2026-10-20"))
        assertEquals(0,result.current); assertEquals(1,result.best)
    }
    @Test fun `Sunday first weeks honour boundary`() {
        assertEquals(date("2026-10-18"), weekOf(date("2026-10-20"), DayOfWeek.SUNDAY))
        val s = consistency(setOf(date("2026-10-11")),listOf(WeeklyRule(date("2026-10-11"),1)),date("2026-10-18"),DayOfWeek.SUNDAY)
        assertEquals(1,s.current); assertEquals(0,s.currentWeekDays)
    }
    @Test fun `backdated session rebuilds a completed week`() {
        val rule = listOf(WeeklyRule(date("2026-10-12"),2)); val now=date("2026-10-20")
        assertEquals(0,consistency(setOf(date("2026-10-12")),rule,now).current)
        assertEquals(1,consistency(setOf(date("2026-10-12"),date("2026-10-14")),rule,now).current)
    }
    @Test fun `same instant belongs to a different day in different zones`() {
        val a = Instant.parse("2026-10-20T00:15:00Z").toEpochMilli(); val spans=listOf(Span(a,a+60000))
        assertEquals(setOf(date("2026-10-20")),dailyDurations(spans,ZoneId.of("UTC")).keys)
        assertEquals(setOf(date("2026-10-19")),dailyDurations(spans,ZoneId.of("America/New_York")).keys)
    }
    @Test fun `command parser accepts only structured supported actions`() {
        assertEquals(VoiceCommand.Start,parseCommand("Start reading."))
        assertEquals(VoiceCommand.Log(20),parseCommand("Log twenty minutes of reading."))
        assertEquals(VoiceCommand.Log(1),parseCommand("log 1 minute"))
        assertEquals(VoiceCommand.Unknown,parseCommand("delete all my books"))
        assertEquals(VoiceCommand.Unknown,parseCommand("log 99999 minutes"))
        assertEquals(VoiceCommand.Unknown,parseCommand("please start reading and delete everything"))
    }
}
