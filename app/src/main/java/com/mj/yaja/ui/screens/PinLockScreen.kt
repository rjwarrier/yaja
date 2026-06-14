package com.mj.yaja.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.mj.yaja.R
import kotlinx.coroutines.delay

/** Walk the ContextWrapper chain to find the underlying FragmentActivity.
 *  LocalContext.current in Compose is often a wrapped context, so a direct cast fails. */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Determines the behaviour of [PinLockScreen]. */
enum class PinMode {
    /** The user is logging in — verifies the PIN against the stored hash. */
    ENTER,
    /** The user is setting up a new PIN — asks for entry twice to confirm. */
    SETUP,
    /** The user confirms the existing PIN before disabling the lock. */
    DISABLE
}

private const val PIN_LENGTH = 4

private fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
    val biometricManager = BiometricManager.from(activity)

    // Try BIOMETRIC_WEAK first, then fall back to BIOMETRIC_STRONG
    val canAuthenticateWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    val canAuthenticateStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    val canAuthenticate = when {
        canAuthenticateWeak == BiometricManager.BIOMETRIC_SUCCESS -> BiometricManager.BIOMETRIC_SUCCESS
        canAuthenticateStrong == BiometricManager.BIOMETRIC_SUCCESS -> BiometricManager.BIOMETRIC_SUCCESS
        else -> BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    when (canAuthenticate) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.pin_biometric_prompt_title))
                .setSubtitle(activity.getString(R.string.pin_biometric_prompt_subtitle))
                .setAllowedAuthenticators(
                    if (canAuthenticateWeak == BiometricManager.BIOMETRIC_SUCCESS)
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                    else
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                .setNegativeButtonText(activity.getString(R.string.action_cancel))
                .build()

            val biometricPrompt = BiometricPrompt(
                activity,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                            onError(errString.toString())
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Single scan failed — the dialog handles retry UI automatically, no extra action needed
                    }
                }
            )

            biometricPrompt.authenticate(promptInfo)
        }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            onError(activity.getString(R.string.pin_error_no_biometric_enrolled))
        }
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
            onError(activity.getString(R.string.pin_error_hardware_unavailable))
        }
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
            onError(activity.getString(R.string.pin_error_no_hardware))
        }
        else -> {
            onError(activity.getString(R.string.pin_error_not_available))
        }
    }
}

@Composable
fun PinLockScreen(
    mode: PinMode,
    onEnterCorrect: () -> Unit,       // Called after correct PIN in ENTER/DISABLE mode
    onSetupComplete: (String) -> Unit, // Called with the confirmed PIN in SETUP mode
    onCancel: (() -> Unit)? = null,   // Optional cancel (only shown in Settings flows)
    checkPin: ((String) -> Boolean)? = null, // Required for ENTER/DISABLE mode
    isBiometricAvailable: Boolean = false // Whether biometric is available and enabled
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Auto-trigger biometric prompt when screen first opens in ENTER mode
    LaunchedEffect(Unit) {
        if (mode == PinMode.ENTER && isBiometricAvailable) {
            val activity = context.findFragmentActivity()
            if (activity != null) {
                showBiometricPrompt(
                    activity,
                    onSuccess = onEnterCorrect,
                    onError = { error -> errorMessage = error }
                )
            }
        }
    }

    // Shake animation state
    val shakeOffset = remember { Animatable(0f) }

    suspend fun triggerShake() {
        shakeOffset.snapTo(0f)
        shakeOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 400
                10f at 50
                -10f at 100
                10f at 150
                -10f at 200
                6f at 250
                -6f at 300
                0f at 400
            }
        )
    }

    val title = when {
        mode == PinMode.SETUP && isConfirming -> stringResource(R.string.pin_title_confirm)
        mode == PinMode.SETUP -> stringResource(R.string.pin_title_set_new)
        mode == PinMode.DISABLE -> stringResource(R.string.pin_title_disable)
        else -> stringResource(R.string.pin_title_enter)
    }

    // Handle a digit tap
    fun onDigit(digit: String) {
        if (pin.length < PIN_LENGTH) {
            pin += digit
            errorMessage = ""
        }
    }

    // Handle backspace
    fun onBackspace() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
        errorMessage = ""
    }

    // Handle confirm (called automatically when 4th digit entered)
    suspend fun onConfirm() {
        when (mode) {
            PinMode.ENTER, PinMode.DISABLE -> {
                if (checkPin != null && checkPin(pin)) {
                    onEnterCorrect()
                } else {
                    errorMessage = context.getString(R.string.pin_error_incorrect)
                    triggerShake()
                    delay(100)
                    pin = ""
                }
            }
            PinMode.SETUP -> {
                if (!isConfirming) {
                    confirmPin = pin
                    pin = ""
                    isConfirming = true
                } else {
                    if (pin == confirmPin) {
                        onSetupComplete(pin)
                    } else {
                        errorMessage = context.getString(R.string.pin_error_mismatch)
                        triggerShake()
                        delay(100)
                        pin = ""
                        confirmPin = ""
                        isConfirming = false
                    }
                }
            }
        }
    }

    // Auto-submit when 4 digits entered
    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) {
            onConfirm()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {

                // Lock icon
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                // Dot indicators with shake offset
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.toInt(), 0) }
                ) {
                    repeat(PIN_LENGTH) { index ->
                        val filled = index < pin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .border(
                                    2.dp,
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                        )
                    }
                }

                // Error message
                Box(modifier = Modifier.height(20.dp)) {
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Numpad
                PinNumpad(
                    onDigit = ::onDigit,
                    onBackspace = ::onBackspace
                )

                // Biometric button (only in ENTER mode if available)
                if (mode == PinMode.ENTER && isBiometricAvailable) {
                    Spacer(modifier = Modifier.height(12.dp))
                    IconButton(
                        onClick = {
                            val activity = context.findFragmentActivity()
                            if (activity != null) {
                                showBiometricPrompt(
                                    activity,
                                    onSuccess = onEnterCorrect,
                                    onError = { error ->
                                        errorMessage = error
                                    }
                                )
                            }
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = stringResource(R.string.pin_cd_use_biometric),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Cancel button (optional, shown in Settings flows)
                if (onCancel != null) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun PinNumpad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            key.isEmpty() -> Spacer(modifier = Modifier.size(72.dp))
                            key == "⌫" -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable { onBackspace() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = stringResource(R.string.pin_cd_backspace),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .clickable { onDigit(key) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
