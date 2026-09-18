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
import kotlinx.coroutines.flow.update
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

    /**
     * Menandai isian yang belum dikirim ke server. Selama ini menyala, pemuatan
     * ulang senyap (mis. dari event realtime) tidak boleh menimpa keputusan dan
     * catatan yang sedang diketik manager.
     */
    @Volatile
    private var adaSuntinganBelumDisimpan = false

    init {
        muatUlang()
    }

    fun pilihOutlet(outletId: String) {
        if (_state.value.outletTerpilih == outletId) return
        // Penyaring area ikut disetel ulang: area yang ada di outlet sebelumnya
        // belum tentu ada di outlet ini, dan daftar kosong terbaca seperti galat.
        // Keputusan juga disemai ulang dari hasil outlet BARU — membawa keputusan
        // outlet sebelumnya adalah cara tercepat menyidak cabang yang salah.
        adaSuntinganBelumDisimpan = false
        _state.update {
            val pindah = it.copy(
                outletTerpilih = outletId,
                section = null,
                keputusan = emptyMap(),
                catatan = "",
            )
            pindah.copy(
                keputusan = keputusanAwal(pindah.hasil),
                catatan = pindah.hasil?.catatan.orEmpty(),
            )
        }
    }

    fun pilihKeputusan(itemId: String, keputusan: KeputusanSidak) {
        adaSuntinganBelumDisimpan = true
        _state.update {
            val sekarang = it.keputusan
            // Menekan tombol yang sudah aktif membatalkannya, seperti `setCheck` di web.
            val berikutnya =
                if (sekarang[itemId] == keputusan) sekarang - itemId else sekarang + (itemId to keputusan)
            it.copy(keputusan = berikutnya)
        }
    }

    fun ubahCatatan(teks: String) {
        adaSuntinganBelumDisimpan = true
        _state.update { it.copy(catatan = teks) }
    }

    fun tutupKabar() {
        _state.update { it.copy(kabar = null, galat = null) }
    }

    fun simpan() {
        val awal = _state.value
        val laporan = awal.laporan ?: return
        val halangan = awal.halangan
        if (halangan != null || !awal.bolehMenyimpan) {
            _state.update {
                it.copy(galat = halangan ?: "Peran Anda tidak berwenang menyimpan hasil sidak.")
            }
            return
        }
        // Kunci dipasang sebelum coroutine dimulai supaya ketukan ganda yang cepat
        // tidak sempat mengirim dua kali hasil sidak yang sama.
        var lolos = false
        _state.update {
            lolos = !it.menyimpan
            if (lolos) it.copy(menyimpan = true, galat = null) else it
        }
        if (!lolos) return

        viewModelScope.launch {
            try {
                SidakRepository.simpanSidak(laporan.id, awal.catatan, awal.keputusan)
                adaSuntinganBelumDisimpan = false
                _state.update { it.copy(menyimpan = false, kabar = "Hasil sidak tersimpan.") }
                muatUlang()
            } catch (e: CancellationException) {
                _state.update { it.copy(menyimpan = false) }
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SidakViewModel", "simpan() gagal", e)
                _state.update { it.copy(menyimpan = false, galat = pesanSimpanGagal(e)) }
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
        _state.update { it.copy(pencarian = teks) }
    }

    fun pilihSection(section: String?) {
        _state.update { it.copy(section = section) }
    }

    fun bukaFoto(item: ItemSidak) {
        val namaOutlet = _state.value.namaOutlet(_state.value.outletTerpilih)
        if (item.fotoPath.isBlank()) {
            _state.update { it.copy(foto = FotoSidak(null, item.nama, namaOutlet, memuat = false)) }
            return
        }
        _state.update { it.copy(foto = FotoSidak(null, item.nama, namaOutlet, memuat = true)) }
        viewModelScope.launch {
            val url = SidakRepository.urlFoto(item.fotoPath)
            // Pengguna bisa saja sudah menutup dialog sementara tanda tangan diminta;
            // jangan menghidupkannya kembali.
            _state.update {
                if (it.foto?.namaItem != item.nama) it
                else it.copy(foto = FotoSidak(url, item.nama, namaOutlet, memuat = false))
            }
        }
    }

    fun tutupFoto() {
        _state.update { it.copy(foto = null) }
    }

    fun muatUlang(silent: Boolean = false) {
        val sudahAdaData = _state.value.data != null
        val senyap = silent || sudahAdaData
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            if (!senyap) {
                _state.update { it.copy(memuat = true, galat = null) }
            }
            try {
                val data = SidakRepository.muat()
                _state.update { lama ->
                    val setelahMuat = lama.copy(
                        memuat = false,
                        galat = null,
                        data = data,
                        role = AppSession.staff.value?.role,
                        // Outlet pertama dipilih otomatis supaya layar tidak terbuka kosong,
                        // tapi pilihan pengguna yang sudah ada tidak ditimpa.
                        outletTerpilih = lama.outletTerpilih ?: data.outlets.firstOrNull()?.id,
                    )
                    // Isian yang belum disimpan dipertahankan: menimpanya dengan data
                    // server akan menghapus penilaian yang sedang dikerjakan manager.
                    if (adaSuntinganBelumDisimpan) {
                        setelahMuat
                    } else {
                        setelahMuat.copy(
                            keputusan = keputusanAwal(setelahMuat.hasil),
                            catatan = setelahMuat.hasil?.catatan.orEmpty(),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SidakViewModel", "muatUlang() gagal", e)
                if (!senyap) {
                    _state.update { it.copy(memuat = false, galat = pesanGalat(e)) }
                }
            } finally {
                _state.update { if (it.memuat) it.copy(memuat = false) else it }
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
