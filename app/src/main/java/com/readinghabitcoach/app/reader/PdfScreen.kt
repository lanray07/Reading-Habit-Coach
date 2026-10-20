package com.readinghabitcoach.app.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.readinghabitcoach.app.data.Book
import com.readinghabitcoach.app.data.JournalNote
import com.readinghabitcoach.app.ui.*
import kotlinx.coroutines.*

@Composable fun PdfScreen(book: Book, save: (Book) -> Unit, bookmark: (JournalNote) -> Unit, close: () -> Unit) {
    val context = LocalContext.current
    var page by remember(book.id) { mutableIntStateOf(book.readerPosition.toIntOrNull() ?: 0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }; var count by remember { mutableIntStateOf(0) }; var error by remember { mutableStateOf("") }
    var zoom by remember { mutableFloatStateOf(1f) }; var dx by remember { mutableFloatStateOf(0f) }; var dy by remember { mutableFloatStateOf(0f) }
    var jump by remember { mutableStateOf("") }
    LaunchedEffect(page) {
        try {
            val rendered = withContext(Dispatchers.IO) {
                val file = DocumentImporter(context).file(book)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { pdf ->
                    count = pdf.pageCount
                    pdf.openPage(page.coerceIn(0, count - 1)).use { p ->
                        val scale = minOf(1600f / p.width, 2000f / p.height, 3f)
                        Bitmap.createBitmap((p.width * scale).toInt().coerceAtLeast(1), (p.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE); p.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                    }
                } }
            }
            bitmap = rendered; zoom = 1f; dx = 0f; dy = 0f
            save(book.copy(readerPosition = page.toString(), page = (page + 1).coerceAtMost(count)))
        } catch (e: Exception) { error = e.message ?: "This PDF could not be rendered." }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(close) { Icon(Icons.Outlined.ArrowBack, "Close reader") }
            Text(book.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton({ bookmark(JournalNote(bookId = book.id, type = "Reflection", body = "Bookmark · page ${page + 1}", location = "pdf:page:${page + 1}")) }) { Icon(Icons.Outlined.BookmarkAdd, "Bookmark page") }
            IconButton({ zoom = (zoom + .25f).coerceAtMost(4f) }) { Icon(Icons.Outlined.ZoomIn, "Zoom in") }
            IconButton({ zoom = (zoom - .25f).coerceAtLeast(1f) }) { Icon(Icons.Outlined.ZoomOut, "Zoom out") }
        }
        if (error.isNotBlank()) Text(error, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
        Box(Modifier.weight(1f).fillMaxWidth().clipToBoundsForPdf().pointerInput(Unit) { detectTransformGestures { _, pan, z, _ -> zoom = (zoom * z).coerceIn(1f, 4f); dx = (dx + pan.x).coerceIn(-2000f, 2000f); dy = (dy + pan.y).coerceIn(-2500f, 2500f) } }, contentAlignment = Alignment.Center) {
            bitmap?.let { Image(it.asImageBitmap(), "PDF page ${page + 1} of $count", Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = dx, translationY = dy)) }
        }
        Text("PDF image view · text selection, search and read-aloud are unavailable in this renderer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (page > 0) page-- }, enabled = page > 0) { Icon(Icons.Outlined.ChevronLeft, "Previous page") }
            Text("${page + 1} / $count", Modifier.padding(horizontal = 16.dp))
            IconButton({ if (page < count - 1) page++ }, enabled = page < count - 1) { Icon(Icons.Outlined.ChevronRight, "Next page") }
            OutlinedTextField(jump, { jump = it.filter(Char::isDigit).take(5) }, Modifier.width(100.dp), label = { Text("Page") }, singleLine = true)
            TextButton({ jump.toIntOrNull()?.takeIf { it in 1..count }?.let { page = it - 1 } }) { Text("Go") }
        }
    }
}
private fun Modifier.clipToBoundsForPdf() = this.then(Modifier.graphicsLayer { clip = true })
