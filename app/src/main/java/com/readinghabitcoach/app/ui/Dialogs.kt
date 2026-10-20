package com.readinghabitcoach.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.readinghabitcoach.app.data.*
import java.time.LocalDate
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable fun EditorDialog(title: String, dismiss: () -> Unit, save: () -> Unit, enabled: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Dialog(dismiss) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.widthIn(max = 640.dp).padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(14.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(dismiss) { Text("Cancel") }; Spacer(Modifier.width(8.dp)); Primary("Save", Icons.Outlined.Check, enabled, save)
                }
            }
        }
    }
}
@Composable fun BookEditor(existing: Book?, dismiss: () -> Unit, save: (Book) -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }; var author by remember { mutableStateOf(existing?.author ?: "") }
    var format by remember { mutableStateOf(existing?.format ?: "Physical") }; var shelf by remember { mutableStateOf(existing?.shelf ?: "Want to Read") }
    var total by remember { mutableStateOf(existing?.totalPages?.takeIf { it > 0 }?.toString() ?: "") }; var page by remember { mutableStateOf(existing?.page?.toString() ?: "0") }
    var genre by remember { mutableStateOf(existing?.genre ?: "") }; var tags by remember { mutableStateOf(existing?.tags ?: "") }; var isbn by remember { mutableStateOf(existing?.isbn ?: "") }
    var rating by remember { mutableIntStateOf(existing?.rating ?: 0) }
    val valid = title.isNotBlank() && (page.toIntOrNull() ?: -1) >= 0 && (total.isBlank() || (total.toIntOrNull() ?: -1) >= (page.toIntOrNull() ?: 0))
    EditorDialog(if (existing == null) "A new book for your shelf" else "Edit book", dismiss, {
        val base = existing ?: Book(title = title.trim(), colour = title.hashCode())
        save(base.copy(title = title.trim(), author = author.trim(), format = format, shelf = shelf, totalPages = total.toIntOrNull() ?: 0,
            page = page.toIntOrNull() ?: 0, genre = genre, tags = tags, isbn = isbn, rating = rating,
            finishedAt = if (shelf == "Finished") base.finishedAt ?: System.currentTimeMillis() else null))
    }, valid) {
        Field(title, { title = it }, "Book title *"); Field(author, { author = it }, "Author")
        Text("Format", style = MaterialTheme.typography.labelLarge); ChoiceRow(formats, format) { format = it }
        Text("Shelf", style = MaterialTheme.typography.labelLarge); ChoiceRow(shelves, shelf) { shelf = it }
        if (format != "Audiobook") Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(total, { total = it.filter(Char::isDigit).take(6) }, "Total pages", Modifier.weight(1f))
            Field(page, { page = it.filter(Char::isDigit).take(6) }, "Current page", Modifier.weight(1f))
        }
        Field(genre, { genre = it }, "Genre"); Field(tags, { tags = it }, "Tags, separated by commas"); Field(isbn, { isbn = it }, "ISBN (optional)")
        Text("Your rating (optional)"); Row { (1..5).forEach { n -> IconButton({ rating = if (rating == n) 0 else n }) { Icon(if (n <= rating) Icons.Outlined.Star else Icons.Outlined.StarOutline, "$n stars", tint = MaterialTheme.colorScheme.secondary) } } }
    }
}
@Composable fun BookDetails(book: Book, sessions: List<ReadingSession>, dismiss: () -> Unit, edit: () -> Unit, start: () -> Unit, reader: () -> Unit, addNote: () -> Unit) {
    Dialog(dismiss) { Paper(Modifier.widthIn(max = 600.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { BookCover(book, 110, 166); Column(Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(8.dp)); Text(book.author)
            Spacer(Modifier.height(8.dp)); Text("${book.shelf} · ${book.format}", color = MaterialTheme.colorScheme.secondary)
            if (book.rating > 0) Text("${book.rating} / 5 · Your rating")
        } }
        Spacer(Modifier.height(20.dp)); Progress(if (book.totalPages > 0) book.page.toFloat() / book.totalPages else 0f, if (book.totalPages > 0) "${book.page} of ${book.totalPages} pages" else "Progress recorded with your reading sessions")
        Spacer(Modifier.height(14.dp)); Text("${sessions.size} sessions · ${durationLabel(sessions.sumOf { it.durationMs })}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (book.tags.isNotBlank()) Text("Tags · ${book.tags}")
        Spacer(Modifier.height(20.dp)); Primary("Start Reading Session", onClick = start)
        if (book.document.isNotBlank()) TextButton(reader) { Icon(Icons.Outlined.MenuBook, null); Text(" Open document") }
        Row { TextButton(edit) { Text("Edit details") }; TextButton(addNote) { Text("Add a note") }; TextButton(dismiss) { Text("Close") } }
    } }
}
@Composable fun NoteEditor(existing: JournalNote?, books: List<Book>, selected: String, dismiss: () -> Unit, save: (JournalNote) -> Unit, delete: (JournalNote) -> Unit) {
    var body by remember { mutableStateOf(existing?.body ?: "") }; var type by remember { mutableStateOf(existing?.type ?: "Idea") }
    var bookId by remember { mutableStateOf(existing?.bookId ?: selected) }; var location by remember { mutableStateOf(existing?.location ?: "") }; var tags by remember { mutableStateOf(existing?.tags ?: "") }
    var confirm by remember { mutableStateOf(false) }
    EditorDialog(if (existing == null) "Keep a thought" else "Your journal entry", dismiss, {
        save((existing ?: JournalNote(body = body)).copy(body = body.trim(), type = type, bookId = bookId, location = location, tags = tags))
    }, body.isNotBlank()) {
        ChoiceRow(noteTypes, type) { type = it }
        BookPicker(books, bookId, { bookId = it }, allowNone = true)
        Field(body, { body = it }, "What would you like to remember?", Modifier.heightIn(min = 160.dp), single = false)
        Field(location, { location = it }, "Page, chapter or location"); Field(tags, { tags = it }, "Tags")
        if (existing != null) TextButton({ confirm = true }) { Text("Delete note", color = MaterialTheme.colorScheme.error) }
    }
    if (confirm) AlertDialog({ confirm = false }, title = { Text("Delete this note?") }, text = { Text("This permanently removes the note from your journal.") },
        confirmButton = { TextButton({ existing?.let(delete); confirm = false }) { Text("Delete") } }, dismissButton = { TextButton({ confirm = false }) { Text("Keep note") } })
}
@Composable fun BookPicker(books: List<Book>, selected: String, change: (String) -> Unit, allowNone: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ expanded = true }, Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text(books.find { it.id == selected }?.title ?: if (allowNone) "General journal entry" else "Choose a book"); Icon(Icons.Outlined.ExpandMore, null) }
        DropdownMenu(expanded, { expanded = false }) {
            if (allowNone) DropdownMenuItem({ Text("General journal entry") }, { change(""); expanded = false })
            books.forEach { book -> DropdownMenuItem({ Text(book.title) }, { change(book.id); expanded = false }) }
        }
    }
}
@Composable fun ManualLog(books: List<Book>, initialMinutes: Int = 20, dismiss: () -> Unit, save: (Book, Int, LocalDate, Int, Int, String, String) -> Unit) {
    var bookId by remember { mutableStateOf(books.firstOrNull { it.shelf == "Reading" }?.id ?: books.firstOrNull()?.id ?: "") }
    val book = books.find { it.id == bookId }
    var minutes by remember { mutableStateOf(initialMinutes.toString()) }; var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var start by remember(bookId) { mutableStateOf(book?.page?.toString() ?: "0") }; var end by remember(bookId) { mutableStateOf(book?.page?.toString() ?: "0") }
    var note by remember { mutableStateOf("") }; var mood by remember { mutableStateOf("") }
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    val a = start.toIntOrNull() ?: -1; val b = end.toIntOrNull() ?: -1; val n = minutes.toIntOrNull() ?: 0
    val valid = book != null && n in 1..1440 && day != null && day <= LocalDate.now() && a >= 0 && b >= a && (book.totalPages == 0 || b <= book.totalPages)
    EditorDialog("A little reading to log", dismiss, { if (valid) save(book!!, n, day!!, a, b, note, mood) }, valid) {
        BookPicker(books, bookId, { bookId = it })
        ChoiceRow(listOf("10", "20", "30", "45", "60"), minutes) { minutes = it }
        Field(minutes, { minutes = it.filter(Char::isDigit).take(4) }, "Minutes read *")
        Field(date, { date = it }, "Date (YYYY-MM-DD)")
        if (book?.format != "Audiobook") Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(start, { start = it.filter(Char::isDigit).take(6) }, "Start page", Modifier.weight(1f))
            Field(end, { end = it.filter(Char::isDigit).take(6) }, "End page", Modifier.weight(1f))
        }
        Field(mood, { mood = it }, "Mood (optional)"); Field(note, { note = it }, "Session note (optional)", single = false)
        if (!valid) Text("Choose a book, 1–1,440 minutes, a valid date and forward page progress.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun PasswordPrompt(creating: Boolean, dismiss: () -> Unit, confirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }; var repeat by remember { mutableStateOf("") }
    AlertDialog(dismiss, title={ Text(if (creating) "Protect your local backup" else "Unlock your backup") }, text={ Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(if (creating) "Choose a passphrase with at least 12 characters. There is no password recovery. Books and document files are not included." else "Enter the passphrase used to create this backup. Records will be merged by ID.")
        OutlinedTextField(password,{password=it.take(4096)},label={Text("Passphrase")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
        if(creating) OutlinedTextField(repeat,{repeat=it.take(4096)},label={Text("Repeat passphrase")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
    } }, confirmButton={ TextButton({confirm(password)},enabled=if(creating) password.length>=12&&password==repeat else password.isNotEmpty()){Text(if(creating)"Create backup" else "Restore")} },dismissButton={TextButton(dismiss){Text("Cancel")}})
}
