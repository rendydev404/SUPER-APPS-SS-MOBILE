package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.domain.LaporanWaste
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.StatusWaste
import com.sukashawarma.superapp.feature.manager.domain.akhirIso
import com.sukashawarma.superapp.feature.manager.domain.awalIso
import com.sukashawarma.superapp.feature.manager.domain.nilaiWaste
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Satu outlet pada dropdown penyaring. */
data class OutletPilihan(val id: String, val nama: String)

/** Satu halaman riwayat beserta jumlah seluruh barisnya. */
data class HalamanRiwayat(
    val baris: List<LaporanWaste>,
    val totalBaris: Int,
    val halaman: Int,
    val totalHalaman: Int,
)

/**
 * Pembacaan dan pemrosesan laporan waste.
 *
 * Seperti [ManagerRepository], tidak ada penyaringan cakupan outlet di sini:
 * `waste_reports_read` dan `waste_reports_update` sama-sama memakai
 * `accessible_outlet_ids()`, jadi RLS sudah membatasi baris yang terlihat maupun
 * yang boleh diubah. Penyaring outlet di layar adalah pilihan pengguna, bukan
 * kendali akses.
 */
object WasteRepository {

    const val BARIS_PER_HALAMAN = 20

    /** Kolom yang sama dipakai antrean dan riwayat, jadi pemetaannya satu jalur. */
    private const val KOLOM =
        "id,outlet_id,bahan_baku_id,qty,reason,photo_url,status,rejection_reason," +
            "reported_by,created_at,outlets(name),bahan_baku(nama,satuan)," +
            "reporter:outlet_staff!reported_by(id,name),approver:outlet_staff!approved_by(name)"

    private fun filterOutlet(outletId: String?): List<Pair<String, String>> =
        if (outletId.isNullOrBlank()) emptyList() else listOf("outlet_id" to "eq.$outletId")

    suspend fun outletTerakses(): List<OutletPilihan> =
        Postgrest.select(
            "outlets",
            listOf("select" to "id,name", "is_active" to "eq.true", "order" to "name"),
        ).mapNotNull { baris ->
            val obj = baris.asJsonObject
            val id = obj.optString("id") ?: return@mapNotNull null
            OutletPilihan(id, obj.optString("name").orEmpty())
        }

    /** Antrean laporan yang masih menunggu keputusan, terbaru di atas. */
    suspend fun menunggu(outletId: String?): List<LaporanWaste> {
        val baris = Postgrest.select(
            "stok_waste_reports",
            listOf(
                "select" to KOLOM,
                "status" to "eq.${StatusWaste.MENUNGGU.nilai}",
                "order" to "created_at.desc",
            ) + filterOutlet(outletId),
        ).map { it.asJsonObject }
        return lengkapiHarga(baris)
    }

    /**
     * Satu halaman riwayat. `totalBaris` dihitung lewat query id-saja atas filter
     * yang sama — PostgREST menaruh hitungan persisnya di header Content-Range,
     * dan [Postgrest] hanya mengembalikan body, jadi ini jalan yang tersedia tanpa
     * menambah API baru. Riwayat waste satu periode berukuran puluhan sampai
     * ratusan baris, jadi biayanya kecil.
     */
    suspend fun riwayat(
        rentang: RentangTanggal,
        outletId: String?,
        status: StatusWaste?,
        halaman: Int,
    ): HalamanRiwayat = coroutineScope {
        val filterStatus = if (status != null) {
            listOf("status" to "eq.${status.nilai}")
        } else {
            // Tanpa pilihan status, riwayat hanya berisi yang sudah diputuskan —
            // yang masih menunggu adalah isi tab sebelah, bukan riwayat.
            listOf("status" to "in.(${StatusWaste.DISETUJUI.nilai},${StatusWaste.DITOLAK.nilai})")
        }
        val dasar = filterStatus +
            ("created_at" to "gte.${rentang.awalIso()}") +
            ("created_at" to "lte.${rentang.akhirIso()}") +
            filterOutlet(outletId)

        val nomor = halaman.coerceAtLeast(1)
        val isi = async {
            Postgrest.select(
                "stok_waste_reports",
                dasar + listOf(
                    "select" to KOLOM,
                    "order" to "created_at.desc",
                    "limit" to BARIS_PER_HALAMAN.toString(),
                    "offset" to ((nomor - 1) * BARIS_PER_HALAMAN).toString(),
                ),
            ).map { it.asJsonObject }
        }
        val total = async { hitungBaris(dasar) }

        val baris = lengkapiHarga(isi.await())
        val totalBaris = total.await()
        HalamanRiwayat(
            baris = baris,
            totalBaris = totalBaris,
            halaman = nomor,
            totalHalaman = maxOf(1, (totalBaris + BARIS_PER_HALAMAN - 1) / BARIS_PER_HALAMAN),
        )
    }

