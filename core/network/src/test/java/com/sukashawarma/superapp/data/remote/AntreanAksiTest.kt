package com.sukashawarma.superapp.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Dua aturan yang menentukan apakah kerja yang sudah dilakukan crew di lapangan dibuang atau
 * dipertahankan. Keduanya hanya muncul pada urutan kejadian yang tidak akan pernah terjadi di
 * pengujian manual: koneksi yang putus persis setelah server mencatat, dan sesi yang belum
 * pulih persis saat jaringan kembali.
 */
class AntreanAksiTest {

    // --- abaikanDuplikat ---

    @Test
    fun `kiriman yang ternyata sudah tercatat dianggap berhasil`() = runBlocking {
        // Skenarionya: kiriman pertama sampai di server, lalu sinyal putus sebelum jawabannya
        // tiba. Klien mengantrekan ulang dengan client_op_id yang sama, dan indeks unik
        // menolaknya. Kalau itu dihitung gagal, pengguna diberi tahu kasbonnya tidak masuk
        // padahal justru sudah.
        abaikanDuplikat {
            throw Postgrest.PostgrestException(
                409,
                """duplicate key value violates unique constraint "cash_advances_client_op_id_key"""",
            )
        }
    }

    @Test
    fun `penolakan lain tetap dilempar`() {
        val galat = runCatching {
            runBlocking {
                abaikanDuplikat { throw Postgrest.PostgrestException(403, "new row violates row-level security") }
            }
        }.exceptionOrNull()
        assertTrue(galat is Postgrest.PostgrestException)
        assertEquals(403, (galat as Postgrest.PostgrestException).code)
    }

    @Test
    fun `konflik yang bukan duplikat tetap dilempar`() {
        // 409 juga dipakai untuk pelanggaran foreign key dan konflik lain yang benar-benar
        // salah; menelannya berarti menyembunyikan data yang tidak pernah tersimpan.
        val galat = runCatching {
            runBlocking {
                abaikanDuplikat { throw Postgrest.PostgrestException(409, "update or delete violates foreign key") }
            }
        }.exceptionOrNull()
        assertTrue(galat is Postgrest.PostgrestException)
    }

    // --- Outbox.bolehDicobaLagi ---

    @Test
    fun `sesi yang belum pulih tidak membuang antrean`() {
        // Sesi yang dibuka dari snapshot offline belum punya access token yang hidup. Kalau
        // 401 dihitung permanen, seluruh antrean hangus persis pada detik jaringan kembali —
        // sebelum pemulihan sesi sempat berjalan.
        assertTrue(Outbox.bolehDicobaLagi(Postgrest.PostgrestException(401, "JWT expired")))
    }

    @Test
    fun `penolakan RLS tidak diulang selamanya`() {
        // 403 adalah jawaban bahwa orang ini memang tidak berhak. Mengulanginya seribu kali
        // tidak mengubah apa pun, dan penggunanya perlu diberi tahu.
        assertFalse(Outbox.bolehDicobaLagi(Postgrest.PostgrestException(403, "not authorized")))
    }

    @Test
    fun `gangguan jaringan selalu boleh diulang`() {
        assertTrue(Outbox.bolehDicobaLagi(IOException("koneksi terputus")))
        assertTrue(Outbox.bolehDicobaLagi(Postgrest.PostgrestException(503, "unavailable")))
    }

    @Test
    fun `permintaan yang salah bentuk tidak diulang`() {
        assertFalse(Outbox.bolehDicobaLagi(Postgrest.PostgrestException(400, "invalid input syntax")))
    }
}
