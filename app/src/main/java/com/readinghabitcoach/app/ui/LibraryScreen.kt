package com.readinghabitcoach.app.ui

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
        PageTitle("Your personal library", "All your books. A place for every chapter.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Primary("Add a book", Icons.Outlined.Add, onClick = add); OutlinedButton(import, Modifier.heightIn(min = 50.dp)) { Icon(Icons.Outlined.FileOpen, null); Text(" Import document") } }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search by title, author, ISBN, genre or tag") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
        ChoiceRow(listOf("All books") + shelves, shelf) { shelf = it }
        ChoiceRow(listOf("All formats") + formats, format) { format = it }
        val filtered = books.filter { (shelf == "All books" || it.shelf == shelf) && (format == "All formats" || it.format == format) &&
            "${it.title} ${it.author} ${it.isbn} ${it.genre} ${it.tags}".contains(query, true) }
        Text("${filtered.size} ${if (filtered.size == 1) "book" else "books"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (filtered.isEmpty()) EmptyState("Room for your next favourite", "Add a book or try another search.", action = { Primary("Add a book", Icons.Outlined.Add, onClick = add) })
        BoxWithConstraints {
            val columns = if (maxWidth > 900.dp) 3 else if (maxWidth > 560.dp) 2 else 1
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                filtered.chunked(columns).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { book -> Paper(Modifier.weight(1f).clickable { detail(book) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            BookCover(book)
                            Column(Modifier.weight(1f)) {
                                Text(book.shelf, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp)); Text(book.title, style = MaterialTheme.typography.titleLarge)
                                Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(14.dp)); Text(book.format, style = MaterialTheme.typography.bodyMedium)
                                if (book.totalPages > 0) { Spacer(Modifier.height(8.dp)); Progress(book.page.toFloat() / book.totalPages, "${book.page} / ${book.totalPages} pages") }
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
        PageTitle("Your reading journal", "Good ideas deserve a place to stay.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Primary("Capture a thought", Icons.Outlined.EditNote, onClick = add); OutlinedButton(export, Modifier.heightIn(min = 50.dp)) { Text("Export notes") } }
        Field(query, { query = it }, "Search notes, quotes and tags")
        ChoiceRow(listOf("All notes") + noteTypes, type) { type = it }
        FilterChip(favourites, { favourites = !favourites }, label = { Text("Favourites") }, leadingIcon = { Icon(Icons.Outlined.StarOutline, null) })
        val filtered = notes.filter { (type == "All notes" || it.type == type) && (!favourites || it.favourite) && "${it.body} ${it.tags}".contains(query, true) }
        if (filtered.isEmpty()) EmptyState("A thought worth remembering", "Keep an idea, a quote or a question here.", Icons.Outlined.EditNote)
        filtered.forEach { n -> Paper(Modifier.fillMaxWidth().clickable { edit(n) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.type.uppercase(), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                IconButton({ favourite(n.copy(favourite = !n.favourite)) }) { Icon(if (n.favourite) Icons.Outlined.Star else Icons.Outlined.StarOutline, if (n.favourite) "Remove favourite" else "Favourite note", tint = MaterialTheme.colorScheme.secondary) }
            }
            Text(n.body, style = if (n.type == "Quote") MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp)); Text(listOfNotNull(books.find { it.id == n.bookId }?.title, n.location.takeIf { it.isNotBlank() }, n.tags.takeIf { it.isNotBlank() }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
}
