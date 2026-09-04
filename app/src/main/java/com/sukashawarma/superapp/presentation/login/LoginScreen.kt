package com.sukashawarma.superapp.presentation.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.presentation.theme.StatusRed
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaPrimary
import com.sukashawarma.superapp.presentation.theme.SukaPrimaryContainer
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerHigh
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerHighest
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerLowest
import kotlin.math.hypot
import kotlin.math.min

/**
 * Layar login. Warnanya murni token design system yang sama dipakai layar lain
 * (`presentation.theme`): permukaan terang `SukaSurface`, kartu putih, judul cokelat
 * `SukaPrimary`, aksen oranye `SukaPrimaryContainer`/`SukaOrange`. Tidak ada palet lokal
 * dan tidak ada warna aksen baru.
 *
 * Kesan premium dibangun dari hal yang bukan warna: ruang kosong yang lega, garis rambut
 * tipis, huruf kapital ber-tracking lebar untuk label, dan satu elemen bergerak saja
 * (kilau tombol). Seluruh dekorasi digambar lewat Canvas/Brush — tanpa aset gambar dan
 * tanpa blur runtime, jadi tetap ringan di HP kelas bawah yang dipakai kru outlet.
 */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LoginSystemBars()

    val submit = {
        keyboard?.hide()
        focusManager.clearFocus()
        viewModel.submit(onLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SukaSurface),
    ) {
        LoginBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark()

            Spacer(Modifier.height(18.dp))

            Text(
                "Selamat Datang",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = SukaPrimary,
            )

            Spacer(Modifier.height(7.dp))

            Text(
                "Masuk untuk melanjutkan ke sistem terpadu",
                fontSize = 13.5.sp,
                color = SukaOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )

            Spacer(Modifier.height(30.dp))

            LoginCard {
                FieldLabel("EMAIL ATAU USERNAME")
                Spacer(Modifier.height(9.dp))
                LoginTextField(
                    value = state.identifier,
                    onValueChange = viewModel::onIdentifierChange,
                    placeholder = "nama@sukashawarma.com",
                    leadingIcon = Icons.Filled.PersonOutline,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )

                Spacer(Modifier.height(18.dp))

                FieldLabel("KATA SANDI")
                Spacer(Modifier.height(9.dp))
                LoginTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    placeholder = "••••••••",
                    leadingIcon = Icons.Outlined.Lock,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Sembunyikan sandi" else "Lihat sandi",
                                tint = SukaOnSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(120)),
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        ErrorBanner(state.error.orEmpty())
                    }
                }

                Spacer(Modifier.height(24.dp))

                SubmitButton(loading = state.loading, onClick = submit)
            }

            Spacer(Modifier.height(26.dp))

            HairlineDivider()

            Spacer(Modifier.height(13.dp))

            Text(
                "PT SUKA PROFIT BERKAH",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = SukaOnSurfaceVariant.copy(alpha = 0.55f),
                letterSpacing = 2.5.sp,
            )
        }
    }
}

/* ------------------------------------------------------------- Brand mark */

/**
 * Logo app yang sama dengan ikon di launcher (`mipmap/ic_launcher`), diberi halo oranye
 * lembut yang bernapas pelan supaya terasa hidup tanpa perlu ornamen tambahan. Logo tidak
 * dipotong ke dalam lingkaran — wordmark dan topi chef-nya bagian dari gambar.
 */
@Composable
private fun BrandMark() {
    val glow by rememberInfiniteTransition(label = "brandGlow").animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brandGlowAlpha",
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(210.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SukaOrange.copy(alpha = 0.20f * glow), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f,
                ),
                radius = size.minDimension / 2f,
            )
        }

        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = "Logo Suka Shawarma",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(158.dp),
        )
    }
}

/* -------------------------------------------------------------- Form card */

@Composable
private fun LoginCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SukaSurfaceContainerLowest,
        border = BorderStroke(1.dp, SukaSurfaceContainerHigh),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            content = content,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = SukaOnSurfaceVariant,
        letterSpacing = 1.5.sp,
    )
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = SukaOnSurfaceVariant.copy(alpha = 0.55f), fontSize = 14.5.sp) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = SukaPrimaryContainer, modifier = Modifier.size(19.dp))
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = SukaOnSurface),
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SukaPrimaryContainer,
            unfocusedBorderColor = SukaSurfaceContainerHighest,
            focusedContainerColor = SukaSurfaceContainerLowest,
            unfocusedContainerColor = SukaSurface,
            cursorColor = SukaPrimaryContainer,
            focusedTextColor = SukaOnSurface,
            unfocusedTextColor = SukaOnSurface,
            selectionColors = TextSelectionColors(
                handleColor = SukaPrimaryContainer,
                backgroundColor = SukaOrange.copy(alpha = 0.3f),
            ),
        ),
    )
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StatusRed.copy(alpha = 0.07f))
            .border(1.dp, StatusRed.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = StatusRed, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text(message, color = StatusRed, fontSize = 12.5.sp, lineHeight = 17.sp)
    }
}

