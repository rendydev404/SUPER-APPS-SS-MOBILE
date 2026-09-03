package com.sukashawarma.superapp.feature.stok.ui.mutasi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.stok.data.MutasiRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.Mutasi
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.data.model.StatusMutasi
import com.sukashawarma.superapp.feature.stok.domain.bolehTampilDiOutlet
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Bahan yang bisa dipilih saat mengajukan mutasi, beserta sisa stoknya. */
data class BahanPilihan(
    val bahanBakuId: String,
    val nama: String,
    val satuan: String?,
    val sisa: Double,
)

data class MutasiUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    val tidakBerhak: Boolean = false,
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val daftar: List<Mutasi> = emptyList(),
    val memproses: Boolean = false,
    // Pengajuan
    val formTerbuka: Boolean = false,
    val memuatForm: Boolean = false,
    val outletTujuan: List<OutletRingkas> = emptyList(),
    val tujuanTerpilih: OutletRingkas? = null,
    val bahan: List<BahanPilihan> = emptyList(),
    val jumlah: Map<String, String> = emptyMap(),
    val catatan: String = "",
    val cari: String = "",
    // Tindakan pada satu mutasi
    val detailUntuk: Mutasi? = null,
    val qtyTindakan: Map<String, String> = emptyMap(),
    val kurir: String = "",
) {
    val bahanTampil: List<BahanPilihan>
        get() {
            val kata = cari.trim().lowercase()
            return if (kata.isEmpty()) bahan else bahan.filter { it.nama.lowercase().contains(kata) }
        }

    val itemDiajukan: List<Pair<BahanPilihan, Double>>
        get() = bahan.mapNotNull { b ->
            val q = jumlah[b.bahanBakuId]?.toDoubleOrNull() ?: return@mapNotNull null
            if (q > 0) b to q else null
        }
}

class MutasiViewModel : ViewModel() {

    private val _state = MutableStateFlow(MutasiUiState())
    val state: StateFlow<MutasiUiState> = _state

    init { muatAwal() }

