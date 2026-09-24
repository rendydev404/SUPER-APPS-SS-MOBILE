package com.sukashawarma.superapp.feature.distribusi.ui.buat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.BuildConfig
import com.sukashawarma.superapp.feature.distribusi.data.MuatanKirim
import com.sukashawarma.superapp.feature.distribusi.data.OutletTujuan
import com.sukashawarma.superapp.feature.distribusi.data.PengirimanRepository
import com.sukashawarma.superapp.feature.distribusi.domain.Alokasi
import com.sukashawarma.superapp.feature.distribusi.domain.AlokasiVendor
import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.PengirimanPusat
import com.sukashawarma.superapp.feature.distribusi.domain.SaldoVendor
import com.sukashawarma.superapp.feature.distribusi.domain.SatuanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Satu baris daftar muatan: qty dan alokasi dalam SATUAN DISTRIBUSI. */
data class BarisMuatan(
    val bahan: BahanBakuMeta,
    val qty: Double,
    val alokasi: List<Alokasi> = emptyList(),
)

data class BuatUiState(
    val bolehTerbitkan: Boolean = false,
    val memuatReferensi: Boolean = true,
    val galatReferensi: String? = null,
    val outlet: List<OutletTujuan> = emptyList(),
    val bahan: List<BahanBakuMeta> = emptyList(),
    val outletId: String? = null,
    val kategori: PengirimanPusat.Kategori = PengirimanPusat.Kategori.SEMUA,
    val cari: String = "",
    val bahanTerpilih: BahanBakuMeta? = null,
    val qtyTeks: String = "",
    val muatan: List<BarisMuatan> = emptyList(),
    /** Saldo vendor per bahan, sudah dalam SATUAN DISTRIBUSI. */
    val saldoVendor: Map<String, List<SaldoVendor>> = emptyMap(),
    /** false selama saldo vendor untuk daftar muatan saat ini belum pasti. */
    val vendorSiap: Boolean = true,
    val galatVendor: String? = null,
    val menyimpan: Boolean = false,
    val pesan: String? = null,
    val error: String? = null,
    /** Diisi setelah surat jalan dibuat — layar pindah ke detailnya. */
    val dibuatId: String? = null,
) {
    val bahanTersaring: List<BahanBakuMeta>
        get() {
            val kunci = cari.trim().lowercase()
            return bahan.filter { b ->
                (kategori == PengirimanPusat.Kategori.SEMUA || PengirimanPusat.kategori(b.kategori) == kategori) &&
                    (kunci.isEmpty() || b.nama.lowercase().contains(kunci))
            }
        }

    fun vendorUntuk(bahanId: String): List<SaldoVendor> = saldoVendor[bahanId].orEmpty()

    fun galatAlokasi(baris: BarisMuatan): String? =
        if (baris.qty <= 0.0) null
        else AlokasiVendor.validasi(baris.qty, baris.alokasi, vendorUntuk(baris.bahan.id))

    val adaGalatAlokasi: Boolean get() = muatan.any { galatAlokasi(it) != null }

    val bisaSimpan: Boolean
        get() = !menyimpan && muatan.isNotEmpty() && outletId != null && vendorSiap && !adaGalatAlokasi
}

/**
 * Form buat surat jalan — cermin `SuratJalanForm.tsx`. Pengguna bekerja dalam
 * satuan distribusi; konversi ke satuan dasar hanya terjadi saat menulis.
 */
class BuatSuratJalanViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        BuatUiState(bolehTerbitkan = DistribusiAkses.bolehTerbitkan(AppSession.staff.value?.role)),
    )
    val state: StateFlow<BuatUiState> = _state

    /** Saldo mentah (satuan besar) seperti dari RPC; dikonversi saat disalin ke state. */
    private var saldoMentah: Map<String, List<SaldoVendor>> = emptyMap()
    private var jobSaldo: Job? = null

    /** Bahan yang alokasi awalnya sudah pernah diisi. Web hanya mengisi saat
     *  alokasinya `undefined`; vendor yang sengaja dilepas pengguna tidak boleh
     *  dipilihkan lagi setiap kali bahan lain ditambahkan. */
    private val alokasiDiisi = HashSet<String>()

    init { muatReferensi() }

    fun muatReferensi() {
        if (!_state.value.bolehTerbitkan) return
        viewModelScope.launch {
            _state.value = _state.value.copy(memuatReferensi = true, galatReferensi = null)
            try {
                val outlet = PengirimanRepository.outletTujuan(sertakanOutletUji = BuildConfig.DEBUG)
                val bahan = PengirimanRepository.bahanAktif()
                _state.value = _state.value.copy(memuatReferensi = false, outlet = outlet, bahan = bahan)
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuatReferensi = false, galatReferensi = distribusiErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(id: String) { _state.value = _state.value.copy(outletId = id) }
    fun ubahKategori(k: PengirimanPusat.Kategori) { _state.value = _state.value.copy(kategori = k) }
    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }
    fun pilihBahan(b: BahanBakuMeta?) { _state.value = _state.value.copy(bahanTerpilih = b, qtyTeks = "") }

    /** Menerima koma sebagai pemisah desimal — keyboard HP Indonesia memakainya. */
    fun ubahQty(teks: String) {
        _state.value = _state.value.copy(qtyTeks = teks.replace(',', '.').filter { it.isDigit() || it == '.' })
    }

    fun tambahPreset(jumlah: Int) {
        val sekarang = _state.value.qtyTeks.toDoubleOrNull() ?: 0.0
        _state.value = _state.value.copy(qtyTeks = AlokasiVendor.angka(maxOf(0.0, sekarang + jumlah)))
    }

    fun tambahKeMuatan() {
        val s = _state.value
        val bahan = s.bahanTerpilih ?: run { beriPesan(error = "Pilih bahan baku terlebih dahulu"); return }
        val qty = s.qtyTeks.toDoubleOrNull()
        if (qty == null || qty <= 0.0) { beriPesan(error = "Kuantitas harus berupa angka lebih dari 0"); return }

        val ada = s.muatan.indexOfFirst { it.bahan.id == bahan.id }
        val baru = if (ada >= 0) {
            s.muatan.mapIndexed { i, m -> if (i == ada) sesuaikanAlokasi(m.copy(qty = m.qty + qty)) else m }
        } else {
            s.muatan + BarisMuatan(bahan, qty)
        }
        _state.value = s.copy(
            muatan = baru,
            bahanTerpilih = null,
            qtyTeks = "",
            pesan = if (ada >= 0) "Menambahkan ${AlokasiVendor.angka(qty)} ke ${bahan.nama}"
            else "${bahan.nama} ditambahkan ke daftar kirim",
        )
        if (ada < 0) muatSaldoVendor()
    }

    fun hapusMuatan(bahanId: String) {
        alokasiDiisi.remove(bahanId)
        _state.value = _state.value.copy(muatan = _state.value.muatan.filterNot { it.bahan.id == bahanId })
        muatSaldoVendor()
    }

    /** Pilih satu vendor untuk seluruh qty (mode "satu vendor saja"). */
    fun pilihVendorTunggal(bahanId: String, vendorId: String) = ubahMuatan(bahanId) {
        it.copy(alokasi = listOf(Alokasi(vendorId, it.qty)))
    }

    /** Mode pecah: centang/lepas vendor. Vendor baru mulai dengan sisa qty yang belum teralokasi. */
    fun alihVendorPecah(bahanId: String, vendorId: String) = ubahMuatan(bahanId) { m ->
        if (m.alokasi.any { it.vendorId == vendorId }) {
            m.copy(alokasi = m.alokasi.filterNot { it.vendorId == vendorId })
        } else {
            val sisa = (m.qty - m.alokasi.sumOf { it.qty }).coerceAtLeast(0.0)
            m.copy(alokasi = m.alokasi + Alokasi(vendorId, sisa))
        }
    }

    fun ubahQtyVendor(bahanId: String, vendorId: String, teks: String) = ubahMuatan(bahanId) { m ->
        val qty = teks.replace(',', '.').toDoubleOrNull() ?: 0.0
        m.copy(alokasi = m.alokasi.map { if (it.vendorId == vendorId) it.copy(qty = qty) else it })
    }

    private fun ubahMuatan(bahanId: String, ubah: (BarisMuatan) -> BarisMuatan) {
        _state.value = _state.value.copy(
            muatan = _state.value.muatan.map { if (it.bahan.id == bahanId) ubah(it) else it },
        )
    }

    /** Alokasi tunggal (belum dipecah) mengikuti qty item, seperti efek `setAlokasi` web. */
    private fun sesuaikanAlokasi(m: BarisMuatan): BarisMuatan =
        if (m.alokasi.size == 1) m.copy(alokasi = listOf(m.alokasi[0].copy(qty = m.qty))) else m

    private fun muatSaldoVendor() {
        val ids = _state.value.muatan.map { it.bahan.id }.toSet()
        jobSaldo?.cancel()
        if (ids.isEmpty()) {
            saldoMentah = emptyMap()
            _state.value = _state.value.copy(saldoVendor = emptyMap(), vendorSiap = true, galatVendor = null)
            return
        }
        // Selama saldo belum pasti, semua bahan tampak satu-vendor dan form akan
        // menulis baris tanpa vendor_id — yang ditolak penjaga saat dikirim.
        _state.value = _state.value.copy(vendorSiap = false, galatVendor = null)
        jobSaldo = viewModelScope.launch {
            try {
                saldoMentah = PengirimanRepository.saldoVendor(ids)
                terapkanSaldo()
            } catch (e: CancellationException) {
                // Job ini digantikan permintaan yang lebih baru (daftar muatan berubah).
                // Tanpa lemparan ulang, galat basinya menimpa saldo hasil job baru dan
                // mengunci tombol simpan.
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    saldoVendor = emptyMap(),
                    vendorSiap = false,
                    galatVendor = distribusiErrorMessage(e),
                )
            }
        }
    }

    private fun terapkanSaldo() {
        val s = _state.value
        val dist = s.muatan.associate { m ->
            val f = SatuanDistribusi.faktor(m.bahan)
            m.bahan.id to saldoMentah[m.bahan.id].orEmpty().map { AlokasiVendor.keSatuanDistribusi(it, f) }
        }
        val muatan = s.muatan.map { m ->
            val vendors = dist[m.bahan.id].orEmpty()
            when {
                m.bahan.id !in alokasiDiisi && vendors.size >= 2 -> {
                    alokasiDiisi.add(m.bahan.id)
                    m.copy(alokasi = AlokasiVendor.alokasiAwal(m.qty, vendors))
                }
                else -> sesuaikanAlokasi(m)
            }
        }
        _state.value = s.copy(saldoVendor = dist, muatan = muatan, vendorSiap = true, galatVendor = null)
    }

    fun muatUlangVendor() = muatSaldoVendor()

    fun simpan() {
        val s = _state.value
        // Tombol baru nonaktif setelah recompose; ketukan kedua yang masuk sebelum
        // itu tidak boleh menerbitkan surat jalan kedua.
        if (s.menyimpan) return
        when {
            s.outletId == null -> { beriPesan(error = "Silakan pilih outlet tujuan"); return }
            s.muatan.isEmpty() -> { beriPesan(error = "Tambahkan minimal 1 item barang yang akan dikirim"); return }
            !s.vendorSiap -> {
                beriPesan(
                    error = s.galatVendor?.let {
                        "Daftar vendor Gudang Pusat gagal dimuat: $it. Muat ulang sebelum membuat surat jalan."
                    } ?: "Sedang memuat daftar vendor Gudang Pusat. Tunggu sebentar lalu coba lagi.",
                )
                return
            }
            s.adaGalatAlokasi -> {
                beriPesan(error = s.muatan.firstNotNullOfOrNull { s.galatAlokasi(it) } ?: "Alokasi vendor belum lengkap")
                return
            }
        }
        // Ditandai sebelum launch, bukan di dalamnya, supaya gerbang di atas
        // langsung berlaku untuk panggilan berikutnya.
        _state.value = s.copy(menyimpan = true)
        viewModelScope.launch {
            try {
                val muatan = s.muatan.map { MuatanKirim(it.bahan, it.qty, it.alokasi) }
                val id = PengirimanRepository.buatSuratJalan(s.outletId!!, muatan) { bahanId ->
                    s.vendorUntuk(bahanId).size >= 2
                }
                _state.value = _state.value.copy(
                    menyimpan = false,
                    dibuatId = id,
                    pesan = "Surat Jalan berhasil dibuat! Lanjutkan ke penandatanganan.",
                )
            } catch (e: Exception) {
                val pesan = (e as? PengirimanRepository.GagalBuat)?.message ?: distribusiErrorMessage(e)
                _state.value = _state.value.copy(menyimpan = false, error = "Gagal menyimpan Surat Jalan: $pesan")
            }
        }
    }

    fun sudahPindah() { _state.value = _state.value.copy(dibuatId = null) }

    private fun beriPesan(error: String? = null) { _state.value = _state.value.copy(error = error) }

    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }
}
