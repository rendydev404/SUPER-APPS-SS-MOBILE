package com.sukashawarma.superapp.core.ui.kaca

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ikon bergaya SF Symbols untuk modul Stok.
 *
 * Bukan salinan SF Symbols: lisensi Apple melarang memakainya di luar platform Apple,
 * jadi bentuknya digambar ulang di kisi 24 dengan idiom yang sama — garis bulat
 * setebal "regular", sudut membulat, dan varian `...Isi` untuk tab yang terpilih
 * seperti `house` / `house.fill` di tab bar iOS.
 *
 * Nama propertinya sengaja sama dengan `Icons.Default.*` yang digantikannya, supaya
 * perpindahan dari Material cukup mengganti awalan.
 */
object IkonIos {
    private const val TEBAL = 1.7f
    private const val TEBAL_AKTIF = 2.2f

    // ── Navigasi & aksi umum ──────────────────────────────────────────────────
    val ArrowBack by lazy { garis("chevron.backward", "M15 4.5L7.5 12l7.5 7.5", tebal = 2.1f) }
    val ArrowForward by lazy { garis("arrow.right", "M4.5 12h15M13.5 6l6 6-6 6") }
    val ArrowUpward by lazy { garis("arrow.up", "M12 19.5v-15M6 10.5l6-6 6 6") }
    val ArrowDownward by lazy { garis("arrow.down", "M12 4.5v15M6 13.5l6 6 6-6") }
    val ArrowDropDown by lazy { garis("chevron.up.chevron.down", "M8.5 9.5L12 6l3.5 3.5M8.5 14.5L12 18l3.5-3.5") }
    val ExpandMore by lazy { garis("chevron.down", "M6 9.5l6 6 6-6") }
    val ExpandLess by lazy { garis("chevron.up", "M6 14.5l6-6 6 6") }
    val ChevronRight by lazy { garis("chevron.right", "M9.5 5.5L16 12l-6.5 6.5") }
    val Close by lazy { garis("xmark", "M6.5 6.5l11 11M17.5 6.5l-11 11") }
    val Clear get() = Close
    val Add by lazy { garis("plus", "M12 5v14M5 12h14") }
    val Remove by lazy { garis("minus", "M5 12h14") }
    val Check by lazy { garis("checkmark", "M5 12.5l4.5 4.5L19 7.5") }
    val Search by lazy { garis("magnifyingglass", lingkaran(10.5f, 10.5f, 6.5f), "M15.3 15.3L20 20") }
    val MoreHoriz by lazy {
        isi("ellipsis", lingkaran(6.5f, 12f, 1.5f) + lingkaran(12f, 12f, 1.5f) + lingkaran(17.5f, 12f, 1.5f))
    }
    val Refresh by lazy {
        ImageVector.Builder("arrow.clockwise", 24.dp, 24.dp, 24f, 24f)
            .garisKe("M18.55 7.41A8 8 0 1 1 11.3 4.03")
            .isiKe("M11.6 2.2L14.8 4.05L11.6 5.9Z", juga = true)
            .build()
    }
    val Restore by lazy { garis("arrow.uturn.backward", "M9 14.5L4.5 10 9 5.5", "M4.5 10h10a5 5 0 0 1 0 10H11") }
    val History by lazy {
        garis(
            "clock.arrow.circlepath",
            "M4.5 12a7.5 7.5 0 1 0 2.2-5.3",
            "M6.7 3v3.7h3.7",
            "M12 8v4l2.8 1.8",
        )
    }
    val SwapHoriz by lazy {
        garis("arrow.left.arrow.right", "M4 8h15M15.5 4.5L19 8l-3.5 3.5", "M20 16H5M8.5 12.5L5 16l3.5 3.5")
    }
    val ImportExport by lazy {
        garis("arrow.up.arrow.down", "M8 19V5M4.5 8.5L8 5l3.5 3.5", "M16 5v14M12.5 15.5L16 19l3.5-3.5")
    }
    val Download by lazy {
        garis(
            "square.and.arrow.down",
            "M12 3.5v11M7.5 10l4.5 4.5 4.5-4.5",
            "M4.5 15v3.5A1.5 1.5 0 0 0 6 20h12a1.5 1.5 0 0 0 1.5-1.5V15",
        )
    }
    val Tune by lazy {
        garis(
            "slider.horizontal.3",
            "M4 7h9M17 7h3", lingkaran(15f, 7f, 2f),
            "M4 12h3M11 12h9", lingkaran(9f, 12f, 2f),
            "M4 17h11M19 17h1", lingkaran(17f, 17f, 2f),
        )
    }

