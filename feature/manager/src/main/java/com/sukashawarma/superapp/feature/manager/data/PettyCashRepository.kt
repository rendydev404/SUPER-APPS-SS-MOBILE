package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.manager.domain.StatusTopup
import com.sukashawarma.superapp.feature.manager.domain.TopupPettyCash

/**
 * Pembacaan dan pemrosesan pengajuan dana operasional cabang.
 *
 * Kedua tindakannya dijalankan lewat RPC `SECURITY DEFINER` yang sudah di-`GRANT`
 * ke `authenticated`, bukan UPDATE langsung — persis seperti web, yang juga
 * memanggilnya dari klien browser dengan JWT pengguna. Pemeriksaan status dan role
 * ada di dalam RPC, jadi dua manajer yang menekan tombol bersamaan tetap aman:
 * yang kedua ditolak karena statusnya sudah berpindah.
 */
object PettyCashRepository {

    /** Web membatasi 100 baris terbaru; disamakan supaya isinya sama. */
    private const val BATAS = 100

    private const val KOLOM =
        "id,outlet_id,amount,description,status,created_at,created_by," +
            "bank_name,bank_account_number,bank_account_name,proof_of_transfer_url," +
            "outlets(name,region)"

    suspend fun topups(outletId: String?): List<TopupPettyCash> {
        val params = buildList {
            add("select" to KOLOM)
            add("order" to "created_at.desc")
            add("limit" to BATAS.toString())
            if (!outletId.isNullOrBlank()) add("outlet_id" to "eq.$outletId")
        }
        val baris = Postgrest.select("petty_cash_topups", params).map { it.asJsonObject }
        if (baris.isEmpty()) return emptyList()

        // Nama pengaju diambil terpisah, bukan embedded, karena `created_by` tidak
        // dijamin punya relasi terdeklarasi ke outlet_staff di PostgREST. Web pun
        // melakukannya dalam dua langkah.
        val idPengaju = baris.mapNotNull { it.optString("created_by") }.toSet()
        val nama = namaStaf(idPengaju)

        return baris.mapNotNull { petakan(it, nama) }
    }

    private suspend fun namaStaf(ids: Set<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        return Postgrest.select(
            "outlet_staff",
            listOf("select" to "id,name", "id" to "in.(${ids.joinToString(",")})"),
        ).mapNotNull { elemen ->
            val staf = elemen.asJsonObject
            val id = staf.optString("id") ?: return@mapNotNull null
            val n = staf.optString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            id to n
        }.toMap()
    }

    private fun petakan(baris: JsonObject, nama: Map<String, String>): TopupPettyCash? {
        val id = baris.optString("id") ?: return null
        val status = StatusTopup.dari(baris.optString("status")) ?: return null
        val pengajuId = baris.optString("created_by")
        return TopupPettyCash(
            id = id,
            outletId = baris.optString("outlet_id").orEmpty(),
            outletNama = baris.optJsonObject("outlets")?.optString("name") ?: "Outlet tidak dikenal",
            jumlah = baris.optDouble("amount")?.toLong() ?: 0L,
            deskripsi = baris.optString("description"),
            status = status,
            dibuatPada = baris.optString("created_at").orEmpty(),
            // "Leader" adalah cadangan yang sama dengan web: pengajuan petty cash
            // memang selalu datang dari leader outlet.
            pengajuNama = pengajuId?.let { nama[it] } ?: "Leader",
            namaBank = baris.optString("bank_name"),
            nomorRekening = baris.optString("bank_account_number"),
            atasNama = baris.optString("bank_account_name"),
            buktiTransferUrl = baris.optString("proof_of_transfer_url")?.takeIf { it.isNotBlank() },
        )
    }

    /** Meneruskan pengajuan ke finance, atau menolaknya. */
    suspend fun proses(topupId: String, setujui: Boolean) {
        Postgrest.rpc(
            "area_manager_process_petty_cash",
            JsonObject().apply {
                addProperty("p_topup_id", topupId)
                addProperty("p_action", if (setujui) "approve" else "reject")
            },
        )
    }

    /** Menyerahkan dana yang sudah cair ke leader outlet. */
    suspend fun serahkanKeLeader(topupId: String) {
        Postgrest.rpc(
            "area_manager_forward_funds",
            JsonObject().apply { addProperty("p_topup_id", topupId) },
        )
    }
}
