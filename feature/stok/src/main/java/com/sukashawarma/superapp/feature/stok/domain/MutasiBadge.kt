package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Bentuk ringkas satu mutasi untuk perhitungan badge — hanya kolom yang benar-benar
 * menentukan angkanya, bukan seluruh baris.
 */
data class MutasiRingkas(
    val id: String,
    val status: String,
    val outletAsalId: String,
    val outletTujuanId: String,
)

/**
 * Rincian badge mutasi. Yang tampil di bilah bawah cuma [total]; tiga angka lainnya
 * dipertahankan karena web memakainya juga dan memisahkannya membuat penyebab angka
 * bisa ditelusuri tanpa menghitung ulang.
 */
data class JumlahBadgeMutasi(
    val menungguPersetujuan: Int = 0,
    val menungguPengiriman: Int = 0,
    val dikirim: Int = 0,
) {
    val total: Int get() = menungguPersetujuan + menungguPengiriman + dikirim
}

/**
 * Badge mutasi antar outlet — cermin `lib/stok/mutasiBadge.ts` web, termasuk
 * test-nya (`mutasiBadge.test.ts`) yang ikut diport ke `MutasiBadgeTest`.
 *
 * Angka ini menjawab satu pertanyaan: berapa mutasi yang MENUNGGU TINDAKAN SAYA.
 * Karena itu ia bukan sekadar "berapa yang belum selesai" — perannya dan outletnya
 * ikut menentukan, dan tiga status yang dihitung menuntut tindakan dari pihak yang
 * berbeda-beda:
 *
 * - `menunggu_persetujuan` — yang bertindak penyetuju, DALAM cakupan outletnya.
 * - `menunggu_pengiriman`  — yang bertindak outlet ASAL.
 * - `dikirim`              — yang bertindak outlet TUJUAN.
 *
 * ## Divergensi yang disengaja dari web
 *
 * `calculateMutasiBadgeCounts` web menghitung SELURUH `menunggu_persetujuan` untuk
 * peran penyetuju, tanpa memandang outlet. Native TIDAK, dan itu bukan kelalaian.
 *
 * Di web maupun native, halaman Mutasi hanya menampilkan mutasi yang menyangkut
 * outlet terpilih. Aturan web membuat lencana menjanjikan pekerjaan yang halamannya
 * sendiri tidak bisa tunjukkan: seorang area manager satu outlet melihat angka 27,
 * membuka halamannya, dan mendapati daftar kosong. Terpantau langsung di perangkat
 * pada 9 September 2026 — lencana 27, daftar 0 baris.
 *
 * Lencana yang menunjuk pekerjaan yang tidak bisa dijangkau lebih buruk daripada
 * tidak ada lencana. Jadi di sini persetujuan ikut discope ke outlet, sehingga angka
 * lencana selalu sama dengan jumlah baris yang benar-benar muncul di layar.
 *
 * `selesai` dan `ditolak` tidak dihitung: tidak ada lagi yang perlu dikerjakan.
 */
object MutasiBadge {

    /**
     * Peran yang boleh menyetujui mutasi.
     *
     * Web menyusunnya sebagai `isApproverRole(role) || APPROVER_MUTASI_ROLES`, dua
     * daftar yang beririsan; di sini keduanya sudah digabung menjadi satu himpunan
     * dengan isi yang sama persis. Crew sengaja di luar daftar — merekalah alasan
     * badge ini ada, bukan yang menyetujuinya.
     */
    private val PENYETUJU = setOf(
        Role.KITCHEN, Role.ADMIN_FINANCE, Role.SPV, Role.LEADER,
        Role.REGIONAL_MANAGER, Role.PURCHASING, Role.ADMIN,
        Role.AREA_MANAGER, Role.OWNER, Role.DEVELOPER,
    )

    fun bolehMenyetujui(role: Role?): Boolean = role != null && role in PENYETUJU

    /**
     * [outletId] null berarti "tidak terikat satu outlet" — gudang pusat dan kantor
     * yang memang mengawasi semua cabang. Dalam keadaan itu pengiriman dan
     * penerimaan dihitung seluruhnya, sama seperti web.
     */
    fun hitung(
        daftar: List<MutasiRingkas>,
        role: Role?,
        outletId: String?,
    ): JumlahBadgeMutasi {
        val penyetuju = bolehMenyetujui(role)
        var persetujuan = 0
        var pengiriman = 0
        var dikirim = 0

        for (m in daftar) {
            when (m.status) {
                StatusMutasiNilai.MENUNGGU_PERSETUJUAN ->
                    if (penyetuju && m.menyangkut(outletId)) persetujuan++
                StatusMutasiNilai.MENUNGGU_PENGIRIMAN ->
                    if (outletId == null || m.outletAsalId == outletId) pengiriman++
                StatusMutasiNilai.DIKIRIM ->
                    if (outletId == null || m.outletTujuanId == outletId) dikirim++
            }
        }

        return JumlahBadgeMutasi(persetujuan, pengiriman, dikirim)
    }

    /**
     * Apakah mutasi ini menyangkut outlet tersebut — baik sebagai pengirim maupun
     * penerima. Lingkup null berarti tidak terikat outlet, jadi semuanya menyangkut.
     *
     * Sengaja memakai penyaring yang sama dengan `MutasiRepository.daftar`, karena
     * dari situlah kesejalanan lencana dan daftar berasal.
     */
    private fun MutasiRingkas.menyangkut(outletId: String?): Boolean =
        outletId == null || outletAsalId == outletId || outletTujuanId == outletId

    /** Apakah satu mutasi menuntut tindakan dari pengguna ini — cermin `isMutasiActionable`. */
    fun perluTindakan(m: MutasiRingkas, role: Role?, outletId: String?): Boolean =
        when (m.status) {
            StatusMutasiNilai.MENUNGGU_PERSETUJUAN -> bolehMenyetujui(role) && m.menyangkut(outletId)
            StatusMutasiNilai.MENUNGGU_PENGIRIMAN -> outletId == null || m.outletAsalId == outletId
            StatusMutasiNilai.DIKIRIM -> outletId == null || m.outletTujuanId == outletId
            else -> false
        }
}

/**
 * Nilai status apa adanya seperti tersimpan di kolom `mutasi_antar_outlet.status`.
 *
 * Sengaja string mentah, bukan `StatusMutasi`: enum itu memetakan nilai tak dikenal
 * menjadi `MENUNGGU_PERSETUJUAN`, dan di sini pemetaan seperti itu akan MENAMBAH
 * angka badge untuk status yang sebenarnya tidak dikenali.
 */
object StatusMutasiNilai {
    const val MENUNGGU_PERSETUJUAN = "menunggu_persetujuan"
    const val MENUNGGU_PENGIRIMAN = "menunggu_pengiriman"
    const val DIKIRIM = "dikirim"
}
