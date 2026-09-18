package com.sukashawarma.superapp.domain.face

import kotlin.math.sqrt

/** Satu staf yang descriptor-nya tersalin ke perangkat. */
data class KandidatWajah(
    val staffId: String,
    val nama: String,
    val descriptor: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KandidatWajah) return false
        return staffId == other.staffId && nama == other.nama &&
            descriptor.contentEquals(other.descriptor)
    }

    override fun hashCode(): Int =
        31 * (31 * staffId.hashCode() + nama.hashCode()) + descriptor.contentHashCode()
}

data class HasilCocok(val staffId: String, val nama: String, val similarity: Double)

/**
 * Pencocokan wajah di perangkat, dipakai HANYA saat server tidak terjangkau.
 *
 * Ini salinan perilaku RPC `match_face_mobile`, bukan algoritma baru: cosine similarity
 * dengan ambang yang sama persis. Kalau keduanya berbeda sedikit saja, orang yang dikenali
 * saat offline bisa ditolak saat online (atau sebaliknya), dan crew tidak punya cara
 * memahami kenapa. Mengubah [AMBANG] di sini tanpa mengubah MATCH_THRESHOLD di RPC itu
 * termasuk memecah keduanya.
 *
 * Saat online, RPC server tetap yang otoritatif. Kelas ini tidak dipakai sama sekali.
 */
object FaceMatcherLokal {

    /** Harus sama dengan MATCH_THRESHOLD di migration 20260821110000_match_face_mobile_rpc.sql. */
    const val AMBANG = 0.65

    /**
     * [lockToStaffId] mencerminkan mode 1:1 pada perangkat personal; null berarti mode
     * 1:N seperti kiosk outlet.
     *
     * Mengembalikan null bila tidak ada yang melewati ambang — sengaja tidak mengembalikan
     * kandidat terbaik "sekadar supaya ada": wajah yang tidak dikenali harus berakhir di
     * penolakan, bukan di absen atas nama orang lain.
     */
    fun cocokkan(
        probe: FloatArray,
        kandidat: List<KandidatWajah>,
        lockToStaffId: String? = null,
        ambang: Double = AMBANG,
    ): HasilCocok? {
        val normProbe = norma(probe)
        if (normProbe == 0.0) return null

        var terbaik: HasilCocok? = null
        for (k in kandidat) {
            if (lockToStaffId != null && k.staffId != lockToStaffId) continue
            // Descriptor dari model lama punya panjang berbeda. Server melewatinya
            // (CONTINUE), bukan menganggapnya tidak cocok — perilakunya ditiru di sini.
            if (k.descriptor.size != probe.size) continue

            val normKandidat = norma(k.descriptor)
            if (normKandidat == 0.0) continue

            var dot = 0.0
            for (i in probe.indices) dot += probe[i].toDouble() * k.descriptor[i].toDouble()
            val sim = dot / (normProbe * normKandidat)

            if (terbaik == null || sim > terbaik.similarity) {
                terbaik = HasilCocok(k.staffId, k.nama, sim)
            }
        }

        return terbaik?.takeIf { it.similarity >= ambang }
    }

    private fun norma(v: FloatArray): Double {
        var jumlah = 0.0
        for (x in v) jumlah += x.toDouble() * x.toDouble()
        return sqrt(jumlah)
    }
}
