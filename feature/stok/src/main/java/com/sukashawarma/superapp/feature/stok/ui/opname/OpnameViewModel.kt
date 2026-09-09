package com.sukashawarma.superapp.feature.stok.ui.opname

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.local.AppDatabase
import com.sukashawarma.superapp.data.local.entity.PendingOpnameFinalizeEntity
import kotlinx.coroutines.CancellationException
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.stok.data.OpnameRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.data.model.OpnameItemRow
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.data.model.StatusOpname
import com.sukashawarma.superapp.feature.stok.domain.AlasanOpnameTerkunci
import com.sukashawarma.superapp.feature.stok.domain.MasukanBerjenjang
import com.sukashawarma.superapp.feature.stok.domain.OpnameHitung
import com.sukashawarma.superapp.feature.stok.domain.bungkusCatatanOpname
import com.sukashawarma.superapp.feature.stok.domain.formatTriUnitAdaptif
import com.sukashawarma.superapp.feature.stok.domain.Selisih
import com.sukashawarma.superapp.feature.stok.domain.bolehTampilDiOutlet
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OpnameUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    val tidakBerhak: Boolean = false,
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val riwayat: List<OpnameHeader> = emptyList(),
    // Form
    val formTerbuka: Boolean = false,
    val memuatForm: Boolean = false,
    val menyimpan: Boolean = false,
    val opnameId: String? = null,
    val statusDraft: String? = null,
    /** Terisi bila opname hari ini ada tapi tidak boleh disunting; lihat [AlasanOpnameTerkunci]. */
    val terkunci: AlasanOpnameTerkunci? = null,
    val items: List<OpnameItemRow> = emptyList(),
    val cari: String = "",
) {
    val itemTampil: List<OpnameItemRow>
        get() {
            val kata = cari.trim().lowercase()
            return if (kata.isEmpty()) items
            else items.filter { it.namaBahan.lowercase().contains(kata) }
        }

    val jumlahTerisi: Int get() = items.count { it.adaMasukan }

    /**
     * Crew yang sudah menyelesaikan opname hari ini tidak boleh membuat opname baru.
     * Role selain crew (leader, SPV, admin, dsb.) tetap boleh.
     */
    val crewSudahOpname: Boolean
        get() {
            val role = AppSession.staff.value?.role
            if (role != Role.CREW) return false
            val hariIni = java.time.LocalDate.now().toString()  // "2026-09-09"
            return riwayat.any { h ->
                h.tanggal == hariIni && (
                    h.status == StatusOpname.FINALIZED ||
                    h.status == StatusOpname.APPROVED ||
                    h.status == StatusOpname.PENDING_APPROVAL
                )
            }
        }
}

class OpnameViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(OpnameUiState())
    val state: StateFlow<OpnameUiState> = _state

    private val antreanFinalisasi = AppDatabase.get(app).pendingOpnameFinalizeDao()

    init {
        muatAwal()
        // Antrean dicoba tiap layar dibuka. Tidak ada pemantau jaringan tersendiri:
        // opname dibuka manual oleh kru, dan kesempatan itu sudah cukup sering — satu
        // pemantau konektivitas hanya menambah bagian yang bisa rusak sendiri.
        kirimUlangAntrean()
    }

    /**
     * Mengirim ulang finalisasi yang tertunda.
     *
     * Baris dibuang HANYA ketika servernya menerima, atau ketika opname itu ternyata
     * sudah finalized — dua-duanya berarti pekerjaannya selesai. Kegagalan lain
     * dibiarkan mengantre; menghapusnya berarti stok yang tidak pernah terpotong.
     */
    private fun kirimUlangAntrean() {
        viewModelScope.launch {
            val tertunda = runCatching { antreanFinalisasi.getAll() }.getOrElse { return@launch }
            if (tertunda.isEmpty()) return@launch
            var berhasil = 0
            for (baris in tertunda) {
                try {
                    OpnameRepository.finalisasi(baris.opnameId)
                    antreanFinalisasi.delete(baris.opnameId)
                    berhasil++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (OpnameRepository.sudahFinalized(baris.opnameId)) {
                        antreanFinalisasi.delete(baris.opnameId)
                        berhasil++
                    } else {
                        antreanFinalisasi.markFailedAttempt(baris.opnameId, e.message)
                    }
                }
            }
            if (berhasil > 0) {
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    pesan = "$berhasil opname yang tertunda berhasil dikirim.",
                )
                muatRiwayat()
            }
        }
    }

    fun muatAwal() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null, tidakBerhak = false)
            try {
                val outlets = StokRepository.accessibleOutlets()
                if (outlets.isEmpty()) {
                    _state.value = _state.value.copy(memuat = false, tidakBerhak = true)
                    return@launch
                }
                val terpilih = _state.value.outletTerpilih?.let { lama ->
                    outlets.firstOrNull { it.id == lama.id }
                } ?: outlets.first()
                _state.value = _state.value.copy(outlets = outlets, outletTerpilih = terpilih)
                muatRiwayat()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(outletTerpilih = outlet, riwayat = emptyList())
        viewModelScope.launch { muatRiwayat() }
    }

    private suspend fun muatRiwayat() {
        val outlet = _state.value.outletTerpilih ?: return
        _state.value = _state.value.copy(memuat = true, error = null)
        try {
            _state.value = _state.value.copy(memuat = false, riwayat = OpnameRepository.daftar(outlet.id))
        } catch (e: Exception) {
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    // ------------------------------------------------------------------ form

    /**
     * Siapkan formulir hitung fisik.
     *
     * Draft dibuat atau dipakai ulang lebih dulu supaya item yang sudah tersimpan
     * bisa dilanjutkan — kru sering menghitung sambil berjalan dan menutup aplikasi
     * di tengah jalan.
     */
    fun bukaForm() {
        val outlet = _state.value.outletTerpilih ?: return
        val staffId = AppSession.staff.value?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(formTerbuka = true, memuatForm = true, error = null, pesan = null)
            try {
                val draft = OpnameRepository.buatAtauPakaiDraft(outlet.id, "harian", staffId)

                // Status diperiksa DI SINI, bukan dibiarkan gagal saat menyimpan.
                // Menulis item ke opname non-draft ditolak policy RLS `opname_item_write`,
                // dan galat PostgREST-nya tidak memberi tahu kru apa pun yang berguna.
                val terkunci = when (draft.status) {
                    StatusOpname.DRAFT -> null
                    StatusOpname.PENDING_APPROVAL -> AlasanOpnameTerkunci.SEDANG_DITINJAU
                    StatusOpname.REJECTED -> AlasanOpnameTerkunci.DITOLAK
                    StatusOpname.FINALIZED, StatusOpname.APPROVED -> AlasanOpnameTerkunci.SUDAH_FINAL
                }
                if (terkunci != null) {
                    _state.value = _state.value.copy(
                        memuatForm = false,
                        opnameId = draft.id,
                        statusDraft = draft.status.nilai,
                        terkunci = terkunci,
                        items = emptyList(),
                    )
                    return@launch
                }

                val tersimpan = OpnameRepository.itemTersimpan(draft.id)

                val baris = StokRepository.monitoringOutlet(outlet.id)
                    .filter { bolehTampilDiOutlet(it.itemName, it.outletName) }
                    .map { row ->
                        val sistem = OpnameHitung.saldoSistemSmallest(
                            saldo = row.currentQty,
                            saldoIsGram = row.saldoIsGram,
                            meta = row.meta,
                        )
                        // Masukan mentah dipulihkan persis seperti diketik. Baris lama
                        // yang belum menyimpannya jatuh ke kolom satuan terkecil —
                        // angkanya tetap benar, hanya penyajiannya yang tidak terpecah.
                        val draft = tersimpan[row.bahanBakuId]
                        val masukan = draft?.masukan
                        OpnameItemRow(
                            bahanBakuId = row.bahanBakuId,
                            namaBahan = row.itemName,
                            kategori = row.kategori,
                            meta = row.meta,
                            qtySystemSmallest = sistem,
                            saldoIsGram = row.saldoIsGram,
                            terukur = Selisih.ambangPersen(row.meta.satuan, row.meta.satuanKecil) > 0,
                            besar = masukan?.besar.orEmpty(),
                            tengah = masukan?.tengah.orEmpty(),
                            kecil = masukan?.kecil
                                ?: draft?.qtyFisik?.let {
                                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                                }
                                ?: "",
                            // Item yang sudah pernah tersimpan di server ditandai;
                            // item baru yang belum pernah disimpan tetap polos.
                            tersimpanDraft = draft != null,
                        )
                    }
                _state.value = _state.value.copy(
                    memuatForm = false,
                    opnameId = draft.id,
                    statusDraft = draft.status.nilai,
                    terkunci = null,
                    items = baris,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuatForm = false, error = stokErrorMessage(e))
            }
        }
    }

    fun tutupForm() {
        _state.value = _state.value.copy(
            formTerbuka = false, items = emptyList(), opnameId = null, cari = "", terkunci = null,
        )
        viewModelScope.launch { muatRiwayat() }
    }

    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }

    fun ubahMasukan(bahanBakuId: String, besar: String? = null, tengah: String? = null, kecil: String? = null) {
        _state.value = _state.value.copy(
            items = _state.value.items.map { item ->
                if (item.bahanBakuId != bahanBakuId) item
                else item.copy(
                    besar = besar ?: item.besar,
                    tengah = tengah ?: item.tengah,
                    kecil = kecil ?: item.kecil,
                )
            }
        )
    }

    /** Hitungan fisik satu baris pada satuan terkecil. */
    fun fisik(item: OpnameItemRow): Double = OpnameHitung.totalFisikSmallest(
        besar = item.besar.toDoubleOrNull() ?: 0.0,
        tengah = item.tengah.toDoubleOrNull() ?: 0.0,
        kecil = item.kecil.toDoubleOrNull() ?: 0.0,
        meta = item.meta,
    )

    fun selisih(item: OpnameItemRow): Double = Selisih.hitung(fisik(item), item.qtySystemSmallest)

    fun ditandai(item: OpnameItemRow): Boolean = Selisih.perluDitandai(
        selisih = selisih(item),
        qtySystem = item.qtySystemSmallest,
        satuan = item.meta.satuan,
        satuanKecil = item.meta.satuanKecil,
    )

    private fun itemUntukDisimpan(opnameId: String): List<OpnameRepository.ItemSimpan> =
        _state.value.items.filter { it.adaMasukan }.map { item ->
            val fisikNilai = fisik(item)
            val selisihNilai = selisih(item)
            OpnameRepository.ItemSimpan(
                opnameId = opnameId,
                bahanBakuId = item.bahanBakuId,
                qtyFisik = fisikNilai,
                qtySystem = item.qtySystemSmallest,
                flagged = ditandai(item),
                // Angka yang DIKETIK kru ikut disimpan, bukan hanya totalnya. Tanpa ini,
                // draft yang dilanjutkan memunculkan satu angka besar di kolom satuan
                // terkecil alih-alih "2 Dus 3 Kg 500 Gram" yang tadi diisi.
                catatan = bungkusCatatanOpname(
                    fisikTeks = formatTriUnitAdaptif(fisikNilai, saldoIsGram = true, meta = item.meta),
                    sistemTeks = formatTriUnitAdaptif(item.qtySystemSmallest, saldoIsGram = true, meta = item.meta),
                    selisihTeks = formatTriUnitAdaptif(selisihNilai, saldoIsGram = true, meta = item.meta),
                    masukan = MasukanBerjenjang(item.besar, item.tengah, item.kecil),
                ),
            )
        }

    fun simpanDraft() {
        val opnameId = _state.value.opnameId ?: return
        val items = itemUntukDisimpan(opnameId)
        if (items.isEmpty()) {
            _state.value = _state.value.copy(pesan = "Belum ada item yang diisi.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(menyimpan = true, error = null, pesan = null)
            try {
                OpnameRepository.simpanItem(items)
                // Tandai baris yang sudah tersimpan supaya UI menampilkan
                // indikator visual — baris yang polos belum pernah ke server.
                val idTersimpan = items.map { it.bahanBakuId }.toSet()
                _state.value = _state.value.copy(
                    menyimpan = false,
                    pesan = "Draft tersimpan (${items.size} item).",
                    items = _state.value.items.map { row ->
                        if (row.bahanBakuId in idTersimpan) row.copy(tersimpanDraft = true)
                        else row
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(menyimpan = false, error = stokErrorMessage(e))
            }
        }
    }

    /**
     * Simpan lalu finalisasi.
     *
     * Web SELALU memanggil `finalize` walau ada item yang ditandai — `setPendingApproval`
     * tersedia tetapi tidak pernah dipakai dari formulir. Perilaku itu ditiru persis di
     * sini secara sadar: kalau native menahan opname bertanda untuk approval sementara
     * web langsung memfinalisasi, satu tindakan yang sama akan menghasilkan saldo yang
     * berbeda tergantung perangkat yang dipakai — dan itu lebih berbahaya daripada
     * meneruskan kesenjangan yang sudah ada.
     */
    fun finalisasi() {
        val opnameId = _state.value.opnameId ?: return
        val items = itemUntukDisimpan(opnameId)
        if (items.isEmpty()) {
            _state.value = _state.value.copy(pesan = "Tidak ada item yang diinput.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(menyimpan = true, error = null, pesan = null)
            try {
                OpnameRepository.simpanItem(items)
                OpnameRepository.finalisasi(opnameId)
                val bertanda = items.count { it.flagged }
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    menyimpan = false,
                    formTerbuka = false,
                    items = emptyList(),
                    opnameId = null,
                    pesan = if (bertanda > 0) {
                        "Opname difinalisasi. $bertanda item di luar toleransi tercatat sebagai selisih."
                    } else {
                        "Opname berhasil difinalisasi."
                    },
                )
                muatRiwayat()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                antrekanFinalisasiGagal(opnameId, e)
            }
        }
    }

    /**
     * Finalisasi gagal — diantre kalau item-nya sudah tersimpan.
     *
     * Yang menentukan bisa-tidaknya diantre adalah [OpnameRepository.simpanItem] yang
     * SUDAH lewat sebelum baris ini tercapai: hitungan kru ada di server, tinggal
     * pemotongan stoknya yang tertunda. Kalau justru penyimpanan item yang gagal,
     * tidak ada yang layak diantre — kru harus mengulang, dan itu jalur galat biasa.
     */
    private suspend fun antrekanFinalisasiGagal(opnameId: String, penyebab: Exception) {
        val outletId = _state.value.outletTerpilih?.id
        val terantre = outletId != null && runCatching {
            antreanFinalisasi.insert(
                PendingOpnameFinalizeEntity(
                    opnameId = opnameId,
                    outletId = outletId,
                    createdAtMs = System.currentTimeMillis(),
                    lastError = penyebab.message,
                )
            )
        }.isSuccess

        android.util.Log.w("OpnameViewModel", "finalisasi gagal, terantre=$terantre", penyebab)
        _state.value = if (terantre) {
            _state.value.copy(
                menyimpan = false,
                formTerbuka = false,
                items = emptyList(),
                opnameId = null,
                pesan = "Hitungan tersimpan, tetapi finalisasi belum terkirim. " +
                    "Akan dicoba lagi otomatis saat modul Opname dibuka kembali dengan koneksi aktif.",
            )
        } else {
            _state.value.copy(menyimpan = false, error = stokErrorMessage(penyebab))
        }
    }

    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }
}