    /** Seluruh laporan disetujui pada periode — bahan mentah kartu KPI dan daftar bahan teratas. */
    suspend fun disetujuiPada(rentang: RentangTanggal, outletId: String?): List<LaporanWaste> {
        val baris = Postgrest.select(
            "stok_waste_reports",
            listOf(
                "select" to KOLOM,
                "status" to "eq.${StatusWaste.DISETUJUI.nilai}",
                "created_at" to "gte.${rentang.awalIso()}",
                "created_at" to "lte.${rentang.akhirIso()}",
            ) + filterOutlet(outletId),
        ).map { it.asJsonObject }
        return lengkapiHarga(baris)
    }

    suspend fun jumlahMenunggu(outletId: String?): Int =
        hitungBaris(listOf("status" to "eq.${StatusWaste.MENUNGGU.nilai}") + filterOutlet(outletId))

    /**
     * Menghitung baris yang cocok dengan [filter].
     *
     * Membaca kolom id saja, dan tetap menelusuri halaman: PostgREST memotong
     * balasan di 1000 baris, jadi satu permintaan polos akan melaporkan "1000"
     * untuk periode panjang mana pun dan penomoran halamannya diam-diam salah.
     */
    private suspend fun hitungBaris(filter: List<Pair<String, String>>): Int {
        val ukuran = 1000
        var offset = 0
        var jumlah = 0
        while (true) {
            val halaman = Postgrest.select(
                "stok_waste_reports",
                filter + listOf(
                    "select" to "id",
                    "limit" to ukuran.toString(),
                    "offset" to offset.toString(),
                ),
            )
            jumlah += halaman.size()
            if (halaman.size() < ukuran) return jumlah
            offset += ukuran
        }
    }

    /**
     * Menyetujui atau menolak satu laporan.
     *
     * Filter `status=eq.PENDING` ikut dikirim, jadi dua manajer yang menekan tombol
     * pada detik yang sama tidak sama-sama berhasil: yang kedua mendapat balasan
     * kosong dan diberi tahu laporannya sudah diproses. Ini penjaga yang sama
     * dengan `.eq('status','PENDING')` di web, dan ia ada di server, bukan di layar.
     *
     * @return null kalau berhasil; pesan siap-tampil kalau kalah balapan.
     */
    suspend fun proses(
        id: String,
        setujui: Boolean,
        idPenyetuju: String,
        alasanPenolakan: String? = null,
    ): String? {
        val patch = JsonObject().apply {
            addProperty("status", if (setujui) StatusWaste.DISETUJUI.nilai else StatusWaste.DITOLAK.nilai)
            addProperty("approved_by", idPenyetuju)
            addProperty("updated_at", java.time.Instant.now().toString())
            if (!setujui) addProperty("rejection_reason", alasanPenolakan?.trim())
        }
        val hasil = Postgrest.update(
            "stok_waste_reports",
            listOf("id" to "eq.$id", "status" to "eq.${StatusWaste.MENUNGGU.nilai}"),
            patch,
        )
        return if (hasil.size() == 0) {
            "Gagal memproses: laporan mungkin sudah diproses manajer lain."
        } else {
            null
        }
    }

    /**
     * Melengkapi baris dengan harga beli bahannya.
     *
     * Harga tidak ikut di-embed karena `bahan_baku_harga` bisa punya lebih dari satu
     * baris per bahan; menariknya terpisah lalu memetakan per id membuat aturan
     * "harga mana yang dipakai" berada di satu tempat, sama seperti web.
     */
    private suspend fun lengkapiHarga(baris: List<JsonObject>): List<LaporanWaste> {
        if (baris.isEmpty()) return emptyList()
        val bahanIds = baris.mapNotNull { it.optString("bahan_baku_id") }.toSet()
        val harga = ManagerRepository.hargaBahan(bahanIds)
        return baris.mapNotNull { petakan(it, harga) }
    }

    private fun petakan(baris: JsonObject, harga: Map<String, Double>): LaporanWaste? {
        val id = baris.optString("id") ?: return null
        val bahanId = baris.optString("bahan_baku_id") ?: return null
        val status = StatusWaste.dari(baris.optString("status")) ?: return null
        val qty = baris.optDouble("qty") ?: 0.0
        val hargaBeli = harga[bahanId] ?: 0.0
        val bahan = baris.optJsonObject("bahan_baku")
        return LaporanWaste(
            id = id,
            outletId = baris.optString("outlet_id").orEmpty(),
            outletNama = baris.optJsonObject("outlets")?.optString("name") ?: "Outlet tidak dikenal",
            bahanId = bahanId,
            bahanNama = bahan?.optString("nama") ?: "Bahan tidak dikenal",
            satuan = bahan?.optString("satuan") ?: "Pcs",
            qty = qty,
            hargaBeli = hargaBeli,
            nilai = nilaiWaste(qty, hargaBeli),
            alasan = baris.optString("reason").orEmpty(),
            fotoUrl = baris.optString("photo_url")?.takeIf { it.isNotBlank() },
            status = status,
            alasanPenolakan = baris.optString("rejection_reason")?.takeIf { it.isNotBlank() },
            pelaporId = baris.optString("reported_by"),
            pelaporNama = baris.optJsonObject("reporter")?.optString("name") ?: "Kru Outlet",
            penyetujuNama = baris.optJsonObject("approver")?.optString("name"),
            dibuatPada = baris.optString("created_at").orEmpty(),
        )
    }
}
