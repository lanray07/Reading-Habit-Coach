package com.readinghabitcoach.app

import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.R

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
    override fun attachBaseContext(base: android.content.Context) { super.attachBaseContext(L10n.wrap(base)) }
    private val vm: CoachViewModel by viewModels()
    private var speech: SpeechController? = null
    private var voice: VoiceInput? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        java.util.Locale.setDefault(L10n.locale())
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
        var voiceStatus by remember { mutableStateOf(L10n.text(R.string.msg_182bf2ce919b)) }; var notificationHour by remember { mutableIntStateOf(19) }
        var importing by remember { mutableStateOf(false) }
        var backupPassword by remember { mutableStateOf("") }; var passwordMode by remember { mutableStateOf("") }
        var encryptedUri by remember { mutableStateOf<Uri?>(null) }
        val app = application as CoachApplication
        fun announce(value: String) { vm.message.value = value }
        fun addBook() { editBook = null; bookEditor = true }
        fun newNote(id: String = "") { editNote = null; noteBook = id; noteEditor = true }
        fun open(book: Book) {
            if (book.document.isBlank()) { detail = book; announce(L10n.text(R.string.msg_4332ad49547f)) }
            else if (book.format == "EPUB") startActivity(Intent(this, EpubActivity::class.java).putExtra("bookId", book.id))
            else reader = book
        }
        fun runCommand(text: String) {
            val current = books.find { it.id == active?.bookId } ?: books.firstOrNull { it.shelf == "Reading" }
            when (val c = parseCommand(L10n.canonicalCommand(text))) {
                VoiceCommand.Start -> { screen = "Sessions"; if (current != null) vm.start(current) else announce(L10n.text(R.string.msg_d00850e6b69e)) }
                VoiceCommand.Pause -> { vm.pause(); announce(L10n.text(R.string.msg_3aa90b8dad23)) }
                VoiceCommand.Resume -> { vm.resume(); screen = "Sessions" }
                VoiceCommand.Finish -> if (active != null) { vm.pause(); finishing = true } else announce(L10n.text(R.string.msg_be61937ba4cd))
                VoiceCommand.Note -> newNote(current?.id ?: "")
                VoiceCommand.Week -> { val week = weekOf(LocalDate.now(), DayOfWeek.of(settings.weekStart)); val ms = vm.daily().filterKeys { it >= week && it <= LocalDate.now() }.values.sum(); val line = L10n.text(R.string.msg_4e28e7c44792, durationLabel(ms)); announce(line); speech?.speak(line) }
                VoiceCommand.Goal -> { val line = L10n.text(R.string.msg_5a42470e4746, settings.minutes, vm.streak().target); announce(line); speech?.speak(line) }
                VoiceCommand.Open -> current?.let(::open) ?: announce(L10n.text(R.string.msg_9a1572ab9716))
                VoiceCommand.LatestNote -> notes.firstOrNull()?.let { speech?.speak(it.body) } ?: announce(L10n.text(R.string.msg_08443a524210))
                is VoiceCommand.Log -> { manualMinutes = c.minutes; manual = true }
                VoiceCommand.Next, VoiceCommand.Bookmark -> announce(L10n.text(R.string.msg_634ea5efb120))
                VoiceCommand.Unknown -> announce(L10n.text(R.string.msg_443d32157816))
            }
        }
        val tts = remember { SpeechController(this) }.also { speech = it }
        // Refresh callbacks when books, notes and the active session change.
        val commandCallback by rememberUpdatedState<(String) -> Unit>({ runCommand(it) })
        val liveInput = remember { VoiceInput(this, { commandCallback(it) }, { voiceStatus = it }) }.also { voice = it }
        DisposableEffect(Unit) { onDispose { tts.close(); liveInput.close() } }
        val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) liveInput.listen(L10n.locale().toLanguageTag()) else announce(L10n.text(R.string.msg_a672f9ef0616)) }
        val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.updateSettings(settings.copy(reminder = granted, reminderHour = notificationHour)); ReadingReminder.schedule(this, granted, notificationHour)
            if (!granted) announce(L10n.text(R.string.msg_8df93a96dc03))
        }
        val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch {
            importing = true
            runCatching { val b = DocumentImporter(this@MainActivity).import(uri); vm.dao.saveBook(b) }.onSuccess { announce(L10n.text(R.string.msg_3a951380ca9a)); screen = "Library" }.onFailure { announce(it.message ?: L10n.text(R.string.msg_1db6c3598511)) }
            importing = false
        } }
        val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch {
            runCatching { DataTransfer(this@MainActivity, app.database).importJson(uri) }.onSuccess { announce(L10n.text(R.string.msg_f6ac87cf3f54)) }.onFailure { announce(it.message ?: L10n.text(R.string.msg_3658cfe41a0e)) }
        } }
        val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri -> if (uri != null) scope.launch {
            runCatching { val transfer=DataTransfer(this@MainActivity, app.database)
                if (exportKind == "enc") transfer.writeEncrypted(uri,backupPassword.toCharArray()) else transfer.write(uri, exportKind)
            }.onSuccess { announce(L10n.text(R.string.msg_0e104fb7c986)) }.onFailure { announce(it.message ?: L10n.text(R.string.msg_307cfd717ce3)) }
            backupPassword = ""
        } else { backupPassword = "" } }
        val encryptedPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { encryptedUri=uri; passwordMode="import" } }
        fun export(kind: String) { exportKind = kind; if (kind == "enc") passwordMode="export" else exporter.launch("reading-coach-${LocalDate.now()}.$kind") }
        val message by vm.message.collectAsState()
        LaunchedEffect(message) { message?.let { vm.message.value = null; snackbar.showSnackbar(it) } }
        if (!ready) { Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } }; return }
        if (!settings.onboarded) { Surface(Modifier.fillMaxSize()) { Onboarding(settings, vm::onboarding) }; return }
        if (reader != null) { PdfScreen(books.find { it.id == reader?.id } ?: reader!!, vm::saveBook, { vm.saveNote(it); announce(L10n.text(R.string.msg_ef3cbf50c070)) }, { reader = null }); return }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 900.dp
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                if (!wide) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    navItems.take(5).forEach { (name, icon) -> NavigationBarItem(screen == name, { screen = name }, icon = { Icon(icon, L10n.label(name)) }, label = { Text(L10n.label(name), maxLines = 2) }) }
                }
            }) { padding ->
                Row(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
                    if (wide) Sidebar(screen) { screen = it }
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                            if (!wide) { Icon(Icons.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(L10n.text(R.string.msg_cce5b3a8da15), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium) }
                            IconButton({ searchDialog = true }) { Icon(Icons.Outlined.Search, L10n.text(R.string.msg_14993f10816b)) }
                            if (!wide) IconButton({ screen = "Goals" }) { Icon(Icons.Outlined.Flag, L10n.text(R.string.msg_2773984ed1d1)) }
                            IconButton({ screen = "Voice" }) { Icon(Icons.Outlined.MicNone, L10n.text(R.string.msg_bf39384bc235)) }
                            IconButton({ screen = "Settings" }) { Icon(Icons.Outlined.Settings, L10n.text(R.string.msg_74a883a037bc)) }
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
                                "Voice" -> VoiceScreen(liveInput.available, voiceStatus, tts, { if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) liveInput.listen(L10n.locale().toLanguageTag()) else microphone.launch(Manifest.permission.RECORD_AUDIO) }, ::runCommand)
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
        if (importing) AlertDialog({}, title = { Text(L10n.text(R.string.msg_146d478411f1)) }, text = { Text(L10n.text(R.string.msg_dc8a3aad5ea9)) }, confirmButton = {})
        if (passwordMode.isNotBlank()) PasswordPrompt(passwordMode == "export", { passwordMode=""; backupPassword=""; encryptedUri=null }, { password ->
            val mode=passwordMode; passwordMode=""
            if (mode=="export") { backupPassword=password; exporter.launch("reading-coach-${LocalDate.now()}.rhcbackup") }
            else { val uri=encryptedUri; encryptedUri=null; if (uri!=null) scope.launch {
                runCatching { DataTransfer(this@MainActivity,app.database).importEncrypted(uri,password.toCharArray()) }.onSuccess { announce(L10n.text(R.string.msg_8eb7a9caa080)) }.onFailure { announce(it.message ?: L10n.text(R.string.msg_c998f96febc3)) }
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
            Spacer(Modifier.height(14.dp)); Text(L10n.text(R.string.msg_65cad793e31a), style = MaterialTheme.typography.headlineSmall)
            Text(L10n.text(R.string.msg_151eb8fafbd9), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            navItems.forEach { (name, icon) ->
                val selected = screen == name
                Surface(onClick = { navigate(name) }, color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Text(name, style = MaterialTheme.typography.labelLarge) }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(10.dp)); Text(L10n.text(R.string.msg_fe513a53861e), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun MetadataDialog(dismiss: () -> Unit, select: (Book) -> Unit) {
    var query by remember { mutableStateOf("") }; var results by remember { mutableStateOf<List<Book>>(emptyList()) }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope()
    AlertDialog(dismiss, title = { Text(L10n.text(R.string.msg_d25b00449a3f)) }, text = { Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(L10n.text(R.string.msg_3225198f4e68), style = MaterialTheme.typography.bodyMedium)
        Field(query, { query = it }, L10n.text(R.string.msg_2831c0a2eb3b))
        Button({ scope.launch { busy = true; error = ""; runCatching { MetadataLookup().search(query) }.onSuccess { results = it; if (it.isEmpty()) error = L10n.text(R.string.msg_bed8a4eb7d57) }.onFailure { error = it.message ?: L10n.text(R.string.msg_28857cdfbb9f) }; busy = false } }, enabled = query.length >= 2 && !busy) { Text(if (busy) L10n.text(R.string.msg_c31723ab3302) else L10n.text(R.string.msg_49c266baaaa7)) }
        if (error.isNotBlank()) Text(error)
        results.forEach { b -> TextButton({ select(b) }) { Column(Modifier.fillMaxWidth()) { Text(b.title, style = MaterialTheme.typography.titleMedium); Text(b.author, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
    } }, confirmButton = { TextButton(dismiss) { Text(L10n.text(R.string.msg_7d9eb7acb13e)) } })
}
