package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role
import kotlin.math.ceil
import kotlin.math.max

/**
 * Aturan katalog form permintaan bahan — cermin penyaringan di
 * `apps/stok/src/components/permintaan/PermintaanForm.tsx`.
 *
 * Web menerapkan aturan yang sama di tiga tempat (allowedBahanBaku,
 * addAllCriticalItems, criticalItems); di sini disatukan supaya tidak
 * bisa saling menyimpang.
 */
object KatalogPermintaan {

    /**
     * Permintaan `menunggu` yang lebih muda dari ini menyembunyikan bahannya dari
     * katalog supaya tidak terduplikasi; yang lebih tua dibebaskan karena RPC
     * `buat_permintaan_svc` toh akan membatalkannya otomatis saat diajukan ulang.
     */
    const val STALE_JAM = 12

    /**
     * Role yang boleh meminta kategori BUMBU penuh — cermin `isKitchenRole` web
     * (`kitchen`, `admin_kitchen`, `admin`, `owner`, `developer`, `purchasing`;
     * `admin_kitchen` tidak ada di union Role sehingga tak dapat diwakili di sini).
     */
    private val ROLE_KATALOG_PENUH = setOf(
        Role.KITCHEN, Role.ADMIN, Role.OWNER, Role.DEVELOPER, Role.PURCHASING,
    )

    fun katalogPenuh(role: Role?): Boolean = role != null && role in ROLE_KATALOG_PENUH

    /**
     * Boleh tampil di katalog permintaan?
     * - ASET & PERLENGKAPAN (hardware) tidak lewat form bahan baku.
     * - PRINTER THERMAL / ID CARD dikecualikan by-name.
     * - BUMBU hanya untuk role dapur — kecuali BAWANG yang memang dipakai outlet.
     */
    fun bolehDiminta(kategori: String?, nama: String?, katalogPenuh: Boolean): Boolean {
        val kat = kategori?.trim()?.uppercase().orEmpty()
        val nm = nama?.trim()?.uppercase().orEmpty()
        if (kat == "ASET" || kat == "PERLENGKAPAN") return false
        if (nm == "PRINTER THERMAL" || nm == "ID CARD") return false
        if (!katalogPenuh && kat == "BUMBU" && nm != "BAWANG") return false
        return true
    }

    /**
     * Kekurangan menuju threshold pada SATUAN BESAR, minimal 1.
     *
     * Web menghitung `max(1, ceil(threshold - current_qty))` mentah-mentah padahal
     * `current_qty` bisa gram-scale (`saldo_is_gram`) sementara threshold selalu
     * besar-scale — untuk bahan berfaktor besar hasilnya meleset ribuan kali lipat.
     * Di sini saldo dinormalkan dulu ke satuan besar; itu perbaikan yang disengaja,
     * bukan penyimpangan.
     */
    fun kekuranganBesar(
        threshold: Double,
        currentQty: Double,
        saldoIsGram: Boolean,
        meta: UnitMeta,
    ): Double {
        val saldoBesar = DistribusiUnit.saldoKeBesar(currentQty, saldoIsGram, meta)
        return max(1.0, ceil(threshold - saldoBesar))
    }

    /** Saran jumlah pesan pada satuan besar, dibulatkan ke atas, minimal 1. */
    fun saranQty(kekuranganBesar: Double): Long =
        ceil(kekuranganBesar).toLong().coerceAtLeast(1L)

    /** Masih dalam jendela sembunyi 12 jam? `createdAtMs` null dianggap masih. */
    fun masihMenunggu(createdAtMs: Long?, nowMs: Long): Boolean {
        if (createdAtMs == null) return true
        return createdAtMs >= nowMs - STALE_JAM * 60L * 60L * 1000L
    }
}
