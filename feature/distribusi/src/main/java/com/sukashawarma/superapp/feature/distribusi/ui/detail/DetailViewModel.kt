package com.sukashawarma.superapp.feature.distribusi.ui.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.FotoBuktiStore
import com.sukashawarma.superapp.feature.distribusi.data.PengirimanRepository
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.domain.AlokasiVendor
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.PengirimanPusat
import com.sukashawarma.superapp.feature.distribusi.domain.SaldoVendor
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDitutup
import com.sukashawarma.superapp.feature.distribusi.domain.SatuanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import com.sukashawarma.superapp.feature.distribusi.ui.ttd.tandaTanganTerlaluBesar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Satu baris item, sudah dalam satuan distribusi dan siap dirender. */
data class BarisItemDetail(
    val itemId: String,
    val bahanId: String,
    val nama: String,
    val qtyDikirim: Long,
    val qtyTerima: Long?,
    val satuan: String,
    val kondisi: String?,
    val catatan: String?,
    val fotoPath: String?,
    val bermasalah: Boolean,
    val vendorId: String? = null,
    val vendorNama: String? = null,
)

data class DetailUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val detail: SuratJalanDetail? = null,
    val baris: List<BarisItemDetail> = emptyList(),
    val bolehLihatKode: Boolean = false,
    val foto: Map<String, Bitmap> = emptyMap(),
    /** Kitchen: tanda tangan, kirim, dan ganti vendor pada draft. */
    val bolehKelolaKirim: Boolean = false,
    val bolehBatalkan: Boolean = false,
    val bolehTutup: Boolean = false,
    /** Saldo vendor per bahan dalam SATUAN DISTRIBUSI, hanya dimuat untuk draft. */
    val saldoVendor: Map<String, List<SaldoVendor>> = emptyMap(),
    /** Saldo vendor draft sudah termuat. Tanpanya item multi-vendor tak terdeteksi
     *  dan Kirim tampak siap padahal penjaga DB akan menolaknya. */
    val saldoTermuat: Boolean = false,
    val namaSemuaStaf: List<String> = emptyList(),
    val namaStafKitchen: List<String> = emptyList(),
    /** Aksi yang sedang berjalan ("ttd", "kirim", "batal", "tutup", "vendor:<itemId>"). */
    val memproses: String? = null,
    val pesan: String? = null,
    val galatAksi: String? = null,
    /** true setelah terkirim/dibatalkan — layar kembali ke daftar. */
    val selesaiAksi: Boolean = false,
) {
    val status: StatusSuratJalan? get() = detail?.status
    val draft: Boolean get() = status == StatusSuratJalan.DRAFT
    val peranTtd: List<String> get() = detail?.ttdPengirim?.map { it.peran }.orEmpty()

    /** Item multi-vendor yang belum punya vendor — `unassignedMultiVendorItems` web. */
    val vendorBelumDipilih: List<BarisItemDetail>
        get() = baris.filter { it.vendorId == null && AlokasiVendor.multiVendor(saldoVendor[it.bahanId].orEmpty()) }

    val alasanTidakBisaKirim: String?
        get() = if (!saldoTermuat) "Memuat saldo vendor Gudang Pusat…"
        else PengirimanPusat.alasanTidakBisaKirim(vendorBelumDipilih.size, peranTtd)
}

class DetailViewModel(private val suratJalanId: String) : ViewModel() {

    class Factory(private val suratJalanId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(suratJalanId) as T
    }

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    init {
        val role = AppSession.staff.value?.role
        _state.value = _state.value.copy(
            bolehKelolaKirim = DistribusiAkses.bolehTerbitkan(role),
            bolehBatalkan = DistribusiAkses.bolehBatalkanDraft(role),
            bolehTutup = DistribusiAkses.bolehTutupDokumen(role),
        )
        muat()
    }

    /**
     * Nomor pemuatan terakhir. `muat()` dipanggil bersamaan dari realtime, dari
     * layar yang aktif kembali, dan setelah setiap aksi; balasan yang tiba belakangan
     * tapi dimulai lebih dulu tidak boleh menimpa data yang lebih baru (mis. TTD
     * yang baru tersimpan hilang lagi dari layar).
     */
    private var generasi = 0

