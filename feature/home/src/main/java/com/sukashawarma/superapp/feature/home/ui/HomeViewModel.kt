package com.sukashawarma.superapp.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.Realtime
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.feature.chat.ChatBacaan
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
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
    val memuatSorotan: Boolean = true,
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

    fun hitungChat(context: Context) {
        val userId = AppSession.staff.value?.id ?: return
        val app = context.applicationContext
        viewModelScope.launch {
            val jumlah = runCatching { ChatBacaan.hitungBelumDibaca(app, userId) }.getOrNull() ?: return@launch
            if (jumlah != _state.value.chatBelumDibaca) {
                _state.value = _state.value.copy(chatBelumDibaca = jumlah)
            }
        }
    }

    private fun muatSorotan(staff: StaffProfile?) {
        val role = staff?.role
        val outletId = staff?.outletId
        if (role == null) {
            _state.value = _state.value.copy(memuatSorotan = false)
            return
        }
        viewModelScope.launch {
            val stok = async {
                if (outletId == null || role !in STOK_ROLES) null
                else runCatching {
                    // Cermin PermintaanRepository.saran(): `monitoring_view_crew` adalah
                    // view SECURITY DEFINER, jadi saldo tetap terbaca walau RLS
                    // stok_balance membatasi.
                    val baris = Postgrest.select(
                        "monitoring_view_crew",
                        listOf("select" to "status", "outlet_id" to "eq.$outletId"),
                    ).map { it.asJsonObject.optString("status") }
                    baris.count { it == "below" } to baris.count { it == "warning" }
                }.getOrNull()
            }
            val kiriman = async {
                if (outletId == null || role !in DISTRIBUSI_ROLES) null
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
                if (role !in LEADER_ROLES) null
                else runCatching {
                    // Cermin LencanaViewModel modul Leader: hanya status
                    // `forwarded_by_area_manager` yang benar-benar menunggu tangan
                    // leader — `leader_forward_funds` menolak status lain apa pun.
                    Postgrest.select(
                        "petty_cash_topups",
                        listOf("select" to "id", "status" to "eq.forwarded_by_area_manager"),
                    ).size()
                }.getOrNull()
            }
            val hasilStok = stok.await()
            _state.value = _state.value.copy(
                stokKritis = hasilStok?.first,
                stokMenipis = hasilStok?.second,
                kirimanMenunggu = kiriman.await(),
                wasteMenunggu = waste.await(),
                pettyCashButuhAksi = pettyCash.await(),
                memuatSorotan = false,
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
