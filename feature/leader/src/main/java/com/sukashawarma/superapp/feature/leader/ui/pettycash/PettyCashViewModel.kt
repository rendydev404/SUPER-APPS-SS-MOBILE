package com.sukashawarma.superapp.feature.leader.ui.pettycash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.leader.data.OutletLeader
import com.sukashawarma.superapp.feature.leader.data.OutletLeaderRepository
import com.sukashawarma.superapp.feature.leader.data.PengajuanRepository
import com.sukashawarma.superapp.feature.leader.domain.FilterPengajuan
import com.sukashawarma.superapp.feature.leader.domain.FormTopup
import com.sukashawarma.superapp.feature.leader.domain.Pengajuan
import com.sukashawarma.superapp.feature.leader.domain.STATUS_BUTUH_AKSI
import com.sukashawarma.superapp.feature.leader.domain.STATUS_MENUNGGU_AM
import com.sukashawarma.superapp.feature.leader.domain.STATUS_SELESAI
import com.sukashawarma.superapp.feature.leader.domain.galatForm
import com.sukashawarma.superapp.feature.leader.domain.saringPengajuan
import com.sukashawarma.superapp.feature.leader.ui.pesanGalatAksi
import com.sukashawarma.superapp.feature.leader.ui.pesanGalatMuat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PettyCashUiState(
    val cabang: List<OutletLeader> = emptyList(),
    val semua: List<Pengajuan> = emptyList(),
    val filter: FilterPengajuan = FilterPengajuan.SEMUA,
    val form: FormTopup = FormTopup(),
    val formTerbuka: Boolean = false,
    val mengirim: Boolean = false,
    val memuat: Boolean = true,
    val sedangDiproses: Set<String> = emptySet(),
    val buktiTerbuka: String? = null,
    val kabar: String? = null,
    val galat: String? = null,
) {
    val terlihat: List<Pengajuan> get() = saringPengajuan(semua, filter)

    // Angka pada pil filter dihitung dari daftar penuh, bukan dari hasil saringan —
    // kalau tidak, tiap pil akan menampilkan cacah pil yang sedang aktif.
    val jumlahSemua: Int get() = semua.size
    val jumlahButuhAksi: Int get() = semua.count { it.status in STATUS_BUTUH_AKSI }
    val jumlahMenungguAm: Int get() = semua.count { it.status in STATUS_MENUNGGU_AM }
    val jumlahSelesai: Int get() = semua.count { it.status in STATUS_SELESAI }
}

/**
 * Top Up Petty Cash — cermin `app/dashboard/leader/petty-cash/page.tsx` web.
 */
class PettyCashViewModel : ViewModel() {

