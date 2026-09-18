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
import kotlinx.coroutines.flow.update
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
    val memuat: Boolean = false,
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
        muatUlang(silent = true)
    }

    fun pilihTab(tab: TabPettyCash) {
        if (_state.value.tab == tab) return
        _state.update { it.copy(tab = tab) }
    }

    // Penyaring bekerja di memori: seluruh 100 baris terbaru sudah ada di tangan,
    // jadi mengganti filter tidak perlu menyentuh jaringan sama sekali.
    fun pilihFilterReview(filter: FilterReview) {
        _state.update { it.copy(filterReview = filter) }
    }

    fun pilihFilterRiwayat(filter: FilterRiwayat) {
        _state.update { it.copy(filterRiwayat = filter) }
    }

    fun pilihFilterTanggal(filter: FilterTanggal) {
        _state.update { it.copy(filterTanggal = filter) }
    }

    /** Penyaring outlet menyentuh jaringan karena ia mempersempit query, bukan hasilnya. */
    fun pilihOutlet(outletId: String?) {
        if (_state.value.outletTerpilih == outletId) return
        _state.update { it.copy(outletTerpilih = outletId) }
        muatUlang(silent = true)
    }

    fun tutupKabar() {
        _state.update { it.copy(kabar = null, galat = null) }
    }

    private fun muatDaftarOutlet() {
        viewModelScope.launch {
            try {
                val outlet = WasteRepository.outletTerakses()
                _state.update { it.copy(daftarOutlet = outlet) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PettyCashViewModel", "muatDaftarOutlet() gagal", e)
            }
        }
    }

    fun muatUlang(silent: Boolean = false) {
        val sudahAdaData = _state.value.semua.isNotEmpty()
        val senyap = silent || sudahAdaData

        // Pemuatan lama selalu dibatalkan: kalau ia dibiarkan berjalan, pergantian
        // outlet saat refresh masih jalan tidak akan pernah mengambil data baru.
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            val outletDiminta = _state.value.outletTerpilih
            if (!senyap) {
                _state.update { it.copy(memuat = true, galat = null) }
            }
            try {
                val daftar = PettyCashRepository.topups(outletDiminta)
                _state.update {
                    it.copy(
                        memuat = false,
                        galat = null,
                        role = AppSession.staff.value?.role,
                        semua = daftar,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PettyCashViewModel", "muatUlang() gagal", e)
                if (!senyap) {
                    _state.update { it.copy(memuat = false, galat = pesanGalat(e)) }
                }
            } finally {
                _state.update { if (it.memuat) it.copy(memuat = false) else it }
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
            _state.update {
                it.copy(galat = "Peran Anda tidak berwenang memproses pengajuan petty cash.")
            }
            return
        }
        // Kunci dipasang sebelum coroutine dimulai supaya ketukan ganda yang cepat
        // tidak sempat mengirim dua RPC.
        var lolos = false
        _state.update {
            lolos = topup.id !in it.sedangDiproses
            if (lolos) it.copy(sedangDiproses = it.sedangDiproses + topup.id) else it
        }
        if (!lolos) return

        viewModelScope.launch {
            try {
                aksi()
                _state.update { it.copy(kabar = kabarSukses) }
                // Tidak ada pembaruan optimistis di sini. Status berikutnya ditentukan
                // RPC (bisa 'forwarded_to_finance', bisa juga ditolak karena statusnya
                // sudah berpindah), jadi menebaknya di layar berisiko menampilkan
                // keadaan yang tidak pernah terjadi di server.
                muatUlang(silent = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PettyCashViewModel", "jalankan() gagal", e)
                _state.update { it.copy(galat = pesanAksiGagal(e)) }
            } finally {
                _state.update { it.copy(sedangDiproses = it.sedangDiproses - topup.id) }
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
