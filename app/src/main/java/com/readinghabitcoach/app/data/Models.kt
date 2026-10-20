package com.readinghabitcoach.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
val shelves = listOf("Want to Read", "Reading", "Finished", "Paused", "Abandoned")
val formats = listOf("Physical", "EPUB", "PDF", "Audiobook", "Library ebook")
val noteTypes = listOf("Idea", "Quote", "Question", "Reflection", "Summary", "Vocabulary", "To Research")

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String = newId(), val title: String, val author: String = "",
    val format: String = "Physical", val shelf: String = "Want to Read", val totalPages: Int = 0,
    val page: Int = 0, val genre: String = "", val tags: String = "", val rating: Int = 0,
    val isbn: String = "", val document: String = "", val cover: String = "",
    val addedAt: Long = System.currentTimeMillis(), val finishedAt: Long? = null,
    val readerPosition: String = "", val colour: Int = 0
)

@Entity(tableName = "sessions", indices = [Index("bookId"), Index("startedAt")])
data class ReadingSession(
    @PrimaryKey val id: String = newId(), val bookId: String, val startedAt: Long,
    val endedAt: Long, val durationMs: Long, val startPage: Int = 0, val endPage: Int = 0,
    val zone: String, val note: String = "", val mood: String = "",
    // Only active time intervals; paused time never contributes to day totals.
    val spans: String = "", @ColumnInfo(defaultValue = "0") val manual: Boolean = false
)

@Entity(tableName = "notes", indices = [Index("bookId")])
data class JournalNote(
    @PrimaryKey val id: String = newId(), val bookId: String = "", val type: String = "Idea",
    val body: String, val location: String = "", val tags: String = "", val favourite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "goal_policies")
data class GoalPolicy(@PrimaryKey val effectiveWeek: Long, val daysPerWeek: Int = 4, val paused: Boolean = false)

@Entity(tableName = "active_session")
data class ActiveSession(
    @PrimaryKey val id: Int = 1, val bookId: String, val startedAt: Long,
    val startPage: Int, val spans: String = "", val anchorEpoch: Long? = null,
    val anchorElapsed: Long = 0, val bootCount: Int = 0, val zone: String = java.time.ZoneId.systemDefault().id
)

@Dao
interface CoachDao {
    @Query("SELECT * FROM books ORDER BY addedAt DESC") fun books(): Flow<List<Book>>
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC") fun sessions(): Flow<List<ReadingSession>>
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC") fun notes(): Flow<List<JournalNote>>
    @Query("SELECT * FROM goal_policies ORDER BY effectiveWeek") fun policies(): Flow<List<GoalPolicy>>
    @Query("SELECT * FROM active_session LIMIT 1") fun active(): Flow<ActiveSession?>
    @Upsert suspend fun saveBook(book: Book)
    @Upsert suspend fun saveSession(session: ReadingSession)
    @Upsert suspend fun saveNote(note: JournalNote)
    @Upsert suspend fun savePolicy(policy: GoalPolicy)
    @Upsert suspend fun saveActive(active: ActiveSession)
    @Query("DELETE FROM active_session") suspend fun clearActive()
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: String)
    @Query("SELECT * FROM books WHERE id = :id LIMIT 1") suspend fun book(id: String): Book?
    @Query("SELECT * FROM books") suspend fun allBooks(): List<Book>
    @Query("SELECT * FROM sessions") suspend fun allSessions(): List<ReadingSession>
    @Query("SELECT * FROM notes") suspend fun allNotes(): List<JournalNote>
    @Query("SELECT * FROM goal_policies") suspend fun allPolicies(): List<GoalPolicy>
    @Query("DELETE FROM books") suspend fun clearBooks()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM notes") suspend fun clearNotes()
    @Query("DELETE FROM goal_policies") suspend fun clearPolicies()
}

@Database(entities = [Book::class, ReadingSession::class, JournalNote::class, GoalPolicy::class, ActiveSession::class], version = 2, exportSchema = true)
abstract class CoachDatabase : RoomDatabase() { abstract fun dao(): CoachDao }
