package com.sukashawarma.superapp.feature.stok.ui.retur

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.core.camera.keJpeg
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.PermintaanRepository
import com.sukashawarma.superapp.feature.stok.data.ReturRepository
import com.sukashawarma.superapp.feature.stok.data.model.BahanBaku
import com.sukashawarma.superapp.feature.stok.data.model.Retur
import com.sukashawarma.superapp.feature.stok.domain.FormulirRetur
import com.sukashawarma.superapp.feature.stok.domain.JenisLogistik
import com.sukashawarma.superapp.feature.stok.domain.ReturAkses
import com.sukashawarma.superapp.feature.stok.domain.StatusRetur
import com.sukashawarma.superapp.feature.stok.domain.TabRetur
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lembar yang sedang terbuka di atas daftar tiket. */
enum class LembarRetur { TIDAK_ADA, FORM, APPROVE, KURIR, KITCHEN }

data class ReturUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    val role: Role? = null,
    val outletSaya: String? = null,
    val outletNama: String? = null,
    val bolehManager: Boolean = false,
    val bolehKitchen: Boolean = false,
    val tab: TabRetur = TabRetur.AKTIF,
    val daftar: List<Retur> = emptyList(),
    val katalog: List<BahanBaku> = emptyList(),
    val lembar: LembarRetur = LembarRetur.TIDAK_ADA,
    val memproses: Boolean = false,
    val kameraTerbuka: Boolean = false,
    val mengunggahFoto: Boolean = false,
    /** Foto yang sedang dilihat penuh (lightbox): judul ke URL. */
    val fotoBesar: Pair<String, String>? = null,
    // ---- formulir pengajuan
    val formBahanId: String? = null,
    val formQtyBesar: String = "",
    val formQtyKecil: String = "",
    val formAlasan: String = FormulirRetur.ALASAN.first(),
    val formAlasanLainnya: String = "",
    val formCatatan: String = "",
    val formFotoUrl: String? = null,
    // ---- lembar keputusan manajer
    val tiketAktif: Retur? = null,
    val catatanManager: String = "",
    // ---- lembar serah terima kurir
    val kurirJenis: JenisLogistik = JenisLogistik.INTERNAL,
    val kurirResi: String = "",
    val kurirNama: String = "",
    val kurirKontak: String = "",
    val kurirPlat: String = "",
    val kurirFotoUrl: String? = null,
    // ---- lembar verifikasi kitchen
    /** retur_stok_item.id -> teks bobot timbang ulang gudang pusat (satuan besar). */
    val timbangKitchen: Map<String, String> = emptyMap(),
    val catatanKitchen: String = "",
    val kirimSekarang: Boolean = true,
) {
    /** Katalog yang boleh diretur — cermin `refundableBahan` di form web. */
    val katalogRefundable: List<BahanBaku>
        get() = katalog.filter { FormulirRetur.refundable(it.isRefundable, it.nama) }

    val bahanTerpilih: BahanBaku? get() = katalog.firstOrNull { it.id == formBahanId }

    val totalKlaimBesar: Double
        get() = FormulirRetur.totalBesar(
            besar = formQtyBesar.toDoubleOrNull(),
            kecil = formQtyKecil.toDoubleOrNull(),
            faktorTampilan = bahanTerpilih?.faktorTampilan,
        )

    val halanganForm: String?
        get() = FormulirRetur.halangan(
            bahanTerpilih = formBahanId != null,
            totalBesar = totalKlaimBesar,
            adaFoto = formFotoUrl != null,
            alasan = formAlasan,
            keteranganLainnya = formAlasanLainnya,
        )

    val aktif: List<Retur> get() = daftar.filter { !it.status.tuntas }
    val riwayat: List<Retur> get() = daftar.filter { it.status.tuntas }
    val antreanManager: List<Retur> get() = daftar.filter { it.status == StatusRetur.DIAJUKAN }
    val antreanKitchen: List<Retur>
        get() = daftar.filter {
            it.status == StatusRetur.DALAM_PENGIRIMAN || it.status == StatusRetur.DITERIMA_KITCHEN
        }

    val tabTerlihat: List<TabRetur>
        get() = buildList {
            add(TabRetur.AKTIF)
            if (bolehManager) add(TabRetur.PERSETUJUAN)
            if (bolehKitchen) add(TabRetur.KITCHEN)
            add(TabRetur.RIWAYAT)
        }

    fun isi(tab: TabRetur): List<Retur> = when (tab) {
        TabRetur.AKTIF -> aktif
        TabRetur.PERSETUJUAN -> antreanManager
        TabRetur.KITCHEN -> antreanKitchen
        TabRetur.RIWAYAT -> riwayat
    }

    fun jumlah(tab: TabRetur): Int = isi(tab).size
}

