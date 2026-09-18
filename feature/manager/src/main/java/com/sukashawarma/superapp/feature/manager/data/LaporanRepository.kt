package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.domain.AnalitikLaporan
import com.sukashawarma.superapp.feature.manager.domain.FilterChannel
import com.sukashawarma.superapp.feature.manager.domain.FilterPembayaran
import com.sukashawarma.superapp.feature.manager.domain.ItemPesanan
import com.sukashawarma.superapp.feature.manager.domain.ItemTerjual
import com.sukashawarma.superapp.feature.manager.domain.RincianPembayaran
import com.sukashawarma.superapp.feature.manager.domain.labelMetodeBayar
import com.sukashawarma.superapp.feature.manager.domain.PesananLaporan
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.ZONA_JAKARTA
import com.sukashawarma.superapp.feature.manager.domain.akhirIso
import com.sukashawarma.superapp.feature.manager.domain.awalIso
import com.sukashawarma.superapp.feature.manager.domain.namaMenuPokok
import com.sukashawarma.superapp.feature.manager.domain.susunAnalitikLaporan
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Pembacaan pesanan untuk layar Laporan.
 *
 * Sama seperti repository lain di modul ini, cakupan outlet ditentukan RLS
 * (`orders_select_scoped` memakai `accessible_outlet_ids()`), bukan penyaringan di
 * klien. Penyaring outlet di layar adalah pilihan pengguna untuk mempersempit,
 * bukan pengaman.
 */
object LaporanRepository {

    private const val UKURAN_HALAMAN = 1000

    private const val KOLOM =
        "id,status,payment_method,channel,total_amount,discount_amount,promo_subsidy," +
            "created_at,outlet_id,order_items(menu_item_name,quantity,subtotal)"

    /** Nama RPC agregasi; lihat `plan/laporan-analitik-rpc.sql`. */
    private const val RPC_ANALITIK = "native_laporan_analitik"

    /**
     * Angka layar Laporan untuk satu periode.
     *
     * Dihitung di server lewat [RPC_ANALITIK]: satu panggilan mengembalikan sekitar
     * tiga puluh angka, menggantikan penarikan puluhan ribu baris pesanan beserta
     * itemnya yang lalu dijumlahkan di HP.
     *
     * Bila RPC-nya belum terpasang di database, PostgREST menjawab 404 dan jalur
     * lama dipakai kembali. Sengaja HANYA 404 yang ditangkap: galat lain (jaringan
     * putus, RLS menolak, server bermasalah) harus tetap terlihat sebagai galat,
     * bukan diam-diam berubah jadi permintaan raksasa yang justru memperparah
     * keadaan.
     */
    suspend fun analitik(
        rentang: RentangTanggal,
        channel: FilterChannel,
        pembayaran: FilterPembayaran,
        outletId: String?,
    ): AnalitikLaporan {
        val argumen = JsonObject().apply {
            addProperty("p_dari", rentang.awalIso())
            addProperty("p_sampai", rentang.akhirIso())
            if (channel.nilaiDb.isEmpty()) {
                add("p_channels", JsonNull.INSTANCE)
            } else {
                add("p_channels", JsonArray().apply { channel.nilaiDb.forEach { add(JsonPrimitive(it)) } })
            }
            addProperty("p_channel_null", channel.kolomKosong)
            if (pembayaran == FilterPembayaran.SEMUA) {
                add("p_payment", JsonNull.INSTANCE)
            } else {
                addProperty("p_payment", pembayaran.kunci)
            }
            if (outletId.isNullOrBlank()) {
                add("p_outlet", JsonNull.INSTANCE)
            } else {
                addProperty("p_outlet", outletId)
            }
        }

        val hasil = try {
            Postgrest.rpc(RPC_ANALITIK, argumen)
        } catch (e: Postgrest.PostgrestException) {
            val alasan = alasanJalurCadangan(e) ?: throw e
            android.util.Log.w("LaporanRepository", "$RPC_ANALITIK $alasan; memakai jalur baris mentah.")
            return susunAnalitikLaporan(pesanan(rentang, channel, pembayaran, outletId))
        }
        return bacaAnalitik(hasil.asJsonObject)
    }

    /**
     * Alasan memakai jalur baris mentah, atau null bila galatnya harus tetap muncul
     * ke pengguna.
     *
     * Hanya dua keadaan yang ditoleransi, dan keduanya berarti hal yang sama: RPC-nya
     * tidak bisa menjawab, sedangkan jalur lama sudah terbukti bisa. Galat lain
     * (jaringan putus, RLS menolak, server bermasalah) TIDAK ikut — menyembunyikannya
     * di balik permintaan raksasa hanya memperparah keadaan yang sudah buruk.
     */
    private fun alasanJalurCadangan(e: Postgrest.PostgrestException): String? = when {
        e.code == 404 ->
            "belum terpasang di database (jalankan plan/laporan-analitik-rpc.sql)"

        // 57014 = statement_timeout. Batas Supabase untuk peran `authenticated` adalah
        // 8 detik; agregasi yang melewatinya berarti rencana kueri di database belum
        // memadai — biasanya indeks pendukungnya belum ada. Jalur lama tetap lolos
        // karena setiap permintaannya dibatasi LIMIT, jadi boleh berhenti lebih awal.
        "57014" in e.message.orEmpty() ->
            "melewati batas waktu statement (lihat bagian INDEKS di plan/laporan-analitik-rpc.sql)"

        else -> null
    }

