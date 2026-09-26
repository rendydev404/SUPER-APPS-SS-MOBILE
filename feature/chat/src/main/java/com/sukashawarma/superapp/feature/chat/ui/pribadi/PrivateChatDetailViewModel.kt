package com.sukashawarma.superapp.feature.chat.ui.pribadi

import com.sukashawarma.superapp.feature.chat.data.KlipyStiker
import com.sukashawarma.superapp.feature.chat.data.StikerKlipy
import com.sukashawarma.superapp.feature.chat.data.TEKS_CADANGAN_STIKER
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.data.PesanPribadi
import com.sukashawarma.superapp.feature.chat.data.PrivateChatRepository
import com.sukashawarma.superapp.feature.chat.data.ReaksiPesan
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PrivateChatDetailState(
    val partnerId: String = "",
    val memuat: Boolean = true,
    val pesanList: List<PesanPribadi> = emptyList(),
    val reaksi: Map<String, List<ReaksiPesan>> = emptyMap(),
    val balasTarget: PesanPribadi? = null,
    val mengirim: Boolean = false,
)

class PrivateChatDetailViewModel(
    val partnerId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(PrivateChatDetailState(partnerId = partnerId))
    val state: StateFlow<PrivateChatDetailState> = _state

    private val _pesanGalat = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val pesanGalat: SharedFlow<String> = _pesanGalat

    private val myId: String get() = AppSession.staff.value?.id.orEmpty()

    init {
        muatPesanDanTandai()

        // Realtime sync untuk pesan & reaksi obrolan ini
        viewModelScope.launch {
            Realtime.updates("private_chat_messages").collect {
                muatPesanDanTandai()
            }
        }
        viewModelScope.launch {
            Realtime.updates("private_chat_reactions").collect {
                muatReaksi()
            }
        }
    }

    fun muatPesanDanTandai() {
        viewModelScope.launch {
            try {
                val list = PrivateChatRepository.ambilPesan(partnerId)
                val listReaksi = PrivateChatRepository.ambilReaksi(partnerId)
                _state.value = _state.value.copy(
                    memuat = false,
                    pesanList = list,
                    reaksi = listReaksi.groupBy { it.messageId },
                )

                // Tandai pesan dari lawan bicara sebagai telah tersampaikan & dibaca
                PrivateChatRepository.tandaiTersampaikan(partnerId)
                PrivateChatRepository.tandaiDibaca(partnerId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false)
            }
        }
    }

    fun muatReaksi() {
        viewModelScope.launch {
            try {
                val listReaksi = PrivateChatRepository.ambilReaksi(partnerId)
                _state.value = _state.value.copy(reaksi = listReaksi.groupBy { it.messageId })
            } catch (_: Exception) {}
        }
    }

    private val sudahDitandaiDengar = mutableSetOf<String>()

    /**
     * Tandai pesan suara dari lawan bicara sebagai sudah didengar.
     *
     * Ditempel optimis ke daftar supaya tombol putarnya langsung berubah warna;
     * RPC-nya sendiri menolak penanda dari pengirim, jadi tombol di bubble
     * sendiri tidak akan pernah menyala karena mendengar rekamannya sendiri.
     */
    fun tandaiSuaraDiputar(pesan: PesanPribadi) {
        if (pesan.senderId == myId || pesan.audioPath == null) return
        if (pesan.audioPlayedAtMs != null) return
        if (!sudahDitandaiDengar.add(pesan.id)) return

        _state.value = _state.value.copy(
            pesanList = _state.value.pesanList.map {
                if (it.id == pesan.id) it.copy(audioPlayedAtMs = System.currentTimeMillis()) else it
            }
        )

        viewModelScope.launch {
            try {
                PrivateChatRepository.tandaiSuaraDiputar(pesan.id)
            } catch (e: Exception) {
                sudahDitandaiDengar.remove(pesan.id)
                android.util.Log.w("PrivateChatDetailVM", "tandai suara diputar gagal: ${e.message}")
            }
        }
    }

    fun setBalas(pesan: PesanPribadi?) {
        _state.value = _state.value.copy(balasTarget = pesan)
    }

    fun batalBalas() {
        _state.value = _state.value.copy(balasTarget = null)
    }

    fun toggleReaksi(pesan: PesanPribadi, emoji: String) {
        val id = myId
        if (id.isBlank()) return
        val sekarang = _state.value.reaksi[pesan.id].orEmpty()
        val milikku = sekarang.firstOrNull { it.userId == id }
        val target = if (milikku?.emoji == emoji) null else emoji
        val namaSendiri = AppSession.staff.value?.let { it.displayName ?: it.name }.orEmpty()

        val baru = sekarang.filterNot { it.userId == id } +
            listOfNotNull(target?.let { ReaksiPesan(pesan.id, id, namaSendiri, it) })
        _state.value = _state.value.copy(
            reaksi = _state.value.reaksi + (pesan.id to baru),
        )

        viewModelScope.launch {
            try {
                PrivateChatRepository.setReaksi(pesan.id, target)
            } catch (e: Exception) {
                android.util.Log.e("PrivateChatDetailVM", "gagal update reaksi", e)
                muatReaksi()
            }
        }
    }

    fun hapusPesan(pesan: PesanPribadi) {
        val list = _state.value.pesanList.map {
            if (it.id == pesan.id) it.copy(deletedAtMs = System.currentTimeMillis(), body = "", imagePath = null, audioPath = null)
            else it
        }
        _state.value = _state.value.copy(pesanList = list)

        viewModelScope.launch {
            try {
                PrivateChatRepository.hapusPesan(pesan.id)
                muatPesanDanTandai()
            } catch (e: Exception) {
                android.util.Log.e("PrivateChatDetailVM", "gagal hapus pesan", e)
                _pesanGalat.tryEmit(e.message ?: "Gagal menghapus pesan.")
                muatPesanDanTandai()
            }
        }
    }

    /** Kirim voice note; berkas cache dihapus setelah isinya dibaca. */
    fun kirimSuara(berkas: java.io.File, durasiMs: Int, wave: String) {
        val isi = runCatching { berkas.readBytes() }.getOrNull()
        berkas.delete()
        if (isi == null || isi.isEmpty()) {
            _pesanGalat.tryEmit("Rekaman suara gagal disimpan.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true)
            try {
                val audioPath = PrivateChatRepository.unggahSuara(myId, isi)
                PrivateChatRepository.kirimPesan(
                    recipientId = partnerId,
                    body = "",
                    replyToId = _state.value.balasTarget?.id,
                    audioPath = audioPath,
                    audioMs = durasiMs,
                    audioWave = wave,
                )
                _state.value = _state.value.copy(mengirim = false, balasTarget = null)
                muatPesanDanTandai()
            } catch (e: Exception) {
                _state.value = _state.value.copy(mengirim = false)
                _pesanGalat.tryEmit(e.message ?: "Gagal mengirim pesan suara.")
            }
        }
    }

    fun kirimStiker(stiker: StikerKlipy) {
        // URL di luar klipy.com ditolak CHECK database; saring sebelum memanggil server.
        if (!KlipyStiker.urlSah(stiker.urlKirim)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true)
            try {
                PrivateChatRepository.kirimPesan(
                    recipientId = partnerId,
                    body = TEKS_CADANGAN_STIKER,
                    replyToId = _state.value.balasTarget?.id,
                    stickerUrl = stiker.urlKirim,
                )
                _state.value = _state.value.copy(mengirim = false, balasTarget = null)
                muatPesanDanTandai()
            } catch (e: Exception) {
                _state.value = _state.value.copy(mengirim = false)
                _pesanGalat.tryEmit(e.message ?: "Gagal mengirim stiker.")
            }
        }
    }

    fun kirim(body: String, fotoWebp: ByteArray? = null) {
        val teks = body.trim()
        if (teks.isBlank() && fotoWebp == null) return

        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true)
            try {
                var imagePath: String? = null
                if (fotoWebp != null) {
                    imagePath = PrivateChatRepository.unggahFoto(myId, fotoWebp)
                }

                val replyId = _state.value.balasTarget?.id
                PrivateChatRepository.kirimPesan(
                    recipientId = partnerId,
                    body = teks,
                    imagePath = imagePath,
                    replyToId = replyId
                )

                _state.value = _state.value.copy(
                    mengirim = false,
                    balasTarget = null
                )
                muatPesanDanTandai()
            } catch (e: Exception) {
                _state.value = _state.value.copy(mengirim = false)
                _pesanGalat.tryEmit(e.message ?: "Gagal mengirim pesan.")
            }
        }
    }
}
