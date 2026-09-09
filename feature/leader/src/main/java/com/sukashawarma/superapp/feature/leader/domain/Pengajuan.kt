package com.sukashawarma.superapp.feature.leader.domain

/**
 * Tahapan pengajuan dana operasional, dilihat dari kursi leader.
 *
 * Nilainya persis seperti tersimpan di kolom `petty_cash_topups.status`, dan
 * labelnya disalin dari badge halaman web supaya kata yang dibaca leader di HP
 * sama dengan yang dibacanya di laptop.
 *
 * `pending` masih ada di sini walau `create_petty_cash_topup` sekarang melahirkan
 * pengajuan langsung pada `forwarded_to_area_manager`: baris lama dari sebelum
 * migrasi itu tetap tersimpan dengan status tersebut, dan tanpa entri ini mereka
 * akan hilang dari daftar.
 */
enum class StatusPengajuan(val nilai: String, val label: String) {
    DIAJUKAN("pending", "Menunggu AM"),
    DITERUSKAN_KE_AM("forwarded_to_area_manager", "Menunggu AM"),
    DI_FINANCE("forwarded_to_finance", "Menunggu Finance"),
    DISETUJUI_FINANCE("approved_by_finance", "Acc Finance"),
    DITERUSKAN_FINANCE("forwarded_by_finance", "Acc Finance"),
    SIAP_DISERAHKAN("forwarded_by_area_manager", "Siap Diserahkan"),
    DISERAHKAN_KE_CREW("forwarded_by_leader", "Diserahkan ke Crew"),
    SELESAI("completed", "Selesai"),
    DITOLAK("rejected", "Ditolak");

    companion object {
        fun dari(nilai: String?): StatusPengajuan? = entries.find { it.nilai == nilai }
    }
}

/**
 * Satu-satunya status yang menuntut tindakan leader.
 *
 * Bukan pilihan tampilan: `leader_forward_funds` menolak status lain apa pun
 * dengan galat "Top up is not ready for Leader forwarding". Jadi tombolnya hanya
 * boleh muncul di sini.
 */
val STATUS_BUTUH_AKSI = setOf(StatusPengajuan.SIAP_DISERAHKAN)

/** Masih menunggu meja area manager. */
val STATUS_MENUNGGU_AM = setOf(
    StatusPengajuan.DIAJUKAN,
    StatusPengajuan.DITERUSKAN_KE_AM,
)

/** Sudah tuntas — dana diserahkan ke crew, atau ditutup oleh konfirmasi crew. */
val STATUS_SELESAI = setOf(
    StatusPengajuan.DISERAHKAN_KE_CREW,
    StatusPengajuan.SELESAI,
)

data class Pengajuan(
    val id: String,
    val outletId: String,
    val outletNama: String,
    val jumlah: Long,
    val deskripsi: String?,
    val status: StatusPengajuan,
    val dibuatPada: String,
    val namaBank: String?,
    val nomorRekening: String?,
    val atasNama: String?,
    val buktiTransferUrl: String?,
) {
    val waktuTeks: String get() = waktuJakarta(dibuatPada)

    val butuhAksi: Boolean get() = status in STATUS_BUTUH_AKSI

    val rekeningTeks: String?
        get() = namaBank?.takeIf { it.isNotBlank() }?.let { bank ->
            buildString {
                append(bank)
                nomorRekening?.takeIf { it.isNotBlank() }?.let { append(" - $it") }
            }
        }
}

/** Tab penyaring pada daftar pengajuan — cermin pil filter di halaman web. */
enum class FilterPengajuan(val label: String) {
    SEMUA("Semua"),
    BUTUH_AKSI("Action Leader"),
    MENUNGGU_AM("Menunggu AM"),
    SELESAI("Selesai"),
}

fun saringPengajuan(daftar: List<Pengajuan>, filter: FilterPengajuan): List<Pengajuan> =
    when (filter) {
        FilterPengajuan.SEMUA -> daftar
        FilterPengajuan.BUTUH_AKSI -> daftar.filter { it.status in STATUS_BUTUH_AKSI }
        FilterPengajuan.MENUNGGU_AM -> daftar.filter { it.status in STATUS_MENUNGGU_AM }
        FilterPengajuan.SELESAI -> daftar.filter { it.status in STATUS_SELESAI }
    }

/** Isi form pengajuan, apa adanya seperti yang diketik pengguna. */
data class FormTopup(
    val outletId: String? = null,
    val nominal: String = "",
    val keperluan: String = "",
    val namaBank: String = "",
    val nomorRekening: String = "",
    val atasNama: String = "",
) {
    /** Nominal sebagai angka; null bila kosong atau bukan bilangan yang masuk akal. */
    val nominalAngka: Long? get() = nominal.filter { it.isDigit() }.toLongOrNull()
}

/**
 * Pesan galat pertama pada form, atau null bila sudah boleh dikirim.
 *
 * Urutan pemeriksaannya sama dengan `handleSubmit` web supaya pengguna yang
 * mengosongkan dua kolom sekaligus mendapat teguran tentang kolom yang sama di
 * kedua aplikasi. Rekening ikut wajib karena RPC-lah yang menuliskannya balik ke
 * `outlets`, dan finance mentransfer ke sana — pengajuan tanpa rekening berarti
 * dana yang tak bisa dikirim ke mana pun.
 */
fun galatForm(form: FormTopup): String? = when {
    form.outletId.isNullOrBlank() ->
        "Pilih outlet tujuan top up terlebih dahulu."
    (form.nominalAngka ?: 0L) <= 0L ->
        "Nominal top up wajib diisi dengan angka positif."
    form.keperluan.isBlank() ->
        "Alasan / keperluan operasional wajib diisi."
    form.namaBank.isBlank() || form.nomorRekening.isBlank() || form.atasNama.isBlank() ->
        "Rekening bank outlet (nama bank, nomor rekening, dan atas nama) wajib diisi lengkap."
    else -> null
}

/** `1250000` menjadi `1.250.000` di kolom nominal, mengikuti gaya `toLocaleString('id-ID')` web. */
fun nominalTerformat(nominal: String): String =
    nominal.filter { it.isDigit() }.toLongOrNull()?.let { ribuan(it) } ?: ""
