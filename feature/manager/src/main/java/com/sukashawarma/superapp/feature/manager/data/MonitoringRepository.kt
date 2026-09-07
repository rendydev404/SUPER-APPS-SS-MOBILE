package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.domain.AbsenBaris
import com.sukashawarma.superapp.feature.manager.domain.OpnameBaris
import com.sukashawarma.superapp.feature.manager.domain.OutletMonitoring
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.StafMonitoring
import com.sukashawarma.superapp.feature.manager.domain.ZONA_JAKARTA
import com.sukashawarma.superapp.feature.manager.domain.akhirIso
import com.sukashawarma.superapp.feature.manager.domain.awalIso
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Baris mentah satu layar monitoring. */
data class DataMonitoring(
    val outlets: List<OutletMonitoring>,
    val staf: List<StafMonitoring>,
    val absen: List<AbsenBaris>,
    val opname: List<OpnameBaris>,
    /** outlet_id -> id item checklist buka yang wajib. */
    val wajibChecklist: Map<String, List<String>>,
    /** outlet_id -> id item yang sudah dicentang pada rentang terpilih. */
    val sudahDicentang: Map<String, Set<String>>,
)

/**
 * Pembacaan papan monitoring POS & Kru — padanan `app/api/monitoring/route.ts` web.
 *
 * Web memakai route API dengan service-role lalu menyaring outlet sendiri. Native
 * membaca langsung dengan JWT pengguna: `attendance`, `opname`, dan `outlet_staff`
 * semuanya sudah discope `accessible_outlet_ids()`, sedangkan tabel checklist
 * terbuka untuk role manajemen lewat `auth_is_supervisor()`.
 */
object MonitoringRepository {

    private const val UKURAN_HALAMAN = 1000
    private val JAM_JAKARTA = DateTimeFormatter.ofPattern("HH.mm")

    /** Outlet global tempat checklist yang berlaku untuk semua cabang disimpan. */
    private const val OUTLET_GLOBAL = "00000000-0000-0000-0000-000000000000"

    private suspend fun selectSemua(
        tabel: String,
        params: List<Pair<String, String>>,
    ): List<JsonObject> {
        val hasil = mutableListOf<JsonObject>()
        var offset = 0
        while (true) {
            val halaman = Postgrest.select(
                tabel,
                params + listOf(
                    "limit" to UKURAN_HALAMAN.toString(),
                    "offset" to offset.toString(),
                ),
            )
            halaman.forEach { hasil += it.asJsonObject }
            if (halaman.size() < UKURAN_HALAMAN) break
            offset += UKURAN_HALAMAN
        }
        return hasil
    }

    suspend fun muat(rentang: RentangTanggal): DataMonitoring = coroutineScope {
        val outlets = async { outlets() }
        val staf = async { staf() }
        val absen = async { absen(rentang) }
        val opname = async { opname(rentang) }
        val kategori = async { kategoriChecklist() }
        val catatan = async { catatanChecklist(rentang) }

        val daftarOutlet = outlets.await()
        DataMonitoring(
            outlets = daftarOutlet,
            staf = staf.await(),
            absen = absen.await(),
            opname = opname.await(),
            wajibChecklist = wajibChecklist(kategori.await(), daftarOutlet.map { it.id }),
            sudahDicentang = centangUntukCatatan(catatan.await()),
        )
    }

    /**
     * Outlet dibatasi cakupan pengguna, BUKAN diserahkan ke RLS.
     * `outlets_select_authenticated` berisi `USING (true)` — tanpa penyaring ini
     * area manager melihat seluruh cabang, termasuk yang bukan binaannya.
     */
    private suspend fun outlets(): List<OutletMonitoring> {
        val filter = CakupanOutletRepository.filterOutlet(CakupanOutletRepository.cakupan())
            ?: return emptyList()
        return selectSemua(
            "outlets",
            listOf("select" to "id,name,region", "is_active" to "eq.true", "order" to "name") + filter,
        ).mapNotNull { baris ->
            val id = baris.optString("id") ?: return@mapNotNull null
            OutletMonitoring(id, baris.optString("name").orEmpty(), baris.optString("region"))
        }
    }

    /** Hanya role lapangan yang muncul di papan; manajemen tidak dipantau di sini. */
    private suspend fun staf(): List<StafMonitoring> =
        selectSemua(
            "outlet_staff",
            listOf(
                "select" to "id,name,outlet_id,role,is_active",
                "role" to "in.(crew,leader,spv)",
                "is_active" to "eq.true",
                "order" to "name",
            ),
        ).mapNotNull { baris ->
            val id = baris.optString("id") ?: return@mapNotNull null
            StafMonitoring(
                id = id,
                nama = baris.optString("name").orEmpty(),
                role = baris.optString("role").orEmpty(),
                outletId = baris.optString("outlet_id"),
            )
        }

