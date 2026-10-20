package com.readinghabitcoach.app.reader

import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.R

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
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

object ArchiveGuard {
    const val MAX_FILE = 100L * 1024 * 1024
    fun safeName(name: String): Boolean = name.isNotBlank() && !name.startsWith('/') && !name.startsWith('\\') &&
        !name.contains('\\') && !name.contains(':') && name.split('/').none { it == ".." }
    fun validateEpub(file: File) {
        ZipFile(file).use { zip ->
            val names = mutableSetOf<String>(); var total = 0L; var count = 0
            zip.entries().asSequence().forEach { e ->
                require(++count <= 10_000) { L10n.text(R.string.msg_73425be4d292) }
                require(safeName(e.name) && names.add(e.name)) { L10n.text(R.string.msg_f208457d96a6) }
                require(e.size in 0..(20L * 1024 * 1024)) { L10n.text(R.string.msg_afce48e14ca7) }
                require(e.name != "META-INF/encryption.xml") { L10n.text(R.string.msg_e518d113bc41) }
                if (!e.isDirectory) zip.getInputStream(e).use { input ->
                    val buffer = ByteArray(8192); var size = 0L
                    while (true) { val n = input.read(buffer); if (n < 0) break; size += n; total += n
                        require(size <= 20L * 1024 * 1024 && total <= 200L * 1024 * 1024) { L10n.text(R.string.msg_3da582366d2f) }
                    }
                    require(e.compressedSize <= 0 || size / e.compressedSize.coerceAtLeast(1) <= 200) { L10n.text(R.string.msg_995b8e4bd44e) }
                }
            }
            require(zip.getEntry("META-INF/container.xml") != null && zip.getEntry("mimetype") != null) { L10n.text(R.string.msg_dd12e852719e) }
            require(zip.getInputStream(zip.getEntry("mimetype")).bufferedReader().use { it.readText() }.trim() == "application/epub+zip") { L10n.text(R.string.msg_079a9e85d728) }
        }
    }
}
class DocumentImporter(private val context: Context) {
    suspend fun import(uri: Uri): Book = withContext(Dispatchers.IO) {
        val name = runCatching { context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull() ?: uri.lastPathSegment ?: L10n.text(R.string.msg_2cee8de7b790)
        val type = context.contentResolver.getType(uri)
        val pdf = name.endsWith(".pdf", true) || type == "application/pdf"
        require(pdf || name.endsWith(".epub", true) || type == "application/epub+zip") { L10n.text(R.string.msg_5c978abb3039) }
        val directory = File(context.filesDir, "documents").apply { mkdirs() }
        val destination = File(directory, "${newId()}.${if (pdf) "pdf" else "epub"}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> destination.outputStream().use { out ->
                val buffer = ByteArray(8192); var size = 0L
                while (true) { val n = input.read(buffer); if (n < 0) break; size += n; require(size <= ArchiveGuard.MAX_FILE) { L10n.text(R.string.msg_3d04e71143cd) }; out.write(buffer, 0, n) }
            } } ?: error(L10n.text(R.string.msg_b68ba94c9352))
            val pages = if (pdf) ParcelFileDescriptor.open(destination, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { it.pageCount } }
                else { ArchiveGuard.validateEpub(destination); 0 }
            val metadata = if (pdf) null else epubMetadata(destination)
            Book(title = metadata?.first?.takeIf { it.isNotBlank() } ?: name.substringBeforeLast('.'), author = metadata?.second.orEmpty(), format = if (pdf) "PDF" else "EPUB", shelf = "Reading", totalPages = pages, document = destination.name, colour = name.hashCode())
        } catch (e: Exception) { destination.delete(); throw e }
    }
    private suspend fun epubMetadata(file: File): Pair<String, String> {
        val http = DefaultHttpClient()
        val assets = AssetRetriever(context.contentResolver, http)
        val asset = assets.retrieve(file.toUrl()).getOrNull() ?: error(L10n.text(R.string.msg_4e4754e67dbe))
        val publication = PublicationOpener(DefaultPublicationParser(context, httpClient = http, assetRetriever = assets, pdfFactory = null))
            .open(asset, allowUserInteraction = false).getOrNull() ?: error(L10n.text(R.string.msg_14b7ff9550e5))
        return try { publication.metadata.title.orEmpty() to publication.metadata.authors.joinToString(", ") { it.name } }
            finally { publication.close() }
    }
    fun file(book: Book): File {
        require(book.document.matches(Regex("[a-fA-F0-9-]+\\.(pdf|epub)"))) { L10n.text(R.string.msg_8b2ff052f8d1) }
        return File(File(context.filesDir, "documents"), book.document).also { require(it.exists()) { L10n.text(R.string.msg_414c36c4ebc7) } }
    }
}