    // ── Keadaan & status ──────────────────────────────────────────────────────
    val CheckCircle by lazy {
        // Centang dilubangi dari lingkaran (even-odd), bukan digambar putih di atasnya:
        // tint Icon mewarnai seluruh vektor sehingga garis putih ikut jadi oranye.
        isi(
            "checkmark.circle.fill",
            lingkaran(12f, 12f, 9.5f) +
                "M8.365 11.609L10.567 13.744L15.602 8.442A1.1 1.1 0 0 1 17.198 9.958" +
                "L10.63 16.856L6.835 13.191A1.1 1.1 0 0 1 8.365 11.609Z",
        )
    }
    val ErrorOutline by lazy {
        ImageVector.Builder("exclamationmark.circle", 24.dp, 24.dp, 24f, 24f)
            .garisKe(lingkaran(12f, 12f, 9f), "M12 7.5v5.5")
            .isiKe(lingkaran(12f, 16.55f, 1.15f))
            .build()
    }
    val WarningAmber by lazy {
        ImageVector.Builder("exclamationmark.triangle", 24.dp, 24.dp, 24f, 24f)
            .garisKe("M10.3 4.2a2 2 0 0 1 3.4 0l7.5 13a2 2 0 0 1-1.7 3H4.5a2 2 0 0 1-1.7-3Z", "M12 9v4.5")
            .isiKe(lingkaran(12f, 16.8f, 1.1f))
            .build()
    }
    val CloudOff by lazy {
        garis(
            "icloud.slash",
            "M7 18.5h10.5A3.5 3.5 0 0 0 18.1 11.55A5.5 5.5 0 0 0 7.6 9.6A4.5 4.5 0 0 0 7 18.5Z",
            "M4 4l16 16",
        )
    }
    val Lock by lazy {
        garis("lock", kotak(5f, 10.5f, 14f, 10f, 1.8f), "M8 10.5V7.5a4 4 0 0 1 8 0v3")
    }
    val Inbox by lazy {
        garis(
            "tray",
            "M3.5 13.5l2.4-7.2a1.5 1.5 0 0 1 1.4-1h9.4a1.5 1.5 0 0 1 1.4 1l2.4 7.2v5a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 3.5 18.5Z",
            "M3.5 13.5H8l1.2 2h5.6l1.2-2h4.5",
        )
    }
    val HourglassEmpty by lazy {
        garis(
            "hourglass",
            "M6.5 3.5h11M6.5 20.5h11",
            "M7.5 3.5c0 4 4.5 5.5 4.5 8.5s-4.5 4.5-4.5 8.5M16.5 3.5c0 4-4.5 5.5-4.5 8.5s4.5 4.5 4.5 8.5",
        )
    }
    val Schedule by lazy { garis("clock", lingkaran(12f, 12f, 9f), "M12 7v5l3.5 2") }
    val Visibility by lazy {
        garis(
            "eye",
            "M2.5 12c2.2-4.2 5.5-6.5 9.5-6.5s7.3 2.3 9.5 6.5c-2.2 4.2-5.5 6.5-9.5 6.5S4.7 16.2 2.5 12Z",
            lingkaran(12f, 12f, 3f),
        )
    }

