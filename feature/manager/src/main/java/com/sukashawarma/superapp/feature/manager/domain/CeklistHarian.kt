package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import java.time.OffsetDateTime

/**
 * Ceklist harian area manager — penilaian kunjungan outlet yang dipantau
 * regional manager.
 *
 * Kontraknya ada di migrasi web `20300238000000_ceklist_harian_area_manager`:
 * nama kategori, sub-item rasa, dan nilai di sini HARUS sama persis dengan CHECK
 * di tabel `ceklist_harian_item`, karena `submit_ceklist_harian` menolak seluruh
 * kiriman bila satu saja tidak dikenal.
 */

/** Tiga tingkat penilaian. Sengaja hanya tiga: satu ketukan, tanpa berpikir skala. */
enum class NilaiCeklist(val nilai: String, val label: String, val emoji: String) {
    BAIK("baik", "Baik", "✅"),
    PERHATIAN("perhatian", "Perhatian", "⚠️"),
    BURUK("buruk", "Buruk", "❌");

    companion object {
        fun dari(nilai: String?): NilaiCeklist? = entries.find { it.nilai == nilai }

        /** Nilai terburuk dari sekumpulan penilaian — dipakai kategori Rasa. */
        fun terburuk(daftar: Collection<NilaiCeklist?>): NilaiCeklist? =
            daftar.filterNotNull().maxByOrNull { it.ordinal }
    }
}

/**
 * Satu baris yang dinilai. Untuk kategori tanpa sub-item, [subItem] kosong dan
 * baris itu mewakili kategorinya sendiri.
 *
 * [kalimat] adalah kalimat siap pakai per tingkat nilai — memilih nilai langsung
 * mengisi keterangannya, jadi kasus umum selesai dengan satu ketukan.
 */
data class ButirCeklist(
    val kategori: String,
    val subItem: String,
    val label: String,
    val kalimat: Map<NilaiCeklist, String>,
) {
    val kunci: String get() = if (subItem.isEmpty()) kategori else "$kategori.$subItem"
}

/** Satu kategori penilaian; setiap kategori wajib dinilai dan berfoto 1..3. */
data class KategoriCeklist(
    val kunci: String,
    val label: String,
    val petunjukFoto: String,
    val butir: List<ButirCeklist>,
) {
    val punyaSubItem: Boolean get() = butir.size > 1 || butir.firstOrNull()?.subItem?.isNotEmpty() == true
}

private fun kalimat(baik: String, perhatian: String, buruk: String) = mapOf(
    NilaiCeklist.BAIK to baik,
    NilaiCeklist.PERHATIAN to perhatian,
    NilaiCeklist.BURUK to buruk,
)

private fun tunggal(kunci: String, label: String, foto: String, k: Map<NilaiCeklist, String>) =
    KategoriCeklist(kunci, label, foto, listOf(ButirCeklist(kunci, "", label, k)))

val KATEGORI_CEKLIST: List<KategoriCeklist> = listOf(
    tunggal(
        "kebersihan", "Kebersihan", "Foto area outlet & dapur",
        kalimat("Sudah baik", "Kurang bersih", "Kotor, perlu dibersihkan segera"),
    ),
    tunggal(
        "stok", "Stok", "Foto freezer / rak stok",
        kalimat("Aman", "Kurang aman", "Kritis, segera order"),
    ),
    tunggal(
        "seragam_crew", "Seragam & Crew", "Foto crew yang bertugas",
        kalimat("Lengkap", "Kurang lengkap", "Tidak sesuai standar"),
    ),
    KategoriCeklist(
        "rasa", "Rasa", "Foto produk yang dicicipi",
        listOf(
            ButirCeklist(
                "rasa", "sapi", "Sapi",
                kalimat(
                    "Tingkat kematangan dan ketebalan sudah sesuai",
                    "Kematangan/ketebalan kurang sesuai",
                    "Tidak sesuai SOP",
                ),
            ),
            ButirCeklist("rasa", "ayam", "Ayam", kalimat("Sesuai SOP", "Kurang sesuai SOP", "Tidak sesuai SOP")),
            ButirCeklist("rasa", "kentang", "Kentang", kalimat("Sesuai SOP", "Kurang sesuai SOP", "Tidak sesuai SOP")),
            ButirCeklist("rasa", "tum", "Tum", kalimat("Sudah sesuai SOP", "Kurang sesuai SOP", "Tidak sesuai SOP")),
            ButirCeklist("rasa", "sayur", "Sayur", kalimat("Fresh", "Kurang fresh", "Layu, tidak layak pakai")),
        ),
    ),
    tunggal(
        "peralatan", "Peralatan", "Foto peralatan & isi freezer",
        kalimat(
            "Sudah lengkap dan bahan baku di dalam freezer tersusun dengan rapih",
            "Kurang lengkap / kurang rapih",
            "Ada peralatan rusak",
        ),
    ),
)

