package com.sukashawarma.superapp.feature.leader.data

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.leader.domain.PesananSelesai
import com.sukashawarma.superapp.feature.leader.domain.RingkasanLeader
import com.sukashawarma.superapp.feature.leader.domain.ShiftAktif
import com.sukashawarma.superapp.feature.leader.domain.akhirHariIso
import com.sukashawarma.superapp.feature.leader.domain.awalHariIso
import com.sukashawarma.superapp.feature.leader.domain.hariIniJakarta
import com.sukashawarma.superapp.feature.leader.domain.jamTransaksiTerakhir
import com.sukashawarma.superapp.feature.leader.domain.susunPerOutlet
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Pembacaan layar Ringkasan Leader.
 *
 * Tabel transaksi (`orders`, `attendance`, `shifts`, `petty_cash_*`) tidak disaring
 * ulang di luar apa yang memang mempersempit pertanyaan: semuanya sudah discope
 * `accessible_outlet_ids()` di RLS, dan native memakai JWT pengguna. Web wajib
 * menyaring manual hanya karena render server-nya memakai kunci yang menembus RLS.
 */
object RingkasanRepository {

    suspend fun muat(): RingkasanLeader = coroutineScope {
        val hariIni = hariIniJakarta()
        val dari = awalHariIso(hariIni)
        val sampai = akhirHariIso(hariIni)

        val outletStaf = OutletLeaderRepository.outletUtamaTerdaftar()
        val ids = OutletLeaderRepository.idTerakses()
        val utama = OutletLeaderRepository.outletUtama(ids, outletStaf)

        val cabang = async { OutletLeaderRepository.cabang(ids) }
        val pesanan = async { pesananSelesai(ids, dari, sampai) }
        val saldo = async { utama?.let { saldoPettyCash(it) } ?: 0L }
        val shift = async { utama?.let { shiftAktif(it) } }
        val stok = async { utama?.let { jumlahItemStok(it) } ?: 0 }
        val kru = async { utama?.let { jumlahKruAktif(it) } ?: 0 }
        val hadir = async { utama?.let { jumlahHadir(it, dari, sampai) } ?: 0 }

        val daftarCabang = cabang.await()
        val nama = daftarCabang.associate { it.id to it.nama }
        val barisPesanan = pesanan.await()
        val shiftBaris = shift.await()

        RingkasanLeader(
            namaOutletUtama = utama?.let { nama[it] } ?: "Cabang",
            jumlahCabang = ids.size,
            omzetHariIni = barisPesanan.sumOf { it.total },
            jumlahTransaksi = barisPesanan.size,
            jamTransaksiTerakhir = jamTransaksiTerakhir(barisPesanan),
            sisaPettyCash = saldo.await(),
            adaShiftAktif = shiftBaris != null,
            shift = shiftBaris,
            jumlahItemStok = stok.await(),
            hadir = hadir.await(),
            totalKru = kru.await(),
            perOutlet = susunPerOutlet(ids, nama, barisPesanan),
            punyaOutlet = utama != null,
        )
    }

    /** Omzet POS hari ini: hanya pesanan `completed`, sama seperti kartu omzet web. */
    private suspend fun pesananSelesai(
        ids: List<String>,
        dari: String,
        sampai: String,
    ): List<PesananSelesai> {
        val filter = if (ids.isEmpty()) {
            emptyList()
        } else {
            listOf("outlet_id" to "in.(${ids.joinToString(",")})")
        }
        return Postgrest.select(
            "orders",
            listOf(
                "select" to "outlet_id,total_amount,created_at",
                "status" to "eq.completed",
                "created_at" to "gte.$dari",
                "created_at" to "lte.$sampai",
            ) + filter,
        ).mapNotNull { elemen ->
            val baris = elemen.asJsonObject
            val outletId = baris.optString("outlet_id") ?: return@mapNotNull null
            PesananSelesai(
                outletId = outletId,
                total = baris.optDouble("total_amount")?.toLong() ?: 0L,
                dibuatPada = baris.optString("created_at").orEmpty(),
            )
        }
    }

