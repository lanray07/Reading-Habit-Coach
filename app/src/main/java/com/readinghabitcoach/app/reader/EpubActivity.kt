@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
package com.readinghabitcoach.app.reader

import com.readinghabitcoach.app.i18n.L10n

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import com.readinghabitcoach.app.CoachApplication
import com.readinghabitcoach.app.R
import com.readinghabitcoach.app.data.*
import com.readinghabitcoach.app.ui.*
import com.readinghabitcoach.app.voice.SpeechController
import kotlinx.coroutines.*
import org.json.JSONObject
import org.readium.r2.navigator.epub.*
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.content.*
import org.readium.r2.shared.publication.services.search.*
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class EpubActivity : FragmentActivity() {
    override fun attachBaseContext(base: android.content.Context) { super.attachBaseContext(L10n.wrap(base)) }
    private var publication: Publication? = null
    private var navigator by mutableStateOf<EpubNavigatorFragment?>(null)
    private lateinit var speech: SpeechController
    private var currentBook: Book? = null
    private var narrationJob: Job? = null
    private val dao get() = (application as CoachApplication).database.dao()
    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        super.onCreate(null) // Reconstruct the publication before restoring its navigator.
        speech = SpeechController(this)
        setContent { CoachTheme { Surface(Modifier.fillMaxSize()) { Box(Modifier.padding(32.dp)) { Text(L10n.text(R.string.msg_d1c57ed5bee6), style = MaterialTheme.typography.headlineMedium) } } } }
        lifecycleScope.launch {
            runCatching {
                val b = dao.book(intent.getStringExtra("bookId") ?: "") ?: error(L10n.text(R.string.msg_585edeb51bd0))
                currentBook = b
                val http = DefaultHttpClient()
                val assets = AssetRetriever(contentResolver, http)
                val opener = PublicationOpener(DefaultPublicationParser(this@EpubActivity, httpClient = http, assetRetriever = assets, pdfFactory = null))
                val asset = assets.retrieve(DocumentImporter(this@EpubActivity).file(b).toUrl()).getOrNull() ?: error(L10n.text(R.string.msg_5e4cb5f6b0c9))
                val p = opener.open(asset, allowUserInteraction = false).getOrNull() ?: error(L10n.text(R.string.msg_69503122397e))
                publication = p
                val location = runCatching { Locator.fromJSON(JSONObject(b.readerPosition)) }.getOrNull()
                supportFragmentManager.fragmentFactory = EpubNavigatorFactory(p).createFragmentFactory(initialLocator = location,
                    initialPreferences = EpubPreferences(theme = Theme.SEPIA, fontSize = 1.1, lineHeight = 1.6))
                setContent { CoachTheme { ReaderContent(b, p) } }
            }.onFailure { e -> setContent { CoachTheme { Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(32.dp)) {
                Text(L10n.text(R.string.msg_cce6c7e18f81), style = MaterialTheme.typography.headlineMedium); Text(e.message ?: L10n.text(R.string.msg_86a7fa1b9f49)); TextButton({ finish() }) { Text(L10n.text(R.string.msg_008e42d22277)) }
            } } } } }
        }
    }
    @Composable private fun ReaderContent(book: Book, p: Publication) {
        var chapters by remember { mutableStateOf(false) }; var bookmarks by remember { mutableStateOf(false) }; var font by remember { mutableDoubleStateOf(1.1) }
        var spacing by remember { mutableDoubleStateOf(1.6) }; var dark by remember { mutableStateOf(false) }; var rate by remember { mutableFloatStateOf(1f) }
        var prompt by remember { mutableStateOf<String?>(null) }; var note by remember { mutableStateOf("") }
        var searching by remember { mutableStateOf(false) }; var searchText by remember { mutableStateOf("") }
        var searchBusy by remember { mutableStateOf(false) }; var matches by remember { mutableStateOf<List<Locator>>(emptyList()) }
        var searchStatus by remember { mutableStateOf("") }
        val notes by dao.notes().collectAsState(emptyList())
        LaunchedEffect(notes, navigator) {
            val decorations = notes.filter { it.bookId == book.id && it.type == "Quote" }.mapNotNull { n ->
                runCatching { Locator.fromJSON(JSONObject(n.location)) }.getOrNull()?.let { locator -> Decoration(n.id, locator, Decoration.Style.Highlight(0xFFE6C77B.toInt())) }
            }
            navigator?.applyDecorations(decorations, "journal-highlights")
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton({ finish() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, L10n.text(R.string.msg_99a9610178ce)) }
                Text(book.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 1)
                IconButton({ chapters = true }) { Icon(Icons.Outlined.List, L10n.text(R.string.msg_9831375bc651)) }
                IconButton({ searching = true }) { Icon(Icons.Outlined.Search, L10n.text(R.string.msg_80dd58750c5b)) }
                IconButton({ bookmarks = true }) { Icon(Icons.Outlined.Bookmarks, L10n.text(R.string.msg_e0ffde6b0f2c)) }
                IconButton({ saveBookmark(book) }) { Icon(Icons.Outlined.BookmarkAdd, L10n.text(R.string.msg_b5255f4dadf0)) }
            }
            AndroidView(factory = { context -> FragmentContainerView(context).apply {
                id = R.id.epub_container
                post {
                    if (supportFragmentManager.findFragmentByTag("epub") == null) {
                        supportFragmentManager.beginTransaction().add(id, EpubNavigatorFragment::class.java, Bundle(), "epub").commitNow()
                    }
                    navigator = supportFragmentManager.findFragmentByTag("epub") as EpubNavigatorFragment
                    lifecycleScope.launch { navigator!!.currentLocator.collect { loc ->
                        currentBook?.let { latest -> val updated = latest.copy(readerPosition = loc.toJSON().toString()); currentBook = updated; dao.saveBook(updated) }
                    } }
                }
            } }, modifier = Modifier.weight(1f).fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton({ navigator?.goBackward() }) { Icon(Icons.Outlined.ChevronLeft, L10n.text(R.string.msg_1208ec01f223)) }
                TextButton({ font = (font - .1).coerceAtLeast(.7); applyPreferences(font, spacing, dark) }) { Text(L10n.text(R.string.msg_8de4e50caae4)) }
                TextButton({ font = (font + .1).coerceAtMost(2.5); applyPreferences(font, spacing, dark) }) { Text(L10n.text(R.string.msg_aee8736c2b78)) }
                TextButton({ spacing = if (spacing < 1.9) spacing + .2 else 1.2; applyPreferences(font, spacing, dark) }) { Text(L10n.text(R.string.msg_62a822a58309)) }
                IconButton({ dark = !dark; applyPreferences(font, spacing, dark) }) { Icon(Icons.Outlined.Contrast, L10n.text(R.string.msg_de04ed4fc532)) }
                IconButton({ navigator?.goForward() }) { Icon(Icons.Outlined.ChevronRight, L10n.text(R.string.msg_c08ac736a5e2)) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton({ lifecycleScope.launch { val selection = navigator?.currentSelection()?.locator
                    if (selection?.text?.highlight.isNullOrBlank()) toast(L10n.text(R.string.msg_d6ed2d4b9bae))
                    else { dao.saveNote(JournalNote(bookId = book.id, type = "Quote", body = selection!!.text.highlight!!, location = selection.toJSON().toString())); navigator?.clearSelection(); toast(L10n.text(R.string.msg_385215bff8be)) }
                } }) { Text(L10n.text(R.string.msg_34b067092ef2)) }
                TextButton({ lifecycleScope.launch { val selected = navigator?.currentSelection()?.locator?.text?.highlight
                    if (selected.isNullOrBlank()) toast(L10n.text(R.string.msg_7af3584bbdf3)) else speech.speak(selected)
                } }) { Text(L10n.text(R.string.msg_556c80850962)) }
                IconButton({ if (speech.playing) speech.pause() else speech.resume() }) { Icon(if (speech.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, L10n.text(R.string.msg_16d3e999d656)) }
                IconButton({ speech.stop() }) { Icon(Icons.Outlined.Stop, L10n.text(R.string.msg_6b4c06e6e74a)) }
                TextButton({ rate = if (rate < 1.5f) rate + .25f else .75f; speech.rate(rate) }) { Text("${rate}×") }
            }
            TextButton({ narrationJob?.cancel(); narrationJob = lifecycleScope.launch {
                val start = navigator?.currentLocator?.value
                if (start == null) return@launch
                val text = withContext(Dispatchers.IO) {
                    val iterator = p.content(start)?.iterator() ?: return@withContext ""
                    val output = StringBuilder()
                    while (iterator.hasNext() && output.length < 50_000) {
                        val element = iterator.next()
                        if (element.locator.href != start.href) break
                        if (element is Content.TextualElement) { output.append(element.text.orEmpty().take(50_000 - output.length)); output.append('\n') }
                    }
                    output.toString()
                }
                if (text.isBlank()) toast(L10n.text(R.string.msg_df56ef4fe07e)) else speech.speak(text)
            } }, modifier = Modifier.fillMaxWidth()) { Text(L10n.text(R.string.msg_06d9c793d7ab)) }
        }
        if (chapters) AlertDialog({ chapters = false }, title = { Text(L10n.text(R.string.msg_9831375bc651)) }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                (p.tableOfContents.ifEmpty { p.readingOrder }).forEachIndexed { i, link -> TextButton({ navigator?.go(link); chapters = false }) { Text(link.title ?: L10n.text(R.string.msg_436121636076, i + 1)) } }
            }
        }, confirmButton = { TextButton({ chapters = false }) { Text(L10n.text(R.string.msg_7d9eb7acb13e)) } })
        if (bookmarks) AlertDialog({ bookmarks = false }, title = { Text(L10n.text(R.string.msg_e028df5e18b4)) }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                val places = notes.filter { it.bookId == book.id && it.location.startsWith("{") }
                if (places.isEmpty()) Text(L10n.text(R.string.msg_0853cef4e268))
                places.forEach { n -> TextButton({ runCatching { Locator.fromJSON(JSONObject(n.location)) }.getOrNull()?.let { navigator?.go(it) }; bookmarks = false }) { Text(n.body.take(100)) } }
            }
        }, confirmButton = { TextButton({ bookmarks = false }) { Text(L10n.text(R.string.msg_7d9eb7acb13e)) } })
        if (searching) AlertDialog({ searching = false }, title = { Text(L10n.text(R.string.msg_837dda3ef200)) }, text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Field(searchText, { searchText = it }, L10n.text(R.string.msg_e9a2e1079d1c))
                TextButton({ lifecycleScope.launch {
                    searchBusy = true; searchStatus = ""; matches = emptyList()
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val iterator = p.search(searchText) ?: error(L10n.text(R.string.msg_09a513118992))
                            try {
                                val found = mutableListOf<Locator>()
                                while (found.size < 100) {
                                    val page = iterator.next().getOrNull() ?: break
                                    if (page.locators.isEmpty()) break
                                    found += page.locators.take(100 - found.size)
                                }
                                found
                            } finally { iterator.close() }
                        }
                    }.onSuccess { matches = it; searchStatus = if (it.isEmpty()) L10n.text(R.string.msg_eb82569d8230) else L10n.text(R.string.msg_3ed32c695f7c, it.size) }
                        .onFailure { searchStatus = it.message ?: L10n.text(R.string.msg_5009b2086617) }
                    searchBusy = false
                } }, enabled = searchText.isNotBlank() && !searchBusy) { Text(if (searchBusy) L10n.text(R.string.msg_c31723ab3302) else L10n.text(R.string.msg_49c266baaaa7)) }
                Text(searchStatus)
                matches.forEach { loc -> TextButton({ navigator?.go(loc); searching = false }) { Text(listOfNotNull(loc.text.before?.takeLast(35), loc.text.highlight, loc.text.after?.take(70)).joinToString(" ").ifBlank { loc.title ?: L10n.text(R.string.msg_56aa1e9d604e) }) } }
            }
        }, confirmButton = { TextButton({ searching = false }) { Text(L10n.text(R.string.msg_7d9eb7acb13e)) } })
    }
    private fun applyPreferences(font: Double, spacing: Double, dark: Boolean) { navigator?.submitPreferences(EpubPreferences(fontSize = font, lineHeight = spacing, theme = if (dark) Theme.DARK else Theme.SEPIA, publisherStyles = false)) }
    private fun saveBookmark(book: Book) { navigator?.currentLocator?.value?.let { loc -> lifecycleScope.launch { dao.saveNote(JournalNote(bookId = book.id, type = "Reflection", body = L10n.text(R.string.msg_a43cafc82922, loc.title ?: book.title), location = loc.toJSON().toString())); toast(L10n.text(R.string.msg_1ae4f9515b01)) } } }
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    override fun onStop() { super.onStop(); narrationJob?.cancel(); speech.pause() }
    override fun onDestroy() { speech.close(); publication?.close(); super.onDestroy() }
}