/**
 * Retur & Refund Bahan — cermin `app/stok/refund/page.tsx` beserta lima komponen di
 * `components/refund/`.
 *
 * Seluruh lembar (form, keputusan manajer, serah terima kurir, verifikasi kitchen)
 * ditampung satu ViewModel, sama seperti web menampungnya di satu halaman: keempatnya
 * bekerja pada daftar tiket yang sama dan tiap aksi harus memuat ulang daftar itu.
 *
 * Lingkup pembacaan ditentukan peran, bukan pilihan pengguna. Manajer dan gudang
 * pusat membaca seluruh outlet yang terjangkau, kru hanya outletnya sendiri —
 * cermin `outletId: isManager || isKitchen ? null : outletId` di web.
 */
class ReturViewModel : ViewModel() {

    private val _state = MutableStateFlow(ReturUiState())
    val state: StateFlow<ReturUiState> = _state

    init { muatAwal() }

    fun muatAwal() {
        val staff = AppSession.staff.value
        val role = staff?.role
        _state.value = _state.value.copy(
            memuat = true,
            error = null,
            role = role,
            outletSaya = staff?.outletId,
            outletNama = staff?.outletName,
            bolehManager = ReturAkses.manager(role),
            bolehKitchen = ReturAkses.kitchen(role),
            tab = if (_state.value.daftar.isEmpty()) ReturAkses.tabAwal(role) else _state.value.tab,
        )
        muatUlang()
    }

