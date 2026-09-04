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
    return sumber.filter { baris ->
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
}

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
    val bolehTutupDokumen: Boolean = false,
    val sedangMenutup: String? = null,
    val namaPengguna: String = "",
)

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        val staff = AppSession.staff.value
        _state.value = _state.value.copy(
            bolehTutupDokumen = DistribusiAkses.bolehTutupDokumen(staff?.role),
            namaPengguna = staff?.name.orEmpty(),
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
        _state.value = _state.value.copy(rentang = rentang)
        muat(paksa = true)
    }

    fun ubahTab(tab: TabStatus) {
        _state.value = _state.value.copy(tab = tab)
        terapkanFilter()
    }

    fun ubahCari(teks: String) {
        _state.value = _state.value.copy(cari = teks)
        terapkanFilter()
    }

    /** Menekan outlet yang sama dua kali melepas filternya. */
    fun pilihOutlet(nama: String?) {
        val sekarang = _state.value.outletTerpilih
        _state.value = _state.value.copy(outletTerpilih = if (sekarang == nama) null else nama)
        terapkanFilter()
    }

    private fun terapkanFilter() {
        val s = _state.value
        _state.value = s.copy(terlihat = saringDaftar(s.semua, s.tab, s.outletTerpilih, s.cari))
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
