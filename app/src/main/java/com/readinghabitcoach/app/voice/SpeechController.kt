package com.readinghabitcoach.app.voice

import com.readinghabitcoach.app.i18n.L10n
import com.readinghabitcoach.app.R

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
    private var generation = 0L
    private var engine: TextToSpeech? = null
    init {
        engine = TextToSpeech(context) { status -> handler.post {
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                voices = engine?.voices?.filter { !it.isNetworkConnectionRequired }?.sortedBy { it.name } ?: emptyList()
                val selected = voices.firstOrNull { it.locale == Locale.getDefault() }
                    ?: voices.firstOrNull { it.locale.language == Locale.getDefault().language } ?: voices.firstOrNull()
                if (selected != null) engine?.voice = selected
                else { ready = false; error = L10n.text(R.string.msg_7377feb6ef3c) }
            }
            else error = L10n.text(R.string.msg_03964f1649f5)
        } }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { handler.post { if (playing && id == "$generation:$index") { index++; next() } } }
            @Deprecated("Legacy TTS callback") override fun onError(id: String?) { handler.post { playing = false; error = L10n.text(R.string.msg_2355d7c2a0e1) } }
        })
    }
    fun speak(text: String) {
        if (!ready || engine?.voice?.isNetworkConnectionRequired != false) { error = L10n.text(R.string.msg_9fa7a63b49a1); return }
        stop(); chunks = text.split(Regex("(?<=[.!?])\\s+|\\n+")).flatMap { it.chunked(1500) }.filter { it.isNotBlank() }
        index = 0; playing = true; next()
    }
    private fun next() { if (index >= chunks.size) { playing = false; return }; engine?.speak(chunks[index], TextToSpeech.QUEUE_FLUSH, null, "$generation:$index") }
    fun pause() { playing = false; generation++; engine?.stop() }
    fun resume() { if (ready && index < chunks.size) { playing = true; next() } }
    fun stop() { playing = false; generation++; engine?.stop(); chunks = emptyList(); index = 0 }
    fun rate(value: Float) { rate = value; engine?.setSpeechRate(value) }
    fun voice(voice: Voice) { if (!voice.isNetworkConnectionRequired) engine?.voice = voice }
    fun close() { stop(); engine?.shutdown(); engine = null }
}

class VoiceInput(context: Context, private val result: (String) -> Unit, private val state: (String) -> Unit) {
    val available = SpeechRecognizer.isRecognitionAvailable(context)
    private val recognizer = if (available) SpeechRecognizer.createSpeechRecognizer(context) else null
    init { recognizer?.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { state(L10n.text(R.string.msg_bbb4106e8144)) }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { state(L10n.text(R.string.msg_a8a1b7fc0410)) }
        override fun onError(error: Int) { state(when (error) { SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> L10n.text(R.string.msg_a0c78d42d4bb); SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> L10n.text(R.string.msg_aea28b7fc15a); else -> L10n.text(R.string.msg_661f4a4b1085) }) }
        override fun onResults(results: Bundle?) { val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull(); state(L10n.text(R.string.msg_182bf2ce919b)); text?.let(result) }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }) }
    fun listen(language: String) { recognizer?.startListening(android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, language).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)) }
    fun cancel() { recognizer?.cancel() }
    fun close() { recognizer?.destroy() }
}
