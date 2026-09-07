package com.sukashawarma.superapp.presentation.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.sukashawarma.superapp.data.remote.authApi

/**
 * Handoff sesi dari superapp ke aplikasi POS (`D:\PROJECT-APPS-NATIVE\POS`) —
 * project terpisah yang dipasang sendiri di perangkat, bukan modul di sini.
 *
 * Alurnya: superapp meminta token sekali pakai ke Edge Function `pos-sso-handoff`
 * (identitas diambil dari access token yang dikirim interceptor, tidak ada
 * parameter user), lalu menitipkannya lewat extra Intent. POS menukar token itu
 * di `POST /auth/v1/verify` dan mendapat sesi terpisah miliknya sendiri.
 *
 * Refresh token superapp sengaja TIDAK dioper: GoTrue merotasi refresh token dan
 * mencabut yang lama setiap kali dipakai, jadi dua aplikasi yang berbagi satu
 * sesi akan saling menjatuhkan — yang menyegarkan belakangan kena "Invalid
 * Refresh Token" dan terlempar ke layar login.
 */
internal const val POS_PACKAGE = "com.sukashawarma.pos"

/** Kontrak extra dengan POS. Namanya di-namespace paket POS supaya jelas siapa
 *  pemilik kontraknya; nilainya harus sama persis dengan yang dibaca POS. */
internal const val EXTRA_SSO_TOKEN_HASH = "com.sukashawarma.pos.extra.SSO_TOKEN_HASH"

/** Intent pembuka POS, atau null bila aplikasinya belum terpasang. Butuh
 *  deklarasi `<queries>` di manifest, kalau tidak Android 11+ selalu memberi null. */
internal fun intentBukaPos(context: Context): Intent? =
    context.packageManager.getLaunchIntentForPackage(POS_PACKAGE)

/**
 * Benar bila paket POS ditandatangani kunci yang sama dengan superapp.
 *
 * Wajib diperiksa sebelum token dititipkan: nama paket hanya unik di perangkat
 * yang sudah memasang POS asli. Di perangkat yang belum, aplikasi lain bisa
 * memasang diri dengan nama paket itu dan memanen token sesi dari extra Intent.
 * Kedua aplikasi memakai keystore rilis yang sama, jadi pemeriksaan ini lolos
 * untuk build asli dan gagal untuk tiruan.
 */
internal fun posDitandatanganiSama(context: Context): Boolean =
    context.packageManager.checkSignatures(context.packageName, POS_PACKAGE) ==
        PackageManager.SIGNATURE_MATCH

/** Token sekali pakai untuk POS, atau null bila gagal (offline, akun tanpa
 *  outlet, sesi kedaluwarsa). Pemanggil tetap boleh membuka POS tanpa token —
 *  POS akan jatuh ke login manualnya sendiri. */
internal suspend fun mintTokenSsoPos(): String? = try {
    val res = authApi.mintPosSsoToken()
    res.body()?.takeIf { it.ok }?.token_hash
} catch (e: Exception) {
    android.util.Log.e("PosSso", "mintTokenSsoPos() gagal", e)
    null
}
