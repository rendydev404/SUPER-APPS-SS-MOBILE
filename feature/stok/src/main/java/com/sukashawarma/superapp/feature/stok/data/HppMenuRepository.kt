package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.domain.BahanMentahHpp
import com.sukashawarma.superapp.feature.stok.domain.MenuHpp
import com.sukashawarma.superapp.feature.stok.domain.susunHpp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Kalkulator HPP menu — cermin `app/actions/hppMenu.ts`.
 *
 * Web memakai service-role karena Server Action-nya memang begitu. Native
 * membaca `bahan_baku_harga` langsung, dan itu memang terbuka untuk role yang
 * halaman ini tujukan: admin, owner, kitchen, purchasing, dan admin finance.
 * Role outlet tidak diberi menu ini, sama seperti web.
 */
object HppMenuRepository {

    suspend fun muat(): List<MenuHpp> = coroutineScope {
        val resep = async {
            allRows(
                "resep",
                listOf(
                    "select" to "id,nama,menu_item_ref,resep_item(id,bahan_baku_id,qty_per_porsi,satuan," +
                        "bahan_baku(id,nama,satuan,satuan_kecil,faktor_konversi,faktor_tampilan,kategori))",
                    "is_active" to "eq.true",
                ),
            )
        }
        val menu = async {
            allRows("menu_items", listOf("select" to "id,name,price,category_id,categories(name)"))
                .mapNotNull { baris ->
                    val id = baris.optString("id") ?: return@mapNotNull null
                    id to MenuRingkas(
                        nama = baris.optString("name").orEmpty(),
                        harga = baris.optDouble("price") ?: 0.0,
                        kategori = baris.optJsonObject("categories")?.optString("name") ?: "Umum",
                    )
                }.toMap()
        }
        val harga = async {
            allRows("bahan_baku_harga", listOf("select" to "bahan_baku_id,harga_beli,harga_beli_display,kemasan_qty"))
                .mapNotNull { baris ->
                    val id = baris.optString("bahan_baku_id") ?: return@mapNotNull null
                    id to HargaRingkas(
                        // `harga_beli_display` diutamakan, sama seperti web —
                        // itu harga per kemasan yang benar-benar dibeli.
                        harga = baris.optDouble("harga_beli_display")?.takeIf { it > 0.0 }
                            ?: baris.optDouble("harga_beli") ?: 0.0,
                        kemasanQty = baris.optDouble("kemasan_qty"),
                    )
                }.toMap()
        }

        val petaMenu = menu.await()
        val petaHarga = harga.await()

        resep.await().mapNotNull { baris ->
            val resepId = baris.optString("id") ?: return@mapNotNull null
            val ref = baris.optString("menu_item_ref")
            val menuData = ref?.let { petaMenu[it] }
            val items = baris.optJsonArray("resep_item") ?: return@mapNotNull null

            val bahan = items.mapNotNull { elemen ->
                val item = elemen.asJsonObject
                val bb = item.optJsonObject("bahan_baku") ?: return@mapNotNull null
                val bahanId = bb.optString("id") ?: return@mapNotNull null
                val hargaBahan = petaHarga[bahanId]
                BahanMentahHpp(
                    bahanBakuId = bahanId,
                    nama = bb.optString("nama").orEmpty(),
                    satuanResep = item.optString("satuan") ?: bb.optString("satuan").orEmpty(),
                    qtyPerPorsi = item.optDouble("qty_per_porsi") ?: 0.0,
                    hargaBeli = hargaBahan?.harga ?: 0.0,
                    kemasanQty = hargaBahan?.kemasanQty,
                    faktorTampilan = bb.optDouble("faktor_tampilan"),
                    faktorKonversi = bb.optDouble("faktor_konversi"),
                )
            }

            susunHpp(
                resepId = resepId,
                // Resep tanpa menu terpaut tetap ditampilkan dengan namanya sendiri;
                // menyembunyikannya akan membuat resep yang belum dijual seolah
                // tidak punya biaya.
                menuNama = menuData?.nama ?: baris.optString("nama").orEmpty(),
                kategori = menuData?.kategori ?: "Menu Lain",
                hargaJual = menuData?.harga ?: 0.0,
                bahanMentah = bahan,
            )
        }.sortedBy { it.menuNama }
    }

    private data class MenuRingkas(val nama: String, val harga: Double, val kategori: String)
    private data class HargaRingkas(val harga: Double, val kemasanQty: Double?)
}
