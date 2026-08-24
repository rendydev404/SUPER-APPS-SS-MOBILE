package com.sukashawarma.superapp.presentation.absensi.profil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilScreen(onExit: () -> Unit, viewModel: ProfilViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showNewPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.result) {
        if (state.result is PasswordChangeResult.Success) {
            newPassword = ""
            confirmPassword = ""
        }
    }

    Scaffold(
        containerColor = SukaSurface,
        topBar = {
            Surface(color = SukaSurface) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("Profile", color = SukaOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            ProfileIdentity(state.staff)
            PasswordCard(
                state = state,
                newPassword = newPassword,
                confirmPassword = confirmPassword,
                showNewPassword = showNewPassword,
                showConfirmation = showConfirmation,
                onNewPasswordChange = {
                    newPassword = it
                    viewModel.resetResult()
                },
                onConfirmationChange = {
                    confirmPassword = it
                    viewModel.resetResult()
                },
                onToggleNewPassword = { showNewPassword = !showNewPassword },
                onToggleConfirmation = { showConfirmation = !showConfirmation },
                onSubmit = { viewModel.changePassword(newPassword, confirmPassword) },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileIdentity(staff: StaffProfile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(SukaOrange.copy(alpha = 0.09f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(SukaOrange.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                staff?.name?.take(1)?.uppercase() ?: "?",
                color = SukaInk,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
            )
            if (!staff?.refPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = staff?.refPhotoUrl,
                    contentDescription = "Foto profil ${staff?.name.orEmpty()}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                staff?.name ?: "-",
                color = SukaInk,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = SukaGray500, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    staff?.outletName ?: "Semua Outlet",
                    color = SukaGray500,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                color = SukaOrange.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = staff?.roleRaw?.replace('_', ' ')?.lowercase()
                        ?.replaceFirstChar { it.titlecase() } ?: "Staff",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    color = SukaInk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PasswordCard(
    state: ProfilUiState,
    newPassword: String,
    confirmPassword: String,
    showNewPassword: Boolean,
    showConfirmation: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onToggleNewPassword: () -> Unit,
    onToggleConfirmation: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SukaOrange.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = SukaOrange, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Ganti Password", color = SukaInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SukaGray200)

            PasswordField(
                label = "Password baru",
                placeholder = "Masukkan password baru",
                value = newPassword,
                visible = showNewPassword,
                onValueChange = onNewPasswordChange,
                onToggleVisibility = onToggleNewPassword,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))
            PasswordField(
                label = "Konfirmasi password baru",
                placeholder = "Ulangi password baru",
                value = confirmPassword,
                visible = showConfirmation,
                onValueChange = onConfirmationChange,
                onToggleVisibility = onToggleConfirmation,
                imeAction = ImeAction.Done,
            )

            AnimatedVisibility(
                visible = state.result !is PasswordChangeResult.Idle,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val success = state.result is PasswordChangeResult.Success
                val message = when (val result = state.result) {
                    is PasswordChangeResult.Failure -> result.message
                    is PasswordChangeResult.Success -> "Password berhasil diganti."
                    else -> ""
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    color = if (success) StatusEmerald.copy(alpha = 0.10f) else StatusRed.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (success) StatusEmerald else StatusRed,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(message, color = if (success) StatusEmerald else StatusRed, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                enabled = !state.changing && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SukaOrange,
                    contentColor = Color.White,
                    disabledContainerColor = SukaOrange.copy(alpha = 0.38f),
                    disabledContentColor = Color.White.copy(alpha = 0.82f),
                ),
            ) {
                if (state.changing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan Password Baru", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    placeholder: String,
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    imeAction: ImeAction,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = SukaInk, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = SukaInk,
                fontSize = 14.sp,
            ),
            placeholder = { Text(placeholder, color = SukaGray400, fontSize = 12.sp) },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (visible) "Sembunyikan password" else "Tampilkan password",
                        tint = SukaGray500,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
            singleLine = true,
            shape = RoundedCornerShape(7.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SukaOrange,
                unfocusedBorderColor = SukaGray200,
                cursorColor = SukaOrange,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
        )
    }
}