    fun muat() {
        val gen = ++generasi
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val detail = SuratJalanRepository.detail(suratJalanId)
                if (gen != generasi) return@launch
                if (detail == null) {
                    _state.value = _state.value.copy(
                        memuat = false,
                        error = "Surat jalan tidak ditemukan.",
                    )
                    return@launch
                }
                _state.value = _state.value.copy(
                    memuat = false,
                    detail = detail,
                    bolehLihatKode = DistribusiAkses.bolehLihatKodeVerifikasi(
                        AppSession.staff.value?.role
                    ),
                    baris = detail.items.map { item ->
                        val meta = item.bahan
                        val kurang = item.qtyTerima != null && item.qtyTerima < item.qtyDikirim
                        BarisItemDetail(
                            itemId = item.id,
                            bahanId = item.bahanBakuId,
                            vendorId = item.vendorId,
                            vendorNama = item.vendorNama,
                            nama = meta?.nama ?: "Bahan tidak dikenal",
                            qtyDikirim = if (meta == null) Math.round(item.qtyDikirim)
                            else SatuanDistribusi.keTampilan(item.qtyDikirim, meta),
                            qtyTerima = item.qtyTerima?.let {
                                if (meta == null) Math.round(it)
                                else SatuanDistribusi.keTampilan(it, meta)
                            },
                            satuan = meta?.let { SatuanDistribusi.satuanTampil(it) } ?: "unit",
                            kondisi = item.kondisi,
                            catatan = item.catatan,
                            fotoPath = item.fotoPath,
                            bermasalah = item.kondisi == "rusak" || kurang,
                        )
                    },
                )
                if (detail.status == StatusSuratJalan.DRAFT && _state.value.bolehKelolaKirim) muatDataDraft(detail, gen)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen != generasi) return@launch
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    /** Saldo vendor dan nama penanda tangan hanya relevan selama dokumen masih draft. */
    private suspend fun muatDataDraft(detail: SuratJalanDetail, gen: Int) {
        try {
            val mentah = PengirimanRepository.saldoVendor(detail.items.map { it.bahanBakuId }.toSet())
            if (gen != generasi) return
            val meta = detail.items.mapNotNull { it.bahan }.associateBy { it.id }
            val dist = mentah.mapValues { (bahanId, daftar) ->
                val f = meta[bahanId]?.let { SatuanDistribusi.faktor(it) } ?: 1.0
                daftar.map { AlokasiVendor.keSatuanDistribusi(it, f) }
            }
            _state.value = _state.value.copy(saldoVendor = dist, saldoTermuat = true)
        } catch (e: Exception) {
            _state.value = _state.value.copy(galatAksi = "Saldo vendor gagal dimuat: ${distribusiErrorMessage(e)}")
        }
        if (_state.value.namaSemuaStaf.isEmpty()) {
            try {
                val (semua, kitchen) = PengirimanRepository.namaStaf()
                _state.value = _state.value.copy(namaSemuaStaf = semua, namaStafKitchen = kitchen)
            } catch (e: Exception) {
                // Nama tetap bisa diketik manual; daftar hanya membantu.
            }
        }
    }

    private fun jalankan(aksi: String, blok: suspend () -> Unit) {
        if (_state.value.memproses != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(memproses = aksi, galatAksi = null)
            try {
                blok()
            } catch (e: Exception) {
                _state.value = _state.value.copy(galatAksi = pesanGalat(aksi, e))
            } finally {
                _state.value = _state.value.copy(memproses = null)
            }
        }
    }

    private fun pesanGalat(aksi: String, e: Exception): String {
        val dasar = (e as? PengirimanRepository.GagalBuat)?.message ?: distribusiErrorMessage(e)
        return when (aksi) {
            "kirim" -> "Gagal Mengirim: ${PengirimanPusat.rapikanPesanKirim(dasar)}"
            "ttd" -> "Gagal menyimpan tanda tangan: $dasar"
            else -> dasar
        }
    }

