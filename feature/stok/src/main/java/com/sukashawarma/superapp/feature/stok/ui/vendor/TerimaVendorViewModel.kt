package com.sukashawarma.superapp.feature.stok.ui.vendor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.BahanDropShip
import com.sukashawarma.superapp.feature.stok.data.CatatanTerimaVendor
import com.sukashawarma.superapp.feature.stok.data.InfoVendorDropShip
import com.sukashawarma.superapp.feature.stok.data.TerimaVendorRepository
import com.sukashawarma.superapp.feature.stok.domain.DropShip
import com.sukashawarma.superapp.feature.stok.domain.PilihanTanggal
import com.sukashawarma.superapp.feature.stok.domain.SatuanTingkat
import com.sukashawarma.superapp.feature.stok.domain.StokAkses
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TerimaVendorUiState(
    val memuatBahan: Boolean = true,
    val memuatVendor: Boolean = false,
    val memuatRiwayat: Boolean = false,
    val menyimpan: Boolean = false,
    val mengunggahFoto: Boolean = false,
    val kameraTerbuka: Boolean = false,
    val daftarBahan: List<BahanDropShip> = emptyList(),
    val bahanTerpilih: BahanDropShip? = null,
    val daftarVendor: List<InfoVendorDropShip> = emptyList(),
    val vendorTerpilih: InfoVendorDropShip? = null,
    val tanggalTerpilih: String = "",
    val pilihanTanggal: List<PilihanTanggal> = emptyList(),
    val tingkatSatuan: SatuanTingkat = SatuanTingkat.BESAR,
    val qtyInput: String = "",
    val catatanInput: String = "",
    val fotoBytes: ByteArray? = null,
    val fotoUrl: String? = null,
    val konfirmasiLonjakan: Boolean = false,
    val riwayat: List<CatatanTerimaVendor> = emptyList(),
    val koreksiTarget: CatatanTerimaVendor? = null,
    val koreksiQtyInput: String = "",
    val koreksiMenyimpan: Boolean = false,
    val pesanSukses: String? = null,
    val pesanError: String? = null,
    val pesanPeringatan: String? = null,
) {
    val konversiResult: Pair<Double, String?>
        get() {
            val bahan = bahanTerpilih ?: return 0.0 to null
            val n = qtyInput.toDoubleOrNull() ?: return 0.0 to null
            if (n <= 0.0) return 0.0 to null
            return try {
                DropShip.keSatuanBesar(n, tingkatSatuan, bahan) to null
            } catch (e: Exception) {
                0.0 to (e.message ?: "Satuan tidak valid")
            }
        }

    val qtyBesar: Double get() = konversiResult.first
    val konversiError: String? get() = konversiResult.second

    val nilaiRupiah: Double
        get() = (vendorTerpilih?.hargaSnapshot ?: 0.0) * qtyBesar

    val butuhKonfirmasi: Boolean
        get() {
            val v = vendorTerpilih ?: return false
            return DropShip.perluKonfirmasiJumlah(
                qtyBesar = qtyBesar,
                hargaSnapshot = v.hargaSnapshot,
                rataPakaiHarian = v.rataPakaiHarian,
            )
        }

    val bolehSimpan: Boolean
        get() = !menyimpan &&
            bahanTerpilih != null &&
            vendorTerpilih != null &&
            qtyBesar > 0.0 &&
            konversiError == null &&
            tanggalTerpilih.isNotBlank() &&
            (!butuhKonfirmasi || konfirmasiLonjakan)
}

class TerimaVendorViewModel : ViewModel() {

    private val _state = MutableStateFlow(TerimaVendorUiState())
    val state: StateFlow<TerimaVendorUiState> = _state.asStateFlow()