/**
 * Bagian isian bebas di bawah penilaian: baris-baris teks tanpa nilai, masing-
 * masing opsional dan boleh berfoto 0..3. Kuncinya sekaligus nilai
 * `ceklist_harian_foto.kategori` untuk fotonya (migrasi `20300240000000`).
 *
 * [bolehGaleri]: tangkapan layar ulasan Google Maps tidak bisa dipotret kamera.
 */
enum class BagianBebas(
    val kunci: String,
    val label: String,
    val contoh: String,
    val teksTambah: String,
    val bolehGaleri: Boolean,
) {
    ONLINE_REVIEW(
        "online_review", "Online Review",
        "Contoh: Google Maps : belum ada ulasan terbaru", "Tambah review", bolehGaleri = true,
    ),
    TEMUAN(
        "temuan", "Temuan",
        "Contoh: 1 unit baling-baling kipas patah", "Tambah temuan", bolehGaleri = false,
    ),
    PERBAIKAN(
        "perbaikan", "Perbaikan",
        "Contoh: perbaikan/pengadaan 1 unit kipas", "Tambah perbaikan", bolehGaleri = false,
    ),
}

val SEMUA_BUTIR: List<ButirCeklist> = KATEGORI_CEKLIST.flatMap { it.butir }

/** Batas foto per kategori dan per bagian bebas — sama dengan batas 3 di RPC. */
const val FOTO_MAKS_PER_KATEGORI = 3

/** Penilaian satu butir. */
data class IsianCeklist(val nilai: NilaiCeklist? = null, val keterangan: String = "")

/**
 * Foto satu kategori. [url] adalah URL bertanda tangan untuk pratinjau; kosong
 * sampai diminta, karena bucket-nya privat.
 */
data class FotoCeklist(val path: String, val url: String? = null)

/**
 * Memilih nilai sekaligus mengisi keterangannya dengan kalimat baku — KECUALI
 * pengguna sudah menulis keterangan sendiri. Kalimat baku tingkat lain dianggap
 * bukan tulisan sendiri, jadi berpindah dari "Aman" ke "Kurang aman" ikut
 * mengganti teksnya.
 */
fun pilihNilai(butir: ButirCeklist, lama: IsianCeklist, nilai: NilaiCeklist): IsianCeklist {
    val ketikanSendiri = lama.keterangan.isNotBlank() && lama.keterangan.trim() !in butir.kalimat.values
    return IsianCeklist(nilai, if (ketikanSendiri) lama.keterangan else butir.kalimat.getValue(nilai))
}

/** Keterangan yang ditampilkan: tulisan AM, atau label nilainya. */
fun keteranganTampil(isian: IsianCeklist?): String {
    val teks = isian?.keterangan?.trim().orEmpty()
    if (teks.isNotEmpty()) return teks
    return isian?.nilai?.label?.lowercase().orEmpty()
}

/** Nilai ringkas satu kategori — butir terburuk di dalamnya. */
fun nilaiKategori(kategori: KategoriCeklist, isian: Map<String, IsianCeklist>): NilaiCeklist? {
    val nilai = kategori.butir.map { isian[it.kunci]?.nilai }
    return if (nilai.any { it == null }) null else NilaiCeklist.terburuk(nilai)
}

/** Apakah satu kategori sudah siap kirim: seluruh butir dinilai dan minimal satu foto. */
fun kategoriLengkap(
    kategori: KategoriCeklist,
    isian: Map<String, IsianCeklist>,
    foto: Map<String, List<FotoCeklist>>,
): Boolean = kategori.butir.all { isian[it.kunci]?.nilai != null } && foto[kategori.kunci].orEmpty().isNotEmpty()