    // ── Benda ─────────────────────────────────────────────────────────────────
    val Dashboard by lazy {
        garis("square.grid.2x2", *KISI_2X2)
    }
    val Assignment by lazy {
        garis(
            "list.clipboard",
            "M8.5 5H7a1.5 1.5 0 0 0-1.5 1.5v13A1.5 1.5 0 0 0 7 21h10a1.5 1.5 0 0 0 1.5-1.5v-13A1.5 1.5 0 0 0 17 5h-1.5",
            kotak(8.5f, 3f, 7f, 3.5f, 1.1f),
            "M11 10.5h4.5M11 14h4.5M11 17.5h4.5",
            titik(8.6f, 10.5f) + titik(8.6f, 14f) + titik(8.6f, 17.5f),
        )
    }
    val Description by lazy {
        garis(
            "doc.text",
            "M13.5 3.5H7A1.5 1.5 0 0 0 5.5 5v14A1.5 1.5 0 0 0 7 20.5h10a1.5 1.5 0 0 0 1.5-1.5V8.5Z",
            "M13.5 3.5V7A1.5 1.5 0 0 0 15 8.5h3.5",
            "M8.5 12.5h7M8.5 16h5",
        )
    }
    val FactCheck by lazy {
        garis(
            "checklist",
            "M3.5 6.8l1.4 1.4 2.6-2.8M3.5 11.8l1.4 1.4 2.6-2.8M3.5 16.8l1.4 1.4 2.6-2.8",
            "M11 7h9.5M11 12h9.5M11 17h9.5",
        )
    }
    val EditNote by lazy {
        garis(
            "square.and.pencil",
            "M11 4.5H6A1.5 1.5 0 0 0 4.5 6v12A1.5 1.5 0 0 0 6 19.5h12a1.5 1.5 0 0 0 1.5-1.5v-5",
            "M17.6 3.9a1.6 1.6 0 0 1 2.3 2.3L12.5 13.6l-3 .8.8-3Z",
        )
    }
    val MenuBook by lazy {
        garis(
            "book",
            "M12 6.5c-1.6-1.3-4.2-2-8-2v13c3.8 0 6.4.7 8 2c1.6-1.3 4.2-2 8-2v-13c-3.8 0-6.4.7-8 2Z",
            "M12 6.5v13",
        )
    }
    val LocalShipping by lazy {
        garis(
            "truck.box",
            "M5.2 17H3.5a.5.5 0 0 1-.5-.5V7a1 1 0 0 1 1-1h9.5a1 1 0 0 1 1 1v10H8.8",
            "M14.5 9h3.1a1 1 0 0 1 .83.45l2.4 3.6a1 1 0 0 1 .17.55V16.5a.5.5 0 0 1-.5.5h-1.7M14.5 17h.7",
            lingkaran(7f, 17f, 1.8f),
            lingkaran(17f, 17f, 1.8f),
        )
    }
    val Inventory2 by lazy {
        garis("shippingbox", "M12 3l8 4v10l-8 4-8-4V7Z", "M4 7l8 4 8-4M12 11v10", "M8 5l8 4")
    }
    val Delete by lazy {
        garis(
            "trash",
            "M4 6.5h16",
            "M9.5 6.5V5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v1.5",
            "M6 6.5l.9 12.1A1.5 1.5 0 0 0 8.4 20h7.2a1.5 1.5 0 0 0 1.5-1.4L18 6.5",
            "M10 10.5v6M14 10.5v6",
        )
    }
    val DeleteSweep get() = Delete
    val Scale by lazy {
        garis(
            "scalemass",
            "M6.5 8h11a1 1 0 0 1 1 .8l1.6 10a1 1 0 0 1-1 1.2H4.9a1 1 0 0 1-1-1.2l1.6-10a1 1 0 0 1 1-.8Z",
            lingkaran(12f, 5.75f, 2.25f),
        )
    }
    val Receipt by lazy {
        garis(
            "receipt",
            "M6 3.5h12v17l-2-1.3-2 1.3-2-1.3-2 1.3-2-1.3-2 1.3Z",
            "M9 8h6M9 11.5h6M9 15h4",
        )
    }
    val ReceiptLong get() = Receipt
    val Image by lazy {
        garis(
            "photo",
            kotak(3f, 5f, 18f, 14f, 1.8f),
            "M3.5 16.5l5-5 4 4 2.5-2.5 5.5 5.5",
            lingkaran(15.5f, 9f, 1.5f),
        )
    }
    val PhotoCamera by lazy {
        garis(
            "camera",
            "M4.5 7.5h2.6l1.4-2.1a1 1 0 0 1 .83-.44h5.34a1 1 0 0 1 .83.44l1.4 2.1h2.6A1.5 1.5 0 0 1 21 9v8.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5V9a1.5 1.5 0 0 1 1.5-1.5Z",
            lingkaran(12f, 13f, 3.5f),
        )
    }
    val CameraAlt get() = PhotoCamera
    val FolderOpen by lazy {
        garis(
            "folder",
            "M3.5 7A1.5 1.5 0 0 1 5 5.5h4l2 2h8A1.5 1.5 0 0 1 20.5 9v8.5A1.5 1.5 0 0 1 19 19H5a1.5 1.5 0 0 1-1.5-1.5Z",
            "M3.5 10h17",
        )
    }
    val Person by lazy {
        garis("person", lingkaran(12f, 8f, 3.8f), "M4.5 20.5c.8-3.8 3.8-6 7.5-6s6.7 2.2 7.5 6")
    }
    val LocationOn by lazy {
        garis(
            "mappin",
            "M12 21s-6.5-6.2-6.5-11.2a6.5 6.5 0 0 1 13 0C18.5 14.8 12 21 12 21Z",
            lingkaran(12f, 9.8f, 2.3f),
        )
    }
    val Eco by lazy {
        garis(
            "leaf",
            "M5.5 18.5C4 10 9 4.5 19.5 4.5c.5 9.5-4 15-10.5 15c-1.3 0-2.5-.3-3.5-1Z",
            "M5 20c3-5 6.5-8.5 10-11",
        )
    }
    val Storefront by lazy {
        garis(
            "storefront",
            "M3.5 9l1.6-4.3a1 1 0 0 1 .94-.7h11.9a1 1 0 0 1 .94.7L20.5 9",
            "M3.5 9a2.83 2.83 0 0 0 5.67 0a2.83 2.83 0 0 0 5.66 0a2.83 2.83 0 0 0 5.67 0",
            "M5 11.5V19a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-7.5",
            "M10 20v-4.5h4V20",
        )
    }
    val ShoppingCart by lazy {
        garis(
            "cart",
            "M3 4h2.2l2.2 10.6a1.5 1.5 0 0 0 1.47 1.2h8.3a1.5 1.5 0 0 0 1.46-1.15L20.5 8H6",
            lingkaran(9.5f, 19.3f, 1.3f),
            lingkaran(17f, 19.3f, 1.3f),
        )
    }
    val ShoppingBag by lazy {
        garis(
            "bag",
            "M5.5 8h13l-.8 11.1a1.5 1.5 0 0 1-1.5 1.4H7.8a1.5 1.5 0 0 1-1.5-1.4Z",
            "M9 10V7a3 3 0 0 1 6 0v3",
        )
    }
    val Sell by lazy {
        garis(
            "tag",
            "M4.2 3.8h6.3a1 1 0 0 1 .7.3l8.7 8.7a1.5 1.5 0 0 1 0 2.1l-5.3 5.3a1.5 1.5 0 0 1-2.1 0L3.8 11.5a1 1 0 0 1-.3-.7V4.5a.7.7 0 0 1 .7-.7Z",
            lingkaran(7.8f, 8f, 1.3f),
        )
    }
    val Savings by lazy {
        garis(
            "banknote",
            kotak(2.5f, 6.5f, 19f, 11f, 1.5f),
            lingkaran(12f, 12f, 2.5f),
            "M5.5 12h.5M18 12h.5",
        )
    }
    val AccountBalanceWallet by lazy {
        garis(
            "wallet",
            kotak(3f, 6.5f, 18f, 13f, 2f),
            "M5 6.5l9.2-2.7a1.5 1.5 0 0 1 1.9 1.1l.3 1.6",
            "M21 11h-4a2 2 0 0 0 0 4h4",
            titik(17f, 13f),
        )
    }
    val Calculate by lazy {
        garis(
            "plus.forwardslash.minus",
            kotak(5.5f, 3f, 13f, 18f, 1.8f),
            "M8.5 6.5h7v3h-7Z",
            titik(9f, 13f) + titik(12f, 13f) + titik(15f, 13f) +
                titik(9f, 16.5f) + titik(12f, 16.5f) + titik(15f, 16.5f),
        )
    }
    val BarChart by lazy {
        garis(
            "chart.bar",
            kotak(4.5f, 12f, 3.5f, 8f, 1f),
            kotak(10.25f, 5f, 3.5f, 15f, 1f),
            kotak(16f, 9f, 3.5f, 11f, 1f),
        )
    }
    val TrendingUp by lazy {
        garis(
            "chart.line.uptrend.xyaxis",
            "M4 4v15.5a.5.5 0 0 0 .5.5H20",
            "M7.5 15l4-4.5 3 3L20 7.5",
        )
    }

