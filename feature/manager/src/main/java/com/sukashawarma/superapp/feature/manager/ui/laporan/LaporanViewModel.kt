package com.sukashawarma.superapp.feature.manager.ui.laporan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.manager.data.LaporanRepository
import com.sukashawarma.superapp.feature.manager.data.OutletPilihan
import com.sukashawarma.superapp.feature.manager.data.WasteRepository
import com.sukashawarma.superapp.feature.manager.domain.AnalitikLaporan
import com.sukashawarma.superapp.feature.manager.domain.FilterChannel
import com.sukashawarma.superapp.feature.manager.domain.FilterPembayaran
import com.sukashawarma.superapp.feature.manager.domain.PresetLaporan
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.ZONA_JAKARTA
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LaporanUiState(
    val preset: PresetLaporan = PresetLaporan.HARI_INI,
    val kustom: RentangTanggal? = null,
    val channel: FilterChannel = FilterChannel.SEMUA,
    val pembayaran: FilterPembayaran = FilterPembayaran.SEMUA,
    val daftarOutlet: List<OutletPilihan> = emptyList(),
    val outletTerpilih: String? = null,
    val analitik: AnalitikLaporan = AnalitikLaporan.KOSONG,
    val memuat: Boolean = true,
    val galat: String? = null,
) {
    val rentang: RentangTanggal
        get() = if (preset == PresetLaporan.KUSTOM) {
            kustom ?: PresetLaporan.HARI_INI.rentang()
        } else {
            preset.rentang()
        }

    val labelRentang: String
        get() = if (preset == PresetLaporan.KUSTOM && kustom != null) {
            "${kustom.dari} - ${kustom.sampai}"
        } else {
            preset.label
        }

    val namaOutletTerpilih: String?
        get() = outletTerpilih?.let { id -> daftarOutlet.find { it.id == id }?.nama }

    /** Identitas satu kombinasi penyaring — kunci singgahan hasil. */
    internal val kunciMuat: String
        get() = "${rentang.dari}|${rentang.sampai}|${channel.kunci}|${pembayaran.kunci}|${outletTerpilih ?: "-"}"

    /**
     * Periodenya masih mencakup hari ini, jadi angkanya masih bisa berubah.
     *
     * Periode yang sudah lewat TIDAK bisa berubah lagi — pesanan baru selalu masuk
     * hari ini. Untuk periode seperti itu, hasil yang sudah pernah dimuat adalah
     * jawaban final dan tidak perlu ditembak ulang ke jaringan sama sekali.
     */
    internal val periodeBerjalan: Boolean
        get() = !rentang.sampai.isBefore(java.time.LocalDate.now(ZONA_JAKARTA))
}

class LaporanViewModel : ViewModel() {

    private val _state = MutableStateFlow(LaporanUiState())
    val state: StateFlow<LaporanUiState> = _state

    private var pemuatan: Job? = null
    private var penyegaran: Job? = null

