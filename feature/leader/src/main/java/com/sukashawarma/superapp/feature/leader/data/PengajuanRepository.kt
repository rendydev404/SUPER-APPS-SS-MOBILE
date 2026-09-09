package com.sukashawarma.superapp.feature.leader.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.leader.domain.FormTopup
import com.sukashawarma.superapp.feature.leader.domain.Pengajuan
import com.sukashawarma.superapp.feature.leader.domain.StatusPengajuan

/**
 * Pengajuan dana operasional dari kursi leader: membuat, memantau, menyerahkan.
 *
 * Dua tindakannya dijalankan lewat RPC `SECURITY DEFINER` yang sudah di-`GRANT` ke
 * `authenticated`, bukan INSERT/UPDATE langsung — sama seperti web, yang juga
 * memanggilnya dari browser dengan JWT pengguna. Pemeriksaan role dan urutan status
 * ada di dalam database (`create_petty_cash_topup` menolak role di bawah leader;
 * trigger `petty_cash_enforce_flow` menolak lompatan tahap), jadi layar ini tidak
 * memutuskan apa pun soal keabsahan — ia hanya menampilkan hasilnya.
 */
object PengajuanRepository {

    /** Sejajar dengan modul Manager: seratus baris terbaru sudah lebih dari cukup. */
    private const val BATAS = 100

    private const val KOLOM =
        "id,outlet_id,amount,description,status,created_at," +
            "bank_name,bank_account_number,bank_account_name,proof_of_transfer_url," +
            "outlets!petty_cash_topups_outlet_id_fkey(name)"

    /**
     * Riwayat pengajuan, terbaru di atas.
     *
     * Embed `outlets` HARUS disebutkan lewat nama constraint-nya. `staff_outlets`
     * membentuk relasi kedua antara `petty_cash_topups` dan `outlets`, dan tanpa
     * disambiguasi PostgREST membalas PGRST201 alih-alih data — kegagalan yang sama
     * pernah membuat halaman leader web selalu jatuh ke layar "belum ditugaskan".
     */
    suspend fun daftar(outletId: String?): List<Pengajuan> {
        val filter = if (outletId.isNullOrBlank()) {
            emptyList()
        } else {
            listOf("outlet_id" to "eq.$outletId")
        }
        return Postgrest.select(
            "petty_cash_topups",
            listOf(
                "select" to KOLOM,
                "order" to "created_at.desc",
                "limit" to BATAS.toString(),
            ) + filter,
        ).mapNotNull { petakan(it.asJsonObject) }
    }

    private fun petakan(baris: JsonObject): Pengajuan? {
        val id = baris.optString("id") ?: return null
        // Status tak dikenal dibuang, bukan dipaksa masuk salah satu tahap: kartu
        // dengan label karangan lebih berbahaya daripada kartu yang tidak muncul.
        val status = StatusPengajuan.dari(baris.optString("status")) ?: return null
        return Pengajuan(
            id = id,
            outletId = baris.optString("outlet_id").orEmpty(),
            outletNama = baris.optJsonObject("outlets")?.optString("name") ?: "Outlet tidak dikenal",
            jumlah = baris.optDouble("amount")?.toLong() ?: 0L,
            deskripsi = baris.optString("description"),
            status = status,
            dibuatPada = baris.optString("created_at").orEmpty(),
            namaBank = baris.optString("bank_name"),
            nomorRekening = baris.optString("bank_account_number"),
            atasNama = baris.optString("bank_account_name"),
            buktiTransferUrl = baris.optString("proof_of_transfer_url")?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Mengajukan top up baru.
     *
     * RPC ikut menuliskan rekening ke baris `outlets`, jadi web memanggil UPDATE
     * `outlets` terlebih dahulu tanpa perlu — pekerjaan yang sama dilakukan dua kali,
     * dan yang pertama lewat jalur yang RLS-nya bisa saja menolaknya. Di sini hanya
     * RPC-nya yang dipanggil.
     *
     * Status awal ditentukan database (`forwarded_to_area_manager`), bukan dikirim
     * dari sini; trigger alurnya memang menolak nilai lain.
     */
    suspend fun ajukan(form: FormTopup) {
        Postgrest.rpc(
            "create_petty_cash_topup",
            JsonObject().apply {
                addProperty("p_outlet_id", form.outletId)
                addProperty("p_amount", form.nominalAngka ?: 0L)
                addProperty("p_description", form.keperluan.trim())
                addProperty("p_bank_name", form.namaBank.trim())
                addProperty("p_bank_account_number", form.nomorRekening.trim())
                addProperty("p_bank_account_name", form.atasNama.trim())
            },
        )
    }

    /**
     * Menyerahkan dana yang sudah cair kepada crew; saldo petty cash outlet naik
     * karena `get_petty_cash_balance` ikut menghitung status `forwarded_by_leader`.
     */
    suspend fun serahkanKeCrew(pengajuanId: String) {
        Postgrest.rpc(
            "leader_forward_funds",
            JsonObject().apply { addProperty("p_topup_id", pengajuanId) },
        )
    }
}
