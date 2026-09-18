package com.sukashawarma.superapp.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.ChatKehadiran
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.BacaanPesan
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.DetailInfoPesan
import com.sukashawarma.superapp.feature.chat.data.PembacaPesan
import com.sukashawarma.superapp.feature.chat.data.PengaturanGrup
import com.sukashawarma.superapp.feature.chat.data.PesanChat
import com.sukashawarma.superapp.feature.chat.data.ReaksiPesan
import com.sukashawarma.superapp.feature.chat.data.Sebutan
import com.sukashawarma.superapp.feature.chat.domain.PelacakPengetik
import com.sukashawarma.superapp.feature.chat.domain.Pengetik
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/** Kiriman yang belum diakui server — bubble jam pasir / gagal di ujung daftar. */
data class KirimanTertunda(
    val kunci: String,
    val body: String,
    val fotoWebp: ByteArray?,
    /** Rekaman suara yang menunggu diunggah. null = bukan pesan suara. */
    val suaraM4a: ByteArray? = null,
    val suaraMs: Int? = null,
    val suaraWave: String? = null,
    val replyTo: PesanChat?,
    val dibuatMs: Long,
    /** Orang yang disebut; ikut bertahan agar kiriman yang diulang tetap
     *  memberi tahu orang yang sama. */
    val mentions: List<Sebutan> = emptyList(),
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
    /** Pesan yang sedang disunting. Non-null = komposer berada dalam mode sunting. */
    val suntingTarget: PesanChat? = null,
    /** Reaksi dikelompokkan per id pesan supaya bubble tinggal melihat miliknya. */
    val reaksi: Map<String, List<ReaksiPesan>> = emptyMap(),
    /** Catatan pembacaan per id pesan, untuk centang biru dan jumlah pembaca. */
    val bacaan: Map<String, List<BacaanPesan>> = emptyMap(),
    /** Pesan yang sedang dibuka detail info pembacaannya (Info Pesan). */
    val infoTarget: PesanChat? = null,
    /** Data detail pembaca dari RPC chat_info_pesan. */
    val detailInfoPesan: DetailInfoPesan? = null,
    /** Siapa saja yang sudah mendengarkan pesan suara yang sedang dibuka infonya. */
    val pendengarSuara: List<PembacaPesan> = emptyList(),
    val memuatInfo: Boolean = false,
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

    private val _pesanGalat = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 4)
    val pesanGalat: kotlinx.coroutines.flow.SharedFlow<String> = _pesanGalat

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
            Realtime.updates(ChatRepository.TABLE_BACAAN).collect {
                muatBacaan()
                val target = _state.value.infoTarget
                if (target != null) {
                    segarkanInfoPesan(target.id, target.audioPath != null)
                }
            }
        }
        viewModelScope.launch {
            Realtime.updates(ChatRepository.TABLE_SUARA_PUTAR).collect {
                val target = _state.value.infoTarget
                if (target != null && target.audioPath != null) {
                    segarkanInfoPesan(target.id, true)
                }
            }
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

    fun setSunting(pesan: PesanChat?) {
        // Menyunting dan membalas tidak bisa berjalan bersamaan: keduanya memakai
        // kotak ketik yang sama, dan menyimpan keduanya hanya melahirkan keadaan
        // yang mustahil dijelaskan ke pengguna.
        _state.value = _state.value.copy(suntingTarget = pesan, balasTarget = null)
    }

    /**
     * Simpan hasil suntingan. Perubahannya ditempel duluan ke daftar supaya
     * terasa seketika; kalau server menolak (mis. lewat 15 menit), daftar dimuat
     * ulang dan alasannya disampaikan lewat [pesanGalat].
     */
    fun simpanSuntingan(teksBaru: String) {
        val target = _state.value.suntingTarget ?: return
        val bersih = teksBaru.trim()
        if (bersih == target.body) {
            _state.value = _state.value.copy(suntingTarget = null)
            return
        }
        _state.value = _state.value.copy(
            suntingTarget = null,
            pesan = _state.value.pesan.map {
                if (it.id == target.id) it.copy(body = bersih, editedAtMs = System.currentTimeMillis())
                else it
            },
        )
        viewModelScope.launch {
            try {
                ChatRepository.suntingPesan(target.id, bersih)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "sunting gagal", e)
                _pesanGalat.tryEmit(pesanRingkas(e))
                muatPesan()
            }
        }
    }

    /** Galat server sering datang sebagai JSON PostgREST; yang berguna bagi
     *  pengguna hanya kalimat di dalamnya. */
    private fun pesanRingkas(e: Exception): String {
        val mentah = e.message ?: return "Gagal menyunting pesan."
        val kunci = "\"message\":\""
        val mulai = mentah.indexOf(kunci)
        if (mulai < 0) return mentah.take(140)
        val sisa = mentah.substring(mulai + kunci.length)
        return sisa.substringBefore("\"").ifBlank { "Gagal menyunting pesan." }
    }

    fun kirimTeks(teks: String, sebutan: List<Sebutan> = emptyList()) {
        val bersih = teks.trim()
        if (bersih.isEmpty()) return
        antre(KirimanTertunda(
            kunci = UUID.randomUUID().toString(),
            body = bersih,
            fotoWebp = null,
            replyTo = _state.value.balasTarget,
            dibuatMs = System.currentTimeMillis(),
            mentions = sebutan,
        ))
    }

    /**
     * Kirim voice note. Berkasnya dibaca ke memori di sini lalu dihapus dari
     * cache: rekaman 5 menit pun di bawah 1,2 MB, dan menyimpan byte-nya di
     * antrean membuat tombol "coba lagi" tetap bekerja walau cache sudah disapu.
     */
    fun kirimSuara(berkas: java.io.File, durasiMs: Int, wave: String) {
        val isi = runCatching { berkas.readBytes() }.getOrNull()
        berkas.delete()
        if (isi == null || isi.isEmpty()) {
            android.util.Log.e("ChatViewModel", "rekaman suara kosong / tidak terbaca")
            return
        }
        antre(KirimanTertunda(
            kunci = UUID.randomUUID().toString(),
            body = "",
            fotoWebp = null,
            suaraM4a = isi,
            suaraMs = durasiMs,
            suaraWave = wave,
            replyTo = _state.value.balasTarget,
            dibuatMs = System.currentTimeMillis(),
        ))
    }

    fun kirimFoto(webp: ByteArray, keterangan: String, sebutan: List<Sebutan> = emptyList()) {
        antre(KirimanTertunda(
            kunci = UUID.randomUUID().toString(),
            body = keterangan.trim(),
            fotoWebp = webp,
            replyTo = _state.value.balasTarget,
            dibuatMs = System.currentTimeMillis(),
            mentions = sebutan,
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
                val pathSuara = kiriman.suaraM4a?.let { ChatRepository.unggahSuara(userId, it) }
                val tersimpan = ChatRepository.kirim(
                    body = kiriman.body,
                    imagePath = path,
                    replyToId = kiriman.replyTo?.id,
                    mentions = kiriman.mentions,
                    audioPath = pathSuara,
                    audioMs = kiriman.suaraMs,
                    audioWave = kiriman.suaraWave,
                )
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

    /**
     * Hapus pesan sendiri — optimis; kalau server menolak, daftar dimuat ulang.
     *
     * Barisnya tidak dibuang dari daftar melainkan diubah jadi nisan, sama
     * seperti yang dilakukan server. Membuangnya di sini akan membuat pesan
     * berkedip hilang lalu muncul lagi begitu pemuatan berikutnya tiba.
     */
    fun hapus(pesan: PesanChat) {
        _state.value = _state.value.copy(
            pesan = _state.value.pesan.map {
                if (it.id != pesan.id) it
                else it.copy(
                    body = "",
                    imagePath = null,
                    mentions = emptyList(),
                    deletedAtMs = System.currentTimeMillis(),
                    // Ditandatangani hanya bila yang dihapus milik orang lain,
                    // sama seperti aturan di server.
                    deletedByName = if (it.senderId == userId) null else namaSendiri,
                )
            },
            // Reaksi ikut lenyap bersama isinya; emoji di bawah nisan hanya
            // menyisakan teka-teki.
            reaksi = _state.value.reaksi - pesan.id,
        )
        viewModelScope.launch {
            try {
                ChatRepository.hapusPesan(pesan.id)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "hapus gagal", e)
                _pesanGalat.tryEmit(pesanRingkas(e))
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
        wallpaper: String? = null,
        onSelesai: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val path = when {
                    fotoBaru != null -> ChatRepository.unggahFotoGrup(userId, fotoBaru)
                    hapusFoto -> ""
                    else -> null
                }
                wallpaper?.let { wp ->
                    _state.value = _state.value.copy(
                        pengaturan = _state.value.pengaturan.copy(wallpaper = wp)
                    )
                }
                ChatRepository.simpanPengaturan(nama, deskripsi, hanyaAdmin, namaSendiri, path, wallpaper)
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
     * Terapkan dan simpan wallpaper secara instan begitu tombol 'Terapkan' diketuk.
     */
    fun simpanWallpaper(idWallpaper: String, onSelesai: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    pengaturan = _state.value.pengaturan.copy(wallpaper = idWallpaper)
                )
                ChatRepository.simpanWallpaper(idWallpaper, namaSendiri)
                muatUlang()
                onSelesai(null)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "simpan wallpaper gagal", e)
                onSelesai(e.message ?: "Gagal menyimpan wallpaper.")
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
            muatBacaanSekarang()
            muatPengaturanSekarang()
            muatAnggota()
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

    private val sudahDitandaiDibaca = mutableSetOf<String>()

    private var infoPesanJob: Job? = null

    /** Buka lembar info pembacaan pesan (Info Pesan ala WA). */
    fun bukaInfoPesan(pesan: PesanChat) {
        infoPesanJob?.cancel()
        _state.update {
            it.copy(
                infoTarget = pesan,
                detailInfoPesan = null,
                pendengarSuara = emptyList(),
                memuatInfo = true,
            )
        }
        val isAudio = pesan.audioPath != null
        infoPesanJob = viewModelScope.launch {
            segarkanInfoPesan(pesan.id, isAudio)
            // Polling periodik 2 detik selama lembar menampilkan pesan ini (menjaga UI tetap real-time jika ada delay/drop jaringan)
            while (isActive && _state.value.infoTarget?.id == pesan.id) {
                delay(2000L)
                if (isActive && _state.value.infoTarget?.id == pesan.id) {
                    segarkanInfoPesan(pesan.id, isAudio)
                }
            }
        }
    }

    fun tutupInfoPesan() {
        infoPesanJob?.cancel()
        infoPesanJob = null
        _state.update {
            it.copy(
                infoTarget = null,
                detailInfoPesan = null,
                pendengarSuara = emptyList(),
                memuatInfo = false,
            )
        }
    }

    private fun segarkanInfoPesan(messageId: String, isAudio: Boolean) {
        viewModelScope.launch {
            try {
                val detail = ChatRepository.ambilInfoPesan(messageId)
                val pendengar = if (isAudio) {
                    try {
                        ChatRepository.ambilPendengarSuara(messageId)
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                _state.update { curr ->
                    if (curr.infoTarget?.id == messageId) {
                        curr.copy(
                            detailInfoPesan = detail,
                            pendengarSuara = if (isAudio) pendengar else curr.pendengarSuara,
                            memuatInfo = false,
                        )
                    } else {
                        curr
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "segarkan info pesan gagal: ${e.message}")
                _state.update { curr ->
                    if (curr.infoTarget?.id == messageId) {
                        curr.copy(memuatInfo = false)
                    } else {
                        curr
                    }
                }
            }
        }
    }

    private fun muatPendengarSuara(messageId: String) {
        viewModelScope.launch {
            try {
                val daftar = ChatRepository.ambilPendengarSuara(messageId)
                _state.update { curr ->
                    if (curr.infoTarget?.id == messageId) {
                        curr.copy(pendengarSuara = daftar)
                    } else {
                        curr
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "pendengar suara gagal: ${e.message}")
            }
        }
    }

    /** Catat bahwa akun ini sudah mendengarkan sebuah pesan suara. */
    fun tandaiSuaraDiputar(messageId: String) {
        if (!sudahDitandaiDengar.add(messageId)) return
        viewModelScope.launch {
            try {
                ChatRepository.tandaiSuaraDiputar(messageId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                sudahDitandaiDengar.remove(messageId)
                android.util.Log.w("ChatViewModel", "tandai suara diputar gagal: ${e.message}")
            }
        }
    }

    private val sudahDitandaiDengar = mutableSetOf<String>()

    fun muatDetailInfoPesan(messageId: String) {
        val isAudio = _state.value.infoTarget?.let { it.id == messageId && it.audioPath != null } ?: false
        segarkanInfoPesan(messageId, isAudio)
    }

    /**
     * Tandai pesan rekan yang belum pernah ditandai dibaca oleh user aktif.
     * Dijalankan secara batch dan aman dari duplikasi.
     */
    fun tandaiPesanDibaca(pesanList: List<PesanChat>) {
        val uid = userId
        if (uid.isBlank()) return
        val belum = pesanList.filter { p ->
            p.senderId != uid &&
            p.id !in sudahDitandaiDibaca &&
            _state.value.bacaan[p.id]?.none { it.userId == uid } != false
        }.map { it.id }

        if (belum.isEmpty()) return
        sudahDitandaiDibaca.addAll(belum)

        viewModelScope.launch {
            try {
                ChatRepository.tandaiDibaca(belum)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "tandai dibaca gagal: ${e.message}")
                sudahDitandaiDibaca.removeAll(belum.toSet())
            }
        }
    }

    private fun muatBacaan() {
        viewModelScope.launch { muatBacaanSekarang() }
    }

    private suspend fun muatBacaanSekarang() {
        try {
            val semua = ChatRepository.ambilSemuaBacaan().groupBy { it.messageId }
            _state.update { curr ->
                if (semua != curr.bacaan) {
                    curr.copy(bacaan = semua)
                } else {
                    curr
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("ChatViewModel", "bacaan tidak tersedia: ${e.message}")
        }
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
                if (ChatKehadiran.terbuka) tandaiPesanDibaca(pesan)
                return
            }
            _state.value = sekarang.copy(memuat = false, galat = null, pesan = pesan)
            segarkanPengetik()
            if (ChatKehadiran.terbuka) tandaiPesanDibaca(pesan)
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
