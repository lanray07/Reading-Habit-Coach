package com.readinghabitcoach.app.ui

import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.R

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readinghabitcoach.app.CoachViewModel
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun SessionScreen(vm: CoachViewModel, add: () -> Unit, log: () -> Unit, finish: () -> Unit) {
    val books by vm.books.collectAsState(); val active by vm.active.collectAsState(); val tick by vm.tick.collectAsState(); val sessions by vm.sessions.collectAsState()
    var selected by remember(books) { mutableStateOf(books.firstOrNull { it.shelf == "Reading" }?.id ?: books.firstOrNull()?.id ?: "") }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageTitle(L10n.text(R.string.msg_a139b7eb7841), L10n.text(R.string.msg_40ec541ae738))
        Paper(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val book = books.find { it.id == (active?.bookId ?: selected) }
                if (book != null) { BookCover(book, 110, 160); Spacer(Modifier.height(20.dp)); Text(book.title, style = MaterialTheme.typography.headlineSmall); Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else { Text(L10n.text(R.string.msg_6a0d9341c953), style = MaterialTheme.typography.headlineSmall) }
                Spacer(Modifier.height(24.dp))
                Text(active?.let { tick; clockLabel(vm.elapsed(it)) } ?: "00:00:00", fontFamily = FontFamily.Serif, fontSize = 54.sp, color = MaterialTheme.colorScheme.primary)
                Text(if (active?.anchorEpoch != null) L10n.text(R.string.msg_4d8d943518b4) else if (active != null) L10n.text(R.string.msg_928c57225ea2) else L10n.text(R.string.msg_069108714008), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                if (active == null) {
                    if (books.isEmpty()) Primary(L10n.text(R.string.msg_ec8141839009), Icons.Outlined.Add, onClick = add)
                    else { BookPicker(books, selected, { selected = it }); Spacer(Modifier.height(14.dp)); Primary(L10n.text(R.string.msg_15671a6e8f28), enabled = book != null) { book?.let(vm::start) } }
                } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Primary(if (active?.anchorEpoch != null) L10n.text(R.string.msg_858e4ba7a29f) else L10n.text(R.string.msg_d640c7421da0), if (active?.anchorEpoch != null) Icons.Outlined.Pause else Icons.Outlined.PlayArrow) { if (active?.anchorEpoch != null) vm.pause() else vm.resume() }
                    OutlinedButton(finish, Modifier.heightIn(min = 50.dp)) { Icon(Icons.Outlined.Check, null); Text(L10n.text(R.string.msg_c65d023ed49d)) }
                }
                TextButton(log, modifier = Modifier.padding(top = 12.dp)) { Text(L10n.text(R.string.msg_f053266ed0ae)) }
            }
        }
        SectionTitle(L10n.text(R.string.msg_b3869ac49b70))
        if (sessions.isEmpty()) EmptyState(L10n.text(R.string.msg_16ef4d585ba6), L10n.text(R.string.msg_184e5541deb8), Icons.Outlined.Schedule)
        sessions.take(30).forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(books.find { it.id == s.bookId }?.title ?: L10n.text(R.string.msg_909cb81127c5), style = MaterialTheme.typography.titleMedium)
                    Text(Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.of(s.zone)).format((if (s.manual) DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM) else DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)).withLocale(L10n.locale())) + if (s.manual) L10n.text(R.string.msg_413e1ae56549) else "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    if (s.note.isNotBlank()) Text(s.note, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) { Text(durationLabel(s.durationMs), color = MaterialTheme.colorScheme.primary); if (s.endPage > s.startPage) Text(L10n.quantity("page_count", s.endPage - s.startPage), style = MaterialTheme.typography.bodyMedium) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable fun FinishSession(vm: CoachViewModel, dismiss: () -> Unit) {
    val active by vm.active.collectAsState(); val books by vm.books.collectAsState(); val book = books.find { it.id == active?.bookId }
    var page by remember { mutableStateOf(book?.page?.toString() ?: "0") }; var note by remember { mutableStateOf("") }
    val n = page.toIntOrNull() ?: -1
    val valid = book != null && n >= (active?.startPage ?: 0) && (book.totalPages == 0 || n <= book.totalPages)
    EditorDialog(L10n.text(R.string.msg_9c9f78d4393f), dismiss, { vm.finish(n, note); dismiss() }, valid) {
        Text("${book?.title ?: L10n.text(R.string.msg_df4bb1fa43af)} · ${active?.let { durationLabel(vm.elapsed(it)) } ?: ""}", style = MaterialTheme.typography.titleLarge)
        if (book?.format != "Audiobook") Field(page, { page = it.filter(Char::isDigit).take(6) }, L10n.text(R.string.msg_e4349b1ac023))
        Field(note, { note = it }, L10n.text(R.string.msg_32ba20294f22), single = false)
        Text(L10n.text(R.string.msg_f3ec3bf1ece6), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
