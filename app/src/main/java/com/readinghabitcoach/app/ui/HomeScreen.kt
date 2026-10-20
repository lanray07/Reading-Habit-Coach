package com.readinghabitcoach.app.ui

import com.readinghabitcoach.app.i18n.L10n

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
        Text(today.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL).withLocale(L10n.locale())), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        PageTitle(L10n.text(R.string.msg_34304bcc142d), L10n.text(R.string.msg_f22413504825))
        Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(16.dp))) {
            Image(painterResource(R.drawable.reading_moment), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
            Column(Modifier.fillMaxHeight().fillMaxWidth(.63f).padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text(L10n.text(R.string.msg_8baf2f616002), fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 32.sp, color = Ink)
                Spacer(Modifier.height(18.dp))
                Primary(L10n.text(R.string.msg_15671a6e8f28)) { if (current != null) start(current) else add() }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(L10n.text(R.string.msg_03c3a2189ec3), durationLabel(daily[today] ?: 0), L10n.text(R.string.msg_fec0990b1504, settings.minutes), Icons.Outlined.Schedule, Modifier.weight(1f))
            Stat(L10n.text(R.string.msg_8c4eef5ab253), "${L10n.number(streak.currentWeekDays)} / ${L10n.number(streak.target)}", L10n.text(R.string.msg_89809d2d8550), Icons.Outlined.DateRange, Modifier.weight(1f))
            Stat(L10n.text(R.string.msg_36e8309d0868), L10n.number(streak.current), L10n.text(R.string.msg_6d838aac15b2), Icons.Outlined.Spa, Modifier.weight(1f))
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
            Spacer(Modifier.width(8.dp)); Text(L10n.text(R.string.msg_1279a5511a12), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    SectionTitle(L10n.text(R.string.msg_3b94074c1cbc))
    Paper(Modifier.fillMaxWidth()) {
        if (book == null) EmptyState(L10n.text(R.string.msg_338639b36001), L10n.text(R.string.msg_f84f639c53df), action = { Primary(L10n.text(R.string.msg_ec8141839009), Icons.Outlined.Add, onClick = add) })
        else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BookCover(book, 104, 154)
            Column(Modifier.weight(1f)) {
                Text(L10n.label(book.format).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(7.dp)); Text(book.title, style = MaterialTheme.typography.titleLarge)
                Text(book.author.ifBlank { L10n.text(R.string.msg_0860c0c0ab29) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp)); Progress(if (book.totalPages > 0) book.page.toFloat() / book.totalPages else 0f,
                    if (book.totalPages > 0) L10n.text(R.string.msg_8b9ffadf4985, book.page, book.totalPages) else "${L10n.label(book.shelf)} · ${L10n.label(book.format)}")
                TextButton({ detail(book) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 48.dp)) { Text(L10n.text(R.string.msg_aa6a98986de0)); Icon(Icons.Outlined.ChevronRight, null) }
            }
        }
    }
}
@Composable fun WeekCard(daily: Map<LocalDate, Long>, firstDay: Int) {
    val today = LocalDate.now(); val week = com.readinghabitcoach.app.domain.weekOf(today, DayOfWeek.of(firstDay))
    SectionTitle(L10n.text(R.string.msg_39c103010f2b))
    Paper(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0L..6).forEach { i -> val day = week.plusDays(i); val read = (daily[day] ?: 0) >= 60_000
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semanticsForDay(day, daily[day] ?: 0)) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(50), color = if (read) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                        border = if (day == today) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null, modifier = Modifier.size(30.dp)) {
                        Box(contentAlignment = Alignment.Center) { if (read) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary) else Text(L10n.number(day.dayOfMonth), fontSize = 12.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp)); Text(L10n.text(R.string.msg_0e7ca8377c32), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
private fun Modifier.semanticsForDay(date: LocalDate, ms: Long) = this.then(Modifier.semantics { contentDescription = "$date, ${durationLabel(ms)}" })
@Composable private fun QuickActions(log: () -> Unit, note: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(log, Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text(L10n.text(R.string.msg_f8de549d9deb)) }
        OutlinedButton(note, Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Outlined.EditNote, null, Modifier.size(18.dp)); Text(L10n.text(R.string.msg_b81952804aa9)) }
    }
}
@Composable private fun RecentBooks(books: List<Book>, detail: (Book) -> Unit, nav: (String) -> Unit) {
    SectionTitle(L10n.text(R.string.msg_19374331d0b4), { TextButton({ nav("Library") }) { Text(L10n.text(R.string.msg_30a64216eaea)); Icon(Icons.Outlined.ChevronRight, null) } })
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        books.take(5).forEach { b -> Column(Modifier.width(100.dp).clickable { detail(b) }) {
            BookCover(b, 100, 145); Spacer(Modifier.height(8.dp)); Text(b.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
        } }
    }
}
@Composable private fun LatestNotes(notes: List<String>, nav: (String) -> Unit) {
    SectionTitle(L10n.text(R.string.msg_109aa68b590a), { TextButton({ nav("Journal") }) { Text(L10n.text(R.string.msg_43b7c75d56f0)) } })
    if (notes.isEmpty()) Text(L10n.text(R.string.msg_cf17ebe7bd9d), color = MaterialTheme.colorScheme.onSurfaceVariant)
    notes.forEach { Text("“${it.take(140)}”", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 14.dp)) }
}
