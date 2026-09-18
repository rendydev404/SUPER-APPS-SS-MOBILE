package com.sukashawarma.superapp.feature.stok.ui.entri

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.core.camera.keJpeg
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.data.remote.HasilAksi
import com.sukashawarma.superapp.data.remote.LampiranOutbox
import com.sukashawarma.superapp.data.remote.kirimAtauAntre
import com.sukashawarma.superapp.feature.stok.offline.WasteOffline
import com.sukashawarma.superapp.feature.stok.data.EntriManualRepository
import com.sukashawarma.superapp.feature.stok.data.LedgerRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** Jenis entri — nilainya persis kolom `ledger_stok.tipe` yang dipakai web. */
enum class JenisEntri(val nilai: String, val label: String) {
    PENYESUAIAN("adjustment", "Penyesuaian"),
    WASTE("waste", "Waste"),
    TRANSFER_KELUAR("transfer_keluar", "Transfer keluar"),
}

/** Satuan yang sedang dipakai saat mengetik jumlah. */
enum class SatuanInput { BESAR, TENGAH, KECIL }

/**
 * Arah penyesuaian — cermin `adjDirection` di `ManualEntryForm.tsx`.
 *
 * Tanpa pilihan ini penyesuaian hanya bisa MENAMBAH stok: `ledger_stok.qty` untuk
 * tipe `adjustment` dipakai apa adanya, dan kolom jumlah tidak menerima angka
 * negatif. Padahal koreksi ke bawah justru yang paling sering dibutuhkan.
 */
enum class ArahPenyesuaian(val label: String, val tanda: String) {
    MASUK("Penambahan stok", "+"),
    KELUAR("Pengurangan stok", "−"),
}

