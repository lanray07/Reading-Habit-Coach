package com.readinghabitcoach.app.data

import android.content.Context
import android.net.Uri
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import org.json.*
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataTransfer(private val context: Context, private val db: CoachDatabase) {
    private val dao get() = db.dao()
    suspend fun json(): String = db.withTransaction {
        val root = JSONObject().put("schemaVersion", 1).put("exportedAt", System.currentTimeMillis()).put("documentsIncluded", false)
        root.put("books", JSONArray().apply { dao.allBooks().forEach { b -> put(JSONObject().put("id", b.id).put("title", b.title).put("author", b.author).put("format", b.format).put("shelf", b.shelf).put("totalPages", b.totalPages).put("page", b.page).put("genre", b.genre).put("tags", b.tags).put("rating", b.rating).put("isbn", b.isbn).put("addedAt", b.addedAt).put("finishedAt", b.finishedAt).put("colour", b.colour)) } })
        root.put("sessions", JSONArray().apply { dao.allSessions().forEach { s -> put(JSONObject().put("id", s.id).put("bookId", s.bookId).put("startedAt", s.startedAt).put("endedAt", s.endedAt).put("durationMs", s.durationMs).put("startPage", s.startPage).put("endPage", s.endPage).put("zone", s.zone).put("note", s.note).put("mood", s.mood).put("spans", s.spans)) } })
        root.put("notes", JSONArray().apply { dao.allNotes().forEach { n -> put(JSONObject().put("id", n.id).put("bookId", n.bookId).put("type", n.type).put("body", n.body).put("location", n.location).put("tags", n.tags).put("favourite", n.favourite).put("createdAt", n.createdAt).put("updatedAt", n.updatedAt)) } })
        root.put("policies", JSONArray().apply { dao.allPolicies().forEach { p -> put(JSONObject().put("effectiveWeek", p.effectiveWeek).put("daysPerWeek", p.daysPerWeek).put("paused", p.paused)) } })
        root.toString(2)
    }
    suspend fun markdown(): String {
        val books = dao.allBooks().associateBy { it.id }
        return "# My reading journal\n\n" + dao.allNotes().joinToString("\n\n---\n\n") { n -> "## ${n.type} — ${books[n.bookId]?.title ?: "Journal"}\n\n${n.body}\n\n${n.location}\n\nTags: ${n.tags}" }
    }
    suspend fun csv(): String {
        fun cell(s: String): String { val safe = if (s.firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$s" else s; return "\"${safe.replace("\"", "\"\"")}\"" }
        val books = dao.allBooks().associateBy { it.id }
        return "book,started_at,ended_at,duration_minutes,start_page,end_page,zone,note\r\n" + dao.allSessions().joinToString("\r\n") { s ->
            listOf(books[s.bookId]?.title ?: "", s.startedAt.toString(), s.endedAt.toString(), (s.durationMs / 60000.0).toString(), s.startPage.toString(), s.endPage.toString(), s.zone, s.note).joinToString(",", transform = ::cell)
        }
    }
    suspend fun write(uri: Uri, kind: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            if (kind == "pdf") {
                val text = markdown(); val pdf = PdfDocument(); val paint = Paint().apply { textSize = 12f; isAntiAlias = true }
                val lines = text.lines().flatMap { line -> if (line.isEmpty()) listOf("") else line.chunked(85) }
                lines.chunked(43).forEachIndexed { index, chunk -> val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                    chunk.forEachIndexed { i, line -> page.canvas.drawText(line, 36f, 48f + i * 17, paint) }; pdf.finishPage(page) }
                pdf.writeTo(out); pdf.close()
            } else out.write((when (kind) { "csv" -> csv(); "md", "txt" -> markdown(); else -> json() }).toByteArray(Charsets.UTF_8))
        } ?: error("The selected destination could not be written.")
    }
    suspend fun importJson(uri: Uri) = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(10 * 1024 * 1024) } ?: error("Cannot read backup.")
        importBytes(bytes)
    }
    suspend fun writeEncrypted(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        try { val plain=json().toByteArray(Charsets.UTF_8)
            try { val encrypted=BackupCipher.encrypt(plain,password); context.contentResolver.openOutputStream(uri,"wt")?.use { it.write(encrypted) } ?: error("Cannot write backup.") }
            finally { plain.fill(0) }
        } finally { password.fill('\u0000') }
    }
    suspend fun importEncrypted(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        try { val envelope=context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(11*1024*1024) } ?: error("Cannot read backup.")
            val plain=BackupCipher.decrypt(envelope,password)
            try { importBytes(plain) } finally { plain.fill(0) }
        } finally { password.fill('\u0000') }
    }
    private suspend fun importBytes(bytes: ByteArray) {
        val root = JSONObject(bytes.toString(Charsets.UTF_8)); require(root.getInt("schemaVersion") == 1) { "Unsupported backup version." }
        fun array(key: String): List<JSONObject> = root.optJSONArray(key)?.let { a -> require(a.length() <= 50_000); (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
        val books = array("books").map { b -> Book(id = b.getString("id"), title = b.getString("title").take(1000), author = b.optString("author"), format = b.getString("format"), shelf = b.getString("shelf"), totalPages = b.optInt("totalPages"), page = b.optInt("page"), genre = b.optString("genre"), tags = b.optString("tags"), rating = b.optInt("rating"), isbn = b.optString("isbn"), addedAt = b.optLong("addedAt"), finishedAt = if (b.has("finishedAt") && !b.isNull("finishedAt")) b.getLong("finishedAt") else null, colour = b.optInt("colour")).also { require(it.title.isNotBlank() && it.format in formats && it.shelf in shelves && it.page >= 0 && it.totalPages >= 0 && it.rating in 0..5) } }
        val ids = (books.map { it.id } + dao.allBooks().map { it.id }).toSet()
        val sessions = array("sessions").map { s -> ReadingSession(s.getString("id"), s.getString("bookId"), s.getLong("startedAt"), s.getLong("endedAt"), s.getLong("durationMs"), s.optInt("startPage"), s.optInt("endPage"), s.getString("zone"), s.optString("note"), s.optString("mood"), s.optString("spans")).also { require(it.bookId in ids && it.durationMs in 0..86_400_000 && it.endedAt >= it.startedAt && it.startPage >= 0 && it.endPage >= it.startPage); ZoneId.of(it.zone)
            val spans = com.readinghabitcoach.app.domain.decodeSpans(it.spans)
            require(it.spans.isBlank() || (spans.isNotEmpty() && spans.sumOf { s -> s.duration } == it.durationMs && spans.all { s -> s.start >= it.startedAt && s.end <= it.endedAt } && spans.zipWithNext().all { (a,b) -> a.end <= b.start })) { "Invalid session time intervals." }
        } }
        val notes = array("notes").map { n -> JournalNote(n.getString("id"), n.optString("bookId"), n.optString("type", "Idea"), n.getString("body"), n.optString("location"), n.optString("tags"), n.optBoolean("favourite"), n.getLong("createdAt"), n.getLong("updatedAt")).also { require(it.body.isNotBlank() && (it.bookId.isBlank() || it.bookId in ids)) } }
        val policies = array("policies").map { p -> GoalPolicy(p.getLong("effectiveWeek"), p.getInt("daysPerWeek"), p.optBoolean("paused")).also { require(it.daysPerWeek in 1..7 && it.effectiveWeek in 0..java.time.LocalDate.now().plusYears(10).toEpochDay()) { "Invalid goal policy date or target." } } }
        db.withTransaction {
            // Merge by ID; document references from existing local books remain private and valid.
            books.forEach { b -> val old = dao.book(b.id); dao.saveBook(if (old != null) b.copy(document = old.document, readerPosition = old.readerPosition) else b) }
            sessions.forEach { dao.saveSession(it) }; notes.forEach { dao.saveNote(it) }; policies.forEach { dao.savePolicy(it) }
        }
    }
}
fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); var size = 0
    while (true) { val n = read(buffer); if (n < 0) break; size += n; require(size <= max) { "The file is too large." }; out.write(buffer, 0, n) }
    return out.toByteArray()
}
