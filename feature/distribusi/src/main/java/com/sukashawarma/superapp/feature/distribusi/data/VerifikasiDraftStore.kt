package com.sukashawarma.superapp.feature.distribusi.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.distribusi.domain.IsianVerifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem

/** Isi layar verifikasi yang sedang dikerjakan, cukup untuk memulihkan
 *  keadaan persis seperti saat aplikasi ditutup. */
data class DraftVerifikasi(
    val isian: Map<String, IsianVerifikasi>,
    val indeksItem: Int,
    val langkah: String,
    val kondisiTerkonfirmasi: Boolean,
)

/**
 * Draft verifikasi dan penanda gerbang QR, disimpan lokal per surat jalan.
 *
 * Web menyimpan hal yang sama di `localStorage`. Bedanya, di HP aplikasi jauh
 * lebih sering dipindah ke latar belakang dan dibunuh sistem, jadi draft ini
 * bukan kenyamanan tambahan melainkan syarat supaya crew tidak mengulang
 * pengisian dari nol.
 *
 * Kunci selalu memuat `suratJalanId`, jadi dua surat jalan yang dikerjakan
 * berdekatan tidak saling menimpa.
 */
object VerifikasiDraftStore {
    private const val PREFS_NAME = "distribusi_verifikasi"
    private const val PREFIX_DRAFT = "draft_"
    private const val PREFIX_UNLOCK = "unlock_"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun simpan(suratJalanId: String, draft: DraftVerifikasi) {
        prefs?.edit()?.putString(PREFIX_DRAFT + suratJalanId, draftKeJson(draft))?.apply()
    }

    fun muat(suratJalanId: String): DraftVerifikasi? {
        val teks = prefs?.getString(PREFIX_DRAFT + suratJalanId, null) ?: return null
        return draftDariJson(teks)
    }

    fun hapus(suratJalanId: String) {
        prefs?.edit()
            ?.remove(PREFIX_DRAFT + suratJalanId)
            ?.remove(PREFIX_UNLOCK + suratJalanId)
            ?.apply()
    }

    /** Dipanggil setelah QR atau kode manual berhasil dicocokkan. */
    fun tandaiTerbuka(suratJalanId: String) {
        prefs?.edit()?.putBoolean(PREFIX_UNLOCK + suratJalanId, true)?.apply()
    }

    /** Layar verifikasi menolak dibuka bila ini false, termasuk saat
     *  dinavigasi langsung tanpa melewati pemindai. */
    fun sudahTerbuka(suratJalanId: String): Boolean =
        prefs?.getBoolean(PREFIX_UNLOCK + suratJalanId, false) ?: false
}

// Serialisasi dipisah dari SharedPreferences supaya bisa diuji dengan JUnit biasa.

internal fun draftKeJson(draft: DraftVerifikasi): String {
    val isian = JsonObject()
    draft.isian.forEach { (itemId, nilai) ->
        val o = JsonObject()
        if (nilai.qtyTerima == null) o.add("qty", null) else o.addProperty("qty", nilai.qtyTerima)
        o.addProperty("kondisi", nilai.kondisi.name)
        o.addProperty("catatan", nilai.catatan)
        o.addProperty("foto", nilai.fotoPath)
        isian.add(itemId, o)
    }
    val akar = JsonObject()
    akar.add("isian", isian)
    akar.addProperty("indeks", draft.indeksItem)
    akar.addProperty("langkah", draft.langkah)
    akar.addProperty("konfirmasi", draft.kondisiTerkonfirmasi)
    return akar.toString()
}

internal fun draftDariJson(teks: String): DraftVerifikasi? = try {
    val akar = JsonParser.parseString(teks).asJsonObject
    val isian = akar.optJsonObject("isian") ?: JsonObject()
    DraftVerifikasi(
        isian = isian.entrySet().associate { (itemId, elemen) ->
            val o = elemen.asJsonObject
            itemId to IsianVerifikasi(
                qtyTerima = o.optDouble("qty"),
                kondisi = KondisiItem.entries.find { it.name == o.optString("kondisi") }
                    ?: KondisiItem.BAIK,
                catatan = o.optString("catatan").orEmpty(),
                fotoPath = o.optString("foto"),
            )
        },
        indeksItem = akar.optInt("indeks") ?: 0,
        langkah = akar.optString("langkah") ?: "kartu",
        kondisiTerkonfirmasi = akar.optBoolean("konfirmasi"),
    )
} catch (e: Exception) {
    // Draft rusak jangan sampai membuat layar verifikasi gagal dibuka —
    // lebih baik mulai dari kosong daripada tidak bisa menerima barang.
    null
}
