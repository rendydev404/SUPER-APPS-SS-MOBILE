package com.sukashawarma.superapp.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.session.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val identifier: String = AuthPrefs.getLoginIdentifier().orEmpty(),
    val password: String = AuthPrefs.getLoginPassword().orEmpty(),
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onIdentifierChange(v: String) { _state.value = _state.value.copy(identifier = v, error = null) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v, error = null) }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.identifier.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Username dan password wajib diisi.")
            return
        }
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = AppSession.login(s.identifier.trim(), s.password)) {
                is LoginResult.Success -> {
                    _state.value = _state.value.copy(loading = false)
                    onSuccess()
                }
                is LoginResult.Failure -> {
                    _state.value = _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