    /** Terbuka untuk pengujian: pemetaan balasan RPC ke model layar. */
    internal fun bacaAnalitik(o: JsonObject): AnalitikLaporan {
        fun angka(kunci: String): Long = o.optDouble(kunci)?.toLong() ?: 0L
        fun cacah(kunci: String): Int = o.optInt(kunci) ?: 0

        val pembayaran = (o.optJsonArray("rincian_pembayaran") ?: JsonArray()).map { el ->
            val r = el.asJsonObject
            val metode = r.optString("metode") ?: "unknown"
            RincianPembayaran(
                metode = metode,
                label = labelMetodeBayar(metode),
                jumlah = r.optInt("jumlah") ?: 0,
                omzet = r.optDouble("omzet")?.toLong() ?: 0L,
            )
        }

        // Tetap dipaksa 24 slot walau server sudah menjaminnya: layar mengindeks
        // daftar ini dengan jam mentah, dan daftar yang lebih pendek berarti crash.
        val perJamServer = (o.optJsonArray("per_jam") ?: JsonArray()).map { it.asInt }
        val perJam = List(24) { jam -> perJamServer.getOrElse(jam) { 0 } }

        val item = (o.optJsonArray("daftar_item") ?: JsonArray()).map { el ->
            val d = el.asJsonObject
            ItemTerjual(
                nama = d.optString("nama") ?: "Item",
                qty = d.optInt("qty") ?: 0,
                omzet = d.optDouble("omzet")?.toLong() ?: 0L,
            )
        }

        // Jam tersibuk dihitung di sini, bukan di SQL: aturannya "yang PERTAMA
        // mencapai puncak, bukan yang terakhir" supaya dua jam berjumlah sama tidak
        // berganti-ganti tampil — dan aturan itu sudah hidup di daftar per jam.
        var puncak = 0
        var jamTersibuk: Int? = null
        for (jam in 0..23) {
            if (perJam[jam] > puncak) {
                puncak = perJam[jam]
                jamTersibuk = jam
            }
        }

        return AnalitikLaporan(
            omzetKotor = angka("omzet_kotor"),
            potonganMerchant = angka("potongan_merchant"),
            subsidiPlatform = angka("subsidi_platform"),
            omzetBersih = angka("omzet_bersih"),
            pesananSukses = cacah("pesanan_sukses"),
            pesananBatal = cacah("pesanan_batal"),
            itemTerjual = cacah("item_terjual"),
            rataRataPerOrder = angka("rata_rata_per_order"),
            rincianPembayaran = pembayaran,
            perJam = perJam,
            jamTersibuk = jamTersibuk,
            daftarItem = item,
        )
    }

    /**
     * Seluruh pesanan pada [rentang] yang cocok dengan penyaring.
     *
     * Pesanan batal dan tertunda ikut dibaca — kartu "Status Transaksi" perlu
     * menghitungnya, dan menyaring `status=completed` di server akan membuat
     * persentase suksesnya selalu 100%.
     */
    internal suspend fun pesanan(
        rentang: RentangTanggal,
        channel: FilterChannel,
        pembayaran: FilterPembayaran,
        outletId: String?,
    ): List<PesananLaporan> {
        val filter = buildList {
            add("select" to KOLOM)
            add("created_at" to "gte.${rentang.awalIso()}")
            add("created_at" to "lte.${rentang.akhirIso()}")
            if (pembayaran != FilterPembayaran.SEMUA) {
                add("payment_method" to "eq.${pembayaran.kunci}")
            }
            when {
                channel.kolomKosong -> add("channel" to "is.null")
                channel.nilaiDb.isNotEmpty() -> add("channel" to "in.(${channel.nilaiDb.joinToString(",")})")
            }
            if (!outletId.isNullOrBlank()) add("outlet_id" to "eq.$outletId")
        }

        val hasil = mutableListOf<PesananLaporan>()
        var offset = 0
        while (true) {
            val halaman = Postgrest.select(
                "orders",
                filter + listOf(
                    "limit" to UKURAN_HALAMAN.toString(),
                    "offset" to offset.toString(),
                ),
            )
            halaman.forEach { baris -> petakan(baris.asJsonObject)?.let(hasil::add) }
            if (halaman.size() < UKURAN_HALAMAN) break
            offset += UKURAN_HALAMAN
        }
        return hasil
    }

    private fun petakan(baris: com.google.gson.JsonObject): PesananLaporan? {
        val status = baris.optString("status") ?: return null
        val items = baris.optJsonArray("order_items")?.mapNotNull { elemen ->
            val item = elemen.asJsonObject
            ItemPesanan(
                nama = namaMenuPokok(item.optString("menu_item_name")),
                qty = item.optInt("quantity") ?: 0,
                subtotal = item.optDouble("subtotal")?.toLong() ?: 0L,
            )
        }.orEmpty()

        return PesananLaporan(
            status = status,
            metodeBayar = baris.optString("payment_method"),
            totalAmount = baris.optDouble("total_amount")?.toLong() ?: 0L,
            discountAmount = baris.optDouble("discount_amount")?.toLong() ?: 0L,
            promoSubsidy = baris.optDouble("promo_subsidy")?.toLong() ?: 0L,
            jamJakarta = baris.optString("created_at")?.let(::jamJakarta),
            items = items,
        )
    }

    /**
     * Jam Jakarta sebuah cap waktu server.
     *
     * Dihitung di sini, sekali per baris, supaya lapisan domain tidak perlu tahu
     * bentuk string tanggal maupun zona waktu — dan supaya sebaran per jam tidak
     * berubah mengikuti zona waktu perangkat yang sedang dipakai.
     */
    private fun jamJakarta(iso: String): Int? = try {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA).hour
    } catch (e: DateTimeParseException) {
        null
    }
}