    // ── Tab & menu modul lain (Absensi, Distribusi, Leader, Manager) ──────────
    val Home by lazy {
        garis(
            "house",
            "M3.5 11L12 3.8l8.5 7.2",
            "M5.5 9.5V19a1 1 0 0 0 1 1h3.5v-5.5h4V20h3.5a1 1 0 0 0 1-1V9.5",
        )
    }
    val Checklist get() = FactCheck
    val Rule get() = FactCheck
    val ListBullet by lazy {
        garis(
            "list.bullet",
            "M9 7h11M9 12h11M9 17h11",
            titik(4.8f, 7f) + titik(4.8f, 12f) + titik(4.8f, 17f),
            tebal = TEBAL,
        )
    }
    val PersonAdd by lazy {
        garis(
            "person.badge.plus",
            lingkaran(10f, 8f, 3.6f),
            "M3.5 20c.7-3.6 3.4-5.6 6.5-5.6 1.6 0 3 .5 4.2 1.4",
            "M18.5 12v6M15.5 15h6",
        )
    }
    val Groups by lazy {
        garis(
            "person.3",
            lingkaran(12f, 8f, 3f),
            "M6.5 20c.5-3.2 2.8-5 5.5-5s5 1.8 5.5 5",
            lingkaran(5.5f, 9.5f, 2.2f),
            lingkaran(18.5f, 9.5f, 2.2f),
            "M2 18.5c.3-2 1.6-3.3 3.3-3.6M22 18.5c-.3-2-1.6-3.3-3.3-3.6",
        )
    }
    val Payments by lazy {
        garis("creditcard", kotak(2.5f, 5.5f, 19f, 13f, 2f), "M2.5 9.5h19", "M6 15h4")
    }
    val TrackChanges by lazy {
        garis("scope", lingkaran(12f, 12f, 9f), lingkaran(12f, 12f, 5f), titik(12f, 12f), tebal = TEBAL)
    }
    val ContentPasteSearch by lazy {
        garis(
            "doc.text.magnifyingglass",
            "M8.5 5H7a1.5 1.5 0 0 0-1.5 1.5v13A1.5 1.5 0 0 0 7 21h4M15.5 5H17a1.5 1.5 0 0 1 1.5 1.5V10",
            kotak(8.5f, 3f, 7f, 3.5f, 1.1f),
            lingkaran(15.5f, 15.5f, 3f),
            "M17.7 17.7L20.5 20.5",
        )
    }
    val Menu by lazy { garis("line.3.horizontal", "M4 7h16M4 12h16M4 17h16") }
    val QrCodeScanner by lazy {
        garis(
            "qrcode.viewfinder",
            "M4 8V5.5A1.5 1.5 0 0 1 5.5 4H8M16 4h2.5A1.5 1.5 0 0 1 20 5.5V8" +
                "M20 16v2.5a1.5 1.5 0 0 1-1.5 1.5H16M8 20H5.5A1.5 1.5 0 0 1 4 18.5V16",
            kotak(7.5f, 7.5f, 3.5f, 3.5f, 0.6f),
            kotak(13f, 7.5f, 3.5f, 3.5f, 0.6f),
            kotak(7.5f, 13f, 3.5f, 3.5f, 0.6f),
            "M13 13h1.5M16.5 13v1.5M13 16.5h3.5",
        )
    }
    val CalendarMonth by lazy {
        garis(
            "calendar",
            kotak(3.5f, 5f, 17f, 15.5f, 2f),
            "M3.5 10h17",
            "M8 3v4M16 3v4",
            titik(8f, 14f) + titik(12f, 14f) + titik(16f, 14f) + titik(8f, 17.5f) + titik(12f, 17.5f),
        )
    }
    val Settings by lazy { garis("gearshape", roda(), lingkaran(12f, 12f, 3f)) }

