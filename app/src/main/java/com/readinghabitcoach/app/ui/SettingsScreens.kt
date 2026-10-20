package com.readinghabitcoach.app.ui

import com.readinghabitcoach.app.i18n.L10n

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
        LanguagePicker()
        Image(painterResource(R.drawable.reading_moment), null, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
        Text(L10n.text(R.string.msg_c3f5fe1d7041), style = MaterialTheme.typography.headlineLarge)
        Text(L10n.text(R.string.msg_1c29c22cae3f), style = MaterialTheme.typography.titleLarge)
        Text(L10n.text(R.string.msg_9a0089d6878e), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Paper(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
            Text(L10n.text(R.string.msg_6d81a85b502f), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp)); Text(L10n.text(R.string.msg_c590d712259b, minutes.toInt())); Slider(minutes, { minutes = it }, valueRange = 5f..60f, steps = 10)
            Text(L10n.text(R.string.msg_8020005061ca, days.toInt())); Slider(days, { days = it }, valueRange = 1f..7f, steps = 5)
            Text(L10n.text(R.string.msg_fcf11b861a36), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Primary(L10n.text(R.string.msg_451dfaaff730), Icons.Outlined.MenuBook) { complete(settings.copy(minutes = minutes.toInt(), weeklyDays = days.toInt())) }
        TextButton({ complete(settings) }) { Text(L10n.text(R.string.msg_b58eb52c8810)) }
        Text(L10n.text(R.string.msg_9a2f18432d8d), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun SettingsScreen(settings: Settings, save: (Settings) -> Unit, export: (String) -> Unit, importBackup: () -> Unit, importEncrypted: () -> Unit, reminder: (Boolean, Int) -> Unit, delete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }; var deleteText by remember { mutableStateOf("") }
    var licences by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var hour by remember(settings.reminderHour) { mutableFloatStateOf(settings.reminderHour.toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageTitle(L10n.text(R.string.msg_a0548a894fc4), L10n.text(R.string.msg_656f7c0d23b3))
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_ba3b7fc6f4e1), style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Text(L10n.text(R.string.msg_ca3eef198cf1), Modifier.weight(1f)); Switch(settings.dark, { save(settings.copy(dark = it)) }) }
            Text(L10n.text(R.string.msg_1d08ef339bb5), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp)); LanguagePicker()
            Text(L10n.text(R.string.msg_d79111008868), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_f47f8881534d), style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Text(L10n.text(R.string.msg_bacfde1c301f), Modifier.weight(1f)); Switch(settings.reminder, { reminder(it, hour.toInt()) }) }
            Text(L10n.text(R.string.msg_7a1be549b4a4, hour.toInt())); Slider(hour, { hour = it }, valueRange = 7f..21f, steps = 13)
            TextButton({ reminder(settings.reminder, hour.toInt()) }) { Text(L10n.text(R.string.msg_dba733ce8f45)) }
            Text(L10n.text(R.string.msg_fc375ab230aa), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_ee346f130879), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp)); Text(L10n.text(R.string.msg_517146bba2f6))
            Row(Modifier.horizontalScroll(rememberScrollState())) { TextButton({ export("json") }) { Text(L10n.text(R.string.msg_ff25653ee9e4)) }; TextButton({ export("csv") }) { Text(L10n.text(R.string.msg_9b0913342966)) }; TextButton({ export("md") }) { Text(L10n.text(R.string.msg_fc165c96475b)) } }
            Row { TextButton({ export("txt") }) { Text(L10n.text(R.string.msg_e11844edb85c)) }; TextButton({ export("pdf") }) { Text(L10n.text(R.string.msg_f849b700ab94)) }; TextButton(importBackup) { Text(L10n.text(R.string.msg_12dd2e2eda81)) } }
            Row { TextButton({ export("enc") }) { Text(L10n.text(R.string.msg_0e6b09c31f1e)) }; TextButton(importEncrypted) { Text(L10n.text(R.string.msg_ceaffb9d0e59)) } }
            Text(L10n.text(R.string.msg_116753079acb), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(L10n.text(R.string.msg_0d43a51e32d9), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp)); TextButton({ confirm = true }) { Text(L10n.text(R.string.msg_ee599b99b1f4), color = MaterialTheme.colorScheme.error) }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_8548c8955e23), style = MaterialTheme.typography.headlineSmall)
            Text(L10n.text(R.string.msg_2fe5ae4c798d, com.readinghabitcoach.app.BuildConfig.VERSION_NAME))
            Spacer(Modifier.height(12.dp)); Text(L10n.text(R.string.msg_9667dd1c1827), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton({ licences = true }) { Text(L10n.text(R.string.msg_3617332acbd7)) }
        }
    }
    if (licences) AlertDialog({ licences = false }, title = { Text(L10n.text(R.string.msg_3617332acbd7)) }, text = {
        val text = remember { listOf("Readium-BSD-3-Clause.txt", "Apache-2.0.txt").joinToString("\n\n") { name -> context.assets.open("licenses/$name").bufferedReader().use { it.readText() } } }
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState())) { Text(text, style = MaterialTheme.typography.bodySmall) }
    }, confirmButton = { TextButton({ licences = false }) { Text(L10n.text(R.string.msg_7d9eb7acb13e)) } })
    if (confirm) AlertDialog({ confirm = false }, title = { Text(L10n.text(R.string.msg_e88d08b92b86)) }, text = { Column { Text(L10n.text(R.string.msg_d42f71e686cc)); Field(deleteText, { deleteText = it }, L10n.text(R.string.msg_d743070540a1)) } },
        confirmButton = { TextButton({ delete(); confirm = false }, enabled = deleteText == "DELETE") { Text(L10n.text(R.string.msg_a867ee684d98)) } }, dismissButton = { TextButton({ confirm = false }) { Text(L10n.text(R.string.msg_891e641daf26)) } })
}
@Composable fun VoiceScreen(available: Boolean, status: String, speech: SpeechController, listen: () -> Unit, command: (String) -> Unit) {
    var text by remember { mutableStateOf("") }; var voiceExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageTitle(L10n.text(R.string.msg_a55de8052790), L10n.text(R.string.msg_5343613da968))
        Paper(Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.MicNone, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp)); Text(if (available) L10n.text(R.string.msg_34ef57043285) else L10n.text(R.string.msg_b9005162a8da), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp)); Text(if (available) status else L10n.text(R.string.msg_5e2f604e825b), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp)); Primary(L10n.text(R.string.msg_954337257185), Icons.Outlined.MicNone, available, listen)
            Spacer(Modifier.height(16.dp)); Field(text, { text = it }, L10n.text(R.string.msg_51f546e5dc81))
            TextButton({ command(text) }, enabled = text.isNotBlank()) { Text(L10n.text(R.string.msg_87e30f34875f)) }
            Text(L10n.text(R.string.msg_82e7e3de9420), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_054d760bfe38), style = MaterialTheme.typography.headlineSmall)
            listOf(L10n.text(R.string.msg_79f199280c52), L10n.text(R.string.msg_99fe285292b1), L10n.text(R.string.msg_b4c5939242ee), L10n.text(R.string.msg_f3f5e3e7c03f), L10n.text(R.string.msg_8457f0657dec), L10n.text(R.string.msg_2e1c325f3743), L10n.text(R.string.msg_ea4f204e5b19), L10n.text(R.string.msg_a8dd5dce9271), L10n.text(R.string.msg_74d8c0e44230), L10n.text(R.string.msg_46699822edb3)).forEach { example ->
                TextButton({ text = example.replace('’', '\'') }) { Text(example) }
            }
        }
        Paper(Modifier.fillMaxWidth()) {
            Text(L10n.text(R.string.msg_0153f6f2a3bc), style = MaterialTheme.typography.headlineSmall)
            Text(if (speech.ready) L10n.text(R.string.msg_a893e5cc917b) else L10n.text(R.string.msg_4b8206902ff3), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row { IconButton({ if (speech.playing) speech.pause() else speech.resume() }) { Icon(if (speech.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, L10n.text(R.string.msg_047e3749baa8)) }; IconButton({ speech.stop() }) { Icon(Icons.Outlined.Stop, L10n.text(R.string.msg_68a5965beb42)) } }
            Text(L10n.text(R.string.msg_5e12631c0bbf, "%.2f".format(speech.rate))); Slider(speech.rate, { speech.rate(it) }, valueRange = .5f..2f)
            Box { TextButton({ voiceExpanded = true }, enabled = speech.voices.isNotEmpty()) { Text(L10n.text(R.string.msg_be40d16b62c6)) }
                DropdownMenu(voiceExpanded, { voiceExpanded = false }) { speech.voices.forEach { v -> DropdownMenuItem({ Text("${v.locale.displayName} · ${v.name}") }, { speech.voice(v); voiceExpanded = false }) } }
            }
            Text(L10n.text(R.string.msg_ac94beaa5668), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun LanguagePicker() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ expanded = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(L10n.text(R.string.msg_a4fe65264ef7) + " · " + (L10n.languageName(L10n.selected(context)).takeUnless { L10n.selected(context).isBlank() } ?: L10n.text(R.string.msg_3fb007ba6533)))
        }
        DropdownMenu(expanded, { expanded = false }, Modifier.heightIn(max = 380.dp)) {
            L10n.languages.forEach { (tag, name) ->
                DropdownMenuItem(text = { Text(if (tag.isBlank()) L10n.text(R.string.msg_3fb007ba6533) else name) }, onClick = {
                    expanded = false; L10n.select(context, tag)
                    if (android.os.Build.VERSION.SDK_INT < 33) (context as? android.app.Activity)?.recreate()
                })
            }
        }
    }
}
