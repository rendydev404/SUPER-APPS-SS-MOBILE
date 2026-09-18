package com.sukashawarma.superapp.feature.stok.domain

/**
 * Status tiket retur — cermin CHECK constraint `retur_stok.status` di migration
 * `20260914160000_skema_retur_refund_bahan.sql` dan `STATUS_CONFIG` di
 * `components/refund/CardReturItem.tsx`.
 *
 * [langkah] adalah indeks pada stepper enam tahap di kartu tiket. Nilainya disalin
 * dari `getStepIndex` web, termasuk dua kejanggalannya yang sengaja dipertahankan:
 * `DITOLAK` bernilai -1 (stepper disembunyikan) dan [MENUNGGU_STOK] jatuh ke 0
 * walau secara alur ia berada setelah barang tiba di gudang pusat. Yang terakhir
 * tidak pernah terlihat di lapangan: tidak ada satu pun RPC yang menulis status
 * itu, ia hanya tersedia di constraint.
 */
enum class StatusRetur(
    val nilai: String,
    val label: String,
    val langkah: Int,
) {
    DIAJUKAN("diajukan", "Menunggu Review AM/RM", 0),
    DISETUJUI_MANAGER("disetujui_manager", "Disetujui AM/RM (Siap Kurir)", 1),
    DALAM_PENGIRIMAN("dalam_pengiriman", "Dalam Pengiriman Kurir", 2),
    DITERIMA_KITCHEN("diterima_kitchen", "Tiba di Kitchen (Menunggu Kirim)", 3),
    MENUNGGU_STOK("menunggu_stok", "Menunggu Stok Kitchen", 0),
    DIKIRIM_PENGGANTI("dikirim_pengganti", "SJ Pengganti OTW", 4),
    SELESAI("selesai", "Selesai (100% Diganti)", 5),
    DITOLAK("ditolak", "Ditolak (Dialihkan ke Waste)", -1);

    /** Tiket yang sudah tidak bergerak lagi — dasar pemisahan tab Aktif vs Riwayat. */
    val tuntas: Boolean get() = this == SELESAI || this == DITOLAK

    companion object {
        /** Status tak dikenal jadi null, bukan melempar: constraint DB bisa bertambah. */
        fun dari(nilai: String?): StatusRetur? = entries.find { it.nilai == nilai }

        /** Judul enam tahap stepper, urut — cermin `steps` di `CardReturItem.tsx`. */
        val LANGKAH = listOf(
            "Diajukan", "Approve AM", "Kurir OTW", "Tiba Kitchen", "SJ Pengganti", "Selesai",
        )
    }
}

/**
 * Jenis armada pengantar fisik retur — cermin CHECK `retur_stok.jenis_logistik` dan
 * `LOGISTIK_OPTIONS` di `components/refund/ModalSerahTerimaKurir.tsx`.
 */
enum class JenisLogistik(val nilai: String, val label: String) {
    INTERNAL("internal", "Armada Internal Suka Shawarma"),
    LALAMOVE("lalamove", "Lalamove"),
    GOSEND("gosend", "GoSend (Gojek)"),
    GRABEXPRESS("grabexpress", "GrabExpress"),
    DELIVEREE("deliveree", "Deliveree"),
    LAINNYA("lainnya", "Ekspedisi / Kurir Lainnya");

    /**
     * Pihak ketiga wajib mengisi nomor resi — tanpa itu paket tidak bisa dilacak
     * dan tidak ada yang bisa dituntut kalau daging hilang di jalan. Cermin `is3PL`.
     */
    val pihakKetiga: Boolean get() = this != INTERNAL

    companion object {
        fun dari(nilai: String?): JenisLogistik = entries.find { it.nilai == nilai } ?: INTERNAL
    }
}

/**
 * Aturan isi formulir retur — cermin `FormPengajuanRefund.tsx`.
 *
 * Dipisah dari layar supaya bisa diuji tanpa Compose: yang menentukan sebuah klaim
 * boleh dikirim adalah aturan di sini, bukan tombol yang kebetulan tidak abu-abu.
 */
object FormulirRetur {

    /** Pilihan alasan, urut seperti dropdown web. Nilainya disimpan apa adanya. */
    val ALASAN = listOf(
        "Basi / Bau Asam",
        "Berubah Warna / Berlendir",
        "Kemasan Vacum Bocor / Rusak",
        "Cacat Potongan / Susut Ekstrem",
        "Hancur / Rusak saat Pengiriman",
        "Lainnya",
    )

    const val ALASAN_LAINNYA = "Lainnya"

    /**
     * Nama bahan yang ditandai `is_refundable` oleh migration.
     *
     * Dipakai sebagai jaring pengaman kedua persis seperti web: katalog disaring
     * `is_refundable || nama in DAFTAR`, supaya form tetap berisi walau kolomnya
     * belum sempat di-backfill di satu-dua baris.
     */
    val NAMA_REFUNDABLE = setOf("AYAM", "SAPI", "KULIT 25", "KULIT 28", "KULIT 32")

    fun refundable(isRefundable: Boolean?, nama: String): Boolean =
        isRefundable == true || nama.trim().uppercase() in NAMA_REFUNDABLE

    /**
     * Total klaim pada SATUAN BESAR dari dua kolom masukan (besar + kecil).
     *
     * Faktor bawaan 1000 disalin dari web (`faktor_tampilan || 1000`): bahan core
     * yang boleh diretur semuanya berskala kilogram-gram, jadi bahan tanpa faktor
     * tersimpan tetap terbaca benar. Angka ini yang dipotong dari saldo outlet,
     * jadi salah skala di sini berarti stok terpotong ribuan kali lipat.
     */
    fun totalBesar(besar: Double?, kecil: Double?, faktorTampilan: Double?): Double {
        val faktor = faktorTampilan?.takeIf { it > 0 } ?: 1000.0
        return (besar ?: 0.0) + (kecil ?: 0.0) / faktor
    }

    /** Alasan akhir yang dikirim ke database — "Lainnya" digabung dengan keterangannya. */
    fun alasanAkhir(pilihan: String, keterangan: String): String =
        if (pilihan != ALASAN_LAINNYA) pilihan
        else keterangan.trim().takeIf { it.isNotEmpty() }?.let { "Lainnya: $it" } ?: ALASAN_LAINNYA

    /**
     * Alasan tidak bisa mengirim formulir, atau null bila sudah boleh — cermin
     * urutan `toast.error` di `handleSubmit` web.
     */
    fun halangan(
        bahanTerpilih: Boolean,
        totalBesar: Double,
        adaFoto: Boolean,
        alasan: String,
        keteranganLainnya: String,
    ): String? = when {
        !bahanTerpilih -> "Pilih bahan baku yang akan diretur."
        totalBesar <= 0.0 -> "Kuantitas timbangan harus lebih besar dari 0."
        !adaFoto -> "Foto bahan baku di atas timbangan wajib diunggah."
        alasan == ALASAN_LAINNYA && keteranganLainnya.isBlank() ->
            "Keterangan alasan lainnya wajib diisi."
        else -> null
    }
}