    /**
     * Saldo petty cash cabang.
     *
     * Hanya `get_petty_cash_balance()` yang ditanya. Web menghitung sendiri lebih
     * dulu (modal awal + topup - pengeluaran) lalu menimpanya dengan hasil RPC yang
     * sama — perhitungan tangannya tidak pernah terpakai kecuali RPC gagal, dan saat
     * itu terjadi angkanya justru salah: ia tidak tahu soal pengeluaran yang di-void
     * maupun penyesuaian admin. Jadi di sini tidak ada versi tangan sama sekali.
     *
     * RPC menolak outlet di luar cakupan dengan galat, bukan dengan nol; itu ditangkap
     * pemanggil dan tampil sebagai pesan, bukan sebagai saldo kosong yang meyakinkan.
     */
    private suspend fun saldoPettyCash(outletId: String): Long {
        val hasil = Postgrest.rpc(
            "get_petty_cash_balance",
            com.google.gson.JsonObject().apply { addProperty("p_outlet_id", outletId) },
        )
        return if (hasil.isJsonPrimitive) hasil.asDouble.toLong() else 0L
    }

    /**
     * Shift yang belum ditutup, terbaru lebih dulu.
     *
     * Penandanya `end_time IS NULL`, sama seperti web. Perhatikan bahwa
     * `get_petty_cash_balance` memakai penanda LAIN (`status = 'open'`), jadi outlet
     * dengan shift yang statusnya sudah bukan `open` tapi `end_time`-nya masih kosong
     * akan tampil "Shift Aktif" dengan saldo nol. Itu keadaan data yang memang ada di
     * sana; layar ini tidak menyembunyikannya dengan menebak salah satu penanda.
     */
    private suspend fun shiftAktif(outletId: String): ShiftAktif? {
        val baris = Postgrest.selectOne(
            "shifts",
            listOf(
                "select" to "id,start_time,admin_petty_cash_note,admin_petty_cash_updated_at",
                "outlet_id" to "eq.$outletId",
                "end_time" to "is.null",
                "order" to "start_time.desc",
            ),
        ) ?: return null
        return ShiftAktif(
            catatanAdmin = baris.optString("admin_petty_cash_note"),
            disesuaikanPada = baris.optString("admin_petty_cash_updated_at"),
        )
    }

    /**
     * Cacah bahan yang saldonya masih ada di cabang.
     *
     * Web menghitungnya dari `inventory_batches` — tabel yang TIDAK ADA di database
     * ini (migrasi `20260709000001_merge_fifo_po` mencatatnya sebagai dibatalkan dan
     * menghapus `inventory_items`), sehingga angka "Stok Cabang" di web selalu nol.
     * Native membacanya dari `monitoring_view_scoped`, view yang sama yang dipakai
     * modul Stok dan berisi saldo sungguhan.
     */
    private suspend fun jumlahItemStok(outletId: String): Int =
        Postgrest.select(
            "monitoring_view_scoped",
            listOf(
                "select" to "bahan_baku_id",
                "outlet_id" to "eq.$outletId",
                "current_qty" to "gt.0",
            ),
        ).size()

    private suspend fun jumlahKruAktif(outletId: String): Int =
        Postgrest.select(
            "outlet_staff",
            listOf(
                "select" to "id",
                "outlet_id" to "eq.$outletId",
                "status" to "eq.active",
            ),
        ).size()

    /**
     * Kru yang sudah absen masuk hari ini, dihitung per orang.
     *
     * Satu kru bisa punya beberapa baris `type = 'in'` dalam sehari (istirahat,
     * pergantian shift), jadi barisnya di-`distinct` dulu — kalau tidak, "8/5 hadir"
     * bisa muncul dan angka itu tidak berarti apa-apa.
     */
    private suspend fun jumlahHadir(outletId: String, dari: String, sampai: String): Int =
        Postgrest.select(
            "attendance",
            listOf(
                "select" to "outlet_staff_id",
                "outlet_id" to "eq.$outletId",
                "type" to "eq.in",
                "ts_server" to "gte.$dari",
                "ts_server" to "lte.$sampai",
            ),
        ).mapNotNull { it.asJsonObject.optString("outlet_staff_id") }.toSet().size
}