data class EntriManualUiState(
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val bahan: List<MonitoringRow> = emptyList(),
    val cari: String = "",
    val jenis: JenisEntri = JenisEntri.PENYESUAIAN,
    /** Hanya berlaku untuk [JenisEntri.PENYESUAIAN]. Bawaan sama dengan web: menambah. */
    val arah: ArahPenyesuaian = ArahPenyesuaian.MASUK,
    val bahanTerpilih: MonitoringRow? = null,
    val satuanInput: SatuanInput = SatuanInput.BESAR,
    val jumlah: String = "",
    val alasan: String = "",
    val catatan: String = "",
    /**
     * Foto bukti waste sebagai BERKAS LOKAL, bukan URL hasil unggahan.
     *
     * Dulu foto diunggah saat dipotret, jadi tanpa sinyal layar ini mentok di "Foto gagal
     * diunggah" dan laporan waste tidak pernah bisa dibuat. Sekarang unggahannya menyatu
     * dengan pengiriman, lewat jalur yang sama dengan antrean offline.
     */
    val fotoBukti: java.io.File? = null,
    val kameraTerbuka: Boolean = false,
    val mengunggahFoto: Boolean = false,
    val memuat: Boolean = true,
    val menyimpan: Boolean = false,
    val pesan: String? = null,
    val error: String? = null,
) {
    val bahanTampil: List<MonitoringRow>
        get() {
            val kueri = cari.trim().lowercase()
            if (kueri.isEmpty()) return bahan
            return bahan.filter { it.itemName.lowercase().contains(kueri) }
        }

    val butuhFoto: Boolean get() = jenis == JenisEntri.WASTE

    val butuhAlasan: Boolean get() = jenis == JenisEntri.WASTE || jenis == JenisEntri.PENYESUAIAN

    val jumlahAngka: Double? get() = jumlah.trim().replace(',', '.').toDoubleOrNull()

    /**
     * Jumlah dikonversi ke satuan BESAR, karena itu yang disimpan kedua tujuannya:
     * `stok_waste_reports.qty` dan `ledger_stok.qty`.
     */
    val jumlahBesar: Double?
        get() {
            val angka = jumlahAngka ?: return null
            val meta = bahanTerpilih?.meta ?: return angka
            return when (satuanInput) {
                SatuanInput.BESAR -> angka
                SatuanInput.TENGAH -> meta.faktorTengah?.takeIf { it > 0 }?.let { angka / it }
                SatuanInput.KECIL -> meta.faktorTampilan?.takeIf { it > 0 }?.let { angka / it }
            }
        }

    /**
     * Jumlah pada SKALA BARIS SALDO, untuk ditulis ke `ledger_stok`.
     *
     * Jalur ini menyisipkan baris ledger langsung dari klien, tidak lewat fungsi
     * database. Jadi konversi yang dilakukan `to_ledger_scale` untuk penulis
     * server-side (lihat migrasi `20300105000017_scale_aware_ledger_writers`)
     * harus dikerjakan di sini — kalau tidak, penyesuaian 1 kg pada baris yang
     * sudah gram-scale tercatat sebagai 1 gram.
     *
     * Waste TIDAK memakai ini: `stok_waste_reports.qty` justru wajib satuan besar
     * karena trigger persetujuannya yang mengonversi.
     */
    val jumlahSkalaLedger: Double?
        get() {
            val besar = jumlahBesar ?: return null
            val row = bahanTerpilih ?: return besar
            return if (row.saldoIsGram) UnitScale.smallestFromBesar(besar, row.meta) else besar
        }

    /** Perubahan saldo bertanda, pada satuan terkecil — dasar pratinjau. */
    val deltaNorm: Double?
        get() {
            val besar = jumlahBesar ?: return null
            val row = bahanTerpilih ?: return null
            val kecil = UnitScale.smallestFromBesar(besar, row.meta) ?: return null
            return if (jenis == JenisEntri.PENYESUAIAN && arah == ArahPenyesuaian.MASUK) kecil else -kecil
        }

    /** Saldo setelah entri ini, pada satuan terkecil. Null bila belum bisa dihitung. */
    val saldoSesudahNorm: Double?
        get() {
            val sebelum = bahanTerpilih?.saldoNorm ?: return null
            return sebelum + (deltaNorm ?: return null)
        }

    /** Entri ini akan membuat saldo jadi minus — diperingatkan, tidak dilarang. */
    val saldoJadiMinus: Boolean get() = (saldoSesudahNorm ?: 0.0) < 0.0

    val labelSatuan: String
        get() {
            val meta = bahanTerpilih?.meta
            return when (satuanInput) {
                SatuanInput.BESAR -> meta?.satuan ?: "satuan"
                SatuanInput.TENGAH -> meta?.satuanTengah ?: "tengah"
                SatuanInput.KECIL -> meta?.satuanKecil ?: "kecil"
            }
        }

    /** Alasan tidak bisa dikirim, atau null bila sudah boleh. */
    val halangan: String?
        get() = when {
            outletTerpilih == null -> "Pilih outlet lebih dulu."
            bahanTerpilih == null -> "Pilih bahan baku."
            jumlahAngka == null || jumlahAngka!! <= 0.0 -> "Jumlah belum diisi."
            jumlahBesar == null -> "Faktor konversi bahan ini belum lengkap, pilih satuan besar."
            jenis != JenisEntri.WASTE && jumlahSkalaLedger == null ->
                "Faktor satuan bahan ini belum lengkap, jadi jumlahnya tidak bisa dicatat dengan aman."
            butuhAlasan && alasan.isBlank() -> "Alasan wajib diisi."
            butuhFoto && fotoBukti == null -> "Foto bukti wajib diambil."
            else -> null
        }
}

/**
 * Entri manual ledger dan pelaporan waste.
 *
 * Satu layar untuk tiga jenis, mengikuti `ManualEntryForm.tsx`. Yang membedakan
 * bukan sekadar label: waste berhenti di `stok_waste_reports` menunggu persetujuan,
 * sedangkan penyesuaian dan transfer keluar langsung mengubah saldo.
 */
class EntriManualViewModel : ViewModel() {

    private val _state = MutableStateFlow(EntriManualUiState())
    val state: StateFlow<EntriManualUiState> = _state

    init {
        muatAwal()
    }

