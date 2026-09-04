package com.sukashawarma.superapp.feature.stok.ui.permintaan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.PermintaanRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.BahanBaku
import com.sukashawarma.superapp.feature.stok.data.model.BudgetStatus
import com.sukashawarma.superapp.feature.stok.data.model.CrosscheckSaldo
import com.sukashawarma.superapp.feature.stok.data.model.EstimasiKeranjang
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.data.model.Permintaan
import com.sukashawarma.superapp.feature.stok.data.model.SaranPermintaan
import com.sukashawarma.superapp.feature.stok.data.model.StatusPermintaan
import com.sukashawarma.superapp.feature.stok.domain.Approver
import com.sukashawarma.superapp.feature.stok.domain.DistribusiUnit
import com.sukashawarma.superapp.feature.stok.domain.KatalogPermintaan
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** Tab utama untuk role outlet — cermin `mainTab` ('buat' | 'riwayat') di web. */
enum class TabPermintaan(val label: String) { BUAT("Buat Baru"), RIWAYAT("Riwayat") }

/** Satu baris keranjang yang sudah diresolusikan ke master bahannya. */
data class BarisKeranjang(val bahan: BahanBaku, val qty: Long)

data class PermintaanUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    val tidakBerhak: Boolean = false,
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    /**
     * Role pengawas/dapur langsung masuk antrean persetujuan dan tidak membuat
     * permintaan — cermin `isKitchen` di `permintaan/page.tsx`.
     */
    val modeAntrean: Boolean = false,
    val bolehApprove: Boolean = false,
    val katalogPenuh: Boolean = false,
    // ---- data bersama
    val katalog: List<BahanBaku> = emptyList(),
    val saran: List<SaranPermintaan> = emptyList(),
    val daftarOutlet: List<Permintaan> = emptyList(),
    val daftarReview: List<Permintaan> = emptyList(),
    // ---- nilai & budget (semua dihitung di database; null/kosong = tak tersedia)
    /** Estimasi keranjang saat ini, diperbarui dengan debounce seperti web. */
    val estimasi: EstimasiKeranjang = EstimasiKeranjang(),
    /** Budget outlet terpilih (mode outlet). */
    val budget: BudgetStatus? = null,
    /** Budget per outlet untuk kartu antrean & layar persetujuan (mode pengawas). */
    val budgetPerOutlet: Map<String, BudgetStatus> = emptyMap(),
    /** permintaanId -> estimasi nilai item yang diminta, untuk badge ringkas kartu antrean. */
    val estimasiPerPermintaan: Map<String, Double> = emptyMap(),
    /** Estimasi hidup dari qty yang sedang disetujui di layar persetujuan. */
    val estimasiSetuju: EstimasiKeranjang = EstimasiKeranjang(),
    // ---- katalog (tab Buat)
    val tab: TabPermintaan = TabPermintaan.BUAT,
    val cari: String = "",
    /** "all", "kritis", atau nama kategori persis. */
    val kategoriTerpilih: String = "all",
    /** bahanBakuId -> qty pada satuan distribusi. */
    val keranjang: Map<String, Long> = emptyMap(),
    val tinjauTerbuka: Boolean = false,
    val nudgeTerbuka: Boolean = false,
    val konfirmasiTerbuka: Boolean = false,
    val mengirim: Boolean = false,
    // ---- riwayat
    val filterStatus: StatusPermintaan? = null,
    val cariRiwayat: String = "",
    // ---- persetujuan
    val approveUntuk: Permintaan? = null,
    /** bahanBakuId -> qty disetujui pada satuan distribusi. */
    val qtySetuju: Map<String, Long> = emptyMap(),
    val memuatCrosscheck: Boolean = false,
    val stokOutlet: Map<String, CrosscheckSaldo> = emptyMap(),
    val stokGudang: Map<String, CrosscheckSaldo> = emptyMap(),
    /** bahanBakuId -> kebutuhan HPP dari target penjualan, bila ada. */
    val kebutuhanTarget: Map<String, Double> = emptyMap(),
) {
    val bahanMap: Map<String, BahanBaku> get() = katalog.associateBy { it.id }

    /**
     * Bahan pada permintaan `menunggu` yang belum lewat 12 jam — disembunyikan dari
     * katalog agar tidak terduplikasi, cermin `pendingItemIds` web.
     */
    val pendingItemIds: Set<String>
        get() {
            val now = System.currentTimeMillis()
            return daftarOutlet
                .filter { it.status == StatusPermintaan.MENUNGGU }
                .filter { KatalogPermintaan.masihMenunggu(parseIsoMs(it.createdAt), now) }
                .flatMap { p -> p.items.map { it.bahanBakuId } }
                .toSet()
        }

    /** Katalog setelah aturan kategori/role — cermin `allowedBahanBaku`. */
    val katalogBoleh: List<BahanBaku>
        get() = katalog.filter { KatalogPermintaan.bolehDiminta(it.kategori, it.nama, katalogPenuh) }

    /** Bahan kritis/menipis yang boleh diminta dan belum tersembunyi pending. */
    val saranBoleh: List<SaranPermintaan>
        get() {
            val pending = pendingItemIds
            val boleh = katalogBoleh.associateBy { it.id }
            return saran.filter { it.bahanBakuId in boleh && it.bahanBakuId !in pending }
        }

    val kategoriList: List<String>
        get() = katalogBoleh.mapNotNull { it.kategori }.distinct()

    /** Katalog setelah pending-hide + filter kategori + pencarian — cermin `filteredItems`. */
    val katalogTerfilter: List<BahanBaku>
        get() {
            val pending = pendingItemIds
            val q = cari.trim().lowercase()
            val idKritis = saranBoleh.map { it.bahanBakuId }.toSet()
            return katalogBoleh.filter { b ->
                if (b.id in pending) return@filter false
                when (kategoriTerpilih) {
                    "kritis" -> if (b.id !in idKritis) return@filter false
                    "all" -> Unit
                    else -> if (b.kategori != kategoriTerpilih) return@filter false
                }
                if (q.isNotEmpty()) {
                    val cocokNama = b.nama.lowercase().contains(q)
                    val cocokKategori = b.kategori?.lowercase()?.contains(q) == true
                    if (!cocokNama && !cocokKategori) return@filter false
                }
                true
            }
        }

    val keranjangItems: List<BarisKeranjang>
        get() {
            val pending = pendingItemIds
            val peta = bahanMap
            return keranjang.entries
                .filter { it.value > 0 && it.key !in pending }
                .mapNotNull { (id, qty) -> peta[id]?.let { BarisKeranjang(it, qty) } }
        }

    val riwayatTerfilter: List<Permintaan>
        get() = daftarOutlet.filter { p ->
            if (filterStatus != null && p.status != filterStatus) return@filter false
            val q = cariRiwayat.trim().lowercase()
            if (q.isNotEmpty()) {
                val cocokKode = p.kodeReq.lowercase().contains(q)
                val cocokStaf = p.pembuatNama?.lowercase()?.contains(q) == true
                val cocokBahan = p.items.any { it.namaBahan?.lowercase()?.contains(q) == true }
                if (!cocokKode && !cocokStaf && !cocokBahan) return@filter false
            }
            true
        }

    companion object {
        fun parseIsoMs(iso: String?): Long? = try {
            java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
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
                modeAntrean = Approver.bolehReviewPermintaan(role) || Approver.bolehApprovePermintaan(role),
                bolehApprove = Approver.bolehApprovePermintaan(role),
                katalogPenuh = KatalogPermintaan.katalogPenuh(role),
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
                muatData()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(
            outletTerpilih = outlet, daftarOutlet = emptyList(),
            saran = emptyList(), keranjang = emptyMap(),
            estimasi = EstimasiKeranjang(), budget = null,
        )
        viewModelScope.launch { muatData() }
    }

    fun pilihTab(tab: TabPermintaan) {
        _state.value = _state.value.copy(tab = tab)
    }

    /**
     * Muat semua data layar sekaligus. Katalog + harga dipakai kedua mode; saran dan
     * riwayat hanya untuk mode outlet, antrean hanya untuk mode pengawas.
     */
    private suspend fun muatData() {
        val s = _state.value
        val outlet = s.outletTerpilih ?: return
        _state.value = s.copy(memuat = true, error = null)
        try {
            val katalog = PermintaanRepository.bahanBaku()
            if (s.modeAntrean) {
                val antrean = PermintaanRepository.menunggu(s.outlets.map { it.id })
                _state.value = _state.value.copy(
                    memuat = false, katalog = katalog, daftarReview = antrean,
                )
                muatBudgetAntrean(antrean, katalog.associateBy { it.id })
            } else {
                _state.value = _state.value.copy(
                    memuat = false, katalog = katalog,
                    saran = PermintaanRepository.saran(outlet.id),
                    daftarOutlet = PermintaanRepository.daftarOutlet(outlet.id),
                )
                muatBudgetOutlet(outlet.id)
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    // ------------------------------------------------------------ nilai & budget
    //
    // Budget dan estimasi adalah lapisan visual di atas data utama — web menandainya
    // "Tahap Developer (Bisa Diabaikan)". Kegagalannya (RPC belum ada, outlet tanpa
    // plafon) tidak boleh menggagalkan layar, jadi semuanya dibungkus runCatching dan
    // ketiadaan data berarti badge disembunyikan.

    private suspend fun muatBudgetOutlet(outletId: String) {
        val budget = runCatching { PermintaanRepository.budgetStatus(outletId) }.getOrNull()
        if (_state.value.outletTerpilih?.id == outletId) {
            _state.value = _state.value.copy(budget = budget)
        }
    }

    /**
     * Budget tiap outlet yang punya antrean, lalu estimasi nilai item yang diminta per
     * permintaan — hanya untuk outlet ber-plafon, sama seperti `ApprovalCardBudget` web.
     */
    private suspend fun muatBudgetAntrean(antrean: List<Permintaan>, bahanMap: Map<String, BahanBaku>) {
        // Paralel, bukan berurutan: antrean lintas outlet bisa berisi puluhan permintaan
        // dan versi berurutan membuat badge baru lengkap belasan detik setelah daftar tampil.
        val perOutlet = coroutineScope {
            antrean.map { it.outletId }.distinct()
                .map { id -> async { runCatching { PermintaanRepository.budgetStatus(id) }.getOrNull()?.let { id to it } } }
                .awaitAll()
        }.filterNotNull().toMap()
        _state.value = _state.value.copy(budgetPerOutlet = perOutlet)

        val estimasi = coroutineScope {
            antrean
                .filter { perOutlet[it.outletId]?.hasConfig == true && it.items.isNotEmpty() }
                .map { p ->
                    async {
                        // Proyeksi memakai qty yang benar-benar akan dikirim: satuan pesan
                        // dibulatkan ke atas, lalu dikembalikan ke satuan besar (skala harga).
                        val items = p.items.map { item ->
                            val bahan = bahanMap[item.bahanBakuId]
                            val dist = qtyDistribusiBulat(item.qtyDiminta, bahan)
                            val base = if (bahan != null) DistribusiUnit.keBase(dist, bahan.faktorDistribusi) else dist
                            item.bahanBakuId to base
                        }
                        runCatching { PermintaanRepository.estimasiNilai(items) }.getOrNull()
                            ?.let { p.id to it.totalNilai }
                    }
                }
                .awaitAll()
        }.filterNotNull().toMap()
        _state.value = _state.value.copy(estimasiPerPermintaan = estimasi)
    }

    /** qty satuan besar -> satuan distribusi dibulatkan ke atas, seperti tampilan web. */
    private fun qtyDistribusiBulat(qtyBase: Double, bahan: BahanBaku?): Double =
        if (bahan != null) ceil(DistribusiUnit.keDistribusi(qtyBase, bahan.faktorDistribusi))
        else qtyBase

    private var estimasiJob: Job? = null

    /** Estimasi keranjang dengan debounce 500 ms — cermin efek di PermintaanForm. */
    private fun jadwalkanEstimasi() {
        estimasiJob?.cancel()
        // Harga per satuan besar, keranjang pada satuan distribusi — wajib dikonversi
        // dulu (lihat catatan di PermintaanRepository.estimasiNilai).
        val items = _state.value.keranjangItems.map {
            it.bahan.id to DistribusiUnit.keBase(it.qty.toDouble(), it.bahan.faktorDistribusi)
        }
        if (items.isEmpty()) {
            _state.value = _state.value.copy(estimasi = EstimasiKeranjang())
            return
        }
        estimasiJob = viewModelScope.launch {
            delay(500)
            val hasil = runCatching { PermintaanRepository.estimasiNilai(items) }.getOrNull() ?: return@launch
            _state.value = _state.value.copy(estimasi = hasil)
        }
    }

    private var estimasiSetujuJob: Job? = null

    /** Estimasi hidup qty disetujui dengan debounce 400 ms — cermin efek di ApprovalModal. */
    private fun jadwalkanEstimasiSetuju() {
        estimasiSetujuJob?.cancel()
        val p = _state.value.approveUntuk ?: return
        // qtySetujuBase() sudah mengembalikan satuan besar, skala yang sama dengan harga.
        val items = p.items
            .map { it.bahanBakuId to qtySetujuBase(it.bahanBakuId) }
            .filter { it.second > 0.0 }
        if (items.isEmpty()) {
            _state.value = _state.value.copy(estimasiSetuju = EstimasiKeranjang())
            return
        }
        estimasiSetujuJob = viewModelScope.launch {
            delay(400)
            val hasil = runCatching { PermintaanRepository.estimasiNilai(items) }.getOrNull() ?: return@launch
            if (_state.value.approveUntuk?.id == p.id) {
                _state.value = _state.value.copy(estimasiSetuju = hasil)
            }
        }
    }

    // ------------------------------------------------------------------ katalog

    fun ubahCari(nilai: String) {
        _state.value = _state.value.copy(cari = nilai)
    }

    fun pilihKategori(kategori: String) {
        _state.value = _state.value.copy(kategoriTerpilih = kategori)
    }

    fun resetFilter() {
        _state.value = _state.value.copy(cari = "", kategoriTerpilih = "all")
    }

    /** Ubah qty keranjang sebesar delta; qty <= 0 menghapus baris — cermin `updateManualBahan`. */
    fun ubahKeranjang(bahanBakuId: String, delta: Long) {
        val k = _state.value.keranjang.toMutableMap()
        val baru = (k[bahanBakuId] ?: 0L) + delta
        if (baru <= 0L) k.remove(bahanBakuId) else k[bahanBakuId] = baru
        _state.value = _state.value.copy(keranjang = k)
        jadwalkanEstimasi()
    }

    fun setKeranjang(bahanBakuId: String, nilai: String) {
        if (nilai.isNotEmpty() && !nilai.matches(Regex("^\\d{1,5}$"))) return
        val k = _state.value.keranjang.toMutableMap()
        val qty = nilai.toLongOrNull() ?: 0L
        if (qty <= 0L) k.remove(bahanBakuId) else k[bahanBakuId] = qty
        _state.value = _state.value.copy(keranjang = k)
        jadwalkanEstimasi()
    }

    /** Masukkan bahan kritis dengan qty rekomendasi (kekurangan menuju threshold). */
    fun tambahKritis(s: SaranPermintaan) {
        val bahan = _state.value.bahanMap[s.bahanBakuId] ?: return
        val kurang = KatalogPermintaan.kekuranganBesar(s.threshold, s.currentQty, s.saldoIsGram, bahan.meta)
        val qty = KatalogPermintaan.saranQtyDistribusi(kurang, bahan.faktorDistribusi)
        _state.value = _state.value.copy(
            keranjang = _state.value.keranjang + (s.bahanBakuId to qty)
        )
        jadwalkanEstimasi()
    }

    /** 1 klik: semua bahan kritis yang belum ada di keranjang — cermin `addAllCriticalItems`. */
    fun tambahSemuaKritis() {
        val s = _state.value
        val k = s.keranjang.toMutableMap()
        s.saranBoleh.filter { (k[it.bahanBakuId] ?: 0L) <= 0L }.forEach { saran ->
            val bahan = s.bahanMap[saran.bahanBakuId] ?: return@forEach
            val kurang = KatalogPermintaan.kekuranganBesar(
                saran.threshold, saran.currentQty, saran.saldoIsGram, bahan.meta,
            )
            k[saran.bahanBakuId] = KatalogPermintaan.saranQtyDistribusi(kurang, bahan.faktorDistribusi)
        }
        _state.value = s.copy(keranjang = k)
        jadwalkanEstimasi()
    }

    fun bukaTinjau() { _state.value = _state.value.copy(tinjauTerbuka = true) }
    fun tutupTinjau() { _state.value = _state.value.copy(tinjauTerbuka = false) }
    fun tutupNudge() { _state.value = _state.value.copy(nudgeTerbuka = false) }
    fun tutupKonfirmasi() { _state.value = _state.value.copy(konfirmasiTerbuka = false) }

    /** Guard sebelum kirim: tawarkan menggabung bila keranjang 1 item & masih ada pending. */
    fun mulaiKirim() {
        val s = _state.value
        if (s.keranjangItems.isEmpty()) return
        if (s.keranjangItems.size == 1 && s.pendingItemIds.isNotEmpty()) {
            _state.value = s.copy(nudgeTerbuka = true)
        } else {
            _state.value = s.copy(konfirmasiTerbuka = true)
        }
    }

    fun lanjutKirimDariNudge() {
        _state.value = _state.value.copy(nudgeTerbuka = false, konfirmasiTerbuka = true)
    }

    fun tambahDuluDariNudge() {
        _state.value = _state.value.copy(nudgeTerbuka = false, tinjauTerbuka = false)
    }

    fun kirimPermintaan() {
        val s = _state.value
        val outlet = s.outletTerpilih ?: return
        val staffId = AppSession.staff.value?.id ?: return
        val items = s.keranjangItems
        if (items.isEmpty()) {
            _state.value = s.copy(konfirmasiTerbuka = false, pesan = "Tidak ada bahan baku yang perlu diminta.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true, error = null, pesan = null)
            try {
                PermintaanRepository.buat(
                    outletId = outlet.id,
                    dibuatOleh = staffId,
                    // qty tersimpan pada satuan besar; keranjang berada di satuan distribusi.
                    items = items.map { baris ->
                        PermintaanRepository.ItemDiminta(
                            baris.bahan.id,
                            DistribusiUnit.keBase(baris.qty.toDouble(), baris.bahan.faktorDistribusi),
                        )
                    },
                )
                estimasiJob?.cancel()
                _state.value = _state.value.copy(
                    mengirim = false, konfirmasiTerbuka = false, tinjauTerbuka = false,
                    keranjang = emptyMap(), estimasi = EstimasiKeranjang(), tab = TabPermintaan.RIWAYAT,
                    pesan = "Permintaan berhasil dikirim (${items.size} item bahan baku). Menunggu persetujuan.",
                )
                muatData()
            } catch (e: Exception) {
                _state.value = _state.value.copy(mengirim = false, error = stokErrorMessage(e))
            }
        }
    }

    // ------------------------------------------------------------------ riwayat

    fun ubahCariRiwayat(nilai: String) {
        _state.value = _state.value.copy(cariRiwayat = nilai)
    }

    fun pilihFilterStatus(status: StatusPermintaan?) {
        _state.value = _state.value.copy(filterStatus = status)
    }

    // -------------------------------------------------------------- persetujuan

    /**
     * Buka layar persetujuan: default qty disetujui = qty diminta pada satuan
     * distribusi (dibulatkan ke atas), lalu muat crosscheck stok outlet+gudang dan
     * kebutuhan target di belakang.
     */
    fun bukaApprove(p: Permintaan) {
        val s = _state.value
        val awal = p.items.associate { item ->
            val bahan = s.bahanMap[item.bahanBakuId]
            val qty = if (bahan != null) {
                ceil(DistribusiUnit.keDistribusi(item.qtyDiminta, bahan.faktorDistribusi)).toLong()
            } else {
                ceil(item.qtyDiminta).toLong()
            }
            item.bahanBakuId to qty
        }
        _state.value = s.copy(
            approveUntuk = p, qtySetuju = awal, memuatCrosscheck = true,
            stokOutlet = emptyMap(), stokGudang = emptyMap(), kebutuhanTarget = emptyMap(),
            estimasiSetuju = EstimasiKeranjang(),
        )
        jadwalkanEstimasiSetuju()
        viewModelScope.launch {
            // Gudang Pusat dicari dari daftar outlet accessible — approver memegang
            // semua outlet. Bila tak ketemu, kolom stok gudang tidak ditampilkan.
            val gudangId = s.outlets.firstOrNull { it.name.uppercase().contains("GUDANG PUSAT") }?.id
            val bahanIds = p.items.map { it.bahanBakuId }
            val cc = runCatching {
                PermintaanRepository.crosscheck(listOfNotNull(p.outletId, gudangId), bahanIds)
            }.getOrDefault(emptyMap())
            val kebutuhan = runCatching {
                PermintaanRepository.kebutuhanTarget(
                    p.outletId,
                    p.targetJual.mapNotNull { t -> t.resepId?.let { it to t.qty } },
                )
            }.getOrDefault(emptyMap())
            if (_state.value.approveUntuk?.id != p.id) return@launch
            _state.value = _state.value.copy(
                memuatCrosscheck = false,
                stokOutlet = cc[p.outletId].orEmpty(),
                stokGudang = gudangId?.let { cc[it] }.orEmpty(),
                kebutuhanTarget = kebutuhan,
            )
        }
    }

    fun tutupApprove() {
        estimasiSetujuJob?.cancel()
        _state.value = _state.value.copy(
            approveUntuk = null, qtySetuju = emptyMap(),
            stokOutlet = emptyMap(), stokGudang = emptyMap(), kebutuhanTarget = emptyMap(),
            estimasiSetuju = EstimasiKeranjang(),
        )
    }

    fun ubahQtySetuju(bahanBakuId: String, delta: Long) {
        val q = _state.value.qtySetuju.toMutableMap()
        q[bahanBakuId] = ((q[bahanBakuId] ?: 0L) + delta).coerceAtLeast(0L)
        _state.value = _state.value.copy(qtySetuju = q)
        jadwalkanEstimasiSetuju()
    }

    fun setQtySetuju(bahanBakuId: String, nilai: String) {
        if (nilai.isNotEmpty() && !nilai.matches(Regex("^\\d{1,5}$"))) return
        _state.value = _state.value.copy(
            qtySetuju = _state.value.qtySetuju + (bahanBakuId to (nilai.toLongOrNull() ?: 0L))
        )
        jadwalkanEstimasiSetuju()
    }

    /** Qty disetujui satu bahan pada satuan besar — untuk banding stok gudang & kirim RPC. */
    fun qtySetujuBase(bahanBakuId: String): Double {
        val s = _state.value
        val qty = (s.qtySetuju[bahanBakuId] ?: 0L).toDouble()
        val bahan = s.bahanMap[bahanBakuId] ?: return qty
        return DistribusiUnit.keBase(qty, bahan.faktorDistribusi)
    }

    /** Stok gudang satu bahan pada satuan besar, atau null bila tak termuat. */
    fun stokGudangBesar(bahanBakuId: String): Double? {
        val s = _state.value
        val cc = s.stokGudang[bahanBakuId] ?: return null
        val bahan = s.bahanMap[bahanBakuId]
        return if (bahan != null) DistribusiUnit.saldoKeBesar(cc.currentQty, cc.saldoIsGram, bahan.meta)
        else cc.currentQty
    }

    /** Ada item yang melebihi stok gudang? Peringatan visual, tidak memblokir. */
    fun adaLebihStokGudang(): Boolean {
        val p = _state.value.approveUntuk ?: return false
        return p.items.any { item ->
            val gudang = stokGudangBesar(item.bahanBakuId) ?: return@any false
            qtySetujuBase(item.bahanBakuId) > gudang
        }
    }

    fun setujui() {
        val p = _state.value.approveUntuk ?: return
        val items = p.items.map { item ->
            PermintaanRepository.ItemDisetujui(
                bahanBakuId = item.bahanBakuId,
                qtyDisetujui = qtySetujuBase(item.bahanBakuId),
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
                estimasiSetujuJob?.cancel()
                _state.value = _state.value.copy(
                    mengirim = false, approveUntuk = null, qtySetuju = emptyMap(),
                    estimasiSetuju = EstimasiKeranjang(),
                    pesan = "Permintaan disetujui dan surat jalan dibuat.",
                )
                muatData()
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
                PermintaanRepository.tolak(p.id, alasan.trim())
                estimasiSetujuJob?.cancel()
                _state.value = _state.value.copy(
                    mengirim = false, approveUntuk = null, qtySetuju = emptyMap(),
                    estimasiSetuju = EstimasiKeranjang(),
                    pesan = "Permintaan ditolak.",
                )
                muatData()
            } catch (e: Exception) {
                _state.value = _state.value.copy(mengirim = false, error = stokErrorMessage(e))
            }
        }
    }

    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }
}
