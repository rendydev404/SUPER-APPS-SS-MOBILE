package com.sukashawarma.superapp.feature.distribusi.ui.verifikasi

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.DraftVerifikasi
import com.sukashawarma.superapp.feature.distribusi.data.FotoBuktiStore
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.VerifikasiDraftStore
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanItem
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.HasilValidasi
import com.sukashawarma.superapp.feature.distribusi.domain.IsianVerifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import com.sukashawarma.superapp.feature.distribusi.domain.SatuanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.ValidasiVerifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDiverifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import com.sukashawarma.superapp.feature.distribusi.domain.sudahDiterima
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class LangkahVerifikasi(val kunci: String) {
    KARTU("kartu"),
    RINGKASAN("ringkasan"),
    TTD("ttd"),
}

/** Item beserta qty kiriman yang sudah dikonversi ke satuan distribusi.
 *  Konversi dilakukan sekali di sini supaya tidak diulang tiap recomposition. */
data class ItemTampil(
    val item: SuratJalanItem,
    val qtyDikirimTampil: Long,
    val satuan: String,
)

data class VerifikasiUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    /** Layar menolak dibuka tanpa gerbang QR, termasuk saat dinavigasi langsung. */
    val terkunci: Boolean = false,
    val tidakBerhak: Boolean = false,
    val sudahDiverifikasi: Boolean = false,
    val detail: SuratJalanDetail? = null,
    val items: List<ItemTampil> = emptyList(),
    val isian: Map<String, IsianVerifikasi> = emptyMap(),
    val indeksItem: Int = 0,
    val langkah: LangkahVerifikasi = LangkahVerifikasi.KARTU,
    val kondisiTerkonfirmasi: Boolean = false,
    val mengunggahFoto: Boolean = false,
    val ttdPenerimaan: List<TandaTangan> = emptyList(),
    val menandatangani: Boolean = false,
    val memfinalisasi: Boolean = false,
    val selesai: Boolean = false,
    val namaCrew: String = "",
) {
    val itemAktif: ItemTampil? get() = items.getOrNull(indeksItem)
    val isianAktif: IsianVerifikasi
        get() = itemAktif?.let { isian[it.item.id] }
            ?: IsianVerifikasi(null, KondisiItem.BAIK, "", null)
    val ttdLengkap: Boolean
        get() = ttdPenerimaan.any { it.peran == SuratJalanRepository.PERAN_CREW } &&
            ttdPenerimaan.any { it.peran == SuratJalanRepository.PERAN_SUPIR }
}

class VerifikasiViewModel(private val suratJalanId: String) : ViewModel() {

    class Factory(private val suratJalanId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VerifikasiViewModel(suratJalanId) as T
    }

    private val _state = MutableStateFlow(VerifikasiUiState())
    val state: StateFlow<VerifikasiUiState> = _state

    init {
        muat()
    }