    private val _state = MutableStateFlow(PettyCashUiState())
    val state: StateFlow<PettyCashUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatCabang()
        muatUlang()
    }

    fun pilihFilter(filter: FilterPengajuan) {
        // Penyaring bekerja di memori: seratus baris terbaru sudah di tangan, jadi
        // berganti pil tidak menyentuh jaringan sama sekali.
        if (_state.value.filter == filter) return
        _state.value = _state.value.copy(filter = filter)
    }

    fun bukaForm(terbuka: Boolean) {
        _state.value = _state.value.copy(formTerbuka = terbuka)
    }

    fun ubahForm(form: FormTopup) {
        _state.value = _state.value.copy(form = form)
    }

    /**
     * Mengganti outlet ikut mengisi ulang kolom rekening dari data cabang itu.
     *
     * Rekening milik cabang, bukan milik form: membiarkan nomor rekening cabang A
     * tertinggal di form saat pengguna berpindah ke cabang B akan menuliskannya ke
     * baris `outlets` cabang B lewat RPC — dan sejak itu setiap transfer ke B masuk
     * ke rekening A.
     */
    fun pilihOutlet(outletId: String) {
        val outlet = _state.value.cabang.find { it.id == outletId } ?: return
        _state.value = _state.value.copy(
            form = _state.value.form.copy(
                outletId = outlet.id,
                namaBank = outlet.namaBank.orEmpty(),
                nomorRekening = outlet.nomorRekening.orEmpty(),
                atasNama = outlet.atasNama.orEmpty(),
            ),
        )
    }

    fun bukaBukti(url: String?) {
        _state.value = _state.value.copy(buktiTerbuka = url)
    }

    fun tutupKabar() {
        _state.value = _state.value.copy(kabar = null, galat = null)
    }

    private fun muatCabang() {
        viewModelScope.launch {
            try {
                val ids = OutletLeaderRepository.idTerakses()
                val daftar = OutletLeaderRepository.cabang(ids)
                _state.value = _state.value.copy(cabang = daftar)
                // Cabang pertama dipilih otomatis supaya form langsung bisa diisi;
                // pilihan pengguna yang sudah ada tidak ditimpa.
                if (_state.value.form.outletId == null) {
                    daftar.firstOrNull()?.let { pilihOutlet(it.id) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LeaderPettyCashViewModel", "muatCabang() gagal", e)
            }
        }
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true)
            try {
                // Tanpa penyaring outlet: RLS `petty_cash_topups` sudah membatasi
                // barisnya ke cabang terakses, dan leader memang perlu melihat
                // seluruh cabang binaannya dalam satu daftar — sama seperti web.
                val daftar = PengajuanRepository.daftar(outletId = null)
                _state.value = _state.value.copy(memuat = false, semua = daftar)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LeaderPettyCashViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = pesanGalatMuat(e, "daftar pengajuan"),
                )
            }
        }
    }

    /** Mengirim pengajuan baru. Validasi form dijalankan lebih dulu supaya galat yang
     *  bisa dijelaskan tidak perlu menempuh perjalanan ke server untuk ditolak di sana. */
    fun kirim() {
        val form = _state.value.form
        galatForm(form)?.let { pesan ->
            _state.value = _state.value.copy(galat = pesan)
            return
        }
        if (_state.value.mengirim) return

        viewModelScope.launch {
            _state.value = _state.value.copy(mengirim = true)
            try {
                PengajuanRepository.ajukan(form)
                _state.value = _state.value.copy(
                    mengirim = false,
                    formTerbuka = false,
                    // Nominal dan keperluan dikosongkan, rekening TIDAK: itu milik
                    // cabang dan akan sama pada pengajuan berikutnya.
                    form = form.copy(nominal = "", keperluan = ""),
                    kabar = "Pengajuan top up terkirim ke Area Manager.",
                )
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LeaderPettyCashViewModel", "kirim() gagal", e)
                _state.value = _state.value.copy(
                    mengirim = false,
                    galat = pesanGalatAksi(e, "mengirim pengajuan"),
                )
            }
        }
    }

    /**
     * Menyerahkan dana ke crew.
     *
     * Tidak ada pembaruan optimistis: status berikutnya ditentukan RPC — yang bisa
     * juga menolak karena pengajuan sudah berpindah tahap di tempat lain — jadi
     * menebaknya di layar berisiko menampilkan keadaan yang tak pernah terjadi.
     */
    fun serahkan(pengajuan: Pengajuan) {
        if (pengajuan.id in _state.value.sedangDiproses) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                sedangDiproses = _state.value.sedangDiproses + pengajuan.id,
            )
            try {
                PengajuanRepository.serahkanKeCrew(pengajuan.id)
                _state.value = _state.value.copy(
                    kabar = "Dana diserahkan ke crew. Saldo petty cash outlet bertambah.",
                )
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LeaderPettyCashViewModel", "serahkan() gagal", e)
                _state.value = _state.value.copy(
                    galat = pesanGalatAksi(e, "menyerahkan dana"),
                )
            } finally {
                _state.value = _state.value.copy(
                    sedangDiproses = _state.value.sedangDiproses - pengajuan.id,
                )
            }
        }
    }
}
