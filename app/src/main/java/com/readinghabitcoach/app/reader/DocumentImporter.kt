package com.readinghabitcoach.app.reader

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.readinghabitcoach.app.data.Book
import com.readinghabitcoach.app.data.newId
import java.io.*
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArchiveGuard {
    const val MAX_FILE = 100L * 1024 * 1024
    fun safeName(name: String): Boolean = name.isNotBlank() && !name.startsWith('/') && !name.startsWith('\\') &&
        !name.contains('\\') && !name.contains(':') && name.split('/').none { it == ".." }
    fun validateEpub(file: File) {
        ZipFile(file).use { zip ->
            val names = mutableSetOf<String>(); var total = 0L; var count = 0
            zip.entries().asSequence().forEach { e ->
                require(++count <= 10_000) { "This EPUB contains too many entries." }
                require(safeName(e.name) && names.add(e.name)) { "This EPUB contains unsafe or duplicate paths." }
                require(e.size in 0..(20L * 1024 * 1024)) { "This EPUB contains an oversized entry." }
                require(e.name != "META-INF/encryption.xml") { "Encrypted or DRM-protected EPUB files are not supported." }
                if (!e.isDirectory) zip.getInputStream(e).use { input ->
                    val buffer = ByteArray(8192); var size = 0L
                    while (true) { val n = input.read(buffer); if (n < 0) break; size += n; total += n
                        require(size <= 20L * 1024 * 1024 && total <= 200L * 1024 * 1024) { "The expanded EPUB is too large." }
                    }
                    require(e.compressedSize <= 0 || size / e.compressedSize.coerceAtLeast(1) <= 200) { "The EPUB compression ratio is unsafe." }
                }
            }
            require(zip.getEntry("META-INF/container.xml") != null && zip.getEntry("mimetype") != null) { "This is not a valid EPUB." }
            require(zip.getInputStream(zip.getEntry("mimetype")).bufferedReader().use { it.readText() }.trim() == "application/epub+zip") { "Unsupported EPUB type." }
        }
    }
}
class DocumentImporter(private val context: Context) {
    suspend fun import(uri: Uri): Book = withContext(Dispatchers.IO) {
        val name = runCatching { context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull() ?: uri.lastPathSegment ?: "My document"
        val type = context.contentResolver.getType(uri)
        val pdf = name.endsWith(".pdf", true) || type == "application/pdf"
        require(pdf || name.endsWith(".epub", true) || type == "application/epub+zip") { "Choose a PDF or DRM-free EPUB document." }
        val directory = File(context.filesDir, "documents").apply { mkdirs() }
        val destination = File(directory, "${newId()}.${if (pdf) "pdf" else "epub"}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> destination.outputStream().use { out ->
                val buffer = ByteArray(8192); var size = 0L
                while (true) { val n = input.read(buffer); if (n < 0) break; size += n; require(size <= ArchiveGuard.MAX_FILE) { "Documents must be under 100 MB." }; out.write(buffer, 0, n) }
            } } ?: error("The document could not be opened.")
            val pages = if (pdf) ParcelFileDescriptor.open(destination, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { it.pageCount } }
                else { ArchiveGuard.validateEpub(destination); 0 }
            Book(title = name.substringBeforeLast('.'), format = if (pdf) "PDF" else "EPUB", shelf = "Reading", totalPages = pages, document = destination.name, colour = name.hashCode())
        } catch (e: Exception) { destination.delete(); throw e }
    }
    fun file(book: Book): File {
        require(book.document.matches(Regex("[a-fA-F0-9-]+\\.(pdf|epub)"))) { "The document reference is invalid." }
        return File(File(context.filesDir, "documents"), book.document).also { require(it.exists()) { "The original document is missing. Import it again to read it." } }
    }
}
