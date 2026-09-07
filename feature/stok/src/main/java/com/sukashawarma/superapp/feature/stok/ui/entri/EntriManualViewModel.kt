package com.sukashawarma.superapp.feature.stok.ui.entri

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.core.camera.keJpeg
import com.sukashawarma.superapp.domain.session.AppSession
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
import kotlinx.coroutines.launch

/** Jenis entri — nilainya persis kolom `ledger_stok.tipe` yang dipakai web. */
enum class JenisEntri(val nilai: String, val label: String) {
    PENYESUAIAN("adjustment", "Penyesuaian"),
    WASTE("waste", "Waste"),
    TRANSFER_KELUAR("transfer_keluar", "Transfer keluar"),
}

/** Satuan yang sedang dipakai saat mengetik jumlah. */
enum class SatuanInput { BESAR, TENGAH, KECIL }

data class EntriManualUiState(
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val bahan: List<MonitoringRow> = emptyList(),
    val cari: String = "",
    val jenis: JenisEntri = JenisEntri.PENYESUAIAN,
    val bahanTerpilih: MonitoringRow? = null,
    val satuanInput: SatuanInput = SatuanInput.BESAR,
    val jumlah: String = "",
    val alasan: String = "",
    val catatan: String = "",
    val fotoUrl: String? = null,
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
            butuhAlasan && alasan.isBlank() -> "Alasan wajib diisi."
            butuhFoto && fotoUrl == null -> "Foto bukti wajib diunggah."
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
            fotoUrl = null,
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
            fotoUrl = if (jenis == JenisEntri.WASTE) _state.value.fotoUrl else null,
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
            fotoUrl = null,
        )
    }

    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }
    fun ubahJumlah(teks: String) { _state.value = _state.value.copy(jumlah = teks) }
    fun ubahAlasan(teks: String) { _state.value = _state.value.copy(alasan = teks) }
    fun ubahCatatan(teks: String) { _state.value = _state.value.copy(catatan = teks) }
    fun pilihSatuanInput(satuan: SatuanInput) { _state.value = _state.value.copy(satuanInput = satuan) }
    fun bukaKamera() { _state.value = _state.value.copy(kameraTerbuka = true) }
    fun tutupKamera() { _state.value = _state.value.copy(kameraTerbuka = false) }
    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }

    fun simpanFoto(bitmap: Bitmap) {
        val outletId = _state.value.outletTerpilih?.id ?: return
        val bahanId = _state.value.bahanTerpilih?.bahanBakuId ?: return
        _state.value = _state.value.copy(kameraTerbuka = false, mengunggahFoto = true)
        viewModelScope.launch {
            try {
                val url = EntriManualRepository.unggahBuktiWaste(outletId, bahanId, bitmap.keJpeg())
                _state.value = _state.value.copy(mengunggahFoto = false, fotoUrl = url)
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

    fun kirim() {
        val s = _state.value
        val halangan = s.halangan
        if (halangan != null) {
            _state.value = s.copy(error = halangan)
            return
        }
        if (s.menyimpan) return

        val outletId = s.outletTerpilih!!.id
        val bahan = s.bahanTerpilih!!
        val qtyBesar = s.jumlahBesar!!
        val pengguna = AppSession.staff.value?.id
        if (pengguna == null) {
            _state.value = s.copy(error = "Sesi tidak valid, silakan login ulang.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(menyimpan = true, error = null, pesan = null)
            try {
                if (s.jenis == JenisEntri.WASTE) {
                    EntriManualRepository.laporWaste(
                        outletId = outletId,
                        bahanBakuId = bahan.bahanBakuId,
                        qtyBesar = qtyBesar,
                        alasan = s.alasan.trim(),
                        photoUrl = s.fotoUrl!!,
                        dilaporkanOleh = pengguna,
                    )
                } else {
                    LedgerRepository.tambahManual(
                        outletId = outletId,
                        createdBy = pengguna,
                        catatanGlobal = s.catatan.trim().ifBlank { s.alasan.trim() },
                        items = listOf(
                            LedgerRepository.ManualItem(
                                bahanBakuId = bahan.bahanBakuId,
                                tipe = s.jenis.nilai,
                                qtyAbs = qtyBesar,
                                catatan = s.alasan.trim().ifBlank { null },
                            )
                        ),
                    )
                }
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    menyimpan = false,
                    jumlah = "",
                    alasan = "",
                    catatan = "",
                    fotoUrl = null,
                    bahanTerpilih = null,
                    pesan = if (s.jenis == JenisEntri.WASTE) {
                        "Laporan waste terkirim dan menunggu persetujuan."
                    } else {
                        "${s.jenis.label} tersimpan. Saldo sudah diperbarui."
                    },
                )
                _state.value.outletTerpilih?.let { muatBahan(it.id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("EntriManualViewModel", "kirim() gagal", e)
                _state.value = _state.value.copy(menyimpan = false, error = stokErrorMessage(e))
            }
        }
    }
}

/** Saldo bahan siap tampil, dipakai layar sebagai konteks saat mengetik jumlah. */
internal fun MonitoringRow.saldoRingkas(): String =
    saldoNorm?.let { UnitScale.formatBerjenjang(it, meta) } ?: saldoTampil
