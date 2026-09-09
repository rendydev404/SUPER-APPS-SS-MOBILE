package com.sukashawarma.superapp.feature.profil.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.profil.data.ProfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PesanProfil {
    data class Sukses(val teks: String) : PesanProfil
    data class Galat(val teks: String) : PesanProfil
}

data class ProfilUiState(
    val staff: StaffProfile? = null,
    val namaTampilan: String = "",
    val usernameTampilan: String = "",
    val menyimpanIdentitas: Boolean = false,
    val mengurusFoto: Boolean = false,
    val menggantiPassword: Boolean = false,
    val pesan: PesanProfil? = null,
) {
    /** Tombol simpan mati selama isian sama persis dengan yang sudah tersimpan —
     *  supaya tidak ada request yang tidak mengubah apa pun. */
    val identitasBerubah: Boolean
        get() = staff != null && (
            namaTampilan.trim() != (staff.displayName ?: "") ||
                usernameTampilan.trim() != (staff.displayUsername ?: "")
            )
}

/**
 * Layar "Profil Saya": nama tampilan, username tampilan, foto, dan ganti password.
 *
 * Yang TIDAK bisa diubah di sini, dan itu disengaja: nama kepegawaian (`name`),
 * username login, peran, dan outlet. Semuanya milik admin/HR lewat web — lihat
 * migration `20300208000000_profil_mandiri_staff.sql`.
 */
class ProfilViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProfilUiState())
    val state: StateFlow<ProfilUiState> = _state

    init {
        viewModelScope.launch {
            AppSession.staff.collect { staff ->
                val lama = _state.value
                _state.value = lama.copy(
                    staff = staff,
                    // Isian hanya disetel ulang ketika BUKAN sedang diketik pengguna,
                    // yaitu saat isian masih mencerminkan nilai tersimpan sebelumnya.
                    // Tanpa syarat ini, refreshStaff() yang datang di tengah pengetikan
                    // akan menghapus ketikan yang belum sempat disimpan.
                    namaTampilan = if (lama.identitasBerubah) lama.namaTampilan
                    else staff?.displayName.orEmpty(),
                    usernameTampilan = if (lama.identitasBerubah) lama.usernameTampilan
                    else staff?.displayUsername.orEmpty(),
                )
            }
        }
    }

    fun ubahNama(nilai: String) {
        _state.value = _state.value.copy(namaTampilan = nilai.take(40), pesan = null)
    }

    fun ubahUsername(nilai: String) {
        // Huruf besar diturunkan selagi diketik, bukan saat disimpan: server
        // menyimpannya huruf kecil, dan isian yang tidak ikut berubah setelah simpan
        // membuat pengguna mengira perubahannya tidak masuk.
        _state.value = _state.value.copy(usernameTampilan = nilai.lowercase().take(20), pesan = null)
    }

    fun bersihkanPesan() {
        if (_state.value.pesan != null) _state.value = _state.value.copy(pesan = null)
    }

    /** Cermin aturan di dalam RPC. Diperiksa di sini juga supaya salah ketik
     *  ketahuan sebelum menghabiskan satu perjalanan ke server. */
    private fun galatIdentitas(nama: String, username: String): String? = when {
        nama.isNotEmpty() && nama.length < 2 -> "Nama tampilan minimal 2 karakter."
        username.isNotEmpty() && !username.matches(Regex("^[a-z0-9._]{3,20}$")) ->
            "Username hanya boleh huruf, angka, titik, dan garis bawah, 3-20 karakter."
        else -> null
    }

    fun simpanIdentitas() {
        val kini = _state.value
        val nama = kini.namaTampilan.trim()
        val username = kini.usernameTampilan.trim()
        galatIdentitas(nama, username)?.let {
            _state.value = kini.copy(pesan = PesanProfil.Galat(it))
            return
        }
        _state.value = kini.copy(menyimpanIdentitas = true, pesan = null)
        viewModelScope.launch {
            try {
                // Keduanya selalu dikirim (string kosong = kosongkan), karena layar
                // ini memang menampilkan keduanya sekaligus: mengosongkan salah satu
                // adalah perubahan yang disengaja, bukan "jangan diubah".
                ProfilRepository.simpan(namaTampilan = nama, usernameTampilan = username)
                _state.value = _state.value.copy(
                    menyimpanIdentitas = false,
                    pesan = PesanProfil.Sukses("Profil tersimpan."),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    menyimpanIdentitas = false,
                    pesan = PesanProfil.Galat(e.message ?: "Gagal menyimpan profil."),
                )
            }
        }
    }

    fun simpanFoto(jpeg: ByteArray) {
        _state.value = _state.value.copy(mengurusFoto = true, pesan = null)
        viewModelScope.launch {
            try {
                val path = ProfilRepository.unggahAvatar(jpeg)
                ProfilRepository.simpan(avatarPath = path)
                _state.value = _state.value.copy(
                    mengurusFoto = false,
                    pesan = PesanProfil.Sukses("Foto profil diperbarui."),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    mengurusFoto = false,
                    pesan = PesanProfil.Galat(e.message ?: "Gagal mengunggah foto."),
                )
            }
        }
    }

    fun hapusFoto() {
        _state.value = _state.value.copy(mengurusFoto = true, pesan = null)
        viewModelScope.launch {
            try {
                // String kosong, bukan null: null berarti "jangan ubah" di RPC.
                ProfilRepository.simpan(avatarPath = "")
                _state.value = _state.value.copy(
                    mengurusFoto = false,
                    pesan = PesanProfil.Sukses("Foto profil dihapus."),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    mengurusFoto = false,
                    pesan = PesanProfil.Galat(e.message ?: "Gagal menghapus foto."),
                )
            }
        }
    }

    /** [onSukses] mengosongkan isian password di layar; nilainya tidak pernah masuk
     *  ke state supaya password tidak ikut tersimpan di ViewModel. */
    fun gantiPassword(baru: String, konfirmasi: String, onSukses: () -> Unit) {
        if (baru.length < 6) {
            _state.value = _state.value.copy(pesan = PesanProfil.Galat("Password minimal 6 karakter."))
            return
        }
        if (baru != konfirmasi) {
            _state.value = _state.value.copy(pesan = PesanProfil.Galat("Konfirmasi password tidak cocok."))
            return
        }
        _state.value = _state.value.copy(menggantiPassword = true, pesan = null)
        viewModelScope.launch {
            try {
                ProfilRepository.gantiPassword(baru)
                _state.value = _state.value.copy(
                    menggantiPassword = false,
                    pesan = PesanProfil.Sukses("Password berhasil diganti."),
                )
                onSukses()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    menggantiPassword = false,
                    pesan = PesanProfil.Galat(e.message ?: "Gagal terhubung ke server."),
                )
            }
        }
    }
}
