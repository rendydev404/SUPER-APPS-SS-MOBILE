package com.sukashawarma.superapp.feature.distribusi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.BarisOutlet
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.HitunganStatus
import com.sukashawarma.superapp.feature.distribusi.domain.RingkasanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDitutup
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Jumlah maksimal dokumen yang tampil dalam satu halaman dashboard. */
internal const val SURAT_JALAN_PER_HALAMAN = 5

/** Tab status dashboard — cermin `StatusTab` di `app/dashboard/page.tsx`. */
enum class TabStatus(val label: String) {
    SEMUA("Semua"),
    DRAFT("Draft"),
    DIKIRIM("Dikirim"),
    BELUM_VERIF("Belum Diverifikasi"),
    SELISIH("Ada Selisih"),
    SELESAI("Selesai"),
}

/**
 * Penyaringan dipisah dari ViewModel supaya bisa diuji tanpa coroutine.
 * Ketiga filter digabung dengan AND, sama seperti `filteredShipments` di web.
 */
fun saringDaftar(
    sumber: List<SuratJalanRingkas>,
    tab: TabStatus,
    outlet: String?,
    cari: String,
): List<SuratJalanRingkas> {
    val kunci = cari.trim().lowercase()
    return sumber.withIndex()
        .filter { (_, baris) ->
            val cocokTab = when (tab) {
                TabStatus.SEMUA -> true
                TabStatus.DRAFT -> baris.status == StatusSuratJalan.DRAFT
                TabStatus.DIKIRIM -> baris.status == StatusSuratJalan.DIKIRIM ||
                    baris.status == StatusSuratJalan.DIKIRIM_LENGKAP
                TabStatus.BELUM_VERIF -> baris.status?.bolehDitutup == true
                TabStatus.SELISIH -> baris.adaSelisih
                TabStatus.SELESAI -> baris.status == StatusSuratJalan.SELESAI
            }
            val cocokOutlet = outlet == null || baris.namaOutlet == outlet
            val cocokCari = kunci.isEmpty() ||
                (baris.nomorDokumen ?: baris.id).lowercase().contains(kunci) ||
                (baris.namaOutlet ?: "").lowercase().contains(kunci)
            cocokTab && cocokOutlet && cocokCari
        }
        // Repository sudah meminta created_at.desc. Pengurutan ini tetap
        // dipertahankan di UI agar hasil filter/realtime selalu newest-first.
        // Index asal menjadi tie-breaker supaya baris dengan waktu sama stabil.
        .sortedWith(
            compareByDescending<IndexedValue<SuratJalanRingkas>> {
                waktuUrutSuratJalan(it.value.dibuatPada)
            }.thenBy { it.index }
        )
        .map { it.value }
}

private fun waktuUrutSuratJalan(waktuIso: String?): Long {
    if (waktuIso.isNullOrBlank()) return Long.MIN_VALUE
    return try {
        Instant.parse(waktuIso).toEpochMilli()
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(waktuIso).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(waktuIso).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: Exception) {
                Long.MIN_VALUE
            }
        }
    }
}

/** Potongan daftar siap-render beserta metadata untuk kontrol pagination. */
internal data class HalamanSuratJalan(
    val baris: List<SuratJalanRingkas>,
    val nomor: Int,
    val totalHalaman: Int,
    val totalBaris: Int,
    val urutanMulai: Int,
    val urutanAkhir: Int,
)

internal fun halamanSuratJalan(
    sumber: List<SuratJalanRingkas>,
    nomorDiminta: Int,
): HalamanSuratJalan {
    val totalHalaman = maxOf(1, (sumber.size + SURAT_JALAN_PER_HALAMAN - 1) / SURAT_JALAN_PER_HALAMAN)
    val nomor = nomorDiminta.coerceIn(1, totalHalaman)
    val offset = (nomor - 1) * SURAT_JALAN_PER_HALAMAN
    val baris = sumber.drop(offset).take(SURAT_JALAN_PER_HALAMAN)
    val urutanMulai = if (baris.isEmpty()) 0 else offset + 1

    return HalamanSuratJalan(
        baris = baris,
        nomor = nomor,
        totalHalaman = totalHalaman,
        totalBaris = sumber.size,
        urutanMulai = urutanMulai,
        urutanAkhir = offset + baris.size,
    )
}

/**
 * Menjaga posisi pagination setelah refresh, sambil mencegah halaman kosong
 * jika jumlah data terbaru sudah lebih sedikit.
 */
internal fun halamanAktifSetelahMuat(
    terlihat: List<SuratJalanRingkas>,
    halamanSaatIni: Int,
): Int = halamanSuratJalan(terlihat, halamanSaatIni).nomor

