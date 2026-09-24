package com.sukashawarma.superapp.feature.manager.ui.ceklist

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.core.camera.keWebp
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.pesanGalatJaringan
import com.sukashawarma.superapp.feature.manager.data.CeklistHarianRepository
import com.sukashawarma.superapp.feature.manager.data.OutletPilihan
import com.sukashawarma.superapp.feature.manager.domain.BagianBebas
import com.sukashawarma.superapp.feature.manager.domain.ButirCeklist
import com.sukashawarma.superapp.feature.manager.domain.FOTO_MAKS_PER_KATEGORI
import com.sukashawarma.superapp.feature.manager.domain.FotoCeklist
import com.sukashawarma.superapp.feature.manager.domain.IsianCeklist
import com.sukashawarma.superapp.feature.manager.domain.SEMUA_BUTIR
import com.sukashawarma.superapp.feature.manager.domain.LaporanCeklist
import com.sukashawarma.superapp.feature.manager.domain.NilaiCeklist
import com.sukashawarma.superapp.feature.manager.domain.adaMasalah
import com.sukashawarma.superapp.feature.manager.domain.halanganCeklist
import com.sukashawarma.superapp.feature.manager.domain.jumlahKategoriLengkap
import com.sukashawarma.superapp.feature.manager.domain.pilihNilai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

data class CeklistHarianUiState(
    val tanggal: LocalDate = CeklistHarianRepository.hariIni(),
    val outlets: List<OutletPilihan> = emptyList(),
    val laporan: Map<String, LaporanCeklist> = emptyMap(),
    /** null = daftar outlet; terisi = sedang mengisi outlet itu. */
    val outletDiisi: String? = null,
    val isian: Map<String, IsianCeklist> = emptyMap(),
    val foto: Map<String, List<FotoCeklist>> = emptyMap(),
    /** Baris teks Online Review, Temuan, dan Perbaikan. */
    val bebas: Map<BagianBebas, List<String>> = emptyMap(),
    val catatan: String = "",
    /** Kategori yang fotonya sedang diunggah, dengan jumlahnya. */
    val mengunggah: Map<String, Int> = emptyMap(),
    /** Kategori yang lembar kameranya sedang terbuka. */
    val kameraUntuk: String? = null,
    val memuat: Boolean = true,
    val mengirim: Boolean = false,
    /** Ceklist baru saja terkirim — layar sukses ditampilkan. */
    val terkirim: Boolean = false,
    val galat: String? = null,
) {
    val namaOutletDiisi: String get() = outlets.find { it.id == outletDiisi }?.nama ?: "Outlet"

    val laporanLama: LaporanCeklist? get() = outletDiisi?.let { laporan[it] }

    val jumlahSelesai: Int get() = outlets.count { laporan.containsKey(it.id) }

    val kategoriLengkap: Int get() = jumlahKategoriLengkap(isian, foto)

    val halangan: String? get() = halanganCeklist(isian, foto)

    val sedangMengunggah: Boolean get() = mengunggah.values.any { it > 0 }

    val siapKirim: Boolean get() = halangan == null && !mengirim && !sedangMengunggah

    fun teks(bagian: BagianBebas): List<String> = bebas[bagian].orEmpty()

    /** Baris yang benar-benar terisi, siap dikirim. */
    fun teksTerisi(bagian: BagianBebas): List<String> = teks(bagian).map { it.trim() }.filter { it.isNotEmpty() }

    val perluTemuan: Boolean get() = adaMasalah(isian) && teksTerisi(BagianBebas.TEMUAN).isEmpty()
}

/**
 * Pengisian ceklist harian oleh area manager.
 *
 * Satu layar dengan dua keadaan — daftar outlet binaan hari ini, lalu form satu
 * outlet — mengikuti pola Inventaris supaya AM tidak mempelajari dua cara kerja.
 */
class CeklistHarianViewModel : ViewModel() {

    private val _state = MutableStateFlow(CeklistHarianUiState())
    val state: StateFlow<CeklistHarianUiState> = _state

    private var pemuatan: Job? = null

