package com.sukashawarma.superapp.feature.stok.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pembacaan saldo SELURUH outlet untuk papan pantau Gudang Pusat.
 *
 * Bentuknya sengaja berbeda dari [StokRepository.monitoringOutlet] demi hemat database
 * dan jaringan — papan ini memuat ±1.150 baris (27 outlet), bukan ±55:
 *
 * 1. **Kolom ramping.** Hanya lima kolom per baris. Nama bahan, kategori, dan satuan
 *    SAMA untuk tiap outlet, jadi diambil sekali dari `bahan_baku` (±70 baris, TTL
 *    10 menit) lalu digabung di memori — bukan di-embed ulang 1.150 kali. Kolom
 *    `projection_text` / `last_opname_date` (subquery & fungsi per baris) tidak diminta,
 *    sehingga Postgres tidak menghitungnya sama sekali. Terukur ±110 ms di server.
 * 2. **Halaman 1.000.** Tiap halaman mengeksekusi ulang view penuh; halaman 200 seperti
 *    layar outlet berarti enam kali eksekusi. 1.000 = batas `max-rows` PostgREST.
 * 3. **Tanpa realtime.** `stok_balance` berubah di tiap transaksi kasir 27 outlet;
 *    berlangganan berarti server Realtime memeriksa RLS untuk tiap perubahan itu dan
 *    HP menyedot ulang 1.150 baris berkali-kali per menit. Layar memakai polling
 *    jarang yang hanya hidup saat terlihat — sama seperti fallback 120 detik papan web.
 * 4. **Cache memori + satu pemuatan berjalan.** Pemicu ganda (polling bertabrakan dengan
 *    tarik-segarkan) menunggu hasil yang sama, bukan menembak query kedua.
 */
object PantauOutletRepository {

    private const val PAGE = 1000
    private const val MAX_PAGE = 10
    private const val TTL_SALDO_MS = 45_000L
    private const val TTL_KATALOG_MS = 10 * 60_000L

    private data class Bahan(
        val nama: String,
        val kategori: String?,
        val faktorKonversi: Double?,
        val meta: UnitMeta,
    )

    private data class Saldo(
        val outletId: String,
        val bahanBakuId: String,
        val qty: Double,
        val threshold: Double?,
        val saldoIsGram: Boolean,
    )

    private val kunci = Mutex()
    @Volatile private var katalog: Pair<Long, Map<String, Bahan>>? = null
    @Volatile private var saldo: Pair<Long, List<Saldo>>? = null

    /** Buang saldo saja; katalog satuan jarang berubah dan tetap dipakai. */
    fun invalidateSaldo() { saldo = null }

    fun invalidate() {
        saldo = null
        katalog = null
    }

    /**
     * Seluruh baris monitoring lintas outlet, siap dihitung statusnya.
     * [namaOutlet] memetakan id ke nama dari daftar outlet yang sudah dimuat.
     */
    suspend fun semuaOutlet(namaOutlet: Map<String, String>): List<MonitoringRow> = kunci.withLock {
        val sekarang = System.currentTimeMillis()
        val (bahan, isi) = coroutineScope {
            val k = async { katalog?.takeIf { sekarang - it.first < TTL_KATALOG_MS }?.second ?: muatKatalog() }
            val s = async { saldo?.takeIf { sekarang - it.first < TTL_SALDO_MS }?.second ?: muatSaldo() }
            k.await() to s.await()
        }
        isi.mapNotNull { s ->
            // Bahan yang tidak ada di katalog (baru dinonaktifkan di antara dua pemuatan)
            // dilewati: tanpa faktor satuan statusnya tidak bisa dihitung dengan jujur.
            val b = bahan[s.bahanBakuId] ?: return@mapNotNull null
            MonitoringRow(
                outletId = s.outletId,
                outletName = namaOutlet[s.outletId] ?: "Outlet",
                bahanBakuId = s.bahanBakuId,
                itemName = b.nama,
                currentQty = s.qty,
                threshold = s.threshold,
                statusView = null,
                // Kolom view selalu `false`; tidak perlu dikirim lewat jaringan.
                isFlagged = false,
                saldoIsGram = s.saldoIsGram,
                lastOpnameDate = null,
                kategori = b.kategori,
                satuan = b.meta.satuan,
                faktorKonversi = b.faktorKonversi,
                meta = b.meta,
            )
        }
    }

    private suspend fun muatKatalog(): Map<String, Bahan> {
        val hasil = Postgrest.select(
            "bahan_baku",
            listOf(
                "select" to "id,nama,kategori,satuan,satuan_tengah,satuan_kecil," +
                    "faktor_tengah,faktor_tampilan,faktor_konversi",
                "is_active" to "eq.true",
            ),
        ).mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@mapNotNull null
            id to Bahan(
                nama = o.optString("nama") ?: "(tanpa nama)",
                kategori = o.optString("kategori"),
                faktorKonversi = o.optDouble("faktor_konversi"),
                meta = UnitMeta(
                    satuan = o.optString("satuan"),
                    satuanTengah = o.optString("satuan_tengah"),
                    satuanKecil = o.optString("satuan_kecil"),
                    faktorTengah = o.optDouble("faktor_tengah"),
                    faktorTampilan = o.optDouble("faktor_tampilan"),
                ),
            )
        }.toMap()
        katalog = System.currentTimeMillis() to hasil
        return hasil
    }

    private suspend fun muatSaldo(): List<Saldo> {
        val hasil = buildList {
            var halaman = 0
            while (halaman < MAX_PAGE) {
                val batch = Postgrest.select(
                    "monitoring_view_scoped",
                    listOf(
                        "select" to "outlet_id,bahan_baku_id,current_qty,threshold,saldo_is_gram",
                        // Urutan stabil wajib untuk paginasi offset; kunci unik (outlet, bahan).
                        "order" to "outlet_id.asc,bahan_baku_id.asc",
                        "limit" to PAGE.toString(),
                        "offset" to (halaman * PAGE).toString(),
                    ),
                ).mapNotNull { el ->
                    val o = el.asJsonObject
                    Saldo(
                        outletId = o.optString("outlet_id") ?: return@mapNotNull null,
                        bahanBakuId = o.optString("bahan_baku_id") ?: return@mapNotNull null,
                        qty = o.optDouble("current_qty") ?: 0.0,
                        threshold = o.optDouble("threshold"),
                        saldoIsGram = o.optBoolean("saldo_is_gram"),
                    )
                }
                addAll(batch)
                if (batch.size < PAGE) break
                halaman++
            }
        }.distinctBy { it.outletId to it.bahanBakuId }
        saldo = System.currentTimeMillis() to hasil
        return hasil
    }
}
