package com.sukashawarma.superapp.feature.chat.ui.emoji

import android.content.Context
import java.text.BreakIterator

/**
 * Daftar emoji papan pilih, dikelompokkan seperti Telegram/iOS.
 *
 * CATATAN SOAL "EMOJI TEMA iOS"
 * Gambar emoji tidak digambar aplikasi ini, melainkan oleh FONT EMOJI milik
 * perangkat. Rupa khas iOS berasal dari font Apple Color Emoji, yang berlisensi
 * hanya untuk perangkat Apple â€” menyertakannya di dalam APK berarti
 * mendistribusikan font berbayar milik Apple tanpa hak, jadi jalan itu tidak
 * ditempuh di sini. Yang dipakai adalah font emoji bawaan perangkat, dan seluruh
 * bingkai papan ini â€” tab, kolom cari, kisi, tombol â€” digambar dengan gaya iOS
 * buatan kita sendiri. Bila kelak ingin rupa emoji yang seragam di semua HP,
 * pilihannya adalah font beelisensi bebas (Noto Color Emoji atau Twemoji) yang
 * dipasang lewat `androidx.emoji2`; keduanya bukan rupa Apple.
 */

data class KelompokEmoji(val nama: String, val ikon: String, val isi: List<String>)

private val SENYUM = (
    "ðŸ˜€ ðŸ˜ƒ ðŸ˜„ ðŸ˜ ðŸ˜† ðŸ˜… ðŸ¤£ ðŸ˜‚ ðŸ™‚ ðŸ™ƒ ðŸ˜‰ ðŸ˜Š ðŸ˜‡ ðŸ¥° ðŸ˜ ðŸ¤© ðŸ˜˜ ðŸ˜— ðŸ˜š ðŸ˜™ ðŸ˜‹ ðŸ˜› ðŸ˜œ ðŸ¤ª ðŸ˜ ðŸ¤‘ " +
        "ðŸ¤— ðŸ¤­ ðŸ¤« ðŸ¤” ðŸ¤ ðŸ¤¨ ðŸ˜ ðŸ˜‘ ðŸ˜¶ ðŸ˜ ðŸ˜’ ðŸ™„ ðŸ˜¬ ðŸ˜Œ ðŸ˜” ðŸ˜ª ðŸ¤¤ ðŸ˜´ ðŸ˜· ðŸ¤’ ðŸ¤• ðŸ¤¢ ðŸ¤® ðŸ¥µ ðŸ¥¶ " +
        "ðŸ¥´ ðŸ˜µ ðŸ¤¯ ðŸ¤  ðŸ¥³ ðŸ˜Ž ðŸ¤“ ðŸ§ ðŸ˜• ðŸ˜Ÿ ðŸ™ ðŸ˜® ðŸ˜¯ ðŸ˜² ðŸ˜³ ðŸ¥º ðŸ˜¦ ðŸ˜§ ðŸ˜¨ ðŸ˜° ðŸ˜¥ ðŸ˜¢ ðŸ˜­ ðŸ˜± ðŸ˜– " +
        "ðŸ˜£ ðŸ˜ž ðŸ˜“ ðŸ˜© ðŸ˜« ðŸ¥± ðŸ˜¤ ðŸ˜¡ ðŸ˜  ðŸ¤¬ ðŸ˜ˆ ðŸ’€ ðŸ’© ðŸ¤¡ ðŸ‘» ðŸ‘½ ðŸ¤– ðŸ˜º ðŸ˜¸ ðŸ˜¹ ðŸ˜» ðŸ˜¼ ðŸ˜½ ðŸ™€ ðŸ˜¿ ðŸ˜¾"
    ).split(" ")

