package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.domain.BarisResep
import com.sukashawarma.superapp.feature.manager.domain.HargaBahan
import com.sukashawarma.superapp.feature.manager.domain.MenuHpp
import com.sukashawarma.superapp.feature.manager.domain.ResepMenu
import com.sukashawarma.superapp.feature.manager.domain.URUTAN_MENU_HPP
import com.sukashawarma.superapp.feature.manager.domain.hitungHppPaket
import com.sukashawarma.superapp.feature.manager.domain.kelompokMenu
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class DataHpp(
    val menu: List<MenuHpp>,
    /** menu_id -> resep global menu itu. */
    val resep: Map<String, ResepMenu>,
)

/**
 * Pembacaan katalog menu dan resep untuk layar Resep & HPP.
 *
 * Seluruhnya baca-saja dan tidak terhalang RLS: `menu_items`, `categories`,
 * `resep`, dan `resep_item` semuanya punya policy SELECT terbuka, sedangkan
 * `bahan_baku_sku` dan `bahan_baku_harga` terbuka untuk `authenticated`.
 * Penyuntingan resep tetap di web — policy tulisnya hanya untuk admin.
 */
object HppRepository {

    private const val UKURAN_HALAMAN = 1000

    private suspend fun selectSemua(tabel: String, params: List<Pair<String, String>>): List<JsonObject> {
        val hasil = mutableListOf<JsonObject>()
        var offset = 0
        while (true) {
            val halaman = Postgrest.select(
                tabel,
                params + listOf("limit" to UKURAN_HALAMAN.toString(), "offset" to offset.toString()),
            )
            halaman.forEach { hasil += it.asJsonObject }
            if (halaman.size() < UKURAN_HALAMAN) break
            offset += UKURAN_HALAMAN
        }
        return hasil
    }

    suspend fun muat(): DataHpp = coroutineScope {
        val menu = async { menuMentah() }
        val resep = async { resepMentah() }

        val barisMenu = menu.await()
        val resepPerMenu = resep.await()

        // HPP tiap menu tunggal lebih dulu; paket membutuhkannya sebagai bahan hitung.
        val hppTunggal = mutableMapOf<String, Long>()
        barisMenu.forEach { baris ->
            val id = baris.optString("id") ?: return@forEach
            val override = baris.optDouble("hpp_override")?.toLong()
            val dariResep = resepPerMenu[id]?.totalHpp
            (override ?: dariResep)?.let { hppTunggal[id] = it }
        }

        DataHpp(
            menu = barisMenu.mapNotNull { petakanMenu(it, resepPerMenu, hppTunggal) }
                .sortedWith(URUTAN_MENU_HPP),
            resep = resepPerMenu,
        )
    }

    private suspend fun menuMentah(): List<JsonObject> = selectSemua(
        "menu_items",
        listOf(
            "select" to (
                "id,name,price,hpp_override,available_online_channels,is_available," +
                    "is_package,sort_order,categories(name)," +
                    "package_items:menu_packages!package_id(menu_item_id,quantity)"
                ),
            "order" to "sort_order",
        ),
    )

    /**
     * Resep berlingkup global — yang sama dipakai seluruh outlet.
     *
     * Harga bahan ikut di-embed lewat dua jalur sekaligus (`bahan_baku_sku` dan
     * `bahan_baku_harga`) karena keduanya memang dipakai sebagai sumber cadangan
     * satu sama lain; memilih mana yang berlaku dikerjakan [hargaBahan].
     */
    private suspend fun resepMentah(): Map<String, ResepMenu> = selectSemua(
        "resep",
        listOf(
            "select" to (
                "menu_item_ref,is_active,buffer_amount," +
                    "resep_item(id,bahan_baku_id,qty_per_porsi," +
                    "bahan_baku:bahan_baku_id(id,nama,satuan," +
                    "bahan_baku_sku(harga_beli,qty_isi,is_default,is_active)," +
                    "bahan_baku_harga(harga_beli_display,kemasan_qty)))"
                ),
            "scope" to "eq.global",
        ),
    ).mapNotNull { baris ->
        val menuId = baris.optString("menu_item_ref") ?: return@mapNotNull null
        menuId to ResepMenu(
            menuId = menuId,
            aktif = baris.optBoolean("is_active", default = true),
            buffer = baris.optDouble("buffer_amount")?.toLong() ?: 0L,
            baris = baris.optJsonArray("resep_item")?.mapNotNull { elemen ->
                petakanBarisResep(elemen.asJsonObject)
            }.orEmpty(),
        )
    }.toMap()

