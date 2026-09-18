package com.sukashawarma.superapp.feature.chat.ui.pribadi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.labelRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihKontakPribadiSheet(
    kontakList: List<AnggotaGrup>,
    memuat: Boolean,
    onPilihKontak: (AnggotaGrup) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(kontakList, query) {
        if (query.isBlank()) kontakList
        else {
            val q = query.trim().lowercase()
            kontakList.filter {
                it.nama.lowercase().contains(q) ||
                it.outlet?.lowercase()?.contains(q) == true ||
                it.role?.lowercase()?.contains(q) == true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF2F2F7),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chat Pribadi Baru",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "Pilih staf atau rekan kerja untuk mulai mengobrol",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF8E8E93))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kolom Pencarian iOS style
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari nama, cabang, atau jabatan...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedBorderColor = Color(0xFFE5E5EA),
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (memuat) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isNotBlank()) "Tidak ada kontak yang cocok dengan '$query'" else "Belum ada kontak tersedia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)
                    )
                }
            } else {
                Text(
                    text = "KONTAK (${filtered.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                ) {
                    items(filtered, key = { it.id }) { kontak ->
                        BarisKontak(kontak = kontak, onClick = { onPilihKontak(kontak) })
                        HorizontalDivider(
                            color = Color(0xFFF2F2F7),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 68.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisKontak(
    kontak: AnggotaGrup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AvatarStaf(
            path = kontak.avatar,
            nama = kontak.nama,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kontak.nama,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF1C1C1E)
            )
            val peran = labelRole(kontak.role)
            val outlet = kontak.outlet?.takeIf { it.isNotBlank() } ?: "Pusat"
            Text(
                text = "$peran • $outlet",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}
