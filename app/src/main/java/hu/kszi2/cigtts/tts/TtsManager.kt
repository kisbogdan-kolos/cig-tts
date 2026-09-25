package hu.kszi2.cigtts.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "Language not supported")
            } else {
                isInitialized = true
                setupUtteranceListener()
            }
        } else {
            Log.e("TtsManager", "Initialization failed")
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Audio focus is acquired before calling speak()
            }

            override fun onDone(utteranceId: String?) {
                abandonAudioFocus()
            }

            override fun onError(utteranceId: String?) {
                abandonAudioFocus()
            }
        })
    }

    fun speak(text: String) {
        if (!isInitialized || tts == null) {
            Log.e("TtsManager", "TTS not initialized")
            return
        }

        if (requestAudioFocus()) {
            val utteranceId = "utterance_${System.currentTimeMillis()}"
            
            // Set audio attributes for ducking
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            Log.e("TtsManager", "Failed to acquire audio focus")
        }
    }

    private fun requestAudioFocus(): Boolean {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { _ ->
                // Handle focus changes if needed
            }
            .build()
        
        audioFocusRequest = request
        val result = audioManager.requestAudioFocus(request)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        abandonAudioFocus()
    }
}