    fun muatAwal() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null, tidakBerhak = false)
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
        _state.value = _state.value.copy(outletTerpilih = outlet, daftar = emptyList())
        viewModelScope.launch { muatDaftar() }
    }

    private suspend fun muatDaftar() {
        val outlet = _state.value.outletTerpilih ?: return
        _state.value = _state.value.copy(memuat = true, error = null)
        try {
            _state.value = _state.value.copy(memuat = false, daftar = MutasiRepository.daftar(outlet.id))
        } catch (e: Exception) {
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    // ------------------------------------------------------------- pengajuan

    fun bukaForm() {
        val outlet = _state.value.outletTerpilih ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(formTerbuka = true, memuatForm = true, error = null, pesan = null)
            try {
                val tujuan = MutasiRepository.outletTujuan(outlet.id)
                val bahan = StokRepository.monitoringOutlet(outlet.id)
                    .filter { bolehTampilDiOutlet(it.itemName, it.outletName) }
                    .map {
                        BahanPilihan(
                            bahanBakuId = it.bahanBakuId,
                            nama = it.itemName,
                            satuan = it.satuan,
                            sisa = it.currentQty,
                        )
                    }
                _state.value = _state.value.copy(
                    memuatForm = false,
                    outletTujuan = tujuan,
                    tujuanTerpilih = tujuan.firstOrNull(),
                    bahan = bahan,
                    jumlah = emptyMap(),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuatForm = false, error = stokErrorMessage(e))
            }
        }
    }

    fun tutupForm() {
        _state.value = _state.value.copy(
            formTerbuka = false, bahan = emptyList(), jumlah = emptyMap(), catatan = "", cari = "",
        )
    }

    fun pilihTujuan(o: OutletRingkas) { _state.value = _state.value.copy(tujuanTerpilih = o) }
    fun ubahCari(t: String) { _state.value = _state.value.copy(cari = t) }
    fun ubahCatatan(t: String) { _state.value = _state.value.copy(catatan = t) }

    fun ubahJumlah(bahanBakuId: String, nilai: String) {
        if (nilai.isNotEmpty() && !nilai.matches(Regex("^\\d*\\.?\\d*$"))) return
        _state.value = _state.value.copy(jumlah = _state.value.jumlah + (bahanBakuId to nilai))
    }

    fun ajukan() {
        val asal = _state.value.outletTerpilih ?: return
        val tujuan = _state.value.tujuanTerpilih
        if (tujuan == null) {
            _state.value = _state.value.copy(pesan = "Pilih outlet tujuan terlebih dahulu.")
            return
        }
        // Outlet asal dan tujuan wajib berbeda — dijaga di sini dan di database.
        if (tujuan.id == asal.id) {
            _state.value = _state.value.copy(pesan = "Outlet tujuan harus berbeda dari outlet asal.")
            return
        }
        val items = _state.value.itemDiajukan
        if (items.isEmpty()) {
            _state.value = _state.value.copy(pesan = "Isi jumlah minimal satu bahan.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(memproses = true, error = null, pesan = null)
            try {
                MutasiRepository.ajukan(
                    outletAsalId = asal.id,
                    outletTujuanId = tujuan.id,
                    catatan = _state.value.catatan,
                    items = items.map { (b, q) -> MutasiRepository.ItemAjuan(b.bahanBakuId, q) },
                )
                _state.value = _state.value.copy(
                    memproses = false, formTerbuka = false, bahan = emptyList(),
                    jumlah = emptyMap(), catatan = "",
                    pesan = "Mutasi diajukan ke ${tujuan.name}.",
                )
                muatDaftar()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memproses = false, error = stokErrorMessage(e))
            }
        }
    }

    // -------------------------------------------------------------- tindakan

    /**
     * Buka lembar tindakan. Nilai awal mengikuti langkah berikutnya pada alur:
     * saat mengirim, jumlah dikirim mengikuti jumlah diajukan; saat menerima,
     * jumlah diterima mengikuti jumlah dikirim — sama seperti default di web.
     */
    fun bukaDetail(m: Mutasi) {
        val awal = when (m.status) {
            StatusMutasi.MENUNGGU_PENGIRIMAN -> m.items.associate { it.id to bersih(it.qtyDiajukan) }
            StatusMutasi.DIKIRIM -> m.items.associate { it.id to bersih(it.qtyDikirim ?: it.qtyDiajukan) }
            else -> emptyMap()
        }
        _state.value = _state.value.copy(detailUntuk = m, qtyTindakan = awal, kurir = "")
    }

    private fun bersih(nilai: Double): String =
        if (nilai % 1.0 == 0.0) nilai.toLong().toString() else nilai.toString()

    fun tutupDetail() {
        _state.value = _state.value.copy(detailUntuk = null, qtyTindakan = emptyMap(), kurir = "")
    }

    fun ubahQtyTindakan(itemId: String, nilai: String) {
        if (nilai.isNotEmpty() && !nilai.matches(Regex("^\\d*\\.?\\d*$"))) return
        _state.value = _state.value.copy(qtyTindakan = _state.value.qtyTindakan + (itemId to nilai))
    }

    fun ubahKurir(t: String) { _state.value = _state.value.copy(kurir = t) }

    fun setujui(disetujui: Boolean, alasan: String = "") {
        val m = _state.value.detailUntuk ?: return
        if (!disetujui && alasan.isBlank()) {
            _state.value = _state.value.copy(pesan = "Alasan penolakan wajib diisi.")
            return
        }
        jalankan("Mutasi ${if (disetujui) "disetujui" else "ditolak"}.") {
            MutasiRepository.setujui(m.id, disetujui, alasan)
        }
    }

    fun kirim() {
        val m = _state.value.detailUntuk ?: return
        val items = m.items.map {
            MutasiRepository.ItemKirim(
                itemId = it.id,
                qtyDikirim = _state.value.qtyTindakan[it.id]?.toDoubleOrNull() ?: 0.0,
            )
        }
        if (items.none { it.qtyDikirim > 0 }) {
            _state.value = _state.value.copy(pesan = "Minimal satu bahan harus punya jumlah kirim di atas nol.")
            return
        }
        jalankan("Mutasi ditandai terkirim; stok outlet asal berkurang.") {
            MutasiRepository.kirim(m.id, _state.value.kurir, items)
        }
    }

    fun terima(kondisi: String) {
        val m = _state.value.detailUntuk ?: return
        val items = m.items.map {
            MutasiRepository.ItemTerima(
                itemId = it.id,
                qtyDiterima = _state.value.qtyTindakan[it.id]?.toDoubleOrNull() ?: 0.0,
                kondisi = kondisi,
            )
        }
        jalankan("Mutasi diterima; stok outlet tujuan bertambah.") {
            MutasiRepository.terima(m.id, items)
        }
    }

    private fun jalankan(pesanSukses: String, aksi: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(memproses = true, error = null, pesan = null)
            try {
                aksi()
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    memproses = false, detailUntuk = null, qtyTindakan = emptyMap(),
                    kurir = "", pesan = pesanSukses,
                )
                muatDaftar()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memproses = false, error = stokErrorMessage(e))
            }
        }
    }

    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }
}
