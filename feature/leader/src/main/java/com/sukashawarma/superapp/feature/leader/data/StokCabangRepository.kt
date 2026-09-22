package com.sukashawarma.superapp.feature.leader.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.leader.domain.BahanCabang
import com.sukashawarma.superapp.feature.leader.domain.bahanCabang
import com.sukashawarma.superapp.feature.leader.domain.urutkanStok
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.bolehTampilDiOutlet

/**
 * Sisa bahan baku satu cabang.
 *
 * Sumbernya `monitoring_view_scoped` — view yang sama dipakai modul Stok native,
 * dan namanya berakhiran `_scoped` karena ia sudah menyaring diri ke
 * `accessible_outlet_ids()`. Penyaring `outlet_id` di bawah hanyalah pilihan
 * pengguna pada pemilih cabang, bukan kendali akses.
 *
 * `current_qty` view itu berskala campuran (lihat `saldo_is_gram`), jadi metadata
 * satuan `bahan_baku` ikut ditarik lewat embed dan dinormalkan di [bahanCabang] —
 * persis seperti `StokRepository.monitoringOutlet`. Bahan milik gudang pusat juga
 * disembunyikan dengan aturan yang sama, supaya daftar di sini dan di layar Stok
 * berisi bahan yang sama.
 *
 * Halaman web membaca `inventory_batches`/`inventory_items`/`inventory_units`.
 * Ketiganya TIDAK ADA di database ini — migrasi `20260709000001_merge_fifo_po`
 * membatalkan rencana FIFO dan malah menghapus `inventory_items`, dengan catatan
 * eksplisit bahwa sisanya tidak pernah dibuat. Karena itu layar stok leader di web
 * selalu berbunyi "Tidak Ada Data". Native membacanya dari tempat stok yang
 * sungguhan tersimpan; itu divergensi yang disengaja, bukan kelalaian menyalin.
 */
object StokCabangRepository {

    /** Satu outlet berisi puluhan bahan; satu halaman selebar ini sudah memuat semuanya. */
    private const val BATAS = 500

    suspend fun bahan(outletId: String): List<BahanCabang> {
        val baris = Postgrest.select(
            "monitoring_view_scoped",
            listOf(
                "select" to "bahan_baku_id,item_name,outlet_name,current_qty,saldo_is_gram,threshold,satuan," +
                    "bahan_baku(satuan,satuan_tengah,satuan_kecil,faktor_tengah,faktor_tampilan)",
                "outlet_id" to "eq.$outletId",
                "order" to "item_name.asc",
                "limit" to BATAS.toString(),
            ),
        ).mapNotNull { elemen ->
            val b = elemen.asJsonObject
            val id = b.optString("bahan_baku_id") ?: return@mapNotNull null
            val nama = b.optString("item_name")?.takeIf { it.isNotBlank() } ?: "(tanpa nama)"
            if (!bolehTampilDiOutlet(nama, b.optString("outlet_name").orEmpty())) return@mapNotNull null
            val bb = b.optJsonObject("bahan_baku")
            bahanCabang(
                id = id,
                nama = nama,
                currentQty = b.optDouble("current_qty") ?: 0.0,
                saldoIsGram = b.optBoolean("saldo_is_gram"),
                threshold = b.optDouble("threshold"),
                meta = UnitMeta(
                    satuan = bb?.optString("satuan") ?: b.optString("satuan"),
                    satuanTengah = bb?.optString("satuan_tengah"),
                    satuanKecil = bb?.optString("satuan_kecil"),
                    faktorTengah = bb?.optDouble("faktor_tengah"),
                    faktorTampilan = bb?.optDouble("faktor_tampilan"),
                ),
            )
        }
        return urutkanStok(baris)
    }
}
