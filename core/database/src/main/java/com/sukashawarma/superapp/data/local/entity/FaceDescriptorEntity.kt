package com.sukashawarma.superapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Descriptor wajah staf satu outlet, disalin ke perangkat agar absensi tetap bisa mengenali
 * orang saat internet mati. Tanpa ini mode offline absensi tidak ada artinya: pencocokan
 * hari ini seluruhnya di server (RPC `match_face_mobile`), jadi offline crew bahkan tidak
 * sampai ke layar liveness.
 *
 * Ini data biometrik, maka aturannya ketat:
 * - hanya staf AKTIF di outlet perangkat itu, bukan seluruh perusahaan;
 * - [descriptor] disimpan terenkripsi (kunci di Android Keystore), bukan float mentah;
 * - dihapus total saat logout dan saat staf pindah outlet.
 *
 * Saat online, RPC server tetap yang otoritatif. Salinan ini murni jalur darurat, dan hasil
 * pencocokannya tetap diverifikasi ulang server ketika antrean absensi disinkronkan.
 */
@Entity(tableName = "face_descriptor_cache", indices = [Index("outletId")])
data class FaceDescriptorEntity(
    @PrimaryKey val staffId: String,
    val outletId: String,
    val nama: String,
    /** Vektor ArcFace terenkripsi. Bentuk mentahnya float32 sepanjang 512. */
    val descriptor: ByteArray,
    val updatedAtMs: Long,
) {
    /**
     * [ByteArray] dibandingkan per-referensi secara bawaan, sehingga dua baris dengan isi sama
     * akan tampak berbeda — cukup untuk membuat perbandingan di test dan di DiffUtil keliru.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceDescriptorEntity) return false
        return staffId == other.staffId &&
            outletId == other.outletId &&
            nama == other.nama &&
            updatedAtMs == other.updatedAtMs &&
            descriptor.contentEquals(other.descriptor)
    }

    override fun hashCode(): Int {
        var hasil = staffId.hashCode()
        hasil = 31 * hasil + outletId.hashCode()
        hasil = 31 * hasil + nama.hashCode()
        hasil = 31 * hasil + updatedAtMs.hashCode()
        hasil = 31 * hasil + descriptor.contentHashCode()
        return hasil
    }
}
