package com.sukashawarma.superapp.core.ui.kaca

import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Penghubung antara isi layar (di belakang) dan kapsul kaca (di depan).
 *
 * Kaca yang sungguhan harus memperlihatkan apa yang ada di belakangnya. Compose 1.6
 * tidak punya API untuk "membaca latar", jadi arahnya dibalik: isi layar merekam
 * dirinya ke sebuah [RenderNode], lalu — masih di lintasan gambar isi itu sendiri —
 * menggambar salinan yang diburamkan tepat di bawah setiap panel kaca. Kapsul yang tembus
 * pandang kemudian duduk di atasnya. Salinan itu tidak sekadar diburamkan tetapi
 * dibiaskan: diperbesar di tengah dan diperkecil di tepi, seperti tetes air. Karena semuanya terjadi di satu lintasan gambar,
 * blurnya ikut bergerak di frame yang sama saat daftar digulir, tanpa tertinggal.
 *
 * Butuh Android 12 (API 31) untuk [RenderEffect]. Di bawah itu, atau bila perekaman
 * gagal karena alasan apa pun, [aktif] menjadi false dan kapsul kembali ke kaca susu
 * yang lebih pekat supaya labelnya tetap terbaca.
 */
@Stable
class LatarKaca {
    /** Panel kaca yang terdaftar (kapsul tab, kartu, pil), dalam koordinat root. */
    internal val panel = mutableStateMapOf<Any, PanelKaca>()
    internal var asalIsi by mutableStateOf(Offset.Zero)
    /** Batas tetes kaca tab aktif dalam koordinat root; kosong bila tidak ada tab aktif. */
    internal var lensa by mutableStateOf(Rect.Zero)
    var aktif by mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        internal set

    internal var nodeIsi: Any? = null
    /** Sepasang node buram per panel ([NodeKaca]) — ukurannya berbeda-beda. */
    internal val nodeKaca = HashMap<Any, Any>()
}

/** Latar kaca lokal, mis. untuk kartu yang mengambang di atas preview kamera. */
@Composable
fun rememberLatarKaca(): LatarKaca = remember { LatarKaca() }

/**
 * Satu bidang kaca di atas sebuah [sumberKaca].
 *
 * [sudut] null berarti kapsul (sudut = setengah tinggi). [perbesarX]/[perbesarY]
 * adalah pembesaran di tengah lensa — nilai bawaannya sama dengan tab bar, supaya
 * setiap kaca di aplikasi membias dengan cara yang sama.
 */
@Immutable
internal data class PanelKaca(
    val batas: Rect,
    val sudut: Float,
    val perbesarX: Float,
    val perbesarY: Float,
)

/**
 * Dipasang di wadah isi yang akan terlihat DI BELAKANG kaca. Menggambar isi seperti
 * biasa, ditambah salinan terbias di bawah setiap panel yang terdaftar di [latar].
 *
 * Panel kaca harus berada di LUAR wadah ini (saudara di atasnya, bukan anaknya):
 * isi wadah direkam utuh, dan kaca yang ikut terekam akan membiaskan dirinya sendiri.
 */
fun Modifier.sumberKaca(latar: LatarKaca): Modifier = this
    .onGloballyPositioned { latar.asalIsi = it.positionInRoot() }
    .drawWithContent {
        if (!latar.aktif || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            drawContent()
            return@drawWithContent
        }
        val ok = runCatching { gambarDenganKaca(latar) }
            .onFailure { android.util.Log.w("KacaCair", "blur latar dimatikan", it) }
            .getOrDefault(false)
        if (!ok) {
            latar.aktif = false
            drawContent()
        }
    }

