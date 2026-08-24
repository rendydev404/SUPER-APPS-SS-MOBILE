package com.sukashawarma.superapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TodayAttendance(val type: String, val tsServerIso: String, val status: String)

data class HomeUiState(
    val staff: StaffProfile? = null,
    val greeting: String = "",
    val dateLabel: String = "",
    val todayAttendance: TodayAttendance? = null,
    val loadingAttendance: Boolean = true,
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        val staff = AppSession.staff.value
        val now = JakartaTime.now()
        val greeting = when {
            now.hour < 11 -> "Selamat pagi"
            now.hour < 15 -> "Selamat siang"
            now.hour < 18 -> "Selamat sore"
            else -> "Selamat malam"
        }
        val dateLabel = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")))
        _state.value = HomeUiState(staff = staff, greeting = greeting, dateLabel = dateLabel)
        loadTodayAttendance(staff?.id)
    }

    private fun loadTodayAttendance(staffId: String?) {
        if (staffId == null) {
            _state.value = _state.value.copy(loadingAttendance = false)
            return
        }
        viewModelScope.launch {
            try {
                val rows = Postgrest.select(
                    "attendance",
                    listOf(
                        "outlet_staff_id" to "eq.$staffId",
                        "ts_server" to "gte.${JakartaTime.todayStartIso()}",
                        "select" to "type,ts_server,status",
                        "order" to "ts_server.desc",
                        "limit" to "1"
                    )
                )
                val latest = if (rows.size() > 0) rows[0].asJsonObject else null
                val att = latest?.let {
                    TodayAttendance(
                        type = it.optString("type") ?: "",
                        tsServerIso = it.optString("ts_server") ?: "",
                        status = it.optString("status") ?: ""
                    )
                }
                _state.value = _state.value.copy(todayAttendance = att, loadingAttendance = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingAttendance = false)
            }
        }
    }

    fun logout() = AppSession.signOut()
}
