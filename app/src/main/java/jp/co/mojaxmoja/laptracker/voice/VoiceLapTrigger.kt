package jp.co.mojaxmoja.laptracker.voice

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceLapTrigger(
    private val context: Context,
    private val usePeakDetector: Boolean = false, // Sound amplitude peak detector mode for 0ms delay
    private val onTriggered: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var isRecordThreadRunning = false
    private var recordThread: Thread? = null

    private var isEnabled = false
    private var lastTriggerTimeMillis = 0L
    private val cooldownMillis = 1200L // 1.2s cooldown to avoid double triggers

    private val targetKeywords = listOf(
        // Distance numbers
        "400", "よんひゃく", "ヨンヒャク", "四百",
        "800", "はっぴゃく", "ハッピャク", "八百",
        "1000", "せん", "いっせん", "千", "一千", "セン",
        "1200", "せんにひゃく", "センニヒャク", "千二百",
        "1500", "せんごひゃく", "センゴヒャク", "千五百",
        "1600", "せんろっぴゃく", "センロッピャク", "千六百",
        "2000", "にせん", "ニセン", "二千",
        "2400", "にせんよんひゃく", "二千四百",
        "2800", "にせんはっぴゃく", "二千八百",
        "3000", "さんぜん", "サンゼン", "三千",
        "5000", "ごせん", "ゴセン", "五千",
        // General action triggers
        "ラップ", "らっぷ", "はい", "ハイ", "はいっ", "通過", "つうか",
        "次", "つぎ", "オッケー", "おっけー", "ゴー", "go", "4", "8"
    )

    fun startListening() {
        isEnabled = true
        if (usePeakDetector) {
            startPeakDetector()
        } else {
            startSpeechRecognizer()
        }
    }

    private fun startSpeechRecognizer() {
        if (speechRecognizer == null) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } else {
                Log.w("VoiceLapTrigger", "Speech recognition not available on device")
                return
            }
        }
        restartListening()
    }

    private fun restartListening() {
        if (!isEnabled || usePeakDetector) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceLapTrigger", "Error starting speech listening", e)
        }
    }

    private fun startPeakDetector() {
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            audioRecord?.startRecording()
            isRecordThreadRunning = true

            recordThread = Thread {
                val buffer = ShortArray(bufferSize)
                while (isRecordThreadRunning) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        var maxAmplitude = 0
                        for (i in 0 until read) {
                            val absVal = kotlin.math.abs(buffer[i].toInt())
                            if (absVal > maxAmplitude) {
                                maxAmplitude = absVal
                            }
                        }
                        // Trigger if amplitude peak threshold (> 18000) is reached with 0ms latency
                        if (maxAmplitude > 18000) {
                            val now = System.currentTimeMillis()
                            if (now - lastTriggerTimeMillis > cooldownMillis) {
                                lastTriggerTimeMillis = now
                                Log.d("VoiceLapTrigger", "Peak detected! Amplitude: $maxAmplitude")
                                onTriggered("Peak: $maxAmplitude")
                            }
                        }
                    }
                }
            }.apply { start() }
        } catch (e: Exception) {
            Log.e("VoiceLapTrigger", "Error starting AudioRecord peak detector", e)
        }
    }

    fun stopListening() {
        isEnabled = false
        isRecordThreadRunning = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordThread = null

        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (isEnabled && !usePeakDetector) {
                    restartListening()
                }
            }

            override fun onResults(results: Bundle?) {
                processBundle(results)
                if (isEnabled && !usePeakDetector) restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                processBundle(partialResults)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun processBundle(bundle: Bundle?) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTimeMillis < cooldownMillis) return // Cooldown check

        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (text in matches) {
            val cleanText = text.trim()
            Log.d("VoiceLapTrigger", "Speech partial heard: $cleanText")
            val hasDigits = cleanText.any { it.isDigit() }
            val matchesKeyword = targetKeywords.any { cleanText.contains(it, ignoreCase = true) }

            if (hasDigits || matchesKeyword) {
                lastTriggerTimeMillis = now
                Log.d("VoiceLapTrigger", "Trigger matched instantly: $cleanText")
                onTriggered(cleanText)
                break
            }
        }
    }
}