    fun ubahVendor(itemId: String, vendorId: String) = jalankan("vendor:$itemId") {
        PengirimanRepository.ubahVendorItem(itemId, vendorId)
        _state.value = _state.value.copy(pesan = "Vendor berhasil diperbarui")
        muat()
    }

    fun tandaTangan(peran: String, nama: String, gambar: String) {
        val bersih = nama.trim()
        when {
            _state.value.memproses != null -> { beriGalat("Tunggu, proses sebelumnya belum selesai."); return }
            bersih.isEmpty() -> { beriGalat("Pilih atau ketik nama penanda tangan terlebih dahulu"); return }
            _state.value.peranTtd.contains(peran) -> { beriGalat("$peran sudah menandatangani."); return }
            tandaTanganTerlaluBesar(gambar) -> {
                beriGalat("Ukuran tanda tangan terlalu besar (${gambar.length / 1024}KB). Coba ulangi.")
                return
            }
        }
        jalankan("ttd") {
            val daftar = PengirimanRepository.tandaTanganPengirim(suratJalanId, bersih, peran, gambar)
            val detail = _state.value.detail
            _state.value = _state.value.copy(
                detail = detail?.copy(ttdPengirim = daftar),
                pesan = "Tanda tangan $bersih ($peran) berhasil ditambahkan!",
            )
            // Menaikkan generasi: pemuatan yang dimulai sebelum TTD ini tersimpan
            // tidak boleh mengembalikan daftar tanda tangan lama ke layar.
            muat()
        }
    }

    fun kirim() {
        _state.value.alasanTidakBisaKirim?.let { beriGalat(it); return }
        jalankan("kirim") {
            PengirimanRepository.kirim(suratJalanId)
            _state.value = _state.value.copy(
                pesan = "Surat Jalan berhasil dikirim! Status sekarang: Dalam Transit.",
                selesaiAksi = true,
            )
        }
    }

    fun batalkan(alasan: String) = jalankan("batal") {
        val hasil = PengirimanRepository.batalkanDraft(suratJalanId, alasan)
        if (!hasil.sukses) {
            beriGalat(hasil.pesan.ifBlank { "Gagal membatalkan dokumen." })
            return@jalankan
        }
        _state.value = _state.value.copy(
            pesan = hasil.pesan.ifBlank { "PO / Surat Jalan draft berhasil dibatalkan" },
            selesaiAksi = true,
        )
    }

    /** Verifikasi akhir pusat: diterima_* -> selesai. */
    fun tutupDokumen() {
        if (!_state.value.bolehTutup || _state.value.status?.bolehDitutup != true) return
        jalankan("tutup") {
            val tertutup = SuratJalanRepository.tutupDokumen(suratJalanId)
            _state.value = _state.value.copy(
                pesan = if (tertutup) "Verifikasi akhir tersimpan. Dokumen ditutup."
                else "Dokumen tidak ditutup: statusnya sudah berubah. Layar dimuat ulang.",
            )
            muat()
        }
    }

    private fun beriGalat(pesan: String) { _state.value = _state.value.copy(galatAksi = pesan) }

    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, galatAksi = null) }

    /**
     * Foto diambil sesuai permintaan, bukan sekaligus saat layar dibuka: satu
     * surat jalan bisa memuat belasan foto, dan menariknya semua di jaringan
     * outlet akan membuat layar terasa macet.
     *
     * Kegagalan satu foto sengaja diabaikan diam-diam — foto yang hilang tidak
     * boleh menutup akses ke sisa dokumen.
     */
    fun muatFoto(path: String) {
        if (_state.value.foto.containsKey(path)) return
        viewModelScope.launch {
            try {
                val bytes = FotoBuktiStore.ambil(path) ?: return@launch
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch
                _state.value = _state.value.copy(foto = _state.value.foto + (path to bitmap))
            } catch (e: Exception) {
                // diabaikan dengan sengaja, lihat komentar di atas
            }
        }
    }
}
