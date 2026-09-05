package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.data.model.AbsenMasuk
import com.sukashawarma.superapp.feature.manager.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.manager.data.model.PesananRingkas
import com.sukashawarma.superapp.feature.manager.data.model.WasteDisetujui
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.ZONA_JAKARTA
import com.sukashawarma.superapp.feature.manager.domain.akhirIso
import com.sukashawarma.superapp.feature.manager.domain.awalIso
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Pembacaan data modul Manager.
 *
 * Tidak ada penyaringan `outlet_id` di sini, dan itu disengaja. Versi web memakai
 * service-role key yang menembus RLS, jadi ia WAJIB menyusun sendiri daftar outlet
 * yang boleh dilihat lalu menempelkannya ke tujuh query. Native memakai JWT
 * pengguna, sehingga `accessible_outlet_ids()` di database sudah membatasi hal yang
 * sama — menyaring ulang di klien hanya menambah cara baru untuk salah.
 */
object ManagerRepository {

    /** PostgREST memotong balasan di 1000 baris; rentang 30 hari jauh melewatinya. */
    private const val UKURAN_HALAMAN = 1000

    private val JAM_JAKARTA = DateTimeFormatter.ofPattern("HH.mm")

    /**
     * Membaca satu tabel sampai habis.
     *
     * Memakai `limit`/`offset`, bukan header Range, supaya jalurnya sama dengan
     * seluruh pembacaan lain di app dan tidak perlu API baru di [Postgrest].
     */
    private suspend fun selectSemua(
        tabel: String,
        params: List<Pair<String, String>>,
    ): List<JsonObject> {
        val hasil = mutableListOf<JsonObject>()
        var offset = 0
        while (true) {
            val halaman = Postgrest.select(
                tabel,
                params + ("limit" to UKURAN_HALAMAN.toString()) + ("offset" to offset.toString()),
            )
            halaman.forEach { hasil += it.asJsonObject }
            if (halaman.size() < UKURAN_HALAMAN) break
            offset += UKURAN_HALAMAN
        }
        return hasil
    }

    suspend fun outlets(): List<OutletRingkas> =
        selectSemua("outlets", listOf("select" to "id,name,is_active", "order" to "name"))
            .mapNotNull { baris ->
                val id = baris.optString("id") ?: return@mapNotNull null
                OutletRingkas(id, baris.optString("name").orEmpty(), baris.optBoolean("is_active"))
            }

    /**
     * Pesanan selesai pada [rentang]. `order_items` ikut dibawa embedded dalam satu
     * permintaan — menariknya terpisah berarti satu query tambahan per halaman pesanan.
     */
    suspend fun pesananSelesai(rentang: RentangTanggal): List<PesananRingkas> =
        selectSemua(
            "orders",
            listOf(
                "select" to "id,outlet_id,total_amount,order_items(quantity)",
                "status" to "eq.completed",
                "created_at" to "gte.${rentang.awalIso()}",
                "created_at" to "lte.${rentang.akhirIso()}",
                "order" to "created_at.asc",
            ),
        ).mapNotNull { baris ->
            val outletId = baris.optString("outlet_id") ?: return@mapNotNull null
            PesananRingkas(
                outletId = outletId,
                total = baris.optDouble("total_amount")?.toLong() ?: 0L,
                jumlahItem = baris.optJsonArray("order_items").jumlahPorsi(),
            )
        }

    private fun JsonArray?.jumlahPorsi(): Int =
        this?.sumOf { it.asJsonObject.optInt("quantity") ?: 0 } ?: 0

    /**
     * Absen masuk PERTAMA tiap outlet pada hari terakhir [rentang] — itulah penanda
     * sebuah cabang sudah buka. Baris dibaca urut waktu, jadi kemunculan pertama
     * sebuah outlet adalah absen paling awal miliknya.
     */
    suspend fun jamBukaOutlet(rentang: RentangTanggal): List<AbsenMasuk> {
        val hariTerakhir = RentangTanggal(rentang.sampai, rentang.sampai)
        val pertama = LinkedHashMap<String, AbsenMasuk>()
        selectSemua(
            "attendance",
            listOf(
                "select" to "outlet_id,ts_server",
                "type" to "eq.in",
                "ts_server" to "gte.${hariTerakhir.awalIso()}",
                "ts_server" to "lte.${hariTerakhir.akhirIso()}",
                "order" to "ts_server.asc",
            ),
        ).forEach { baris ->
            val outletId = baris.optString("outlet_id") ?: return@forEach
            if (pertama.containsKey(outletId)) return@forEach
            val jam = baris.optString("ts_server")?.let(::jamJakarta) ?: return@forEach
            pertama[outletId] = AbsenMasuk(outletId, jam)
        }
        return pertama.values.toList()
    }

