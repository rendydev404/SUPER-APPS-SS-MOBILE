package com.sukashawarma.superapp.feature.chat.ui.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.feature.chat.data.PesanPribadi
import com.sukashawarma.superapp.feature.chat.data.PrivateChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DeveloperChatDetailState(
    val userAId: String = "",
    val userBId: String = "",
    val userAName: String = "",
    val userBName: String = "",
    val memuat: Boolean = true,
    val pesanList: List<PesanPribadi> = emptyList(),
)

class DeveloperChatDetailViewModel(
    val userAId: String,
    val userBId: String,
    val userAName: String,
    val userBName: String,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DeveloperChatDetailState(
            userAId = userAId,
            userBId = userBId,
            userAName = userAName,
            userBName = userBName
        )
    )
    val state: StateFlow<DeveloperChatDetailState> = _state

    init {
        muatPesanSiluman()

        // Realtime update untuk mode pengawasan
        viewModelScope.launch {
            Realtime.updates("private_chat_messages").collect {
                muatPesanSiluman()
            }
        }
    }

    /**
     * Memuat pesan secara SILUMAN murni (read-only).
     * TIDAK memanggil tandaiDibaca atau tandaiTersampaikan sama sekali.
     */
    fun muatPesanSiluman() {
        viewModelScope.launch {
            try {
                val list = PrivateChatRepository.developerAmbilPesanSiluman(userAId, userBId)
                _state.value = _state.value.copy(memuat = false, pesanList = list)
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false)
            }
        }
    }
}
