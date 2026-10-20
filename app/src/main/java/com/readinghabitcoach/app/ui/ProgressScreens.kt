package com.readinghabitcoach.app.ui

import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.R

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.readinghabitcoach.app.CoachViewModel
import com.readinghabitcoach.app.data.Settings
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun GoalsScreen(vm: CoachViewModel) {
    val settings by vm.settings.collectAsState(); val policies by vm.policies.collectAsState(); val sessions by vm.sessions.collectAsState(); val books by vm.books.collectAsState()
    var minutes by remember(settings.minutes) { mutableFloatStateOf(settings.minutes.toFloat()) }
    var days by remember(settings.weeklyDays) { mutableFloatStateOf(settings.weeklyDays.toFloat()) }
    var yearly by remember(settings.yearlyBooks) { mutableStateOf(settings.yearlyBooks.toString()) }
    var paused by remember(policies.lastOrNull()?.paused) { mutableStateOf(policies.lastOrNull()?.paused ?: false) }
    val streak = vm.streak(); val todayMinutes = (vm.daily()[LocalDate.now()] ?: 0) / 60_000
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageTitle(L10n.text(R.string.msg_48b07a67a5d8), L10n.text(R.string.msg_741474b80c28))
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_a8c7c2d2de92), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp)); Text(L10n.text(R.string.msg_c590d712259b, minutes.toInt()), style = MaterialTheme.typography.titleLarge)
            Slider(minutes, { minutes = it }, valueRange = 5f..120f, steps = 22)
            Text(L10n.text(R.string.msg_0dbea2b395d9), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp)); Progress((todayMinutes.toFloat() / settings.minutes).coerceIn(0f, 1f), L10n.text(R.string.msg_23fc29ae174c, todayMinutes, settings.minutes))
            TextButton({ vm.updateSettings(settings.copy(minutes = minutes.toInt())) }) { Text(L10n.text(R.string.msg_61d71b387e5e)) }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_12cf6abd391e), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp)); Text(L10n.text(R.string.msg_7d8274cd54b1, days.toInt()), style = MaterialTheme.typography.titleLarge)
            Slider(days, { days = it }, valueRange = 1f..7f, steps = 5)
            Text(L10n.text(R.string.msg_0eeb7813871e), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp)); Progress(streak.currentWeekDays.toFloat() / streak.target, L10n.text(R.string.msg_61f8273e1bc2, streak.currentWeekDays, streak.target))
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(paused, { paused = it }); Spacer(Modifier.width(12.dp)); Text(L10n.text(R.string.msg_41eb2d957a5c)) }
            TextButton({ vm.changeGoal(days.toInt(), paused) }) { Text(L10n.text(R.string.msg_847161f09b51)) }
            Text(L10n.text(R.string.msg_331110799adb), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Paper(Modifier.weight(1f)) { Text(L10n.number(streak.current), style = MaterialTheme.typography.headlineLarge); Text(L10n.text(R.string.msg_6d838aac15b2)) }
            Paper(Modifier.weight(1f)) { Text(L10n.number(streak.best), style = MaterialTheme.typography.headlineLarge); Text(L10n.text(R.string.msg_7028345286d2)) }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_020f0c842953), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp)); Field(yearly, { yearly = it.filter(Char::isDigit).take(3) }, L10n.text(R.string.msg_6982b28f37f0))
            val finished = books.count { it.finishedAt?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).year == LocalDate.now().year } == true }
            Spacer(Modifier.height(16.dp)); Progress(finished.toFloat() / settings.yearlyBooks, L10n.text(R.string.msg_d096a7f387c2, finished, settings.yearlyBooks))
            TextButton({ yearly.toIntOrNull()?.takeIf { it in 1..999 }?.let { vm.updateSettings(settings.copy(yearlyBooks = it)) } }) { Text(L10n.text(R.string.msg_fcc64ba6e28b)) }
        }
    }
}