    /** Path yang URL tanda tangannya sudah diminta — lihat InventarisViewModel. */
    private val urlDiminta: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        muatUlang()
    }

    fun muatUlang(silent: Boolean = false) {
        val senyap = silent || _state.value.outlets.isNotEmpty()
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            // Hari bisa berganti selagi aplikasi terbuka semalaman.
            val hari = CeklistHarianRepository.hariIni()
            if (!senyap) _state.update { it.copy(memuat = true, galat = null) }
            try {
                val data = CeklistHarianRepository.muatHari(hari)
                _state.update {
                    it.copy(memuat = false, tanggal = hari, outlets = data.outlets, laporan = data.laporan)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("CeklistHarianVM", "muatUlang() gagal", e)
                _state.update {
                    it.copy(memuat = false, galat = if (senyap) null else pesanGalatJaringan(e, "ceklist harian"))
                }
            }
        }
    }

    /** Membuka form satu outlet, disemai dari laporan hari ini bila sudah pernah dikirim. */
    fun mulaiIsi(outletId: String) {
        urlDiminta.clear()
        val lama = _state.value.laporan[outletId]
        _state.update {
            it.copy(
                outletDiisi = outletId,
                isian = lama?.isian.orEmpty(),
                foto = lama?.foto.orEmpty(),
                bebas = BagianBebas.entries.associateWith { lama?.teks(it).orEmpty() },
                catatan = lama?.catatan.orEmpty(),
                mengunggah = emptyMap(),
                terkirim = false,
                galat = null,
            )
        }
    }

    fun tutupForm() {
        urlDiminta.clear()
        _state.update {
            it.copy(
                outletDiisi = null,
                isian = emptyMap(),
                foto = emptyMap(),
                bebas = emptyMap(),
                catatan = "",
                terkirim = false,
            )
        }
    }

    fun nilai(butir: ButirCeklist, nilai: NilaiCeklist) {
        _state.update {
            val lama = it.isian[butir.kunci] ?: IsianCeklist()
            it.copy(isian = it.isian + (butir.kunci to pilihNilai(butir, lama, nilai)))
        }
    }

    fun keterangan(butir: ButirCeklist, teks: String) {
        _state.update {
            val lama = it.isian[butir.kunci] ?: IsianCeklist()
            it.copy(isian = it.isian + (butir.kunci to lama.copy(keterangan = teks)))
        }
    }

    /**
     * "Semua sesuai": butir yang BELUM dinilai diberi Baik. Yang sudah dinilai
     * tidak disentuh — AM yang sudah menandai Stok "kurang aman" tidak boleh
     * kehilangan penilaiannya karena satu ketukan pintas.
     */
    fun semuaBaik() {
        _state.update {
            val baru = it.isian.toMutableMap()
            SEMUA_BUTIR.forEach { butir ->
                if (baru[butir.kunci]?.nilai == null) {
                    baru[butir.kunci] = pilihNilai(butir, baru[butir.kunci] ?: IsianCeklist(), NilaiCeklist.BAIK)
                }
            }
            it.copy(isian = baru)
        }
    }

    fun tambahBaris(bagian: BagianBebas) = _state.update {
        it.copy(bebas = it.bebas + (bagian to it.teks(bagian) + ""))
    }

    fun ubahBaris(bagian: BagianBebas, i: Int, teks: String) = _state.update {
        it.copy(bebas = it.bebas + (bagian to it.teks(bagian).mapIndexed { j, t -> if (j == i) teks else t }))
    }

    fun hapusBaris(bagian: BagianBebas, i: Int) = _state.update {
        it.copy(bebas = it.bebas + (bagian to it.teks(bagian).filterIndexed { j, _ -> j != i }))
    }

    fun ubahCatatan(teks: String) = _state.update { it.copy(catatan = teks) }

    fun bukaKamera(kategori: String) {
        val s = _state.value
        val terpakai = s.foto[kategori].orEmpty().size + (s.mengunggah[kategori] ?: 0)
        if (terpakai >= FOTO_MAKS_PER_KATEGORI) {
            _state.update { it.copy(galat = "Maksimal $FOTO_MAKS_PER_KATEGORI foto per kategori.") }
            return
        }
        _state.update { it.copy(kameraUntuk = kategori) }
    }

    fun tutupKamera() = _state.update { it.copy(kameraUntuk = null) }

    fun hapusFoto(kategori: String, path: String) = _state.update {
        it.copy(foto = it.foto + (kategori to it.foto[kategori].orEmpty().filterNot { f -> f.path == path }))
    }

    fun tutupKabar() = _state.update { it.copy(galat = null) }

    /** URL pratinjau diminta sekali per path, hanya untuk foto yang sedang tampil. */
    fun pastikanUrlFoto(kategori: String, path: String) {
        if (!urlDiminta.add(path)) return
        viewModelScope.launch {
            val url = CeklistHarianRepository.urlFoto(path)
            if (url == null) {
                urlDiminta.remove(path)
                return@launch
            }
            _state.update {
                it.copy(
                    foto = it.foto + (
                        kategori to it.foto[kategori].orEmpty().map { f -> if (f.path == path) f.copy(url = url) else f }
                        ),
                )
            }
        }
    }

    /**
     * Foto diunggah segera setelah dipotret, bukan ditumpuk sampai tombol kirim —
     * satu kegagalan jaringan di akhir tidak boleh membuang seluruh kunjungan.
     */
    fun simpanFoto(kategori: String, bitmap: Bitmap) {
        val outletId = _state.value.outletDiisi ?: return
        _state.update {
            it.copy(kameraUntuk = null, mengunggah = it.mengunggah + (kategori to (it.mengunggah[kategori] ?: 0) + 1))
        }
        viewModelScope.launch {
            try {
                val webp = withContext(Dispatchers.Default) { bitmap.keWebp() }
                val path = CeklistHarianRepository.unggahFoto(outletId, kategori, webp)
                _state.update {
                    // Form bisa sudah ditutup atau berganti outlet selama unggahan berjalan.
                    if (it.outletDiisi != outletId) it
                    else it.copy(foto = it.foto + (kategori to it.foto[kategori].orEmpty() + FotoCeklist(path)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("CeklistHarianVM", "simpanFoto() gagal", e)
                _state.update { it.copy(galat = "Foto gagal diunggah. Coba potret ulang.") }
            } finally {
                _state.update {
                    it.copy(mengunggah = it.mengunggah + (kategori to ((it.mengunggah[kategori] ?: 1) - 1).coerceAtLeast(0)))
                }
            }
        }
    }

    /**
     * Foto dari galeri — untuk kategori yang mengizinkannya (tangkapan layar
     * ulasan). Diperkecil saat decode supaya screenshot resolusi tinggi tidak
     * dimuat penuh ke memori, lalu dipampatkan WebP seperti foto kamera.
     */
    fun simpanFotoGaleri(kategori: String, uri: Uri, resolver: ContentResolver) {
        val s = _state.value
        val terpakai = s.foto[kategori].orEmpty().size + (s.mengunggah[kategori] ?: 0)
        if (terpakai >= FOTO_MAKS_PER_KATEGORI) {
            _state.update { it.copy(galat = "Maksimal $FOTO_MAKS_PER_KATEGORI foto per kategori.") }
            return
        }
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { bacaBitmap(resolver, uri) }
            if (bitmap == null) {
                _state.update { it.copy(galat = "Gambar tidak bisa dibaca. Pilih gambar lain.") }
                return@launch
            }
            simpanFoto(kategori, bitmap)
        }
    }

    private fun bacaBitmap(resolver: ContentResolver, uri: Uri): Bitmap? = try {
        // JANGAN elvis-kan decode pertama: dengan inJustDecodeBounds hasilnya
        // selalu null — dimensinya dititipkan ke `batas`.
        val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
        var sampel = 1
        while (maxOf(batas.outWidth, batas.outHeight) / (sampel * 2) >= 1280) sampel *= 2
        val opsi = BitmapFactory.Options().apply { inSampleSize = sampel }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opsi) }
    } catch (e: Exception) {
        android.util.Log.e("CeklistHarianVM", "bacaBitmap() gagal", e)
        null
    }

    fun kirim() {
        val awal = _state.value
        val outletId = awal.outletDiisi ?: return
        awal.halangan?.let { h -> _state.update { it.copy(galat = h) }; return }
        if (awal.sedangMengunggah) {
            _state.update { it.copy(galat = "Tunggu foto selesai diunggah.") }
            return
        }
        // Kunci dipasang sebelum coroutine dimulai supaya ketukan ganda tidak
        // mengirim dua kali.
        var lolos = false
        _state.update {
            lolos = !it.mengirim
            if (lolos) it.copy(mengirim = true, galat = null) else it
        }
        if (!lolos) return

        viewModelScope.launch {
            try {
                CeklistHarianRepository.kirim(
                    outletId = outletId,
                    isian = awal.isian,
                    foto = awal.foto,
                    onlineReview = awal.teksTerisi(BagianBebas.ONLINE_REVIEW),
                    temuan = awal.teksTerisi(BagianBebas.TEMUAN),
                    perbaikan = awal.teksTerisi(BagianBebas.PERBAIKAN),
                    catatan = awal.catatan,
                )
                _state.update { it.copy(mengirim = false, terkirim = true) }
                muatUlang(silent = true)
            } catch (e: CancellationException) {
                _state.update { it.copy(mengirim = false) }
                throw e
            } catch (e: Exception) {
                android.util.Log.e("CeklistHarianVM", "kirim() gagal", e)
                _state.update { it.copy(mengirim = false, galat = pesanKirimGagal(e)) }
            }
        }
    }

    /** Pesan dari RPC sudah berbahasa Indonesia; diteruskan bila dikenali. */
    private fun pesanKirimGagal(e: Exception): String {
        if (e !is Postgrest.PostgrestException) return pesanGalatJaringan(e, "ceklist harian")
        // Badan galat PostgREST berbentuk JSON; `message` berisi teks RAISE EXCEPTION.
        val isi = runCatching {
            com.google.gson.JsonParser.parseString(e.message.orEmpty()).asJsonObject.get("message").asString
        }.getOrDefault(e.message.orEmpty())
        return when {
            "sudah diisi oleh" in isi -> "$isi."
            "Outlet di luar scope" in isi -> "Outlet ini di luar cakupan binaan Anda."
            "Hanya area manager" in isi -> "Hanya area manager yang bisa mengisi ceklist harian."
            "Foto ceklist" in isi || "Path foto" in isi -> "Ada foto yang belum lengkap atau tidak sah. Potret ulang."
            "Penilaian ceklist" in isi -> "Masih ada penilaian yang belum diisi."
            else -> "Gagal mengirim ceklist. Coba lagi."
        }
    }
}
