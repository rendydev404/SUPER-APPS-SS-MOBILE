package com.sukashawarma.superapp.feature.profil.ui

import android.graphics.Bitmap
import com.sukashawarma.superapp.core.camera.keJpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            usernameTampilan.trim() != (staff.displayUsername ?: "")
        )
}

/**
 * Layar "Profil Saya": username tampilan, foto, dan ganti password.
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
                    usernameTampilan = if (lama.identitasBerubah) lama.usernameTampilan
                    else staff?.displayUsername.orEmpty(),
                )
            }
        }
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
    private fun galatUsername(username: String): String? = when {
        username.isNotEmpty() && !username.matches(Regex("^[a-z0-9._]{3,20}$")) ->
            "Username hanya boleh huruf, angka, titik, dan garis bawah, 3-20 karakter."
        else -> null
    }

    fun simpanIdentitas() {
        val kini = _state.value
        val username = kini.usernameTampilan.trim()
        galatUsername(username)?.let {
            _state.value = kini.copy(pesan = PesanProfil.Galat(it))
            return
        }
        _state.value = kini.copy(menyimpanIdentitas = true, pesan = null)
        viewModelScope.launch {
            try {
                // Ketika username disimpan, kita kirim username ke namaTampilan dan usernameTampilan.
                // Supabase update_my_profile mengisi display_name dan display_username dengan username,
                // sehingga di seluruh chat (chat_messages_fill_sender) dan profil nama yang tampil adalah username.
                ProfilRepository.simpan(namaTampilan = username, usernameTampilan = username)
                _state.value = _state.value.copy(
                    menyimpanIdentitas = false,
                    pesan = PesanProfil.Sukses("Username tersimpan."),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    menyimpanIdentitas = false,
                    pesan = PesanProfil.Galat(e.message ?: "Gagal menyimpan username."),
                )
            }
        }
    }

    /**
     * Menyimpan foto profil baru dalam dua ukuran: [SISI_AVATAR_KECIL] untuk avatar di
     * daftar (ringan) dan [SISI_AVATAR_HD] untuk dibuka layar penuh (tajam).
     * Kompresi berjalan di Dispatchers.Default — dua kali encode JPEG sampai 1280px
     * cukup berat untuk membuat UI tersendat bila dilakukan di thread utama.
     */
    fun simpanFoto(foto: Bitmap) {
        _state.value = _state.value.copy(mengurusFoto = true, pesan = null)
        viewModelScope.launch {
            try {
                val (jpeg, jpegHd) = withContext(Dispatchers.Default) {
                    foto.keJpeg(SISI_AVATAR_KECIL, 82) to foto.keJpeg(SISI_AVATAR_HD, 82)
                }
                val path = ProfilRepository.unggahAvatar(jpeg, jpegHd)
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

/** Avatar di daftar & kartu: cukup tajam sampai ~120dp. */
private const val SISI_AVATAR_KECIL = 512
/**
 * Foto profil yang dibuka layar penuh ("ultra HD"): lebih lebar dari layar HP umum
 * (720-1440px) supaya tetap tajam saat di-zoom. JPEG mutu 82 di ukuran ini ~200-300 KB,
 * dan hanya diunduh saat foto dibuka besar - daftar tetap memakai versi 512px.
 * Bucket `avatars` hanya menerima image/jpeg (batas 2 MB), jadi bukan WebP.
 */
private const val SISI_AVATAR_HD = 1600