    fun muatAwal() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val outlets = StokRepository.accessibleOutlets()
                val pilihan = _state.value.outletTerpilih ?: outlets.firstOrNull()
                _state.value = _state.value.copy(outlets = outlets, outletTerpilih = pilihan)
                if (pilihan != null) muatBahan(pilihan.id) else _state.value = _state.value.copy(memuat = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(
            outletTerpilih = outlet,
            bahanTerpilih = null,
            fotoBukti = null,
            memuat = true,
        )
        viewModelScope.launch { muatBahan(outlet.id) }
    }

    private suspend fun muatBahan(outletId: String) {
        try {
            val baris = StokRepository.monitoringOutlet(outletId).sortedBy { it.itemName }
            _state.value = _state.value.copy(memuat = false, bahan = baris)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    fun pilihJenis(jenis: JenisEntri) {
        // Foto hanya relevan untuk waste; menyimpannya saat pindah jenis membuat
        // pengguna mengira buktinya ikut terkirim padahal tidak.
        _state.value = _state.value.copy(
            jenis = jenis,
            alasan = "",
            fotoBukti = if (jenis == JenisEntri.WASTE) _state.value.fotoBukti else null,
        )
    }

    fun pilihBahan(row: MonitoringRow?) {
        val meta = row?.meta
        _state.value = _state.value.copy(
            bahanTerpilih = row,
            // Timbangan di outlet membaca satuan terkecil, jadi itu yang paling sering
            // diketik. Web memilih default yang sama.
            satuanInput = if (meta?.satuanKecil != null) SatuanInput.KECIL else SatuanInput.BESAR,
            jumlah = "",
            fotoBukti = null,
        )
    }

    fun pilihArah(arah: ArahPenyesuaian) { _state.value = _state.value.copy(arah = arah) }

    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }
    fun ubahJumlah(teks: String) { _state.value = _state.value.copy(jumlah = teks) }
    fun ubahAlasan(teks: String) { _state.value = _state.value.copy(alasan = teks) }
    fun ubahCatatan(teks: String) { _state.value = _state.value.copy(catatan = teks) }
    fun pilihSatuanInput(satuan: SatuanInput) { _state.value = _state.value.copy(satuanInput = satuan) }
    fun bukaKamera() { _state.value = _state.value.copy(kameraTerbuka = true) }
    fun tutupKamera() { _state.value = _state.value.copy(kameraTerbuka = false) }
    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }

