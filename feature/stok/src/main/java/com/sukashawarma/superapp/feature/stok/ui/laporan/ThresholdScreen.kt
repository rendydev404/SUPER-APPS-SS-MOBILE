package com.sukashawarma.superapp.feature.stok.ui.laporan

import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.BarisThreshold
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.ThresholdRepository
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


data class ThresholdUiState(
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val baris: List<BarisThreshold> = emptyList(),
    val cari: String = "",
    val sedangDiubah: String? = null,
    val nilaiKetikan: String = "",
    val menyimpan: Boolean = false,
    val memuat: Boolean = true,
    val pesan: String? = null,
    val error: String? = null,
) {
    val tampil: List<BarisThreshold>
        get() {
            val kueri = cari.trim().lowercase()
            if (kueri.isEmpty()) return baris
            return baris.filter {
                it.nama.lowercase().contains(kueri) || it.kategori.lowercase().contains(kueri)
            }
        }

    val jumlahDitimpa: Int get() = baris.count { it.ditimpa }
}

/**
 * Pengaturan titik pesan ulang — cermin `components/settings/ThresholdPage.tsx`.
 *
 * Angka di sini menentukan bahan mana yang tampil kritis di monitoring, jadi
 * perubahannya berdampak langsung ke seluruh layar lain. Karena itu nilai bawaan
 * selalu ikut ditampilkan: pengguna harus tahu dari mana angka itu berasal
 * sebelum menimpanya.
 */
class ThresholdViewModel : ViewModel() {
    private val _state = MutableStateFlow(ThresholdUiState())
    val state: StateFlow<ThresholdUiState> = _state

    init { muatAwal() }

