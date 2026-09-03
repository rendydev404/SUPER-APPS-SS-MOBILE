package com.sukashawarma.superapp.feature.stok.ui.opname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.OpnameRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.data.model.OpnameItemRow
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.OpnameHitung
import com.sukashawarma.superapp.feature.stok.domain.Selisih
import com.sukashawarma.superapp.feature.stok.domain.bolehTampilDiOutlet
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OpnameUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    val tidakBerhak: Boolean = false,
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val riwayat: List<OpnameHeader> = emptyList(),
    // Form
    val formTerbuka: Boolean = false,
    val memuatForm: Boolean = false,
    val menyimpan: Boolean = false,
    val opnameId: String? = null,
    val statusDraft: String? = null,
    val items: List<OpnameItemRow> = emptyList(),
    val cari: String = "",
) {
    val itemTampil: List<OpnameItemRow>
        get() {
            val kata = cari.trim().lowercase()
            return if (kata.isEmpty()) items
            else items.filter { it.namaBahan.lowercase().contains(kata) }
        }

    val jumlahTerisi: Int get() = items.count { it.adaMasukan }
}

class OpnameViewModel : ViewModel() {

    private val _state = MutableStateFlow(OpnameUiState())
    val state: StateFlow<OpnameUiState> = _state

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
                muatRiwayat()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(outletTerpilih = outlet, riwayat = emptyList())
        viewModelScope.launch { muatRiwayat() }
    }

    private suspend fun muatRiwayat() {
        val outlet = _state.value.outletTerpilih ?: return
        _state.value = _state.value.copy(memuat = true, error = null)
        try {
            _state.value = _state.value.copy(memuat = false, riwayat = OpnameRepository.daftar(outlet.id))
        } catch (e: Exception) {
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    // ------------------------------------------------------------------ form

    /**
     * Siapkan formulir hitung fisik.
     *
     * Draft dibuat atau dipakai ulang lebih dulu supaya item yang sudah tersimpan
     * bisa dilanjutkan — kru sering menghitung sambil berjalan dan menutup aplikasi
     * di tengah jalan.
     */
    fun bukaForm() {
        val outlet = _state.value.outletTerpilih ?: return
        val staffId = AppSession.staff.value?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(formTerbuka = true, memuatForm = true, error = null, pesan = null)
            try {
                val draft = OpnameRepository.buatAtauPakaiDraft(outlet.id, "harian", staffId)
                val tersimpan = OpnameRepository.itemTersimpan(draft.id)

                val baris = StokRepository.monitoringOutlet(outlet.id)
                    .filter { bolehTampilDiOutlet(it.itemName, it.outletName) }
                    .map { row ->
                        val sistem = OpnameHitung.saldoSistemSmallest(
                            saldo = row.currentQty,
                            saldoIsGram = row.saldoIsGram,
                            meta = row.meta,
                        )
                        // Item yang sudah tersimpan dikembalikan ke kolom satuan kecil
                        // apa adanya; memecahnya lagi ke tiga jenjang berisiko bergeser
                        // karena pembulatan, dan angka kru tidak boleh berubah sendiri.
                        val fisik = tersimpan[row.bahanBakuId]
                        OpnameItemRow(
                            bahanBakuId = row.bahanBakuId,
                            namaBahan = row.itemName,
                            kategori = row.kategori,
                            meta = row.meta,
                            qtySystemSmallest = sistem,
                            saldoIsGram = row.saldoIsGram,
                            terukur = Selisih.ambangPersen(row.meta.satuan, row.meta.satuanKecil) > 0,
                            kecil = fisik?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "",
                        )
                    }
                _state.value = _state.value.copy(
                    memuatForm = false,
                    opnameId = draft.id,
                    statusDraft = draft.status.nilai,
                    items = baris,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuatForm = false, error = stokErrorMessage(e))
            }
        }
    }

    fun tutupForm() {
        _state.value = _state.value.copy(formTerbuka = false, items = emptyList(), opnameId = null, cari = "")
        viewModelScope.launch { muatRiwayat() }
    }

    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }

    fun ubahMasukan(bahanBakuId: String, besar: String? = null, tengah: String? = null, kecil: String? = null) {
        _state.value = _state.value.copy(
            items = _state.value.items.map { item ->
                if (item.bahanBakuId != bahanBakuId) item
                else item.copy(
                    besar = besar ?: item.besar,
                    tengah = tengah ?: item.tengah,
                    kecil = kecil ?: item.kecil,
                )
            }
        )
    }

    /** Hitungan fisik satu baris pada satuan terkecil. */
    fun fisik(item: OpnameItemRow): Double = OpnameHitung.totalFisikSmallest(
        besar = item.besar.toDoubleOrNull() ?: 0.0,
        tengah = item.tengah.toDoubleOrNull() ?: 0.0,
        kecil = item.kecil.toDoubleOrNull() ?: 0.0,
        meta = item.meta,
    )

    fun selisih(item: OpnameItemRow): Double = Selisih.hitung(fisik(item), item.qtySystemSmallest)

    fun ditandai(item: OpnameItemRow): Boolean = Selisih.perluDitandai(
        selisih = selisih(item),
        qtySystem = item.qtySystemSmallest,
        satuan = item.meta.satuan,
        satuanKecil = item.meta.satuanKecil,
    )

    private fun itemUntukDisimpan(opnameId: String): List<OpnameRepository.ItemSimpan> =
        _state.value.items.filter { it.adaMasukan }.map { item ->
            OpnameRepository.ItemSimpan(
                opnameId = opnameId,
                bahanBakuId = item.bahanBakuId,
                qtyFisik = fisik(item),
                qtySystem = item.qtySystemSmallest,
                flagged = ditandai(item),
                catatan = item.catatan.ifBlank { null },
            )
        }

    fun simpanDraft() {
        val opnameId = _state.value.opnameId ?: return
        val items = itemUntukDisimpan(opnameId)
        if (items.isEmpty()) {
            _state.value = _state.value.copy(pesan = "Belum ada item yang diisi.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(menyimpan = true, error = null, pesan = null)
            try {
                OpnameRepository.simpanItem(items)
                _state.value = _state.value.copy(menyimpan = false, pesan = "Draft tersimpan (${items.size} item).")
            } catch (e: Exception) {
                _state.value = _state.value.copy(menyimpan = false, error = stokErrorMessage(e))
            }
        }
    }

    /**
     * Simpan lalu finalisasi.
     *
     * Web SELALU memanggil `finalize` walau ada item yang ditandai — `setPendingApproval`
     * tersedia tetapi tidak pernah dipakai dari formulir. Perilaku itu ditiru persis di
     * sini secara sadar: kalau native menahan opname bertanda untuk approval sementara
     * web langsung memfinalisasi, satu tindakan yang sama akan menghasilkan saldo yang
     * berbeda tergantung perangkat yang dipakai — dan itu lebih berbahaya daripada
     * meneruskan kesenjangan yang sudah ada.
     */
    fun finalisasi() {
        val opnameId = _state.value.opnameId ?: return
        val items = itemUntukDisimpan(opnameId)
        if (items.isEmpty()) {
            _state.value = _state.value.copy(pesan = "Tidak ada item yang diinput.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(menyimpan = true, error = null, pesan = null)
            try {
                OpnameRepository.simpanItem(items)
                OpnameRepository.finalisasi(opnameId)
                val bertanda = items.count { it.flagged }
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    menyimpan = false,
                    formTerbuka = false,
                    items = emptyList(),
                    opnameId = null,
                    pesan = if (bertanda > 0) {
                        "Opname difinalisasi. $bertanda item di luar toleransi tercatat sebagai selisih."
                    } else {
                        "Opname berhasil difinalisasi."
                    },
                )
                muatRiwayat()
            } catch (e: Exception) {
                _state.value = _state.value.copy(menyimpan = false, error = stokErrorMessage(e))
            }
        }
    }

    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }
}
