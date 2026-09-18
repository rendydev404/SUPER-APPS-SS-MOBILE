package com.sukashawarma.superapp.feature.manager.ui.persetujuan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.manager.data.PersetujuanRepository
import com.sukashawarma.superapp.feature.manager.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.manager.domain.PengajuanBypass
import com.sukashawarma.superapp.feature.manager.domain.PengajuanVoid
import com.sukashawarma.superapp.feature.manager.domain.PesananSelesaiItem
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.TabPersetujuan
import com.sukashawarma.superapp.feature.manager.domain.saringPeriode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PersetujuanUiState(
    val tab: TabPersetujuan = TabPersetujuan.VOID,
    val preset: PresetPeriode = PresetPeriode.HARI_INI,
    val kustom: RentangTanggal? = null,
    val semuaVoid: List<PengajuanVoid> = emptyList(),
    val semuaBypass: List<PengajuanBypass> = emptyList(),
    val memuat: Boolean = true,
    val sedangDiproses: Set<String> = emptySet(),
    val galat: String? = null,
    val kabar: String? = null,
    // Tab Pesanan Selesai (Batal Paksa)
    val daftarOutlet: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val kueriPencarian: String = "",
    val pesananSelesai: List<PesananSelesaiItem> = emptyList(),
    val memuatPesananSelesai: Boolean = false,
    val targetBatal: PesananSelesaiItem? = null,
    val catatanBatal: String = "",
    val sedangMembatalkan: Boolean = false,
) {
    val rentang: RentangTanggal get() = kustom ?: preset.rentang()
    val memakaiKustom: Boolean get() = kustom != null

    val void: List<PengajuanVoid> get() = saringPeriode(semuaVoid, rentang) { it.dibuatPada }
    val bypass: List<PengajuanBypass> get() = saringPeriode(semuaBypass, rentang) { it.dibuatPada }

    /** Lencana tab memakai jumlah SEBELUM penyaring periode, seperti web. */
    val jumlahVoid: Int get() = semuaVoid.size
    val jumlahBypass: Int get() = semuaBypass.size
}

class PersetujuanViewModel : ViewModel() {

    private val _state = MutableStateFlow(PersetujuanUiState())
    val state: StateFlow<PersetujuanUiState> = _state

    private var pemuatan: Job? = null
    private var jobPencarian: Job? = null
    private var jobPencarianPesanan: Job? = null

    init {
        muatUlang()
        muatOutletDanPesanan()
    }

    fun pilihTab(tab: TabPersetujuan) {
        if (_state.value.tab == tab) return
        _state.update { it.copy(tab = tab) }
        if (tab == TabPersetujuan.BATAL_PAKSA) {
            if (_state.value.daftarOutlet.isEmpty()) {
                muatOutletDanPesanan()
            } else if (_state.value.pesananSelesai.isEmpty() && _state.value.outletTerpilih != null) {
                cariPesananSelesai()
            }
        }
    }

    // Penyaring periode bekerja di memori untuk void/bypass; untuk pesanan selesai,
    // langsung memicu query ulang ke database.
    fun pilihPreset(preset: PresetPeriode) {
        _state.update { it.copy(preset = preset, kustom = null) }
        if (_state.value.tab == TabPersetujuan.BATAL_PAKSA) {
            cariPesananSelesai()
        }
    }

