package com.readinghabitcoach.app.ui

import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.R

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.readinghabitcoach.app.data.*

@Composable fun LibraryScreen(books: List<Book>, add: () -> Unit, import: () -> Unit, detail: (Book) -> Unit) {
    var query by remember { mutableStateOf("") }; var shelf by remember { mutableStateOf("All books") }; var format by remember { mutableStateOf("All formats") }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageTitle(L10n.text(R.string.msg_0860c0c0ab29), L10n.text(R.string.msg_505e11519ec2))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Primary(L10n.text(R.string.msg_1e31eb41e110), Icons.Outlined.Add, onClick = add); OutlinedButton(import, Modifier.heightIn(min = 50.dp)) { Icon(Icons.Outlined.FileOpen, null); Text(L10n.text(R.string.msg_9177d93dead4)) } }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text(L10n.text(R.string.msg_8cf5d5df7179)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
        ChoiceRow(listOf("All books") + shelves, shelf) { shelf = it }
        ChoiceRow(listOf("All formats") + formats, format) { format = it }
        val filtered = books.filter { (shelf == "All books" || it.shelf == shelf) && (format == "All formats" || it.format == format) &&
            "${it.title} ${it.author} ${it.isbn} ${it.genre} ${it.tags}".contains(query, true) }
        Text(L10n.quantity("book_count", filtered.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (filtered.isEmpty()) EmptyState(L10n.text(R.string.msg_5dcc81447516), L10n.text(R.string.msg_23920a8c4af1), action = { Primary(L10n.text(R.string.msg_1e31eb41e110), Icons.Outlined.Add, onClick = add) })
        BoxWithConstraints {
            val columns = if (maxWidth > 900.dp) 3 else if (maxWidth > 560.dp) 2 else 1
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                filtered.chunked(columns).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { book -> Paper(Modifier.weight(1f).clickable { detail(book) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            BookCover(book)
                            Column(Modifier.weight(1f)) {
                                Text(L10n.label(book.shelf), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp)); Text(book.title, style = MaterialTheme.typography.titleLarge)
                                Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(14.dp)); Text(L10n.label(book.format), style = MaterialTheme.typography.bodyMedium)
                                if (book.totalPages > 0) { Spacer(Modifier.height(8.dp)); Progress(book.page.toFloat() / book.totalPages, L10n.text(R.string.msg_22c891163f12, book.page, book.totalPages)) }
                            }
                        }
                    } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                } }
            }
        }
    }
}

@Composable fun JournalScreen(notes: List<JournalNote>, books: List<Book>, add: () -> Unit, edit: (JournalNote) -> Unit, favourite: (JournalNote) -> Unit, export: () -> Unit) {
    var query by remember { mutableStateOf("") }; var type by remember { mutableStateOf("All notes") }; var favourites by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageTitle(L10n.text(R.string.msg_a50b2b2340e6), L10n.text(R.string.msg_86d6f3c5a5e7))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Primary(L10n.text(R.string.msg_a1ba5790b07e), Icons.Outlined.EditNote, onClick = add); OutlinedButton(export, Modifier.heightIn(min = 50.dp)) { Text(L10n.text(R.string.msg_7672569979ce)) } }
        Field(query, { query = it }, L10n.text(R.string.msg_01fb752b1249))
        ChoiceRow(listOf("All notes") + noteTypes, type) { type = it }
        FilterChip(favourites, { favourites = !favourites }, label = { Text(L10n.text(R.string.msg_d97d51d37092)) }, leadingIcon = { Icon(Icons.Outlined.StarOutline, null) })
        val filtered = notes.filter { (type == "All notes" || it.type == type) && (!favourites || it.favourite) && "${it.body} ${it.tags}".contains(query, true) }
        if (filtered.isEmpty()) EmptyState(L10n.text(R.string.msg_16fdb77b4290), L10n.text(R.string.msg_5c1057266302), Icons.Outlined.EditNote)
        filtered.forEach { n -> Paper(Modifier.fillMaxWidth().clickable { edit(n) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.label(n.type).uppercase(), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                IconButton({ favourite(n.copy(favourite = !n.favourite)) }) { Icon(if (n.favourite) Icons.Outlined.Star else Icons.Outlined.StarOutline, if (n.favourite) L10n.text(R.string.msg_5b81c16b71d4) else L10n.text(R.string.msg_68838f237792), tint = MaterialTheme.colorScheme.secondary) }
            }
            Text(n.body, style = if (n.type == "Quote") MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp)); Text(listOfNotNull(books.find { it.id == n.bookId }?.title, n.location.takeIf { it.isNotBlank() }, n.tags.takeIf { it.isNotBlank() }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
}
