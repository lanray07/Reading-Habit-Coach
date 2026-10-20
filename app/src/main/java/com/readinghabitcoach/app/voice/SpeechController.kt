package com.readinghabitcoach.app.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.*
import android.speech.tts.*
import androidx.compose.runtime.*
import java.util.Locale

class SpeechController(private val context: Context) {
    var ready by mutableStateOf(false); private set
    var playing by mutableStateOf(false); private set
    var rate by mutableFloatStateOf(1f); private set
    var voices by mutableStateOf<List<Voice>>(emptyList()); private set
    var error by mutableStateOf(""); private set
    private val handler = Handler(Looper.getMainLooper())
    private var chunks: List<String> = emptyList(); private var index = 0
    private var engine: TextToSpeech? = null
    init {
        engine = TextToSpeech(context) { status -> handler.post {
            ready = status == TextToSpeech.SUCCESS
            if (ready) { engine?.language = Locale.getDefault(); voices = engine?.voices?.filter { !it.isNetworkConnectionRequired }?.sortedBy { it.name } ?: emptyList() }
            else error = "No compatible text-to-speech engine is available on this device."
        } }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { handler.post { if (playing && id == index.toString()) { index++; next() } } }
            @Deprecated("Legacy TTS callback") override fun onError(id: String?) { handler.post { playing = false; error = "This voice could not read the text." } }
        })
    }
    fun speak(text: String) {
        if (!ready) { error = "Read-aloud is unavailable. You can still read all text on screen."; return }
        stop(); chunks = text.split(Regex("(?<=[.!?])\\s+|\\n+")).flatMap { it.chunked(1500) }.filter { it.isNotBlank() }
        index = 0; playing = true; next()
    }
    private fun next() { if (index >= chunks.size) { playing = false; return }; engine?.speak(chunks[index], TextToSpeech.QUEUE_FLUSH, null, index.toString()) }
    fun pause() { playing = false; engine?.stop() }
    fun resume() { if (ready && index < chunks.size) { playing = true; next() } }
    fun stop() { playing = false; engine?.stop(); chunks = emptyList(); index = 0 }
    fun rate(value: Float) { rate = value; engine?.setSpeechRate(value) }
    fun voice(voice: Voice) { engine?.voice = voice }
    fun close() { stop(); engine?.shutdown(); engine = null }
}

class VoiceInput(context: Context, private val result: (String) -> Unit, private val state: (String) -> Unit) {
    val available = SpeechRecognizer.isRecognitionAvailable(context)
    private val recognizer = if (available) SpeechRecognizer.createSpeechRecognizer(context) else null
    init { recognizer?.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { state("Listening…") }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { state("Processing your command…") }
        override fun onError(error: Int) { state(when (error) { SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed to use voice."; SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No command recognised. Try again or type below."; else -> "Speech recognition is unavailable right now. Use the text command field." }) }
        override fun onResults(results: Bundle?) { val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull(); state("Ready when you are."); text?.let(result) }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }) }
    fun listen(language: String) { recognizer?.startListening(android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, language).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)) }
    fun cancel() { recognizer?.cancel() }
    fun close() { recognizer?.destroy() }
}
