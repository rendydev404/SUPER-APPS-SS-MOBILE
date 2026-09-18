package com.sukashawarma.superapp.feature.stok.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Logika murni drop-ship vendor -> outlet.
 * Cermin `apps/stok/src/lib/stok/dropShip.ts` di web.
 */
enum class SatuanTingkat {
    BESAR,
    TENGAH,
    KECIL;

    companion object {
        fun fromString(s: String?): SatuanTingkat = when (s?.lowercase()?.trim()) {
            "kecil" -> KECIL
            "tengah" -> TENGAH
            else -> BESAR
        }
    }
}

interface FaktorBahan {
    val faktorTengah: Double?
    val faktorTampilan: Double?
}

data class FaktorBahanImpl(
    override val faktorTengah: Double?,
    override val faktorTampilan: Double?,
) : FaktorBahan

data class PilihanTanggal(
    val value: String, // 'YYYY-MM-DD'
    val label: String, // 'Hari ini', 'Kemarin', dst.
)

object DropShip {

    const val AMBANG_RUPIAH_KONFIRMASI = 2_000_000.0
    const val KELIPATAN_RATA_KONFIRMASI = 5.0

    private val ZONE_WIB = ZoneId.of("Asia/Jakarta")
    private val LABEL_HARI = listOf("Hari ini", "Kemarin", "2 hari lalu", "3 hari lalu")
    private val ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Konversi input crew ke SATUAN BESAR (kontrak kolom qty).
     * Melempar [IllegalArgumentException] bila tingkat yang dipilih tak punya faktor —
     * jangan diam-diam menganggap 1.
     */
    fun keSatuanBesar(qty: Double, tingkat: SatuanTingkat, b: FaktorBahan): Double {
        if (tingkat == SatuanTingkat.BESAR) return qty
        val faktor = if (tingkat == SatuanTingkat.KECIL) b.faktorTampilan else b.faktorTengah
        require(faktor != null && faktor > 0) {
            "Bahan ini tidak punya satuan ${tingkat.name.lowercase()}"
        }
        return qty / faktor
    }

    /**
     * Konfirmasi di form, bukan penolakan. Penolakan mutlak (> Rp 10 jt) ada di RPC.
     * Mengembalikan true jika:
     * 1. Total nilai rupiah > Rp 2.000.000, ATAU
     * 2. Qty > 5x rata-rata pemakaian harian 7 hari outlet.
     */
    fun perluKonfirmasiJumlah(
        qtyBesar: Double,
        hargaSnapshot: Double,
        rataPakaiHarian: Double?,
    ): Boolean {
        if (qtyBesar <= 0) return false
        if (qtyBesar * hargaSnapshot > AMBANG_RUPIAH_KONFIRMASI) return true
        if (rataPakaiHarian != null && rataPakaiHarian > 0) {
            return qtyBesar > KELIPATAN_RATA_KONFIRMASI * rataPakaiHarian
        }
        return false
    }

    /**
     * Format tanggal WIB (UTC+7) hari ini string 'YYYY-MM-DD'.
     */
    fun hariIniWib(): String {
        return LocalDate.now(ZONE_WIB).format(ISO_FORMATTER)
    }

    /**
     * Pilihan tanggal terima: hari ini s/d 3 hari lalu — SAMA dengan batas RPC catat_terima_vendor.
     */
    fun pilihanTanggalTerima(hariIni: LocalDate = LocalDate.now(ZONE_WIB)): List<PilihanTanggal> {
        return LABEL_HARI.mapIndexed { index, label ->
            val d = hariIni.minusDays(index.toLong())
            PilihanTanggal(
                value = d.format(ISO_FORMATTER),
                label = label,
            )
        }
    }
}
