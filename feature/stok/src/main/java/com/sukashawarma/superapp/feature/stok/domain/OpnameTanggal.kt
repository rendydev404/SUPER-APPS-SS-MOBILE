package com.sukashawarma.superapp.feature.stok.domain

/**
 * Satu aturan pengalihan tanggal opname.
 *
 * Artinya: bila hari ini salah satu dari [berlakuPada] dan outletnya [outletId],
 * opname dicatat bertanggal [tanggalTujuan] — TETAPI hanya kalau outlet itu belum
 * punya [minimalFinalized] opname finalized pada tanggal tujuan tersebut. Begitu
 * kuotanya terpenuhi, tanggalnya kembali normal.
 */
data class PengalihanTanggalOpname(
    val outletId: String,
    val namaOutlet: String,
    val berlakuPada: Set<String>,
    val tanggalTujuan: String,
    val minimalFinalized: Int = 1,
)

/**
 * Jatah opname lebih dari sekali sehari untuk satu outlet pada tanggal tertentu.
 *
 * [berlakuPada] null berarti berlaku pada tanggal berapa pun — dipakai outlet tes.
 */
data class JatahOpnameGanda(
    val outletId: String,
    val namaOutlet: String,
    val berlakuPada: Set<String>?,
    val maksimal: Int,
)

/**
 * Pengecualian tanggal opname — port `getEffectiveTodayWIB` dan blok pengecualian di
 * `createOrReuseOpnameDraftAction` (`apps/stok/src/lib/stok/opnameDate.ts` dan
 * `app/actions/opname.ts`).
 *
 * Ini murni tambalan operasional: ketika sebuah outlet lupa atau gagal opname pada
 * hari H, kantor membuka jalan agar hitungan hari itu tetap bisa dimasukkan keesokan
 * harinya, bertanggal mundur, supaya ledger tidak bolong. Karena itu isinya deretan
 * UUID dan tanggal mati, dan memang begitu di web.
 *
 * Sebelumnya native sengaja TIDAK memport ini dengan alasan "semua tanggalnya sudah
 * lewat". Alasan itu kedaluwarsa: web menambah dua aturan baru sesudahnya (Cicurug
 * dan lima outlet susulan 5 September), dan aturan Cicurug menyebut tanggal yang
 * masih di depan. Sebuah outlet yang mendapat pengalihan di web tetapi tidak di
 * native akan menghasilkan DUA baris ledger bertanggal berbeda untuk satu hitungan
 * yang sama — persis kerusakan yang tambalan ini hendak cegah.
 *
 * Aturan yang tanggalnya sudah lewat sengaja tetap disimpan, bukan dibuang: nilainya
 * ada pada saat opname lama dibaca ulang atau dibandingkan dengan ledger, dan
 * membuang aturan mati membuat riwayatnya tak bisa dijelaskan lagi.
 */
object OpnameTanggal {

    private const val CILEUNGSI = "62a56103-2085-4dd5-9d25-a3c0cffc88ff"
    private const val EMPANG = "550e8400-e29b-41d4-a716-446655440002"
    private const val PALEDANG = "550e8400-e29b-41d4-a716-446655440003"
    private const val JATIWARINGIN = "550e8400-e29b-41d4-a716-446655440010"
    private const val CICURUG = "d9a2ef93-c298-4501-a471-1c5e2b3dff08"
    private const val CIBINONG = "550e8400-e29b-41d4-a716-446655440014"
    private const val PEKAYON = "550e8400-e29b-41d4-a716-446655440018"
    private const val BNR = "550e8400-e29b-41d4-a716-446655440001"
    private const val DRAMAGA = "550e8400-e29b-41d4-a716-446655440013"

    /** Outlet uji coba: boleh opname berkali-kali, tanggal berapa pun. */
    private const val OUTLET_TES = "eb174b2b-ff69-47eb-97af-b6c824d3ce4a"

    /** Lima outlet yang menyusul opname 5 September 2026. */
    private val SUSULAN_5_SEP = mapOf(
        CIBINONG to "MITRA CIBINONG",
        PEKAYON to "MITRA PEKAYON",
        BNR to "SUKA SHAWARMA BNR",
        DRAMAGA to "SUKA SHAWARMA DRAMAGA",
        JATIWARINGIN to "SUKA SHAWARMA JATIWARINGIN",
    )

