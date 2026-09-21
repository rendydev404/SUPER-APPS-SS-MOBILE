package com.sukashawarma.superapp.feature.chat.ui.pribadi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.PercakapanPengawasanItem
import com.sukashawarma.superapp.feature.chat.data.PercakapanPribadiItem
import com.sukashawarma.superapp.feature.chat.data.PesanPribadi
import com.sukashawarma.superapp.feature.chat.data.PrivateChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class TabChatUtama {
    GRUP,
    AREA,
    PRIBADI,
    PANTAU_DEV
}

data class PrivateChatListState(
    val memuat: Boolean = true,
    val memuatPengawasan: Boolean = false,
    val percakapan: List<PercakapanPribadiItem> = emptyList(),
    val pengawasan: List<PercakapanPengawasanItem> = emptyList(),
    val kontakStaf: List<AnggotaGrup> = emptyList(),
    val memuatKontak: Boolean = false,
    val isDeveloper: Boolean = false,
    val tabAktif: TabChatUtama = TabChatUtama.GRUP,
    val totalUnreadPribadi: Int = 0,
)

class PrivateChatListViewModel : ViewModel() {

    private val _state = MutableStateFlow(PrivateChatListState())
    val state: StateFlow<PrivateChatListState> = _state

    init {
        val dev = AppSession.staff.value?.role == Role.DEVELOPER ||
            AppSession.staff.value?.roleRaw?.equals("developer", ignoreCase = true) == true
        _state.value = _state.value.copy(isDeveloper = dev)
        muatPercakapan()

        // Dengarkan pembaruan realtime pada tabel private_chat_messages
        viewModelScope.launch {
            // Akui seluruh pesan yang masuk saat aplikasi aktif
            runCatching { PrivateChatRepository.tandaiSemuaTersampaikan() }

            Realtime.updates("private_chat_messages").collect {
                runCatching { PrivateChatRepository.tandaiSemuaTersampaikan() }
                muatPercakapan()
                if (_state.value.isDeveloper && _state.value.tabAktif == TabChatUtama.PANTAU_DEV) {
                    muatPengawasan()
                }
            }
        }
    }

    fun setTab(tab: TabChatUtama) {
        _state.value = _state.value.copy(tabAktif = tab)
        if (tab == TabChatUtama.PANTAU_DEV) {
            muatPengawasan()
        } else if (tab == TabChatUtama.PRIBADI) {
            muatPercakapan()
        }
    }

    fun muatPercakapan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true)
            try {
                val list = PrivateChatRepository.ambilDaftarPercakapan()
                val totalUnread = list.sumOf { it.unreadCount }
                _state.value = _state.value.copy(
                    memuat = false,
                    percakapan = list,
                    totalUnreadPribadi = totalUnread
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false)
            }
        }
    }

    fun muatPengawasan() {
        if (!_state.value.isDeveloper) return
        viewModelScope.launch {
            _state.value = _state.value.copy(memuatPengawasan = true)
            try {
                val list = PrivateChatRepository.developerAmbilSemuaPercakapan()
                _state.value = _state.value.copy(memuatPengawasan = false, pengawasan = list)
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuatPengawasan = false)
            }
        }
    }

    fun muatKontakStaf() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuatKontak = true)
            try {
                val myId = AppSession.staff.value?.id.orEmpty()
                val kontak = ChatRepository.ambilAnggota().filter { it.id != myId }
                _state.value = _state.value.copy(memuatKontak = false, kontakStaf = kontak)
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuatKontak = false)
            }
        }
    }
}