    /** Garis luar roda gigi delapan gigi, digambar dari sudut supaya giginya simetris. */
    private fun roda(): String {
        val sb = StringBuilder()
        fun titikKutub(r: Float, derajat: Float): String {
            val rad = Math.toRadians(derajat.toDouble())
            val x = 12f + r * kotlin.math.cos(rad).toFloat()
            val y = 12f + r * kotlin.math.sin(rad).toFloat()
            return "$x $y"
        }
        for (k in 0 until 8) {
            val a = k * 45f
            sb.append(if (k == 0) "M" else "L").append(titikKutub(7f, a - 22.5f + 5f))
            sb.append("L").append(titikKutub(9.2f, a - 9f))
            sb.append("L").append(titikKutub(9.2f, a + 9f))
            sb.append("L").append(titikKutub(7f, a + 22.5f - 5f))
        }
        return sb.append("Z").toString()
    }

    val HomeIsi by lazy {
        isi(
            "house.fill",
            "M12 3l9 7.8h-2.2V20a1 1 0 0 1-1 1H6.2a1 1 0 0 1-1-1v-9.2H3Z" +
                kotak(10f, 14.5f, 4f, 6.5f, 1f),
        )
    }

    // ── Varian terisi untuk tab terpilih ─────────────────────────────────────
    val DashboardIsi by lazy { isi("square.grid.2x2.fill", KISI_2X2.joinToString("")) }
    val AssignmentIsi by lazy {
        isi(
            "list.clipboard.fill",
            // Badan papan dengan takik di atas untuk penjepitnya, lalu tiga baris
            // dilubangi even-odd supaya tetap terbaca sebagai daftar.
            "M7 5h.8v1.3a1.8 1.8 0 0 0 1.8 1.8h4.8a1.8 1.8 0 0 0 1.8-1.8V5h.8a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" +
                kotak(8.8f, 2.8f, 6.4f, 4.3f, 1.2f) +
                kotak(8.5f, 10.6f, 7f, 1.6f, 0.8f) +
                kotak(8.5f, 13.8f, 7f, 1.6f, 0.8f) +
                kotak(8.5f, 17f, 4.5f, 1.6f, 0.8f),
        )
    }
    val DescriptionIsi by lazy {
        isi(
            "doc.text.fill",
            "M7 2.8h6v4.2a2 2 0 0 0 2 2h4.2V19a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4.8a2 2 0 0 1 2-2Z" +
                "M14.5 3.2l4.3 4.3H15a.5.5 0 0 1-.5-.5Z" +
                kotak(8f, 12f, 8f, 1.6f, 0.8f) +
                kotak(8f, 15.5f, 5.5f, 1.6f, 0.8f),
        )
    }
    val DeleteIsi by lazy {
        isi(
            "trash.fill",
            "M10.3 2.8h3.4a1.3 1.3 0 0 1 1.3 1.3v1.1h4.6a.9.9 0 0 1 0 1.8H4.4a.9.9 0 0 1 0-1.8H9V4.1a1.3 1.3 0 0 1 1.3-1.3Z" +
                "M5.6 8.4h12.8l-.85 11.2a1.6 1.6 0 0 1-1.6 1.5H8.05a1.6 1.6 0 0 1-1.6-1.5Z" +
                kotak(9.2f, 10.8f, 1.5f, 7.2f, 0.75f) +
                kotak(13.3f, 10.8f, 1.5f, 7.2f, 0.75f),
        )
    }
    val MenuBookIsi by lazy {
        isi(
            "book.fill",
            "M11.2 6.1C9.6 4.9 7 4.3 3.5 4.3v13.4c3.5 0 6.1.6 7.7 1.8Z" +
                "M12.8 6.1c1.6-1.2 4.2-1.8 7.7-1.8v13.4c-3.5 0-6.1.6-7.7 1.8Z",
        )
    }

