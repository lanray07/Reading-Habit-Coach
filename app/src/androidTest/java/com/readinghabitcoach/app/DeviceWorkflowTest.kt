package com.readinghabitcoach.app

import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.readinghabitcoach.app.data.*
import com.readinghabitcoach.app.reader.*
import com.readinghabitcoach.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.*

@RunWith(AndroidJUnit4::class)
class DeviceWorkflowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    @Test fun roomStoresLibrarySessionsNotesAndGoalHistory() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(context, CoachDatabase::class.java).build()
        try {
            val dao=db.dao(); val book=Book(title="Original test record", totalPages=150, shelf="Reading")
            dao.saveBook(book); dao.saveBook(book.copy(page=30))
            dao.saveSession(ReadingSession(bookId=book.id,startedAt=1000,endedAt=601000,durationMs=600000,zone="UTC",spans=encodeSpans(listOf(Span(1000,601000)))))
            dao.saveNote(JournalNote(bookId=book.id,body="A private thought",type="Reflection"))
            dao.savePolicy(GoalPolicy(LocalDate.of(2026,10,19).toEpochDay(),4))
            dao.savePolicy(GoalPolicy(LocalDate.of(2026,10,26).toEpochDay(),2,true))
            assertEquals(30,dao.books().first().single().page)
            assertEquals(600000L,dao.sessions().first().single().durationMs)
            assertEquals("A private thought",dao.notes().first().single().body)
            assertEquals(2,dao.policies().first().size)
            dao.saveSession(ReadingSession(bookId=book.id,startedAt=1000,endedAt=1000,durationMs=1_200_000,zone="UTC",manual=true))
            val transfer=DataTransfer(context,db)
            val backup=transfer.json(); assertFalse(backup.contains("document\"")); assertTrue(backup.contains("\"documentsIncluded\": false"))
            val f=File(context.cacheDir,"roundtrip.json").apply { writeText(backup) }
            transfer.importJson(Uri.fromFile(f)); assertEquals(1,dao.books().first().size); assertEquals(1,dao.notes().first().size)
            assertTrue(dao.sessions().first().any { it.manual && it.durationMs==1_200_000L }); assertTrue(transfer.csv().contains("manual"))
            f.delete()
        } finally { db.close() }
    }
    @Test fun realDocumentImportAndPdfRenderer() = runBlocking<Unit> {
        val pdf = android.graphics.pdf.PdfDocument()
        val page=pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,1).create())
        page.canvas.drawText("Original Reading Habit Coach PDF fixture",40f,80f,android.graphics.Paint().apply { textSize=18f }); pdf.finishPage(page)
        val file=File(context.cacheDir,"original-test.pdf"); file.outputStream().use { pdf.writeTo(it) }; pdf.close()
        val imported=DocumentImporter(context).import(Uri.fromFile(file))
        assertEquals("PDF",imported.format); assertEquals(1,imported.totalPages)
        DocumentImporter(context).file(imported).delete(); file.delete()
        val epub=File(context.cacheDir,"original-test.epub")
        instrumentation.context.assets.open("a-quiet-reading-moment.epub").use { input -> epub.outputStream().use { input.copyTo(it) } }
        val book=DocumentImporter(context).import(Uri.fromFile(epub)); assertEquals("EPUB",book.format); assertEquals("A Quiet Reading Moment",book.title); assertEquals("Reading Habit Coach",book.author)
        DocumentImporter(context).file(book).delete(); epub.delete()
    }
    /** Physical-device UI testing requires an explicit runner flag to avoid accidental disruption. */
    @Test fun emulatorReadingFlowAndScreenshots() = runBlocking<Unit> {
        Assume.assumeTrue("Enable allowPhysicalUi only for a dedicated physical-device test run", android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk_gphone") || InstrumentationRegistry.getArguments().getString("allowPhysicalUi") == "true")
        val app=context.applicationContext as CoachApplication; val dao=app.database.dao()
        // Named original sample content; the test creates real stored records, not baked-in statistics.
        val existing=dao.allBooks().firstOrNull { it.tags=="QA sample" }
        val book=existing ?: Book(title="A Quiet Reading Moment",author="Reading Habit Coach",shelf="Reading",totalPages=120,tags="QA sample",colour=0).also { dao.saveBook(it) }
        app.preferences.save(Settings(onboarded=true))
        dao.savePolicy(GoalPolicy(weekOf(LocalDate.now()).toEpochDay(),4))
        val device=UiDevice.getInstance(instrumentation)
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.text("A little reading,\na little more you.")),20000))
        val output=File(context.getExternalFilesDir(null),"qa-screenshots").apply { mkdirs() }
        fun capture(name: String) { device.waitForIdle(); android.os.SystemClock.sleep(400); device.takeScreenshot(File(output,name)) }
        capture("01-home.png")
        fun clickText(text: String) { val target=device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("\\s*"+java.util.regex.Pattern.quote(text)+"\\s*"))),10000); assertNotNull("Missing $text",target); target.click(); device.waitForIdle() }
        clickText("Start Reading Session")
        assertTrue(device.wait(Until.hasObject(By.text("Pause")),10000))
        delay(1200)
        capture("03-session.png")
        clickText("Pause"); assertTrue(device.wait(Until.hasObject(By.text("Resume")),10000))
        clickText("Resume"); clickText("Finish session")
        assertTrue(device.wait(Until.hasObject(By.text("A few pages further")),10000))
        clickText("Save")
        assertTrue(device.wait(Until.hasObject(By.text("Start Reading Session")),10000))
        assertTrue(dao.allSessions().any { it.bookId==book.id })
        clickText("Library"); capture("02-library.png")
        clickText("Journal"); clickText("Capture a thought")
        val fields=device.wait(Until.findObjects(By.clazz("android.widget.EditText")),10000)
        if(fields.isNullOrEmpty()) { device.takeScreenshot(File(output,"failure-journal.png")); device.dumpWindowHierarchy(File(output,"failure-journal.xml")) }
        assertTrue("Journal dialog exposes an editable field", !fields.isNullOrEmpty())
        var entered=false
        repeat(5) { if(!entered) try {
            device.waitForIdle()
            device.findObject(UiSelector().className("android.widget.EditText").instance(0)).setText("A little reading can change the shape of a day.")
            entered=true
        } catch (_: StaleObjectException) { device.waitForIdle() } }
        assertTrue("The note body accepts text",entered)
        clickText("Save")
        assertTrue(device.wait(Until.hasObject(By.text("A little reading can change the shape of a day.")),10000))
        capture("05-journal.png")
        clickText("Goals"); capture("04-goals.png")
        clickText("Insights"); capture("08-insights.png")
        clickText("Voice"); capture("07-voice.png")
        clickText("Home")
        // Verify Room persisted through an activity recreation.
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.text("A little reading,\na little more you.")),10000))
        assertTrue(dao.allNotes().any { it.body=="A little reading can change the shape of a day." })
        val epub=File(context.cacheDir,"a-quiet-reading-moment.epub")
        instrumentation.context.assets.open("a-quiet-reading-moment.epub").use { input -> epub.outputStream().use { input.copyTo(it) } }
        val imported=DocumentImporter(context).import(Uri.fromFile(epub))
        dao.saveBook(imported.copy(tags="QA sample"))
        context.startActivity(Intent(context,EpubActivity::class.java).putExtra("bookId",imported.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.desc("Chapters")),20000))
        assertTrue(device.wait(Until.hasObject(By.text("Making room")),20000))
        capture("06-reader.png")
        device.findObject(By.desc("Add bookmark")).click()
        delay(500)
        assertTrue(dao.allNotes().any { it.bookId==imported.id && it.location.startsWith("{") })
        device.findObject(By.desc("Search in book")).click()
        val searchField=device.wait(Until.findObject(By.clazz("android.widget.EditText")),10000)
        searchField.text="reading"
        clickText("Search")
        assertTrue(device.wait(Until.hasObject(By.textContains("matches")),20000))
        clickText("Close")
        capture("06-reader.png")
        device.findObject(By.desc("Close reader")).click()
        device.setOrientationNatural()
        try {
            device.waitForIdle(); capture("portrait-home.png")
            clickText("Library"); capture("portrait-library.png")
        } finally { device.unfreezeRotation() }
        epub.delete()
    }
}