private val ORANG = (
    "ðŸ‘‹ ðŸ¤š ðŸ– âœ‹ ðŸ–– ðŸ‘Œ ðŸ¤ âœŒï¸ ðŸ¤ž ðŸ¤Ÿ ðŸ¤˜ ðŸ¤™ ðŸ‘ˆ ðŸ‘‰ ðŸ‘† ðŸ–• ðŸ‘‡ â˜ï¸ ðŸ‘ ðŸ‘Ž âœŠ ðŸ‘Š ðŸ¤› ðŸ¤œ ðŸ‘ ðŸ™Œ " +
        "ðŸ‘ ðŸ¤² ðŸ¤ ðŸ™ âœï¸ ðŸ’… ðŸ¤³ ðŸ’ª ðŸ¦¾ ðŸ¦µ ðŸ¦¶ ðŸ‘‚ ðŸ‘ƒ ðŸ§  ðŸ¦· ðŸ‘€ ðŸ‘ ðŸ‘… ðŸ‘„ ðŸ’‹ ðŸ©¸ ðŸ‘¶ ðŸ§’ ðŸ‘¦ ðŸ‘§ " +
        "ðŸ§‘ ðŸ‘¨ ðŸ‘© ðŸ§“ ðŸ‘´ ðŸ‘µ ðŸ™ ðŸ™Ž ðŸ™… ðŸ™† ðŸ’ ðŸ™‹ ðŸ§ ðŸ™‡ ðŸ¤¦ ðŸ¤· ðŸ‘® ðŸ•µï¸ ðŸ’‚ ðŸ‘· ðŸ¤´ ðŸ‘¸ ðŸ‘³ ðŸ‘² ðŸ§• " +
        "ðŸ¤µ ðŸ‘° ðŸ¤° ðŸ¤± ðŸ‘¼ ðŸŽ… ðŸ¤¶ ðŸ¦¸ ðŸ¦¹ ðŸ§™ ðŸ§š ðŸ§› ðŸ§œ ðŸ§ ðŸ’† ðŸ’‡ ðŸš¶ ðŸ§ ðŸ§Ž ðŸƒ ðŸ’ƒ ðŸ•º ðŸ‘¯ ðŸ§– ðŸ§—"
    ).split(" ")

private val HEWAN = (
    "ðŸ¶ ðŸ± ðŸ­ ðŸ¹ ðŸ° ðŸ¦Š ðŸ» ðŸ¼ ðŸ¨ ðŸ¯ ðŸ¦ ðŸ® ðŸ· ðŸ½ ðŸ¸ ðŸµ ðŸ™ˆ ðŸ™‰ ðŸ™Š ðŸ’ ðŸ” ðŸ§ ðŸ¦ ðŸ¤ ðŸ£ ðŸ¥ " +
        "ðŸ¦† ðŸ¦… ðŸ¦‰ ðŸ¦‡ ðŸº ðŸ— ðŸ´ ðŸ¦„ ðŸ ðŸ› ðŸ¦‹ ðŸŒ ðŸž ðŸœ ðŸ¦— ðŸ•· ðŸ¦‚ ðŸ¢ ðŸ ðŸ¦Ž ðŸ¦– ðŸ¦• ðŸ™ ðŸ¦‘ ðŸ¦ " +
        "ðŸ¦€ ðŸ¡ ðŸ  ðŸŸ ðŸ¬ ðŸ³ ðŸ‹ ðŸ¦ˆ ðŸŠ ðŸ… ðŸ† ðŸ¦“ ðŸ¦ ðŸ˜ ðŸ¦› ðŸª ðŸ« ðŸ¦’ ðŸ¦˜ ðŸƒ ðŸ‚ ðŸ„ ðŸŽ ðŸ– ðŸ " +
        "ðŸ‘ ðŸ¦™ ðŸ ðŸ¦Œ ðŸ• ðŸ© ðŸˆ ðŸ“ ðŸ¦ƒ ðŸ¦š ðŸ¦œ ðŸ¦¢ ðŸ•Š ðŸ‡ ðŸ¦ ðŸ¦¡ ðŸ ðŸ€ ðŸ¿ ðŸŒµ ðŸŽ„ ðŸŒ² ðŸŒ³ ðŸŒ´ ðŸŒ± ðŸŒ¿ " +
        "â˜˜ï¸ ðŸ€ ðŸŽ ðŸƒ ðŸ‚ ðŸ ðŸŒ¾ ðŸŒº ðŸŒ» ðŸŒ¹ ðŸŒ· ðŸŒ¸ ðŸ’ ðŸ„ ðŸŒ° ðŸŒ ðŸŒ™ â­ ðŸŒŸ âœ¨ âš¡ ðŸ”¥ ðŸŒˆ â˜€ï¸ â›… â˜ï¸ â„ï¸ ðŸ’§ ðŸŒŠ"
    ).split(" ")