    fun muatUlang() {
        viewModelScope.launch {
            val s = _state.value
            try {
                // Katalog hanya dipakai formulir pengajuan dan sudah di-cache 5 menit
                // oleh repository permintaan, jadi memanggilnya di sini tidak
                // menambah kueri untuk peran yang tidak pernah membuka form.
                val katalog = runCatching { PermintaanRepository.bahanBaku() }
                    .getOrDefault(s.katalog)
                val lingkup = if (s.bolehManager || s.bolehKitchen) null else s.outletSaya
                val daftar = ReturRepository.daftar(lingkup)
                _state.value = _state.value.copy(memuat = false, katalog = katalog, daftar = daftar)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihTab(tab: TabRetur) { _state.value = _state.value.copy(tab = tab) }

    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }

    fun lihatFoto(judul: String, url: String) {
        _state.value = _state.value.copy(fotoBesar = judul to url)
    }

    fun tutupFoto() { _state.value = _state.value.copy(fotoBesar = null) }

    fun tutupLembar() {
        _state.value = _state.value.copy(
            lembar = LembarRetur.TIDAK_ADA,
            tiketAktif = null,
            kameraTerbuka = false,
        )
    }

    // ------------------------------------------------------------- formulir

    fun bukaForm() {
        _state.value = _state.value.copy(
            lembar = LembarRetur.FORM,
            formBahanId = null,
            formQtyBesar = "",
            formQtyKecil = "",
            formAlasan = FormulirRetur.ALASAN.first(),
            formAlasanLainnya = "",
            formCatatan = "",
            formFotoUrl = null,
        )
    }

    fun pilihBahan(id: String) {
        // Qty ikut dikosongkan seperti web: angka 2 "kg" yang tertinggal saat bahan
        // berganti ke satuan Pack akan terbaca sebagai klaim yang sama sekali lain.
        _state.value = _state.value.copy(formBahanId = id, formQtyBesar = "", formQtyKecil = "")
    }

    fun ubahQtyBesar(nilai: String) { if (angkaValid(nilai)) _state.value = _state.value.copy(formQtyBesar = nilai) }
    fun ubahQtyKecil(nilai: String) { if (angkaValid(nilai)) _state.value = _state.value.copy(formQtyKecil = nilai) }
    fun pilihAlasan(nilai: String) { _state.value = _state.value.copy(formAlasan = nilai) }
    fun ubahAlasanLainnya(nilai: String) { _state.value = _state.value.copy(formAlasanLainnya = nilai) }
    fun ubahCatatanForm(nilai: String) { _state.value = _state.value.copy(formCatatan = nilai) }

    private fun angkaValid(nilai: String) = nilai.isEmpty() || nilai.matches(Regex("^\\d{0,6}([.,]\\d{0,3})?$"))

    fun kirimKlaim() {
        var lanjut = false
        _state.update { current ->
            val halangan = current.halanganForm
            if (halangan != null) {
                current.copy(error = halangan)
            } else if (current.memproses) {
                current
            } else {
                lanjut = true
                current.copy(memproses = true, error = null)
            }
        }
        if (!lanjut) return

        val s = _state.value
        val outletId = s.outletSaya ?: run {
            _state.update { it.copy(memproses = false) }
            return
        }
        val bahanId = s.formBahanId ?: run {
            _state.update { it.copy(memproses = false) }
            return
        }
        val foto = s.formFotoUrl ?: run {
            _state.update { it.copy(memproses = false) }
            return
        }

        viewModelScope.launch {
            try {
                ReturRepository.ajukan(
                    outletId = outletId,
                    bahanBakuId = bahanId,
                    qtyBesar = s.totalKlaimBesar,
                    fotoUrl = foto,
                    alasan = FormulirRetur.alasanAkhir(s.formAlasan, s.formAlasanLainnya),
                    catatan = s.formCatatan.trim().takeIf { it.isNotEmpty() },
                )
                _state.update {
                    it.copy(
                        memproses = false,
                        lembar = LembarRetur.TIDAK_ADA,
                        tab = TabRetur.AKTIF,
                        pesan = "Pengajuan retur berhasil dibuat! Menunggu review AM/RM.",
                    )
                }
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(memproses = false, error = stokErrorMessage(e)) }
            }
        }
    }

    // ----------------------------------------------------------------- foto

    fun bukaKamera() { _state.value = _state.value.copy(kameraTerbuka = true) }
    fun tutupKamera() { _state.value = _state.value.copy(kameraTerbuka = false) }

    /**
     * Foto diunggah langsung setelah dipotret, bukan saat tombol kirim ditekan.
     *
     * Bedanya terasa di outlet bersinyal buruk: unggahan gagal ketahuan saat orang
     * masih memegang barangnya, bukan setelah ia mengira klaimnya sudah masuk.
     */
    fun simpanFoto(bitmap: Bitmap) {
        val s = _state.value
        val outletId = s.outletSaya ?: s.tiketAktif?.outletId ?: return
        val untukKurir = s.lembar == LembarRetur.KURIR
        _state.value = s.copy(kameraTerbuka = false, mengunggahFoto = true)
        viewModelScope.launch {
            try {
                val url = ReturRepository.unggahBukti(outletId, bitmap.keJpeg())
                _state.value = if (untukKurir) {
                    _state.value.copy(mengunggahFoto = false, kurirFotoUrl = url)
                } else {
                    _state.value.copy(mengunggahFoto = false, formFotoUrl = url)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    mengunggahFoto = false,
                    error = "Foto gagal diunggah. Coba potret ulang.",
                )
            }
        }
    }

    // -------------------------------------------------------- keputusan AM/RM

    fun bukaApprove(retur: Retur) {
        _state.value = _state.value.copy(
            lembar = LembarRetur.APPROVE,
            tiketAktif = retur,
            catatanManager = "",
        )
    }

    fun ubahCatatanManager(nilai: String) { _state.value = _state.value.copy(catatanManager = nilai) }

    /**
     * Menolak WAJIB beralasan — cermin penjaga di `ModalApproveManager`. Alasannya
     * masuk ke `catatan_manager` dan ikut tertulis pada baris ledger waste, jadi
     * itulah satu-satunya keterangan yang kelak dibaca outlet dan bagian keuangan.
     */
    fun putuskanManager(setuju: Boolean) {
        val s = _state.value
        val tiket = s.tiketAktif ?: return
        if (!setuju && s.catatanManager.isBlank()) {
            _state.update { it.copy(error = "Wajib mengisi alasan penolakan untuk kru outlet.") }
            return
        }
        var lanjut = false
        _state.update { current ->
            if (current.memproses) {
                current
            } else {
                lanjut = true
                current.copy(memproses = true, error = null)
            }
        }
        if (!lanjut) return

        viewModelScope.launch {
            try {
                ReturRepository.putuskanManager(
                    returId = tiket.id,
                    setuju = setuju,
                    catatan = s.catatanManager.trim().takeIf { it.isNotEmpty() },
                    // Sama dengan auth.uid(); dikirim untuk memecah ambiguitas
                    // overload fungsi di database — lihat KDoc putuskanManager.
                    managerId = AppSession.staff.value?.id,
                )
                _state.update {
                    it.copy(
                        memproses = false,
                        lembar = LembarRetur.TIDAK_ADA,
                        tiketAktif = null,
                        pesan = if (setuju) "Pengajuan retur disetujui. Siap dijemput kurir."
                        else "Pengajuan retur ditolak & dialihkan resmi menjadi waste outlet.",
                    )
                }
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(memproses = false, error = stokErrorMessage(e)) }
                muatUlang()
            }
        }
    }