    private suspend fun absen(rentang: RentangTanggal): List<AbsenBaris> =
        selectSemua(
            "attendance",
            listOf(
                "select" to "outlet_id,outlet_staff_id,type,ts_server",
                "ts_server" to "gte.${rentang.awalIso()}",
                "ts_server" to "lte.${rentang.akhirIso()}",
                "order" to "ts_server.asc",
            ),
        ).mapNotNull { baris ->
            val ts = baris.optString("ts_server") ?: return@mapNotNull null
            val outletId = baris.optString("outlet_id") ?: return@mapNotNull null
            val stafId = baris.optString("outlet_staff_id") ?: return@mapNotNull null
            AbsenBaris(
                outletId = outletId,
                stafId = stafId,
                tipe = baris.optString("type").orEmpty(),
                tanggal = tanggalDari(ts),
                jam = jamDari(ts),
                tsServer = ts,
            )
        }

    private suspend fun opname(rentang: RentangTanggal): List<OpnameBaris> =
        selectSemua(
            "opname",
            listOf(
                "select" to "id,outlet_id,created_at",
                "created_at" to "gte.${rentang.awalIso()}",
                "created_at" to "lte.${rentang.akhirIso()}",
                "order" to "created_at.asc",
            ),
        ).mapNotNull { baris ->
            val ts = baris.optString("created_at") ?: return@mapNotNull null
            val outletId = baris.optString("outlet_id") ?: return@mapNotNull null
            OpnameBaris(outletId, tanggalDari(ts), jamDari(ts))
        }

    /**
     * Item checklist buka yang wajib, per outlet.
     *
     * Kategori milik outlet global berlaku untuk semua cabang, jadi itemnya ikut
     * ditempelkan ke setiap outlet — kalau tidak, outlet yang hanya memakai
     * checklist global akan terlihat tidak punya syarat buka sama sekali.
     */
    private suspend fun kategoriChecklist(): List<JsonObject> = selectSemua(
        "checklist_categories",
        listOf(
            "select" to "id,outlet_id,checklist_items(id,is_required)",
            "phase" to "eq.buka",
        ),
    )

    private fun wajibChecklist(
        kategori: List<JsonObject>,
        outletIds: List<String>,
    ): Map<String, List<String>> {
        val perOutlet = mutableMapOf<String, MutableList<String>>()
        val global = mutableListOf<String>()
        kategori.forEach { baris ->
            val outletId = baris.optString("outlet_id") ?: return@forEach
            val wajib = baris.optJsonArray("checklist_items")
                ?.map { it.asJsonObject }
                ?.filter { it.optBoolean("is_required") }
                ?.mapNotNull { it.optString("id") }
                .orEmpty()
            if (outletId == OUTLET_GLOBAL) {
                global += wajib
            } else {
                perOutlet.getOrPut(outletId) { mutableListOf() } += wajib
            }
        }
        if (global.isEmpty()) return perOutlet
        return (perOutlet.keys + outletIds).associateWith { id -> perOutlet[id].orEmpty() + global }
    }

    private suspend fun catatanChecklist(rentang: RentangTanggal): List<Pair<String, String>> =
        selectSemua(
            "daily_checklist_records",
            listOf(
                "select" to "id,outlet_id,date",
                "date" to "gte.${rentang.dari}",
                "date" to "lte.${rentang.sampai}",
            ),
        ).mapNotNull { baris ->
            val id = baris.optString("id") ?: return@mapNotNull null
            val outletId = baris.optString("outlet_id") ?: return@mapNotNull null
            id to outletId
        }

    /**
     * Centang per outlet. Dibaca dalam potongan 200 id agar URL `in.(...)` tidak
     * melewati batas panjang server saat rentang 30 hari menghasilkan ratusan catatan.
     */
    private suspend fun centangUntukCatatan(catatan: List<Pair<String, String>>): Map<String, Set<String>> {
        if (catatan.isEmpty()) return emptyMap()
        val outletPerCatatan = catatan.toMap()
        val hasil = mutableMapOf<String, MutableSet<String>>()
        catatan.map { it.first }.chunked(200).forEach { potongan ->
            selectSemua(
                "daily_checklist_ticks",
                listOf(
                    "select" to "item_id,record_id",
                    "record_id" to "in.(${potongan.joinToString(",")})",
                ),
            ).forEach { tick ->
                val recordId = tick.optString("record_id") ?: return@forEach
                val itemId = tick.optString("item_id") ?: return@forEach
                val outletId = outletPerCatatan[recordId] ?: return@forEach
                hasil.getOrPut(outletId) { mutableSetOf() } += itemId
            }
        }
        return hasil
    }

    private fun tanggalDari(iso: String): LocalDate? = try {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA).toLocalDate()
    } catch (e: DateTimeParseException) {
        null
    }

    private fun jamDari(iso: String): String = try {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA).format(JAM_JAKARTA)
    } catch (e: DateTimeParseException) {
        ""
    }
}
