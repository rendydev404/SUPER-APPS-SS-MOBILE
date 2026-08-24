package com.sukashawarma.superapp.presentation.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.presentation.theme.StatusRed
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray200
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaInk
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import kotlin.math.hypot
import kotlin.math.min

/**
 * Layar login — mengikuti desain Stitch "Login - Technical Pattern Background" (project
 * 16991912726833518585, screen 235b89fdd62b46f1a2260c24dd3a05c4): latar titik-titik teknis
 * yang memudar radial + dua gumpalan cahaya lembut, kartu bergaya glass, logo asli app
 * (bukan ikon generik lagi), dan field dengan toggle lihat/sembunyikan password.
 */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SukaCream),
        contentAlignment = Alignment.Center,
    ) {
        LoginBackdrop(modifier = Modifier.fillMaxSize())

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.86f),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "Suka Shawarma",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(104.dp),
                )

                Spacer(Modifier.height(16.dp))

                Text("Masuk", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = SukaInk)
                Spacer(Modifier.height(4.dp))
                Text("SSO Suka Shawarma", fontSize = 14.sp, color = SukaGray500)

                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = state.identifier,
                    onValueChange = viewModel::onIdentifierChange,
                    label = { Text("Email atau Username") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SukaOrange,
                        focusedLabelColor = SukaOrange,
                        cursorColor = SukaOrange,
                        unfocusedBorderColor = SukaGray200,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Kata Sandi") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Sembunyikan sandi" else "Lihat sandi",
                                tint = SukaGray500,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SukaOrange,
                        focusedLabelColor = SukaOrange,
                        cursorColor = SukaOrange,
                        unfocusedBorderColor = SukaGray200,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                    ),
                )

                AnimatedVisibility(visible = state.error != null, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            color = StatusRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = state.error.orEmpty(),
                                color = StatusRed,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                LoginSubmitButton(
                    loading = state.loading,
                    onClick = { viewModel.submit(onLoggedIn) },
                )
            }
        }
    }
}

@Composable
private fun LoginSubmitButton(loading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120), label = "loginBtnScale")

    Button(
        onClick = onClick,
        enabled = !loading,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SukaOrange,
            disabledContainerColor = SukaOrange.copy(alpha = 0.5f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Text("Masuk ke Sistem", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * Latar "technical pattern": grid titik halus yang memudar radial dari tengah (murni
 * trigonometri sederhana, bukan gambar/aset), ditambah dua gumpalan cahaya lembut yang
 * berdenyut pelan — memberi kedalaman tanpa efek blur berat yang mahal di HP kelas bawah.
 */
@Composable
private fun LoginBackdrop(modifier: Modifier = Modifier) {
    val breathe by rememberInfiniteTransition(label = "loginBackdropBreathe").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "loginBackdropBreatheAlpha",
    )

    Canvas(modifier = modifier) {
        // Gumpalan cahaya lembut — kanan-atas & kiri-bawah, meniru dua "blob" blur di desain.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SukaOrange.copy(alpha = 0.16f * breathe), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.06f),
                radius = size.width * 0.55f,
            ),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.92f, size.height * 0.06f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SukaOrange.copy(alpha = 0.14f * breathe), Color.Transparent),
                center = Offset(size.width * 0.06f, size.height * 0.92f),
                radius = size.width * 0.5f,
            ),
            radius = size.width * 0.5f,
            center = Offset(size.width * 0.06f, size.height * 0.92f),
        )

        // Grid titik teknis, alpha memudar radial dari tengah kanvas (mask-radial-fade).
        val spacing = 24.dp.toPx()
        val dotRadius = 1.4.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxDist = min(size.width, size.height) * 0.62f
        val dotColor = SukaInk

        var y = spacing / 2f
        while (y < size.height) {
            var x = spacing / 2f
            while (x < size.width) {
                val dist = hypot(x - cx, y - cy)
                val t = (1f - dist / maxDist).coerceIn(0f, 1f)
                if (t > 0f) {
                    drawCircle(color = dotColor.copy(alpha = 0.05f * t), radius = dotRadius, center = Offset(x, y))
                }
                x += spacing
            }
            y += spacing
        }
    }
}
