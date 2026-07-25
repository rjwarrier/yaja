package com.mj.yaja.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.mj.yaja.R
import java.util.Locale

/**
 * Whether the device has a speech recognition service is a fixed device capability
 * for the life of the process, but [SpeechRecognizer.isRecognitionAvailable] queries
 * PackageManager over a Binder call every time it's asked. This dialog reopens on
 * every quick-capture, so caching the result once avoids repeating that IPC on the
 * main thread each time — exactly the kind of thing quick-capture can't afford.
 */
private object VoiceRecognitionAvailability {
    @Volatile private var cached: Boolean? = null

    fun isAvailable(context: Context): Boolean =
        cached ?: SpeechRecognizer.isRecognitionAvailable(context).also { cached = it }
}

/**
 * Mic button that transcribes speech and hands the recognized text to [onResult].
 * Requests RECORD_AUDIO at tap time (no permission requested up front) and hints
 * EXTRA_PREFER_OFFLINE so the platform recognizer prefers an on-device model where
 * one is installed — no network permission is used by Yaja itself either way.
 * Renders nothing if the device has no speech recognizer at all.
 */
@Composable
fun VoiceCaptureButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    val latestOnResult by rememberUpdatedState(onResult)

    val recognitionAvailable = remember { VoiceRecognitionAvailability.isAvailable(context) }
    val recognizer = remember {
        if (recognitionAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer?.destroy() }
    }

    fun startListening() {
        val activeRecognizer = recognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        activeRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { latestOnResult(it) }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        activeRecognizer.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() }

    if (recognizer != null) {
        IconButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    startListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = if (isListening) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                contentDescription = stringResource(R.string.cd_voice_capture),
                tint = if (isListening) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
