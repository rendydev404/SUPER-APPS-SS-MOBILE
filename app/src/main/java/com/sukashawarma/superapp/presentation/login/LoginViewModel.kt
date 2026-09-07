package com.sukashawarma.superapp.presentation.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.session.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    /** Tombol sidik jari hanya ditawarkan bila staff terakhir di perangkat ini
     *  memang mengaktifkannya. Password tidak pernah lagi diisi otomatis. */
    val biometricEnabled: Boolean = AuthPrefs.isBiometricEnabledForLastActiveUser(),
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onIdentifierChange(nilai: String) {
        _state.value = _state.value.copy(identifier = nilai, error = null)
    }

    fun onPasswordChange(nilai: String) {
        _state.value = _state.value.copy(password = nilai, error = null)
    }

    /**
     * Buka sesi dengan sidik jari.
     *
     * Refresh token baru dibaca SETELAH prompt biometrik berhasil — bukan
     * sebelumnya — sehingga tidak ada jalur yang memakai token tersimpan tanpa
     * melewati sidik jari.
     */
    fun loginWithBiometric(activity: Activity, onSuccess: () -> Unit) {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            val cocok = BiometricAuth.authenticate(activity, "Masuk ke SUKA Superapp")
            if (!cocok) {
                _state.value = _state.value.copy(loading = false)
                return@launch
            }

            val token = AuthPrefs.getRefreshToken()
            if (token.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Sesi tersimpan sudah tidak ada. Silakan login dengan password.",
                )
                return@launch
            }

            when (val hasil = AppSession.loginWithBiometric(token)) {
                is LoginResult.Success -> {
                    _state.value = _state.value.copy(loading = false)
                    onSuccess()
                }
                is LoginResult.Failure -> {
                    _state.value = _state.value.copy(loading = false, error = hasil.message)
                }
            }
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.loading) return
        if (s.identifier.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Username dan password wajib diisi.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val hasil = AppSession.login(s.identifier, s.password)) {
                is LoginResult.Success -> {
                    _state.value = _state.value.copy(loading = false)
                    onSuccess()
                }
                is LoginResult.Failure -> {
                    _state.value = _state.value.copy(loading = false, error = hasil.message)
                }
            }
        }
    }
}
