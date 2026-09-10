package com.sukashawarma.superapp.feature.chat.domain

import com.sukashawarma.superapp.feature.chat.data.PesanChat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Logika murni layar chat — tanpa Android, supaya bisa diuji JVM.
 */

/** Umur maksimal pesan yang boleh tampil. Cermin policy `chat_messages_select_24h`. */
const val UMUR_PESAN_MS: Long = 24L * 60 * 60 * 1000

/** Dua pesan beruntun dari orang yang sama digabung satu grup bila jaraknya <= ini. */
const val JARAK_GRUP_MS: Long = 5L * 60 * 1000

/** Posisi bubble di dalam satu grup pengirim — menentukan bentuk sudut ala iMessage. */
enum class PosisiGrup { TUNGGAL, AWAL, TENGAH, AKHIR }

sealed interface ItemChat {
    /** Pemisah "Hari Ini" / "Kemarin" / tanggal. */
    data class Pemisah(val label: String) : ItemChat

    data class Bubble(
        val pesan: PesanChat,
        val milikSendiri: Boolean,
        val posisi: PosisiGrup,
        /** Nama + avatar hanya tampil di bubble PERTAMA grup lawan bicara. */
        val tampilkanIdentitas: Boolean,
        /** "HH:mm" yang sudah jadi. Diformat di sini, sekali per pesan, bukan di
         *  dalam composable: pemformatan tanggal termasuk yang paling mahal di
         *  jalur gambar, dan bubble digambar ulang jauh lebih sering daripada
         *  isinya berubah. */
        val jam: String = "",
    ) : ItemChat
}

private val FORMAT_TANGGAL = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))

/** DateTimeFormatter aman dipakai bersama antar-thread, tidak seperti
 *  SimpleDateFormat yang sebelumnya dipanggil dari dalam composable. */
private val FORMAT_JAM = DateTimeFormatter.ofPattern("HH:mm", Locale("id", "ID"))

fun labelTanggal(hari: LocalDate, hariIni: LocalDate): String = when (hari) {
    hariIni -> "Hari Ini"
    hariIni.minusDays(1) -> "Kemarin"
    else -> hari.format(FORMAT_TANGGAL)
}

/**
 * Susun daftar item layar dari pesan mentah: buang yang kedaluwarsa, urutkan,
 * sisipkan pemisah tanggal, dan tandai posisi tiap bubble dalam grupnya.
 *
 * Pengelompokan mengikuti WhatsApp: pengirim sama + hari sama + jarak antar
 * pesan <= 5 menit = satu grup; identitas (nama+avatar) hanya di bubble pertama.
 */
fun susunItemChat(
    pesan: List<PesanChat>,
    userId: String,
    nowMs: Long,
    zona: ZoneId = ZoneId.systemDefault(),
): List<ItemChat> {
    val hidup = pesan
        .filter { nowMs - it.createdAtMs < UMUR_PESAN_MS }
        .sortedBy { it.createdAtMs }
    if (hidup.isEmpty()) return emptyList()

    val hariIni = Instant.ofEpochMilli(nowMs).atZone(zona).toLocalDate()

    // Tanggal dan jam dihitung SEKALI per pesan, bukan tiga kali seperti versi
    // sebelumnya (hari ini, hari sebelum, hari sesudah masing-masing memanggil
    // atZone lagi). Konversi zona waktu adalah bagian termahal fungsi ini, dan
    // pada daftar 500 pesan bedanya terasa di HP kelas bawah.
    val waktu = hidup.map { it.createdAtMs.let(Instant::ofEpochMilli).atZone(zona) }
    val hariPer = waktu.map { it.toLocalDate() }
    val jamPer = waktu.map { it.format(FORMAT_JAM) }

    val hasil = ArrayList<ItemChat>(hidup.size + 4)

    hidup.forEachIndexed { i, p ->
        val hari = hariPer[i]
        val sebelum = hidup.getOrNull(i - 1)
        val sesudah = hidup.getOrNull(i + 1)

        val hariSebelum = if (i > 0) hariPer[i - 1] else null
        if (hari != hariSebelum) hasil += ItemChat.Pemisah(labelTanggal(hari, hariIni))

        val nyambungAtas = sebelum != null && sebelum.senderId == p.senderId &&
            hari == hariSebelum && p.createdAtMs - sebelum.createdAtMs <= JARAK_GRUP_MS
        val hariSesudah = if (i + 1 < hariPer.size) hariPer[i + 1] else null
        val nyambungBawah = sesudah != null && sesudah.senderId == p.senderId &&
            hari == hariSesudah && sesudah.createdAtMs - p.createdAtMs <= JARAK_GRUP_MS

        val posisi = when {
            !nyambungAtas && !nyambungBawah -> PosisiGrup.TUNGGAL
            !nyambungAtas -> PosisiGrup.AWAL
            nyambungBawah -> PosisiGrup.TENGAH
            else -> PosisiGrup.AKHIR
        }
        hasil += ItemChat.Bubble(
            pesan = p,
            milikSendiri = p.senderId == userId,
            posisi = posisi,
            tampilkanIdentitas = p.senderId != userId && !nyambungAtas,
            jam = jamPer[i],
        )
    }
    return hasil
}

