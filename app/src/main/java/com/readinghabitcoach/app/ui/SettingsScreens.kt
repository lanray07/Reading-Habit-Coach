package com.readinghabitcoach.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.readinghabitcoach.app.R
import com.readinghabitcoach.app.data.Settings
import com.readinghabitcoach.app.voice.SpeechController
import androidx.compose.ui.platform.LocalContext

@Composable fun Onboarding(settings: Settings, complete: (Settings) -> Unit) {
    var minutes by remember { mutableFloatStateOf(settings.minutes.toFloat()) }; var days by remember { mutableFloatStateOf(settings.weeklyDays.toFloat()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Image(painterResource(R.drawable.reading_moment), null, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
        Text("A reading life,\nat your own pace.", style = MaterialTheme.typography.headlineLarge)
        Text("Read more consistently. Remember what matters.", style = MaterialTheme.typography.titleLarge)
        Text("A private home for your books, reading moments and ideas. No account needed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Paper(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
            Text("Start with a small intention", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp)); Text("${minutes.toInt()} minutes a day"); Slider(minutes, { minutes = it }, valueRange = 5f..60f, steps = 10)
            Text("${days.toInt()} reading days a week"); Slider(days, { days = it }, valueRange = 1f..7f, steps = 5)
            Text("Suggestions, never obligations. Change these whenever you like.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Primary("Make room for reading", Icons.Outlined.MenuBook) { complete(settings.copy(minutes = minutes.toInt(), weeklyDays = days.toInt())) }
        TextButton({ complete(settings) }) { Text("Skip for now") }
        Text("Reminders are optional and can be enabled in Settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun SettingsScreen(settings: Settings, save: (Settings) -> Unit, export: (String) -> Unit, importBackup: () -> Unit, importEncrypted: () -> Unit, reminder: (Boolean, Int) -> Unit, delete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }; var deleteText by remember { mutableStateOf("") }
    var licences by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var hour by remember(settings.reminderHour) { mutableFloatStateOf(settings.reminderHour.toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageTitle("Make yourself at home", "Your reading companion, your preferences.")
        Paper(Modifier.fillMaxWidth()) {
            Text("Reading comfort", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Dark appearance", Modifier.weight(1f)); Switch(settings.dark, { save(settings.copy(dark = it)) }) }
            Text("Text size follows your device’s accessibility settings. The EPUB reader has separate type and spacing controls.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp)); Text("Language · English (UK)")
            Text("Other translations are in preparation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("A gentle reminder", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Daily reading reminder", Modifier.weight(1f)); Switch(settings.reminder, { reminder(it, hour.toInt()) }) }
            Text("Around ${hour.toInt()}:00"); Slider(hour, { hour = it }, valueRange = 7f..21f, steps = 13)
            TextButton({ reminder(settings.reminder, hour.toInt()) }) { Text("Save reminder time") }
            Text("Quiet hours: 22:00–07:00. Your device may delay reminders to save battery.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("Your data stays yours", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp)); Text("Books, sessions, goals and notes are stored on this device. Backups contain metadata and notes, never your imported document files.")
            Row(Modifier.horizontalScroll(rememberScrollState())) { TextButton({ export("json") }) { Text("Export JSON") }; TextButton({ export("csv") }) { Text("Sessions CSV") }; TextButton({ export("md") }) { Text("Journal Markdown") } }
            Row { TextButton({ export("txt") }) { Text("Journal text") }; TextButton({ export("pdf") }) { Text("Journal PDF") }; TextButton(importBackup) { Text("Import JSON") } }
            Row { TextButton({ export("enc") }) { Text("Encrypted backup") }; TextButton(importEncrypted) { Text("Restore encrypted") } }
            Text("Encrypted local backups use a passphrase you choose. Keep it safe: it cannot be recovered. Imported document files are excluded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Import merges entries by ID. No reading history is uploaded for analytics. Optional book search sends only your search terms to Open Library. Device speech services may process voice online.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp)); TextButton({ confirm = true }) { Text("Delete all local data", color = MaterialTheme.colorScheme.error) }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("A thoughtful companion", style = MaterialTheme.typography.headlineSmall)
            Text("Reading Habit Coach · Early access 0.1.0")
            Spacer(Modifier.height(12.dp)); Text("Premium purchases are not configured in this build. All available features are open to explore. No payment is taken.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton({ licences = true }) { Text("Open source licences") }
        }
    }
    if (licences) AlertDialog({ licences = false }, title = { Text("Open source licences") }, text = {
        val text = remember { listOf("Readium-BSD-3-Clause.txt", "Apache-2.0.txt").joinToString("\n\n") { name -> context.assets.open("licenses/$name").bufferedReader().use { it.readText() } } }
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState())) { Text(text, style = MaterialTheme.typography.bodySmall) }
    }, confirmButton = { TextButton({ licences = false }) { Text("Close") } })
    if (confirm) AlertDialog({ confirm = false }, title = { Text("Delete your reading data?") }, text = { Column { Text("This permanently removes books, sessions, goals, notes and imported documents from this device. Export a backup first if you want to keep your journal. Type DELETE to continue."); Field(deleteText, { deleteText = it }, "Confirmation") } },
        confirmButton = { TextButton({ delete(); confirm = false }, enabled = deleteText == "DELETE") { Text("Delete all data") } }, dismissButton = { TextButton({ confirm = false }) { Text("Keep my data") } })
}
@Composable fun VoiceScreen(available: Boolean, status: String, speech: SpeechController, listen: () -> Unit, command: (String) -> Unit) {
    var text by remember { mutableStateOf("") }; var voiceExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageTitle("A little help, hands free", "Speak a command or type what you’d like to do.")
        Paper(Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.MicNone, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp)); Text(if (available) "Ready when you are" else "Touch and text are ready", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp)); Text(if (available) status else "This tablet has no compatible speech recognition engine. All commands are available through text below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp)); Primary("Speak a command", Icons.Outlined.MicNone, available, listen)
            Spacer(Modifier.height(16.dp)); Field(text, { text = it }, "Try “start reading” or “log twenty minutes of reading”")
            TextButton({ command(text) }, enabled = text.isNotBlank()) { Text("Run command") }
            Text("The microphone is used only after you tap. No recordings are saved by this app. Your device’s recognition service may use the internet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("A few things you can say", style = MaterialTheme.typography.headlineSmall)
            listOf("Start reading", "Pause my reading session", "Resume reading", "Finish my session", "Log twenty minutes of reading", "Add a note to this book", "How much have I read this week?", "What’s my reading goal?", "Open my current book", "Read my latest note").forEach { example ->
                TextButton({ text = example.replace('’', '\'') }) { Text(example) }
            }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text("Listen at your pace", style = MaterialTheme.typography.headlineSmall)
            Text(if (speech.ready) "Read your notes aloud here, or select text inside an EPUB and choose Read selection." else "No compatible text-to-speech engine is available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row { IconButton({ if (speech.playing) speech.pause() else speech.resume() }) { Icon(if (speech.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, "Pause or resume speech") }; IconButton({ speech.stop() }) { Icon(Icons.Outlined.Stop, "Stop speech") } }
            Text("Speaking rate · ${"%.2f".format(speech.rate)}×"); Slider(speech.rate, { speech.rate(it) }, valueRange = .5f..2f)
            Box { TextButton({ voiceExpanded = true }, enabled = speech.voices.isNotEmpty()) { Text("Choose an installed offline voice") }
                DropdownMenu(voiceExpanded, { voiceExpanded = false }) { speech.voices.forEach { v -> DropdownMenuItem({ Text("${v.locale.displayName} · ${v.name}") }, { speech.voice(v); voiceExpanded = false }) } }
            }
            Text("Pause resumes from the beginning of the current sentence. Availability depends on installed device services.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