    fun pilihRentangKustom(dari: LocalDate, sampai: LocalDate) {
        val rentang = if (sampai.isBefore(dari)) RentangTanggal(sampai, dari) else RentangTanggal(dari, sampai)
        _state.update { it.copy(kustom = rentang) }
        if (_state.value.tab == TabPersetujuan.BATAL_PAKSA) {
            cariPesananSelesai()
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (_state.value.outletTerpilih?.id == outlet.id) return
        _state.update { it.copy(outletTerpilih = outlet) }
        cariPesananSelesai()
    }

    fun ubahKueri(kueri: String) {
        _state.update { it.copy(kueriPencarian = kueri) }
        jobPencarian?.cancel()
        jobPencarian = viewModelScope.launch {
            delay(400)
            cariPesananSelesai()
        }
    }

    fun muatOutletDanPesanan() {
        viewModelScope.launch {
            try {
                val daftar = PersetujuanRepository.outlets()
                val aktif = _state.value.outletTerpilih ?: daftar.firstOrNull()
                _state.update {
                    it.copy(
                        daftarOutlet = daftar,
                        outletTerpilih = it.outletTerpilih ?: aktif,
                    )
                }
                if (aktif != null && _state.value.tab == TabPersetujuan.BATAL_PAKSA) {
                    cariPesananSelesai()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanViewModel", "muatOutletDanPesanan() gagal", e)
            }
        }
    }

    /**
     * Kueri sebelumnya selalu dibatalkan supaya hasil pencarian usang (mis. dari outlet
     * atau kata kunci lama) tidak menimpa hasil kueri yang baru saja dimulai.
     */
    fun cariPesananSelesai() {
        val outlet = _state.value.outletTerpilih ?: return
        jobPencarianPesanan?.cancel()
        jobPencarianPesanan = viewModelScope.launch {
            _state.update { it.copy(memuatPesananSelesai = true) }
            try {
                val hasil = PersetujuanRepository.cariPesananSelesai(
                    outletId = outlet.id,
                    kueri = _state.value.kueriPencarian,
                    rentang = _state.value.rentang,
                )
                _state.update {
                    it.copy(
                        pesananSelesai = hasil,
                        memuatPesananSelesai = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanViewModel", "cariPesananSelesai() gagal", e)
                _state.update {
                    it.copy(
                        memuatPesananSelesai = false,
                        galat = pesanGalat(e),
                    )
                }
            }
        }
    }

    fun bukaDialogBatal(pesanan: PesananSelesaiItem) {
        _state.update { it.copy(targetBatal = pesanan, catatanBatal = "") }
    }

    fun tutupDialogBatal() {
        _state.update { it.copy(targetBatal = null, catatanBatal = "", sedangMembatalkan = false) }
    }

    fun ubahCatatanBatal(catatan: String) {
        _state.update { it.copy(catatanBatal = catatan) }
    }

    fun eksekusiBatalPaksa() {
        val target = _state.value.targetBatal ?: return
        val catatan = _state.value.catatanBatal.trim()
        if (catatan.isEmpty()) {
            _state.update { it.copy(galat = "Catatan pembatalan wajib diisi.") }
            return
        }
        // Kunci dipasang sebelum coroutine dimulai supaya ketukan ganda yang cepat
        // tidak sempat mengirim dua RPC pembatalan.
        var lolos = false
        _state.update {
            lolos = !it.sedangMembatalkan
            if (lolos) it.copy(sedangMembatalkan = true) else it
        }
        if (!lolos) return

        viewModelScope.launch {
            try {
                val error = PersetujuanRepository.batalPaksaPesanan(
                    orderId = target.id,
                    outletId = target.outletId,
                    alasan = catatan,
                )
                if (error != null) {
                    _state.update { it.copy(galat = error, sedangMembatalkan = false) }
                } else {
                    _state.update {
                        it.copy(
                            pesananSelesai = it.pesananSelesai.filterNot { p -> p.id == target.id },
                            targetBatal = null,
                            catatanBatal = "",
                            sedangMembatalkan = false,
                            kabar = "Pesanan #${target.nomorOrder} berhasil dibatalkan.",
                        )
                    }
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(sedangMembatalkan = false) }
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanViewModel", "eksekusiBatalPaksa() gagal", e)
                _state.update { it.copy(galat = pesanGalat(e), sedangMembatalkan = false) }
            }
        }
    }

    fun tutupKabar() {
        _state.update { it.copy(kabar = null, galat = null) }
    }

    fun muatUlang(silent: Boolean = false) {
        val sudahAdaData = _state.value.semuaVoid.isNotEmpty() || _state.value.semuaBypass.isNotEmpty()
        val senyap = silent || sudahAdaData
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            if (!senyap) {
                _state.update { it.copy(memuat = true, galat = null) }
            }
            try {
                val data = PersetujuanRepository.muat()
                _state.update {
                    it.copy(
                        memuat = false,
                        galat = null,
                        semuaVoid = data.void,
                        semuaBypass = data.bypass,
                    )
                }
                if (_state.value.tab == TabPersetujuan.BATAL_PAKSA && _state.value.outletTerpilih != null) {
                    cariPesananSelesai()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanViewModel", "muatUlang() gagal", e)
                if (!senyap) {
                    _state.update { it.copy(memuat = false, galat = pesanGalat(e)) }
                }
            } finally {
                _state.update { if (it.memuat) it.copy(memuat = false) else it }
            }
        }
    }

    fun prosesBypass(pengajuan: PengajuanBypass, setujui: Boolean) {
        // Kunci dipasang di luar coroutine supaya ketukan ganda tidak memicu RPC dua kali.
        if (!kunciProses(pengajuan.id)) return
        viewModelScope.launch {
            try {
                val kalah = PersetujuanRepository.prosesBypass(pengajuan.id, setujui)
                if (kalah != null) {
                    _state.update { it.copy(galat = kalah) }
                } else {
                    _state.update {
                        it.copy(
                            // Baris dibuang lebih dulu supaya tombol tidak bisa ditekan dua
                            // kali selagi pemuatan ulang masih berjalan.
                            semuaBypass = it.semuaBypass.filterNot { b -> b.id == pengajuan.id },
                            kabar = if (setujui) {
                                "Pengajuan bypass POS disetujui."
                            } else {
                                "Pengajuan bypass POS ditolak."
                            },
                        )
                    }
                }
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanViewModel", "prosesBypass() gagal", e)
                _state.update { it.copy(galat = pesanGalat(e)) }
            } finally {
                bukaKunciProses(pengajuan.id)
            }
        }
    }

    /**
     * Bentuknya sengaja sama persis dengan [prosesBypass]: baris dibuang lebih dulu
     * supaya tombolnya tidak bisa ditekan dua kali selagi pemuatan ulang berjalan,
     * lalu antrean dimuat ulang agar cocok dengan keadaan server.
     */
    fun prosesVoid(pengajuan: PengajuanVoid, setujui: Boolean) {
        if (!kunciProses(pengajuan.id)) return
        viewModelScope.launch {
            try {
                val kalah = PersetujuanRepository.prosesVoid(pengajuan.id, setujui)
                if (kalah != null) {
                    _state.update { it.copy(galat = kalah) }
                } else {
                    _state.update {
                        it.copy(
                            semuaVoid = it.semuaVoid.filterNot { v -> v.id == pengajuan.id },
                            kabar = if (setujui) {
                                "Pembatalan ${pengajuan.nomorOrder} disetujui."
                            } else {
                                "Pembatalan ${pengajuan.nomorOrder} ditolak."
                            },
                        )
                    }
                }
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanViewModel", "prosesVoid() gagal", e)
                _state.update { it.copy(galat = pesanGalat(e)) }
            } finally {
                bukaKunciProses(pengajuan.id)
            }
        }
    }

    /** Menandai satu id sedang diproses; mengembalikan false bila sudah terkunci. */
    private fun kunciProses(id: String): Boolean {
        var lolos = false
        _state.update {
            lolos = id !in it.sedangDiproses
            if (lolos) it.copy(sedangDiproses = it.sedangDiproses + id) else it
        }
        return lolos
    }

    private fun bukaKunciProses(id: String) {
        _state.update { it.copy(sedangDiproses = it.sedangDiproses - id) }
    }

    private fun pesanGalat(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat). Coba lagi."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        else -> "Gagal memuat antrean persetujuan. Coba lagi."
    }
}
