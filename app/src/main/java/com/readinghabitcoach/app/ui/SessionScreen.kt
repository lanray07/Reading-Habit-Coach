package com.readinghabitcoach.app.ui

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
        PageTitle("Time for a good book", "Settle in. The rest can wait.")
        Paper(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val book = books.find { it.id == (active?.bookId ?: selected) }
                if (book != null) { BookCover(book, 110, 160); Spacer(Modifier.height(20.dp)); Text(book.title, style = MaterialTheme.typography.headlineSmall); Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else { Text("Choose your next chapter", style = MaterialTheme.typography.headlineSmall) }
                Spacer(Modifier.height(24.dp))
                Text(active?.let { tick; clockLabel(vm.elapsed(it)) } ?: "00:00:00", fontFamily = FontFamily.Serif, fontSize = 54.sp, color = MaterialTheme.colorScheme.primary)
                Text(if (active?.anchorEpoch != null) "A moment just for reading" else if (active != null) "Paused · take your time" else "Ready whenever you are", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                if (active == null) {
                    if (books.isEmpty()) Primary("Add your first book", Icons.Outlined.Add, onClick = add)
                    else { BookPicker(books, selected, { selected = it }); Spacer(Modifier.height(14.dp)); Primary("Start Reading Session", enabled = book != null) { book?.let(vm::start) } }
                } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Primary(if (active?.anchorEpoch != null) "Pause" else "Resume", if (active?.anchorEpoch != null) Icons.Outlined.Pause else Icons.Outlined.PlayArrow) { if (active?.anchorEpoch != null) vm.pause() else vm.resume() }
                    OutlinedButton(finish, Modifier.heightIn(min = 50.dp)) { Icon(Icons.Outlined.Check, null); Text(" Finish session") }
                }
                TextButton(log, modifier = Modifier.padding(top = 12.dp)) { Text("Already read? Log a session") }
            }
        }
        SectionTitle("Your recent reading")
        if (sessions.isEmpty()) EmptyState("A fresh page", "Your saved sessions will appear here.", Icons.Outlined.Schedule)
        sessions.take(30).forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(books.find { it.id == s.bookId }?.title ?: "Book", style = MaterialTheme.typography.titleMedium)
                    Text(Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.of(s.zone)).format(DateTimeFormatter.ofPattern(if (s.manual) "d MMM yyyy" else "d MMM yyyy · HH:mm")) + if (s.manual) " · Manually logged" else "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    if (s.note.isNotBlank()) Text(s.note, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) { Text(durationLabel(s.durationMs), color = MaterialTheme.colorScheme.primary); if (s.endPage > s.startPage) Text("${s.endPage - s.startPage} pages", style = MaterialTheme.typography.bodyMedium) }
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
    EditorDialog("A few pages further", dismiss, { vm.finish(n, note); dismiss() }, valid) {
        Text("${book?.title ?: "Your book"} · ${active?.let { durationLabel(vm.elapsed(it)) } ?: ""}", style = MaterialTheme.typography.titleLarge)
        if (book?.format != "Audiobook") Field(page, { page = it.filter(Char::isDigit).take(6) }, "Page reached")
        Field(note, { note = it }, "A thought from this session (optional)", single = false)
        Text("Your reading time will be saved when you finish.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
