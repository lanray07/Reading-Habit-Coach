package com.readinghabitcoach.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.readinghabitcoach.app.data.*
import com.readinghabitcoach.app.domain.*
import com.readinghabitcoach.app.reader.*
import com.readinghabitcoach.app.reminders.ReadingReminder
import com.readinghabitcoach.app.ui.*
import com.readinghabitcoach.app.voice.*
import kotlinx.coroutines.launch
import java.time.*

class MainActivity : ComponentActivity() {
    private val vm: CoachViewModel by viewModels()
    private var speech: SpeechController? = null
    private var voice: VoiceInput? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { val settings by vm.settings.collectAsState(); CoachTheme(settings.dark) { CoachApp() } }
    }
    @Composable private fun CoachApp() {
        val settings by vm.settings.collectAsState(); val ready by vm.ready.collectAsState()
        val books by vm.books.collectAsState(); val sessions by vm.sessions.collectAsState(); val notes by vm.notes.collectAsState(); val active by vm.active.collectAsState()
        val scope = rememberCoroutineScope(); val snackbar = remember { SnackbarHostState() }
        var screen by rememberSaveable { mutableStateOf("Home") }
        var editBook by remember { mutableStateOf<Book?>(null) }; var bookEditor by remember { mutableStateOf(false) }
        var detail by remember { mutableStateOf<Book?>(null) }; var reader by remember { mutableStateOf<Book?>(null) }
        var editNote by remember { mutableStateOf<JournalNote?>(null) }; var noteEditor by remember { mutableStateOf(false) }; var noteBook by remember { mutableStateOf("") }
        var manual by remember { mutableStateOf(false) }; var manualMinutes by remember { mutableIntStateOf(20) }; var finishing by remember { mutableStateOf(false) }
        var exportKind by remember { mutableStateOf("json") }; var searchDialog by remember { mutableStateOf(false) }
        var voiceStatus by remember { mutableStateOf("Ready when you are.") }; var notificationHour by remember { mutableIntStateOf(19) }
        var importing by remember { mutableStateOf(false) }
        var backupPassword by remember { mutableStateOf("") }; var passwordMode by remember { mutableStateOf("") }
        var encryptedUri by remember { mutableStateOf<Uri?>(null) }
        val app = application as CoachApplication
        fun announce(value: String) { vm.message.value = value }
        fun addBook() { editBook = null; bookEditor = true }
        fun newNote(id: String = "") { editNote = null; noteBook = id; noteEditor = true }
        fun open(book: Book) {
            if (book.document.isBlank()) { detail = book; announce("This is a tracked book. Import a personal PDF or DRM-free EPUB to read in the app.") }
            else if (book.format == "EPUB") startActivity(Intent(this, EpubActivity::class.java).putExtra("bookId", book.id))
            else reader = book
        }
        fun runCommand(text: String) {
            val current = books.find { it.id == active?.bookId } ?: books.firstOrNull { it.shelf == "Reading" }
            when (val c = parseCommand(text)) {
                VoiceCommand.Start -> { screen = "Sessions"; if (current != null) vm.start(current) else announce("Choose or add a book to start reading.") }
                VoiceCommand.Pause -> { vm.pause(); announce("Your reading session is paused.") }
                VoiceCommand.Resume -> { vm.resume(); screen = "Sessions" }
                VoiceCommand.Finish -> if (active != null) { vm.pause(); finishing = true } else announce("There is no active session to finish.")
                VoiceCommand.Note -> newNote(current?.id ?: "")
                VoiceCommand.Week -> { val week = weekOf(LocalDate.now(), DayOfWeek.of(settings.weekStart)); val ms = vm.daily().filterKeys { it >= week && it <= LocalDate.now() }.values.sum(); val line = "You’ve read for ${durationLabel(ms)} this week."; announce(line); speech?.speak(line) }
                VoiceCommand.Goal -> { val line = "Your daily intention is ${settings.minutes} minutes. Your weekly goal is ${vm.streak().target} reading days."; announce(line); speech?.speak(line) }
                VoiceCommand.Open -> current?.let(::open) ?: announce("Choose a current book in your library.")
                VoiceCommand.LatestNote -> notes.firstOrNull()?.let { speech?.speak(it.body) } ?: announce("Your journal is ready for its first note.")
                is VoiceCommand.Log -> { manualMinutes = c.minutes; manual = true }
                VoiceCommand.Next, VoiceCommand.Bookmark -> announce("Open a document and use the next-page or bookmark control in its reader.")
                VoiceCommand.Unknown -> announce("That command wasn’t recognised. Try one of the examples below.")
            }
        }
        val tts = remember { SpeechController(this) }.also { speech = it }
        // Refresh callbacks when books, notes and the active session change.
        val commandCallback by rememberUpdatedState<(String) -> Unit>({ runCommand(it) })
        val liveInput = remember { VoiceInput(this, { commandCallback(it) }, { voiceStatus = it }) }.also { voice = it }
        DisposableEffect(Unit) { onDispose { tts.close(); liveInput.close() } }
        val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) liveInput.listen(settings.language) else announce("Microphone access is off. Type a command or use touch controls.") }
        val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.updateSettings(settings.copy(reminder = granted, reminderHour = notificationHour)); ReadingReminder.schedule(this, granted, notificationHour)
            if (!granted) announce("Notifications are off. You can still track reading normally.")
        }
        val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch {
            importing = true
            runCatching { val b = DocumentImporter(this@MainActivity).import(uri); vm.dao.saveBook(b) }.onSuccess { announce("Document imported privately."); screen = "Library" }.onFailure { announce(it.message ?: "The document could not be imported.") }
            importing = false
        } }
        val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch {
            runCatching { DataTransfer(this@MainActivity, app.database).importJson(uri) }.onSuccess { announce("Backup merged into your library.") }.onFailure { announce(it.message ?: "This backup could not be imported.") }
        } }
        val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri -> if (uri != null) scope.launch {
            runCatching { val transfer=DataTransfer(this@MainActivity, app.database)
                if (exportKind == "enc") transfer.writeEncrypted(uri,backupPassword.toCharArray()) else transfer.write(uri, exportKind)
            }.onSuccess { announce("Your export is ready.") }.onFailure { announce(it.message ?: "Export failed.") }
            backupPassword = ""
        } else { backupPassword = "" } }
        val encryptedPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { encryptedUri=uri; passwordMode="import" } }
        fun export(kind: String) { exportKind = kind; if (kind == "enc") passwordMode="export" else exporter.launch("reading-coach-${LocalDate.now()}.$kind") }
        val message by vm.message.collectAsState()
        LaunchedEffect(message) { message?.let { vm.message.value = null; snackbar.showSnackbar(it) } }
        if (!ready) { Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } }; return }
        if (!settings.onboarded) { Surface(Modifier.fillMaxSize()) { Onboarding(settings, vm::onboarding) }; return }
        if (reader != null) { PdfScreen(books.find { it.id == reader?.id } ?: reader!!, vm::saveBook, { vm.saveNote(it); announce("Bookmark saved to your journal.") }, { reader = null }); return }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 900.dp
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                if (!wide) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    navItems.take(5).forEach { (name, icon) -> NavigationBarItem(screen == name, { screen = name }, icon = { Icon(icon, name) }, label = { Text(name) }) }
                }
            }) { padding ->
                Row(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
                    if (wide) Sidebar(screen) { screen = it }
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                            if (!wide) { Icon(Icons.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Reading Habit Coach", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium) }
                            IconButton({ searchDialog = true }) { Icon(Icons.Outlined.Search, "Find book metadata online") }
                            if (!wide) IconButton({ screen = "Goals" }) { Icon(Icons.Outlined.Flag, "Reading goals") }
                            IconButton({ screen = "Voice" }) { Icon(Icons.Outlined.MicNone, "Voice companion") }
                            IconButton({ screen = "Settings" }) { Icon(Icons.Outlined.Settings, "Settings") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 40.dp else 24.dp, vertical = 28.dp)) {
                            when (screen) {
                                "Home" -> HomeScreen(vm, ::addBook, { vm.start(it); screen = "Sessions" }, { manual = true }, { newNote() }, { screen = it }, { detail = it })
                                "Library" -> LibraryScreen(books, ::addBook, { importer.launch(arrayOf("application/pdf", "application/epub+zip")) }, { detail = it })
                                "Sessions" -> SessionScreen(vm, ::addBook, { manualMinutes = 20; manual = true }, { vm.pause(); finishing = true })
                                "Journal" -> JournalScreen(notes, books, { newNote() }, { editNote = it; noteEditor = true }, vm::saveNote, { export("md") })
                                "Goals" -> GoalsScreen(vm)
                                "Insights" -> InsightsScreen(vm)
                                "Voice" -> VoiceScreen(liveInput.available, voiceStatus, tts, { if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) liveInput.listen(settings.language) else microphone.launch(Manifest.permission.RECORD_AUDIO) }, ::runCommand)
                                "Settings" -> SettingsScreen(settings, vm::updateSettings, ::export, { backupImport.launch(arrayOf("application/json", "text/plain")) }, { encryptedPicker.launch(arrayOf("*/*")) }, { enabled, hour ->
                                    notificationHour = hour
                                    if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    else { vm.updateSettings(settings.copy(reminder = enabled, reminderHour = hour)); ReadingReminder.schedule(this@MainActivity, enabled, hour) }
                                }, { ReadingReminder.schedule(this@MainActivity, false, 19); vm.deleteAll() })
                            }
                        }
                    }
                }
            }
        }
        if (bookEditor) BookEditor(editBook, { bookEditor = false }, { vm.saveBook(it); bookEditor = false })
        detail?.let { selected -> val b = books.find { it.id == selected.id } ?: selected
            BookDetails(b, sessions.filter { it.bookId == b.id }, { detail = null }, { editBook = b; detail = null; bookEditor = true }, { vm.start(b); detail = null; screen = "Sessions" }, { detail = null; open(b) }, { detail = null; newNote(b.id) })
        }
        if (noteEditor) NoteEditor(editNote, books, noteBook, { noteEditor = false }, { vm.saveNote(it); noteEditor = false }, { vm.deleteNote(it); noteEditor = false })
        if (manual) ManualLog(books, manualMinutes, { manual = false }, { b, m, d, a, e, n, mood -> vm.log(b, m, d, a, e, n, mood); manual = false })
        if (finishing) FinishSession(vm) { finishing = false }
        if (searchDialog) MetadataDialog({ searchDialog = false }, { editBook = it; searchDialog = false; bookEditor = true })
        if (importing) AlertDialog({}, title = { Text("Importing your document") }, text = { Text("Checking the file and saving a private copy…") }, confirmButton = {})
        if (passwordMode.isNotBlank()) PasswordPrompt(passwordMode == "export", { passwordMode=""; backupPassword=""; encryptedUri=null }, { password ->
            val mode=passwordMode; passwordMode=""
            if (mode=="export") { backupPassword=password; exporter.launch("reading-coach-${LocalDate.now()}.rhcbackup") }
            else { val uri=encryptedUri; encryptedUri=null; if (uri!=null) scope.launch {
                runCatching { DataTransfer(this@MainActivity,app.database).importEncrypted(uri,password.toCharArray()) }.onSuccess { announce("Encrypted backup restored.") }.onFailure { announce(it.message ?: "Restore failed.") }
            } }
        })
    }
    override fun onStop() { super.onStop(); voice?.cancel(); speech?.pause() }
}
private val navItems = listOf("Home" to Icons.Outlined.Home, "Library" to Icons.Outlined.MenuBook, "Sessions" to Icons.Outlined.Schedule,
    "Journal" to Icons.Outlined.EditNote, "Insights" to Icons.Outlined.BarChart, "Goals" to Icons.Outlined.Flag, "Voice" to Icons.Outlined.MicNone, "Settings" to Icons.Outlined.Settings)
