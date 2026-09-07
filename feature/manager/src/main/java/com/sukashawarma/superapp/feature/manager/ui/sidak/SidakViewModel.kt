package com.sukashawarma.superapp.feature.manager.ui.sidak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.manager.data.DataSidak
import com.sukashawarma.superapp.feature.manager.data.SidakRepository
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.domain.HasilSidak
import com.sukashawarma.superapp.feature.manager.domain.KeputusanSidak
import com.sukashawarma.superapp.feature.manager.domain.ItemSidak
import com.sukashawarma.superapp.feature.manager.domain.KeadaanSidak
import com.sukashawarma.superapp.feature.manager.domain.LaporanSidak
import com.sukashawarma.superapp.feature.manager.domain.OutletSidak
import com.sukashawarma.superapp.feature.manager.domain.RingkasanSidak
import com.sukashawarma.superapp.feature.manager.domain.bolehMenyidak
import com.sukashawarma.superapp.feature.manager.domain.halanganSimpanSidak
import com.sukashawarma.superapp.feature.manager.domain.keadaanOutlet
import com.sukashawarma.superapp.feature.manager.domain.keputusanAwal
import com.sukashawarma.superapp.feature.manager.domain.ringkasSidakBerjalan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Foto yang sedang dibuka besar. */
data class FotoSidak(val url: String?, val namaItem: String, val namaOutlet: String, val memuat: Boolean)

data class SidakUiState(
    val data: DataSidak? = null,
    val outletTerpilih: String? = null,
    val pencarian: String = "",
    /** null = semua area. */
    val section: String? = null,
    val foto: FotoSidak? = null,
    /** Keputusan per item pada sesi ini; disemai dari hasil yang sudah tersimpan. */
    val keputusan: Map<String, KeputusanSidak> = emptyMap(),
    val catatan: String = "",
    val memuat: Boolean = true,
    val menyimpan: Boolean = false,
    val galat: String? = null,
    val kabar: String? = null,
    val role: Role? = null,
) {
    val outlets: List<OutletSidak> get() = data?.outlets.orEmpty()

    val outletTerlihat: List<OutletSidak>
        get() {
            val kueri = pencarian.trim().lowercase()
            if (kueri.isEmpty()) return outlets
            return outlets.filter { "${it.nama} ${it.region.orEmpty()}".lowercase().contains(kueri) }
        }

    val laporan: LaporanSidak? get() = outletTerpilih?.let { data?.laporan?.get(it) }
    val hasil: HasilSidak? get() = laporan?.let { data?.hasil?.get(it.id) }

    /** Kemajuan dihitung dari keputusan di layar, bukan dari yang tersimpan —
     *  itulah yang sedang dikerjakan pengguna sekarang. */
    val ringkasan: RingkasanSidak get() = ringkasSidakBerjalan(laporan, keputusan)

    val bolehMenyimpan: Boolean get() = bolehMenyidak(role)
    val halangan: String? get() = halanganSimpanSidak(laporan, keputusan)
    val siapDisimpan: Boolean get() = bolehMenyimpan && halangan == null && !menyimpan

    val sectionTersedia: List<String>
        get() = laporan?.items?.map { it.section }?.distinct().orEmpty()

    val itemTerlihat: List<ItemSidak>
        get() = laporan?.items.orEmpty().filter { section == null || it.section == section }

    fun keadaan(outletId: String): KeadaanSidak {
        val laporanOutlet = data?.laporan?.get(outletId)
        return keadaanOutlet(laporanOutlet, laporanOutlet?.let { data?.hasil?.get(it.id) })
    }

    fun namaOutlet(outletId: String?): String =
        outlets.find { it.id == outletId }?.nama ?: "Outlet"
}

class SidakViewModel : ViewModel() {

