package com.sukashawarma.superapp.feature.leader.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession

/** Satu cabang binaan beserta rekening yang dipakai form top up. */
data class OutletLeader(
    val id: String,
    val nama: String,
    val slug: String?,
    val namaBank: String?,
    val nomorRekening: String?,
    val atasNama: String?,
) {
    val punyaRekening: Boolean
        get() = !namaBank.isNullOrBlank() && !nomorRekening.isNullOrBlank()
}

/**
 * Cakupan cabang seorang leader.
 *
 * Sumber kebenarannya `accessible_outlet_ids()` — fungsi yang SAMA dipakai RLS
 * pada `orders`, `petty_cash_topups`, dan `get_petty_cash_balance`. Halaman web
 * juga memanggilnya lebih dulu dan baru jatuh ke `staff_outlets` bila hasilnya
 * kosong; urutan itu ditiru di sini.
 *
 * Menyaring `outlets` di klien BUKAN pilihan gaya. Policy bacanya
 * `outlets_select_authenticated` berisi `USING (true)`, jadi tanpa penyaring ini
 * leader melihat seluruh cabang perusahaan pada pemilih outlet.
 */
object OutletLeaderRepository {

    private const val KOLOM =
        "id,name,slug,bank_name,bank_account_number,bank_account_name"

    /** Baris `outlet_staff` milik pengguna — dipakai menentukan outlet utama. */
    suspend fun outletUtamaTerdaftar(): String? {
        val id = AppSession.staff.value?.id ?: return null
        return Postgrest.selectOne(
            "outlet_staff",
            listOf("select" to "id,outlet_id", "id" to "eq.$id"),
        )?.optString("outlet_id")
    }

    /**
     * Id cabang yang boleh dilihat. Daftar kosong berarti benar-benar kosong —
     * jangan pernah ditafsirkan sebagai "berarti semua cabang".
     */
    suspend fun idTerakses(): List<String> {
        val dariRpc = Postgrest.rpc("accessible_outlet_ids").let { el ->
            when {
                el.isJsonArray -> el.asJsonArray.mapNotNull { item ->
                    when {
                        item.isJsonPrimitive -> item.asString
                        item.isJsonObject -> item.asJsonObject.optString("accessible_outlet_ids")
                        else -> null
                    }
                }
                else -> emptyList()
            }
        }.filter { it.isNotBlank() }.distinct()
        if (dariRpc.isNotEmpty()) return dariRpc

        // Cadangan yang sama dengan web: outlet langsung pada baris staf, ditambah
        // pemetaan `staff_outlets` miliknya sendiri (satu-satunya yang boleh dibaca
        // policy `staff_outlets_select_self`).
        val stafId = AppSession.staff.value?.id ?: return emptyList()
        val ids = LinkedHashSet<String>()
        outletUtamaTerdaftar()?.let { ids.add(it) }
        Postgrest.select(
            "staff_outlets",
            listOf("select" to "outlet_id", "staff_id" to "eq.$stafId"),
        ).mapNotNullTo(ids) { it.asJsonObject.optString("outlet_id") }
        return ids.toList()
    }

    /** Cabang terakses yang masih aktif, urut nama — isi pemilih outlet di tiap layar. */
    suspend fun cabang(ids: List<String>): List<OutletLeader> {
        if (ids.isEmpty()) return emptyList()
        return Postgrest.select(
            "outlets",
            listOf(
                "select" to KOLOM,
                "id" to "in.(${ids.joinToString(",")})",
                "is_active" to "eq.true",
                "order" to "name.asc",
            ),
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val id = baris.optString("id") ?: return@mapNotNull null
            OutletLeader(
                id = id,
                nama = baris.optString("name").orEmpty().ifBlank { "Cabang" },
                slug = baris.optString("slug"),
                namaBank = baris.optString("bank_name"),
                nomorRekening = baris.optString("bank_account_number"),
                atasNama = baris.optString("bank_account_name"),
            )
        }
    }

    /**
     * Cabang mana yang jadi acuan kartu petty cash, stok, dan kehadiran.
     *
     * Outlet pada baris `outlet_staff` menang bila ia memang terakses; kalau tidak,
     * cabang pertama menurut abjad. Aturan yang sama dipakai `primaryOutletId` web,
     * dan menyamakannya penting: kedua layar menampilkan saldo petty cash SATU
     * cabang, jadi mereka harus memilih cabang yang sama.
     */
    fun outletUtama(idTerakses: List<String>, outletStaf: String?): String? = when {
        outletStaf != null && outletStaf in idTerakses -> outletStaf
        idTerakses.isNotEmpty() -> idTerakses.first()
        else -> outletStaf
    }
}
