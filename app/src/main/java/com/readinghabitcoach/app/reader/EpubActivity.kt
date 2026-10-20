@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
package com.readinghabitcoach.app.reader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
    private var publication: Publication? = null
    private var navigator by mutableStateOf<EpubNavigatorFragment?>(null)
    private lateinit var speech: SpeechController
    private var currentBook: Book? = null
    private val dao get() = (application as CoachApplication).database.dao()
    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        super.onCreate(null) // Reconstruct the publication before restoring its navigator.
        speech = SpeechController(this)
        setContent { CoachTheme { Surface(Modifier.fillMaxSize()) { Box(Modifier.padding(32.dp)) { Text("Opening your book…", style = MaterialTheme.typography.headlineMedium) } } } }
        lifecycleScope.launch {
            runCatching {
                val b = dao.book(intent.getStringExtra("bookId") ?: "") ?: error("This book could not be found.")
                currentBook = b
                val http = DefaultHttpClient()
                val assets = AssetRetriever(contentResolver, http)
                val opener = PublicationOpener(DefaultPublicationParser(this@EpubActivity, httpClient = http, assetRetriever = assets, pdfFactory = null))
                val asset = assets.retrieve(DocumentImporter(this@EpubActivity).file(b).toUrl()).getOrNull() ?: error("This EPUB could not be opened.")
                val p = opener.open(asset, allowUserInteraction = false).getOrNull() ?: error("Unsupported or protected EPUB.")
                publication = p
                val location = runCatching { Locator.fromJSON(JSONObject(b.readerPosition)) }.getOrNull()
                supportFragmentManager.fragmentFactory = EpubNavigatorFactory(p).createFragmentFactory(initialLocator = location,
                    initialPreferences = EpubPreferences(theme = Theme.SEPIA, fontSize = 1.1, lineHeight = 1.6))
                setContent { CoachTheme { ReaderContent(b, p) } }
            }.onFailure { e -> setContent { CoachTheme { Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(32.dp)) {
                Text("Unable to open this document", style = MaterialTheme.typography.headlineMedium); Text(e.message ?: "Try another DRM-free EPUB."); TextButton({ finish() }) { Text("Back to library") }
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
                IconButton({ finish() }) { Icon(Icons.Outlined.ArrowBack, "Close reader") }
                Text(book.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 1)
                IconButton({ chapters = true }) { Icon(Icons.Outlined.List, "Chapters") }
                IconButton({ searching = true }) { Icon(Icons.Outlined.Search, "Search in book") }
                IconButton({ bookmarks = true }) { Icon(Icons.Outlined.Bookmarks, "Bookmarks and highlights") }
                IconButton({ saveBookmark(book) }) { Icon(Icons.Outlined.BookmarkAdd, "Add bookmark") }
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
                IconButton({ navigator?.goBackward() }) { Icon(Icons.Outlined.ChevronLeft, "Previous page") }
                TextButton({ font = (font - .1).coerceAtLeast(.7); applyPreferences(font, spacing, dark) }) { Text("A−") }
                TextButton({ font = (font + .1).coerceAtMost(2.5); applyPreferences(font, spacing, dark) }) { Text("A+") }
                TextButton({ spacing = if (spacing < 1.9) spacing + .2 else 1.2; applyPreferences(font, spacing, dark) }) { Text("Spacing") }
                IconButton({ dark = !dark; applyPreferences(font, spacing, dark) }) { Icon(Icons.Outlined.Contrast, "Toggle reader theme") }
                IconButton({ navigator?.goForward() }) { Icon(Icons.Outlined.ChevronRight, "Next page") }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton({ lifecycleScope.launch { val selection = navigator?.currentSelection()?.locator
                    if (selection?.text?.highlight.isNullOrBlank()) toast("Select some text in the book first.")
                    else { dao.saveNote(JournalNote(bookId = book.id, type = "Quote", body = selection!!.text.highlight!!, location = selection.toJSON().toString())); navigator?.clearSelection(); toast("Selection saved to your journal.") }
                } }) { Text("Keep selection") }
                TextButton({ lifecycleScope.launch { val selected = navigator?.currentSelection()?.locator?.text?.highlight
                    if (selected.isNullOrBlank()) toast("Select text to read aloud.") else speech.speak(selected)
                } }) { Text("Read selection") }
                IconButton({ if (speech.playing) speech.pause() else speech.resume() }) { Icon(if (speech.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, "Pause or resume read-aloud") }
                IconButton({ speech.stop() }) { Icon(Icons.Outlined.Stop, "Stop read-aloud") }
                TextButton({ rate = if (rate < 1.5f) rate + .25f else .75f; speech.rate(rate) }) { Text("${rate}×") }
            }
            TextButton({ lifecycleScope.launch {
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
                if (text.isBlank()) toast("This chapter has no compatible extractable text.") else speech.speak(text)
            } }, modifier = Modifier.fillMaxWidth()) { Text("Read this chapter aloud (up to 50,000 characters)") }
        }
        if (chapters) AlertDialog({ chapters = false }, title = { Text("Chapters") }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                (p.tableOfContents.ifEmpty { p.readingOrder }).forEachIndexed { i, link -> TextButton({ navigator?.go(link); chapters = false }) { Text(link.title ?: "Chapter ${i + 1}") } }
            }
        }, confirmButton = { TextButton({ chapters = false }) { Text("Close") } })
        if (bookmarks) AlertDialog({ bookmarks = false }, title = { Text("Your places") }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                val places = notes.filter { it.bookId == book.id && it.location.startsWith("{") }
                if (places.isEmpty()) Text("Bookmarks and saved selections will appear here.")
                places.forEach { n -> TextButton({ runCatching { Locator.fromJSON(JSONObject(n.location)) }.getOrNull()?.let { navigator?.go(it) }; bookmarks = false }) { Text(n.body.take(100)) } }
            }
        }, confirmButton = { TextButton({ bookmarks = false }) { Text("Close") } })
        if (searching) AlertDialog({ searching = false }, title = { Text("Search this book") }, text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Field(searchText, { searchText = it }, "Word or phrase")
                TextButton({ lifecycleScope.launch {
                    searchBusy = true; searchStatus = ""; matches = emptyList()
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val iterator = p.search(searchText) ?: error("Search is unavailable for this document.")
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
                    }.onSuccess { matches = it; searchStatus = if (it.isEmpty()) "No matches found." else "${it.size} matches (up to 100 shown)" }
                        .onFailure { searchStatus = it.message ?: "Search could not be completed." }
                    searchBusy = false
                } }, enabled = searchText.isNotBlank() && !searchBusy) { Text(if (searchBusy) "Searching…" else "Search") }
                Text(searchStatus)
                matches.forEach { loc -> TextButton({ navigator?.go(loc); searching = false }) { Text(listOfNotNull(loc.text.before?.takeLast(35), loc.text.highlight, loc.text.after?.take(70)).joinToString(" ").ifBlank { loc.title ?: "Go to result" }) } }
            }
        }, confirmButton = { TextButton({ searching = false }) { Text("Close") } })
    }
    private fun applyPreferences(font: Double, spacing: Double, dark: Boolean) { navigator?.submitPreferences(EpubPreferences(fontSize = font, lineHeight = spacing, theme = if (dark) Theme.DARK else Theme.SEPIA, publisherStyles = false)) }
    private fun saveBookmark(book: Book) { navigator?.currentLocator?.value?.let { loc -> lifecycleScope.launch { dao.saveNote(JournalNote(bookId = book.id, type = "Reflection", body = "Bookmark · ${loc.title ?: book.title}", location = loc.toJSON().toString())); toast("Bookmark saved.") } } }
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    override fun onStop() { super.onStop(); speech.pause() }
    override fun onDestroy() { speech.close(); publication?.close(); super.onDestroy() }
}
