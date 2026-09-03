package com.sukashawarma.superapp.feature.stok.ui.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.KategoriStok
import com.sukashawarma.superapp.feature.stok.domain.ProduksiEstimator
import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.domain.bolehTampilDiOutlet
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor

/** Pengurutan dalam tiap kategori — cermin `SortBy` di `CrewList.tsx`. */
enum class UrutanStok(val label: String) {
    NAMA("Sort: Nama"),
    STATUS("Sort: Status"),
}

/** Filter yang dinyalakan lewat kartu ringkasan di atas. */
enum class FilterKpi { SEMUA, KRITIS, SELISIH }

data class MonitoringUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val tidakBerhak: Boolean = false,
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val semua: List<MonitoringRow> = emptyList(),
    val cari: String = "",
    val urutan: UrutanStok = UrutanStok.NAMA,
    val filter: FilterKpi = FilterKpi.SEMUA,
    val porsiPerBahan: Map<String, Int> = emptyMap(),
) {
    val tampilkanPemilihOutlet: Boolean get() = outlets.size > 1

    fun status(row: MonitoringRow): StokStatus = row.status(
        porsiTersisa = porsiPerBahan[row.bahanBakuId],
        marqueeWarning = outletTerpilih?.marqueeWarningThreshold ?: UnitScale.DEFAULT_MARQUEE_WARNING,
    )

    /**
     * Hitungan ringkasan dihitung atas SELURUH isi outlet, bukan hasil penyaringan —
     * kalau ikut tersaring, menekan kartu "Kritis" akan mengubah angkanya sendiri.
     */
    val jumlahKritis: Int get() = semua.count { status(it) == StokStatus.BELOW }
    val jumlahSelisih: Int get() = semua.count { it.isFlagged }
    val jumlahAman: Int get() = semua.count { status(it) == StokStatus.OK && !it.isFlagged }

    /** Isi daftar setelah dicari, disaring, diurutkan, lalu dikelompokkan per kategori. */
    val perKategori: List<Pair<KategoriStok, List<MonitoringRow>>>
        get() {
            val kata = cari.trim().lowercase()
            val tersaring = semua.filter { row ->
                val cocokFilter = when (filter) {
                    FilterKpi.SEMUA -> true
                    FilterKpi.KRITIS -> status(row) == StokStatus.BELOW
                    FilterKpi.SELISIH -> row.isFlagged
                }
                val cocokCari = kata.isEmpty() || row.itemName.lowercase().contains(kata)
                cocokFilter && cocokCari
            }
            val pembanding = compareBy<MonitoringRow>(
                { if (urutan == UrutanStok.STATUS) urutanStatus(status(it)) else 0 },
                { it.itemName.lowercase() },
            )
            return KategoriStok.entries.mapNotNull { kategori ->
                val isi = tersaring
                    .filter { KategoriStok.dari(it.kategori) == kategori }
                    .sortedWith(pembanding)
                if (isi.isEmpty()) null else kategori to isi
            }
        }

    val kosongSetelahDisaring: Boolean get() = perKategori.isEmpty()

    private fun urutanStatus(status: StokStatus): Int = when (status) {
        StokStatus.BELOW -> 0
        StokStatus.WARNING -> 1
        StokStatus.UNKNOWN -> 2
        StokStatus.OK -> 3
    }
}

class MonitoringViewModel : ViewModel() {

    private val _state = MutableStateFlow(MonitoringUiState())
    val state: StateFlow<MonitoringUiState> = _state

    init { muatAwal() }

    fun muatAwal() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null, tidakBerhak = false)
            try {
                val outlets = StokRepository.accessibleOutlets()
                if (outlets.isEmpty()) {
                    // Daftar kosong berarti benar-benar tidak ada akses. Tidak pernah
                    // ditafsirkan sebagai "berarti semua outlet".
                    _state.value = _state.value.copy(memuat = false, tidakBerhak = true, outlets = emptyList())
                    return@launch
                }
                val terpilih = _state.value.outletTerpilih?.let { lama ->
                    outlets.firstOrNull { it.id == lama.id }
                } ?: outlets.first()
                _state.value = _state.value.copy(outlets = outlets, outletTerpilih = terpilih)
                muatBahan()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(outletTerpilih = outlet, porsiPerBahan = emptyMap(), semua = emptyList())
        viewModelScope.launch { muatBahan() }
    }

    fun ubahCari(teks: String) {
        // Tidak perlu debounce: penyaringan berjalan di memori, tanpa menyentuh jaringan.
        _state.value = _state.value.copy(cari = teks)
    }

    fun ubahUrutan(urutan: UrutanStok) {
        _state.value = _state.value.copy(urutan = urutan)
    }

    fun tekanKartu(filter: FilterKpi) {
        val sekarang = _state.value.filter
        _state.value = _state.value.copy(filter = if (sekarang == filter) FilterKpi.SEMUA else filter)
    }

    fun segarkan() {
        StokRepository.invalidate()
        muatAwal()
    }

    private suspend fun muatBahan() {
        val outlet = _state.value.outletTerpilih ?: return
        _state.value = _state.value.copy(memuat = true, error = null)
        try {
            val baris = StokRepository.monitoringOutlet(outlet.id)
                // Bahan milik gudang pusat disembunyikan dari outlet biasa, sama seperti web.
                .filter { bolehTampilDiOutlet(it.itemName, it.outletName) }
            _state.value = _state.value.copy(memuat = false, semua = baris)
            hitungPorsiLatar()
        } catch (e: Exception) {
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    /**
     * Porsi per bahan dihitung setelah daftar tampil, supaya layar tidak menunggu query
     * resep. Angkanya menyempurnakan status; selama belum siap, status tetap benar
     * berdasarkan perbandingan saldo terhadap threshold.
     */
    private fun hitungPorsiLatar() {
        val outletId = _state.value.outletTerpilih?.id ?: return
        viewModelScope.launch {
            try {
                val resep = StokRepository.resep(outletId)
                val saldo = _state.value.semua.associate { it.bahanBakuId to it.saldoNorm }
                val porsi = withContext(Dispatchers.Default) {
                    val hasil = HashMap<String, Int>()
                    for (r in ProduksiEstimator.pilihResepBerlaku(resep)) {
                        for (item in r.items) {
                            val s = saldo[item.bahanBakuId] ?: continue
                            val kebutuhan = item.kebutuhanSmallest ?: continue
                            val p = floor(s / kebutuhan).toInt().coerceAtLeast(0)
                            hasil[item.bahanBakuId] = minOf(hasil[item.bahanBakuId] ?: Int.MAX_VALUE, p)
                        }
                    }
                    hasil
                }
                _state.value = _state.value.copy(porsiPerBahan = porsi)
            } catch (_: Exception) {
                // Gagal memuat resep tidak boleh menjatuhkan layar monitoring; status
                // tetap dihitung dari saldo terhadap threshold saja.
            }
        }
    }
}
