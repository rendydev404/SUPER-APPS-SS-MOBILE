package com.sukashawarma.superapp.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.PengaturanGrup
import com.sukashawarma.superapp.feature.chat.data.PesanChat
import com.sukashawarma.superapp.feature.chat.data.ReaksiPesan
import com.sukashawarma.superapp.feature.chat.domain.PelacakPengetik
import com.sukashawarma.superapp.feature.chat.domain.Pengetik
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
    val pengetik: List<Pengetik> = emptyList(),
    val balasTarget: PesanChat? = null,
    /** Reaksi dikelompokkan per id pesan supaya bubble tinggal melihat miliknya. */
    val reaksi: Map<String, List<ReaksiPesan>> = emptyMap(),
    val pengaturan: PengaturanGrup = PengaturanGrup(),
    /** true bila akun ini boleh mengubah pengaturan grup (developer/admin/HR). */
    val pengelola: Boolean = false,
    /** Anggota grup. Kosong sampai ada yang benar-benar membutuhkannya. */
    val anggota: List<AnggotaGrup> = emptyList(),
    val memuatAnggota: Boolean = false,
) {
    /** Mode pengumuman menutup kotak ketik untuk yang bukan pengelola. */
    val bolehKirim: Boolean get() = !pengaturan.hanyaAdmin || pengelola
}

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
        // TIGA aliran terpisah, bukan satu gabungan. Sebelumnya satu pesan masuk
        // ikut menarik ulang seluruh daftar reaksi DAN pengaturan grup — tiga
        // permintaan jaringan untuk satu kejadian. Sekarang tiap tabel hanya
        // memuat ulang bagiannya sendiri.
        viewModelScope.launch {
            Realtime.updates(ChatRepository.TABLE).collect { muatPesan() }
        }
        viewModelScope.launch {
            Realtime.updates(ChatRepository.TABLE_REAKSI).collect { muatReaksi() }
        }
        viewModelScope.launch {
            Realtime.updates(ChatRepository.TABLE_PENGATURAN).collect { muatPengaturan() }
        }

        // Sinyal typing dari klien lain (broadcast murni, tanpa database).
        viewModelScope.launch {
            Realtime.broadcasts(KANAL_TYPING).collect { payload ->
                val isi = payload.takeIf { it.get("event")?.asString == "typing" }
                    ?.getAsJsonObject("payload") ?: return@collect
                val id = isi.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return@collect
                val nama = isi.get("nama")?.takeIf { !it.isJsonNull }?.asString ?: return@collect
                val avatar = isi.get("avatar")?.takeIf { !it.isJsonNull }?.asString
                if (id == userId) return@collect
                pelacak.catat(id, nama, avatar, System.currentTimeMillis())
                segarkanPengetik()
            }
        }

        // Nama pengetik kedaluwarsa sendiri setelah 5 detik hening; detak ini
        // yang menurunkannya dari layar tanpa perlu sinyal "berhenti".
        //
        // Detaknya BERHENTI saat tidak ada yang mengetik. Versi sebelumnya
        // bangun tiap detik sepanjang layar hidup walau tidak ada apa pun yang
        // berubah — pekerjaan sia-sia yang paling terasa di HP lemah dan di
        // baterai.
        viewModelScope.launch {
            while (isActive) {
                // Tidur TANPA BATAS sampai ada yang mengetik: `first` menunggu
                // perubahan state, bukan berdetak. Nol pekerjaan saat grup sepi,
                // dan grup memang sepi hampir sepanjang waktu.
                _state.first { it.pengetik.isNotEmpty() }
                while (isActive && _state.value.pengetik.isNotEmpty()) {
                    delay(1_000)
                    segarkanPengetik()
                }
            }
        }
    }

    private fun segarkanPengetik() {
        val orang = pelacak.aktif(System.currentTimeMillis())
        if (orang != _state.value.pengetik) _state.value = _state.value.copy(pengetik = orang)
    }

    /** Dipanggil setiap isi kotak ketik berubah; memancarkan sinyal typing
     *  maksimal sekali per 3 detik (lihat [PelacakPengetik.bolehKirim]). */
    fun ketikan(teks: String) {
        if (teks.isBlank()) return
        if (!pelacak.bolehKirim(System.currentTimeMillis())) return
        val payload = JsonObject().apply {
            addProperty("id", userId)
            addProperty("nama", namaSendiri)
            // Wajah ikut dikirim supaya penerima bisa menumpuknya tanpa perlu
            // membaca tabel staff — yang RLS-nya memang tidak membukanya.
            AppSession.staff.value?.avatarUrl?.let { addProperty("avatar", it) }
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

    /**
     * Pasang/ganti/cabut reaksi. Dioptimiskan di layar lebih dulu supaya emoji
     * langsung menempel saat ditekan; sinyal realtime yang menyusul akan
     * menyamakan dengan kebenaran server.
     */
    fun toggleReaksi(pesan: PesanChat, emoji: String) {
        val id = userId
        if (id.isBlank()) return
        val sekarang = _state.value.reaksi[pesan.id].orEmpty()
        val milikku = sekarang.firstOrNull { it.userId == id }
        val target = if (milikku?.emoji == emoji) null else emoji

        val baru = sekarang.filterNot { it.userId == id } +
            listOfNotNull(target?.let { ReaksiPesan(pesan.id, id, namaSendiri, it) })
        _state.value = _state.value.copy(
            reaksi = _state.value.reaksi + (pesan.id to baru),
        )

        viewModelScope.launch {
            try {
                ChatRepository.setReaksi(pesan.id, target, id)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "reaksi gagal", e)
                muatUlang()
            }
        }
    }

    /**
     * [fotoBaru] = JPEG yang baru dipilih; [hapusFoto] = kembalikan ke ikon
     * bawaan. Keduanya null/false berarti foto yang sekarang dibiarkan apa
     * adanya — menyimpan nama grup tidak boleh ikut menghapus fotonya.
     */
    fun simpanPengaturan(
        nama: String,
        deskripsi: String,
        hanyaAdmin: Boolean,
        fotoBaru: ByteArray? = null,
        hapusFoto: Boolean = false,
        onSelesai: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val path = when {
                    fotoBaru != null -> ChatRepository.unggahFotoGrup(userId, fotoBaru)
                    hapusFoto -> ""
                    else -> null
                }
                ChatRepository.simpanPengaturan(nama, deskripsi, hanyaAdmin, namaSendiri, path)
                muatUlang()
                onSelesai(null)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "simpan pengaturan gagal", e)
                onSelesai(e.message ?: "Gagal menyimpan pengaturan.")
            }
        }
    }

    /**
     * Daftar anggota diambil SESUAI PERMINTAAN, bukan saat layar dibuka.
     *
     * Isinya jarang berubah dan hanya dipakai dua tempat (info grup, kartu
     * profil), sementara jumlahnya se-perusahaan. Menariknya di awal berarti
     * satu permintaan tambahan untuk setiap orang yang cuma ingin membaca chat.
     */
    fun muatAnggota(paksa: Boolean = false) {
        val sekarang = _state.value
        if (sekarang.memuatAnggota) return
        if (!paksa && sekarang.anggota.isNotEmpty()) return
        _state.value = sekarang.copy(memuatAnggota = true)
        viewModelScope.launch {
            try {
                val anggota = ChatRepository.ambilAnggota()
                _state.value = _state.value.copy(anggota = anggota, memuatAnggota = false)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "daftar anggota tidak tersedia: ${e.message}")
                _state.value = _state.value.copy(memuatAnggota = false)
            }
        }
    }

    /** Muat semuanya — hanya untuk pembukaan layar dan pemulihan galat. */
    fun muatUlang(awal: Boolean = false) {
        if (awal) _state.value = _state.value.copy(memuat = true)
        viewModelScope.launch {
            muatPesanSekarang(tandaiSelesai = true)
            muatReaksiSekarang()
            muatPengaturanSekarang()
        }
    }

    private fun muatPesan() {
        viewModelScope.launch { muatPesanSekarang(tandaiSelesai = true) }
    }

    private fun muatReaksi() {
        viewModelScope.launch { muatReaksiSekarang() }
    }

    private fun muatPengaturan() {
        viewModelScope.launch { muatPengaturanSekarang() }
    }

    private suspend fun muatPesanSekarang(tandaiSelesai: Boolean) {
        try {
            val pesan = ChatRepository.ambilPesan()

            // Orang yang pesannya baru tiba jelas sudah selesai mengetik.
            if (tandaiSelesai) {
                pesan.forEach { if (it.createdAtMs > maksCreatedMs) pelacak.selesai(it.senderId) }
            }
            maksCreatedMs = pesan.maxOfOrNull { it.createdAtMs } ?: maksCreatedMs

            val sekarang = _state.value
            // Isi yang sama tidak ditulis ulang ke state. Tanpa penjaga ini,
            // setiap sinyal realtime — termasuk yang tidak mengubah apa pun —
            // membuat seluruh daftar disusun ulang dan digambar ulang.
            if (sekarang.pesan == pesan && !sekarang.memuat && sekarang.galat == null) {
                segarkanPengetik()
                return
            }
            _state.value = sekarang.copy(memuat = false, galat = null, pesan = pesan)
            segarkanPengetik()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "muat pesan gagal", e)
            // Galat hanya ditampilkan bila layar belum punya apa-apa; kalau
            // sudah ada isi, kegagalan refresh cukup diam (realtime akan
            // mencoba lagi sendiri).
            _state.value = _state.value.copy(
                memuat = false,
                galat = if (_state.value.pesan.isEmpty()) "Gagal memuat chat. Periksa koneksi." else _state.value.galat,
            )
        }
    }

    // Reaksi dan pengaturan punya penjaga sendiri: keduanya datang dari migrasi
    // yang lebih baru, dan chat harus tetap jalan di perangkat yang databasenya
    // belum diperbarui.
    private suspend fun muatReaksiSekarang() {
        try {
            val reaksi = ChatRepository.ambilReaksi().groupBy { it.messageId }
            if (reaksi != _state.value.reaksi) {
                _state.value = _state.value.copy(reaksi = reaksi)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("ChatViewModel", "reaksi tidak tersedia: ${e.message}")
        }
    }

    private suspend fun muatPengaturanSekarang() {
        val pengelola = AppSession.staff.value?.roleRaw in ChatRepository.ROLE_PENGELOLA
        try {
            val pengaturan = ChatRepository.ambilPengaturan()
            if (pengaturan != _state.value.pengaturan || pengelola != _state.value.pengelola) {
                _state.value = _state.value.copy(pengaturan = pengaturan, pengelola = pengelola)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("ChatViewModel", "pengaturan tidak tersedia: ${e.message}")
            if (pengelola != _state.value.pengelola) {
                _state.value = _state.value.copy(pengelola = pengelola)
            }
        }
    }
}
