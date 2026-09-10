package com.sukashawarma.superapp.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.PesanChat
import com.sukashawarma.superapp.feature.chat.domain.PelacakPengetik
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/** Kiriman yang belum diakui server — bubble jam pasir / gagal di ujung daftar. */
data class KirimanTertunda(
    val kunci: String,
    val body: String,
    val fotoWebp: ByteArray?,
    val replyTo: PesanChat?,
    val dibuatMs: Long,
    val gagal: Boolean = false,
    /** Alasan gagal, sudah dipendekkan untuk ditampilkan di bawah bubble.
     *  Tanpa ini kegagalan unggah hanya terlihat di Logcat. */
    val alasanGagal: String? = null,
)

data class ChatState(
    val memuat: Boolean = true,
    val galat: String? = null,
    val pesan: List<PesanChat> = emptyList(),
    val tertunda: List<KirimanTertunda> = emptyList(),
    val namaPengetik: List<String> = emptyList(),
    val balasTarget: PesanChat? = null,
)

class ChatViewModel : ViewModel() {

    private companion object {
        const val KANAL_TYPING = "chat-typing"
    }

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    private val pelacak = PelacakPengetik()

    private val userId: String get() = AppSession.staff.value?.id.orEmpty()
    private val namaSendiri: String get() = AppSession.staff.value?.namaTampil.orEmpty()

    /** Waktu pesan termuda yang sudah dilihat — dipakai mendeteksi pesan BARU
     *  saat muat ulang, untuk menurunkan indikator typing pengirimnya. */
    private var maksCreatedMs: Long = 0

    init {
        muatUlang(awal = true)

        // Sinyal "tabel berubah" -> muat ulang lewat jalur baca normal. Emisi
        // pertama saat sambungan terbuka sekaligus mengejar yang terlewat
        // selama offline.
        viewModelScope.launch {
            Realtime.updates(ChatRepository.TABLE).collect { muatUlang() }
        }

        // Sinyal typing dari klien lain (broadcast murni, tanpa database).
        viewModelScope.launch {
            Realtime.broadcasts(KANAL_TYPING).collect { payload ->
                val isi = payload.takeIf { it.get("event")?.asString == "typing" }
                    ?.getAsJsonObject("payload") ?: return@collect
                val id = isi.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return@collect
                val nama = isi.get("nama")?.takeIf { !it.isJsonNull }?.asString ?: return@collect
                if (id == userId) return@collect
                pelacak.catat(id, nama, System.currentTimeMillis())
                segarkanPengetik()
            }
        }

        // Nama pengetik kedaluwarsa sendiri setelah 5 detik hening; detak ini
        // yang menurunkannya dari layar tanpa perlu sinyal "berhenti".
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                segarkanPengetik()
            }
        }
    }

    private fun segarkanPengetik() {
        val nama = pelacak.namaAktif(System.currentTimeMillis())
        if (nama != _state.value.namaPengetik) _state.value = _state.value.copy(namaPengetik = nama)
    }

    /** Dipanggil setiap isi kotak ketik berubah; memancarkan sinyal typing
     *  maksimal sekali per 3 detik (lihat [PelacakPengetik.bolehKirim]). */
    fun ketikan(teks: String) {
        if (teks.isBlank()) return
        if (!pelacak.bolehKirim(System.currentTimeMillis())) return
        val payload = JsonObject().apply {
            addProperty("id", userId)
            addProperty("nama", namaSendiri)
        }
        Realtime.sendBroadcast(KANAL_TYPING, "typing", payload)
    }

    fun setBalas(pesan: PesanChat?) {
        _state.value = _state.value.copy(balasTarget = pesan)
    }

    fun kirimTeks(teks: String) {
        val bersih = teks.trim()
        if (bersih.isEmpty()) return
        antre(KirimanTertunda(
            kunci = UUID.randomUUID().toString(),
            body = bersih,
            fotoWebp = null,
            replyTo = _state.value.balasTarget,
            dibuatMs = System.currentTimeMillis(),
        ))
    }

    fun kirimFoto(webp: ByteArray, keterangan: String) {
        antre(KirimanTertunda(
            kunci = UUID.randomUUID().toString(),
            body = keterangan.trim(),
            fotoWebp = webp,
            replyTo = _state.value.balasTarget,
            dibuatMs = System.currentTimeMillis(),
        ))
    }

    private fun antre(kiriman: KirimanTertunda) {
        _state.value = _state.value.copy(
            tertunda = _state.value.tertunda + kiriman,
            balasTarget = null,
        )
        proses(kiriman)
    }

    fun ulangi(kunci: String) {
        val kiriman = _state.value.tertunda.firstOrNull { it.kunci == kunci } ?: return
        _state.value = _state.value.copy(
            tertunda = _state.value.tertunda.map {
                if (it.kunci == kunci) it.copy(gagal = false, alasanGagal = null) else it
            },
        )
        proses(kiriman)
    }

    fun batalkanKiriman(kunci: String) {
        _state.value = _state.value.copy(tertunda = _state.value.tertunda.filterNot { it.kunci == kunci })
    }

    private fun proses(kiriman: KirimanTertunda) {
        viewModelScope.launch {
            try {
                val path = kiriman.fotoWebp?.let { ChatRepository.unggahFoto(userId, it) }
                val tersimpan = ChatRepository.kirim(kiriman.body, path, kiriman.replyTo?.id)
                _state.value = _state.value.copy(
                    tertunda = _state.value.tertunda.filterNot { it.kunci == kiriman.kunci },
                    // Langsung ditempel supaya bubble tidak "hilang sekejap" menunggu
                    // sinyal realtime; muat ulang berikutnya de-dupe lewat id.
                    pesan = (_state.value.pesan.filterNot { it.id == tersimpan.id } + tersimpan),
                )
                maksCreatedMs = maxOf(maksCreatedMs, tersimpan.createdAtMs)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "kirim gagal", e)
                val alasan = (e.message ?: e::class.java.simpleName).take(160)
                _state.value = _state.value.copy(
                    tertunda = _state.value.tertunda.map {
                        if (it.kunci == kiriman.kunci) it.copy(gagal = true, alasanGagal = alasan) else it
                    },
                )
            }
        }
    }

    /** Hapus pesan sendiri — optimis; kalau server menolak, daftar dimuat ulang. */
    fun hapus(pesan: PesanChat) {
        _state.value = _state.value.copy(pesan = _state.value.pesan.filterNot { it.id == pesan.id })
        viewModelScope.launch {
            try {
                ChatRepository.hapus(pesan.id)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "hapus gagal", e)
                muatUlang()
            }
        }
    }

    fun muatUlang(awal: Boolean = false) {
        if (awal) _state.value = _state.value.copy(memuat = true)
        viewModelScope.launch {
            try {
                val pesan = ChatRepository.ambilPesan()
                // Orang yang pesannya baru tiba jelas sudah selesai mengetik.
                pesan.filter { it.createdAtMs > maksCreatedMs }.forEach { pelacak.selesai(it.senderId) }
                maksCreatedMs = pesan.maxOfOrNull { it.createdAtMs } ?: maksCreatedMs
                _state.value = _state.value.copy(memuat = false, galat = null, pesan = pesan)
                segarkanPengetik()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "muatUlang gagal", e)
                // Galat hanya ditampilkan bila layar belum punya apa-apa; kalau
                // sudah ada isi, kegagalan refresh cukup diam (realtime akan
                // mencoba lagi sendiri).
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = if (_state.value.pesan.isEmpty()) "Gagal memuat chat. Periksa koneksi." else _state.value.galat,
                )
            }
        }
    }
}
