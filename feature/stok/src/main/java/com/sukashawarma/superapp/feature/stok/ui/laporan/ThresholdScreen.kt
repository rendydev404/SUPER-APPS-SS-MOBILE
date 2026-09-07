package com.sukashawarma.superapp.feature.stok.ui.laporan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val ORANGE = Color(0xFFEA580C)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val SLATE900 = Color(0xFF0F172A)
private val GARIS = Color(0xFFE2E8F0)
private val HIJAU = Color(0xFF15803D)

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

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "Pengaturan Threshold",
            subjudul = if (state.memuat) "Memuat…" else "${state.jumlahDitimpa} dari ${state.baris.size} bahan diatur khusus",
            onKembali = onBack,
        )

        val pesan = state.pesan ?: state.error
        if (pesan != null) {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable { viewModel.bersihkanPesan() },
                shape = RoundedCornerShape(12.dp),
                color = if (state.pesan != null) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
            ) {
                Text(
                    pesan,
                    Modifier.padding(12.dp),
                    color = if (state.pesan != null) HIJAU else Color(0xFFB91C1C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (state.memuat) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ORANGE)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                PemilihOutletThreshold(state, viewModel)
                OutlinedTextField(
                    value = state.cari,
                    onValueChange = viewModel::ubahCari,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Cari bahan…", fontSize = 12.5.sp, color = SLATE400) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = kolomThreshold(),
                )
                Spacer(Modifier.height(12.dp))
                if (state.tampil.isEmpty()) {
                    PesanKosongLaporan("Tidak ada bahan yang cocok.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
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
    Box(Modifier.padding(16.dp)) {
        Surface(
            Modifier.fillMaxWidth().clickable(enabled = state.outlets.size > 1) { terbuka = true },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, GARIS),
        ) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.outletTerpilih?.name ?: "Pilih outlet",
                    Modifier.weight(1f),
                    color = SLATE900,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.outlets.size > 1) Icon(Icons.Default.ArrowDropDown, null, tint = SLATE500)
            }
        }
        DropdownMenu(terbuka, { terbuka = false }) {
            state.outlets.forEach { outlet ->
                DropdownMenuItem(
                    text = { Text(outlet.name, fontSize = 13.sp) },
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
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (baris.ditimpa) ORANGE.copy(alpha = 0.4f) else GARIS),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        baris.nama,
                        color = SLATE900,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (baris.ditimpa) {
                            "Khusus outlet ini · bawaan ${formatAngkaStok(baris.defaultReorderPoint)} ${baris.satuan}"
                        } else {
                            "Memakai nilai bawaan"
                        },
                        color = if (baris.ditimpa) ORANGE else SLATE400,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (!sedang) {
                    Text(
                        "${formatAngkaStok(baris.berlaku)} ${baris.satuan}",
                        color = SLATE900,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.mulaiUbah(baris) }) {
                        Text("UBAH", color = ORANGE, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            if (sedang) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.nilaiKetikan,
                    onValueChange = viewModel::ubahNilai,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Titik pesan ulang (${baris.satuan})", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(11.dp),
                    colors = kolomThreshold(),
                )
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::batalUbah, modifier = Modifier.weight(1f)) {
                        Text("Batal", fontSize = 12.sp, color = SLATE500, fontWeight = FontWeight.Bold)
                    }
                    if (baris.ditimpa) {
                        TextButton(
                            onClick = { viewModel.kembalikanKeBawaan(baris) },
                            enabled = !state.menyimpan,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Pakai bawaan", fontSize = 12.sp, color = ORANGE, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { viewModel.simpan(baris) },
                        enabled = !state.menyimpan,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ORANGE),
                    ) {
                        Text(if (state.menyimpan) "…" else "Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun kolomThreshold() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = ORANGE,
    unfocusedBorderColor = GARIS,
)
