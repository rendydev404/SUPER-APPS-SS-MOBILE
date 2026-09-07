package com.sukashawarma.superapp.feature.manager.ui.pettycash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.data.OutletPilihan
import com.sukashawarma.superapp.feature.manager.data.PettyCashRepository
import com.sukashawarma.superapp.feature.manager.data.WasteRepository
import com.sukashawarma.superapp.feature.manager.domain.FilterReview
import com.sukashawarma.superapp.feature.manager.domain.FilterRiwayat
import com.sukashawarma.superapp.feature.manager.domain.FilterTanggal
import com.sukashawarma.superapp.feature.manager.domain.STATUS_BUTUH_REVIEW
import com.sukashawarma.superapp.feature.manager.domain.TopupPettyCash
import com.sukashawarma.superapp.feature.manager.domain.bolehMemprosesPettyCash
import com.sukashawarma.superapp.feature.manager.domain.saringReview
import com.sukashawarma.superapp.feature.manager.domain.saringRiwayat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class TabPettyCash { REVIEW, RIWAYAT }

data class PettyCashUiState(
    val tab: TabPettyCash = TabPettyCash.REVIEW,
    val daftarOutlet: List<OutletPilihan> = emptyList(),
    val outletTerpilih: String? = null,
    val filterReview: FilterReview = FilterReview.SEMUA,
    val filterRiwayat: FilterRiwayat = FilterRiwayat.SEMUA,
    val filterTanggal: FilterTanggal = FilterTanggal.SEMUA,
    val semua: List<TopupPettyCash> = emptyList(),
    val memuat: Boolean = true,
    val sedangDiproses: Set<String> = emptySet(),
    val galat: String? = null,
    val kabar: String? = null,
    val role: Role? = null,
) {
    val review: List<TopupPettyCash> get() = saringReview(semua, filterReview, filterTanggal)
    val riwayat: List<TopupPettyCash> get() = saringRiwayat(semua, filterRiwayat, filterTanggal)

    /** Angka pada lencana tab dihitung sebelum penyaring, sama seperti web. */
    val jumlahReview: Int get() = semua.count { it.status in STATUS_BUTUH_REVIEW }
    val jumlahRiwayat: Int get() = semua.size - jumlahReview

    val bolehMemproses: Boolean get() = bolehMemprosesPettyCash(role)
    val namaOutletTerpilih: String?
        get() = outletTerpilih?.let { id -> daftarOutlet.find { it.id == id }?.nama }
}

class PettyCashViewModel : ViewModel() {

    private val _state = MutableStateFlow(PettyCashUiState(role = AppSession.staff.value?.role))
    val state: StateFlow<PettyCashUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatDaftarOutlet()
        muatUlang()
    }

    fun pilihTab(tab: TabPettyCash) {
        if (_state.value.tab == tab) return
        _state.value = _state.value.copy(tab = tab)
    }

    // Penyaring bekerja di memori: seluruh 100 baris terbaru sudah ada di tangan,
    // jadi mengganti filter tidak perlu menyentuh jaringan sama sekali.
    fun pilihFilterReview(filter: FilterReview) {
        _state.value = _state.value.copy(filterReview = filter)
    }

    fun pilihFilterRiwayat(filter: FilterRiwayat) {
        _state.value = _state.value.copy(filterRiwayat = filter)
    }

    fun pilihFilterTanggal(filter: FilterTanggal) {
        _state.value = _state.value.copy(filterTanggal = filter)
    }

    /** Penyaring outlet menyentuh jaringan karena ia mempersempit query, bukan hasilnya. */
    fun pilihOutlet(outletId: String?) {
        if (_state.value.outletTerpilih == outletId) return
        _state.value = _state.value.copy(outletTerpilih = outletId)
        muatUlang()
    }

    fun tutupKabar() {
        _state.value = _state.value.copy(kabar = null, galat = null)
    }

    private fun muatDaftarOutlet() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(daftarOutlet = WasteRepository.outletTerakses())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PettyCashViewModel", "muatDaftarOutlet() gagal", e)
            }
        }
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            val awal = _state.value
            _state.value = awal.copy(memuat = true, galat = null)
            try {
                val daftar = PettyCashRepository.topups(awal.outletTerpilih)
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = null,
                    role = AppSession.staff.value?.role,
                    semua = daftar,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PettyCashViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, galat = pesanGalat(e))
            }
        }
    }

    fun setujui(topup: TopupPettyCash) = jalankan(topup, "Pengajuan disetujui dan diteruskan ke Finance.") {
        PettyCashRepository.proses(topup.id, setujui = true)
    }

    fun tolak(topup: TopupPettyCash) = jalankan(topup, "Pengajuan ditolak.") {
        PettyCashRepository.proses(topup.id, setujui = false)
    }

    fun serahkan(topup: TopupPettyCash) = jalankan(topup, "Dana diserahkan ke Leader.") {
        PettyCashRepository.serahkanKeLeader(topup.id)
    }

    private fun jalankan(topup: TopupPettyCash, kabarSukses: String, aksi: suspend () -> Unit) {
        if (!_state.value.bolehMemproses) {
            _state.value = _state.value.copy(
                galat = "Peran Anda tidak berwenang memproses pengajuan petty cash.",
            )
            return
        }
        if (topup.id in _state.value.sedangDiproses) return

        viewModelScope.launch {
            _state.value = _state.value.copy(sedangDiproses = _state.value.sedangDiproses + topup.id)
            try {
                aksi()
                _state.value = _state.value.copy(kabar = kabarSukses)
                // Tidak ada pembaruan optimistis di sini. Status berikutnya ditentukan
                // RPC (bisa 'forwarded_to_finance', bisa juga ditolak karena statusnya
                // sudah berpindah), jadi menebaknya di layar berisiko menampilkan
                // keadaan yang tidak pernah terjadi di server.
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PettyCashViewModel", "jalankan() gagal", e)
                _state.value = _state.value.copy(galat = pesanAksiGagal(e))
            } finally {
                _state.value = _state.value.copy(sedangDiproses = _state.value.sedangDiproses - topup.id)
            }
        }
    }

    /**
     * RPC melempar pesan Postgres yang berguna (mis. status sudah berpindah), jadi
     * pesan itu diteruskan apa adanya alih-alih ditelan menjadi "gagal memproses".
     */
    private fun pesanAksiGagal(e: Exception): String = when {
        e is com.sukashawarma.superapp.data.remote.Postgrest.PostgrestException -> {
            val isi = e.message.orEmpty()
            when {
                "not ready for" in isi ->
                    "Pengajuan ini sudah diproses oleh orang lain. Muat ulang untuk melihat status terbarunya."
                "Not authorized" in isi ->
                    "Peran Anda tidak berwenang memproses pengajuan petty cash."
                else -> "Gagal memproses pengajuan. Coba lagi."
            }
        }
        else -> pesanGalat(e)
    }

    private fun pesanGalat(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat). Coba lagi."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        else -> "Gagal memuat data petty cash. Coba lagi."
    }
}
