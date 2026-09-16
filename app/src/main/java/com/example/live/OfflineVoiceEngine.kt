package com.example.live

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class OfflineVoiceEngine(
    private val context: Context,
    private val toolEngine: ToolExecutionEngine,
    private val onStateChanged: (ZoyaState) -> Unit,
    private val onTranscript: (String) -> Unit,
    private val onAssistantResponse: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var isListening = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                try {
                    // Try Hindi first, fallback to English
                    val result = textToSpeech?.setLanguage(Locale("hi", "IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.US)
                    }
                    textToSpeech?.setPitch(1.08f)
                    textToSpeech?.setSpeechRate(1.05f)

                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            onStateChanged(ZoyaState.SPEAKING)
                        }

                        override fun onDone(utteranceId: String?) {
                            onStateChanged(ZoyaState.LISTENING)
                        }

                        override fun onError(utteranceId: String?) {
                            onStateChanged(ZoyaState.LISTENING)
                        }
                    })
                    Log.i("OfflineVoiceEngine", "TextToSpeech initialized successfully.")
                } catch (e: Exception) {
                    Log.e("OfflineVoiceEngine", "TTS setup error", e)
                }
            } else {
                Log.e("OfflineVoiceEngine", "Failed to initialize TextToSpeech.")
            }
        }
    }

    fun startListening() {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                        Log.w("OfflineVoiceEngine", "Speech recognition not available on device")
                        speak("Speech recognition device par available nahi hai.")
                        return@post
                    }
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer?.setRecognitionListener(createRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("hi-IN", "en-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                onStateChanged(ZoyaState.LISTENING)
                Log.i("OfflineVoiceEngine", "Offline speech recognition started.")
            } catch (e: Exception) {
                Log.e("OfflineVoiceEngine", "Error starting speech recognizer", e)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("OfflineVoiceEngine", "Error stopping speech recognizer", e)
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onStateChanged(ZoyaState.LISTENING)
        }

        override fun onBeginningOfSpeech() {
            onStateChanged(ZoyaState.LISTENING)
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            onStateChanged(ZoyaState.THINKING)
        }

        override fun onError(error: Int) {
            Log.w("OfflineVoiceEngine", "SpeechRecognizer error: $error")
            onStateChanged(ZoyaState.IDLE)
            // Auto restart if continuous
            if (isListening) {
                mainHandler.postDelayed({
                    if (isListening) startListening()
                }, 1000)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spokenText = matches?.firstOrNull() ?: return
            Log.i("OfflineVoiceEngine", "Recognized speech: $spokenText")
            onTranscript(spokenText)
            processOfflineCommand(spokenText)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: return
            onTranscript(partial)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun processOfflineCommand(query: String) {
        scope.launch {
            onStateChanged(ZoyaState.THINKING)
            val lower = query.lowercase().trim()
            val reply: String

            when {
                // Flashlight / Torch
                lower.contains("torch on") || lower.contains("flashlight on") || lower.contains("torch jalao") || lower.contains("torch chalao") || lower.contains("light jalao") -> {
                    toolEngine.toggleTorch("on")
                    reply = "Torch chalu kar di hai."
                }
                lower.contains("torch off") || lower.contains("flashlight off") || lower.contains("torch band") || lower.contains("light band") -> {
                    toolEngine.toggleTorch("off")
                    reply = "Torch band kar di hai."
                }

                // Call Contact
                lower.startsWith("call ") || lower.contains("ko call") || lower.contains("phone lagao") || lower.contains("call lagao") -> {
                    val contactName = extractContactName(query)
                    if (contactName.isNotBlank()) {
                        val result = toolEngine.callContact(contactName, useDialer = true)
                        reply = "Maine $contactName ke liye dialer open kar diya hai."
                    } else {
                        reply = "Aap kisko call lagana chahte hain? Naam batayein."
                    }
                }

                // Volume Controls
                lower.contains("volume full") || lower.contains("full volume") -> {
                    toolEngine.setVolumePercent(100)
                    reply = "Volume full kar diya hai."
                }
                lower.contains("volume badhao") || lower.contains("volume up") || lower.contains("awaaz badhao") -> {
                    toolEngine.adjustSystemVolume("up")
                    reply = "Volume badha diya hai."
                }
                lower.contains("volume kam") || lower.contains("volume down") || lower.contains("awaaz kam") -> {
                    toolEngine.adjustSystemVolume("down")
                    reply = "Volume kam kar diya hai."
                }
                lower.contains("mute") || lower.contains("silent") -> {
                    toolEngine.adjustSystemVolume("mute")
                    reply = "Phone mute kar diya hai."
                }
                lower.contains("volume") && extractNumber(lower) != null -> {
                    val percent = extractNumber(lower) ?: 50
                    toolEngine.setVolumePercent(percent)
                    reply = "Volume $percent percent set kar diya hai."
                }

                // Brightness Controls
                lower.contains("brightness full") -> {
                    toolEngine.setBrightness(100)
                    reply = "Screen brightness full kar di hai."
                }
                lower.contains("brightness kam") -> {
                    toolEngine.setBrightness(20)
                    reply = "Brightness kam kar di hai."
                }
                lower.contains("brightness badhao") -> {
                    toolEngine.setBrightness(85)
                    reply = "Brightness badha di hai."
                }
                lower.contains("brightness") && extractNumber(lower) != null -> {
                    val level = extractNumber(lower) ?: 50
                    toolEngine.setBrightness(level)
                    reply = "Brightness $level percent set kar di hai."
                }

                // Battery Telemetry
                lower.contains("battery") || lower.contains("charge") || lower.contains("charging") -> {
                    reply = toolEngine.getBatteryStatus()
                }

                // Time & Date
                lower.contains("time") || lower.contains("baje") || lower.contains("samay") || lower.contains("date") || lower.contains("tarikh") -> {
                    reply = toolEngine.getCurrentTimeAndDate()
                }

                // App Opening
                lower.startsWith("open ") || lower.endsWith("kholo") || lower.contains("chalu karo") -> {
                    val appName = extractAppName(query)
                    val result = toolEngine.openAppGeneric(appName)
                    reply = if (result.contains("launched", ignoreCase = true) || result.contains("opened", ignoreCase = true)) {
                        "$appName open kar diya hai."
                    } else {
                        result
                    }
                }

                // YouTube Search
                lower.startsWith("play ") || lower.contains("youtube") -> {
                    val cleanQuery = lower.replace("play", "").replace("youtube", "").replace("par chalao", "").replace("chalao", "").trim()
                    toolEngine.searchYouTube(cleanQuery)
                    reply = "YouTube par $cleanQuery search kar diya hai."
                }

                // 2050 Cyber Assistant Persona
                lower.contains("kaun ho") || lower.contains("who are you") || lower.contains("naam kya hai") -> {
                    reply = "Main Zoya hoon, aapki 2050 Futuristic Quantum AI Assistant! Main Online aur Offline dono me aapka phone control kar sakti hoon."
                }
                lower.contains("kaise ho") || lower.contains("how are you") -> {
                    reply = "Main 100 percent active aur ready hoon! Aap batayein aaj kya order hai?"
                }
                lower.contains("namaste") || lower.contains("hello") || lower.contains("hi zoya") -> {
                    reply = "Namaste! Zoya 2050 Cyber Core online hai. Kahiye, kya kaam karna hai?"
                }
                lower.contains("kya kar sakti ho") || lower.contains("help") -> {
                    reply = "Main bina internet ke bhi call dial, torch, brightness, volume, apps open, battery aur time batana sab instant handle kar sakti hoon!"
                }

                else -> {
                    reply = "Offline Neural Core me ye command register hui: $query. Call, torch, volume, app open ya battery status bol kar dekhein!"
                }
            }

            onAssistantResponse(reply)
            speak(reply)
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || textToSpeech == null) {
            Log.w("OfflineVoiceEngine", "TTS not ready to speak: $text")
            return
        }

        // Auto detect Hindi vs English text
        val isHindi = text.any { it in '\u0900'..'\u097F' } ||
                text.contains("hai", ignoreCase = true) ||
                text.contains("hoon", ignoreCase = true) ||
                text.contains("kar di", ignoreCase = true)

        try {
            if (isHindi) {
                textToSpeech?.setLanguage(Locale("hi", "IN"))
            } else {
                textToSpeech?.setLanguage(Locale.US)
            }
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zoya_offline_utterance")
        } catch (e: Exception) {
            Log.e("OfflineVoiceEngine", "Speech synthesis error", e)
        }
    }

    private fun extractContactName(query: String): String {
        return query.replace(Regex("(?i)call|lagao|karo|ko|phone|please"), "").trim()
    }

    private fun extractAppName(query: String): String {
        return query.replace(Regex("(?i)open|kholo|chalu|karo|app|please"), "").trim()
    }

    private fun extractNumber(text: String): Int? {
        val regex = Regex("\\b(\\d{1,3})\\b")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            } catch (e: Exception) {
                Log.e("OfflineVoiceEngine", "Error destroying engine", e)
            }
        }
    }
}