/** Ringkasan pesan untuk kartu kutipan reply di composer. */
fun snippetPesan(body: String, imagePath: String?): String = when {
    body.isNotBlank() -> body.take(140)
    imagePath != null -> "📷 Foto"
    else -> ""
}

/**
 * Indeks warna nama yang stabil per pengirim (WA memberi tiap anggota grup satu
 * warna tetap). Hash sendiri, bukan hashCode(), supaya hasil test tidak
 * bergantung implementasi JVM.
 */
fun indeksWarnaNama(senderId: String, jumlahWarna: Int): Int {
    var h = 0
    senderId.forEach { c -> h = (h * 31 + c.code) and 0x7FFFFFFF }
    return h % jumlahWarna
}

/**
 * Pelacak "siapa sedang mengetik", meniru WhatsApp:
 * - pengirim memancarkan sinyal maksimal sekali per [JEDA_KIRIM_MS] selama mengetik;
 * - penerima menganggap orang berhenti setelah [TIMEOUT_MS] tanpa sinyal.
 * Waktu disuntikkan pemanggil supaya bisa diuji tanpa jam sungguhan.
 */
class PelacakPengetik {
    companion object {
        const val JEDA_KIRIM_MS: Long = 3_000
        const val TIMEOUT_MS: Long = 5_000

        /** Batas orang yang dilacak sekaligus. Layar hanya sanggup menampilkan
         *  segelintir wajah, dan tanpa batas ini satu grup besar bisa menumpuk
         *  entri tanpa akhir hanya karena semua orang ikut mengetik. */
        const val MAKS_DILACAK = 10
    }

    private val aktif = LinkedHashMap<String, Pengetik>()
    private val terakhirTerlihat = HashMap<String, Long>()

    // Bukan 0: dengan 0, ketikan pertama setelah aplikasi baru dibuka (nowMs
    // kecil di jam yang dimulai dari epoch mana pun) bisa tertahan 3 detik.
    // Sinyal pertama harus selalu lolos.
    private var terakhirKirimMs: Long = Long.MIN_VALUE / 2

    /** true bila giliran ini pantas memancarkan sinyal typing lagi. */
    fun bolehKirim(nowMs: Long): Boolean {
        if (nowMs - terakhirKirimMs < JEDA_KIRIM_MS) return false
        terakhirKirimMs = nowMs
        return true
    }

    /**
     * Mencatat satu sinyal. Urutan kemunculan SENGAJA dipertahankan: menaruh
     * ulang kunci yang sudah ada di LinkedHashMap tidak memindahkannya ke
     * belakang, jadi wajah dan nama tidak bertukar tempat setiap tiga detik
     * selagi orangnya masih mengetik.
     */
    fun catat(id: String, nama: String, avatar: String?, nowMs: Long) {
        if (id !in aktif && aktif.size >= MAKS_DILACAK) return
        aktif[id] = Pengetik(id, nama, avatar)
        terakhirTerlihat[id] = nowMs
    }

    /** Sesuai kedatangan pesan dari orang itu: dia jelas sudah selesai mengetik. */
    fun selesai(id: String) {
        aktif.remove(id)
        terakhirTerlihat.remove(id)
    }

    /** Yang masih dianggap mengetik, urut kedatangan sinyal pertamanya. */
    fun aktif(nowMs: Long): List<Pengetik> {
        val kedaluwarsa = terakhirTerlihat.filterValues { nowMs - it > TIMEOUT_MS }.keys
        kedaluwarsa.forEach { aktif.remove(it); terakhirTerlihat.remove(it) }
        return aktif.values.toList()
    }
}

/** Seorang yang sedang mengetik, beserta wajahnya untuk ditumpuk di layar. */
data class Pengetik(val id: String, val nama: String, val avatar: String?)

/** Wajah yang ditumpuk paling banyak sekian; sisanya diwakili angka. */
const val MAKS_WAJAH_PENGETIK = 3

/**
 * Nama-nama untuk baris pengetik di dasar percakapan.
 *
 * SENGAJA TANPA kata "sedang mengetik": baris itu sudah berisi wajah dan
 * gelembung tiga titik beranimasi, yang mengatakan hal yang sama. Dengan
 * verbanya, kalimatnya melewati lebar layar dan terpotong di tengah kata —
 * "dan 2 lai..." — sehingga justru kehilangan bagian yang berguna.
 *
 * Di atas dua orang sisanya diringkas jadi "+N", supaya lebar baris punya
 * batas atas yang pasti berapa pun ramainya grup.
 */
fun labelPengetik(orang: List<Pengetik>): String? = when {
    orang.isEmpty() -> null
    orang.size == 1 -> orang[0].nama
    orang.size == 2 -> "${orang[0].nama} dan ${orang[1].nama}"
    else -> "${orang[0].nama}, ${orang[1].nama}, +${orang.size - 2}"
}

/** Versi pendek untuk subjudul header, yang ruangnya jauh lebih sempit. */
fun labelPengetikPendek(orang: List<Pengetik>): String? = when {
    orang.isEmpty() -> null
    orang.size == 1 -> "${orang[0].nama} sedang mengetik…"
    else -> "${orang.size} orang sedang mengetik…"
}
