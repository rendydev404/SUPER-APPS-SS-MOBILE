package com.sukashawarma.superapp.feature.manager.ui.inventaris

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.core.camera.keJpeg
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.data.InventarisRepository
import com.sukashawarma.superapp.feature.manager.data.LaporanTerkini
import com.sukashawarma.superapp.feature.manager.data.OutletPilihan
import com.sukashawarma.superapp.feature.manager.domain.IsianItem
import com.sukashawarma.superapp.feature.manager.domain.ItemMaster
import com.sukashawarma.superapp.feature.manager.domain.KondisiAset
import com.sukashawarma.superapp.feature.manager.domain.halanganKirim
import com.sukashawarma.superapp.feature.manager.domain.kelompokPerSubsection
import com.sukashawarma.superapp.feature.manager.domain.kemajuanIsian
import com.sukashawarma.superapp.feature.manager.domain.mengisiInventaris
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class InventarisUiState(
    val outlets: List<OutletPilihan> = emptyList(),
    val master: List<ItemMaster> = emptyList(),
    val sudahTersimpan: Set<String> = emptySet(),
    /** null = sedang di daftar outlet; terisi = sedang mengisi form outlet itu. */
    val outletDiisi: String? = null,
    val laporanLama: LaporanTerkini? = null,
    val isian: Map<String, IsianItem> = emptyMap(),
    val catatan: String = "",
    val langkah: Int = 0,
    val pencarian: String = "",
    /** id item yang fotonya sedang diunggah. */
    val mengunggah: Set<String> = emptySet(),
    /** id item yang lembar kameranya sedang terbuka. */
    val kameraUntuk: String? = null,
    val memuat: Boolean = true,
    val memuatForm: Boolean = false,
    val mengirim: Boolean = false,
    val berhasilDikirim: Boolean = false,
    val galat: String? = null,
    val kabar: String? = null,
    val role: Role? = null,
) {
    val bolehMengisi: Boolean get() = mengisiInventaris(role)

    val kelompok: List<Pair<String, List<ItemMaster>>> get() = kelompokPerSubsection(master)

    val namaOutletDiisi: String
        get() = outlets.find { it.id == outletDiisi }?.nama ?: "Outlet"

    val kemajuan: Int get() = kemajuanIsian(master, isian)

    val halangan: String? get() = halanganKirim(master, isian)

    val siapKirim: Boolean get() = halangan == null && !mengirim && mengunggah.isEmpty()

    /**
     * Item yang tampil. Saat mencari, seluruh langkah ikut disisir — kalau tidak,
     * mencari "freezer" hanya menemukannya bila kebetulan sedang di area yang benar.
     */
    val kelompokTerlihat: List<Pair<String, List<ItemMaster>>>
        get() {
            val kueri = pencarian.trim().lowercase()
            if (kueri.isEmpty()) return kelompok.getOrNull(langkah)?.let { listOf(it) }.orEmpty()
            return kelompok.mapNotNull { (judul, isi) ->
                val cocok = isi.filter { "${it.nama} ${it.subsection}".lowercase().contains(kueri) }
                if (cocok.isEmpty()) null else judul to cocok
            }
        }

    fun isianUntuk(itemId: String): IsianItem = isian[itemId] ?: IsianItem()
}

class InventarisViewModel : ViewModel() {

    private val _state = MutableStateFlow(InventarisUiState(role = AppSession.staff.value?.role))
    val state: StateFlow<InventarisUiState> = _state

    private var pemuatan: Job? = null
    private var jobPemuatanForm: Job? = null