data class DashboardUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    val semua: List<SuratJalanRingkas> = emptyList(),
    val terlihat: List<SuratJalanRingkas> = emptyList(),
    val hitungan: HitunganStatus = HitunganStatus(0, 0, 0, 0),
    val akurasi: Int = 100,
    val rincianOutlet: List<BarisOutlet> = emptyList(),
    val rentang: RentangTanggal = RentangTanggal.SEMUA,
    val tab: TabStatus = TabStatus.SEMUA,
    val cari: String = "",
    val outletTerpilih: String? = null,
    val halamanAktif: Int = 1,
    val bolehTutupDokumen: Boolean = false,
    val sedangMenutup: String? = null,
    val namaPengguna: String = "",
    val namaOutlet: String = "",
    /** Menentukan apakah tombol "Scan QR Kedatangan" di banner ditampilkan.
     *  Pengawas membuka dashboard untuk memantau, bukan menerima barang. */
    val bolehVerifikasi: Boolean = false,
)

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        val staff = AppSession.staff.value
        _state.value = _state.value.copy(
            bolehTutupDokumen = DistribusiAkses.bolehTutupDokumen(staff?.role),
            namaPengguna = staff?.name.orEmpty(),
            namaOutlet = staff?.outletName.orEmpty(),
            bolehVerifikasi = DistribusiAkses.bolehVerifikasi(staff?.role),
        )
        muat()
    }

    fun muat(paksa: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            if (paksa) SuratJalanRepository.invalidate()
            try {
                val daftar = SuratJalanRepository.daftar(_state.value.rentang)
                _state.value = _state.value.copy(
                    memuat = false,
                    semua = daftar,
                    hitungan = RingkasanDistribusi.hitungStatus(daftar),
                    akurasi = RingkasanDistribusi.tingkatAkurasi(daftar),
                    rincianOutlet = RingkasanDistribusi.rincianOutlet(daftar, "Gudang Pusat"),
                )
                terapkanFilter()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    fun ubahRentang(rentang: RentangTanggal) {
        _state.value = _state.value.copy(rentang = rentang, halamanAktif = 1)
        muat(paksa = true)
    }

    fun ubahTab(tab: TabStatus) {
        _state.value = _state.value.copy(tab = tab)
        terapkanFilter(resetHalaman = true)
    }

    fun ubahCari(teks: String) {
        _state.value = _state.value.copy(cari = teks)
        terapkanFilter(resetHalaman = true)
    }

    /** Menekan outlet yang sama dua kali melepas filternya. */
    fun pilihOutlet(nama: String?) {
        val sekarang = _state.value.outletTerpilih
        _state.value = _state.value.copy(outletTerpilih = if (sekarang == nama) null else nama)
        terapkanFilter(resetHalaman = true)
    }

    fun pindahHalaman(nomor: Int) {
        val s = _state.value
        _state.value = s.copy(halamanAktif = halamanSuratJalan(s.terlihat, nomor).nomor)
    }

    private fun terapkanFilter(resetHalaman: Boolean = false) {
        val s = _state.value
        val terlihat = saringDaftar(s.semua, s.tab, s.outletTerpilih, s.cari)
        _state.value = s.copy(
            terlihat = terlihat,
            // Refresh saat kembali dari detail mempertahankan posisi pengguna.
            // Jika jumlah data menyusut, `halamanSuratJalan` melakukan clamp ke
            // halaman terakhir yang masih tersedia agar tidak menampilkan halaman kosong.
            halamanAktif = if (resetHalaman) 1 else halamanAktifSetelahMuat(terlihat, s.halamanAktif),
        )
    }

    /**
     * Menutup dokumen jadi `selesai`. Kedua syaratnya diperiksa di sini sebelum
     * menyentuh jaringan: role harus berhak, dan status harus sudah diverifikasi
     * outlet. RLS di server adalah jaring pengaman terakhir, bukan yang pertama.
     */
    fun tutupDokumen(baris: SuratJalanRingkas) {
        if (!_state.value.bolehTutupDokumen) return
        if (baris.status?.bolehDitutup != true) return
        viewModelScope.launch {
            _state.value = _state.value.copy(sedangMenutup = baris.id, error = null)
            try {
                SuratJalanRepository.tutupDokumen(baris.id)
                _state.value = _state.value.copy(
                    sedangMenutup = null,
                    pesan = "Dokumen ${baris.nomorDokumen ?: ""} ditutup.",
                )
                muat(paksa = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    sedangMenutup = null,
                    error = distribusiErrorMessage(e),
                )
            }
        }
    }

    fun bersihkanPesan() {
        _state.value = _state.value.copy(pesan = null, error = null)
    }
}