/** @return false bila isi belum tergambar sama sekali dan pemanggil harus menggambarnya. */
@RequiresApi(Build.VERSION_CODES.S)
private fun ContentDrawScope.gambarDenganKaca(latar: LatarKaca): Boolean {
    val kanvas = drawContext.canvas.nativeCanvas
    // Perangkat lunak (mis. tangkapan pratinjau) tidak bisa menggambar RenderNode.
    if (!kanvas.isHardwareAccelerated || size.width < 1f || size.height < 1f) return false

    val isi = (latar.nodeIsi as? RenderNode) ?: RenderNode("isiKaca").also { latar.nodeIsi = it }
    isi.setPosition(0, 0, size.width.toInt(), size.height.toInt())
    val rekamIsi = isi.beginRecording()
    try {
        draw(this, layoutDirection, androidx.compose.ui.graphics.Canvas(rekamIsi), size) {
            this@gambarDenganKaca.drawContent()
        }
    } finally {
        isi.endRecording()
    }
    kanvas.drawRenderNode(isi)

    // Node milik panel yang sudah hilang dibuang, supaya peta tidak tumbuh terus.
    latar.nodeKaca.keys.retainAll(latar.panel.keys)
    val l = latar.lensa.translate(-latar.asalIsi)
    val adaLensa = l.width > 1f && l.height > 1f
    var lensaTergambar = false
    for ((kunci, p) in latar.panel) {
        val b = p.batas.translate(-latar.asalIsi)
        if (b.width < 1f || b.height < 1f) continue
        // Panel di luar isi (mis. saat isi belum terukur) tidak perlu kaca.
        if (b.bottom <= 0f || b.top >= size.height || b.right <= 0f || b.left >= size.width) continue
        val node = (latar.nodeKaca[kunci] as? NodeKaca) ?: NodeKaca().also { latar.nodeKaca[kunci] = it }
        node.rekam(this, isi, b)
        gambarPanel(node.hasil, b, p.sudut.coerceAtMost(b.minDimension / 2), p.perbesarX, p.perbesarY)

        // Tetes kaca tab aktif selalu berada di dalam kapsulnya, jadi ia memakai ulang
        // hasil blur kapsul itu — tidak perlu blur kedua untuk area yang sama.
        if (adaLensa && !lensaTergambar && b.contains(l.center)) {
            clipPath(bulat(l, l.height / 2)) { gambarKaca(node.hasil, l, 1.32f, 1.55f) }
            lensaTergambar = true
        }
    }
    return true
}

/**
 * Sepasang RenderNode untuk satu panel.
 *
 * [kaca] membawa efek blur+saturasi; [hasil] membungkusnya sebagai lapisan
 * komposit. Pembungkus ini penting untuk performa: RenderEffect diterapkan ulang
 * SETIAP kali node-nya digambar, sedangkan lensa menggambar isi panel belasan kali
 * per frame (satu kali per pita). Dengan lapisan komposit, blur dihitung sekali saat
 * lapisan diperbarui, lalu setiap pita hanya menyalin tekstur yang sudah jadi.
 * Tanpa ini, preview kamera 30 fps di bawah tiga panel membuat GPU menghitung blur
 * ~90 kali per frame dan layar Absensi turun ke ~14 fps.
 */
@RequiresApi(Build.VERSION_CODES.S)
private class NodeKaca {
    val kaca = RenderNode("kaca").apply {
        val radius = 2.5f * android.content.res.Resources.getSystem().displayMetrics.density
        val saturasi = ColorMatrix().apply { setSaturation(1.35f) }
        setRenderEffect(
            RenderEffect.createChainEffect(
                RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(saturasi)),
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
            ),
        )
    }
    val hasil = RenderNode("kacaHasil").apply { setUseCompositingLayer(true, null) }

    /**
     * Merekam isi di sekitar [b]. Direkam sedikit lebih lebar dari panel: tepi lensa
     * yang memperkecil isi memperlihatkan hingga ~18dp di luar panel (pil terpendek),
     * ditambah radius blur. Area sekecil mungkin karena biaya blur sebanding luasnya.
     */
    fun rekam(scope: DrawScope, isi: RenderNode, b: Rect) {
        val tepi = with(scope) { 24.dp.toPx() }
        val kiri = (b.left - tepi).toInt()
        val atas = (b.top - tepi).toInt()
        val lebar = (b.width + tepi * 2).toInt()
        val tinggi = (b.height + tepi * 2).toInt()

        kaca.setPosition(0, 0, lebar, tinggi)
        val rk = kaca.beginRecording()
        try {
            rk.translate(-kiri.toFloat(), -atas.toFloat())
            rk.drawRenderNode(isi)
        } finally {
            kaca.endRecording()
        }

        hasil.setPosition(kiri, atas, kiri + lebar, atas + tinggi)
        val rh = hasil.beginRecording()
        try {
            rh.drawRenderNode(kaca)
        } finally {
            hasil.endRecording()
        }
    }
}

