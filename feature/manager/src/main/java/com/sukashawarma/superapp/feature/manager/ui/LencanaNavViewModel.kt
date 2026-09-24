package com.sukashawarma.superapp.feature.manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.data.CeklistHarianRepository
import com.sukashawarma.superapp.feature.manager.data.PersetujuanRepository
import com.sukashawarma.superapp.feature.manager.domain.meninjauCeklist
import com.sukashawarma.superapp.feature.manager.data.WasteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Angka lencana pada nav bawah dan lembar menu. */
data class LencanaNav(val persetujuan: Int = 0, val waste: Int = 0, val ceklist: Int = 0)

/**
 * Hitungan antrean yang menempel di navigasi, bukan di satu layar.
 *
 * Dipisahkan dari ViewModel layar karena umurnya berbeda: lencana harus tetap
 * benar saat pengguna sedang berada di layar lain, dan mematikannya bersama layar
 * Persetujuan akan membuat angka menghilang persis ketika ia paling berguna.
 *
 * Kegagalan sengaja ditelan — lencana adalah petunjuk tambahan, dan memunculkan
 * galat untuknya akan menutupi layar yang sedang dipakai.
 */
class LencanaNavViewModel : ViewModel() {

    private val _lencana = MutableStateFlow(LencanaNav())
    val lencana: StateFlow<LencanaNav> = _lencana

    init {
        muatUlang()
    }

    /** Dibatalkan setiap kali muat ulang baru dimulai: saat realtime beruntun,
     *  balasan lama yang tiba belakangan tidak boleh menimpa angka yang lebih baru. */
    private var pemuatan: Job? = null

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            try {
                coroutineScope {
                    // Lencana menghitung seluruh antrean layar Persetujuan, bukan hanya
                    // Void: tab Bypass POS ada di layar yang sama dan antreannya sama
                    // mendesaknya.
                    val persetujuan = async {
                        val data = PersetujuanRepository.muat()
                        data.void.size + data.bypass.size
                    }
                    val waste = async { WasteRepository.jumlahMenunggu(null) }
                    // Ceklist hari ini yang belum ditinjau — antrean milik peninjau
                    // saja. Bagi area manager angka ini bukan tugasnya, jadi nol.
                    // Galatnya ditelan DI DALAM async: anak yang gagal membatalkan
                    // seluruh coroutineScope dan ikut menghapus dua lencana lain.
                    val ceklist = async {
                        if (!meninjauCeklist(AppSession.staff.value?.role)) return@async 0
                        try {
                            CeklistHarianRepository.jumlahBelumDitinjau()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.e("LencanaNavViewModel", "jumlahBelumDitinjau() gagal", e)
                            0
                        }
                    }
                    val jumlahPersetujuan = persetujuan.await()
                    val jumlahWaste = waste.await()
                    val jumlahCeklist = ceklist.await()
                    _lencana.update { LencanaNav(jumlahPersetujuan, jumlahWaste, jumlahCeklist) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LencanaNavViewModel", "muatUlang() gagal", e)
            }
        }
    }
}
