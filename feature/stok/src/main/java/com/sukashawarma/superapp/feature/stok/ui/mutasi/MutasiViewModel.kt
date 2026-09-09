package com.sukashawarma.superapp.feature.stok.ui.mutasi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.MutasiRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.Mutasi
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.data.model.StatusMutasi
import com.sukashawarma.superapp.feature.stok.domain.bolehTampilDiOutlet
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Pilihan semu "Semua Outlet" — cermin `queryOutletId = undefined` di
 * `app/stok/mutasi/page.tsx` web, yang membuat `fetchMutasiList` mengembalikan
 * seluruh mutasi yang terlihat RLS.
 *
 * Tanpa ini, peran pusat yang tidak memegang outlet (admin, owner, kitchen — 
 * `outlet_id`-nya null) tidak punya cara melihat antrean persetujuan lintas outlet:
 * layar memaksa satu outlet, sementara lencana di bilah bawah menghitung semuanya.
 * Angka lencana lalu menjanjikan pekerjaan yang halamannya sendiri tidak bisa
 * tunjukkan — persis keluhan yang memunculkan perbaikan ini.
 *
 * `id` kosong dipakai sebagai penanda karena tidak ada outlet asli yang ber-id kosong.
 */
val SEMUA_OUTLET = OutletRingkas(id = "", name = "Semua Outlet")

/** Cakupan ini menampilkan lintas outlet, jadi tidak punya outlet asal yang konkret. */
val OutletRingkas?.adalahSemuaOutlet: Boolean get() = this != null && id.isEmpty()

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

    /**
     * Bahan yang jumlahnya melampaui sisa stok outlet asal.
     *
     * Dihitung di sini supaya barisnya bisa ditandai saat diketik. Sebelumnya
     * kelebihan baru ketahuan setelah `ajukan_mutasi` ditolak database, jadi
     * pengguna sudah menekan kirim dan menunggu sia-sia dulu.
     */
    val melebihiSisa: Set<String>
        get() = bahan.mapNotNull { b ->
            val q = jumlah[b.bahanBakuId]?.toDoubleOrNull() ?: return@mapNotNull null
            if (q > b.sisa) b.bahanBakuId else null
        }.toSet()

    /** Alasan pengajuan belum boleh dikirim, atau null bila sudah siap. */
    val halanganAjukan: String?
        get() = when {
            // Mutasi selalu berangkat dari satu outlet tertentu; cakupan "Semua Outlet"
            // hanya untuk melihat, bukan untuk mengajukan.
            outletTerpilih.adalahSemuaOutlet ->
                "Pilih outlet asal dulu — pengajuan tidak bisa dari cakupan Semua Outlet."
            tujuanTerpilih == null -> "Pilih outlet tujuan lebih dulu."
            tujuanTerpilih.id == outletTerpilih?.id -> "Outlet tujuan harus berbeda dari outlet asal."
            itemDiajukan.isEmpty() -> "Isi jumlah minimal satu bahan."
            melebihiSisa.isNotEmpty() -> "Ada jumlah yang melebihi sisa stok."
            else -> null
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
                // Peran pusat tidak memegang outlet, jadi outlet mana pun yang "pertama"
                // sama sewenang-wenangnya. Mereka dibuka pada cakupan Semua Outlet supaya
                // apa yang dihitung lencana benar-benar terlihat di daftar.
                val outletSendiri = AppSession.staff.value?.outletId
                val pilihanTersedia = daftarPilihan(outlets)
                val terpilih = _state.value.outletTerpilih?.let { lama ->
                    pilihanTersedia.firstOrNull { it.id == lama.id }
                }
                    ?: outletSendiri?.let { id -> outlets.firstOrNull { it.id == id } }
                    ?: if (pilihanTersedia.size > outlets.size) SEMUA_OUTLET else outlets.first()
                _state.value = _state.value.copy(outlets = pilihanTersedia, outletTerpilih = terpilih)
                muatDaftar()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    /**
     * Isi pemilih outlet. "Semua Outlet" hanya ditawarkan bila memang ada lebih dari
     * satu outlet untuk digabung — menawarkannya kepada kru satu outlet cuma menambah
     * pilihan yang tidak mengubah apa pun.
     */
    private fun daftarPilihan(outlets: List<OutletRingkas>): List<OutletRingkas> =
        if (outlets.size > 1) listOf(SEMUA_OUTLET) + outlets else outlets

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(outletTerpilih = outlet, daftar = emptyList())
        viewModelScope.launch { muatDaftar() }
    }

    private suspend fun muatDaftar() {
        val outlet = _state.value.outletTerpilih ?: return
        // id kosong -> tanpa penyaring outlet, yang oleh repository diartikan
        // "seluruh mutasi yang terlihat RLS".
        val lingkup = outlet.id.ifEmpty { null }
        _state.value = _state.value.copy(memuat = true, error = null)
        try {
            _state.value = _state.value.copy(memuat = false, daftar = MutasiRepository.daftar(lingkup))
        } catch (e: Exception) {
            android.util.Log.e("MutasiViewModel", "muatDaftar() gagal, lingkup=$lingkup", e)
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    // ------------------------------------------------------------- pengajuan

    fun bukaForm() {
        val outlet = _state.value.outletTerpilih ?: return
        // Ditolak dengan alasan, bukan `return` diam-diam: tombolnya terlihat, dan
        // tombol yang ditekan tanpa terjadi apa-apa terbaca sebagai aplikasi rusak.
        if (outlet.adalahSemuaOutlet) {
            _state.value = _state.value.copy(
                pesan = "Pilih satu outlet asal dulu sebelum mengajukan mutasi.",
            )
            return
        }
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

    /**
     * Isi jumlah sebanyak seluruh sisa. Mengosongkan satu bahan dari outlet adalah
     * alasan mutasi yang paling sering, dan mengetik ulang angka sisa yang panjang
     * (mis. 7055) rawan salah ketik.
     */
    fun isiSeluruhSisa(bahanBakuId: String) {
        val b = _state.value.bahan.firstOrNull { it.bahanBakuId == bahanBakuId } ?: return
        if (b.sisa <= 0) return
        _state.value = _state.value.copy(
            jumlah = _state.value.jumlah + (bahanBakuId to formatAngkaStok(b.sisa)),
        )
    }

    fun hapusJumlah(bahanBakuId: String) {
        _state.value = _state.value.copy(jumlah = _state.value.jumlah - bahanBakuId)
    }

    fun ajukan() {
        val asal = _state.value.outletTerpilih?.takeUnless { it.adalahSemuaOutlet } ?: return
        // Semua syaratnya — termasuk outlet tujuan wajib berbeda dari asal, yang juga
        // dijaga database — terkumpul di halanganAjukan supaya layar bisa menampilkan
        // alasan yang sama persis di bawah tombol sebelum ditekan.
        val halangan = _state.value.halanganAjukan
        if (halangan != null) {
            _state.value = _state.value.copy(pesan = halangan)
            return
        }
        val tujuan = _state.value.tujuanTerpilih!!
        val items = _state.value.itemDiajukan
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