    val PENGALIHAN: List<PengalihanTanggalOpname> = buildList {
        add(PengalihanTanggalOpname(CILEUNGSI, "CILEUNGSI", setOf("2026-08-21"), "2026-08-20"))
        // Empang menuntut DUA opname finalized, bukan satu: hari itu outletnya memang
        // dijatah dua kali, jadi satu yang sudah masuk belum menutup kuotanya.
        add(PengalihanTanggalOpname(EMPANG, "EMPANG", setOf("2026-08-24"), "2026-08-23", minimalFinalized = 2))
        add(PengalihanTanggalOpname(PALEDANG, "PALEDANG", setOf("2026-08-26"), "2026-08-25"))
        add(PengalihanTanggalOpname(JATIWARINGIN, "JATIWARINGIN", setOf("2026-08-30"), "2026-08-29"))
        add(PengalihanTanggalOpname(CICURUG, "MITRA CICURUG", setOf("2026-09-03", "2026-09-09"), "2026-09-02"))
        SUSULAN_5_SEP.forEach { (id, nama) ->
            add(PengalihanTanggalOpname(id, nama, setOf("2026-09-06"), "2026-09-05"))
        }
    }

    /**
     * Perhatikan tanggalnya diperiksa terhadap tanggal EFEKTIF (hasil pengalihan),
     * bukan tanggal kalender — sama seperti web. Karena itu daftar tanggal di sini
     * tidak sama persis dengan [PENGALIHAN]: Cicurug misalnya dialihkan pada 9
     * September tetapi jatah gandanya melekat pada 2 September, tanggal tujuannya.
     */
    val JATAH_GANDA: List<JatahOpnameGanda> = buildList {
        add(JatahOpnameGanda(OUTLET_TES, "OUTLET TES", berlakuPada = null, maksimal = 999))
        add(JatahOpnameGanda(EMPANG, "EMPANG", setOf("2026-08-23"), maksimal = 2))
        add(JatahOpnameGanda(JATIWARINGIN, "JATIWARINGIN", setOf("2026-08-29", "2026-08-30"), maksimal = 2))
        add(JatahOpnameGanda(CICURUG, "MITRA CICURUG", setOf("2026-09-02", "2026-09-03"), maksimal = 2))
        SUSULAN_5_SEP.forEach { (id, nama) ->
            add(JatahOpnameGanda(id, nama, setOf("2026-09-05", "2026-09-06"), maksimal = 2))
        }
    }

    /** Aturan pengalihan yang berlaku untuk outlet ini hari ini, atau null. */
    fun pengalihanUntuk(outletId: String, hariIni: String): PengalihanTanggalOpname? =
        PENGALIHAN.firstOrNull { it.outletId == outletId && hariIni in it.berlakuPada }

    /**
     * Berapa opname yang boleh ada pada [tanggalEfektif] untuk outlet ini.
     * Satu bila tidak ada pengecualian — aturan normal "sehari sekali".
     */
    fun maksimalOpname(outletId: String, tanggalEfektif: String): Int =
        JATAH_GANDA
            .filter { it.outletId == outletId && (it.berlakuPada == null || tanggalEfektif in it.berlakuPada) }
            .maxOfOrNull { it.maksimal }
            ?: 1
}

/**
 * Kenapa sebuah opname tidak bisa diisi — dipisah dari UI supaya alasannya bisa
 * diuji, dan supaya penyebabnya tidak tersamar jadi galat jaringan generik.
 *
 * `pending_approval` tidak bisa disentuh native BUKAN karena aturan bisnis melainkan
 * karena policy RLS `opname_item_write` hanya mengizinkan opname berstatus `draft`.
 * Web bisa karena menulisnya lewat service-role yang menembus RLS. Melonggarkan
 * policy itu demi menyamakan diri berarti membuka tulis untuk baris yang sedang
 * ditinjau — mahal, dan tidak perlu selama native tidak pernah mengirim opname ke
 * antrean persetujuan.
 */
enum class AlasanOpnameTerkunci(val pesan: String) {
    SUDAH_FINAL(
        "Opname untuk tanggal ini sudah difinalisasi. " +
            "Jatah opname outlet ini pada tanggal tersebut sudah terpakai."
    ),
    SEDANG_DITINJAU(
        "Opname ini sedang menunggu persetujuan, jadi isinya terkunci. " +
            "Menyunting opname yang sedang ditinjau hanya bisa lewat aplikasi web."
    ),
    DITOLAK(
        "Opname ini ditolak dan tidak bisa dilanjutkan. Mulai hitungan baru."
    ),
}