    /**
     * Pasangan ikon untuk tab bar: varian `.fill` bila ada, dan bila tidak, bentuk
     * yang sama dengan garis lebih tebal — tab bar iOS pun menebalkan simbol yang
     * tidak punya varian isi.
     */
    fun aktif(ikon: ImageVector): ImageVector = when (ikon) {
        Dashboard -> DashboardIsi
        Home -> HomeIsi
        Assignment -> AssignmentIsi
        Description -> DescriptionIsi
        Delete -> DeleteIsi
        MenuBook -> MenuBookIsi
        else -> cacheTebal.getOrPut(ikon.name) { tebalkan(ikon) }
    }

    private val cacheTebal = mutableMapOf<String, ImageVector>()

    /** Salinan [ikon] dengan setiap garisnya ditebalkan. */
    private fun tebalkan(ikon: ImageVector): ImageVector {
        val b = ImageVector.Builder(ikon.name + ".bold", 24.dp, 24.dp, 24f, 24f)
        val akar = ikon.root
        for (i in 0 until akar.size) {
            val p = akar[i] as? androidx.compose.ui.graphics.vector.VectorPath ?: continue
            b.addPath(
                pathData = p.pathData,
                pathFillType = p.pathFillType,
                fill = p.fill,
                stroke = p.stroke,
                strokeLineWidth = if (p.stroke != null) TEBAL_AKTIF else p.strokeLineWidth,
                strokeLineCap = p.strokeLineCap,
                strokeLineJoin = p.strokeLineJoin,
            )
        }
        return b.build()
    }

