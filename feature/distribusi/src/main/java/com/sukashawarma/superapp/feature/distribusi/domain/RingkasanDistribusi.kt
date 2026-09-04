package com.sukashawarma.superapp.feature.distribusi.domain

/** Kontrak minimal satu baris daftar untuk keperluan statistik. */
interface BarisRingkasan {
    val status: StatusSuratJalan?
    val namaOutlet: String?
    val adaSelisih: Boolean
}

data class HitunganStatus(
    val draft: Int,
    val dikirim: Int,
    val diterima: Int,
    val selesai: Int,
)

data class BarisOutlet(
    val nama: String,
    val total: Int,
    val aktif: Int,
    val bermasalah: Int,
)

/**
 * Statistik dashboard. Cermin blok `useMemo` di `app/dashboard/page.tsx` —
 * angkanya harus sama dengan web karena pengawas membandingkan layar HP dengan
 * layar laptop.
 */
object RingkasanDistribusi {

    fun hitungStatus(baris: List<BarisRingkasan>): HitunganStatus = HitunganStatus(
        draft = baris.count { it.status == StatusSuratJalan.DRAFT },
        dikirim = baris.count {
            it.status == StatusSuratJalan.DIKIRIM || it.status == StatusSuratJalan.DIKIRIM_LENGKAP
        },
        diterima = baris.count {
            it.status == StatusSuratJalan.DITERIMA_LENGKAP ||
                it.status == StatusSuratJalan.DITERIMA_SEBAGIAN
        },
        selesai = baris.count { it.status == StatusSuratJalan.SELESAI },
    )

    /**
     * Persentase kiriman terverifikasi yang tiba tanpa selisih.
     *
     * Penyebutnya hanya yang sudah diverifikasi: kiriman yang masih di jalan
     * belum bisa dinilai akurat atau tidak. Bila belum ada satu pun yang
     * terverifikasi, nilainya 100 — sama dengan web, dan lebih jujur daripada
     * menampilkan 0% pada outlet yang baru mulai.
     */
    fun tingkatAkurasi(baris: List<BarisRingkasan>): Int {
        val terverifikasi = baris.count { it.status?.sudahDiterima == true }
        if (terverifikasi == 0) return 100
        val bermasalah = baris.count { it.adaSelisih }
        val akurat = terverifikasi - bermasalah
        return maxOf(0, Math.round(akurat * 100.0 / terverifikasi).toInt())
    }

    /**
     * Volume per outlet, terbanyak lebih dulu. `namaBawaan` dipakai untuk baris
     * yang outletnya tidak ter-embed (biasanya karena kiriman berasal dari
     * gudang pusat).
     */
    fun rincianOutlet(
        baris: List<BarisRingkasan>,
        namaBawaan: String,
        maksimum: Int = 6,
    ): List<BarisOutlet> = baris
        .groupBy { it.namaOutlet?.takeIf { nama -> nama.isNotBlank() } ?: namaBawaan }
        .map { (nama, rows) ->
            BarisOutlet(
                nama = nama,
                total = rows.size,
                aktif = rows.count {
                    it.status == StatusSuratJalan.DIKIRIM ||
                        it.status == StatusSuratJalan.DIKIRIM_LENGKAP ||
                        it.status == StatusSuratJalan.DITERIMA_LENGKAP ||
                        it.status == StatusSuratJalan.DITERIMA_SEBAGIAN
                },
                bermasalah = rows.count { it.adaSelisih },
            )
        }
        .sortedByDescending { it.total }
        .take(maksimum)
}
