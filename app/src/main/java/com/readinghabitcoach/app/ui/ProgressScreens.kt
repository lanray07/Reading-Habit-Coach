package com.readinghabitcoach.app.ui

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
        PageTitle("At your own pace", "Small intentions. Lasting reading habits.")
        Paper(Modifier.fillMaxWidth()) {
            Text("Your daily moment", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp)); Text("${minutes.toInt()} minutes a day", style = MaterialTheme.typography.titleLarge)
            Slider(minutes, { minutes = it }, valueRange = 5f..120f, steps = 22)
            Text("Start small. You can always stay a little longer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp)); Progress((todayMinutes.toFloat() / settings.minutes).coerceIn(0f, 1f), "$todayMinutes of ${settings.minutes} minutes today")
            TextButton({ vm.updateSettings(settings.copy(minutes = minutes.toInt())) }) { Text("Save daily goal") }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("Consistency, with breathing room", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp)); Text("Read ${days.toInt()} days each week", style = MaterialTheme.typography.titleLarge)
            Slider(days, { days = it }, valueRange = 1f..7f, steps = 5)
            Text("A reading day is any day with at least one logged minute. Choose the days that suit you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp)); Progress(streak.currentWeekDays.toFloat() / streak.target, "${streak.currentWeekDays} of ${streak.target} days this week")
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(paused, { paused = it }); Spacer(Modifier.width(12.dp)); Text("Pause weekly goal from next week") }
            TextButton({ vm.changeGoal(days.toInt(), paused) }) { Text("Save weekly intention") }
            Text("Changes take effect next week. Confirmed history stays intact.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Paper(Modifier.weight(1f)) { Text("${streak.current}", style = MaterialTheme.typography.headlineLarge); Text("confirmed weeks") }
            Paper(Modifier.weight(1f)) { Text("${streak.best}", style = MaterialTheme.typography.headlineLarge); Text("personal best") }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("A year of good books", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp)); Field(yearly, { yearly = it.filter(Char::isDigit).take(3) }, "Books this year")
            val finished = books.count { it.finishedAt?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).year == LocalDate.now().year } == true }
            Spacer(Modifier.height(16.dp)); Progress(finished.toFloat() / settings.yearlyBooks, "$finished of ${settings.yearlyBooks} books finished")
            TextButton({ yearly.toIntOrNull()?.takeIf { it in 1..999 }?.let { vm.updateSettings(settings.copy(yearlyBooks = it)) } }) { Text("Save annual goal") }
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
        PageTitle("Your reading, reflected", "A gentle look at the pages and moments adding up.")
        ChoiceRow(listOf("This week", "This month", "This year", "All time"), range) { range = it }
        ChoiceRow(listOf("All formats") + com.readinghabitcoach.app.data.formats, format) { format = it }
        Field(query, { query = it }, "Filter by genre, tag or shelf")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Paper(Modifier.weight(1f)) { Text(durationLabel(time), style = MaterialTheme.typography.headlineMedium); Text("reading time") }
            Paper(Modifier.weight(1f)) { Text("$pages", style = MaterialTheme.typography.headlineMedium); Text("pages logged") }
            Paper(Modifier.weight(1f)) { Text("${selected.size}", style = MaterialTheme.typography.headlineMedium); Text("sessions") }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("The rhythm of your week", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            val week = com.readinghabitcoach.app.domain.weekOf(today)
            val filteredDaily = selected.flatMap { s -> com.readinghabitcoach.app.domain.dailyDurations(com.readinghabitcoach.app.domain.decodeSpans(s.spans), ZoneId.of(s.zone)).entries.map { it.key to it.value } }.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
            val peak = (0L..6).maxOf { filteredDaily[week.plusDays(it)] ?: 0 }.coerceAtLeast(60_000)
            Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                (0L..6).forEach { i -> val date = week.plusDays(i); val ms = filteredDaily[date] ?: 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { contentDescription = "$date: ${durationLabel(ms)}" }) {
                        Text("${ms / 60_000}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp)); Box(Modifier.width(28.dp).height((ms.toFloat() / peak * 100).coerceAtLeast(3f).dp).clip(RoundedCornerShape(5.dp, 5.dp, 0.dp, 0.dp)).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.height(8.dp)); Text(date.dayOfWeek.name.take(1), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp)); Text("Minutes per day · ${week.format(DateTimeFormatter.ofPattern("d MMM"))} – ${week.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("A calendar of reading", style = MaterialTheme.typography.headlineSmall)
            Text("All reading · last 12 weeks", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text("A few more reflections", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp)); Text("Average session  ·  ${durationLabel(if (selected.isEmpty()) 0 else time / selected.size)}")
            val pageTime = selected.filter { s -> s.endPage > s.startPage && books.find { it.id == s.bookId }?.format != "Audiobook" }.sumOf { it.durationMs }
            Text("Logged pace  ·  ${if (pageTime > 0) "%.1f pages/hour".format(pages * 3_600_000.0 / pageTime) else "Add page progress to see your pace"}")
            val completed = selectedBooks.count { it.finishedAt?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() >= start } == true }
            Text("Books finished  ·  $completed")
            Spacer(Modifier.height(16.dp)); selected.groupBy { s -> books.find { it.id == s.bookId }?.format ?: "Other" }.forEach { (key, value) -> Text("$key  ·  ${durationLabel(value.sumOf { it.durationMs })}") }
            Text("Audiobook time is recorded as time, never converted to pages.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