    private val KISI_2X2 = arrayOf(
        kotak(4f, 4f, 6.5f, 6.5f, 1.8f),
        kotak(13.5f, 4f, 6.5f, 6.5f, 1.8f),
        kotak(4f, 13.5f, 6.5f, 6.5f, 1.8f),
        kotak(13.5f, 13.5f, 6.5f, 6.5f, 1.8f),
    )

    // ── Perakit ──────────────────────────────────────────────────────────────
    private fun garis(nama: String, vararg d: String, tebal: Float = TEBAL): ImageVector =
        ImageVector.Builder(nama, 24.dp, 24.dp, 24f, 24f).garisKe(*d, tebal = tebal).build()

    private fun isi(nama: String, d: String): ImageVector =
        ImageVector.Builder(nama, 24.dp, 24.dp, 24f, 24f).isiKe(d).build()

    private fun ImageVector.Builder.garisKe(vararg d: String, tebal: Float = TEBAL) = apply {
        d.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = tebal,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }

    /** Bidang terisi even-odd. [juga] ikut menggaris tepinya supaya sudutnya membulat. */
    private fun ImageVector.Builder.isiKe(d: String, juga: Boolean = false) = apply {
        addPath(
            pathData = addPathNodes(d),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black),
            stroke = if (juga) SolidColor(Color.Black) else null,
            strokeLineWidth = TEBAL,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    private fun lingkaran(cx: Float, cy: Float, r: Float): String =
        "M$cx ${cy - r}a$r $r 0 1 0 0 ${2 * r}a$r $r 0 1 0 0 ${-2 * r}Z"

    private fun kotak(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val lw = w - 2 * r
        val lh = h - 2 * r
        return "M${x + r} ${y}h${lw}a$r $r 0 0 1 $r ${r}v${lh}a$r $r 0 0 1 ${-r} ${r}" +
            "h${-lw}a$r $r 0 0 1 ${-r} ${-r}v${-lh}a$r $r 0 0 1 $r ${-r}Z"
    }

    /** Titik bulat: ruas sependek mungkin yang dibulatkan ujungnya oleh StrokeCap.Round. */
    private fun titik(x: Float, y: Float): String = "M$x ${y}h0.01"
}