@Composable private fun Sidebar(screen: String, navigate: (String) -> Unit) {
    Surface(Modifier.width(208.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp)) {
            Icon(Icons.Outlined.MenuBook, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(14.dp)); Text("Reading\nHabit Coach", style = MaterialTheme.typography.headlineSmall)
            Text("A little, often.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            navItems.forEach { (name, icon) ->
                val selected = screen == name
                Surface(onClick = { navigate(name) }, color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Text(name, style = MaterialTheme.typography.labelLarge) }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(10.dp)); Text("A few pages can\nchange your day.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun MetadataDialog(dismiss: () -> Unit, select: (Book) -> Unit) {
    var query by remember { mutableStateOf("") }; var results by remember { mutableStateOf<List<Book>>(emptyList()) }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    AlertDialog(dismiss, title = { Text("Find your next book") }, text = { Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Search Open Library by title, author or ISBN. Your search terms are sent to Open Library; your notes and reading history stay private.", style = MaterialTheme.typography.bodyMedium)
        Field(query, { query = it }, "Title, author or ISBN")
        Button({ scope.launch { busy = true; error = ""; runCatching { MetadataLookup().search(query) }.onSuccess { results = it; if (it.isEmpty()) error = "No matches. Try another search or add manually." }.onFailure { error = it.message ?: "Search unavailable. Add a book manually." }; busy = false } }, enabled = query.length >= 2 && !busy) { Text(if (busy) "Searching…" else "Search") }
        if (error.isNotBlank()) Text(error)
        results.forEach { b -> TextButton({ select(b) }) { Column(Modifier.fillMaxWidth()) { Text(b.title, style = MaterialTheme.typography.titleMedium); Text(b.author, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
    } }, confirmButton = { TextButton(dismiss) { Text("Close") } })
}