    /** `2026-09-05T01:15:00+00:00` menjadi `08.15` waktu Jakarta. */
    private fun jamJakarta(tsServer: String): String? = try {
        OffsetDateTime.parse(tsServer).atZoneSameInstant(ZONA_JAKARTA).format(JAM_JAKARTA)
    } catch (e: java.time.format.DateTimeParseException) {
        null
    }

    /**
     * Pemetaan outlet ke nama area manager.
     *
     * RLS `staff_outlets_select_self` hanya memberi baris milik pengguna sendiri,
     * jadi untuk regional manager hasilnya kosong dan penamaan zona jatuh ke daftar
     * cadangan di [com.sukashawarma.superapp.feature.manager.domain.AreaManagerNama].
     * Tetap dibaca karena murah dan langsung berguna begitu kebijakan bacanya
     * diperluas di sisi database.
     */
    suspend fun pemetaanAreaManager(): Map<String, String> =
        selectSemua(
            "staff_outlets",
            listOf("select" to "outlet_id,outlet_staff(name,role,is_active)"),
        ).mapNotNull { baris ->
            val outletId = baris.optString("outlet_id") ?: return@mapNotNull null
            val staf = baris.optJsonObject("outlet_staff") ?: return@mapNotNull null
            if (staf.optString("role") != "area_manager") return@mapNotNull null
            if (!staf.optBoolean("is_active", default = true)) return@mapNotNull null
            val nama = staf.optString("name")?.trim().orEmpty()
            if (nama.isEmpty()) return@mapNotNull null
            outletId to nama
        }.toMap()

    suspend fun wasteDisetujui(rentang: RentangTanggal): List<WasteDisetujui> =
        selectSemua(
            "stok_waste_reports",
            listOf(
                "select" to "bahan_baku_id,qty",
                "status" to "eq.APPROVED",
                "created_at" to "gte.${rentang.awalIso()}",
                "created_at" to "lte.${rentang.akhirIso()}",
            ),
        ).mapNotNull { baris ->
            val bahan = baris.optString("bahan_baku_id") ?: return@mapNotNull null
            WasteDisetujui(bahan, baris.optDouble("qty") ?: 0.0)
        }

    /**
     * Jumlah waste yang masih menunggu persetujuan — TANPA batas tanggal, sama seperti
     * web: laporan yang menggantung sejak minggu lalu justru yang paling perlu terlihat.
     */
    suspend fun jumlahWasteMenunggu(): Int =
        selectSemua("stok_waste_reports", listOf("select" to "id", "status" to "eq.PENDING")).size

    /** Harga beli untuk bahan yang muncul di laporan waste. */
    suspend fun hargaBahan(bahanIds: Collection<String>): Map<String, Double> {
        if (bahanIds.isEmpty()) return emptyMap()
        return selectSemua(
            "bahan_baku_harga",
            listOf(
                "select" to "bahan_baku_id,harga_beli",
                "bahan_baku_id" to "in.(${bahanIds.joinToString(",")})",
            ),
        ).mapNotNull { baris ->
            val id = baris.optString("bahan_baku_id") ?: return@mapNotNull null
            id to (baris.optDouble("harga_beli") ?: 0.0)
        }.toMap()
    }

    /** Seluruh pembacaan satu layar Ringkasan Area, dijalankan berbarengan. */
    suspend fun muatRingkasan(
        rentang: RentangTanggal,
        rentangPembanding: RentangTanggal,
    ): DataRingkasan = coroutineScope {
        val outlets = async { outlets() }
        val pesanan = async { pesananSelesai(rentang) }
        val pesananSebelumnya = async { pesananSelesai(rentangPembanding) }
        val absen = async { jamBukaOutlet(rentang) }
        val pemetaan = async { pemetaanAreaManager() }
        val waste = async { wasteDisetujui(rentang) }
        val wastePending = async { jumlahWasteMenunggu() }

        val barisWaste = waste.await()
        // Harga baru bisa diminta setelah tahu bahan mana yang terbuang, jadi query ini
        // memang menunggu — sisanya sudah berjalan paralel sejak awal.
        val harga = hargaBahan(barisWaste.map { it.bahanBakuId }.toSet())

        DataRingkasan(
            outlets = outlets.await(),
            pesanan = pesanan.await(),
            pesananSebelumnya = pesananSebelumnya.await(),
            absenMasuk = absen.await(),
            pemetaanAreaManager = pemetaan.await(),
            waste = barisWaste,
            hargaBahan = harga,
            wasteMenungguPersetujuan = wastePending.await(),
        )
    }
}

/** Kumpulan baris mentah satu layar; diolah menjadi tampilan oleh `susunRingkasanArea`. */
data class DataRingkasan(
    val outlets: List<OutletRingkas>,
    val pesanan: List<PesananRingkas>,
    val pesananSebelumnya: List<PesananRingkas>,
    val absenMasuk: List<AbsenMasuk>,
    val pemetaanAreaManager: Map<String, String>,
    val waste: List<WasteDisetujui>,
    val hargaBahan: Map<String, Double>,
    val wasteMenungguPersetujuan: Int,
)
