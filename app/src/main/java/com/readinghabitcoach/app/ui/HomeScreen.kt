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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readinghabitcoach.app.CoachViewModel
import com.readinghabitcoach.app.R
import com.readinghabitcoach.app.data.Book
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable fun HomeScreen(vm: CoachViewModel, add: () -> Unit, start: (Book) -> Unit, log: () -> Unit, note: () -> Unit, navigate: (String) -> Unit, detail: (Book) -> Unit) {
    val books by vm.books.collectAsState(); val sessions by vm.sessions.collectAsState(); val notes by vm.notes.collectAsState()
    val policies by vm.policies.collectAsState(); val settings by vm.settings.collectAsState()
    val current = books.firstOrNull { it.shelf == "Reading" } ?: books.firstOrNull { it.shelf == "Want to Read" }
    val today = LocalDate.now(); val daily = vm.daily(); val streak = vm.streak()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        PageTitle("A little reading,\na little more you.", "Make room for a few good pages.")
        Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(16.dp))) {
            Image(painterResource(R.drawable.reading_moment), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
            Column(Modifier.fillMaxHeight().fillMaxWidth(.63f).padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("Your next chapter\nis waiting.", fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 32.sp, color = Ink)
                Spacer(Modifier.height(18.dp))
                Primary("Start Reading Session") { if (current != null) start(current) else add() }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Today’s reading", durationLabel(daily[today] ?: 0), "of ${settings.minutes} minutes", Icons.Outlined.Schedule, Modifier.weight(1f))
            Stat("This week", "${streak.currentWeekDays} / ${streak.target}", "reading days", Icons.Outlined.DateRange, Modifier.weight(1f))
            Stat("Consistency", "${streak.current}", "confirmed weeks", Icons.Outlined.Spa, Modifier.weight(1f))
        }
        BoxWithConstraints {
            if (maxWidth > 700.dp) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1.25f)) { ContinueCard(current, start, detail, add); RecentBooks(books, detail, navigate) }
                Column(Modifier.weight(1f)) { WeekCard(daily, settings.weekStart); QuickActions(log, note); LatestNotes(notes.take(2).map { it.body }, navigate) }
            } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ContinueCard(current, start, detail, add); WeekCard(daily, settings.weekStart); QuickActions(log, note); RecentBooks(books, detail, navigate)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp)); Text("Your reading life, privately yours. Saved on this device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun Stat(title: String, value: String, caption: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Paper(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(7.dp)); Text(title, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(12.dp)); Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun ContinueCard(book: Book?, start: (Book) -> Unit, detail: (Book) -> Unit, add: () -> Unit) {
    SectionTitle("Between the pages")
    Paper(Modifier.fillMaxWidth()) {
        if (book == null) EmptyState("Your story starts here", "Add the book you’re reading.", action = { Primary("Add your first book", Icons.Outlined.Add, onClick = add) })
        else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BookCover(book, 104, 154)
            Column(Modifier.weight(1f)) {
                Text(book.format.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(7.dp)); Text(book.title, style = MaterialTheme.typography.titleLarge)
                Text(book.author.ifBlank { "Your personal library" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp)); Progress(if (book.totalPages > 0) book.page.toFloat() / book.totalPages else 0f,
                    if (book.totalPages > 0) "Page ${book.page} of ${book.totalPages}" else "${book.shelf} · ${book.format}")
                TextButton({ detail(book) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) { Text("Open book details"); Icon(Icons.Outlined.ChevronRight, null) }
            }
        }
    }
}
@Composable fun WeekCard(daily: Map<LocalDate, Long>, firstDay: Int) {
    val today = LocalDate.now(); val week = com.readinghabitcoach.app.domain.weekOf(today, DayOfWeek.of(firstDay))
    SectionTitle("A week of small moments")
    Paper(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0L..6).forEach { i -> val day = week.plusDays(i); val read = (daily[day] ?: 0) >= 60_000
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semanticsForDay(day, daily[day] ?: 0)) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(50), color = if (read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                        border = if (day == today) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null, modifier = Modifier.size(30.dp)) {
                        Box(contentAlignment = Alignment.Center) { if (read) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary) else Text(day.dayOfMonth.toString(), fontSize = 12.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp)); Text("Every reading day counts. There’s room for rest, too.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
private fun Modifier.semanticsForDay(date: LocalDate, ms: Long) = this.then(Modifier.semantics { contentDescription = "$date, ${durationLabel(ms)}" })
@Composable private fun QuickActions(log: () -> Unit, note: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(log, Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text(" Log reading") }
        OutlinedButton(note, Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Outlined.EditNote, null, Modifier.size(18.dp)); Text(" Quick note") }
    }
}
@Composable private fun RecentBooks(books: List<Book>, detail: (Book) -> Unit, nav: (String) -> Unit) {
    SectionTitle("On your bookshelf", { TextButton({ nav("Library") }) { Text("View all"); Icon(Icons.Outlined.ChevronRight, null) } })
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        books.take(5).forEach { b -> Column(Modifier.width(100.dp).clickable { detail(b) }) {
            BookCover(b, 100, 145); Spacer(Modifier.height(8.dp)); Text(b.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
        } }
    }
}
@Composable private fun LatestNotes(notes: List<String>, nav: (String) -> Unit) {
    SectionTitle("Thoughts to keep", { TextButton({ nav("Journal") }) { Text("Journal") } })
    if (notes.isEmpty()) Text("A sentence, a question, a thought worth remembering.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    notes.forEach { Text("“${it.take(140)}”", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 14.dp)) }
}