/** Menggambar [hasil] di tempatnya, diskalakan dari titik tengah [b]. */
@RequiresApi(Build.VERSION_CODES.S)
private fun DrawScope.gambarKaca(hasil: RenderNode, b: Rect, sx: Float, sy: Float) = drawIntoCanvas {
    val c = it.nativeCanvas
    c.save()
    c.scale(sx, sy, b.center.x, b.center.y)
    // RenderNode diletakkan lewat setPosition, jadi cukup digambar di titik asal.
    c.drawRenderNode(hasil)
    c.restore()
}

private fun bulat(r: Rect, sudut: Float) = Path().apply {
    addRoundRect(RoundRect(r, CornerRadius(sudut.coerceAtLeast(0f))))
}

/**
 * Lensa cembung dari potongan-potongan: tepi paling luar MEMPERKECIL isi
 * (memperlihatkan yang di luar panel, seperti tepi tetes air yang membengkokkan
 * cahaya), lalu makin ke dalam makin memperbesar sampai puncaknya di tengah.
 *
 * Pengecilan tepi dihitung dalam dp, bukan rasio, supaya panel kecil maupun lebar
 * menunjukkan pita bias yang sama tebalnya.
 */
@RequiresApi(Build.VERSION_CODES.S)
private fun DrawScope.gambarPanel(
    hasil: RenderNode,
    b: Rect,
    sudut: Float,
    sxDalam: Float,
    syDalam: Float,
) {
    val geser = 10.dp.toPx()
    val sxLuar = (1f - geser / (b.width / 2)).coerceIn(0.5f, 1f)
    val syLuar = (1f - geser / (b.height / 2)).coerceIn(0.5f, 1f)
    val kedalaman = minOf(18.dp.toPx(), b.minDimension / 2 * 0.9f)
    // Pita dibuat setipis ~0,6dp. Dengan pita tebal, skala dua pita yang bersebelahan
    // cukup berbeda sehingga isi "melompat" di batasnya — terlihat sebagai garis
    // lengkung tak rapi di ujung kiri-kanan panel yang lebar. Setipis ini lompatannya
    // lebih kecil dari radius blur, jadi batas pita tidak lagi terlihat.
    val potongan = (kedalaman / 0.6.dp.toPx()).toInt().coerceIn(12, 32)
    // Tiap pita hanya mengisi CINCIN-nya sendiri (bukan seluruh bagian dalam panel),
    // jadi setiap piksel digambar sekitar sekali, bukan sampai puluhan kali. Lubang
    // cincin disisip sedikit lebih dalam supaya pita bertumpuk tipis — tanpa itu
    // antialias clip meninggalkan celah rambut yang memperlihatkan isi tanpa bias.
    val tumpang = 0.75f
    for (k in 0..potongan) {
        val t = k / potongan.toFloat()
        // Profil lengkung smoothstep: landai di tepi DAN di tengah, sehingga pita
        // terluar menyatu dengan bingkai dan pita terdalam menyatu dengan badan
        // lensa tanpa sambungan.
        val u = t * t * (3f - 2f * t)
        val sx = sxLuar + (sxDalam - sxLuar) * u
        val sy = syLuar + (syDalam - syLuar) * u
        val d = kedalaman * t
        val luar = b.deflate(d)
        if (luar.width <= 0f || luar.height <= 0f) break
        val jalur = bulat(luar, sudut - d)
        if (k < potongan) {
            val dBerikut = kedalaman * (k + 1) / potongan + tumpang
            val lubang = b.deflate(dBerikut)
            if (lubang.width > 0f && lubang.height > 0f) {
                jalur.fillType = PathFillType.EvenOdd
                jalur.addRoundRect(RoundRect(lubang, CornerRadius((sudut - dBerikut).coerceAtLeast(0f))))
            }
        }
        clipPath(jalur) { gambarKaca(hasil, b, sx, sy) }
    }
}

