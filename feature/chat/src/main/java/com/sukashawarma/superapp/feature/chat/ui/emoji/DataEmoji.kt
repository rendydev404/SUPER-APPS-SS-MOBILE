package com.sukashawarma.superapp.feature.chat.ui.emoji

import android.content.Context
import java.text.BreakIterator

/**
 * Daftar emoji papan pilih, dikelompokkan seperti Telegram/iOS.
 *
 * BERKAS INI HARUS UTF-8. Jangan menyuntingnya lewat pipa PowerShell:
 * `Get-Content -Raw` di Windows PowerShell 5.1 membaca berkas dengan codepage
 * ANSI, sehingga setiap emoji berubah jadi "ï¿½ï¿½" begitu ditulis ulang sebagai
 * UTF-8. Itu sudah pernah merusak seluruh daftar di bawah.
 *
 * CATATAN SOAL "EMOJI TEMA iOS"
 * Gambar emoji tidak digambar aplikasi ini, melainkan oleh FONT EMOJI milik
 * perangkat. Rupa khas iOS berasal dari font Apple Color Emoji yang berlisensi
 * hanya untuk perangkat Apple, jadi menyertakannya di APK bukan pilihan yang
 * sah. Yang dipakai adalah font emoji bawaan perangkat, sementara bingkai papan
 * ini digambar bergaya iOS sendiri. Bila kelak ingin rupa seragam di semua HP,
 * pilihannya font berlisensi bebas (Noto Color Emoji atau Twemoji) lewat
 * `androidx.emoji2` — keduanya bukan rupa Apple.
 */

data class KelompokEmoji(val nama: String, val ikon: String, val isi: List<String>)

private val SENYUM = (
    "😀 😃 😄 😁 😆 😅 🤣 😂 🙂 🙃 😉 😊 😇 🥰 😍 🤩 😘 😗 😚 😙 😋 😛 😜 🤪 😝 🤑 " +
        "🤗 🤭 🤫 🤔 🤐 🤨 😐 😑 😶 😏 😒 🙄 😬 😌 😔 😪 🤤 😴 😷 🤒 🤕 🤢 🤮 🥵 🥶 " +
        "🥴 😵 🤯 🤠 🥳 😎 🤓 🧐 😕 😟 🙁 😮 😯 😲 😳 🥺 😦 😧 😨 😰 😥 😢 😭 😱 😖 " +
        "😣 😞 😓 😩 😫 🥱 😤 😡 😠 🤬 😈 💀 💩 🤡 👻 👽 🤖 😺 😸 😹 😻 😼 😽 🙀 😿 😾"
    ).split(" ")

private val ORANG = (
    "👋 🤚 🖐 ✋ 🖖 👌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 " +
        "👐 🤲 🤝 🙏 ✍️ 💅 🤳 💪 🦾 🦵 🦶 👂 👃 🧠 🦷 👀 👁 👅 👄 💋 🩸 👶 🧒 👦 👧 " +
        "🧑 👨 👩 🧓 👴 👵 🙍 🙎 🙅 🙆 💁 🙋 🧏 🙇 🤦 🤷 👮 🕵️ 💂 👷 🤴 👸 👳 👲 🧕 " +
        "🤵 👰 🤰 🤱 👼 🎅 🤶 🦸 🦹 🧙 🧚 🧛 🧜 🧝 💆 💇 🚶 🧍 🧎 🏃 💃 🕺 👯 🧖 🧗"
    ).split(" ")

private val HEWAN = (
    "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐽 🐸 🐵 🙈 🙉 🙊 🐒 🐔 🐧 🐦 🐤 🐣 🐥 " +
        "🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐜 🦗 🕷 🦂 🐢 🐍 🦎 🦖 🦕 🐙 🦑 🦐 " +
        "🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐅 🐆 🦓 🦍 🐘 🦛 🐪 🐫 🦒 🦘 🐃 🐂 🐄 🐎 🐖 🐏 " +
        "🐑 🦙 🐐 🦌 🐕 🐩 🐈 🐓 🦃 🦚 🦜 🦢 🕊 🐇 🦝 🦡 🐁 🐀 🐿 🌵 🎄 🌲 🌳 🌴 🌱 🌿 " +
        "☘️ 🍀 🎍 🍃 🍂 🍁 🌾 🌺 🌻 🌹 🌷 🌸 💐 🍄 🌰 🌍 🌙 ⭐ 🌟 ✨ ⚡ 🔥 🌈 ☀️ ⛅ ☁️ ❄️ 💧 🌊"
    ).split(" ")

