package com.sukashawarma.superapp.presentation.absensi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos

/*
 * Pelengkap design system iOS (`core.ui.ios`) yang belum tersedia di core tetapi dipakai
 * berulang oleh layar-layar Absensi. Sengaja `internal` dan hanya memakai token
 * WarnaIos/TipeIos/UkuranIos, supaya tampilannya tetap satu bahasa dengan modul lain.
 */

/** Kartu putih berisi pemutar aksen — keadaan memuat di tengah daftar kartu. */
@Composable
internal fun KartuMemuatIos(modifier: Modifier = Modifier) {
    KartuIos(modifier.heightIn(min = 120.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 88.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WarnaIos.Aksen, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
        }
    }
}

/** Nada lencana untuk status pengajuan (cuti, kasbon, izin): approved/rejected/lainnya. */
internal fun nadaStatusPengajuan(status: String): NadaIos = when (status) {
    "approved" -> NadaIos.SUKSES
    "rejected" -> NadaIos.BAHAYA
    else -> NadaIos.PERINGATAN
}

/** Warna TextField terisi (tanpa garis) gaya iOS — isian abu seperti kolom cari. */
@Composable
internal fun warnaBidangIsianIos(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = WarnaIos.Isian,
    unfocusedContainerColor = WarnaIos.Isian,
    disabledContainerColor = WarnaIos.Isian,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = WarnaIos.Aksen,
    focusedTextColor = WarnaIos.Label,
    unfocusedTextColor = WarnaIos.Label,
    focusedPlaceholderColor = WarnaIos.Abu,
    unfocusedPlaceholderColor = WarnaIos.Abu,
)