/**
 * Bidang "liquid glass" dengan bentuk bebas — tampilan dan pembiasan yang sama persis
 * dengan kapsul [BilahTabKaca], untuk kartu, pil, atau banner yang mengambang di atas
 * isi.
 *
 * Syarat: [latar] yang sama dipasang dengan [sumberKaca] pada wadah isi yang ada di
 * BELAKANG panel ini (saudara, bukan induk). Tanpa latar (null) panel tetap tampil,
 * hanya tanpa pembiasan.
 *
 * @param sudut radius sudut; null = kapsul penuh.
 * @param rona kepekatan putih badan kaca. Bawaan 10% (sama dengan tab bar); naikkan
 *   hanya bila latar di belakangnya sangat ramai dan teks di atas kaca sulit terbaca.
 */
fun Modifier.panelKaca(
    latar: LatarKaca?,
    sudut: Dp? = null,
    perbesarX: Float = 1.16f,
    perbesarY: Float = 1.38f,
    rona: Float = 0.10f,
    bayangan: Boolean = true,
): Modifier = composed {
    val kunci = remember { Any() }
    val kepadatan = LocalDensity.current
    DisposableEffect(latar, kunci) {
        onDispose { latar?.panel?.remove(kunci) }
    }
    val tembus = latar?.aktif == true
    val bentuk = if (sudut == null) CircleShape else RoundedCornerShape(sudut)
    this
        .then(
            if (latar == null) Modifier
            else Modifier.onGloballyPositioned {
                val batas = it.boundsInRoot()
                val r = with(kepadatan) { sudut?.toPx() } ?: (batas.height / 2)
                val baru = PanelKaca(batas, r, perbesarX, perbesarY)
                if (latar.panel[kunci] != baru) latar.panel[kunci] = baru
            },
        )
        .then(if (bayangan) Modifier.bayanganLembut(sudut) else Modifier)
        .clip(bentuk)
        .drawBehind {
            val r = (sudut?.toPx() ?: (size.height / 2)).coerceAtMost(size.minDimension / 2)
            drawRoundRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (tembus) rona else 0.88f),
                    1f to Color.White.copy(alpha = if (tembus) rona * 0.3f else 0.80f),
                ),
                cornerRadius = CornerRadius(r),
            )
            // Cahaya lembut di sisi dalam — beberapa garis tipis yang makin pudar ke
            // tengah, memberi kesan kaca yang tebal dan melengkung.
            val langkah = 1.dp.toPx()
            for (k in 0 until 8) {
                val sisip = langkah * (k + 0.5f)
                val a = 0.28f * (1f - k / 8f)
                drawRoundRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = a),
                        0.5f to Color.White.copy(alpha = a * 0.15f),
                        1f to Color.White.copy(alpha = a * 0.75f),
                    ),
                    topLeft = Offset(sisip, sisip),
                    size = Size(size.width - sisip * 2, size.height - sisip * 2),
                    cornerRadius = CornerRadius((r - sisip).coerceAtLeast(0f)),
                    style = Stroke(width = langkah),
                )
            }
            // Kilau spekular di tepi atas dan pantulan di tepi bawah. Keduanya digambar
            // sejajar bingkai (sebentuk dengan panel, disisip sedikit), bukan sebagai
            // kotak yang lebih sempit: kotak sempit meninggalkan sisi kiri-kanan yang
            // melengkung di dalam kaca — tampak sebagai garis tak rapi di ujung panel.
            val sisipKilau = 1.5.dp.toPx()
            val ukuranKilau = Size(size.width - sisipKilau * 2, size.height - sisipKilau * 2)
            val sudutKilau = CornerRadius((r - sisipKilau).coerceAtLeast(0f))
            drawRoundRect(
                Brush.verticalGradient(
                    0f to Color.White,
                    0.25f to Color.White.copy(alpha = 0f),
                    endY = size.height,
                ),
                topLeft = Offset(sisipKilau, sisipKilau),
                size = ukuranKilau,
                cornerRadius = sudutKilau,
                style = Stroke(width = 1.6.dp.toPx()),
            )
            drawRoundRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0f),
                    1f to Color.White.copy(alpha = 0.75f),
                    startY = size.height * 0.55f,
                    endY = size.height,
                ),
                topLeft = Offset(sisipKilau, sisipKilau),
                size = ukuranKilau,
                cornerRadius = sudutKilau,
                style = Stroke(width = 1.3.dp.toPx()),
            )
        }
        // Bingkai ganda: garis gelap sangat tipis di luar supaya tepi kaca tetap
        // terbaca di atas kartu putih, lalu garis putih terang di dalamnya.
        .border(0.5.dp, Color.Black.copy(alpha = 0.10f), bentuk)
        .border(
            1.5.dp,
            Brush.linearGradient(
                0f to Color.White,
                0.35f to Color.White.copy(alpha = 0.55f),
                0.65f to Color.White.copy(alpha = 0.50f),
                1f to Color.White,
            ),
            bentuk,
        )
}