    /**
     * Menyimpan foto bukti ke berkas lokal, TANPA mengunggahnya.
     *
     * Unggahan menyatu dengan pengiriman (lihat [kirim]) karena dua alasan: laporan waste
     * jadi bisa dibuat saat internet mati, dan crew tidak perlu menunggu unggahan selesai
     * sambil berdiri di depan bahan yang mau dibuang.
     */
    fun simpanFoto(bitmap: Bitmap, direktoriCache: java.io.File) {
        _state.value = _state.value.copy(kameraTerbuka = false, mengunggahFoto = true)
        viewModelScope.launch {
            try {
                val berkas = withContext(Dispatchers.IO) {
                    val dir = java.io.File(direktoriCache, "bukti_waste").apply { mkdirs() }
                    // Nama tetap: memotret ulang menimpa berkas sebelumnya alih-alih
                    // meninggalkan foto yatim yang tidak akan pernah terkirim.
                    java.io.File(dir, "bukti.jpg").apply { writeBytes(bitmap.keJpeg()) }
                }
                _state.value = _state.value.copy(mengunggahFoto = false, fotoBukti = berkas)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    mengunggahFoto = false,
                    error = "Foto gagal disimpan. Coba potret ulang.",
                )
            }
        }
    }

    fun kirim() {
        var lanjut = false
        _state.update { current ->
            val halangan = current.halangan
            if (halangan != null) {
                current.copy(error = halangan)
            } else if (current.menyimpan) {
                current
            } else {
                lanjut = true
                current.copy(menyimpan = true, error = null, pesan = null)
            }
        }
        if (!lanjut) return

        val s = _state.value
        val outletId = s.outletTerpilih?.id ?: run {
            _state.update { it.copy(menyimpan = false) }
            return
        }
        val bahan = s.bahanTerpilih ?: run {
            _state.update { it.copy(menyimpan = false) }
            return
        }
        val qtyBesar = s.jumlahBesar ?: run {
            _state.update { it.copy(menyimpan = false) }
            return
        }
        val pengguna = AppSession.staff.value?.id
        if (pengguna == null) {
            _state.update { it.copy(menyimpan = false, error = "Sesi tidak valid, silakan login ulang.") }
            return
        }

        viewModelScope.launch {
            try {
                var masukAntrean = false
                if (s.jenis == JenisEntri.WASTE) {
                    val clientOpId = java.util.UUID.randomUUID().toString()
                    val hasil = kirimAtauAntre(
                        jenis = WasteOffline.JENIS,
                        clientOpId = clientOpId,
                        payload = WasteOffline.payload(
                            outletId = outletId,
                            bahanBakuId = bahan.bahanBakuId,
                            qtyBesar = qtyBesar,
                            alasan = s.alasan.trim(),
                            dilaporkanOleh = pengguna,
                        ),
                        kirim = WasteOffline::kirim,
                        outletId = outletId,
                        dibuatOleh = pengguna,
                        lampiran = s.fotoBukti?.let {
                            // Disalin ke berkas ber-nama unik: berkas "bukti.jpg" ditimpa
                            // begitu crew memotret laporan berikutnya, dan laporan ini bisa
                            // masih menunggu di antrean saat itu terjadi.
                            // Disalin, BUKAN dipindah: kalau server menolak kiriman ini,
                            // berkas aslinya harus masih ada supaya pratinjau foto di layar
                            // tidak tiba-tiba kosong dan crew tidak perlu memotret ulang.
                            val tetap = java.io.File(it.parentFile, "$clientOpId.jpg")
                            it.copyTo(tetap, overwrite = true)
                            LampiranOutbox(tetap, WasteOffline.BUCKET, WasteOffline.tujuanFoto(outletId, clientOpId))
                        },
                    )
                    masukAntrean = hasil == HasilAksi.MASUK_ANTREAN
                } else {
                    // Skala baris saldo, bukan satuan besar — lihat [jumlahSkalaLedger].
                    val qtyLedger = s.jumlahSkalaLedger!!
                    LedgerRepository.tambahManual(
                        outletId = outletId,
                        createdBy = pengguna,
                        catatanGlobal = s.catatan.trim().ifBlank { s.alasan.trim() },
                        items = listOf(
                            LedgerRepository.ManualItem(
                                bahanBakuId = bahan.bahanBakuId,
                                tipe = s.jenis.nilai,
                                qtyAbs = qtyLedger,
                                // Arah penyesuaian ditentukan pemanggil; transfer keluar
                                // selalu negatif dan itu diurus tambahManual sendiri.
                                signedOverride = if (s.jenis == JenisEntri.PENYESUAIAN) {
                                    if (s.arah == ArahPenyesuaian.MASUK) qtyLedger else -qtyLedger
                                } else {
                                    null
                                },
                                catatan = s.alasan.trim().ifBlank { null },
                            )
                        ),
                    )
                }
                StokRepository.invalidate()
                _state.update {
                    it.copy(
                        menyimpan = false,
                        jumlah = "",
                        alasan = "",
                        catatan = "",
                        fotoBukti = null,
                        bahanTerpilih = null,
                        pesan = if (masukAntrean) {
                            "Laporan waste tersimpan di HP dan akan terkirim otomatis begitu ada internet."
                        } else if (s.jenis == JenisEntri.WASTE) {
                            "Laporan waste terkirim dan menunggu persetujuan."
                        } else {
                            "${s.jenis.label} tersimpan. Saldo sudah diperbarui."
                        },
                    )
                }
                // Saldo hanya dimuat ulang kalau kiriman benar-benar sampai server; saat
                // offline pemuatan itu gagal dan menimpa pesan di atas dengan pesan galat.
                if (!masukAntrean) _state.value.outletTerpilih?.let { muatBahan(it.id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(menyimpan = false, error = stokErrorMessage(e))
                }
            }
        }
    }
}

/** Saldo bahan siap tampil, dipakai layar sebagai konteks saat mengetik jumlah. */
internal fun MonitoringRow.saldoRingkas(): String =
    saldoNorm?.let { UnitScale.formatBerjenjang(it, meta) } ?: saldoTampil
