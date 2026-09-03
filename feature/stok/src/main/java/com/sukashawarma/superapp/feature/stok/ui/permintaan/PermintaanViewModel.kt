package com.sukashawarma.superapp.feature.stok.ui.permintaan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.PermintaanRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.data.model.Permintaan
import com.sukashawarma.superapp.feature.stok.data.model.SaranPermintaan
import com.sukashawarma.superapp.feature.stok.domain.Approver
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil

enum class TabPermintaan(val label: String) { OUTLET("Outlet Saya"), REVIEW("Perlu Review") }

data class PermintaanUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    val tidakBerhak: Boolean = false,
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val tab: TabPermintaan = TabPermintaan.OUTLET,
    val daftarOutlet: List<Permintaan> = emptyList(),
    val daftarReview: List<Permintaan> = emptyList(),
    val bolehReview: Boolean = false,
    val bolehApprove: Boolean = false,
    // Formulir pengajuan
    val formTerbuka: Boolean = false,
    val memuatSaran: Boolean = false,
    val saran: List<SaranPermintaan> = emptyList(),
    /** Jumlah yang diminta per bahan, sebagai teks agar bisa dikosongkan. */
    val jumlah: Map<String, String> = emptyMap(),
    val mengirim: Boolean = false,
    // Formulir persetujuan
    val approveUntuk: Permintaan? = null,
    val qtySetuju: Map<String, String> = emptyMap(),
) {
    val terpilihUntukDiminta: List<Pair<SaranPermintaan, Double>>
        get() = saran.mapNotNull { s ->
            val q = jumlah[s.bahanBakuId]?.toDoubleOrNull() ?: return@mapNotNull null
            if (q > 0) s to q else null
        }
}

class PermintaanViewModel : ViewModel() {

    private val _state = MutableStateFlow(PermintaanUiState())
    val state: StateFlow<PermintaanUiState> = _state

    init { muatAwal() }

