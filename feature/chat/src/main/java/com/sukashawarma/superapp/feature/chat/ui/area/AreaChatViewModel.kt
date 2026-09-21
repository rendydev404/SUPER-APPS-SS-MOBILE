package com.sukashawarma.superapp.feature.chat.ui.area

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.data.AnggotaArea
import com.sukashawarma.superapp.feature.chat.data.AreaChatRepository
import com.sukashawarma.superapp.feature.chat.data.AreaInfo
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.PesanAreaChat
import com.sukashawarma.superapp.feature.chat.data.ReaksiPesan
import com.sukashawarma.superapp.feature.chat.data.Sebutan
import com.sukashawarma.superapp.feature.chat.domain.AreaResolver
import com.sukashawarma.superapp.feature.chat.domain.PelacakPengetik
import com.sukashawarma.superapp.feature.chat.domain.Pengetik
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AreaChatState(
    val areaAktif: AreaInfo = AreaResolver.AREA_DEFAULT,
    val daftarArea: List<AreaInfo> = AreaResolver.DAFTAR_AREA,
    val bolehGantiArea: Boolean = false,
    val memuat: Boolean = true,
    val pesan: List<PesanAreaChat> = emptyList(),
    val anggota: List<AnggotaArea> = emptyList(),
    val memuatAnggota: Boolean = false,
    val reaksi: Map<String, List<ReaksiPesan>> = emptyMap(),
    val pengetik: List<Pengetik> = emptyList(),
    val inputTeks: String = "",
    val balasPesan: PesanAreaChat? = null,
    val sedangMengirim: Boolean = false,
    val sebutanAktif: List<AnggotaArea> = emptyList(),
)

class AreaChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(AreaChatState())
    val state: StateFlow<AreaChatState> = _state.asStateFlow()

    private val pelacakPengetik = PelacakPengetik()
    private var jobTypingLoop: Job? = null
    private var jobBroadcastTyping: Job? = null

    init {
        val staff = AppSession.staff.value
        val bolehGanti = AreaResolver.bolehGantiArea(staff?.role, staff?.roleRaw)
        val areaAwal = AreaResolver.tentukanAreaPengguna(
            role = staff?.role,
            roleRaw = staff?.roleRaw,
            namaPengguna = staff?.name,
            outletNama = staff?.outletName
        )

        _state.update {
            it.copy(
                areaAktif = areaAwal,
                bolehGantiArea = bolehGanti
            )
        }

        muatPesan()
        muatAnggota()
        mulaiLanggananRealtime()
    }

    private fun mulaiLanggananRealtime() {
        // 1. Dengarkan pembaruan database realtime
        viewModelScope.launch {
            Realtime.updates(AreaChatRepository.TABLE).collect {
                muatPesan()
            }
        }

        // 2. Dengarkan broadcast pengetik
        viewModelScope.launch {
            val topic = "area-typing-${_state.value.areaAktif.areaId}"
            Realtime.broadcasts(topic).collect { payload ->
                val id = payload.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return@collect
                val nama = payload.get("nama")?.takeIf { !it.isJsonNull }?.asString ?: return@collect
                val myId = AppSession.staff.value?.id.orEmpty()
                if (id != myId) {
                    pelacakPengetik.catat(id, nama, null, System.currentTimeMillis())
                    _state.update { it.copy(pengetik = pelacakPengetik.aktif(System.currentTimeMillis())) }
                }
            }
        }

        // 3. Loop pembersih status pengetik
        jobTypingLoop = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(pengetik = pelacakPengetik.aktif(System.currentTimeMillis())) }
            }
        }
    }

    fun gantiArea(area: AreaInfo) {
        if (_state.value.areaAktif.areaId == area.areaId) return
        _state.update {
            it.copy(
                areaAktif = area,
                memuat = true,
                pesan = emptyList(),
                balasPesan = null,
                inputTeks = "",
                pengetik = emptyList()
            )
        }
        muatPesan()
        muatAnggota()
    }

    fun setInputTeks(teks: String) {
        _state.update { it.copy(inputTeks = teks) }
        evaluasiSebutan(teks)
        kirimSinyalTyping()
    }

    private fun evaluasiSebutan(teks: String) {
        val cursorIdx = teks.length
        val prefix = teks.take(cursorIdx)
        val lastAt = prefix.lastIndexOf('@')
        if (lastAt >= 0 && (lastAt == 0 || prefix[lastAt - 1].isWhitespace())) {
            val query = prefix.substring(lastAt + 1).lowercase()
            val cocok = _state.value.anggota.filter {
                it.namaTampil.lowercase().contains(query) || it.nama.lowercase().contains(query)
            }.take(5)
            _state.update { it.copy(sebutanAktif = cocok) }
        } else {
            _state.update { it.copy(sebutanAktif = emptyList()) }
        }
    }

    fun sisipkanSebutan(anggota: AnggotaArea) {
        val teks = _state.value.inputTeks
        val lastAt = teks.lastIndexOf('@')
        if (lastAt >= 0) {
            val baru = teks.substring(0, lastAt) + "@" + anggota.namaTampil + " "
            _state.update { it.copy(inputTeks = baru, sebutanAktif = emptyList()) }
        }
    }

    private fun kirimSinyalTyping() {
        val sekarang = System.currentTimeMillis()
        if (!pelacakPengetik.bolehKirim(sekarang)) return

        val staff = AppSession.staff.value ?: return
        val payload = JsonObject().apply {
            addProperty("id", staff.id)
            addProperty("nama", staff.namaTampil)
        }
        val topic = "area-typing-${_state.value.areaAktif.areaId}"
        viewModelScope.launch {
            runCatching { Realtime.sendBroadcast(topic, "typing", payload) }
        }
    }

    fun setBalas(pesan: PesanAreaChat?) {
        _state.update { it.copy(balasPesan = pesan) }
    }

    fun muatPesan() {
        viewModelScope.launch {
            _state.update { it.copy(memuat = true) }
            val areaId = _state.value.areaAktif.areaId
            val list = AreaChatRepository.ambilPesan(areaId)
            val reaksiList = AreaChatRepository.ambilReaksi()
            val mapReaksi = reaksiList.groupBy { it.messageId }

            _state.update {
                it.copy(
                    memuat = false,
                    pesan = list,
                    reaksi = mapReaksi
                )
            }
        }
    }

    fun muatAnggota() {
        viewModelScope.launch {
            _state.update { it.copy(memuatAnggota = true) }
            val semuaAnggota = ChatRepository.ambilAnggota()
            val anggotaArea = AreaResolver.saringAnggotaArea(semuaAnggota, _state.value.areaAktif)
            _state.update {
                it.copy(
                    memuatAnggota = false,
                    anggota = anggotaArea
                )
            }
        }
    }

    fun kirimPesan(
        teks: String = _state.value.inputTeks,
        imagePath: String? = null,
        audioPath: String? = null,
        audioMs: Int? = null,
        audioWave: String? = null,
    ) {
        if (teks.isBlank() && imagePath == null && audioPath == null) return

        val areaId = _state.value.areaAktif.areaId
        val balas = _state.value.balasPesan
        val balasId = balas?.id
        val balasName = balas?.senderName
        val balasSnippet = balas?.let {
            when {
                it.body.isNotBlank() -> it.body.take(140)
                it.imagePath != null -> "📷 Foto"
                it.audioPath != null -> "🎤 Pesan suara"
                else -> ""
            }
        }
        val balasImage = balas?.imagePath

        // Ekstrak sebutan @
        val mentions = mutableListOf<Sebutan>()
        _state.value.anggota.forEach { ang ->
            if (teks.contains("@" + ang.namaTampil) || teks.contains("@" + ang.nama)) {
                mentions.add(Sebutan(ang.id, ang.namaTampil))
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(sedangMengirim = true) }
            try {
                val hasil = AreaChatRepository.kirim(
                    areaId = areaId,
                    body = teks,
                    imagePath = imagePath,
                    replyToId = balasId,
                    replyToName = balasName,
                    replyToSnippet = balasSnippet,
                    replyToImage = balasImage,
                    mentions = mentions,
                    audioPath = audioPath,
                    audioMs = audioMs,
                    audioWave = audioWave
                )
                _state.update {
                    it.copy(
                        inputTeks = "",
                        balasPesan = null,
                        sebutanAktif = emptyList(),
                        sedangMengirim = false,
                        pesan = it.pesan + hasil
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(sedangMengirim = false) }
            }
        }
    }

    fun toggleReaksi(messageId: String, emoji: String) {
        val staff = AppSession.staff.value ?: return
        val myId = staff.id
        val myName = staff.namaTampil
        val list = _state.value.reaksi[messageId].orEmpty()
        val sudahPunya = list.any { it.userId == myId && it.emoji == emoji }

        viewModelScope.launch {
            if (sudahPunya) {
                AreaChatRepository.setReaksi(messageId, null, myId, myName)
            } else {
                AreaChatRepository.setReaksi(messageId, emoji, myId, myName)
            }
            val reaksiBaru = AreaChatRepository.ambilReaksi()
            _state.update { it.copy(reaksi = reaksiBaru.groupBy { r -> r.messageId }) }
        }
    }

    fun hapusPesan(messageId: String) {
        val staff = AppSession.staff.value ?: return
        val isDev = staff.role == com.sukashawarma.superapp.domain.model.Role.DEVELOPER ||
            staff.roleRaw.equals("developer", ignoreCase = true)
        val areaId = _state.value.areaAktif.areaId

        viewModelScope.launch {
            AreaChatRepository.hapusPesan(areaId, messageId, staff.id, isDev)
            muatPesan()
        }
    }
}