    /**
     * Hasil per kombinasi penyaring, LRU 12 entri.
     *
     * Berpindah-pindah penyaring lalu kembali adalah pola paling lazim di layar ini,
     * dan sebelumnya setiap kunjungan ulang membayar penuh lagi. Dua belas entri
     * cukup untuk seluruh preset dikali beberapa outlet, dan isinya hanya angka
     * rangkuman — bukan puluhan ribu baris pesanan.
     */
    private val singgahan = object : LinkedHashMap<String, AnalitikLaporan>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AnalitikLaporan>?): Boolean =
            size > 12
    }

    private companion object {
        /**
         * Jeda peredam sebelum langganan realtime memuat ulang laporan.
         *
         * `orders` berdetak tiap kali kasir mana pun menutup pesanan. Saat jam makan
         * siang itu bisa beberapa kali per detik lintas outlet, dan setiap detak
         * dulunya memicu pemuatan penuh — layar yang tidak pernah selesai memuat
         * justru karena datanya terlalu hidup. Satu ledakan kini menyusut jadi satu
         * pemuatan.
         */
        const val JEDA_REALTIME_MS = 2_500L
    }

    init {
        muatDaftarOutlet()
        muatUlang()
    }

    fun pilihPreset(preset: PresetLaporan) {
        // Memilih "Kustom" tanpa tanggal belum mengubah apa pun; layar membuka
        // pemilih tanggal dan pemuatan menunggu sampai rentangnya benar-benar ada.
        if (preset == PresetLaporan.KUSTOM && _state.value.kustom == null) {
            _state.update { it.copy(preset = preset) }
            return
        }
        if (_state.value.preset == preset) return
        _state.update { it.copy(preset = preset) }
        muatUlang()
    }

    fun pilihRentangKustom(dari: LocalDate, sampai: LocalDate) {
        val rentang = if (sampai.isBefore(dari)) RentangTanggal(sampai, dari) else RentangTanggal(dari, sampai)
        _state.update { it.copy(preset = PresetLaporan.KUSTOM, kustom = rentang) }
        muatUlang()
    }

    fun pilihChannel(channel: FilterChannel) {
        if (_state.value.channel == channel) return
        _state.update { it.copy(channel = channel) }
        muatUlang()
    }

    fun pilihPembayaran(pembayaran: FilterPembayaran) {
        if (_state.value.pembayaran == pembayaran) return
        _state.update { it.copy(pembayaran = pembayaran) }
        muatUlang()
    }

    fun pilihOutlet(outletId: String?) {
        if (_state.value.outletTerpilih == outletId) return
        _state.update { it.copy(outletTerpilih = outletId) }
        muatUlang()
    }

    private fun muatDaftarOutlet() {
        viewModelScope.launch {
            try {
                val outlet = WasteRepository.outletTerakses()
                _state.update { it.copy(daftarOutlet = outlet) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Penyaring outlet adalah pelengkap; kegagalannya tidak boleh menutupi angka.
                android.util.Log.e("LaporanViewModel", "muatDaftarOutlet() gagal", e)
            }
        }
    }

    /**
     * Muat ulang atas permintaan langganan realtime.
     *
     * Diredam, dan sengaja TIDAK menyalakan indikator memuat: pesanan yang masuk di
     * outlet lain bukan sesuatu yang diminta pengguna layar ini, jadi angkanya
     * diperbarui diam-diam alih-alih mengosongkan layar yang sedang dibaca.
     */
    fun segarkanDariRealtime() {
        penyegaran?.cancel()
        penyegaran = viewModelScope.launch {
            delay(JEDA_REALTIME_MS)
            // Ditanggalkan lebih dulu supaya muatUlang() tidak membatalkan coroutine
            // yang sedang memanggilnya sendiri.
            penyegaran = null
            muatUlang(diam = true)
        }
    }

    /** Tombol muat ulang: selalu menembak jaringan, singgahan diabaikan. */
    fun muatPaksa() = muatUlang(paksa = true)

    fun muatUlang(diam: Boolean = false, paksa: Boolean = false) {
        penyegaran?.cancel()
        pemuatan?.cancel()

        val awal = _state.value
        val kunci = awal.kunciMuat
        // Singgahan LinkedHashMap ber-accessOrder ikut berubah bentuk saat dibaca,
        // jadi baca dan tulisnya sama-sama harus di bawah kunci yang sama.
        val tersimpan = if (paksa) null else synchronized(singgahan) { singgahan[kunci] }

        if (tersimpan != null) {
            _state.update { it.copy(memuat = false, galat = null, analitik = tersimpan) }
            // Periode yang sudah lewat tidak akan berubah lagi: berhenti di sini,
            // tanpa satu pun permintaan jaringan.
            if (!awal.periodeBerjalan) return
        }

        pemuatan = viewModelScope.launch {
            // Indikator memuat hanya untuk layar yang benar-benar kosong. Bila sudah
            // ada angka dari singgahan, pembaruannya berjalan di belakang tanpa
            // mengosongkan apa yang sedang dibaca.
            _state.update { it.copy(memuat = tersimpan == null && !diam, galat = null) }
            try {
                val analitik = LaporanRepository.analitik(
                    rentang = awal.rentang,
                    channel = awal.channel,
                    pembayaran = awal.pembayaran,
                    outletId = awal.outletTerpilih,
                )
                synchronized(singgahan) { singgahan[kunci] = analitik }
                // Hasil hanya dipasang bila penyaringnya belum berpindah; kalau sudah,
                // angka kombinasi lama akan tampil di bawah label kombinasi baru.
                _state.update {
                    if (it.kunciMuat != kunci) {
                        it
                    } else {
                        it.copy(
                            memuat = false,
                            galat = null,
                            analitik = analitik,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LaporanViewModel", "muatUlang() gagal", e)
                // Angka lama yang masih terpampang lebih berguna daripada spanduk
                // galat yang menggantikannya. Galat hanya ditampilkan bila memang
                // tidak ada apa pun untuk dibaca.
                _state.update {
                    if (it.kunciMuat != kunci) {
                        it
                    } else {
                        it.copy(
                            memuat = false,
                            galat = if (tersimpan == null) pesanGalat(e) else null,
                        )
                    }
                }
            }
        }
    }

    private fun pesanGalat(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat). Coba lagi."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        else -> "Gagal memuat laporan. Coba lagi."
    }
}