@Composable fun InsightsScreen(vm: CoachViewModel) {
    val sessions by vm.sessions.collectAsState(); val books by vm.books.collectAsState()
    var range by remember { mutableStateOf("This month") }; var format by remember { mutableStateOf("All formats") }
    var query by remember { mutableStateOf("") }
    val today = LocalDate.now(); val start = when (range) { "This week" -> com.readinghabitcoach.app.domain.weekOf(today); "This year" -> today.withDayOfYear(1); "All time" -> LocalDate.of(1970, 1, 1); else -> today.withDayOfMonth(1) }
    val selectedBooks = books.filter { (format == "All formats" || it.format == format) && "${it.genre} ${it.tags} ${it.shelf}".contains(query, true) }
    val selected = sessions.filter { s -> selectedBooks.any { it.id == s.bookId } && Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.of(s.zone)).toLocalDate() >= start }
    val time = selected.sumOf { it.durationMs }; val pages = selected.filter { s -> books.find { it.id == s.bookId }?.format != "Audiobook" }.sumOf { (it.endPage - it.startPage).coerceAtLeast(0) }
    val daily = vm.daily()
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageTitle(L10n.text(R.string.msg_b576fc8068e7), L10n.text(R.string.msg_4466567bb416))
        ChoiceRow(listOf("This week", "This month", "This year", "All time"), range) { range = it }
        ChoiceRow(listOf("All formats") + com.readinghabitcoach.app.data.formats, format) { format = it }
        Field(query, { query = it }, L10n.text(R.string.msg_24a7b3d9b8aa))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Paper(Modifier.weight(1f)) { Text(durationLabel(time), style = MaterialTheme.typography.headlineMedium); Text(L10n.text(R.string.msg_d648f2ec79d8)) }
            Paper(Modifier.weight(1f)) { Text(L10n.number(pages), style = MaterialTheme.typography.headlineMedium); Text(L10n.text(R.string.msg_253223c8d3da)) }
            Paper(Modifier.weight(1f)) { Text(L10n.number(selected.size), style = MaterialTheme.typography.headlineMedium); Text(L10n.text(R.string.msg_1225ae6c1ae6)) }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_f211e99b6657), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            val week = com.readinghabitcoach.app.domain.weekOf(today)
            val filteredDaily = vm.daily(selected)
            val peak = (0L..6).maxOf { filteredDaily[week.plusDays(it)] ?: 0 }.coerceAtLeast(60_000)
            Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                (0L..6).forEach { i -> val date = week.plusDays(i); val ms = filteredDaily[date] ?: 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { contentDescription = "$date: ${durationLabel(ms)}" }) {
                        Text(L10n.number(ms / 60_000), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp)); Box(Modifier.width(28.dp).height((ms.toFloat() / peak * 100).coerceAtLeast(3f).dp).clip(RoundedCornerShape(5.dp, 5.dp, 0.dp, 0.dp)).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.height(8.dp)); Text(date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, L10n.locale()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp)); Text(L10n.text(R.string.msg_4e04d5b93ab8, week.format(DateTimeFormatter.ofPattern("d MMM")), week.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_054333db6376), style = MaterialTheme.typography.headlineSmall)
            Text(L10n.text(R.string.msg_62a839f61d51), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val beginning = com.readinghabitcoach.app.domain.weekOf(today).minusWeeks(11)
                (0L..11).forEach { w -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    (0L..6).forEach { d -> val day = beginning.plusWeeks(w).plusDays(d); val ms = daily[day] ?: 0
                        Box(Modifier.size(23.dp).clip(RoundedCornerShape(4.dp)).background(if (ms == 0L) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primary.copy(alpha = (ms.toFloat() / 1_800_000).coerceIn(.3f, 1f))).semantics { contentDescription = "$day: ${durationLabel(ms)}" })
                    }
                } }
            }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_53ef03812ceb), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp)); Text(L10n.text(R.string.msg_162d83c2a605, durationLabel(if (selected.isEmpty()) 0 else time / selected.size)))
            val pageTime = selected.filter { s -> s.endPage > s.startPage && books.find { it.id == s.bookId }?.format != "Audiobook" }.sumOf { it.durationMs }
            Text(L10n.text(R.string.msg_491245c4a09c, if (pageTime > 0) L10n.text(R.string.msg_9491c1c98467).format(L10n.locale(), pages * 3_600_000.0 / pageTime) else L10n.text(R.string.msg_ac41c3cb8318)))
            val completed = selectedBooks.count { it.finishedAt?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() >= start } == true }
            Text(L10n.text(R.string.msg_5c3773b10c3e, completed))
            Spacer(Modifier.height(16.dp)); selected.groupBy { s -> books.find { it.id == s.bookId }?.format ?: L10n.text(R.string.msg_f97e9da0e3b8) }.forEach { (key, value) -> Text("${L10n.label(key)}  ·  ${durationLabel(value.sumOf { it.durationMs })}") }
            Text(L10n.text(R.string.msg_e9daab3619b2), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