    /**
     * Foto yang URL tanda tangannya sudah pernah diminta, dikunci "$itemId|$path".
     *
     * Kartu item meminta URL tiap kali masuk komposisi; tanpa catatan ini,
     * menggulir bolak-balik satu area akan memesan tanda tangan yang sama
     * berulang kali. Himpunannya thread-safe karena kartu yang berbeda bisa
     * memintanya dari coroutine yang berbeda pada saat yang sama.
     */
    private val urlDiminta: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        muatUlang()
    }

    fun muatUlang(silent: Boolean = false) {
        val sudahAdaData = _state.value.outlets.isNotEmpty()
        val senyap = silent || sudahAdaData
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            if (!senyap) {
                _state.update { it.copy(memuat = true, galat = null) }
            }
            try {
                val referensi = InventarisRepository.referensi()
                _state.update {
                    it.copy(
                        memuat = false,
                        role = AppSession.staff.value?.role,
                        outlets = referensi.outlets,
                        master = referensi.master,
                        sudahTersimpan = referensi.sudahTersimpan,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("InventarisViewModel", "muatUlang() gagal", e)
                if (!senyap) {
                    _state.update { it.copy(memuat = false, galat = pesanGalat(e)) }
                }
            } finally {
                _state.update { if (it.memuat) it.copy(memuat = false) else it }
            }
        }
    }

    /** Membuka form satu outlet, memuat laporan lamanya sebagai isian awal. */
    fun mulaiIsi(outletId: String) {
        urlDiminta.clear()
        _state.update {
            it.copy(
                outletDiisi = outletId,
                memuatForm = true,
                isian = emptyMap(),
                catatan = "",
                langkah = 0,
                pencarian = "",
                berhasilDikirim = false,
                galat = null,
            )
        }
        // Pemuatan form outlet sebelumnya dibatalkan supaya isian outlet lama tidak
        // mendarat di form outlet yang baru dibuka.
        jobPemuatanForm?.cancel()
        jobPemuatanForm = viewModelScope.launch {
            try {
                val lama = InventarisRepository.laporanTerkini(outletId)
                // Isian disemai dari laporan lama supaya pengisian berikutnya adalah
                // penyuntingan, bukan mengetik ulang 87 baris. Fotonya pun diwarisi:
                // `submit_inventaris` menerima path lama selama laporannya memang milik
                // outlet ini.
                _state.update {
                    if (it.outletDiisi != outletId) {
                        it
                    } else {
                        it.copy(
                            memuatForm = false,
                            laporanLama = lama,
                            isian = lama?.isian.orEmpty(),
                            catatan = lama?.catatan.orEmpty(),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("InventarisViewModel", "mulaiIsi() gagal", e)
                _state.update { it.copy(memuatForm = false, galat = pesanGalat(e)) }
            }
        }
    }

    fun tutupForm() {
        jobPemuatanForm?.cancel()
        urlDiminta.clear()
        _state.update {
            it.copy(
                outletDiisi = null,
                laporanLama = null,
                isian = emptyMap(),
                catatan = "",
                langkah = 0,
                pencarian = "",
                berhasilDikirim = false,
            )
        }
    }

    fun ubahIsian(itemId: String, ubah: (IsianItem) -> IsianItem) {
        _state.update { it.copy(isian = it.isian + (itemId to ubah(it.isianUntuk(itemId)))) }
    }

    fun ubahJumlah(itemId: String, teks: String) = ubahIsian(itemId) { it.copy(jumlah = teks) }
    fun ubahAda(itemId: String, ada: Boolean) = ubahIsian(itemId) { it.copy(ada = ada) }
    fun ubahKondisi(itemId: String, kondisi: KondisiAset) = ubahIsian(itemId) { it.copy(kondisi = kondisi) }
    fun ubahCatatanItem(itemId: String, teks: String) = ubahIsian(itemId) { it.copy(catatan = teks) }
    fun ubahMerek(itemId: String, teks: String) = ubahIsian(itemId) { it.copy(merek = teks) }
    fun ubahHarga(itemId: String, teks: String) = ubahIsian(itemId) { it.copy(harga = teks) }
    fun ubahTanggalBeli(itemId: String, teks: String) = ubahIsian(itemId) { it.copy(tanggalBeli = teks) }
    fun ubahDepresiasi(itemId: String, teks: String) = ubahIsian(itemId) { it.copy(depresiasi = teks) }

    fun ubahCatatan(teks: String) {
        _state.update { it.copy(catatan = teks) }
    }

    fun pilihLangkah(langkah: Int) {
        _state.update {
            val batas = it.kelompok.lastIndex.coerceAtLeast(0)
            it.copy(langkah = langkah.coerceIn(0, batas))
        }
    }

    fun ubahPencarian(teks: String) {
        _state.update { it.copy(pencarian = teks) }
    }

    fun bukaKamera(itemId: String) {
        _state.update { it.copy(kameraUntuk = itemId) }
    }

    fun tutupKamera() {
        _state.update { it.copy(kameraUntuk = null) }
    }

    fun tutupKabar() {
        _state.update { it.copy(kabar = null, galat = null) }
    }

    /**
     * Menyiapkan URL pratinjau sebuah foto, sekali per path.
     *
     * Bucket `inventaris-foto` bersifat privat, jadi path saja tidak bisa
     * ditampilkan — perlu URL bertanda tangan. Permintaannya dipicu kartu yang
     * sedang tampil, bukan sekaligus saat form dibuka: satu laporan berisi
     * puluhan foto dan sebagian besar tidak pernah terlihat.
     */
    fun pastikanUrlFoto(itemId: String) {
        val isi = _state.value.isianUntuk(itemId)
        val path = isi.fotoPath?.takeIf { it.isNotBlank() } ?: return
        if (isi.fotoUrl != null) return
        if (!urlDiminta.add("$itemId|$path")) return

        viewModelScope.launch {
            val url = try {
                InventarisRepository.urlFoto(path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("InventarisViewModel", "urlFoto() gagal", e)
                null
            }
            if (url == null) {
                // Gagal sekali tidak boleh mengunci pratinjau selamanya.
                urlDiminta.remove("$itemId|$path")
                return@launch
            }
            // Foto bisa sudah diganti selagi tanda tangan diminta; jangan
            // memasang URL milik foto yang sudah tidak dipakai.
            ubahIsian(itemId) { if (it.fotoPath == path) it.copy(fotoUrl = url) else it }
        }
    }

    /**
     * Foto diunggah SEGERA setelah dipotret, bukan ditumpuk sampai tombol kirim.
     *
     * Satu laporan berisi puluhan foto; mengunggah semuanya sekaligus di akhir
     * berarti satu kegagalan jaringan membatalkan seluruh pekerjaan pengisian.
     */
    fun simpanFoto(itemId: String, bitmap: Bitmap) {
        val outletId = _state.value.outletDiisi ?: return
        _state.update {
            it.copy(
                kameraUntuk = null,
                mengunggah = it.mengunggah + itemId,
            )
        }
        viewModelScope.launch {
            try {
                // Kompresi JPEG satu foto kamera memakan ratusan milidetik; di UI
                // thread itu terasa sebagai layar yang membeku sehabis memotret.
                val jpeg = withContext(Dispatchers.Default) { bitmap.keJpeg() }
                val path = InventarisRepository.unggahFoto(outletId, itemId, jpeg)
                ubahIsian(itemId) { it.copy(fotoPath = path, fotoUrl = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("InventarisViewModel", "simpanFoto() gagal", e)
                _state.update { it.copy(galat = "Foto gagal diunggah. Coba potret ulang.") }
            } finally {
                _state.update { it.copy(mengunggah = it.mengunggah - itemId) }
            }
        }
    }

    fun kirim() {
        val awal = _state.value
        val outletId = awal.outletDiisi ?: return
        val halangan = awal.halangan
        if (halangan != null) {
            _state.update { it.copy(galat = halangan) }
            return
        }
        if (awal.mengunggah.isNotEmpty()) {
            _state.update { it.copy(galat = "Masih ada foto yang sedang diunggah.") }
            return
        }
        // Kunci dipasang sebelum coroutine dimulai supaya ketukan ganda yang cepat
        // tidak sempat membuat dua laporan inventaris baru di database.
        var lolos = false
        _state.update {
            lolos = !it.mengirim
            if (lolos) it.copy(mengirim = true, galat = null) else it
        }
        if (!lolos) return

        viewModelScope.launch {
            try {
                InventarisRepository.kirim(
                    outletId = outletId,
                    catatan = awal.catatan,
                    master = awal.master,
                    isian = awal.isian,
                    laporanLamaId = awal.laporanLama?.id,
                )
                _state.update {
                    it.copy(
                        mengirim = false,
                        berhasilDikirim = true,
                        sudahTersimpan = it.sudahTersimpan + outletId,
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(mengirim = false) }
                throw e
            } catch (e: Exception) {
                android.util.Log.e("InventarisViewModel", "kirim() gagal", e)
                _state.update { it.copy(mengirim = false, galat = pesanKirimGagal(e)) }
            }
        }
    }

    /** Pesan dari `submit_inventaris` sudah spesifik; diteruskan apa adanya bila dikenali. */
    private fun pesanKirimGagal(e: Exception): String = when {
        e is com.sukashawarma.superapp.data.remote.Postgrest.PostgrestException -> {
            val isi = e.message.orEmpty()
            when {
                "Outlet di luar scope" in isi -> "Outlet ini di luar cakupan akses Anda."
                "Detail inventaris belum lengkap" in isi ->
                    "Jumlah item yang dikirim tidak cocok dengan master inventaris. Muat ulang lalu coba lagi."
                "Path foto" in isi -> "Ada foto yang tidak sah. Potret ulang item yang bermasalah."
                else -> "Gagal menyimpan inventaris. Coba lagi."
            }
        }
        else -> pesanGalat(e)
    }

    private fun pesanGalat(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat). Coba lagi."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        else -> "Gagal memuat data inventaris. Coba lagi."
    }
}