    // ------------------------------------------------------- serah terima kurir

    fun bukaKurir(retur: Retur) {
        _state.value = _state.value.copy(
            lembar = LembarRetur.KURIR,
            tiketAktif = retur,
            kurirJenis = JenisLogistik.INTERNAL,
            kurirResi = "",
            kurirNama = "",
            kurirKontak = "",
            kurirPlat = "",
            kurirFotoUrl = null,
        )
    }

    fun pilihLogistik(jenis: JenisLogistik) { _state.value = _state.value.copy(kurirJenis = jenis) }
    fun ubahKurirResi(nilai: String) { _state.value = _state.value.copy(kurirResi = nilai) }
    fun ubahKurirNama(nilai: String) { _state.value = _state.value.copy(kurirNama = nilai) }
    fun ubahKurirKontak(nilai: String) { _state.value = _state.value.copy(kurirKontak = nilai) }
    fun ubahKurirPlat(nilai: String) { _state.value = _state.value.copy(kurirPlat = nilai) }

    fun kirimSerahTerima() {
        val s = _state.value
        val tiket = s.tiketAktif ?: return
        if (s.kurirNama.isBlank()) {
            _state.update { it.copy(error = "Nama supir / kurir wajib diisi.") }
            return
        }
        if (s.kurirJenis.pihakKetiga && s.kurirResi.isBlank()) {
            _state.update {
                it.copy(
                    error = "Nomor Order / Resi ${s.kurirJenis.nilai.uppercase()} wajib diisi untuk pelacakan.",
                )
            }
            return
        }
        var lanjut = false
        _state.update { current ->
            if (current.memproses) {
                current
            } else {
                lanjut = true
                current.copy(memproses = true, error = null)
            }
        }
        if (!lanjut) return

        viewModelScope.launch {
            try {
                ReturRepository.serahKurir(
                    returId = tiket.id,
                    jenis = s.kurirJenis,
                    nomorResi = s.kurirResi.trim().takeIf { it.isNotEmpty() },
                    driverNama = s.kurirNama.trim(),
                    driverKontak = s.kurirKontak.trim().takeIf { it.isNotEmpty() },
                    driverPlat = s.kurirPlat.trim().takeIf { it.isNotEmpty() },
                    fotoUrl = s.kurirFotoUrl,
                )
                _state.update {
                    it.copy(
                        memproses = false,
                        lembar = LembarRetur.TIDAK_ADA,
                        tiketAktif = null,
                        pesan = "Serah terima kurir berhasil dicatat. Status: Dalam Pengiriman.",
                    )
                }
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(memproses = false, error = stokErrorMessage(e)) }
                muatUlang()
            }
        }
    }

    // ------------------------------------------------------- verifikasi kitchen

    fun bukaKitchen(retur: Retur) {
        // Bobot diisi awal dengan timbangan tersimpan, atau klaim outlet bila belum
        // pernah ditimbang — sama seperti web, supaya petugas tinggal mengoreksi.
        val awal = retur.items.associate { it.id to formatBobot(it.qtyDiterimaKitchen ?: it.qtyKlaim) }
        _state.value = _state.value.copy(
            lembar = LembarRetur.KITCHEN,
            tiketAktif = retur,
            timbangKitchen = awal,
            catatanKitchen = retur.catatanKitchen.orEmpty(),
            // Tiket yang fisiknya sudah diterima hanya menyisakan satu tindakan:
            // menerbitkan SJ Pengganti. Cermin `isAlreadyReceived` di web.
            kirimSekarang = true,
        )
    }

    fun ubahTimbang(itemId: String, nilai: String) {
        if (!angkaValid(nilai)) return
        _state.value = _state.value.copy(timbangKitchen = _state.value.timbangKitchen + (itemId to nilai))
    }

    fun ubahCatatanKitchen(nilai: String) { _state.value = _state.value.copy(catatanKitchen = nilai) }
    fun pilihWaktuKirim(sekarang: Boolean) { _state.value = _state.value.copy(kirimSekarang = sekarang) }

    fun kirimVerifikasiKitchen() {
        val s = _state.value
        val tiket = s.tiketAktif ?: return

        val qty = HashMap<String, Double>()
        tiket.items.forEach { item ->
            val nilai = s.timbangKitchen[item.id]?.replace(',', '.')?.toDoubleOrNull()
            if (nilai == null || nilai <= 0.0) {
                _state.update {
                    it.copy(
                        error = "Kuantitas timbangan untuk ${item.namaBahan ?: "item"} harus valid.",
                    )
                }
                return
            }
            qty[item.id] = nilai
        }

        // Tiket yang sudah berstatus diterima_kitchen tidak punya pilihan "nanti":
        // fisiknya sudah di gudang, satu-satunya langkah berikutnya adalah menerbitkan
        // surat jalan. Cermin `kirimSekarang` di web.
        val sudahDiterima = tiket.status == StatusRetur.DITERIMA_KITCHEN
        val terbitkan = sudahDiterima || s.kirimSekarang

        var lanjut = false
        _state.update { current ->
            if (current.memproses) {
                current
            } else {
                lanjut = true
                current.copy(memproses = true, error = null)
            }
        }
        if (!lanjut) return

        viewModelScope.launch {
            try {
                ReturRepository.verifikasiKitchen(
                    returId = tiket.id,
                    qtyPerItem = qty,
                    catatan = s.catatanKitchen.trim().takeIf { it.isNotEmpty() },
                    terbitkanSjSekarang = terbitkan,
                )
                _state.update {
                    it.copy(
                        memproses = false,
                        lembar = LembarRetur.TIDAK_ADA,
                        tiketAktif = null,
                        pesan = if (terbitkan) "Surat Jalan Pengganti berhasil diterbitkan!"
                        else "Hasil verifikasi timbang fisik tersimpan. Penggantian dapat menumpang pengiriman berikutnya.",
                    )
                }
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(memproses = false, error = stokErrorMessage(e)) }
                muatUlang()
            }
        }
    }
}

/** Tiga desimal sudah cukup untuk timbangan dapur, dan menghindari ekor ".0". */
internal fun formatBobot(nilai: Double): String =
    if (nilai % 1.0 == 0.0) nilai.toLong().toString()
    else String.format(java.util.Locale.US, "%.3f", nilai).trimEnd('0').trimEnd('.')
