package com.sukashawarma.superapp.feature.stok.ui.po

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.stok.data.ItemPo
import com.sukashawarma.superapp.feature.stok.data.PenerimaanPoRepository
import com.sukashawarma.superapp.feature.stok.data.PoInbound
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Isian pemeriksaan fisik satu baris PO. */
data class IsianItemPo(
    val qtyDatang: String = "",
    val hargaTerima: String = "",
    val kondisi: String = "baik",
    val catatan: String = "",
)

data class PenerimaanPoUiState(
    val daftar: List<PoInbound> = emptyList(),
    val cari: String = "",
    val poDibuka: PoInbound? = null,
    val item: List<ItemPo> = emptyList(),
    val isian: Map<String, IsianItemPo> = emptyMap(),
    val memuat: Boolean = true,
    val memuatItem: Boolean = false,
    val mengirim: Boolean = false,
    val pesan: String? = null,
    val error: String? = null,
) {
    val daftarTampil: List<PoInbound>
        get() {
            val kueri = cari.trim().lowercase()
            if (kueri.isEmpty()) return daftar
            return daftar.filter {
                it.nomorPo.lowercase().contains(kueri) || it.supplierNama.lowercase().contains(kueri)
            }
        }

    fun isianUntuk(itemId: String): IsianItemPo = isian[itemId] ?: IsianItemPo()

    /** Alasan belum bisa dikirim, atau null bila sudah boleh. */
    val halangan: String?
        get() {
            if (item.isEmpty()) return "PO ini tidak punya baris barang."
            val adaIsi = item.any { (isianUntuk(it.id).qtyDatang.toDoubleOrNull() ?: 0.0) > 0.0 }
            if (!adaIsi) return "Isi jumlah yang datang minimal pada satu barang."
            val hargaSalah = item.firstOrNull {
                val isi = isianUntuk(it.id)
                (isi.qtyDatang.toDoubleOrNull() ?: 0.0) > 0.0 && isi.hargaTerima.toDoubleOrNull() == null
            }
            if (hargaSalah != null) return "Harga terima ${hargaSalah.nama} belum diisi."
            return null
        }
}

/**
 * Penerimaan PO supplier — cermin `penerimaan-po` + `KitchenVerifikasiModal` web.
 *
 * Unggah foto faktur sengaja belum diikutkan: kolom `purchase_order.invoice_urls`
 * di web ditulis dengan klien service-role dalam alur yang sama, dan menulisnya
 * dari JWT pengguna belum tentu lolos RLS. Verifikasi barangnya sendiri tidak
 * bergantung pada foto itu.
 */
class PenerimaanPoViewModel : ViewModel() {

    private val _state = MutableStateFlow(PenerimaanPoUiState())
    val state: StateFlow<PenerimaanPoUiState> = _state

    init {
        muatUlang()
    }

    fun muatUlang() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                _state.value = _state.value.copy(memuat = false, daftar = PenerimaanPoRepository.daftar())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PenerimaanPoVM", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }
    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }

    fun bukaPo(po: PoInbound) {
        _state.value = _state.value.copy(poDibuka = po, memuatItem = true, item = emptyList(), isian = emptyMap())
        viewModelScope.launch {
            try {
                val item = PenerimaanPoRepository.item(po.id)
                // Nilai awal mengikuti web: yang datang = sisa yang belum tiba, dan
                // harga terima = harga pesan. Keduanya tinggal dikoreksi kalau berbeda,
                // sehingga kasus normal cukup ditekan simpan.
                val isian = item.associate { baris ->
                    baris.id to IsianItemPo(
                        qtyDatang = angkaRapi(if (baris.sisa > 0) baris.sisa else baris.qtyPesan),
                        hargaTerima = angkaRapi(baris.hargaPesan),
                    )
                }
                _state.value = _state.value.copy(memuatItem = false, item = item, isian = isian)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PenerimaanPoVM", "bukaPo() gagal", e)
                _state.value = _state.value.copy(memuatItem = false, error = stokErrorMessage(e))
            }
        }
    }

    fun tutupPo() {
        _state.value = _state.value.copy(poDibuka = null, item = emptyList(), isian = emptyMap())
    }

    private fun ubah(itemId: String, blok: (IsianItemPo) -> IsianItemPo) {
        _state.value = _state.value.copy(isian = _state.value.isian + (itemId to blok(_state.value.isianUntuk(itemId))))
    }

    fun ubahQty(itemId: String, teks: String) = ubah(itemId) { it.copy(qtyDatang = teks) }
    fun ubahHarga(itemId: String, teks: String) = ubah(itemId) { it.copy(hargaTerima = teks) }
    fun ubahKondisi(itemId: String, kondisi: String) = ubah(itemId) { it.copy(kondisi = kondisi) }
    fun ubahCatatan(itemId: String, teks: String) = ubah(itemId) { it.copy(catatan = teks) }

    fun kirim() {
        val s = _state.value
        val po = s.poDibuka ?: return
        val halangan = s.halangan
        if (halangan != null) {
            _state.value = s.copy(error = halangan)
            return
        }
        if (s.mengirim) return

        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true, error = null, pesan = null)
            try {
                PenerimaanPoRepository.verifikasi(
                    poId = po.id,
                    items = s.item.map { baris ->
                        val isi = s.isianUntuk(baris.id)
                        PenerimaanPoRepository.Verifikasi(
                            itemId = baris.id,
                            bahanBakuId = baris.bahanBakuId,
                            qtyDatang = isi.qtyDatang.toDoubleOrNull() ?: 0.0,
                            qtyTerimaSebelumnya = baris.qtyTerimaSebelumnya,
                            hargaTerima = isi.hargaTerima.toDoubleOrNull() ?: baris.hargaPesan,
                            kondisi = isi.kondisi,
                            catatan = isi.catatan,
                        )
                    },
                )
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    mengirim = false,
                    poDibuka = null,
                    item = emptyList(),
                    isian = emptyMap(),
                    pesan = "PO ${po.nomorPo} diverifikasi. Stok gudang sudah bertambah.",
                )
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PenerimaanPoVM", "kirim() gagal", e)
                _state.value = _state.value.copy(
                    mengirim = false,
                    error = e.message?.takeIf { it.isNotBlank() && e is IllegalStateException }
                        ?: stokErrorMessage(e),
                )
            }
        }
    }

    private fun angkaRapi(nilai: Double): String =
        if (nilai % 1.0 == 0.0) nilai.toLong().toString() else nilai.toString()
}