private val MAKANAN = (
    "ðŸ ðŸŽ ðŸ ðŸŠ ðŸ‹ ðŸŒ ðŸ‰ ðŸ‡ ðŸ“ ðŸˆ ðŸ’ ðŸ‘ ðŸ¥­ ðŸ ðŸ¥¥ ðŸ¥ ðŸ… ðŸ† ðŸ¥‘ ðŸ¥¦ ðŸ¥¬ ðŸ¥’ ðŸŒ¶ ðŸŒ½ ðŸ¥• ðŸ§„ " +
        "ðŸ§… ðŸ¥” ðŸ  ðŸ¥ ðŸ¥¯ ðŸž ðŸ¥– ðŸ¥¨ ðŸ§€ ðŸ¥š ðŸ³ ðŸ§ˆ ðŸ¥ž ðŸ§‡ ðŸ¥“ ðŸ¥© ðŸ— ðŸ– ðŸŒ­ ðŸ” ðŸŸ ðŸ• ðŸ¥ª ðŸ¥™ ðŸ§† " +
        "ðŸŒ® ðŸŒ¯ ðŸ¥— ðŸ¥˜ ðŸ ðŸœ ðŸ² ðŸ› ðŸ£ ðŸ± ðŸ¥Ÿ ðŸ¤ ðŸ™ ðŸš ðŸ˜ ðŸ¥ ðŸ¥  ðŸ¢ ðŸ¡ ðŸ§ ðŸ¨ ðŸ¦ ðŸ¥§ ðŸ§ ðŸ° " +
        "ðŸŽ‚ ðŸ® ðŸ­ ðŸ¬ ðŸ« ðŸ¿ ðŸ© ðŸª ðŸŒ° ðŸ¥œ ðŸ¯ ðŸ¥› ðŸ¼ â˜• ðŸµ ðŸ§ƒ ðŸ¥¤ ðŸº ðŸ» ðŸ¥‚ ðŸ· ðŸ¥ƒ ðŸ¸ ðŸ¹ ðŸ§Š ðŸ¥„ ðŸ´ ðŸ½"
    ).split(" ")

private val AKTIVITAS = (
    "âš½ ðŸ€ ðŸˆ âš¾ ðŸ¥Ž ðŸŽ¾ ðŸ ðŸ‰ ðŸ¥ ðŸŽ± ðŸª€ ðŸ“ ðŸ¸ ðŸ’ ðŸ‘ ðŸ¥ ðŸ ðŸ¥… â›³ ðŸª ðŸ¹ ðŸŽ£ ðŸ¤¿ ðŸ¥Š ðŸ¥‹ ðŸŽ½ " +
        "ðŸ›¹ ðŸ›· â›¸ ðŸ¥Œ ðŸŽ¿ â›· ðŸ‚ ðŸª‚ ðŸ‹ï¸ ðŸ¤¼ ðŸ¤¸ â›¹ï¸ ðŸ¤º ðŸ¤¾ ðŸŒï¸ ðŸ‡ ðŸ§˜ ðŸ„ ðŸŠ ðŸ¤½ ðŸš£ ðŸ§— ðŸšµ ðŸš´ ðŸ† " +
        "ðŸ¥‡ ðŸ¥ˆ ðŸ¥‰ ðŸ… ðŸŽ– ðŸµ ðŸŽ— ðŸŽ« ðŸŽŸ ðŸŽª ðŸ¤¹ ðŸŽ­ ðŸ©° ðŸŽ¨ ðŸŽ¬ ðŸŽ¤ ðŸŽ§ ðŸŽ¼ ðŸŽ¹ ðŸ¥ ðŸŽ· ðŸŽº ðŸŽ¸ ðŸª• ðŸŽ» ðŸŽ² â™Ÿ ðŸŽ¯ ðŸŽ³ ðŸŽ® ðŸŽ° ðŸ§©"
    ).split(" ")

