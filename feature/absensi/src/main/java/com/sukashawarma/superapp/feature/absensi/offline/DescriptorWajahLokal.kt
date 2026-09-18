package com.sukashawarma.superapp.feature.absensi.offline

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.local.AppDatabase
import com.sukashawarma.superapp.data.local.entity.FaceDescriptorEntity
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.face.KandidatWajah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Salinan descriptor wajah staf satu outlet di perangkat, untuk absensi saat internet mati.
 *
 * Ini data biometrik, jadi aturannya lebih ketat daripada cache biasa:
 * - hanya staf outlet perangkat itu (RPC `sync_face_descriptors` yang membatasi, bukan klien);
 * - disimpan terenkripsi dengan kunci di Android Keystore yang tidak pernah keluar dari
 *   perangkat, sehingga berkas database yang tersalin keluar tidak berisi wajah siapa pun;
 * - diganti utuh setiap sinkron, bukan ditambahkan — staf yang resign atau pindah harus
 *   hilang dari perangkat, bukan tertinggal dan tetap bisa absen;
 * - dihapus total saat logout.
 *
 * Saat online, pencocokan tetap lewat RPC server. Salinan ini tidak dipakai sama sekali.
 */
object DescriptorWajahLokal {
    private const val TAG = "DescriptorWajahLokal"
    private const val ALIAS_KUNCI = "suka_descriptor_wajah_v1"
    private const val TRANSFORMASI = "AES/GCM/NoPadding"
    private const val PANJANG_IV = 12
    private const val PANJANG_TAG_BIT = 128

    /** Disegarkan paling sering sekali sehari; enrollment baru tidak sesering itu. */
    const val UMUR_SEGAR_MS = 24L * 60 * 60 * 1000

    /**
     * Tarik ulang descriptor outlet dari server. Dipanggil saat online — saat login dan
     * saat layar absensi dibuka.
     *
     * [paksa] melewati pemeriksaan umur, dipakai setelah ada enrollment baru.
     * Mengembalikan jumlah descriptor yang tersimpan, atau -1 bila dilewati.
     */
    suspend fun sinkronkan(context: Context, outletId: String, paksa: Boolean = false): Int =
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.get(context).faceDescriptorDao()
            if (!paksa) {
                val terakhir = dao.disegarkanTerakhir(outletId) ?: 0L
                if (System.currentTimeMillis() - terakhir < UMUR_SEGAR_MS) return@withContext -1
            }

            val body = JsonObject().apply { addProperty("p_outlet_id", outletId) }
            val baris = Postgrest.rpc("sync_face_descriptors", body).asJsonArray

            val sekarang = System.currentTimeMillis()
            val entitas = baris.mapNotNull { elemen ->
                val obj = elemen.asJsonObject
                val staffId = obj.optString("staff_id") ?: return@mapNotNull null
                val descriptor = obj.get("descriptor")
                    ?.takeIf { it.isJsonArray }
                    ?.asJsonArray
                    ?.map { it.asFloat }
                    ?.toFloatArray()
                    ?: return@mapNotNull null
                if (descriptor.isEmpty()) return@mapNotNull null

                FaceDescriptorEntity(
                    staffId = staffId,
                    outletId = outletId,
                    nama = obj.optString("name").orEmpty(),
                    descriptor = enkripsi(keFloatBytes(descriptor)),
                    updatedAtMs = sekarang,
                )
            }

            // Hapus lalu simpan, bukan upsert: upsert menyisakan staf yang sudah tidak ada
            // di jawaban server.
            dao.hapusOutlet(outletId)
            dao.simpanSemua(entitas)
            entitas.size
        }

    /**
     * Kandidat untuk pencocokan offline. Baris yang gagal didekripsi dilewati, bukan
     * membuat seluruh pemanggilan gagal: satu baris rusak tidak boleh membuat semua orang
     * di outlet tidak bisa absen.
     */
    suspend fun kandidat(context: Context, outletId: String): List<KandidatWajah> =
        withContext(Dispatchers.IO) {
            AppDatabase.get(context).faceDescriptorDao().untukOutlet(outletId).mapNotNull { baris ->
                try {
                    KandidatWajah(baris.staffId, baris.nama, dariFloatBytes(dekripsi(baris.descriptor)))
                } catch (e: Exception) {
                    Log.e(TAG, "descriptor '${baris.staffId}' gagal dibaca", e)
                    null
                }
            }
        }

    suspend fun hapusSemua(context: Context) = withContext(Dispatchers.IO) {
        AppDatabase.get(context).faceDescriptorDao().hapusSemua()
    }

    // --- penyandian ---

    private fun kunci(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(ALIAS_KUNCI, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS_KUNCI,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Sengaja TIDAK setUserAuthenticationRequired: pencocokan berjalan di latar
                // layar kamera, jauh sebelum ada identitas yang bisa diminta membuka kunci.
                .build()
        )
        return generator.generateKey()
    }

    /** IV disimpan di depan ciphertext — GCM menuntut IV unik per enkripsi, bukan rahasia. */
    private fun enkripsi(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMASI).apply { init(Cipher.ENCRYPT_MODE, kunci()) }
        val terenkripsi = cipher.doFinal(data)
        return cipher.iv + terenkripsi
    }

    private fun dekripsi(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, PANJANG_IV)
        val isi = data.copyOfRange(PANJANG_IV, data.size)
        val cipher = Cipher.getInstance(TRANSFORMASI).apply {
            init(Cipher.DECRYPT_MODE, kunci(), GCMParameterSpec(PANJANG_TAG_BIT, iv))
        }
        return cipher.doFinal(isi)
    }

    private fun keFloatBytes(v: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        v.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun dariFloatBytes(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { buffer.getFloat() }
    }
}
