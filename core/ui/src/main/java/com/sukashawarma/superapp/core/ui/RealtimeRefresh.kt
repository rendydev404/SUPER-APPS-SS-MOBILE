package com.sukashawarma.superapp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.sukashawarma.superapp.data.remote.Realtime

/**
 * Nama tabel yang benar-benar mengirim event.
 *
 * Hanya tabel di publication `supabase_realtime` yang menghasilkan perubahan;
 * berlangganan nama lain gagal diam-diam — tidak error, sekadar tidak pernah ada
 * kabar. Daftar ini disalin dari migrasi web `20300103000009` dan
 * `20260713100001` supaya salah ketik tertangkap kompilator, bukan di lapangan.
 *
 * Yang TIDAK ada di publication dan karena itu tidak bisa realtime tanpa
 * migration: `mutasi_antar_outlet`, `surat_jalan_item`, `staff_outlets`,
 * `permintaan_bahan_item`.
 */
object RealtimeTables {
    const val ATTENDANCE = "attendance"
    const val ATTENDANCE_CONFIG = "outlet_attendance_config"
    const val LEAVE_REQUESTS = "leave_requests"
    const val CASH_ADVANCES = "cash_advances"
    const val OUTLET_STAFF = "outlet_staff"
    const val CHECKLIST_RECORDS = "daily_checklist_records"
    const val CHECKLIST_TICKS = "daily_checklist_ticks"
    const val CHECKLIST_ITEMS = "checklist_items"
    const val STOK_BALANCE = "stok_balance"
    const val LEDGER = "ledger_stok"
    const val WASTE_REPORTS = "stok_waste_reports"
    const val PERMINTAAN = "permintaan_bahan"
    const val OPNAME = "opname"
    const val OPNAME_ITEM = "opname_item"
    const val SURAT_JALAN = "surat_jalan"
    const val BAHAN_BAKU = "bahan_baku"
    const val BAHAN_BAKU_HARGA = "bahan_baku_harga"
    const val PURCHASE_ORDER = "purchase_order"
    const val OUTLETS = "outlets"

    // Ditambahkan ke publication lewat migrasi web `20260623130000_orders_realtime_publication`
    // dan `20260707153000_order_items_realtime` — dipakai dashboard manajer agar omzet,
    // jumlah transaksi, dan porsi terjual bergerak begitu kasir menutup pesanan.
    const val ORDERS = "orders"
    const val ORDER_ITEMS = "order_items"
}

/**
 * Memanggil [onChange] setiap kali salah satu [tables] berubah di server, dan
 * sekali lagi tiap sambungan pulih supaya perubahan yang terlewat saat jaringan
 * putus ikut terkejar.
 *
 * Langganan hanya hidup selama layar terlihat: begitu layar berpindah ke
 * belakang, channel-nya ditinggalkan dan perangkat berhenti menerima aliran
 * tabel itu.
 */
@Composable
fun RealtimeRefresh(vararg tables: String, onChange: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latest = rememberUpdatedState(onChange)
    val key = remember(tables) { tables.sorted().joinToString(",") }
    LaunchedEffect(key, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            Realtime.updates(*tables).collect { latest.value() }
        }
    }
}