private val PERJALANAN = (
    "ðŸš— ðŸš• ðŸš™ ðŸšŒ ðŸšŽ ðŸŽ ðŸš“ ðŸš‘ ðŸš’ ðŸš ðŸ›» ðŸšš ðŸš› ðŸšœ ðŸ¦¯ ðŸ¦½ ðŸ¦¼ ðŸ›´ ðŸš² ðŸ›µ ðŸ ðŸ›º ðŸš¨ ðŸš” ðŸš ðŸš˜ " +
        "ðŸš– ðŸš¡ ðŸš  ðŸšŸ ðŸšƒ ðŸš‹ ðŸšž ðŸš ðŸš„ ðŸš… ðŸšˆ ðŸš‚ ðŸš† ðŸš‡ ðŸšŠ ðŸš‰ âœˆï¸ ðŸ›« ðŸ›¬ ðŸ›© ðŸ’º ðŸ›° ðŸš€ ðŸ›¸ ðŸš " +
        "ðŸ›¶ â›µ ðŸš¤ ðŸ›¥ ðŸ›³ â›´ ðŸš¢ âš“ â›½ ðŸš§ ðŸš¦ ðŸš¥ ðŸ—º ðŸ—¿ ðŸ—½ ðŸ—¼ ðŸ° ðŸ¯ ðŸŸ ðŸŽ¡ ðŸŽ¢ ðŸŽ  â›² â›± ðŸ– ðŸ " +
        "ðŸœ ðŸŒ‹ â›° ðŸ” ðŸ—» ðŸ• â›º ðŸ  ðŸ¡ ðŸ˜ ðŸš ðŸ— ðŸ­ ðŸ¢ ðŸ¬ ðŸ£ ðŸ¤ ðŸ¥ ðŸ¦ ðŸ¨ ðŸª ðŸ« ðŸ© ðŸ’’ ðŸ› â›ª ðŸ•Œ ðŸ• ðŸ›•"
    ).split(" ")

private val BENDA = (
    "âŒš ðŸ“± ðŸ’» âŒ¨ï¸ ðŸ–¥ ðŸ–¨ ðŸ–± ðŸ’½ ðŸ’¾ ðŸ’¿ ðŸ“€ ðŸ“· ðŸ“¸ ðŸ“¹ ðŸŽ¥ ðŸ“½ ðŸ“ž â˜Žï¸ ðŸ“Ÿ ðŸ“  ðŸ“º ðŸ“» ðŸŽ™ â± â² â° " +
        "ðŸ•° âŒ› â³ ðŸ“¡ ðŸ”‹ ðŸ”Œ ðŸ’¡ ðŸ”¦ ðŸ•¯ ðŸ§¯ ðŸ›¢ ðŸ’¸ ðŸ’µ ðŸ’´ ðŸ’¶ ðŸ’· ðŸ’° ðŸ’³ ðŸ§¾ ðŸ’Ž âš–ï¸ ðŸ§° ðŸ”§ ðŸ”¨ âš’ ðŸ›  " +
        "â› ðŸ”© âš™ï¸ ðŸ§± â›“ ðŸ§² ðŸ”« ðŸ’£ ðŸ§¨ ðŸ”ª ðŸ—¡ âš”ï¸ ðŸ›¡ ðŸš¬ âš°ï¸ ðŸº ðŸ”® ðŸ“¿ ðŸ§¿ ðŸ’ˆ âš—ï¸ ðŸ”­ ðŸ”¬ ðŸ•³ ðŸ’Š " +
        "ðŸ’‰ ðŸ©¹ ðŸ©º ðŸŒ¡ ðŸ§¹ ðŸ§º ðŸ§» ðŸš½ ðŸš° ðŸš¿ ðŸ› ðŸ§¼ ðŸª’ ðŸ§½ ðŸ§´ ðŸ›Ž ðŸ”‘ ðŸ— ðŸšª ðŸª‘ ðŸ›‹ ðŸ› ðŸ§¸ ðŸ–¼ ðŸ› ðŸ›’ ðŸŽ ðŸŽˆ ðŸŽ ðŸŽ€ ðŸŽ‰ ðŸŽŠ"
    ).split(" ")