    fun muatAwal() {
        viewModelScope.launch {
            val role = AppSession.staff.value?.role
            _state.value = _state.value.copy(
                memuat = true, error = null, tidakBerhak = false,
                bolehReview = Approver.bolehReviewPermintaan(role),
                bolehApprove = Approver.bolehApprovePermintaan(role),
            )
            try {
                val outlets = StokRepository.accessibleOutlets()
                if (outlets.isEmpty()) {
                    _state.value = _state.value.copy(memuat = false, tidakBerhak = true)
                    return@launch
                }
                val terpilih = _state.value.outletTerpilih?.let { lama ->
                    outlets.firstOrNull { it.id == lama.id }
                } ?: outlets.first()
                _state.value = _state.value.copy(outlets = outlets, outletTerpilih = terpilih)
                muatDaftar()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(outletTerpilih = outlet, daftarOutlet = emptyList())
        viewModelScope.launch { muatDaftar() }
    }

    fun pilihTab(tab: TabPermintaan) {
        _state.value = _state.value.copy(tab = tab)
        viewModelScope.launch { muatDaftar() }
    }

    private suspend fun muatDaftar() {
        val s = _state.value
        val outlet = s.outletTerpilih ?: return
        _state.value = _state.value.copy(memuat = true, error = null)
        try {
            if (s.tab == TabPermintaan.OUTLET) {
                _state.value = _state.value.copy(
                    memuat = false,
                    daftarOutlet = PermintaanRepository.daftarOutlet(outlet.id),
                )
            } else {
                _state.value = _state.value.copy(
                    memuat = false,
                    daftarReview = PermintaanRepository.menunggu(s.outlets.map { it.id }),
                )
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    // ------------------------------------------------------------- pengajuan

    fun bukaForm() {
        val outlet = _state.value.outletTerpilih ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(formTerbuka = true, memuatSaran = true, error = null, pesan = null)
            try {
                val saran = PermintaanRepository.saran(outlet.id)
                // Saran jumlah = kekurangan menuju threshold, minimal 1 dan dibulatkan
                // ke atas — cermin `shortage base = max(1, ceil(threshold - saldo))` di web.
                val awal = saran.associate { s ->
                    val kurang = s.threshold - s.currentQty
                    s.bahanBakuId to maxOf(1.0, ceil(kurang)).toLong().toString()
                }
                _state.value = _state.value.copy(memuatSaran = false, saran = saran, jumlah = awal)
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuatSaran = false, error = stokErrorMessage(e))
            }
        }
    }

    fun tutupForm() {
        _state.value = _state.value.copy(formTerbuka = false, saran = emptyList(), jumlah = emptyMap())
    }

    fun ubahJumlah(bahanBakuId: String, nilai: String) {
        if (nilai.isNotEmpty() && !nilai.matches(Regex("^\\d*\\.?\\d*$"))) return
        _state.value = _state.value.copy(jumlah = _state.value.jumlah + (bahanBakuId to nilai))
    }

    fun kirimPermintaan() {
        val outlet = _state.value.outletTerpilih ?: return
        val staffId = AppSession.staff.value?.id ?: return
        val items = _state.value.terpilihUntukDiminta
        if (items.isEmpty()) {
            _state.value = _state.value.copy(pesan = "Isi jumlah minimal satu bahan.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true, error = null, pesan = null)
            try {
                PermintaanRepository.buat(
                    outletId = outlet.id,
                    dibuatOleh = staffId,
                    items = items.map { (s, q) -> PermintaanRepository.ItemDiminta(s.bahanBakuId, q) },
                )
                _state.value = _state.value.copy(
                    mengirim = false,
                    formTerbuka = false,
                    saran = emptyList(),
                    jumlah = emptyMap(),
                    pesan = "Permintaan terkirim (${items.size} bahan).",
                )
                muatDaftar()
            } catch (e: Exception) {
                _state.value = _state.value.copy(mengirim = false, error = stokErrorMessage(e))
            }
        }
    }

    // ------------------------------------------------------------ persetujuan

    /** Default jumlah disetujui = jumlah diminta, dibulatkan ke atas seperti web. */
    fun bukaApprove(p: Permintaan) {
        _state.value = _state.value.copy(
            approveUntuk = p,
            qtySetuju = p.items.associate { it.bahanBakuId to ceil(it.qtyDiminta).toLong().toString() },
        )
    }

    fun tutupApprove() {
        _state.value = _state.value.copy(approveUntuk = null, qtySetuju = emptyMap())
    }

    fun ubahQtySetuju(bahanBakuId: String, nilai: String) {
        if (nilai.isNotEmpty() && !nilai.matches(Regex("^\\d*\\.?\\d*$"))) return
        _state.value = _state.value.copy(qtySetuju = _state.value.qtySetuju + (bahanBakuId to nilai))
    }

    fun setujui() {
        val p = _state.value.approveUntuk ?: return
        val items = p.items.map { item ->
            PermintaanRepository.ItemDisetujui(
                bahanBakuId = item.bahanBakuId,
                qtyDisetujui = _state.value.qtySetuju[item.bahanBakuId]?.toDoubleOrNull() ?: 0.0,
            )
        }
        // RPC menolak bila semua item nol dan menyuruh memakai jalur tolak; dicegat di
        // sini supaya pengguna tidak menerima pesan error mentah dari database.
        if (items.none { it.qtyDisetujui > 0 }) {
            _state.value = _state.value.copy(
                pesan = "Tidak ada item dengan jumlah di atas nol. Gunakan tombol Tolak bila memang ditolak."
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true, error = null, pesan = null)
            try {
                PermintaanRepository.setujui(p.id, items)
                _state.value = _state.value.copy(
                    mengirim = false, approveUntuk = null, qtySetuju = emptyMap(),
                    pesan = "Permintaan disetujui dan surat jalan dibuat.",
                )
                muatDaftar()
            } catch (e: Exception) {
                _state.value = _state.value.copy(mengirim = false, error = stokErrorMessage(e))
            }
        }
    }

    fun tolak(alasan: String) {
        val p = _state.value.approveUntuk ?: return
        if (alasan.isBlank()) {
            _state.value = _state.value.copy(pesan = "Alasan penolakan wajib diisi.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true, error = null, pesan = null)
            try {
                PermintaanRepository.tolak(p.id, alasan)
                _state.value = _state.value.copy(
                    mengirim = false, approveUntuk = null, qtySetuju = emptyMap(),
                    pesan = "Permintaan ditolak.",
                )
                muatDaftar()
            } catch (e: Exception) {
                _state.value = _state.value.copy(mengirim = false, error = stokErrorMessage(e))
            }
        }
    }

    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }
}