    private val zoneWib = ZoneId.of("Asia/Jakarta")
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        val hariIni = LocalDate.now(zoneWib)
        val tglStr = hariIni.format(isoFormatter)
        val pilihan = DropShip.pilihanTanggalTerima(hariIni)
        _state.update {
            it.copy(
                tanggalTerpilih = tglStr,
                pilihanTanggal = pilihan,
            )
        }
        muatDataAwal()
    }

    fun muatDataAwal() {
        muatBahan()
        muatRiwayat()
    }

    private fun muatBahan() {
        viewModelScope.launch {
            _state.update { it.copy(memuatBahan = true, pesanError = null) }
            try {
                val daftar = TerimaVendorRepository.daftarBahanDropShip()
                _state.update {
                    it.copy(
                        memuatBahan = false,
                        daftarBahan = daftar,
                    )
                }
                if (daftar.size == 1) {
                    pilihBahan(daftar.first())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        memuatBahan = false,
                        pesanError = stokErrorMessage(e),
                    )
                }
            }
        }
    }

    fun muatRiwayat() {
        viewModelScope.launch {
            _state.update { it.copy(memuatRiwayat = true) }
            try {
                val hariIni = LocalDate.now(zoneWib)
                val dari = hariIni.minusDays(6).format(isoFormatter)
                val sampai = hariIni.format(isoFormatter)
                val daftar = TerimaVendorRepository.riwayatTerimaVendorSaya(dari, sampai)
                _state.update {
                    it.copy(
                        memuatRiwayat = false,
                        riwayat = daftar,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        memuatRiwayat = false,
                        pesanError = stokErrorMessage(e),
                    )
                }
            }
        }
    }

    fun pilihBahan(bahan: BahanDropShip) {
        _state.update {
            it.copy(
                bahanTerpilih = bahan,
                vendorTerpilih = null,
                daftarVendor = emptyList(),
                tingkatSatuan = SatuanTingkat.BESAR,
                qtyInput = "",
                konfirmasiLonjakan = false,
                pesanError = null,
            )
        }
        muatVendorUntukBahan(bahan.id)
    }

    private fun muatVendorUntukBahan(bahanId: String) {
        viewModelScope.launch {
            _state.update { it.copy(memuatVendor = true) }
            try {
                val vendors = TerimaVendorRepository.infoTerimaVendor(bahanId)
                _state.update {
                    it.copy(
                        memuatVendor = false,
                        daftarVendor = vendors,
                        vendorTerpilih = if (vendors.size == 1) vendors.first() else null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        memuatVendor = false,
                        pesanError = stokErrorMessage(e),
                    )
                }
            }
        }
    }

    fun pilihVendor(vendor: InfoVendorDropShip) {
        _state.update {
            it.copy(vendorTerpilih = vendor, konfirmasiLonjakan = false)
        }
    }

    fun pilihTanggal(tanggal: String) {
        _state.update { it.copy(tanggalTerpilih = tanggal) }
    }

    fun pilihTingkatSatuan(tingkat: SatuanTingkat) {
        _state.update { it.copy(tingkatSatuan = tingkat, konfirmasiLonjakan = false) }
    }

    fun ubahQty(input: String) {
        _state.update { it.copy(qtyInput = input, konfirmasiLonjakan = false) }
    }

    fun ubahCatatan(input: String) {
        _state.update { it.copy(catatanInput = input) }
    }

    fun ubahKonfirmasi(centang: Boolean) {
        _state.update { it.copy(konfirmasiLonjakan = centang) }
    }

    fun bukaKamera() {
        _state.update { it.copy(kameraTerbuka = true) }
    }

    fun tutupKamera() {
        _state.update { it.copy(kameraTerbuka = false) }
    }

    fun simpanFoto(jpegBytes: ByteArray) {
        _state.update {
            it.copy(
                kameraTerbuka = false,
                fotoBytes = jpegBytes,
                pesanPeringatan = null,
            )
        }
    }

    fun hapusFoto() {
        _state.update { it.copy(fotoBytes = null, fotoUrl = null) }
    }

    fun simpan() {
        var lanjut = false
        _state.update { current ->
            if (!current.bolehSimpan || current.menyimpan) {
                current
            } else {
                lanjut = true
                current.copy(menyimpan = true, pesanError = null, pesanPeringatan = null)
            }
        }
        if (!lanjut) return

        val outletId = AppSession.staff.value?.outletId
        if (outletId.isNullOrBlank()) {
            _state.update { it.copy(menyimpan = false, pesanError = "Akun tidak terhubung ke outlet mana pun.") }
            return
        }

        val s = _state.value
        viewModelScope.launch {
            var urlFoto: String? = null

            // Unggah foto jika ada
            if (s.fotoBytes != null) {
                _state.update { it.copy(mengunggahFoto = true) }
                try {
                    urlFoto = TerimaVendorRepository.unggahFotoBukti(outletId, s.fotoBytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    // Sesuai prinsip web: foto opsional, gagal upload tidak menahan pencatatan stok
                    _state.update {
                        it.copy(
                            pesanPeringatan = "Foto gagal diunggah (${e.message}), catatan tetap disimpan tanpa foto.",
                        )
                    }
                } finally {
                    _state.update { it.copy(mengunggahFoto = false) }
                }
            }

            try {
                val bahan = s.bahanTerpilih!!
                val vendor = s.vendorTerpilih!!
                val qtyBesar = s.qtyBesar

                TerimaVendorRepository.catatTerimaVendor(
                    bahanBakuId = bahan.id,
                    supplierId = vendor.supplierId,
                    qtyBesar = qtyBesar,
                    tanggal = s.tanggalTerpilih,
                    catatan = s.catatanInput,
                    fotoUrl = urlFoto,
                )

                _state.update {
                    it.copy(
                        menyimpan = false,
                        qtyInput = "",
                        catatanInput = "",
                        fotoBytes = null,
                        fotoUrl = null,
                        konfirmasiLonjakan = false,
                        pesanSukses = "Tercatat $qtyBesar ${bahan.satuan} ${bahan.nama} — stok outlet sudah bertambah",
                    )
                }
                muatRiwayat()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        menyimpan = false,
                        pesanError = stokErrorMessage(e),
                    )
                }
            }
        }
    }

    fun bukaDialogKoreksi(catatan: CatatanTerimaVendor) {
        _state.update {
            it.copy(
                koreksiTarget = catatan,
                koreksiQtyInput = catatan.qty.toString(),
                pesanError = null,
            )
        }
    }

    fun tutupDialogKoreksi() {
        _state.update {
            it.copy(
                koreksiTarget = null,
                koreksiQtyInput = "",
                koreksiMenyimpan = false,
            )
        }
    }

    fun ubahKoreksiQty(input: String) {
        _state.update { it.copy(koreksiQtyInput = input) }
    }

    fun simpanKoreksi() {
        var lanjut = false
        _state.update { current ->
            if (current.koreksiMenyimpan || current.koreksiTarget == null) {
                current
            } else {
                lanjut = true
                current.copy(koreksiMenyimpan = true, pesanError = null)
            }
        }
        if (!lanjut) return

        val s = _state.value
        val target = s.koreksiTarget ?: run {
            _state.update { it.copy(koreksiMenyimpan = false) }
            return
        }
        val n = s.koreksiQtyInput.toDoubleOrNull()
        if (n == null || n <= 0) {
            _state.update { it.copy(koreksiMenyimpan = false, pesanError = "Jumlah harus lebih dari 0") }
            return
        }

        viewModelScope.launch {
            try {
                TerimaVendorRepository.koreksiTerimaVendor(target.id, n)
                _state.update {
                    it.copy(
                        koreksiTarget = null,
                        koreksiQtyInput = "",
                        koreksiMenyimpan = false,
                        pesanSukses = "Dikoreksi — stok ikut menyesuaikan",
                    )
                }
                muatRiwayat()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        koreksiMenyimpan = false,
                        pesanError = stokErrorMessage(e),
                    )
                }
            }
        }
    }

    fun bersihkanPesan() {
        _state.update {
            it.copy(
                pesanSukses = null,
                pesanError = null,
                pesanPeringatan = null,
            )
        }
    }
}
