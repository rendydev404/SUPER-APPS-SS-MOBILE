package com.sukashawarma.superapp.feature.manager.ui.laporan

import com.sukashawarma.superapp.feature.manager.domain.FilterChannel
import com.sukashawarma.superapp.feature.manager.domain.FilterPembayaran
import com.sukashawarma.superapp.feature.manager.domain.PresetLaporan
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.ZONA_JAKARTA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Kunci singgahan dan penanda periode berjalan.
 *
 * Keduanya yang menentukan apakah layar Laporan menembak jaringan atau menjawab
 * seketika dari hasil yang sudah ada.
 */
class LaporanSinggahanTest {

    private val hariIni = LocalDate.now(ZONA_JAKARTA)

    private fun state(
        preset: PresetLaporan = PresetLaporan.KUSTOM,
        kustom: RentangTanggal? = RentangTanggal(hariIni, hariIni),
        channel: FilterChannel = FilterChannel.SEMUA,
        pembayaran: FilterPembayaran = FilterPembayaran.SEMUA,
        outlet: String? = null,
    ) = LaporanUiState(
        preset = preset,
        kustom = kustom,
        channel = channel,
        pembayaran = pembayaran,
        outletTerpilih = outlet,
    )

    @Test
    fun `penyaring yang sama menghasilkan kunci yang sama`() {
        assertEquals(state().kunciMuat, state().kunciMuat)
    }

    @Test
    fun `tiap penyaring ikut membentuk kunci`() {
        val dasar = state().kunciMuat
        assertNotEquals(dasar, state(channel = FilterChannel.GOFOOD).kunciMuat)
        assertNotEquals(dasar, state(pembayaran = FilterPembayaran.QRIS).kunciMuat)
        assertNotEquals(dasar, state(outlet = "outlet-a").kunciMuat)
        assertNotEquals(dasar, state(kustom = RentangTanggal(hariIni.minusDays(6), hariIni)).kunciMuat)
    }

    @Test
    fun `periode yang mencakup hari ini masih bisa berubah`() {
        assertTrue(state(kustom = RentangTanggal(hariIni.minusDays(6), hariIni)).periodeBerjalan)
        assertTrue(state(preset = PresetLaporan.HARI_INI, kustom = null).periodeBerjalan)
        assertTrue(state(preset = PresetLaporan.TIGA_PULUH_HARI, kustom = null).periodeBerjalan)
    }

    @Test
    fun `periode yang sudah lewat tidak akan berubah lagi`() {
        // Pesanan baru selalu masuk hari ini, jadi rentang yang berakhir kemarin
        // adalah jawaban final — tidak perlu ditembak ulang ke jaringan.
        assertFalse(state(preset = PresetLaporan.KEMARIN, kustom = null).periodeBerjalan)
        assertFalse(
            state(kustom = RentangTanggal(hariIni.minusDays(30), hariIni.minusDays(1))).periodeBerjalan,
        )
    }
}