/* ---------------------------------------------------------- Submit button */

/** Satu-satunya elemen bergerak di dalam kartu — kilau tipis yang menyapu pelan, supaya
 *  mata langsung tahu ke mana harus menuju tanpa perlu warna tambahan. */
@Composable
private fun SubmitButton(loading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(120), label = "loginBtnScale")
    val sheen by rememberInfiniteTransition(label = "loginBtnSheen").animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "loginBtnSheenPos",
    )

    Surface(
        onClick = onClick,
        enabled = !loading,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        shadowElevation = if (loading) 0.dp else 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .graphicsLayer { alpha = if (loading) 0.8f else 1f },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SukaOrange, SukaPrimaryContainer))),
            contentAlignment = Alignment.Center,
        ) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                            start = Offset(w * sheen, 0f),
                            end = Offset(w * sheen + w * 0.32f, h),
                        )
                    )
            )

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(23.dp),
                    color = Color.White,
                    strokeWidth = 2.4.dp,
                )
            } else {
                Text(
                    "MASUK KE SISTEM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        Modifier
            .fillMaxWidth(0.5f)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, SukaSurfaceContainerHighest, Color.Transparent)
                )
            )
    )
}

/* --------------------------------------------------------------- Backdrop */

/**
 * Latar bertekstur di atas `SukaSurface`: dua cahaya oranye lembut yang bernapas pelan
 * plus kisi titik halus yang memudar radial — kedalaman tanpa mengubah warna dasar layar.
 */
@Composable
private fun LoginBackdrop(modifier: Modifier = Modifier) {
    val breathe by rememberInfiniteTransition(label = "loginBackdropBreathe").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "loginBackdropBreatheAlpha",
    )

    Canvas(modifier = modifier) {
        val glowTop = Offset(size.width * 0.9f, size.height * 0.07f)
        val glowTopRadius = size.width * 0.66f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SukaOrange.copy(alpha = 0.20f * breathe), Color.Transparent),
                center = glowTop,
                radius = glowTopRadius,
            ),
            radius = glowTopRadius,
            center = glowTop,
        )

        val glowBottom = Offset(size.width * 0.06f, size.height * 0.9f)
        val glowBottomRadius = size.width * 0.58f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SukaPrimaryContainer.copy(alpha = 0.14f * breathe), Color.Transparent),
                center = glowBottom,
                radius = glowBottomRadius,
            ),
            radius = glowBottomRadius,
            center = glowBottom,
        )

        // Kisi titik, alpha memudar radial dari tengah — tekstur yang baru terlihat kalau
        // diperhatikan, bukan pola yang berteriak.
        val spacing = 26.dp.toPx()
        val dotRadius = 1.2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height * 0.46f
        val maxDist = min(size.width, size.height) * 0.72f

        var y = spacing / 2f
        while (y < size.height) {
            var x = spacing / 2f
            while (x < size.width) {
                val t = (1f - hypot(x - cx, y - cy) / maxDist).coerceIn(0f, 1f)
                if (t > 0f) {
                    drawCircle(
                        color = SukaPrimary.copy(alpha = 0.055f * t),
                        radius = dotRadius,
                        center = Offset(x, y),
                    )
                }
                x += spacing
            }
            y += spacing
        }
    }
}

/* ------------------------------------------------------------ System bars */

/** Samakan system bar dengan permukaan layar (terang, ikon gelap), lalu kembalikan persis
 *  seperti semula saat keluar dari login. */
@Composable
private fun LoginSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val prevStatusColor = window.statusBarColor
            val prevNavColor = window.navigationBarColor
            val prevLightStatus = controller.isAppearanceLightStatusBars
            val prevLightNav = controller.isAppearanceLightNavigationBars

            window.statusBarColor = SukaSurface.toArgb()
            window.navigationBarColor = SukaSurface.toArgb()
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true

            onDispose {
                window.statusBarColor = prevStatusColor
                window.navigationBarColor = prevNavColor
                controller.isAppearanceLightStatusBars = prevLightStatus
                controller.isAppearanceLightNavigationBars = prevLightNav
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
