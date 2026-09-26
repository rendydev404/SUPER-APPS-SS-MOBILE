package com.sukashawarma.superapp.presentation.home

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.feature.chat.ChatBacaan
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.feature.home.domain.BonusBulanan
import com.sukashawarma.superapp.feature.home.domain.skemaBonusUntuk
import com.sukashawarma.superapp.feature.home.domain.susunBonusBulanan
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TodayAttendance(val type: String, val tsServerIso: String, val status: String)

data class HomeUiState(
    val staff: StaffProfile? = null,
    val greeting: String = "",
    val dateLabel: String = "",
    val todayAttendance: TodayAttendance? = null,
    val loadingAttendance: Boolean = true,
    /** Menahan klik kedua saat token SSO POS sedang diterbitkan. */
    val membukaPos: Boolean = false,
    /**
     * Angka hidup untuk kartu modul. `null` berarti belum termuat atau memang
     * tidak berlaku untuk role/outlet ini — kartu menampilkan "—", bukan "0",
     * karena nol adalah kabar baik dan tidak boleh tertukar dengan tidak tahu.
     */
    val stokKritis: Int? = null,
    val stokMenipis: Int? = null,
    val kirimanMenunggu: Int? = null,
    val wasteMenunggu: Int? = null,
    val pettyCashButuhAksi: Int? = null,
    /** Pesan Chat Tim yang belum dibaca di perangkat ini. 0 = tidak ada lencana. */
    val chatBelumDibaca: Int = 0,
    /** Ada pesan belum dibaca yang menyebut nama pengguna. Menaikkan lencana
     *  dari sekadar "ada pesan baru" menjadi "ada yang memanggil Anda". */
    val chatAdaSebutan: Boolean = false,
    val memuatSorotan: Boolean = true,
    /**
     * Estimasi insentif bulan berjalan. `null` selagi memuat, gagal, atau role
     * ini memang tidak punya skema bonus — kartu hanya tampil bila [adaSkemaBonus].
     */
    val bonus: BonusBulanan? = null,
    val memuatBonus: Boolean = true,
    val adaSkemaBonus: Boolean = false,
) {
    /** Jam absen terakhir hari ini dalam WIB, mis. "07:12". */
    val jamAbsen: String? get() = todayAttendance?.tsServerIso?.let { iso ->
        runCatching {
            OffsetDateTime.parse(iso).atZoneSameInstant(JakartaTime.ZONE)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
    }
}

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    /** Pesan sekali tayang untuk kartu POS (toast), bukan bagian dari state layar. */
    private val _pesanPos = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val pesanPos: SharedFlow<String> = _pesanPos

    init {
        val now = JakartaTime.now()
        val greeting = when {
            now.hour < 11 -> "Selamat pagi"
            now.hour < 15 -> "Selamat siang"
            now.hour < 18 -> "Selamat sore"
            else -> "Selamat malam"
        }
        val dateLabel = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")))
        _state.value = _state.value.copy(greeting = greeting, dateLabel = dateLabel)

        // MENGIKUTI sesi, bukan memotretnya sekali di sini. Versi sebelumnya membaca
        // AppSession.staff.value satu kali, jadi nama dan foto profil yang baru
        // disimpan tidak pernah sampai ke beranda sampai ViewModel dibuat ulang —
        // yang praktis berarti "tutup dan buka lagi aplikasinya".
        viewModelScope.launch {
            var kunciSebelumnya: String? = null
            AppSession.staff.collect { staff ->
                _state.value = _state.value.copy(staff = staff)

                // Angka absensi dan sorotan modul hanya bergantung pada SIAPA dan DI
                // MANA, bukan pada nama tampilan atau foto. Tanpa gerbang ini, setiap
                // penyuntingan profil menembakkan ulang empat query beranda —
                // pemborosan yang langsung terasa di ratusan perangkat.
                val kunci = "${staff?.id}|${staff?.outletId}|${staff?.roleRaw}"
                if (kunci == kunciSebelumnya) return@collect
                kunciSebelumnya = kunci
                loadTodayAttendance(staff?.id)
                muatSorotan(staff)
            }
        }
    }

    /**
     * Angka yang tampil di kartu modul. Tiga query ringan (hanya kolom penanda,
     * tanpa join) dijalankan berbarengan dan masing-masing boleh gagal sendiri —
     * beranda tidak boleh ikut kosong hanya karena satu modul tidak terbaca.
     *
     * Query hanya dijalankan untuk kartu yang memang tampil bagi role ini, jadi
     * crew tidak pernah menembak tabel waste dan manager tanpa outlet tidak
     * menembak monitoring stok.
     */
    private var pemantauChat: kotlinx.coroutines.Job? = null

    /**
     * Menyalakan pemantau lencana Chat Tim.
     *
     * Angkanya disegarkan dua kali: sekali saat dipanggil (Beranda kembali
     * terlihat — penandanya bisa saja baru berubah karena percakapan ditutup),
     * dan setiap kali tabel pesan berubah selagi Beranda terbuka. Tanpa yang
     * kedua, lencana baru muncul setelah pengguna keluar-masuk Beranda.
     *
     * Context diminta per panggilan, bukan disimpan: menyimpan Context di dalam
     * ViewModel adalah cara paling mudah menahan Activity dari GC.
     */
    fun pantauChat(context: Context) {
        hitungChat(context)
        if (pemantauChat != null) return
        pemantauChat = viewModelScope.launch {
            Realtime.updates(ChatRepository.TABLE).collect { hitungChat(context) }
        }
    }

    /** Hentikan seluruh pekerjaan Chat Tim setelah masa tayangnya selesai. */
    fun berhentiPantauChat() {
        pemantauChat?.cancel()
        pemantauChat = null
        val kini = _state.value
        if (kini.chatBelumDibaca != 0 || kini.chatAdaSebutan) {
            _state.value = kini.copy(chatBelumDibaca = 0, chatAdaSebutan = false)
        }
    }

    fun hitungChat(context: Context) {
        val userId = AppSession.staff.value?.id ?: return
        val app = context.applicationContext
        viewModelScope.launch {
            val jumlah = runCatching { ChatBacaan.hitungBelumDibaca(app, userId) }.getOrNull() ?: return@launch
            // Sebutan hanya ditanyakan bila memang ADA yang belum dibaca. Tanpa
            // pesan baru, jawabannya sudah pasti tidak — dan permintaan kedua
            // ini ikut berjalan setiap kali ada pesan masuk di grup.
            val disebut = jumlah > 0 &&
                runCatching { ChatBacaan.adaSebutan(app, userId) }.getOrDefault(false)
            val kini = _state.value
            if (jumlah != kini.chatBelumDibaca || disebut != kini.chatAdaSebutan) {
                _state.value = kini.copy(chatBelumDibaca = jumlah, chatAdaSebutan = disebut)
            }
        }
    }

    /**
     * Muat ulang angka absensi & sorotan modul untuk sesi yang sedang aktif.
     *
     * Dipanggil dari langganan realtime Beranda. Sengaja membaca [AppSession]
     * langsung, bukan menerima staff sebagai argumen: pemanggilnya adalah layar,
     * yang tidak punya urusan tahu siapa pemilik sesi.
     */
    fun segarkanSorotan() {
        val staff = AppSession.staff.value
        loadTodayAttendance(staff?.id)
        // Estimasi bonus adalah angka bulanan, dan RPC-nya termasuk yang paling
        // mahal di database (AM/RM hampir satu detik per panggilan). Dulu ikut
        // dihitung ulang di setiap event realtime beranda — artinya tiap absen atau
        // surat jalan di outlet mana pun. Kini cukup sekali per [UMUR_BONUS_MS];
        // pemuatan awal & pergantian sesi tetap selalu menghitungnya.
        val bonusBasi = SystemClock.elapsedRealtime() - bonusDimuatPada >= UMUR_BONUS_MS
        muatSorotan(staff, hitungBonus = bonusBasi)
    }

    /**
     * Muat ulang hanya angka stok kritis/menipis.
     *
     * Dipisah dari [segarkanSorotan] karena sumbernya `stok_balance`, tabel yang
     * berubah di SETIAP transaksi kasir di semua outlet. Dulu setiap perubahan itu
     * memicu seluruh sorotan beranda (bonus, absen, surat jalan, waste, petty
     * cash) di setiap HP yang sedang membuka beranda.
     */
    fun segarkanStok() {
        val staff = AppSession.staff.value ?: return
        if (staff.role !in STOK_ROLES) return
        viewModelScope.launch {
            val hasil = runCatching { hitungStok(staff.role, staff.outletId) }.getOrNull() ?: return@launch
            _state.value = _state.value.copy(stokKritis = hasil.first, stokMenipis = hasil.second)
        }
    }

    /**
     * Jumlah bahan kritis & menipis. null = tidak ada outlet untuk dihitung.
     *
     * AM/RM menjumlahkan SEMUA outlet binaannya — mereka tidak memegang satu outlet,
     * dan `outlet_id` di profilnya kadang hanya penanda. Role lain tetap outletnya
     * sendiri.
     */
    private suspend fun hitungStok(role: Role?, outletId: String?): Pair<Int, Int>? {
        val ids = if (role in MANAGER_ROLES) outletBinaan() else listOfNotNull(outletId)
        if (ids.isEmpty()) return null
        // Cermin PermintaanRepository.saran(): `monitoring_view_crew` adalah
        // view SECURITY DEFINER, jadi saldo tetap terbaca walau RLS
        // stok_balance membatasi. Karena itu cakupannya WAJIB dari filter ini.
        val baris = Postgrest.select(
            "monitoring_view_crew",
            listOf("select" to "status", "outlet_id" to "in.(${ids.joinToString(",")})"),
        ).map { it.asJsonObject.optString("status") }
        return baris.count { it == "below" } to baris.count { it == "warning" }
    }

    /**
     * Cabang penjualan dalam cakupan pengguna menurut `accessible_outlet_ids()`.
     *
     * Tabel `outlets` sendiri terbaca penuh oleh semua orang (policy `USING (true)`),
     * jadi cakupannya dari RPC, lalu disaring ke tipe `outlet`/`mitra`. Kantor Pusat
     * dan Gudang Pusat tidak dihitung: bukan cabang binaan.
     */
    private suspend fun outletBinaan(): List<String> {
        val ids = Postgrest.rpc("accessible_outlet_ids").let { el ->
            if (!el.isJsonArray) emptyList()
            else el.asJsonArray.mapNotNull { item ->
                when {
                    item.isJsonPrimitive -> item.asString
                    item.isJsonObject -> item.asJsonObject.optString("accessible_outlet_ids")
                    else -> null
                }
            }
        }.distinct()
        if (ids.isEmpty()) return emptyList()
        return Postgrest.select(
            "outlets",
            listOf(
                "select" to "id",
                "id" to "in.(${ids.joinToString(",")})",
                "is_active" to "eq.true",
                "type" to "in.(outlet,mitra)",
            ),
        ).mapNotNull { it.asJsonObject.optString("id") }
    }

    /** Waktu (elapsedRealtime) estimasi bonus terakhir berhasil dihitung. */
    private var bonusDimuatPada = 0L

    private companion object {
        /** Umur estimasi bonus sebelum event realtime boleh menghitungnya ulang. */
        const val UMUR_BONUS_MS = 10 * 60_000L
    }

    private fun muatSorotan(staff: StaffProfile?, hitungBonus: Boolean = true) {
        val role = staff?.role
        val outletId = staff?.outletId
        if (role == null) {
            _state.value = _state.value.copy(memuatSorotan = false)
            return
        }
        val skemaBonus = skemaBonusUntuk(role)
        // Bonus yang tidak dihitung ulang dibiarkan apa adanya di state — bukan
        // dikosongkan — supaya kartunya tidak berkedip "memuat".
        val muatBonus = hitungBonus && skemaBonus != null
        _state.value = if (muatBonus) {
            _state.value.copy(adaSkemaBonus = true, memuatBonus = true)
        } else {
            _state.value.copy(adaSkemaBonus = skemaBonus != null)
        }
        viewModelScope.launch {
            val bonus = async {
                if (!muatBonus || skemaBonus == null || staff == null) null
                else runCatching {
                    // Bulan berjalan menurut WIB — RPC-nya juga memotong periode di
                    // Asia/Jakarta, jadi keduanya sepakat soal "bulan ini".
                    val kini = JakartaTime.now()
                    val body = com.google.gson.JsonObject().apply {
                        addProperty("p_month", kini.monthValue)
                        addProperty("p_year", kini.year)
                        // Skema kru menerima filter outlet; AM/RM tidak punya parameter itu.
                        if (skemaBonus == com.sukashawarma.superapp.feature.home.domain.SkemaBonus.KRU) {
                            addProperty("p_outlet_id", outletId)
                        }
                    }
                    val baris = Postgrest.rpc(skemaBonus.rpc, body)
                        .takeIf { it.isJsonArray }?.asJsonArray
                        ?.map { it.asJsonObject } ?: emptyList()
                    susunBonusBulanan(skemaBonus, staff.id, baris, kini.monthValue, kini.year)
                }.getOrNull()
            }
            val stok = async {
                if (role !in STOK_ROLES) null
                else runCatching { hitungStok(role, outletId) }.getOrNull()
            }
            val kiriman = async {
                // Kitchen adalah pengirim; "kiriman masuk" ke outletnya tidak bermakna.
                if (outletId == null || role !in DISTRIBUSI_ROLES || role == Role.KITCHEN) null
                else runCatching {
                    // Status yang sama dengan SuratJalanRepository.inbox().
                    Postgrest.select(
                        "surat_jalan",
                        listOf(
                            "select" to "id",
                            "outlet_id" to "eq.$outletId",
                            "status" to "in.(dikirim,dikirim_lengkap,diterima_sebagian)",
                        ),
                    ).size()
                }.getOrNull()
            }
            val waste = async {
                if (role !in MANAGER_ROLES) null
                else runCatching {
                    // Cermin ManagerRepository.jumlahWasteMenunggu(): sengaja tanpa
                    // batas tanggal — laporan yang menggantung sejak minggu lalu
                    // justru yang paling perlu terlihat.
                    Postgrest.select(
                        "stok_waste_reports",
                        listOf("select" to "id", "status" to "eq.PENDING"),
                    ).size()
                }.getOrNull()
            }
            val pettyCash = async {
                // Status yang menunggu tangan pengguna ini. Cakupan outletnya sudah
                // dijaga RLS `petty_cash_topups` lewat accessible_outlet_ids().
                val status = when (role) {
                    // Cermin LencanaViewModel modul Leader: `leader_forward_funds`
                    // menolak status lain apa pun.
                    in LEADER_ROLES -> "eq.forwarded_by_area_manager"
                    // Cermin STATUS_BUTUH_REVIEW modul Manager: menunggu ACC atau
                    // dana sudah cair dan tinggal diserahkan ke leader.
                    in MANAGER_ROLES ->
                        "in.(pending,forwarded_to_area_manager,approved_by_finance,forwarded_by_finance)"
                    else -> null
                }
                if (status == null) null
                else runCatching {
                    Postgrest.select(
                        "petty_cash_topups",
                        listOf("select" to "id", "status" to status),
                    ).size()
                }.getOrNull()
            }
            val hasilStok = stok.await()
            val hasilBonus = bonus.await()
            if (muatBonus && hasilBonus != null) bonusDimuatPada = SystemClock.elapsedRealtime()
            _state.value = _state.value.copy(
                stokKritis = hasilStok?.first,
                stokMenipis = hasilStok?.second,
                kirimanMenunggu = kiriman.await(),
                wasteMenunggu = waste.await(),
                pettyCashButuhAksi = pettyCash.await(),
                memuatSorotan = false,
                bonus = if (muatBonus) hasilBonus else _state.value.bonus,
                memuatBonus = false,
            )
        }
    }

    private fun loadTodayAttendance(staffId: String?) {
        if (staffId == null) {
            _state.value = _state.value.copy(loadingAttendance = false)
            return
        }
        viewModelScope.launch {
            try {
                val rows = Postgrest.select(
                    "attendance",
                    listOf(
                        "outlet_staff_id" to "eq.$staffId",
                        "ts_server" to "gte.${JakartaTime.todayStartIso()}",
                        "select" to "type,ts_server,status",
                        "order" to "ts_server.desc",
                        "limit" to "1"
                    )
                )
                val latest = if (rows.size() > 0) rows[0].asJsonObject else null
                val att = latest?.let {
                    TodayAttendance(
                        type = it.optString("type") ?: "",
                        tsServerIso = it.optString("ts_server") ?: "",
                        status = it.optString("status") ?: ""
                    )
                }
                _state.value = _state.value.copy(todayAttendance = att, loadingAttendance = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingAttendance = false)
            }
        }
    }

    /**
     * Membuka aplikasi POS dengan sesi yang sudah jadi, tanpa login ulang.
     *
     * Token SSO dititipkan lewat extra Intent hanya kalau paket POS terbukti
     * ditandatangani kunci yang sama. Kalau token gagal terbit (offline, akun
     * tanpa outlet), POS tetap dibuka — kasir tinggal login manual di sana,
     * jauh lebih baik daripada kartu yang tidak melakukan apa-apa.
     */
    fun bukaPos(context: Context) {
        if (_state.value.membukaPos) return
        val intent = intentBukaPos(context)
        if (intent == null) {
            _pesanPos.tryEmit("Aplikasi POS belum terpasang di perangkat ini")
            return
        }
        _state.value = _state.value.copy(membukaPos = true)
        viewModelScope.launch {
            val token = if (posDitandatanganiSama(context)) {
                mintTokenSsoPos()
            } else {
                // Paket bernama POS tapi bukan build kami: buka saja, jangan
                // pernah titipkan token sesi ke aplikasi yang tidak dikenal.
                _pesanPos.tryEmit("Aplikasi POS tidak dikenali, silakan login manual di sana")
                null
            }
            if (token != null) intent.putExtra(EXTRA_SSO_TOKEN_HASH, token)
            _state.value = _state.value.copy(membukaPos = false)
            context.startActivity(intent)
        }
    }

    fun logout() = AppSession.signOut()
}