private val MAKANAN = (
    "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶 🌽 🥕 🧄 " +
        "🧅 🥔 🍠 🥐 🥯 🍞 🥖 🥨 🧀 🥚 🍳 🧈 🥞 🧇 🥓 🥩 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🥙 🧆 " +
        "🌮 🌯 🥗 🥘 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🍤 🍙 🍚 🍘 🍥 🥠 🍢 🍡 🍧 🍨 🍦 🥧 🧁 🍰 " +
        "🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 🥜 🍯 🥛 🍼 ☕ 🍵 🧃 🥤 🍺 🍻 🥂 🍷 🥃 🍸 🍹 🧊 🥄 🍴 🍽"
    ).split(" ")

private val AKTIVITAS = (
    "⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🥏 🎱 🪀 🏓 🏸 🏒 🏑 🥍 🏏 🥅 ⛳ 🪁 🏹 🎣 🤿 🥊 🥋 🎽 " +
        "🛹 🛷 ⛸ 🥌 🎿 ⛷ 🏂 🪂 🏋️ 🤼 🤸 ⛹️ 🤺 🤾 🏌️ 🏇 🧘 🏄 🏊 🤽 🚣 🧗 🚵 🚴 🏆 " +
        "🥇 🥈 🥉 🏅 🎖 🏵 🎗 🎫 🎟 🎪 🤹 🎭 🩰 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🪕 🎻 " +
        "🎲 ♟ 🎯 🎳 🎮 🎰 🧩"
    ).split(" ")

private val PERJALANAN = (
    "🚗 🚕 🚙 🚌 🚎 🏎 🚓 🚑 🚒 🚐 🛻 🚚 🚛 🚜 🦽 🦼 🛴 🚲 🛵 🏍 🛺 🚨 🚔 🚍 🚘 " +
        "🚖 🚡 🚠 🚟 🚃 🚋 🚞 🚝 🚄 🚅 🚈 🚂 🚆 🚇 🚊 🚉 ✈️ 🛫 🛬 🛩 💺 🛰 🚀 🛸 🚁 " +
        "🛶 ⛵ 🚤 🛥 🛳 ⛴ 🚢 ⚓ ⛽ 🚧 🚦 🚥 🗺 🗿 🗽 🗼 🏰 🏯 🏟 🎡 🎢 🎠 ⛲ ⛱ 🏖 🏝 " +
        "🏜 🌋 ⛰ 🏔 🗻 🏕 ⛺ 🏠 🏡 🏘 🏚 🏗 🏭 🏢 🏬 🏣 🏤 🏥 🏦 🏨 🏪 🏫 🏩 💒 🏛 ⛪ 🕌 🕍 🛕"
    ).split(" ")

private val BENDA = (
    "⌚ 📱 💻 ⌨️ 🖥 🖨 🖱 💽 💾 💿 📀 📷 📸 📹 🎥 📽 📞 ☎️ 📟 📠 📺 📻 🎙 ⏱ ⏲ ⏰ " +
        "🕰 ⌛ ⏳ 📡 🔋 🔌 💡 🔦 🕯 🧯 🛢 💸 💵 💴 💶 💷 💰 💳 🧾 💎 ⚖️ 🧰 🔧 🔨 ⚒ 🛠 " +
        "⛏ 🔩 ⚙️ 🧱 ⛓ 🧲 🧨 🔪 🛡 🏺 🔮 📿 🧿 💈 ⚗️ 🔭 🔬 🕳 💊 " +
        "💉 🩹 🩺 🌡 🧹 🧺 🧻 🚽 🚰 🚿 🛁 🧼 🪒 🧽 🧴 🛎 🔑 🗝 🚪 🪑 🛋 🛏 🧸 🖼 🛍 🛒 🎁 🎈 🎏 🎀 🎉 🎊"
    ).split(" ")

private val SIMBOL = (
    "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ☮️ ✝️ ☪️ 🕉 ☸️ ✡️ 🔯 " +
        "🕎 ☯️ ☦️ 🛐 ⛎ ♈ ♉ ♊ ♋ ♌ ♍ ♎ ♏ ♐ ♑ ♒ ♓ 🆔 ⚛️ 🉑 ☢️ ☣️ 📴 📳 🈶 🈚 🈸 " +
        "🈺 🉐 ✴️ 🆚 💮 ㊙️ ㊗️ 🈴 🈵 🈹 🈲 🅰️ 🅱️ 🆎 🆑 🅾️ 🆘 ❌ ⭕ 🛑 ⛔ 📛 🚫 💯 " +
        "💢 ♨️ 🚷 🚯 🚳 🚱 🔞 📵 ❗ ❓ ❕ ❔ ‼️ ⁉️ 🔅 🔆 〽️ ⚠️ 🚸 🔱 ⚜️ 🔰 ♻️ ✅ 🈯 " +
        "💹 ❇️ ✳️ ❎ 🌐 💠 Ⓜ️ 🌀 💤 🏧 🚾 ♿ 🅿️ 🈳 🈂️ 🛂 🛃 🛄 🛅 🚹 🚺 🚼 🚻 🚮 🎦 📶 🈁 🔣 ℹ️ 🔤 🔡 🔠 🆖 🆗 🆙 🆒 🆕 🆓"
    ).split(" ")