/**
 * Permukaan kapsul tab bar — [panelKaca] berbentuk kapsul penuh dengan pembesaran
 * bawaan. Disimpan sebagai nama tersendiri karena dipakai [BilahTabKaca].
 */
fun Modifier.kacaCair(latar: LatarKaca?): Modifier = panelKaca(latar)

/**
 * Bayangan jatuh yang hanya digambar DI LUAR panel.
 *
 * Elevation bawaan tidak dipakai: bayangan platform ikut tergambar di bawah bidang
 * yang tembus pandang, sehingga kaca tampak kelabu kotor dari dalam.
 */
private fun Modifier.bayanganLembut(sudut: Dp?): Modifier = drawBehind {
    val r = (sudut?.toPx() ?: (size.height / 2)).coerceAtMost(size.minDimension / 2)
    val luar = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(r))) }
    val cat = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0xFF0F172A).copy(alpha = 0.16f).toArgb()
        maskFilter = BlurMaskFilter(16.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    clipPath(luar, ClipOp.Difference) {
        drawIntoCanvas {
            it.nativeCanvas.drawRoundRect(
                0f, 7.dp.toPx(), size.width, size.height + 7.dp.toPx(), r, r, cat,
            )
        }
    }
}

/**
 * Tetes kaca di bawah tab terpilih: lebih terang dari badan kapsul, dengan bingkai
 * dan kilaunya sendiri. Isi di bawahnya diperbesar lebih kuat oleh [sumberKaca]
 * lewat [LatarKaca.lensa].
 */
fun Modifier.lensaKaca(latar: LatarKaca?): Modifier = this
    .then(if (latar != null) Modifier.onGloballyPositioned { latar.lensa = it.boundsInRoot() } else Modifier)
    .clip(CircleShape)
    .drawBehind {
        drawRoundRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.22f),
                1f to Color.White.copy(alpha = 0.08f),
            ),
            cornerRadius = CornerRadius(size.height / 2),
        )
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.45f),
                0.35f to Color.White.copy(alpha = 0f),
                endY = size.height,
            ),
        )
        drawRect(
            Brush.verticalGradient(
                0.72f to Color.Transparent,
                1f to Color.White.copy(alpha = 0.55f),
                endY = size.height,
            ),
        )
    }
    .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
    .border(
        1.2.dp,
        Brush.verticalGradient(
            0f to Color.White,
            0.5f to Color.White.copy(alpha = 0.40f),
            1f to Color.White.copy(alpha = 0.85f),
        ),
        CircleShape,
    )
