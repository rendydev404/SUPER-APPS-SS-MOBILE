package com.sukashawarma.superapp.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Placeholder untuk layar yang belum digarap task berikutnya — dipakai sementara
 *  di AbsensiNavGraph supaya app tetap buildable & bisa di-navigasi sambil dicicil. */
@Composable
fun ComingSoon(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label — segera hadir", style = MaterialTheme.typography.bodyMedium)
    }
}