    fun muat() {
        viewModelScope.launch {
            val staff = AppSession.staff.value
            if (!DistribusiAkses.bolehVerifikasi(staff?.role)) {
                _state.value = _state.value.copy(memuat = false, tidakBerhak = true)
                return@launch
            }
            if (!VerifikasiDraftStore.sudahTerbuka(suratJalanId)) {
                _state.value = _state.value.copy(memuat = false, terkunci = true)
                return@launch
            }
            _state.value = _state.value.copy(memuat = true, error = null, namaCrew = staff?.name.orEmpty())
            try {
                val detail = SuratJalanRepository.detail(suratJalanId)
                if (detail == null) {
                    _state.value = _state.value.copy(
                        memuat = false,
                        error = "Surat jalan tidak ditemukan.",
                    )
                    return@launch
                }
                if (detail.status?.sudahDiterima == true || detail.status?.bolehDiverifikasi != true) {
                    _state.value = _state.value.copy(memuat = false, sudahDiverifikasi = true)
                    return@launch
                }

                val items = detail.items.map { item ->
                    val meta = item.bahan
                    ItemTampil(
                        item = item,
                        qtyDikirimTampil = if (meta == null) Math.round(item.qtyDikirim)
                        else SatuanDistribusi.keTampilan(item.qtyDikirim, meta),
                        satuan = meta?.let { SatuanDistribusi.satuanTampil(it) } ?: "unit",
                    )
                }

                val draft = VerifikasiDraftStore.muat(suratJalanId)
                val isian = items.associate { tampil ->
                    tampil.item.id to (
                        draft?.isian?.get(tampil.item.id)
                            ?: IsianVerifikasi(null, KondisiItem.BAIK, "", null)
                        )
                }

                _state.value = _state.value.copy(
                    memuat = false,
                    detail = detail,
                    items = items,
                    isian = isian,
                    ttdPenerimaan = detail.ttdPenerimaan,
                    indeksItem = draft?.indeksItem?.takeIf { it in items.indices } ?: 0,
                    langkah = LangkahVerifikasi.entries.find { it.kunci == draft?.langkah }
                        ?: LangkahVerifikasi.KARTU,
                    kondisiTerkonfirmasi = draft?.kondisiTerkonfirmasi ?: false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    // ------------------------------------------------------------ isian

    private fun ubahIsian(ubah: (IsianVerifikasi) -> IsianVerifikasi) {
        val item = _state.value.itemAktif ?: return
        val baru = _state.value.isian.toMutableMap()
        baru[item.item.id] = ubah(_state.value.isianAktif)
        _state.value = _state.value.copy(isian = baru, kondisiTerkonfirmasi = false)
        simpanDraft()
    }

    fun ubahQty(teks: String) = ubahIsian { it.copy(qtyTerima = teks.toDoubleOrNull()) }

    fun ubahKondisi(kondisi: KondisiItem) = ubahIsian {
        // Kembali ke "Baik" berarti tidak ada keluhan lagi, jadi catatannya ikut hilang.
        if (kondisi == KondisiItem.BAIK) it.copy(kondisi = kondisi, catatan = "")
        else it.copy(kondisi = kondisi)
    }

    fun ubahCatatan(teks: String) = ubahIsian { it.copy(catatan = teks) }

    /** Mengisi qty dengan jumlah yang dikirim — tombol "Sesuai Kirim". */
    fun samakanQty() {
        val item = _state.value.itemAktif ?: return
        ubahIsian { it.copy(qtyTerima = item.qtyDikirimTampil.toDouble(), kondisi = KondisiItem.BAIK, catatan = "") }
    }

    fun konfirmasiKondisi() {
        val item = _state.value.itemAktif ?: return
        when (val hasil = ValidasiVerifikasi.konfirmasiKondisi(_state.value.isianAktif, item.qtyDikirimTampil)) {
            is HasilValidasi.Tolak -> _state.value = _state.value.copy(error = hasil.pesan)
            HasilValidasi.Lolos -> {
                _state.value = _state.value.copy(kondisiTerkonfirmasi = true, error = null)
                simpanDraft()
            }
        }
    }

    fun unggahFoto(bitmap: Bitmap) {
        val item = _state.value.itemAktif ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(mengunggahFoto = true, error = null)
            try {
                val path = FotoBuktiStore.unggah(suratJalanId, item.item.id, bitmap)
                val baru = _state.value.isian.toMutableMap()
                baru[item.item.id] = _state.value.isianAktif.copy(fotoPath = path)
                _state.value = _state.value.copy(mengunggahFoto = false, isian = baru)
                simpanDraft()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    mengunggahFoto = false,
                    error = distribusiErrorMessage(e),
                )
            }
        }
    }

    fun lanjut() {
        when (val hasil = ValidasiVerifikasi.bolehLanjut(_state.value.isianAktif)) {
            is HasilValidasi.Tolak -> _state.value = _state.value.copy(error = hasil.pesan)
            HasilValidasi.Lolos -> {
                val s = _state.value
                _state.value = if (s.indeksItem + 1 >= s.items.size) {
                    s.copy(langkah = LangkahVerifikasi.RINGKASAN, kondisiTerkonfirmasi = false, error = null)
                } else {
                    s.copy(indeksItem = s.indeksItem + 1, kondisiTerkonfirmasi = false, error = null)
                }
                simpanDraft()
            }
        }
    }

    fun mundur() {
        val s = _state.value
        _state.value = when {
            s.langkah == LangkahVerifikasi.TTD -> s.copy(langkah = LangkahVerifikasi.RINGKASAN)
            s.langkah == LangkahVerifikasi.RINGKASAN ->
                s.copy(langkah = LangkahVerifikasi.KARTU, indeksItem = (s.items.size - 1).coerceAtLeast(0))
            s.indeksItem > 0 -> s.copy(indeksItem = s.indeksItem - 1, kondisiTerkonfirmasi = false)
            else -> s
        }
        simpanDraft()
    }

    fun keTandaTangan() {
        _state.value = _state.value.copy(langkah = LangkahVerifikasi.TTD, error = null)
        simpanDraft()
    }

    fun bersihkanPesan() {
        _state.value = _state.value.copy(error = null, pesan = null)
    }

    private fun simpanDraft() {
        val s = _state.value
        VerifikasiDraftStore.simpan(
            suratJalanId,
            DraftVerifikasi(s.isian, s.indeksItem, s.langkah.kunci, s.kondisiTerkonfirmasi),
        )
    }

    // ------------------------------------------------------------ tanda tangan

    fun tandaTangan(peran: String, nama: String, gambar: String) {
        if (nama.isBlank()) {
            _state.value = _state.value.copy(error = "Nama penanda tangan harus diisi.")
            return
        }
        if (com.sukashawarma.superapp.feature.distribusi.ui.ttd.tandaTanganTerlaluBesar(gambar)) {
            _state.value = _state.value.copy(
                error = "Tanda tangan terlalu besar. Ulangi goresan dengan lebih sederhana.",
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(menandatangani = true, error = null)
            try {
                val daftar = SuratJalanRepository.tandaTanganPenerimaan(suratJalanId, nama, peran, gambar)
                _state.value = _state.value.copy(
                    menandatangani = false,
                    // Daftar dari server jadi sumber kebenaran, bukan salinan lokal:
                    // TTD yang sudah tersimpan harus tetap terlihat setelah app ditutup.
                    ttdPenerimaan = daftar,
                    pesan = "Tanda tangan $peran tersimpan.",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    menandatangani = false,
                    error = distribusiErrorMessage(e),
                )
            }
        }
    }

    // ------------------------------------------------------------ finalisasi

    /**
     * Dua langkah, urutannya mengikat: tulis tiap item lebih dulu, baru panggil
     * RPC — RPC membaca `qty_terima`, `kondisi`, dan `flagged` yang baru ditulis
     * untuk menyusun `ledger_stok` dan menetapkan status akhir.
     *
     * Bila satu item gagal ditulis, RPC TIDAK dipanggil dan draft dipertahankan,
     * supaya crew bisa mencoba ulang tanpa mengisi dari awal.
     */
    fun finalisasi() {
        val s = _state.value
        if (!s.ttdLengkap || s.memfinalisasi) return
        viewModelScope.launch {
            _state.value = s.copy(memfinalisasi = true, error = null)
            try {
                s.items.forEach { tampil ->
                    val isian = s.isian[tampil.item.id] ?: return@forEach
                    val meta = tampil.item.bahan
                    val qtyTampil = isian.qtyTerima ?: 0.0
                    val qtyDasar = if (meta == null) qtyTampil
                    else SatuanDistribusi.keDasar(qtyTampil, meta)
                    SuratJalanRepository.simpanVerifikasiItem(
                        itemId = tampil.item.id,
                        qtyTerimaDasar = qtyDasar,
                        qtyDikirimDasar = tampil.item.qtyDikirim,
                        kondisi = isian.kondisi,
                        catatan = isian.catatan,
                        fotoPath = isian.fotoPath,
                    )
                }

                val hasil = SuratJalanRepository.finalisasi(suratJalanId)
                // `success:false` dengan pesan "sudah diverifikasi sebelumnya"
                // berarti percobaan terdahulu sampai ke server walau jaringannya
                // putus. Itu keberhasilan, bukan kegagalan.
                val sudahPernah = !hasil.sukses && hasil.pesan.contains("sudah diverifikasi", true)
                if (hasil.sukses || sudahPernah) {
                    VerifikasiDraftStore.hapus(suratJalanId)
                    _state.value = _state.value.copy(memfinalisasi = false, selesai = true)
                } else {
                    _state.value = _state.value.copy(
                        memfinalisasi = false,
                        error = hasil.pesan.ifBlank { "Finalisasi gagal. Coba lagi." },
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    memfinalisasi = false,
                    error = distribusiErrorMessage(e),
                )
            }
        }
    }
}