private val SIMBOL = (
    "â¤ï¸ ðŸ§¡ ðŸ’› ðŸ’š ðŸ’™ ðŸ’œ ðŸ–¤ ðŸ¤ ðŸ¤Ž ðŸ’” â£ï¸ ðŸ’• ðŸ’ž ðŸ’“ ðŸ’— ðŸ’– ðŸ’˜ ðŸ’ ðŸ’Ÿ â˜®ï¸ âœï¸ â˜ªï¸ ðŸ•‰ â˜¸ï¸ âœ¡ï¸ ðŸ”¯ " +
        "ðŸ•Ž â˜¯ï¸ â˜¦ï¸ ðŸ› â›Ž â™ˆ â™‰ â™Š â™‹ â™Œ â™ â™Ž â™ â™ â™‘ â™’ â™“ ðŸ†” âš›ï¸ ðŸ‰‘ â˜¢ï¸ â˜£ï¸ ðŸ“´ ðŸ“³ ðŸˆ¶ ðŸˆš ðŸˆ¸ " +
        "ðŸˆº ðŸ‰ âœ´ï¸ ðŸ†š ðŸ’® ðŸ‰  ãŠ™ï¸ ãŠ—ï¸ ðŸˆ´ ðŸˆµ ðŸˆ¹ ðŸˆ² ðŸ…°ï¸ ðŸ…±ï¸ ðŸ†Ž ðŸ†‘ ðŸ…¾ï¸ ðŸ†˜ âŒ â­• ðŸ›‘ â›” ðŸ“› ðŸš« ðŸ’¯ " +
        "ðŸ’¢ â™¨ï¸ ðŸš· ðŸš¯ ðŸš³ ðŸš± ðŸ”ž ðŸ“µ â— â“ â• â” â€¼ï¸ â‰ï¸ ðŸ”… ðŸ”† ã€½ï¸ âš ï¸ ðŸš¸ ðŸ”± âšœï¸ ðŸ”° â™»ï¸ âœ… ðŸˆ¯ " +
        "ðŸ’¹ â‡ï¸ âœ³ï¸ âŽ ðŸŒ ðŸ’  â“‚ï¸ ðŸŒ€ ðŸ’¤ ðŸ§ ðŸš¾ â™¿ ðŸ…¿ï¸ ðŸ›— ðŸˆ³ ðŸˆ‚ï¸ ðŸ›‚ ðŸ›ƒ ðŸ›„ ðŸ›… ðŸš¹ ðŸšº ðŸš¼ âš§ ðŸš» ðŸš® ðŸŽ¦ ðŸ“¶ ðŸˆ ðŸ”£ â„¹ï¸ ðŸ”¤ ðŸ”¡ ðŸ”  ðŸ†– ðŸ†— ðŸ†™ ðŸ†’ ðŸ†• ðŸ†“"
    ).split(" ")