/** Jumlah kategori yang sudah siap, untuk bilah kemajuan. */
fun jumlahKategoriLengkap(isian: Map<String, IsianCeklist>, foto: Map<String, List<FotoCeklist>>): Int =
    KATEGORI_CEKLIST.count { kategoriLengkap(it, isian, foto) }

/**
 * Alasan ceklist belum bisa dikirim, atau null bila sudah boleh.
 *
 * Menyebut kategori pertama yang bermasalah supaya AM langsung tahu harus
 * menggulir ke mana.
 */
fun halanganCeklist(isian: Map<String, IsianCeklist>, foto: Map<String, List<FotoCeklist>>): String? {
    for (kategori in KATEGORI_CEKLIST) {
        val belum = kategori.butir.filter { isian[it.kunci]?.nilai == null }
        if (belum.isNotEmpty()) {
            return if (kategori.punyaSubItem) {
                "${kategori.label}: ${belum.joinToString { it.label }} belum dinilai."
            } else {
                "${kategori.label} belum dinilai."
            }
        }
        if (foto[kategori.kunci].orEmpty().isEmpty()) return "Foto ${kategori.label} belum ada."
    }
    return null
}

/** Ada butir yang tidak baik — dipakai untuk mengingatkan AM mencatat temuan. */
fun adaMasalah(isian: Map<String, IsianCeklist>): Boolean =
    SEMUA_BUTIR.any { (isian[it.kunci]?.nilai ?: NilaiCeklist.BAIK) != NilaiCeklist.BAIK }

/** Laporan ceklist tersimpan, sebagaimana dibaca dari database. */
data class LaporanCeklist(
    val id: String,
    val outletId: String,
    val submittedBy: String,
    val namaAm: String,
    val tanggal: String,
    val isian: Map<String, IsianCeklist>,
    val foto: Map<String, List<FotoCeklist>>,
    val onlineReview: List<String>,
    val temuan: List<String>,
    val perbaikan: List<String>,
    val catatan: String?,
    val namaPeninjau: String?,
    val ditinjauPada: String?,
    val tanggapanRm: String?,
    val dibuatPada: String,
    val diperbaruiPada: String,
) {
    val sudahDitinjau: Boolean get() = !ditinjauPada.isNullOrBlank()

    /** Nilai terburuk seluruh laporan — warna kartu di daftar RM. */
    val nilaiKeseluruhan: NilaiCeklist?
        get() = NilaiCeklist.terburuk(isian.values.map { it.nilai })

    val perluPerhatian: Boolean
        get() = temuan.isNotEmpty() || (nilaiKeseluruhan ?: NilaiCeklist.BAIK) != NilaiCeklist.BAIK

    fun teks(bagian: BagianBebas): List<String> = when (bagian) {
        BagianBebas.ONLINE_REVIEW -> onlineReview
        BagianBebas.TEMUAN -> temuan
        BagianBebas.PERBAIKAN -> perbaikan
    }
}

/** Filter layar pemantauan RM. */
enum class FilterCeklist(val label: String) {
    SEMUA("Semua"),
    PERHATIAN("Perlu tindakan"),
    BELUM_DICEK("Belum dicek"),
    BELUM_DITINJAU("Belum disetujui"),
}

/**
 * Hanya area manager yang MENGISI — `submit_ceklist_harian` menolak role lain.
 * Regional manager (dan admin/owner) membuka layar pemantauan.
 */
fun mengisiCeklist(role: Role?): Boolean = role == Role.AREA_MANAGER

/** Role yang boleh menandai ceklist sudah ditinjau — cermin `tinjau_ceklist_harian`. */
fun meninjauCeklist(role: Role?): Boolean =
    role == Role.REGIONAL_MANAGER || role == Role.ADMIN || role == Role.OWNER

/** "09.15" waktu Jakarta dari cap waktu server; string tak terbaca dikembalikan kosong. */
fun jamJakarta(iso: String?): String = runCatching {
    val t = OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA)
    "%02d.%02d".format(t.hour, t.minute)
}.getOrDefault("")