    fun muatAwal() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val outlets = StokRepository.accessibleOutlets()
                val pilihan = _state.value.outletTerpilih ?: outlets.firstOrNull()
                _state.value = _state.value.copy(outlets = outlets, outletTerpilih = pilihan)
                if (pilihan != null) muatBaris(pilihan.id)
                else _state.value = _state.value.copy(memuat = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    private suspend fun muatBaris(outletId: String) {
        try {
            _state.value = _state.value.copy(memuat = false, baris = ThresholdRepository.daftar(outletId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ThresholdVM", "muatBaris() gagal", e)
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(outletTerpilih = outlet, memuat = true, sedangDiubah = null)
        viewModelScope.launch { muatBaris(outlet.id) }
    }

    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }
    fun bersihkanPesan() { _state.value = _state.value.copy(pesan = null, error = null) }

    fun mulaiUbah(baris: BarisThreshold) {
        _state.value = _state.value.copy(
            sedangDiubah = baris.bahanBakuId,
            nilaiKetikan = formatAngkaStok(baris.berlaku),
        )
    }

    fun batalUbah() { _state.value = _state.value.copy(sedangDiubah = null, nilaiKetikan = "") }
    fun ubahNilai(teks: String) { _state.value = _state.value.copy(nilaiKetikan = teks) }

    fun simpan(baris: BarisThreshold) {
        val outletId = _state.value.outletTerpilih?.id ?: return
        val nilai = _state.value.nilaiKetikan.trim().replace(',', '.').toDoubleOrNull()
        if (nilai == null || nilai < 0) {
            _state.value = _state.value.copy(error = "Nilai threshold tidak sah.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(menyimpan = true, error = null, pesan = null)
            try {
                ThresholdRepository.simpan(outletId, baris.bahanBakuId, nilai)
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    menyimpan = false,
                    sedangDiubah = null,
                    baris = _state.value.baris.map {
                        if (it.bahanBakuId == baris.bahanBakuId) it.copy(overrideOutlet = nilai) else it
                    },
                    pesan = "Threshold ${baris.nama} disimpan.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ThresholdVM", "simpan() gagal", e)
                _state.value = _state.value.copy(menyimpan = false, error = stokErrorMessage(e))
            }
        }
    }

    fun kembalikanKeBawaan(baris: BarisThreshold) {
        val outletId = _state.value.outletTerpilih?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(menyimpan = true, error = null, pesan = null)
            try {
                ThresholdRepository.kembalikanKeBawaan(outletId, baris.bahanBakuId)
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    menyimpan = false,
                    sedangDiubah = null,
                    baris = _state.value.baris.map {
                        if (it.bahanBakuId == baris.bahanBakuId) it.copy(overrideOutlet = null) else it
                    },
                    pesan = "${baris.nama} kembali memakai nilai bawaan.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(menyimpan = false, error = stokErrorMessage(e))
            }
        }
    }
}

@Composable
fun ThresholdScreen(
    onBack: () -> Unit,
    viewModel: ThresholdViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Bahan baku baru atau nilai bawaan yang diubah dari web harus terlihat di sini
    // tanpa keluar-masuk layar — angka di sini menentukan bahan mana yang tampil
    // kritis di monitoring, jadi daftar basi menyesatkan.
    RealtimeRefresh(RealtimeTables.BAHAN_BAKU) { viewModel.muatAwal() }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Pengaturan Threshold",
            subjudul = if (state.memuat) "Memuat…" else "${state.jumlahDitimpa} dari ${state.baris.size} bahan diatur khusus",
            onKembali = onBack,
        )

        val pesan = state.pesan ?: state.error
        if (pesan != null) {
            // Ketuk untuk menutup, sama seperti sebelumnya.
            BannerIos(
                pesan,
                if (state.pesan != null) NadaIos.SUKSES else NadaIos.BAHAYA,
                Modifier
                    .padding(horizontal = UkuranIos.TepiLayar, vertical = 10.dp)
                    .tekanIos({ viewModel.bersihkanPesan() }, skalaTekan = 0.99f),
                ikon = if (state.pesan != null) IkonIos.CheckCircle else IkonIos.ErrorOutline,
            )
        }

        if (state.memuat) {
            MemuatPenuh()
        } else {
            Column(Modifier.fillMaxSize()) {
                PemilihOutletThreshold(state, viewModel)
                KolomCariIos(
                    nilai = state.cari,
                    onUbah = viewModel::ubahCari,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = UkuranIos.TepiLayar),
                    placeholder = "Cari bahan…",
                )
                Spacer(Modifier.height(12.dp))
                if (state.tampil.isEmpty()) {
                    PesanKosongLaporan("Tidak ada bahan yang cocok.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, bottom = 16.dp).denganRuangNav(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.tampil, key = { it.bahanBakuId }) { baris ->
                            KartuThreshold(baris, state, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PemilihOutletThreshold(state: ThresholdUiState, viewModel: ThresholdViewModel) {
    var terbuka by remember { mutableStateOf(false) }
    val bisaPilih = state.outlets.size > 1
    Box(Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .permukaanIos(UkuranIos.SudutGrup)
                .then(if (bisaPilih) Modifier.tekanIos({ terbuka = true }) else Modifier)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Storefront, null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                state.outletTerpilih?.name ?: "Pilih outlet",
                Modifier.weight(1f),
                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (bisaPilih) Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
        }
        SukaDropdownMenu(terbuka, { terbuka = false }) {
            SukaDropdownHeader(title = "PILIH OUTLET", onClose = { terbuka = false })
            state.outlets.forEach { outlet ->
                SukaDropdownMenuItem(
                    text = outlet.name,
                    selected = (state.outletTerpilih?.id == outlet.id),
                    onClick = { terbuka = false; viewModel.pilihOutlet(outlet) },
                )
            }
        }
    }
}

@Composable
private fun KartuThreshold(
    baris: BarisThreshold,
    state: ThresholdUiState,
    viewModel: ThresholdViewModel,
) {
    val sedang = state.sedangDiubah == baris.bahanBakuId
    KartuIos(padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    baris.nama,
                    style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (baris.ditimpa) {
                    Spacer(Modifier.height(3.dp))
                    LencanaIos(
                        "Khusus outlet ini · bawaan ${formatAngkaStok(baris.defaultReorderPoint)} ${baris.satuan}",
                        NadaIos.AKSEN,
                    )
                } else {
                    Text("Memakai nilai bawaan", style = TipeIos.Catatan)
                }
            }
            if (!sedang) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "${formatAngkaStok(baris.berlaku)} ${baris.satuan}",
                    color = WarnaIos.Label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                TombolKapsulIos("Ubah", { viewModel.mulaiUbah(baris) })
            }
        }

        if (sedang) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.nilaiKetikan,
                onValueChange = viewModel::ubahNilai,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Titik pesan ulang (${baris.satuan})", fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = TipeIos.Isi,
                shape = UkuranIos.SudutKontrol,
                colors = warnaKolomIos(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TombolKeduaIos("Batal", viewModel::batalUbah, Modifier.weight(1f).height(44.dp), warna = WarnaIos.AbuGelap)
                if (baris.ditimpa) {
                    TombolKeduaIos(
                        "Pakai bawaan",
                        { viewModel.kembalikanKeBawaan(baris) },
                        Modifier.weight(1f).height(44.dp),
                        aktif = !state.menyimpan,
                    )
                }
                TombolUtamaIos(
                    if (state.menyimpan) "…" else "Simpan",
                    { viewModel.simpan(baris) },
                    Modifier.weight(1f).height(44.dp),
                    aktif = !state.menyimpan,
                )
            }
        }
    }
}