private val BENDERA = (
    "ðŸ ðŸš© ðŸŽŒ ðŸ´ ðŸ³ï¸ ðŸ³ï¸â€ðŸŒˆ ðŸ‡®ðŸ‡© ðŸ‡²ðŸ‡¾ ðŸ‡¸ðŸ‡¬ ðŸ‡¹ðŸ‡­ ðŸ‡»ðŸ‡³ ðŸ‡µðŸ‡­ ðŸ‡§ðŸ‡³ ðŸ‡°ðŸ‡­ ðŸ‡±ðŸ‡¦ ðŸ‡²ðŸ‡² ðŸ‡¹ðŸ‡± ðŸ‡¯ðŸ‡µ ðŸ‡°ðŸ‡· ðŸ‡¨ðŸ‡³ ðŸ‡­ðŸ‡° ðŸ‡¹ðŸ‡¼ " +
        "ðŸ‡®ðŸ‡³ ðŸ‡µðŸ‡° ðŸ‡§ðŸ‡© ðŸ‡¦ðŸ‡º ðŸ‡³ðŸ‡¿ ðŸ‡ºðŸ‡¸ ðŸ‡¨ðŸ‡¦ ðŸ‡²ðŸ‡½ ðŸ‡§ðŸ‡· ðŸ‡¦ðŸ‡· ðŸ‡¬ðŸ‡§ ðŸ‡®ðŸ‡ª ðŸ‡«ðŸ‡· ðŸ‡©ðŸ‡ª ðŸ‡®ðŸ‡¹ ðŸ‡ªðŸ‡¸ ðŸ‡µðŸ‡¹ ðŸ‡³ðŸ‡± ðŸ‡§ðŸ‡ª ðŸ‡¨ðŸ‡­ ðŸ‡¦ðŸ‡¹ " +
        "ðŸ‡¸ðŸ‡ª ðŸ‡³ðŸ‡´ ðŸ‡©ðŸ‡° ðŸ‡«ðŸ‡® ðŸ‡µðŸ‡± ðŸ‡·ðŸ‡º ðŸ‡ºðŸ‡¦ ðŸ‡¹ðŸ‡· ðŸ‡¸ðŸ‡¦ ðŸ‡¦ðŸ‡ª ðŸ‡¶ðŸ‡¦ ðŸ‡ªðŸ‡¬ ðŸ‡¿ðŸ‡¦ ðŸ‡³ðŸ‡¬ ðŸ‡°ðŸ‡ª ðŸ‡²ðŸ‡¦"
    ).split(" ")

/** Kelompok tetap, tanpa "sering dipakai" â€” itu disusun saat papan dibuka. */
val KELOMPOK_EMOJI: List<KelompokEmoji> = listOf(
    KelompokEmoji("Senyum", "ðŸ˜€", SENYUM),
    KelompokEmoji("Orang", "ðŸ‘‹", ORANG),
    KelompokEmoji("Hewan & Alam", "ðŸ¶", HEWAN),
    KelompokEmoji("Makanan", "ðŸ”", MAKANAN),
    KelompokEmoji("Aktivitas", "âš½", AKTIVITAS),
    KelompokEmoji("Perjalanan", "ðŸš—", PERJALANAN),
    KelompokEmoji("Benda", "ðŸ’¡", BENDA),
    KelompokEmoji("Simbol", "â¤ï¸", SIMBOL),
    KelompokEmoji("Bendera", "ðŸ", BENDERA),
)

/**
 * Emoji yang paling sering dipakai pengguna, disimpan lokal.
 *
 * Per perangkat, bukan per akun: ini kenyamanan mengetik, bukan data yang perlu
 * ikut berpindah HP â€” dan menyimpannya di server berarti satu tabel lagi yang
 * harus dijaga demi hal yang tidak ada nilainya bila hilang.
 */
object EmojiSering {
    private const val BERKAS = "chat_emoji_sering"
    private const val KUNCI = "daftar"
    private const val MAKS = 24

    /** Pemisah karakter kendali. Satu emoji bisa tersusun dari beberapa code
     *  point (bendera, warna kulit, keluarga), jadi daftarnya tidak boleh
     *  dipisah per karakter â€” dan pemisahnya harus sesuatu yang mustahil muncul
     *  di dalam emoji itu sendiri. */
    private const val PEMISAH = "\u0001"

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
