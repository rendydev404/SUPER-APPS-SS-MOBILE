package com.sukashawarma.superapp.presentation.absensi.profil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import com.sukashawarma.superapp.data.remote.UpdatePasswordPayload
import com.sukashawarma.superapp.data.remote.authApi
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PasswordChangeResult {
    data object Idle : PasswordChangeResult
    data object Success : PasswordChangeResult
    data class Failure(val message: String) : PasswordChangeResult
}

data class ProfilUiState(
    val staff: StaffProfile? = AppSession.staff.value,
    val changing: Boolean = false,
    val result: PasswordChangeResult = PasswordChangeResult.Idle,
)

/** Ganti password akun sendiri lewat GoTrue `PUT /auth/v1/user` (self-service, pakai
 *  access_token aktif) — cermin halaman "Profil & Password" web, tanpa perlu password
 *  lama (GoTrue tidak minta itu untuk endpoint ini selama access_token masih valid). */
class ProfilViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProfilUiState())
    val state: StateFlow<ProfilUiState> = _state

    fun changePassword(newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _state.value = _state.value.copy(result = PasswordChangeResult.Failure("Password minimal 6 karakter."))
            return
        }
        if (newPassword != confirmPassword) {
            _state.value = _state.value.copy(result = PasswordChangeResult.Failure("Konfirmasi password tidak cocok."))
            return
        }
        val token = SessionTokenHolder.accessToken
        if (token == null) {
            _state.value = _state.value.copy(result = PasswordChangeResult.Failure("Sesi tidak valid, silakan login ulang."))
            return
        }
        _state.value = _state.value.copy(changing = true, result = PasswordChangeResult.Idle)
        viewModelScope.launch {
            try {
                val res = authApi.updatePassword("Bearer $token", UpdatePasswordPayload(newPassword))
                _state.value = _state.value.copy(
                    changing = false,
                    result = if (res.isSuccessful) PasswordChangeResult.Success
                    else PasswordChangeResult.Failure("Gagal mengganti password (${res.code()}).")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(changing = false, result = PasswordChangeResult.Failure("Gagal terhubung ke server."))
            }
        }
    }

    fun resetResult() {
        _state.value = _state.value.copy(result = PasswordChangeResult.Idle)
    }
}