private val BENDERA = (
    "🏁 🚩 🎌 🏴 🏳️ 🇮🇩 🇲🇾 🇸🇬 🇹🇭 🇻🇳 🇵🇭 🇧🇳 🇰🇭 🇱🇦 🇲🇲 🇹🇱 🇯🇵 🇰🇷 🇨🇳 🇭🇰 🇹🇼 " +
        "🇮🇳 🇵🇰 🇧🇩 🇦🇺 🇳🇿 🇺🇸 🇨🇦 🇲🇽 🇧🇷 🇦🇷 🇬🇧 🇮🇪 🇫🇷 🇩🇪 🇮🇹 🇪🇸 🇵🇹 🇳🇱 🇧🇪 🇨🇭 🇦🇹 " +
        "🇸🇪 🇳🇴 🇩🇰 🇫🇮 🇵🇱 🇷🇺 🇺🇦 🇹🇷 🇸🇦 🇦🇪 🇶🇦 🇪🇬 🇿🇦 🇳🇬 🇰🇪 🇲🇦"
    ).split(" ")

/** Kelompok tetap, tanpa "sering dipakai" — itu disusun saat papan dibuka. */
val KELOMPOK_EMOJI: List<KelompokEmoji> = listOf(
    KelompokEmoji("Senyum", "😀", SENYUM),
    KelompokEmoji("Orang", "👋", ORANG),
    KelompokEmoji("Hewan & Alam", "🐶", HEWAN),
    KelompokEmoji("Makanan", "🍔", MAKANAN),
    KelompokEmoji("Aktivitas", "⚽", AKTIVITAS),
    KelompokEmoji("Perjalanan", "🚗", PERJALANAN),
    KelompokEmoji("Benda", "💡", BENDA),
    KelompokEmoji("Simbol", "❤️", SIMBOL),
    KelompokEmoji("Bendera", "🏁", BENDERA),
)

/**
 * Emoji yang paling sering dipakai pengguna, disimpan lokal.
 *
 * Per perangkat, bukan per akun: ini kenyamanan mengetik, bukan data yang perlu
 * ikut berpindah HP — dan menyimpannya di server berarti satu tabel lagi yang
 * harus dijaga demi hal yang tidak ada nilainya bila hilang.
 */
object EmojiSering {
    private const val BERKAS = "chat_emoji_sering"
    private const val KUNCI = "daftar"
    private const val MAKS = 24

    /** Satu emoji bisa tersusun dari beberapa code point, jadi daftarnya tidak
     *  boleh dipisah per karakter. Pemisahnya sengaja ASCII biasa — sebuah
     *  karakter kendali di dalam kode sumber gampang tercabik penyunting atau
     *  pipa yang salah encoding, dan "|" mustahil muncul di dalam emoji. */
    private const val PEMISAH = "|"

    fun ambil(context: Context): List<String> =
        context.getSharedPreferences(BERKAS, Context.MODE_PRIVATE)
            .getString(KUNCI, "")
            .orEmpty()
            .split(PEMISAH)
            .filter { it.isNotBlank() }

    fun catat(context: Context, emoji: String) {
        val prefs = context.getSharedPreferences(BERKAS, Context.MODE_PRIVATE)
        val baru = (listOf(emoji) + ambil(context).filterNot { it == emoji }).take(MAKS)
        prefs.edit().putString(KUNCI, baru.joinToString(PEMISAH)).apply()
    }
}

/**
 * Membuang satu karakter tampak dari ujung teks.
 *
 * Emoji bukan satu char: bendera, keluarga, dan emoji berwarna kulit tersusun
 * dari beberapa code point yang disatukan penanda gabung. `dropLast(1)` akan
 * memotongnya di tengah dan meninggalkan pecahan aneh, jadi pemenggalannya
 * memakai BreakIterator yang mengerti batas grafem.
 */
fun hapusSatuKarakter(teks: String): String {
    if (teks.isEmpty()) return teks
    val iterator = BreakIterator.getCharacterInstance().apply { setText(teks) }
    iterator.last()
    val batas = iterator.previous()
    return if (batas == BreakIterator.DONE) "" else teks.substring(0, batas)
}
