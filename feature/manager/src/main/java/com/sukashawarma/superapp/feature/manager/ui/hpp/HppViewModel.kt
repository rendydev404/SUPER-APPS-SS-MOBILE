package com.sukashawarma.superapp.feature.manager.ui.hpp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.manager.data.DataHpp
import com.sukashawarma.superapp.feature.manager.data.HppRepository
import com.sukashawarma.superapp.feature.manager.domain.KelompokMenu
import com.sukashawarma.superapp.feature.manager.domain.MenuHpp
import com.sukashawarma.superapp.feature.manager.domain.ResepMenu
import com.sukashawarma.superapp.feature.manager.domain.RingkasanHpp
import com.sukashawarma.superapp.feature.manager.domain.ringkasHpp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class TabHpp(val label: String) {
    ANALISIS("Analisis HPP"),
    RESEP("Resep BOM"),
}

data class HppUiState(
    val tab: TabHpp = TabHpp.ANALISIS,
    val data: DataHpp? = null,
    val pencarian: String = "",
    /** null = semua kelompok. */
    val kelompok: KelompokMenu? = null,
    /** Menu yang resepnya sedang dibuka. */
    val menuTerbuka: MenuHpp? = null,
    val memuat: Boolean = true,
    val galat: String? = null,
) {
    val semuaMenu: List<MenuHpp> get() = data?.menu.orEmpty()

    val menuTerlihat: List<MenuHpp>
        get() {
            val kueri = pencarian.trim().lowercase()
            return semuaMenu
                .filter { kelompok == null || it.kelompok == kelompok }
                .filter {
                    kueri.isEmpty() ||
                        it.nama.lowercase().contains(kueri) ||
                        it.kelompok.nama.lowercase().contains(kueri)
                }
        }

    /** Kelompok yang benar-benar punya menu, urut seperti di web. */
    val kelompokTersedia: List<KelompokMenu>
        get() = semuaMenu.map { it.kelompok }.distinct().sortedBy { it.urutan }

    fun jumlahDalamKelompok(k: KelompokMenu): Int = semuaMenu.count { it.kelompok == k }

    /** Ringkasan dihitung dari SELURUH menu, bukan hasil penyaring — angka
     *  ringkas yang ikut berubah saat mencari akan menyesatkan. */
    val ringkasan: RingkasanHpp get() = ringkasHpp(semuaMenu)

    val resepTerbuka: ResepMenu? get() = menuTerbuka?.let { data?.resep?.get(it.id) }
}

class HppViewModel : ViewModel() {

    private val _state = MutableStateFlow(HppUiState())
    val state: StateFlow<HppUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatUlang()
    }

    fun pilihTab(tab: TabHpp) {
        if (_state.value.tab == tab) return
        _state.value = _state.value.copy(tab = tab)
    }

    fun ubahPencarian(teks: String) {
        _state.value = _state.value.copy(pencarian = teks)
    }

    fun pilihKelompok(kelompok: KelompokMenu?) {
        _state.value = _state.value.copy(kelompok = kelompok)
    }

    fun bukaResep(menu: MenuHpp) {
        _state.value = _state.value.copy(menuTerbuka = menu)
    }

    fun tutupResep() {
        _state.value = _state.value.copy(menuTerbuka = null)
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, galat = null)
            try {
                val data = HppRepository.muat()
                _state.value = _state.value.copy(memuat = false, galat = null, data = data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("HppViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, galat = pesanGalat(e))
            }
        }
    }

    private fun pesanGalat(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat). Coba lagi."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        else -> "Gagal memuat data resep & HPP. Coba lagi."
    }
}