    private fun petakanBarisResep(baris: JsonObject): BarisResep? {
        val bahanId = baris.optString("bahan_baku_id") ?: return null
        val bahan = baris.optJsonObject("bahan_baku")
        return BarisResep(
            bahanId = bahanId,
            bahanNama = bahan?.optString("nama") ?: "Bahan tidak dikenal",
            qtyPerPorsi = baris.optDouble("qty_per_porsi") ?: 0.0,
            satuan = bahan?.optString("satuan").orEmpty(),
            harga = hargaBahan(bahan),
        )
    }

    /**
     * Memilih harga acuan sebuah bahan.
     *
     * SKU aktif menang, yang ditandai default lebih dulu — cermin
     * `calcHppFromRecipe` web. `bahan_baku_harga` hanya dipakai kalau tidak ada
     * satu pun SKU aktif, dan barisnya diambil yang pertama seperti di sana.
     */
    private fun hargaBahan(bahan: JsonObject?): HargaBahan {
        if (bahan == null) return HargaBahan(0.0, 0.0)

        val sku = bahan.optJsonArray("bahan_baku_sku")
            ?.map { it.asJsonObject }
            ?.filter { it.optBoolean("is_active") }
            .orEmpty()
        if (sku.isNotEmpty()) {
            val dipakai = sku.firstOrNull { it.optBoolean("is_default") } ?: sku.first()
            return HargaBahan(
                hargaBeli = dipakai.optDouble("harga_beli") ?: 0.0,
                qtyIsi = dipakai.optDouble("qty_isi") ?: 0.0,
            )
        }

        val harga = bahan.optJsonArray("bahan_baku_harga")?.firstOrNull()?.asJsonObject
            ?: bahan.optJsonObject("bahan_baku_harga")
        return HargaBahan(
            hargaBeli = harga?.optDouble("harga_beli_display") ?: 0.0,
            qtyIsi = harga?.optDouble("kemasan_qty") ?: 0.0,
        )
    }

    private fun petakanMenu(
        baris: JsonObject,
        resep: Map<String, ResepMenu>,
        hppTunggal: Map<String, Long>,
    ): MenuHpp? {
        val id = baris.optString("id") ?: return null
        val nama = baris.optString("name").orEmpty()
        val paket = baris.optBoolean("is_package")
        val kategori = baris.optJsonObject("categories")?.optString("name")
        val channel = baris.optJsonArray("available_online_channels")
            ?.mapNotNull { it.takeIf { e -> !e.isJsonNull }?.asString }
            .orEmpty()

        val komponen = baris.optJsonArray("package_items")?.mapNotNull { elemen ->
            val isi = elemen.asJsonObject
            val menuId = isi.optString("menu_item_id") ?: return@mapNotNull null
            menuId to (isi.optInt("quantity") ?: 1)
        }.orEmpty()

        val (hppResep, parsial) = if (paket) {
            hitungHppPaket(komponen, hppTunggal) ?: (null to false)
        } else {
            resep[id]?.totalHpp to false
        }

        return MenuHpp(
            id = id,
            nama = nama,
            kelompok = kelompokMenu(nama, kategori, paket, channel),
            kategoriAsli = kategori,
            hargaJual = baris.optDouble("price")?.toLong() ?: 0L,
            hppResep = hppResep,
            hppOverride = baris.optDouble("hpp_override")?.toLong(),
            paket = paket,
            parsial = parsial,
            tersedia = baris.optBoolean("is_available", default = true),
            urutan = baris.optInt("sort_order") ?: 0,
        )
    }
}