    private val _state = MutableStateFlow(SidakUiState())
    val state: StateFlow<SidakUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatUlang()
    }

    fun pilihOutlet(outletId: String) {
        if (_state.value.outletTerpilih == outletId) return
        // Penyaring area ikut disetel ulang: area yang ada di outlet sebelumnya
        // belum tentu ada di outlet ini, dan daftar kosong terbaca seperti galat.
        // Keputusan juga disemai ulang dari hasil outlet BARU — membawa keputusan
        // outlet sebelumnya adalah cara tercepat menyidak cabang yang salah.
        _state.value = _state.value.copy(
            outletTerpilih = outletId,
            section = null,
            keputusan = emptyMap(),
            catatan = "",
        ).let { it.copy(keputusan = keputusanAwal(it.hasil), catatan = it.hasil?.catatan.orEmpty()) }
    }

    fun pilihKeputusan(itemId: String, keputusan: KeputusanSidak) {
        val sekarang = _state.value.keputusan
        // Menekan tombol yang sudah aktif membatalkannya, seperti `setCheck` di web.
        val berikutnya = if (sekarang[itemId] == keputusan) sekarang - itemId else sekarang + (itemId to keputusan)
        _state.value = _state.value.copy(keputusan = berikutnya)
    }

    fun ubahCatatan(teks: String) {
        _state.value = _state.value.copy(catatan = teks)
    }

    fun tutupKabar() {
        _state.value = _state.value.copy(kabar = null, galat = null)
    }

    fun simpan() {
        val awal = _state.value
        val laporan = awal.laporan ?: return
        val halangan = awal.halangan
        if (halangan != null || !awal.bolehMenyimpan) {
            _state.value = awal.copy(
                galat = halangan ?: "Peran Anda tidak berwenang menyimpan hasil sidak.",
            )
            return
        }
        if (awal.menyimpan) return

        viewModelScope.launch {
            _state.value = _state.value.copy(menyimpan = true, galat = null)
            try {
                SidakRepository.simpanSidak(laporan.id, awal.catatan, awal.keputusan)
                _state.value = _state.value.copy(menyimpan = false, kabar = "Hasil sidak tersimpan.")
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SidakViewModel", "simpan() gagal", e)
                _state.value = _state.value.copy(menyimpan = false, galat = pesanSimpanGagal(e))
            }
        }
    }

    /**
     * RPC-nya berasal dari migrasi yang mungkin belum dijalankan di database.
     * PostgREST membalas 404 untuk fungsi yang tidak ada, dan pesan mentahnya tidak
     * memberi tahu apa pun yang bisa ditindaklanjuti.
     */
    private fun pesanSimpanGagal(e: Exception): String = when {
        e is com.sukashawarma.superapp.data.remote.Postgrest.PostgrestException -> {
            val isi = e.message.orEmpty()
            when {
                e.code == 404 || "submit_sidak_inventaris" in isi ->
                    "Fitur simpan sidak belum aktif di database. Migrasi " +
                        "20300131000000_submit_sidak_inventaris_rpc perlu dijalankan lebih dulu."
                "Outlet di luar scope" in isi ->
                    "Outlet ini di luar cakupan akses Anda."
                "Akses sidak tidak tersedia" in isi ->
                    "Peran Anda tidak berwenang menyimpan hasil sidak."
                "harus diberi hasil" in isi ->
                    "Setiap item inventaris harus diberi hasil sidak."
                else -> "Gagal menyimpan hasil sidak. Coba lagi."
            }
        }
        else -> pesanGalat(e)
    }

    fun ubahPencarian(teks: String) {
        _state.value = _state.value.copy(pencarian = teks)
    }

    fun pilihSection(section: String?) {
        _state.value = _state.value.copy(section = section)
    }

    fun bukaFoto(item: ItemSidak) {
        val namaOutlet = _state.value.namaOutlet(_state.value.outletTerpilih)
        if (item.fotoPath.isBlank()) {
            _state.value = _state.value.copy(
                foto = FotoSidak(null, item.nama, namaOutlet, memuat = false),
            )
            return
        }
        _state.value = _state.value.copy(
            foto = FotoSidak(null, item.nama, namaOutlet, memuat = true),
        )
        viewModelScope.launch {
            val url = SidakRepository.urlFoto(item.fotoPath)
            // Pengguna bisa saja sudah menutup dialog sementara tanda tangan diminta;
            // jangan menghidupkannya kembali.
            if (_state.value.foto?.namaItem != item.nama) return@launch
            _state.value = _state.value.copy(
                foto = FotoSidak(url, item.nama, namaOutlet, memuat = false),
            )
        }
    }

    fun tutupFoto() {
        _state.value = _state.value.copy(foto = null)
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, galat = null)
            try {
                val data = SidakRepository.muat()
                val setelahMuat = _state.value.copy(
                    memuat = false,
                    galat = null,
                    data = data,
                    role = AppSession.staff.value?.role,
                    // Outlet pertama dipilih otomatis supaya layar tidak terbuka kosong,
                    // tapi pilihan pengguna yang sudah ada tidak ditimpa.
                    outletTerpilih = _state.value.outletTerpilih ?: data.outlets.firstOrNull()?.id,
                )
                _state.value = setelahMuat.copy(
                    keputusan = keputusanAwal(setelahMuat.hasil),
                    catatan = setelahMuat.hasil?.catatan.orEmpty(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SidakViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, galat = pesanGalat(e))
            }
        }
    }

    private fun pesanGalat(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat). Coba lagi."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        else -> "Gagal memuat data sidak inventaris. Coba lagi."
    }
}
