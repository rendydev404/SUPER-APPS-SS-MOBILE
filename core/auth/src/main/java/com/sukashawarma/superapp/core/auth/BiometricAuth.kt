package com.sukashawarma.superapp.core.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object BiometricAuth {
    fun isAvailable(activity: Activity?): Boolean {
        if (activity !is FragmentActivity) return false
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        return BiometricManager.from(activity).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(
        activity: Activity,
        title: String = "Masuk ke SUKA Superapp",
        subtitle: String = "Gunakan sidik jari untuk membuka akun Anda",
        negativeButtonText: String = "Gunakan password"
    ): Boolean {
        if (activity !is FragmentActivity || !isAvailable(activity)) return false
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) continuation.resume(false)
                    }

                    override fun onAuthenticationFailed() = Unit
                }
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText(negativeButtonText)
                    .setConfirmationRequired(true)
                    .build()
            )
        }
    }
}
