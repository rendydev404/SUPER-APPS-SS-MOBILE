# Modul Distribusi Native Fase 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun modul `:feature:distribusi` native yang memindahkan seluruh alur penerimaan barang dan pemantauan surat jalan dari web ke Android untuk role `crew`, `leader`, `area_manager`, dan `regional_manager`.

**Architecture:** Mengikuti pola `:feature:stok` yang sudah ada — `domain/` berisi fungsi murni tanpa dependensi Android (diuji dengan JUnit), `data/` hanya tahu PostgREST dan JSON lewat `Postgrest` di `:core:network`, `ui/` satu paket per layar dengan `ViewModel` + `StateFlow`. Satu `NavHost` bersarang dipasang pada satu rute di `NavHost` root.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2024.02.00), Material3, Navigation Compose 2.7.7, CameraX 1.3.4, ML Kit barcode-scanning, SharedPreferences, OkHttp 4.12.0 + Gson lewat `Postgrest`, Hilt 2.51, JUnit 4.13.2.

**Spec:** `docs/superpowers/specs/2026-09-04-distribusi-native-fase-1-design.md`

## Global Constraints

Setiap task tunduk pada batasan ini. Melanggar salah satunya membatalkan pekerjaan.

- **Nol migration.** Tidak membuat atau mengubah tabel, kolom, view, RPC, RLS, trigger, bucket, atau policy storage. Semua yang dipakai sudah ada di database produksi hari ini.
- **Nol perubahan pada repo web.** Tidak menyentuh satu file pun di `C:\Users\Creator MPB\OneDrive\Desktop\New folder\DIGITALISASI-SS-PROJECT`.
- **Tidak menyentuh service-role.** Semua request memakai token pengguna lewat `SupabaseClient.okHttpClient`. Service key tidak boleh ada di dalam APK.
- **Jangan menulis kolom `surat_jalan_item.selisih`** — kolom `GENERATED ALWAYS AS ... STORED`, Postgres menolaknya.
- **Jangan menulis kolom `surat_jalan_item.harga_snapshot`** — diisi trigger `fill_harga_snapshot`.
- **Jangan menulis kolom `surat_jalan_item.verified_by`** — web tidak menulisnya; bentuk baris hasil native harus identik dengan hasil web.
- **Jangan menulis `ledger_stok` atau `stok_balance` langsung.** Hanya RPC `finalize_surat_jalan_and_ledger` yang boleh.
- **Cakupan outlet selalu dari RPC `accessible_outlet_ids()`**, tidak pernah dari daftar role yang di-hardcode di aplikasi.
- **Bahasa antarmuka Indonesia.** Nama kelas, fungsi, dan variabel domain juga Indonesia, mengikuti `:feature:stok`.
- **Gradle dijalankan dari PowerShell dengan `.\gradlew.bat`**, bukan dari Bash — daemon gagal start dari Bash di mesin ini.
- Package root modul: `com.sukashawarma.superapp.feature.distribusi`. Direktori sumber: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/`. Direktori test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/`.

## Struktur File

| File | Tanggung jawab |
|---|---|
| `feature/distribusi/build.gradle.kts` | Dependensi modul (modifikasi) |
| `DistribusiRoutes.kt` | Konstanta rute internal + helper encode argumen |
| `DistribusiNavGraph.kt` | `NavHost` modul, dipasang di `Routes.DISTRIBUSI` |
| `domain/StatusSuratJalan.kt` | Enum status, label Indonesia, aturan "ada selisih" |
| `domain/SatuanDistribusi.kt` | Konversi satuan dasar ⇄ satuan distribusi |
| `domain/DistribusiAkses.kt` | Role → kemampuan |
| `domain/ValidasiVerifikasi.kt` | Aturan boleh-lanjut per item |
| `domain/RingkasanDistribusi.kt` | Statistik dashboard dari daftar SJ |
| `domain/DistribusiError.kt` | Pemetaan exception → pesan Indonesia |
| `data/model/DistribusiModels.kt` | `SuratJalan`, `SuratJalanItem`, `TandaTangan`, `BahanBakuMeta`, `OutletRingkas` |
| `data/SuratJalanRepository.kt` | Baca daftar & detail, tulis verifikasi, panggil RPC |
| `data/VerifikasiDraftStore.kt` | Simpanan lokal: draft verifikasi + penanda unlock QR |
| `data/FotoBuktiStore.kt` | Kompres, unggah, dan ambil kembali foto bukti |
| `ui/DistribusiComponents.kt` | Kartu SJ, lencana status, baris item — dipakai lintas layar |
| `ui/SegarkanSaatAktif.kt` | Muat ulang saat layar kembali ke depan |
| `ui/dashboard/DashboardScreen.kt` + `DashboardViewModel.kt` | Statistik, filter, pencarian, tutup dokumen |
| `ui/inbox/InboxScreen.kt` + `InboxViewModel.kt` | Daftar kiriman masuk |
| `ui/scan/ScanQrScreen.kt` + `ScanQrViewModel.kt` | Gerbang QR + kode manual |
| `ui/verifikasi/VerifikasiScreen.kt` + `VerifikasiViewModel.kt` | Kartu per item → ringkasan → TTD → finalisasi |
| `ui/verifikasi/FotoCameraSheet.kt` | Pengambilan foto bukti dengan CameraX |
| `ui/ttd/TandaTanganCanvas.kt` | Canvas goresan → PNG data URL |
| `ui/riwayat/RiwayatScreen.kt` + `RiwayatViewModel.kt` | Daftar penerimaan selesai |
| `ui/detail/DetailSuratJalanScreen.kt` + `DetailViewModel.kt` | Dokumen: item, selisih, foto, dua blok TTD |
| `app/.../MainActivity.kt` | Rute `Routes.DISTRIBUSI` (modifikasi) |
| `feature/home/.../HomeScreen.kt` | Kartu modul Distribusi (modifikasi) |
| `app/build.gradle.kts` | `implementation(project(":feature:distribusi"))` (modifikasi) |

Urutan task: domain murni lebih dulu (Task 1–6), lalu data (Task 7–10), lalu UI (Task 11–17), lalu perakitan dan penyegaran (Task 18–19).

---

### Task 1: Dependensi modul + status surat jalan

**Files:**
- Modify: `feature/distribusi/build.gradle.kts`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/StatusSuratJalan.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/StatusSuratJalanTest.kt`

**Interfaces:**
- Consumes: tidak ada — task pertama.
- Produces:
  - `enum class StatusSuratJalan(val nilai: String, val label: String)` dengan konstanta `DRAFT`, `DIKIRIM`, `DIKIRIM_LENGKAP`, `DITERIMA_SEBAGIAN`, `DITERIMA_LENGKAP`, `SELESAI`
  - `StatusSuratJalan.Companion.dari(nilai: String?): StatusSuratJalan?`
  - `val StatusSuratJalan.bolehDiverifikasi: Boolean`
  - `val StatusSuratJalan.sudahDiterima: Boolean`
  - `val StatusSuratJalan.bolehDitutup: Boolean`
  - `interface PenandaSelisih { val qtyDikirim: Double; val qtyTerima: Double?; val kondisi: String? }`
  - `fun adaSelisih(items: List<PenandaSelisih>): Boolean`

- [ ] **Step 1: Tambahkan dependensi modul**

Ganti seluruh blok `dependencies` di `feature/distribusi/build.gradle.kts` dengan:

```kotlin
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(project(":core:ui"))
    implementation(project(":core:roles"))
    implementation(project(":core:network"))
    implementation(project(":core:storage"))
    implementation(project(":core:camera"))
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
    testImplementation("junit:junit:4.13.2")
}
```

`:core:camera` mengekspor CameraX lewat `api(...)`, jadi CameraX ikut terbawa tanpa perlu disebut ulang. Izin `android.permission.CAMERA` sudah ada di `app/src/main/AndroidManifest.xml`, tidak perlu ditambah.

- [ ] **Step 2: Tulis test yang gagal**

Buat `StatusSuratJalanTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private data class ItemUji(
    override val qtyDikirim: Double,
    override val qtyTerima: Double?,
    override val kondisi: String?,
) : PenandaSelisih

class StatusSuratJalanTest {

    @Test
    fun `keenam nilai status dikenali`() {
        assertEquals(StatusSuratJalan.DRAFT, StatusSuratJalan.dari("draft"))
        assertEquals(StatusSuratJalan.DIKIRIM, StatusSuratJalan.dari("dikirim"))
        assertEquals(StatusSuratJalan.DIKIRIM_LENGKAP, StatusSuratJalan.dari("dikirim_lengkap"))
        assertEquals(StatusSuratJalan.DITERIMA_SEBAGIAN, StatusSuratJalan.dari("diterima_sebagian"))
        assertEquals(StatusSuratJalan.DITERIMA_LENGKAP, StatusSuratJalan.dari("diterima_lengkap"))
        assertEquals(StatusSuratJalan.SELESAI, StatusSuratJalan.dari("selesai"))
    }

    @Test
    fun `nilai tak dikenal jadi null, bukan lempar`() {
        assertNull(StatusSuratJalan.dari("diterima"))
        assertNull(StatusSuratJalan.dari(null))
        assertNull(StatusSuratJalan.dari(""))
    }

    @Test
    fun `label berbahasa Indonesia`() {
        assertEquals("Draft", StatusSuratJalan.DRAFT.label)
        assertEquals("Dalam Transit", StatusSuratJalan.DIKIRIM.label)
        assertEquals("Dalam Transit", StatusSuratJalan.DIKIRIM_LENGKAP.label)
        assertEquals("Diterima Sebagian", StatusSuratJalan.DITERIMA_SEBAGIAN.label)
        assertEquals("Diterima Lengkap", StatusSuratJalan.DITERIMA_LENGKAP.label)
        assertEquals("Selesai", StatusSuratJalan.SELESAI.label)
    }

    @Test
    fun `hanya status transit dan diterima sebagian yang boleh diverifikasi`() {
        assertTrue(StatusSuratJalan.DIKIRIM.bolehDiverifikasi)
        assertTrue(StatusSuratJalan.DIKIRIM_LENGKAP.bolehDiverifikasi)
        assertTrue(StatusSuratJalan.DITERIMA_SEBAGIAN.bolehDiverifikasi)
        assertFalse(StatusSuratJalan.DRAFT.bolehDiverifikasi)
        assertFalse(StatusSuratJalan.DITERIMA_LENGKAP.bolehDiverifikasi)
        assertFalse(StatusSuratJalan.SELESAI.bolehDiverifikasi)
    }

    @Test
    fun `hanya yang sudah diterima yang boleh ditutup jadi selesai`() {
        assertTrue(StatusSuratJalan.DITERIMA_LENGKAP.bolehDitutup)
        assertTrue(StatusSuratJalan.DITERIMA_SEBAGIAN.bolehDitutup)
        assertFalse(StatusSuratJalan.SELESAI.bolehDitutup)
        assertFalse(StatusSuratJalan.DIKIRIM.bolehDitutup)
        assertFalse(StatusSuratJalan.DRAFT.bolehDitutup)
    }

    @Test
    fun `sudah diterima mencakup selesai`() {
        assertTrue(StatusSuratJalan.DITERIMA_LENGKAP.sudahDiterima)
        assertTrue(StatusSuratJalan.DITERIMA_SEBAGIAN.sudahDiterima)
        assertTrue(StatusSuratJalan.SELESAI.sudahDiterima)
        assertFalse(StatusSuratJalan.DIKIRIM.sudahDiterima)
    }

    @Test
    fun `item rusak dihitung selisih`() {
        assertTrue(adaSelisih(listOf(ItemUji(10.0, 10.0, "rusak"))))
    }

    @Test
    fun `qty terima kurang dari dikirim dihitung selisih`() {
        assertTrue(adaSelisih(listOf(ItemUji(10.0, 9.5, "baik"))))
    }

    @Test
    fun `qty terima pas tidak dihitung selisih`() {
        assertFalse(adaSelisih(listOf(ItemUji(10.0, 10.0, "baik"))))
    }

    /** Item yang belum diverifikasi (qty_terima null) BUKAN selisih. Kalau dihitung,
     *  setiap surat jalan yang baru dikirim akan tampak bermasalah di dashboard. */
    @Test
    fun `item belum diverifikasi bukan selisih`() {
        assertFalse(adaSelisih(listOf(ItemUji(10.0, null, null))))
    }

    @Test
    fun `satu item bermasalah menandai seluruh surat jalan`() {
        val items = listOf(
            ItemUji(10.0, 10.0, "baik"),
            ItemUji(5.0, 3.0, "baik"),
            ItemUji(2.0, 2.0, "baik"),
        )
        assertTrue(adaSelisih(items))
    }

    @Test
    fun `daftar kosong tidak bermasalah`() {
        assertFalse(adaSelisih(emptyList()))
    }
}
```

- [ ] **Step 3: Jalankan test, pastikan gagal**

Dari PowerShell:

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*StatusSuratJalanTest*"
```

Diharapkan: GAGAL kompilasi dengan `Unresolved reference: StatusSuratJalan`.

- [ ] **Step 4: Tulis implementasi minimal**

Buat `StatusSuratJalan.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

/**
 * Siklus hidup surat jalan, cermin CHECK constraint `surat_jalan_status_check`:
 * draft -> dikirim -> diterima_lengkap/diterima_sebagian -> selesai.
 *
 * `dikirim_lengkap` adalah varian lama dari `dikirim` yang masih ada di data
 * produksi; keduanya diperlakukan sama persis, termasuk labelnya.
 */
enum class StatusSuratJalan(val nilai: String, val label: String) {
    DRAFT("draft", "Draft"),
    DIKIRIM("dikirim", "Dalam Transit"),
    DIKIRIM_LENGKAP("dikirim_lengkap", "Dalam Transit"),
    DITERIMA_SEBAGIAN("diterima_sebagian", "Diterima Sebagian"),
    DITERIMA_LENGKAP("diterima_lengkap", "Diterima Lengkap"),
    SELESAI("selesai", "Selesai");

    companion object {
        /** Nilai tak dikenal mengembalikan null, bukan melempar: satu baris lama
         *  di database tidak boleh membuat seluruh layar gagal dimuat. */
        fun dari(nilai: String?): StatusSuratJalan? = entries.find { it.nilai == nilai }
    }
}

/** Surat jalan yang masih bisa dibuka di layar verifikasi penerimaan. Cermin
 *  gerbang status di dalam RPC `sign_receipt_surat_jalan`. */
val StatusSuratJalan.bolehDiverifikasi: Boolean
    get() = this == StatusSuratJalan.DIKIRIM ||
        this == StatusSuratJalan.DIKIRIM_LENGKAP ||
        this == StatusSuratJalan.DITERIMA_SEBAGIAN

/** Sudah pernah diverifikasi outlet — termasuk yang sudah ditutup pusat. */
val StatusSuratJalan.sudahDiterima: Boolean
    get() = this == StatusSuratJalan.DITERIMA_LENGKAP ||
        this == StatusSuratJalan.DITERIMA_SEBAGIAN ||
        this == StatusSuratJalan.SELESAI

/** Boleh ditutup jadi `selesai` oleh area/regional manager. */
val StatusSuratJalan.bolehDitutup: Boolean
    get() = this == StatusSuratJalan.DITERIMA_LENGKAP ||
        this == StatusSuratJalan.DITERIMA_SEBAGIAN

/** Kontrak minimal yang dibutuhkan `adaSelisih` — dipenuhi `SuratJalanItem`
 *  maupun proyeksi ringkas yang dipakai daftar. */
interface PenandaSelisih {
    val qtyDikirim: Double
    val qtyTerima: Double?
    val kondisi: String?
}

/**
 * Cermin `has_problem` di `useSuratJalanList.ts` dan `useRiwayatList.ts`:
 * item rusak, atau qty terima kurang dari qty dikirim.
 *
 * `qtyTerima == null` berarti belum diverifikasi, bukan kurang — pemeriksaan
 * null harus mendahului perbandingan.
 */
fun adaSelisih(items: List<PenandaSelisih>): Boolean = items.any { item ->
    val terima = item.qtyTerima
    item.kondisi == "rusak" || (terima != null && terima < item.qtyDikirim)
}
```

- [ ] **Step 5: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*StatusSuratJalanTest*"
```

Diharapkan: LULUS, 12 test.

- [ ] **Step 6: Commit**

```bash
git add feature/distribusi/build.gradle.kts feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/StatusSuratJalan.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/StatusSuratJalanTest.kt
git commit -m "feat(distribusi): status surat jalan dan aturan ada-selisih"
```

---

### Task 2: Konversi satuan distribusi

Kolom `qty_dikirim` dan `qty_terima` selalu dalam satuan dasar (`bahan_baku.satuan`), sedangkan layar selalu menampilkan satuan distribusi (`bahan_baku.satuan_distribusi`). Task ini memuat satu-satunya tempat konversi itu terjadi, meniru `getDistribusiFactor` di `SuratJalanForm.tsx` dan `VerifikasiForm.tsx` persis.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/SatuanDistribusi.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/SatuanDistribusiTest.kt`

**Interfaces:**
- Consumes: tidak ada.
- Produces:
  - `data class BahanBakuMeta(val id: String, val nama: String, val satuan: String, val satuanDistribusi: String?, val satuanTengah: String?, val satuanKecil: String?, val faktorTengah: Double?, val faktorTampilan: Double?, val kategori: String?)`
  - `object SatuanDistribusi` dengan `faktor(b: BahanBakuMeta): Double`, `keTampilan(qtyDasar: Double, b: BahanBakuMeta): Long`, `keDasar(qtyTampilan: Double, b: BahanBakuMeta): Double`, `satuanTampil(b: BahanBakuMeta): String`

- [ ] **Step 1: Tulis test yang gagal**

Buat `SatuanDistribusiTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SatuanDistribusiTest {

    private fun meta(
        satuan: String,
        satuanDistribusi: String? = null,
        satuanTengah: String? = null,
        satuanKecil: String? = null,
        faktorTengah: Double? = null,
        faktorTampilan: Double? = null,
    ) = BahanBakuMeta(
        id = "b1",
        nama = "Uji",
        satuan = satuan,
        satuanDistribusi = satuanDistribusi,
        satuanTengah = satuanTengah,
        satuanKecil = satuanKecil,
        faktorTengah = faktorTengah,
        faktorTampilan = faktorTampilan,
        kategori = null,
    )

    @Test
    fun `tanpa satuan distribusi faktornya satu`() {
        assertEquals(1.0, SatuanDistribusi.faktor(meta("Dus")), 0.0001)
    }

    @Test
    fun `satuan distribusi sama dengan satuan dasar faktornya satu`() {
        assertEquals(1.0, SatuanDistribusi.faktor(meta("Dus", satuanDistribusi = "dus")), 0.0001)
    }

    @Test
    fun `satuan distribusi sama dengan satuan tengah memakai faktor tengah`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = 24.0)
        assertEquals(24.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    @Test
    fun `satuan distribusi sama dengan satuan kecil memakai faktor tampilan`() {
        val b = meta("Dus", satuanDistribusi = "Lembar", satuanKecil = "Lembar", faktorTampilan = 240.0)
        assertEquals(240.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    /** Pemetaan implisit di web: satuan distribusi "kg" dengan satuan kecil "gram".
     *  1 karung = 25000 gram = 25 kg, jadi faktornya 25000/1000. */
    @Test
    fun `kg dengan satuan kecil gram membagi faktor tampilan seribu`() {
        val b = meta("Karung", satuanDistribusi = "kg", satuanKecil = "Gram", faktorTampilan = 25000.0)
        assertEquals(25.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    @Test
    fun `satuan distribusi tak dikenali jatuh ke faktor satu`() {
        val b = meta("Dus", satuanDistribusi = "Palet", satuanTengah = "Pack", faktorTengah = 24.0)
        assertEquals(1.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    /** Faktor null di database tidak boleh membuat konversi menebak. */
    @Test
    fun `faktor null jatuh ke satu`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = null)
        assertEquals(1.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    @Test
    fun `perbandingan satuan mengabaikan besar kecil huruf`() {
        val b = meta("Dus", satuanDistribusi = "PACK", satuanTengah = "pack", faktorTengah = 24.0)
        assertEquals(24.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    @Test
    fun `ke tampilan mengalikan faktor lalu membulatkan`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = 24.0)
        // 0,5 Dus = 12 Pack
        assertEquals(12L, SatuanDistribusi.keTampilan(0.5, b))
        // 0,2083 Dus = 4,999 Pack -> dibulatkan jadi 5, sama dengan Math.round di web
        assertEquals(5L, SatuanDistribusi.keTampilan(5.0 / 24.0, b))
    }

    @Test
    fun `ke dasar membagi faktor tanpa pembulatan`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = 24.0)
        assertEquals(0.5, SatuanDistribusi.keDasar(12.0, b), 0.0001)
        assertEquals(5.0 / 24.0, SatuanDistribusi.keDasar(5.0, b), 0.000001)
    }

    @Test
    fun `bolak balik pada faktor bulat kembali ke nilai semula`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = 24.0)
        val asal = 3.0
        val bolakBalik = SatuanDistribusi.keDasar(SatuanDistribusi.keTampilan(asal, b).toDouble(), b)
        assertEquals(asal, bolakBalik, 0.0001)
    }

    @Test
    fun `satuan tampil memakai satuan distribusi bila ada`() {
        assertEquals("Pack", SatuanDistribusi.satuanTampil(meta("Dus", satuanDistribusi = "Pack")))
        assertEquals("Dus", SatuanDistribusi.satuanTampil(meta("Dus")))
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*SatuanDistribusiTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: BahanBakuMeta`.

- [ ] **Step 3: Tulis implementasi minimal**

Buat `SatuanDistribusi.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

/**
 * Metadata satuan satu bahan baku, sebagaimana dibutuhkan modul Distribusi.
 * Hanya kolom yang benar-benar dipakai konversi dan tampilan yang diambil.
 */
data class BahanBakuMeta(
    val id: String,
    val nama: String,
    val satuan: String,
    val satuanDistribusi: String?,
    val satuanTengah: String?,
    val satuanKecil: String?,
    val faktorTengah: Double?,
    val faktorTampilan: Double?,
    val kategori: String?,
)

/**
 * Jembatan antara satuan dasar (yang disimpan database) dan satuan distribusi
 * (yang dilihat manusia). Cermin `getDistribusiFactor` di `SuratJalanForm.tsx`
 * dan blok `useMemo` di `VerifikasiForm.tsx` — angka yang ditulis native untuk
 * masukan yang sama HARUS identik dengan angka yang ditulis web, karena keduanya
 * mengalir ke `ledger_stok` yang sama.
 */
object SatuanDistribusi {

    fun faktor(b: BahanBakuMeta): Double {
        val dist = b.satuanDistribusi ?: return 1.0
        if (dist.equals(b.satuan, ignoreCase = true)) return 1.0

        if (dist.equals(b.satuanTengah, ignoreCase = true) && b.faktorTengah != null) {
            return b.faktorTengah
        }
        if (dist.equals(b.satuanKecil, ignoreCase = true) && b.faktorTampilan != null) {
            return b.faktorTampilan
        }
        // Pemetaan implisit yang ada di web: satuan distribusi "kg" sementara
        // satuan kecilnya "gram". Faktor tampilan dinyatakan dalam gram, jadi
        // harus dibagi seribu dulu untuk mendapatkan faktor per kilogram.
        if (dist.equals("kg", ignoreCase = true) &&
            b.satuanKecil.equals("gram", ignoreCase = true) &&
            b.faktorTampilan != null
        ) {
            return b.faktorTampilan / 1000.0
        }
        return 1.0
    }

    /**
     * Satuan dasar -> satuan distribusi untuk ditampilkan. Dibulatkan, meniru
     * `Math.round` di web, supaya angka di HP identik dengan angka di dokumen
     * cetak dan di layar web.
     */
    fun keTampilan(qtyDasar: Double, b: BahanBakuMeta): Long = Math.round(qtyDasar * faktor(b))

    /** Satuan distribusi -> satuan dasar untuk ditulis ke database. Tidak
     *  dibulatkan: pembulatan di sini akan menggeser saldo ledger. */
    fun keDasar(qtyTampilan: Double, b: BahanBakuMeta): Double = qtyTampilan / faktor(b)

    /** Label satuan yang ditampilkan di samping angka. */
    fun satuanTampil(b: BahanBakuMeta): String =
        b.satuanDistribusi?.takeIf { it.isNotBlank() } ?: b.satuan
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*SatuanDistribusiTest*"
```

Diharapkan: LULUS, 12 test.

- [ ] **Step 5: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/SatuanDistribusi.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/SatuanDistribusiTest.kt
git commit -m "feat(distribusi): konversi satuan dasar ke satuan distribusi"
```

---

### Task 3: Hak akses per role

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/DistribusiAkses.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/DistribusiAksesTest.kt`

**Interfaces:**
- Consumes: `com.sukashawarma.superapp.domain.model.Role` dari `:core:roles`.
- Produces:
  - `object DistribusiAkses` dengan `val ROLE_MODUL: Set<Role>`, `bolehMembuka(role: Role?): Boolean`, `bolehVerifikasi(role: Role?): Boolean`, `bolehTutupDokumen(role: Role?): Boolean`, `bolehLihatKodeVerifikasi(role: Role?): Boolean`

- [ ] **Step 1: Tulis test yang gagal**

Buat `DistribusiAksesTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistribusiAksesTest {

    @Test
    fun `keempat role native boleh membuka modul`() {
        assertTrue(DistribusiAkses.bolehMembuka(Role.CREW))
        assertTrue(DistribusiAkses.bolehMembuka(Role.LEADER))
        assertTrue(DistribusiAkses.bolehMembuka(Role.AREA_MANAGER))
        assertTrue(DistribusiAkses.bolehMembuka(Role.REGIONAL_MANAGER))
    }

    /** Kitchen dan admin tetap memakai versi web; modul native tidak untuk mereka. */
    @Test
    fun `kitchen dan admin tidak membuka modul native`() {
        assertFalse(DistribusiAkses.bolehMembuka(Role.KITCHEN))
        assertFalse(DistribusiAkses.bolehMembuka(Role.ADMIN))
        assertFalse(DistribusiAkses.bolehMembuka(Role.OWNER))
        assertFalse(DistribusiAkses.bolehMembuka(Role.MITRA))
    }

    @Test
    fun `role tak dikenal ditolak, bukan diloloskan`() {
        assertFalse(DistribusiAkses.bolehMembuka(null))
        assertFalse(DistribusiAkses.bolehVerifikasi(null))
        assertFalse(DistribusiAkses.bolehTutupDokumen(null))
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(null))
    }

    @Test
    fun `hanya crew dan leader yang memverifikasi penerimaan`() {
        assertTrue(DistribusiAkses.bolehVerifikasi(Role.CREW))
        assertTrue(DistribusiAkses.bolehVerifikasi(Role.LEADER))
        assertFalse(DistribusiAkses.bolehVerifikasi(Role.AREA_MANAGER))
        assertFalse(DistribusiAkses.bolehVerifikasi(Role.REGIONAL_MANAGER))
    }

    @Test
    fun `hanya area dan regional manager yang menutup dokumen`() {
        assertTrue(DistribusiAkses.bolehTutupDokumen(Role.AREA_MANAGER))
        assertTrue(DistribusiAkses.bolehTutupDokumen(Role.REGIONAL_MANAGER))
        assertFalse(DistribusiAkses.bolehTutupDokumen(Role.CREW))
        assertFalse(DistribusiAkses.bolehTutupDokumen(Role.LEADER))
    }

    /** Kalau crew bisa membaca kode verifikasi di layar, gerbang scan QR kehilangan
     *  maknanya: dia bisa membuka verifikasi tanpa memegang dokumen fisik. */
    @Test
    fun `kode verifikasi disembunyikan dari crew dan leader`() {
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(Role.CREW))
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(Role.LEADER))
        assertTrue(DistribusiAkses.bolehLihatKodeVerifikasi(Role.AREA_MANAGER))
        assertTrue(DistribusiAkses.bolehLihatKodeVerifikasi(Role.REGIONAL_MANAGER))
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*DistribusiAksesTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: DistribusiAkses`.

- [ ] **Step 3: Tulis implementasi minimal**

Buat `DistribusiAkses.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Satu-satunya tempat role dibedakan di modul Distribusi.
 *
 * CAKUPAN OUTLET TIDAK DIPUTUSKAN DI SINI. Outlet mana yang terlihat oleh
 * seorang pengguna ditentukan RPC `accessible_outlet_ids()` di database, supaya
 * perubahan kebijakan tidak menuntut rilis APK baru. Yang ada di file ini hanya
 * kemampuan — dua hal saja, dan keduanya betul-betul berbeda antar role.
 */
object DistribusiAkses {

    /** Role yang modul Distribusi native-nya terbuka. `kitchen` dan `admin`
     *  sengaja tidak masuk: penerbitan surat jalan tetap di web. */
    val ROLE_MODUL: Set<Role> = setOf(
        Role.CREW,
        Role.LEADER,
        Role.AREA_MANAGER,
        Role.REGIONAL_MANAGER,
    )

    fun bolehMembuka(role: Role?): Boolean = role in ROLE_MODUL

    /** Menerima barang adalah pekerjaan orang yang berdiri di outlet. */
    fun bolehVerifikasi(role: Role?): Boolean =
        role == Role.CREW || role == Role.LEADER

    /** Menutup dokumen jadi `selesai` adalah pekerjaan pengawas. */
    fun bolehTutupDokumen(role: Role?): Boolean =
        role == Role.AREA_MANAGER || role == Role.REGIONAL_MANAGER

    /** Kode/QR verifikasi hanya boleh dilihat pengawas — lihat komentar
     *  gerbang QR di spec §3. */
    fun bolehLihatKodeVerifikasi(role: Role?): Boolean = bolehTutupDokumen(role)
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*DistribusiAksesTest*"
```

Diharapkan: LULUS, 6 test.

- [ ] **Step 5: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/DistribusiAkses.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/DistribusiAksesTest.kt
git commit -m "feat(distribusi): hak akses per role"
```

---

### Task 4: Aturan validasi verifikasi per item

Aturan ini menentukan kapan crew boleh menandai satu item selesai dan lanjut ke item berikutnya. Diambil persis dari `handleBaik`, `handleTidakSesuaiConfirm`, dan `handleAdvance` di `VerifikasiForm.tsx`.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/ValidasiVerifikasi.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/ValidasiVerifikasiTest.kt`

**Interfaces:**
- Consumes: tidak ada.
- Produces:
  - `enum class KondisiItem(val nilaiDb: String) { BAIK("baik"), TIDAK_SESUAI("rusak") }`
  - `data class IsianVerifikasi(val qtyTerima: Double?, val kondisi: KondisiItem, val catatan: String, val fotoPath: String?)`
  - `sealed interface HasilValidasi { data object Lolos : HasilValidasi; data class Tolak(val pesan: String) : HasilValidasi }`
  - `object ValidasiVerifikasi` dengan `konfirmasiKondisi(isian: IsianVerifikasi, qtyDikirimTampil: Long): HasilValidasi` dan `bolehLanjut(isian: IsianVerifikasi): HasilValidasi`

- [ ] **Step 1: Tulis test yang gagal**

Buat `ValidasiVerifikasiTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidasiVerifikasiTest {

    private fun isian(
        qtyTerima: Double? = 10.0,
        kondisi: KondisiItem = KondisiItem.BAIK,
        catatan: String = "",
        fotoPath: String? = "sj1/item1.jpg",
    ) = IsianVerifikasi(qtyTerima, kondisi, catatan, fotoPath)

    private fun pesanTolak(hasil: HasilValidasi): String =
        (hasil as HasilValidasi.Tolak).pesan

    @Test
    fun `kondisi baik dengan qty pas lolos`() {
        assertEquals(
            HasilValidasi.Lolos,
            ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = 10.0), 10L),
        )
    }

    @Test
    fun `qty kosong ditolak`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = null), 10L)
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Isi jumlah fisik yang diterima terlebih dahulu.", pesanTolak(hasil))
    }

    /** Nol sah untuk item yang tidak sesuai (barang tidak sampai sama sekali),
     *  tapi tidak masuk akal untuk item yang dinyatakan baik. */
    @Test
    fun `qty nol ditolak untuk kondisi baik`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = 0.0), 10L)
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Isi jumlah fisik yang diterima terlebih dahulu.", pesanTolak(hasil))
    }

    @Test
    fun `qty nol diterima untuk kondisi tidak sesuai bila ada catatan`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(
            isian(qtyTerima = 0.0, kondisi = KondisiItem.TIDAK_SESUAI, catatan = "Barang tidak sampai"),
            10L,
        )
        assertEquals(HasilValidasi.Lolos, hasil)
    }

    @Test
    fun `qty negatif ditolak`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = -1.0), 10L)
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Jumlah terima tidak boleh kurang dari 0.", pesanTolak(hasil))
    }

    @Test
    fun `qty melebihi kiriman ditolak`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = 11.0), 10L)
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Jumlah terima tidak boleh melebihi jumlah yang dikirim.", pesanTolak(hasil))
    }

    @Test
    fun `qty sama dengan kiriman diterima`() {
        assertEquals(
            HasilValidasi.Lolos,
            ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = 10.0), 10L),
        )
    }

    @Test
    fun `tidak sesuai tanpa catatan ditolak`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(
            isian(qtyTerima = 8.0, kondisi = KondisiItem.TIDAK_SESUAI, catatan = "   "),
            10L,
        )
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Item tidak sesuai wajib disertai catatan alasan.", pesanTolak(hasil))
    }

    @Test
    fun `tidak sesuai dengan catatan lolos`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(
            isian(qtyTerima = 8.0, kondisi = KondisiItem.TIDAK_SESUAI, catatan = "2 dus penyok"),
            10L,
        )
        assertEquals(HasilValidasi.Lolos, hasil)
    }

    @Test
    fun `lanjut tanpa foto ditolak`() {
        val hasil = ValidasiVerifikasi.bolehLanjut(isian(fotoPath = null))
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Foto bukti wajib diambil sebelum lanjut ke item berikutnya.", pesanTolak(hasil))
    }

    @Test
    fun `lanjut dengan foto lolos`() {
        assertEquals(HasilValidasi.Lolos, ValidasiVerifikasi.bolehLanjut(isian()))
    }

    @Test
    fun `kondisi dipetakan ke nilai kolom database`() {
        assertEquals("baik", KondisiItem.BAIK.nilaiDb)
        assertEquals("rusak", KondisiItem.TIDAK_SESUAI.nilaiDb)
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*ValidasiVerifikasiTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: KondisiItem`.

- [ ] **Step 3: Tulis implementasi minimal**

Buat `ValidasiVerifikasi.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

/**
 * Kondisi item sebagaimana dilihat crew, dan nilai yang ditulis ke kolom
 * `surat_jalan_item.kondisi`.
 *
 * Layar hanya menawarkan dua pilihan, sama seperti web: "Baik" dan "Tidak
 * Sesuai". Nilai `hilang_qty` ada di CHECK constraint database tapi tidak pernah
 * ditulis web, jadi native juga tidak menulisnya — bentuk baris hasil kedua
 * aplikasi harus identik.
 */
enum class KondisiItem(val nilaiDb: String) {
    BAIK("baik"),
    TIDAK_SESUAI("rusak"),
}

/** Isian crew untuk satu item. `qtyTerima` dalam SATUAN DISTRIBUSI. */
data class IsianVerifikasi(
    val qtyTerima: Double?,
    val kondisi: KondisiItem,
    val catatan: String,
    val fotoPath: String?,
)

sealed interface HasilValidasi {
    data object Lolos : HasilValidasi
    data class Tolak(val pesan: String) : HasilValidasi
}

/**
 * Aturan boleh-lanjut per item. Cermin `handleBaik`, `handleTidakSesuaiConfirm`,
 * dan `handleAdvance` di `VerifikasiForm.tsx`.
 */
object ValidasiVerifikasi {

    /**
     * Dipanggil saat crew menekan tombol konfirmasi kondisi.
     * `qtyDikirimTampil` adalah qty kiriman yang sudah dikonversi ke satuan
     * distribusi — bandingannya harus pada satuan yang sama dengan yang diketik.
     */
    fun konfirmasiKondisi(isian: IsianVerifikasi, qtyDikirimTampil: Long): HasilValidasi {
        val qty = isian.qtyTerima
        if (qty == null || (qty == 0.0 && isian.kondisi == KondisiItem.BAIK)) {
            return HasilValidasi.Tolak("Isi jumlah fisik yang diterima terlebih dahulu.")
        }
        if (qty < 0) {
            return HasilValidasi.Tolak("Jumlah terima tidak boleh kurang dari 0.")
        }
        if (qty > qtyDikirimTampil) {
            return HasilValidasi.Tolak("Jumlah terima tidak boleh melebihi jumlah yang dikirim.")
        }
        if (isian.kondisi == KondisiItem.TIDAK_SESUAI && isian.catatan.isBlank()) {
            return HasilValidasi.Tolak("Item tidak sesuai wajib disertai catatan alasan.")
        }
        return HasilValidasi.Lolos
    }

    /** Foto bukti tidak bisa dilewati — inilah yang membuat selisih bisa
     *  ditelusuri belakangan. */
    fun bolehLanjut(isian: IsianVerifikasi): HasilValidasi =
        if (isian.fotoPath.isNullOrBlank()) {
            HasilValidasi.Tolak("Foto bukti wajib diambil sebelum lanjut ke item berikutnya.")
        } else {
            HasilValidasi.Lolos
        }
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*ValidasiVerifikasiTest*"
```

Diharapkan: LULUS, 12 test.

- [ ] **Step 5: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/ValidasiVerifikasi.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/ValidasiVerifikasiTest.kt
git commit -m "feat(distribusi): aturan validasi verifikasi per item"
```

---

### Task 5: Statistik dashboard

Perhitungan dashboard dipisah dari layar supaya bisa diuji tanpa Compose. Angkanya harus sama dengan web (`app/dashboard/page.tsx`), karena pengawas membandingkan layar HP dengan layar laptop.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/RingkasanDistribusi.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/RingkasanDistribusiTest.kt`

**Interfaces:**
- Consumes: `StatusSuratJalan`, `sudahDiterima` (Task 1).
- Produces:
  - `interface BarisRingkasan { val status: StatusSuratJalan?; val namaOutlet: String?; val adaSelisih: Boolean }`
  - `data class HitunganStatus(val draft: Int, val dikirim: Int, val diterima: Int, val selesai: Int)`
  - `data class BarisOutlet(val nama: String, val total: Int, val aktif: Int, val bermasalah: Int)`
  - `object RingkasanDistribusi` dengan `hitungStatus(baris: List<BarisRingkasan>): HitunganStatus`, `tingkatAkurasi(baris: List<BarisRingkasan>): Int`, `rincianOutlet(baris: List<BarisRingkasan>, namaBawaan: String, maksimum: Int = 6): List<BarisOutlet>`

- [ ] **Step 1: Tulis test yang gagal**

Buat `RingkasanDistribusiTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

private data class BarisUji(
    override val status: StatusSuratJalan?,
    override val namaOutlet: String?,
    override val adaSelisih: Boolean,
) : BarisRingkasan

class RingkasanDistribusiTest {

    @Test
    fun `hitungan per status memisahkan keenam nilai`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.DRAFT, "A", false),
            BarisUji(StatusSuratJalan.DIKIRIM, "A", false),
            BarisUji(StatusSuratJalan.DIKIRIM_LENGKAP, "A", false),
            BarisUji(StatusSuratJalan.DITERIMA_LENGKAP, "A", false),
            BarisUji(StatusSuratJalan.DITERIMA_SEBAGIAN, "A", true),
            BarisUji(StatusSuratJalan.SELESAI, "A", false),
        )
        val hitungan = RingkasanDistribusi.hitungStatus(baris)
        assertEquals(1, hitungan.draft)
        // `dikirim` dan `dikirim_lengkap` sama-sama "dalam transit".
        assertEquals(2, hitungan.dikirim)
        assertEquals(2, hitungan.diterima)
        assertEquals(1, hitungan.selesai)
    }

    @Test
    fun `status null tidak dihitung di mana pun`() {
        val hitungan = RingkasanDistribusi.hitungStatus(listOf(BarisUji(null, "A", false)))
        assertEquals(HitunganStatus(0, 0, 0, 0), hitungan)
    }

    @Test
    fun `akurasi seratus persen bila belum ada yang terverifikasi`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.DRAFT, "A", false),
            BarisUji(StatusSuratJalan.DIKIRIM, "A", false),
        )
        assertEquals(100, RingkasanDistribusi.tingkatAkurasi(baris))
    }

    @Test
    fun `akurasi seratus persen bila daftar kosong`() {
        assertEquals(100, RingkasanDistribusi.tingkatAkurasi(emptyList()))
    }

    @Test
    fun `akurasi membandingkan yang bermasalah terhadap yang terverifikasi`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.SELESAI, "A", false),
            BarisUji(StatusSuratJalan.DITERIMA_LENGKAP, "A", false),
            BarisUji(StatusSuratJalan.DITERIMA_SEBAGIAN, "A", true),
            BarisUji(StatusSuratJalan.DITERIMA_LENGKAP, "A", false),
            // Yang belum terverifikasi tidak ikut menghitung penyebut.
            BarisUji(StatusSuratJalan.DIKIRIM, "A", false),
        )
        // 4 terverifikasi, 1 bermasalah -> 3/4 = 75%
        assertEquals(75, RingkasanDistribusi.tingkatAkurasi(baris))
    }

    @Test
    fun `akurasi tidak pernah negatif`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.DITERIMA_SEBAGIAN, "A", true),
            BarisUji(StatusSuratJalan.DIKIRIM, "A", true),
        )
        // 1 terverifikasi, 2 bermasalah -> hasil mentahnya negatif, dijepit ke 0.
        assertEquals(0, RingkasanDistribusi.tingkatAkurasi(baris))
    }

    @Test
    fun `rincian outlet mengelompokkan dan mengurutkan menurun`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.DIKIRIM, "Outlet B", false),
            BarisUji(StatusSuratJalan.DIKIRIM, "Outlet A", false),
            BarisUji(StatusSuratJalan.SELESAI, "Outlet A", false),
            BarisUji(StatusSuratJalan.DITERIMA_SEBAGIAN, "Outlet A", true),
        )
        val rincian = RingkasanDistribusi.rincianOutlet(baris, "Gudang Pusat")
        assertEquals(2, rincian.size)
        assertEquals("Outlet A", rincian[0].nama)
        assertEquals(3, rincian[0].total)
        // Aktif = dalam transit atau sudah diterima tapi belum ditutup.
        assertEquals(2, rincian[0].aktif)
        assertEquals(1, rincian[0].bermasalah)
        assertEquals("Outlet B", rincian[1].nama)
        assertEquals(1, rincian[1].total)
    }

    @Test
    fun `outlet tanpa nama memakai nama bawaan`() {
        val rincian = RingkasanDistribusi.rincianOutlet(
            listOf(BarisUji(StatusSuratJalan.DIKIRIM, null, false)),
            "Gudang Pusat",
        )
        assertEquals("Gudang Pusat", rincian[0].nama)
    }

    @Test
    fun `rincian outlet dipotong pada batas maksimum`() {
        val baris = (1..10).map { BarisUji(StatusSuratJalan.DIKIRIM, "Outlet $it", false) }
        assertEquals(6, RingkasanDistribusi.rincianOutlet(baris, "Gudang Pusat").size)
        assertEquals(3, RingkasanDistribusi.rincianOutlet(baris, "Gudang Pusat", maksimum = 3).size)
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*RingkasanDistribusiTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: BarisRingkasan`.

- [ ] **Step 3: Tulis implementasi minimal**

Buat `RingkasanDistribusi.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

/** Kontrak minimal satu baris daftar untuk keperluan statistik. */
interface BarisRingkasan {
    val status: StatusSuratJalan?
    val namaOutlet: String?
    val adaSelisih: Boolean
}

data class HitunganStatus(
    val draft: Int,
    val dikirim: Int,
    val diterima: Int,
    val selesai: Int,
)

data class BarisOutlet(
    val nama: String,
    val total: Int,
    val aktif: Int,
    val bermasalah: Int,
)

/**
 * Statistik dashboard. Cermin blok `useMemo` di `app/dashboard/page.tsx` —
 * angkanya harus sama dengan web karena pengawas membandingkan layar HP dengan
 * layar laptop.
 */
object RingkasanDistribusi {

    fun hitungStatus(baris: List<BarisRingkasan>): HitunganStatus = HitunganStatus(
        draft = baris.count { it.status == StatusSuratJalan.DRAFT },
        dikirim = baris.count {
            it.status == StatusSuratJalan.DIKIRIM || it.status == StatusSuratJalan.DIKIRIM_LENGKAP
        },
        diterima = baris.count {
            it.status == StatusSuratJalan.DITERIMA_LENGKAP ||
                it.status == StatusSuratJalan.DITERIMA_SEBAGIAN
        },
        selesai = baris.count { it.status == StatusSuratJalan.SELESAI },
    )

    /**
     * Persentase kiriman terverifikasi yang tiba tanpa selisih.
     *
     * Penyebutnya hanya yang sudah diverifikasi: kiriman yang masih di jalan
     * belum bisa dinilai akurat atau tidak. Bila belum ada satu pun yang
     * terverifikasi, nilainya 100 — sama dengan web, dan lebih jujur daripada
     * menampilkan 0% pada outlet yang baru mulai.
     */
    fun tingkatAkurasi(baris: List<BarisRingkasan>): Int {
        val terverifikasi = baris.count { it.status?.sudahDiterima == true }
        if (terverifikasi == 0) return 100
        val bermasalah = baris.count { it.adaSelisih }
        val akurat = terverifikasi - bermasalah
        return maxOf(0, Math.round(akurat * 100.0 / terverifikasi).toInt())
    }

    /**
     * Volume per outlet, terbanyak lebih dulu. `namaBawaan` dipakai untuk baris
     * yang outletnya tidak ter-embed (biasanya karena kiriman berasal dari
     * gudang pusat).
     */
    fun rincianOutlet(
        baris: List<BarisRingkasan>,
        namaBawaan: String,
        maksimum: Int = 6,
    ): List<BarisOutlet> = baris
        .groupBy { it.namaOutlet?.takeIf { nama -> nama.isNotBlank() } ?: namaBawaan }
        .map { (nama, rows) ->
            BarisOutlet(
                nama = nama,
                total = rows.size,
                aktif = rows.count {
                    it.status == StatusSuratJalan.DIKIRIM ||
                        it.status == StatusSuratJalan.DIKIRIM_LENGKAP ||
                        it.status == StatusSuratJalan.DITERIMA_LENGKAP ||
                        it.status == StatusSuratJalan.DITERIMA_SEBAGIAN
                },
                bermasalah = rows.count { it.adaSelisih },
            )
        }
        .sortedByDescending { it.total }
        .take(maksimum)
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*RingkasanDistribusiTest*"
```

Diharapkan: LULUS, 9 test.

- [ ] **Step 5: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/RingkasanDistribusi.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/RingkasanDistribusiTest.kt
git commit -m "feat(distribusi): statistik dashboard distribusi"
```

---

### Task 6: Pemetaan galat ke pesan Indonesia

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/DistribusiError.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/DistribusiErrorTest.kt`

**Interfaces:**
- Consumes: `com.sukashawarma.superapp.data.remote.Postgrest.PostgrestException` dari `:core:network`.
- Produces: `fun distribusiErrorMessage(e: Throwable): String`

- [ ] **Step 1: Tulis test yang gagal**

Buat `DistribusiErrorTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.data.remote.Postgrest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistribusiErrorTest {

    @Test
    fun `403 jadi pesan akses outlet`() {
        val e = Postgrest.PostgrestException(403, """{"message":"permission denied"}""")
        assertEquals(
            "Anda tidak punya akses ke outlet ini. Hubungi atasan bila ini keliru.",
            distribusiErrorMessage(e),
        )
    }

    @Test
    fun `401 jadi pesan sesi berakhir`() {
        val e = Postgrest.PostgrestException(401, "JWT expired")
        assertEquals("Sesi Anda berakhir. Silakan masuk kembali.", distribusiErrorMessage(e))
    }

    /** Pesan RPC sudah berbahasa Indonesia dan sudah ditujukan ke pengguna —
     *  meneruskannya apa adanya lebih berguna daripada pesan generik. */
    @Test
    fun `pesan dari RPC diteruskan apa adanya`() {
        val e = Postgrest.PostgrestException(
            400,
            """{"code":"P0001","message":"Supir sudah menandatangani penerimaan"}""",
        )
        assertEquals("Supir sudah menandatangani penerimaan", distribusiErrorMessage(e))
    }

    @Test
    fun `body tanpa message jatuh ke pesan generik server`() {
        val e = Postgrest.PostgrestException(500, "Internal Server Error")
        assertEquals(
            "Server sedang bermasalah. Coba lagi beberapa saat lagi.",
            distribusiErrorMessage(e),
        )
    }

    @Test
    fun `tidak ada koneksi`() {
        assertEquals(
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi atau data seluler Anda.",
            distribusiErrorMessage(java.net.UnknownHostException("supabase.co")),
        )
    }

    @Test
    fun `timeout`() {
        assertEquals(
            "Server tidak merespons. Coba lagi.",
            distribusiErrorMessage(java.net.SocketTimeoutException()),
        )
    }

    @Test
    fun `exception lain tetap menghasilkan kalimat yang bisa dibaca`() {
        val pesan = distribusiErrorMessage(IllegalStateException("boom"))
        assertEquals("Terjadi kesalahan tak terduga. Coba lagi.", pesan)
        assertTrue(pesan.endsWith("."))
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*DistribusiErrorTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: distribusiErrorMessage`.

- [ ] **Step 3: Tulis implementasi minimal**

Buat `DistribusiError.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.domain

import com.google.gson.JsonParser
import com.sukashawarma.superapp.data.remote.Postgrest

/**
 * Menerjemahkan kegagalan jaringan dan kegagalan PostgREST menjadi kalimat
 * Indonesia yang menyebut tindakan yang bisa diambil pengguna. Mengikuti pola
 * `networkErrorMessage` di `AppSession`.
 *
 * RPC distribusi (`sign_receipt_surat_jalan`, `finalize_surat_jalan_and_ledger`)
 * sudah melempar pesan berbahasa Indonesia yang ditujukan ke pengguna, jadi
 * pesan itu diteruskan apa adanya alih-alih ditimpa kalimat generik.
 */
fun distribusiErrorMessage(e: Throwable): String = when (e) {
    is Postgrest.PostgrestException -> when (e.code) {
        401 -> "Sesi Anda berakhir. Silakan masuk kembali."
        403 -> "Anda tidak punya akses ke outlet ini. Hubungi atasan bila ini keliru."
        else -> pesanDariBody(e.message) ?: "Server sedang bermasalah. Coba lagi beberapa saat lagi."
    }
    is java.net.UnknownHostException ->
        "Tidak ada koneksi internet. Periksa jaringan Wi-Fi atau data seluler Anda."
    is java.net.SocketTimeoutException -> "Server tidak merespons. Coba lagi."
    is javax.net.ssl.SSLException ->
        "Gagal membangun koneksi aman. Pastikan tanggal dan waktu perangkat Anda benar."
    is java.io.IOException -> "Gagal terhubung ke server. Periksa koneksi internet."
    else -> "Terjadi kesalahan tak terduga. Coba lagi."
}

/** PostgREST membalas JSON `{"code":..,"message":..,"details":..}`. Bila bukan
 *  JSON atau tanpa `message`, kembalikan null supaya pemanggil memakai kalimat
 *  generiknya sendiri — jangan pernah menampilkan potongan JSON mentah. */
private fun pesanDariBody(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        val pesan = JsonParser.parseString(body).asJsonObject.get("message")?.asString
        pesan?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*DistribusiErrorTest*"
```

Diharapkan: LULUS, 7 test.

- [ ] **Step 5: Jalankan seluruh test modul**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest
```

Diharapkan: LULUS, 58 test dari enam kelas (StatusSuratJalan 12, SatuanDistribusi 12, DistribusiAkses 6, ValidasiVerifikasi 12, RingkasanDistribusi 9, DistribusiError 7).

- [ ] **Step 6: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/domain/DistribusiError.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/domain/DistribusiErrorTest.kt
git commit -m "feat(distribusi): pemetaan galat ke pesan berbahasa Indonesia"
```

---

### Task 7: Model data + pembacaan surat jalan

Lapisan ini hanya tahu PostgREST dan JSON. Tidak ada aturan bisnis di sini — konversi satuan dan penentuan selisih tinggal di `domain/`.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/model/DistribusiModels.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/SuratJalanRepository.kt`

**Interfaces:**
- Consumes: `StatusSuratJalan`, `PenandaSelisih`, `adaSelisih`, `BarisRingkasan` (Task 1, 5); `BahanBakuMeta` (Task 2); `Postgrest`, `optString`, `optDouble`, `optJsonArray`, `optJsonObject` dari `:core:network`.
- Produces:
  - `data class TandaTangan(val namaPenandaTangan: String, val peran: String, val waktu: String, val gambar: String?)`
  - `data class SuratJalanItem(...) : PenandaSelisih` — lihat kode
  - `data class SuratJalanRingkas(...) : BarisRingkasan` — lihat kode
  - `data class SuratJalanDetail(...)` — lihat kode
  - `enum class RentangTanggal { SEMUA, HARI_INI, TUJUH_HARI, TIGA_PULUH_HARI }`
  - `object SuratJalanRepository` dengan `outletTerjangkau(): List<String>`, `daftar(rentang: RentangTanggal): List<SuratJalanRingkas>`, `inbox(): List<SuratJalanRingkas>`, `riwayat(): List<SuratJalanRingkas>`, `detail(id: String): SuratJalanDetail?`, `cariUntukVerifikasi(kode: String): SuratJalanRingkas?`, `invalidate()`

- [ ] **Step 1: Tulis model data**

Buat `data/model/DistribusiModels.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.data.model

import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import com.sukashawarma.superapp.feature.distribusi.domain.BarisRingkasan
import com.sukashawarma.superapp.feature.distribusi.domain.PenandaSelisih
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan

/** Satu tanda tangan di dalam `signatures` atau `receipt_signatures`.
 *  `gambar` adalah data URL PNG, bisa null pada baris lama. */
data class TandaTangan(
    val namaPenandaTangan: String,
    val peran: String,
    val waktu: String,
    val gambar: String?,
)

/**
 * Satu baris `surat_jalan_item`. `qtyDikirim` dan `qtyTerima` dalam SATUAN DASAR
 * — konversi ke satuan distribusi dilakukan lapisan UI lewat `SatuanDistribusi`.
 */
data class SuratJalanItem(
    val id: String,
    val bahanBakuId: String,
    override val qtyDikirim: Double,
    override val qtyTerima: Double?,
    override val kondisi: String?,
    val catatan: String?,
    val fotoPath: String?,
    val terverifikasiPada: String?,
    val bahan: BahanBakuMeta?,
) : PenandaSelisih

/** Proyeksi ringkas untuk daftar dan dashboard. */
data class SuratJalanRingkas(
    val id: String,
    val outletId: String,
    override val status: StatusSuratJalan?,
    override val namaOutlet: String?,
    val nomorDokumen: String?,
    val dibuatPada: String?,
    override val adaSelisih: Boolean,
) : BarisRingkasan

/** Dokumen lengkap beserta itemnya. */
data class SuratJalanDetail(
    val id: String,
    val outletId: String,
    val status: StatusSuratJalan?,
    val namaOutlet: String?,
    val nomorDokumen: String?,
    val kodeVerifikasi: String?,
    val dibuatPada: String?,
    val ttdPengirim: List<TandaTangan>,
    val ttdPenerimaan: List<TandaTangan>,
    val items: List<SuratJalanItem>,
)

/** Filter rentang tanggal dashboard — cermin `DateFilter` di `useSuratJalanList.ts`. */
enum class RentangTanggal(val label: String) {
    SEMUA("Semua"),
    HARI_INI("Hari Ini"),
    TUJUH_HARI("7 Hari"),
    TIGA_PULUH_HARI("30 Hari"),
}
```

- [ ] **Step 2: Tulis repository pembacaan**

Buat `data/SuratJalanRepository.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanItem
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.feature.distribusi.domain.adaSelisih
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Akses ke domain surat jalan.
 *
 * Cakupan outlet SELALU berasal dari RPC `accessible_outlet_ids()`, tidak pernah
 * dari daftar role di dalam APK: kebijakan yang berubah di database tidak boleh
 * menuntut rilis aplikasi baru. RLS `surat_jalan_select` menegakkan hal yang
 * sama di sisi server, jadi filter di sini adalah efisiensi, bukan keamanan.
 */
object SuratJalanRepository {

    /** Kolom yang cukup untuk daftar dan penghitungan selisih. */
    private const val SELECT_RINGKAS =
        "id,outlet_id,status,created_at,document_number,outlets(name)," +
            "surat_jalan_item(qty_dikirim,qty_terima,kondisi)"

    private const val SELECT_DETAIL =
        "id,outlet_id,status,created_at,document_number,verification_code," +
            "signatures,receipt_signatures,outlets(name)," +
            "surat_jalan_item(id,bahan_baku_id,qty_dikirim,qty_terima,kondisi,catatan," +
            "foto_path,verified_at,bahan_baku(id,nama,kategori,satuan,satuan_distribusi," +
            "satuan_tengah,satuan_kecil,faktor_tengah,faktor_tampilan))"

    private const val TTL_MS = 60_000L
    private val cache = HashMap<String, Pair<Long, Any>>()

    private suspend fun <T : Any> cached(key: String, load: suspend () -> T): T {
        val hit = cache[key]
        if (hit != null && System.currentTimeMillis() - hit.first < TTL_MS) {
            @Suppress("UNCHECKED_CAST")
            return hit.second as T
        }
        val value = load()
        cache[key] = System.currentTimeMillis() to value
        return value
    }

    /** Dipanggil saat pengguna menarik untuk menyegarkan, dan setelah setiap tulis. */
    fun invalidate() = cache.clear()

    // ------------------------------------------------------------ cakupan

    suspend fun outletTerjangkau(): List<String> = cached("outlet_terjangkau") {
        val hasil = Postgrest.rpc("accessible_outlet_ids")
        if (!hasil.isJsonArray) return@cached emptyList()
        hasil.asJsonArray.mapNotNull { elemen ->
            elemen.takeIf { !it.isJsonNull }?.asString
        }
    }

    /** Format nilai untuk operator `in.` PostgREST: `in.(id1,id2)`. */
    private fun filterOutlet(ids: List<String>): Pair<String, String> =
        "outlet_id" to ids.joinToString(prefix = "in.(", postfix = ")", separator = ",")

    // ------------------------------------------------------------ daftar

    suspend fun daftar(rentang: RentangTanggal): List<SuratJalanRingkas> {
        val ids = outletTerjangkau()
        if (ids.isEmpty()) return emptyList()

        val params = mutableListOf(
            "select" to SELECT_RINGKAS,
            filterOutlet(ids),
            "order" to "created_at.desc",
        )
        batasWaktu(rentang)?.let { params += "created_at" to "gte.$it" }
        return Postgrest.select("surat_jalan", params).map { it.asJsonObject.keRingkas() }
    }

    /** Kiriman yang masih menunggu diterima outlet. */
    suspend fun inbox(): List<SuratJalanRingkas> {
        val ids = outletTerjangkau()
        if (ids.isEmpty()) return emptyList()
        return Postgrest.select(
            "surat_jalan",
            listOf(
                "select" to SELECT_RINGKAS,
                filterOutlet(ids),
                "status" to "in.(dikirim,dikirim_lengkap,diterima_sebagian)",
                "order" to "created_at.desc",
            ),
        ).map { it.asJsonObject.keRingkas() }
    }

    /** Penerimaan yang sudah diverifikasi, termasuk yang sudah ditutup pusat. */
    suspend fun riwayat(): List<SuratJalanRingkas> {
        val ids = outletTerjangkau()
        if (ids.isEmpty()) return emptyList()
        return Postgrest.select(
            "surat_jalan",
            listOf(
                "select" to SELECT_RINGKAS,
                filterOutlet(ids),
                "status" to "in.(diterima_lengkap,diterima_sebagian,selesai)",
                "order" to "created_at.desc",
            ),
        ).map { it.asJsonObject.keRingkas() }
    }

    suspend fun detail(id: String): SuratJalanDetail? {
        val baris = Postgrest.selectOne(
            "surat_jalan",
            listOf("select" to SELECT_DETAIL, "id" to "eq.$id"),
        ) ?: return null
        return baris.keDetail()
    }

    /**
     * Pencarian untuk gerbang QR. Kode 36 karakter bertanda hubung diperlakukan
     * sebagai `id` (huruf kecil), selain itu sebagai `verification_code` (huruf
     * besar) — persis seperti `navigateToVerifikasi` di `QRScanner.tsx`.
     */
    suspend fun cariUntukVerifikasi(kode: String): SuratJalanRingkas? {
        val bersih = kode.trim().substringAfterLast('/')
        if (bersih.isBlank()) return null
        val berupaUuid = bersih.length == 36 && bersih.contains('-')
        val kolom = if (berupaUuid) "id" else "verification_code"
        val nilai = if (berupaUuid) bersih.lowercase() else bersih.uppercase()
        val baris = Postgrest.selectOne(
            "surat_jalan",
            listOf("select" to SELECT_RINGKAS, kolom to "eq.$nilai"),
        ) ?: return null
        return baris.keRingkas()
    }

    // ------------------------------------------------------------ pemetaan

    private fun batasWaktu(rentang: RentangTanggal): String? {
        val zona = ZoneId.systemDefault()
        val sekarang = Instant.now()
        return when (rentang) {
            RentangTanggal.SEMUA -> null
            RentangTanggal.HARI_INI -> LocalDate.now(zona).atStartOfDay(zona).toInstant().toString()
            RentangTanggal.TUJUH_HARI -> sekarang.minus(7, ChronoUnit.DAYS).toString()
            RentangTanggal.TIGA_PULUH_HARI -> sekarang.minus(30, ChronoUnit.DAYS).toString()
        }
    }

    /** PostgREST mengembalikan relasi to-one sebagai objek, tapi beberapa versi
     *  membungkusnya dalam array satu elemen. Tangani keduanya. */
    private fun JsonObject.relasiTunggal(key: String): JsonObject? {
        optJsonObject(key)?.let { return it }
        val arr = optJsonArray(key) ?: return null
        return arr.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
    }

    private fun JsonObject.keRingkas(): SuratJalanRingkas {
        val items = (optJsonArray("surat_jalan_item") ?: JsonArray()).map { elemen ->
            val o = elemen.asJsonObject
            SuratJalanItem(
                id = o.optString("id").orEmpty(),
                bahanBakuId = o.optString("bahan_baku_id").orEmpty(),
                qtyDikirim = o.optDouble("qty_dikirim") ?: 0.0,
                qtyTerima = o.optDouble("qty_terima"),
                kondisi = o.optString("kondisi"),
                catatan = null,
                fotoPath = null,
                terverifikasiPada = null,
                bahan = null,
            )
        }
        return SuratJalanRingkas(
            id = optString("id").orEmpty(),
            outletId = optString("outlet_id").orEmpty(),
            status = StatusSuratJalan.dari(optString("status")),
            namaOutlet = relasiTunggal("outlets")?.optString("name"),
            nomorDokumen = optString("document_number"),
            dibuatPada = optString("created_at"),
            adaSelisih = adaSelisih(items),
        )
    }

    private fun JsonObject.keDetail(): SuratJalanDetail = SuratJalanDetail(
        id = optString("id").orEmpty(),
        outletId = optString("outlet_id").orEmpty(),
        status = StatusSuratJalan.dari(optString("status")),
        namaOutlet = relasiTunggal("outlets")?.optString("name"),
        nomorDokumen = optString("document_number"),
        kodeVerifikasi = optString("verification_code"),
        dibuatPada = optString("created_at"),
        ttdPengirim = keTandaTangan(optJsonArray("signatures")),
        ttdPenerimaan = keTandaTangan(optJsonArray("receipt_signatures")),
        items = (optJsonArray("surat_jalan_item") ?: JsonArray()).map { elemen ->
            val o = elemen.asJsonObject
            SuratJalanItem(
                id = o.optString("id").orEmpty(),
                bahanBakuId = o.optString("bahan_baku_id").orEmpty(),
                qtyDikirim = o.optDouble("qty_dikirim") ?: 0.0,
                qtyTerima = o.optDouble("qty_terima"),
                kondisi = o.optString("kondisi"),
                catatan = o.optString("catatan"),
                fotoPath = o.optString("foto_path"),
                terverifikasiPada = o.optString("verified_at"),
                bahan = o.relasiTunggal("bahan_baku")?.keBahanMeta(),
            )
        }.sortedBy { it.bahan?.nama ?: "" },
    )

    private fun JsonObject.keBahanMeta() = BahanBakuMeta(
        id = optString("id").orEmpty(),
        nama = optString("nama").orEmpty(),
        satuan = optString("satuan").orEmpty(),
        satuanDistribusi = optString("satuan_distribusi"),
        satuanTengah = optString("satuan_tengah"),
        satuanKecil = optString("satuan_kecil"),
        faktorTengah = optDouble("faktor_tengah"),
        faktorTampilan = optDouble("faktor_tampilan"),
        kategori = optString("kategori"),
    )

    private fun keTandaTangan(arr: JsonArray?): List<TandaTangan> =
        (arr ?: JsonArray()).mapNotNull { elemen ->
            val o = elemen.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            TandaTangan(
                namaPenandaTangan = o.optString("signed_by").orEmpty(),
                peran = o.optString("role").orEmpty(),
                waktu = o.optString("signed_at").orEmpty(),
                gambar = o.optString("signature_image"),
            )
        }
}
```

- [ ] **Step 3: Kompilasi modul**

```
.\gradlew.bat :feature:distribusi:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL. `java.time` aman dipakai karena `minSdk = 26`.

- [ ] **Step 4: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/
git commit -m "feat(distribusi): model data dan pembacaan surat jalan"
```

---

### Task 8: Simpanan draft verifikasi & penanda unlock QR

Crew memverifikasi barang sambil berdiri di depan kiriman, sering dengan sinyal jelek dan tangan penuh. Draft harus bertahan kalau aplikasi tertutup di tengah jalan. Memakai `SharedPreferences`, pola yang sama dengan `AuthPrefs` di `:core:auth` — bukan DataStore, karena yang dibutuhkan hanya baca-tulis sinkron atas satu peta kecil.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/VerifikasiDraftStore.kt`
- Modify: `app/src/main/java/com/sukashawarma/superapp/SuperappApplication.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/data/DraftSerialisasiTest.kt`

**Interfaces:**
- Consumes: `KondisiItem`, `IsianVerifikasi` (Task 4).
- Produces:
  - `data class DraftVerifikasi(val isian: Map<String, IsianVerifikasi>, val indeksItem: Int, val langkah: String, val kondisiTerkonfirmasi: Boolean)`
  - `object VerifikasiDraftStore` dengan `init(context: Context)`, `simpan(suratJalanId: String, draft: DraftVerifikasi)`, `muat(suratJalanId: String): DraftVerifikasi?`, `hapus(suratJalanId: String)`, `tandaiTerbuka(suratJalanId: String)`, `sudahTerbuka(suratJalanId: String): Boolean`
  - `internal fun draftKeJson(draft: DraftVerifikasi): String` dan `internal fun draftDariJson(teks: String): DraftVerifikasi?` — dipisah supaya bisa diuji tanpa Android

- [ ] **Step 1: Tulis test yang gagal**

Buat `DraftSerialisasiTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.data

import com.sukashawarma.superapp.feature.distribusi.domain.IsianVerifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftSerialisasiTest {

    @Test
    fun `draft bolak balik lewat json tanpa kehilangan isian`() {
        val draft = DraftVerifikasi(
            isian = mapOf(
                "item-1" to IsianVerifikasi(10.0, KondisiItem.BAIK, "", "sj1/item-1.jpg"),
                "item-2" to IsianVerifikasi(8.5, KondisiItem.TIDAK_SESUAI, "2 dus penyok", "sj1/item-2.jpg"),
            ),
            indeksItem = 1,
            langkah = "kartu",
            kondisiTerkonfirmasi = true,
        )
        val pulih = draftDariJson(draftKeJson(draft))
        assertEquals(draft, pulih)
    }

    @Test
    fun `qty null bertahan sebagai null, bukan berubah jadi nol`() {
        val draft = DraftVerifikasi(
            isian = mapOf("item-1" to IsianVerifikasi(null, KondisiItem.BAIK, "", null)),
            indeksItem = 0,
            langkah = "kartu",
            kondisiTerkonfirmasi = false,
        )
        val pulih = draftDariJson(draftKeJson(draft))
        assertNull(pulih!!.isian.getValue("item-1").qtyTerima)
    }

    @Test
    fun `json rusak menghasilkan null, bukan lemparan`() {
        assertNull(draftDariJson("{bukan json"))
        assertNull(draftDariJson(""))
    }

    @Test
    fun `draft kosong tetap bisa dipulihkan`() {
        val draft = DraftVerifikasi(emptyMap(), 0, "kartu", false)
        assertEquals(draft, draftDariJson(draftKeJson(draft)))
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*DraftSerialisasiTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: DraftVerifikasi`.

- [ ] **Step 3: Tulis implementasi**

Buat `VerifikasiDraftStore.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.distribusi.domain.IsianVerifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem

/** Isi layar verifikasi yang sedang dikerjakan, cukup untuk memulihkan
 *  keadaan persis seperti saat aplikasi ditutup. */
data class DraftVerifikasi(
    val isian: Map<String, IsianVerifikasi>,
    val indeksItem: Int,
    val langkah: String,
    val kondisiTerkonfirmasi: Boolean,
)

/**
 * Draft verifikasi dan penanda gerbang QR, disimpan lokal per surat jalan.
 *
 * Web menyimpan hal yang sama di `localStorage`. Bedanya, di HP aplikasi jauh
 * lebih sering dipindah ke latar belakang dan dibunuh sistem, jadi draft ini
 * bukan kenyamanan tambahan melainkan syarat supaya crew tidak mengulang
 * pengisian dari nol.
 *
 * Kunci selalu memuat `suratJalanId`, jadi dua surat jalan yang dikerjakan
 * berdekatan tidak saling menimpa.
 */
object VerifikasiDraftStore {
    private const val PREFS_NAME = "distribusi_verifikasi"
    private const val PREFIX_DRAFT = "draft_"
    private const val PREFIX_UNLOCK = "unlock_"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun simpan(suratJalanId: String, draft: DraftVerifikasi) {
        prefs?.edit()?.putString(PREFIX_DRAFT + suratJalanId, draftKeJson(draft))?.apply()
    }

    fun muat(suratJalanId: String): DraftVerifikasi? {
        val teks = prefs?.getString(PREFIX_DRAFT + suratJalanId, null) ?: return null
        return draftDariJson(teks)
    }

    fun hapus(suratJalanId: String) {
        prefs?.edit()
            ?.remove(PREFIX_DRAFT + suratJalanId)
            ?.remove(PREFIX_UNLOCK + suratJalanId)
            ?.apply()
    }

    /** Dipanggil setelah QR atau kode manual berhasil dicocokkan. */
    fun tandaiTerbuka(suratJalanId: String) {
        prefs?.edit()?.putBoolean(PREFIX_UNLOCK + suratJalanId, true)?.apply()
    }

    /** Layar verifikasi menolak dibuka bila ini false, termasuk saat
     *  dinavigasi langsung tanpa melewati pemindai. */
    fun sudahTerbuka(suratJalanId: String): Boolean =
        prefs?.getBoolean(PREFIX_UNLOCK + suratJalanId, false) ?: false
}

// Serialisasi dipisah dari SharedPreferences supaya bisa diuji dengan JUnit biasa.

internal fun draftKeJson(draft: DraftVerifikasi): String {
    val isian = JsonObject()
    draft.isian.forEach { (itemId, nilai) ->
        val o = JsonObject()
        if (nilai.qtyTerima == null) o.add("qty", null) else o.addProperty("qty", nilai.qtyTerima)
        o.addProperty("kondisi", nilai.kondisi.name)
        o.addProperty("catatan", nilai.catatan)
        o.addProperty("foto", nilai.fotoPath)
        isian.add(itemId, o)
    }
    val akar = JsonObject()
    akar.add("isian", isian)
    akar.addProperty("indeks", draft.indeksItem)
    akar.addProperty("langkah", draft.langkah)
    akar.addProperty("konfirmasi", draft.kondisiTerkonfirmasi)
    return akar.toString()
}

internal fun draftDariJson(teks: String): DraftVerifikasi? = try {
    val akar = JsonParser.parseString(teks).asJsonObject
    val isian = akar.optJsonObject("isian") ?: JsonObject()
    DraftVerifikasi(
        isian = isian.entrySet().associate { (itemId, elemen) ->
            val o = elemen.asJsonObject
            itemId to IsianVerifikasi(
                qtyTerima = o.optDouble("qty"),
                kondisi = KondisiItem.entries.find { it.name == o.optString("kondisi") }
                    ?: KondisiItem.BAIK,
                catatan = o.optString("catatan").orEmpty(),
                fotoPath = o.optString("foto"),
            )
        },
        indeksItem = akar.optInt("indeks") ?: 0,
        langkah = akar.optString("langkah") ?: "kartu",
        kondisiTerkonfirmasi = akar.optBoolean("konfirmasi"),
    )
} catch (e: Exception) {
    // Draft rusak jangan sampai membuat layar verifikasi gagal dibuka —
    // lebih baik mulai dari kosong daripada tidak bisa menerima barang.
    null
}
```

- [ ] **Step 4: Inisialisasi dari Application**

Di `app/src/main/java/com/sukashawarma/superapp/SuperappApplication.kt`, tambahkan import dan satu baris di `onCreate` tepat setelah `AuthPrefs.init(this)`:

```kotlin
import com.sukashawarma.superapp.feature.distribusi.data.VerifikasiDraftStore
```

```kotlin
        AuthPrefs.init(this)
        VerifikasiDraftStore.init(this)
```

Pastikan `app/build.gradle.kts` sudah memuat `implementation(project(":feature:distribusi"))`; bila belum, tambahkan di sebelah `implementation(project(":feature:stok"))`.

- [ ] **Step 5: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*DraftSerialisasiTest*"
```

Diharapkan: LULUS, 4 test.

- [ ] **Step 6: Kompilasi app**

```
.\gradlew.bat :app:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/VerifikasiDraftStore.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/data/DraftSerialisasiTest.kt app/src/main/java/com/sukashawarma/superapp/SuperappApplication.kt app/build.gradle.kts
git commit -m "feat(distribusi): simpanan draft verifikasi dan penanda unlock QR"
```

---

### Task 9: Foto bukti — kompres, unggah, ambil kembali

Foto bukti adalah satu-satunya alasan selisih bisa ditelusuri belakangan, jadi jalurnya tidak boleh gagal diam-diam. Bucket `verif-foto-bahan` bersifat privat dengan batas keras 200 KB, jadi kompresi wajib dilakukan sebelum unggah.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/FotoBuktiStore.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/data/FotoBuktiPathTest.kt`

**Interfaces:**
- Consumes: `StorageUtil` dari `:core:storage`, `SupabaseClient` dari `:core:network`.
- Produces:
  - `object FotoBuktiStore` dengan `const val BUCKET`, `const val BATAS_BYTE`, `pathUntuk(suratJalanId: String, itemId: String): String`, `suspend unggah(suratJalanId: String, itemId: String, bitmap: Bitmap): String`, `suspend ambil(path: String): ByteArray?`, `internal kompres(bitmap: Bitmap, batasByte: Int): ByteArray`

- [ ] **Step 1: Tulis test yang gagal**

Buat `FotoBuktiPathTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FotoBuktiPathTest {

    /**
     * Kolom `foto_path` di web berisi path TANPA nama bucket, sementara
     * `StorageUtil.uploadJpeg` mengembalikan "bucket/path". Kalau nama bucket
     * ikut tersimpan, foto yang diunggah dari native tidak akan bisa dibuka
     * dari web.
     */
    @Test
    fun `path tidak memuat nama bucket`() {
        val path = FotoBuktiStore.pathUntuk("sj-1", "item-9")
        assertEquals("sj-1/item-9.jpg", path)
    }

    @Test
    fun `batas byte sama dengan batas bucket`() {
        assertEquals(204800, FotoBuktiStore.BATAS_BYTE)
    }

    @Test
    fun `nama bucket persis seperti di database`() {
        assertEquals("verif-foto-bahan", FotoBuktiStore.BUCKET)
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*FotoBuktiPathTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: FotoBuktiStore`.

- [ ] **Step 3: Tulis implementasi**

Buat `FotoBuktiStore.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.data

import android.graphics.Bitmap
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.ByteArrayOutputStream

/**
 * Foto bukti penerimaan barang.
 *
 * Bucket `verif-foto-bahan` privat dengan `file_size_limit` 204800 byte yang
 * ditegakkan server: berkas yang lebih besar ditolak, bukan dipotong. Kompresi
 * karena itu wajib, dan strateginya meniru `compressImage` di `VerifikasiForm.tsx`
 * supaya mutu foto dari HP setara dengan yang dari browser.
 */
object FotoBuktiStore {

    const val BUCKET = "verif-foto-bahan"
    const val BATAS_BYTE = 204800
    private const val DIMENSI_MAKS = 1280

    /** Path objek storage, TANPA nama bucket — inilah yang masuk ke kolom
     *  `surat_jalan_item.foto_path`, sama persis dengan yang ditulis web. */
    fun pathUntuk(suratJalanId: String, itemId: String): String = "$suratJalanId/$itemId.jpg"

    /** Mengembalikan path yang harus disimpan ke `foto_path`. */
    suspend fun unggah(suratJalanId: String, itemId: String, bitmap: Bitmap): String {
        val path = pathUntuk(suratJalanId, itemId)
        val bytes = kompres(bitmap, BATAS_BYTE)
        // StorageUtil.uploadJpeg mengembalikan "bucket/path"; nilai itu sengaja
        // diabaikan supaya nama bucket tidak ikut tersimpan di kolom.
        StorageUtil.uploadJpeg(BUCKET, path, bytes)
        return path
    }

    /**
     * Mengambil foto dari bucket privat. Endpoint `authenticated` cukup karena
     * interseptor di `SupabaseClient.okHttpClient` sudah menyisipkan token
     * pengguna — tidak perlu signed URL.
     *
     * Mengembalikan null bila objeknya tidak ada, supaya satu foto yang hilang
     * tidak menggagalkan seluruh layar detail.
     */
    suspend fun ambil(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val url = "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/$BUCKET/$path"
        val req = Request.Builder().url(url).get().build()
        SupabaseClient.okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.bytes()
        }
    }

    /**
     * Turunkan resolusi lalu turunkan mutu bertahap sampai muat. Mulai dari 85
     * dan turun 10 tiap putaran sampai 20, sama dengan web — di bawah itu foto
     * bukti sudah terlalu buruk untuk jadi bukti, jadi hasil terakhir dipakai
     * apa adanya dan pemanggil yang memutuskan menolaknya.
     */
    internal fun kompres(bitmap: Bitmap, batasByte: Int): ByteArray {
        val sumber = kecilkan(bitmap)
        var mutu = 85
        var keluaran = jpeg(sumber, mutu)
        while (keluaran.size > batasByte && mutu > 20) {
            mutu -= 10
            keluaran = jpeg(sumber, mutu)
        }
        return keluaran
    }

    private fun kecilkan(bitmap: Bitmap): Bitmap {
        val lebar = bitmap.width
        val tinggi = bitmap.height
        if (lebar <= DIMENSI_MAKS && tinggi <= DIMENSI_MAKS) return bitmap
        val rasio = minOf(DIMENSI_MAKS.toFloat() / lebar, DIMENSI_MAKS.toFloat() / tinggi)
        return Bitmap.createScaledBitmap(
            bitmap,
            Math.round(lebar * rasio),
            Math.round(tinggi * rasio),
            true,
        )
    }

    private fun jpeg(bitmap: Bitmap, mutu: Int): ByteArray {
        val keluaran = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, mutu, keluaran)
        return keluaran.toByteArray()
    }
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*FotoBuktiPathTest*"
```

Diharapkan: LULUS, 3 test.

- [ ] **Step 5: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/FotoBuktiStore.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/data/FotoBuktiPathTest.kt
git commit -m "feat(distribusi): kompres, unggah, dan ambil foto bukti"
```

---

### Task 10: Penulisan — verifikasi item, tanda tangan, finalisasi, tutup dokumen

Ini satu-satunya task yang menulis ke database produksi. Baca ulang bagian **Global Constraints** sebelum mulai: kolom `selisih`, `harga_snapshot`, dan `verified_by` tidak boleh disebut sama sekali, dan `ledger_stok` hanya boleh ditulis oleh RPC.

**Files:**
- Modify: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/SuratJalanRepository.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/data/PatchVerifikasiTest.kt`

**Interfaces:**
- Consumes: `Postgrest`, `KondisiItem` (Task 4), `TandaTangan` (Task 7).
- Produces, ditambahkan ke `object SuratJalanRepository`:
  - `internal fun patchVerifikasi(qtyTerimaDasar: Double, qtyDikirimDasar: Double, kondisi: KondisiItem, catatan: String, fotoPath: String?, waktuIso: String): JsonObject`
  - `suspend fun simpanVerifikasiItem(itemId: String, qtyTerimaDasar: Double, qtyDikirimDasar: Double, kondisi: KondisiItem, catatan: String, fotoPath: String?)`
  - `suspend fun tandaTanganPenerimaan(suratJalanId: String, nama: String, peran: String, gambar: String): List<TandaTangan>`
  - `data class HasilFinalisasi(val sukses: Boolean, val pesan: String, val statusAkhir: String?)`
  - `suspend fun finalisasi(suratJalanId: String): HasilFinalisasi`
  - `suspend fun tutupDokumen(suratJalanId: String)`
  - `const val PERAN_CREW = "Crew Penerima"`, `const val PERAN_SUPIR = "Supir"`

- [ ] **Step 1: Tulis test yang gagal**

Buat `PatchVerifikasiTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.data

import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatchVerifikasiTest {

    private val waktu = "2026-09-04T10:00:00Z"

    @Test
    fun `qty pas dan kondisi baik tidak ditandai`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            qtyTerimaDasar = 10.0,
            qtyDikirimDasar = 10.0,
            kondisi = KondisiItem.BAIK,
            catatan = "",
            fotoPath = "sj1/item1.jpg",
            waktuIso = waktu,
        )
        assertEquals(10.0, patch.get("qty_terima").asDouble, 0.0001)
        assertEquals("baik", patch.get("kondisi").asString)
        assertFalse(patch.get("flagged").asBoolean)
        assertEquals("sj1/item1.jpg", patch.get("foto_path").asString)
        assertEquals(waktu, patch.get("verified_at").asString)
    }

    @Test
    fun `catatan kosong ditulis null, bukan string kosong`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            10.0, 10.0, KondisiItem.BAIK, "  ", "sj1/item1.jpg", waktu,
        )
        assertTrue(patch.get("catatan").isJsonNull)
    }

    @Test
    fun `qty kurang menandai flagged walau kondisinya baik`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            9.0, 10.0, KondisiItem.BAIK, "", "sj1/item1.jpg", waktu,
        )
        assertTrue(patch.get("flagged").asBoolean)
    }

    @Test
    fun `kondisi tidak sesuai menandai flagged walau qty pas`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            10.0, 10.0, KondisiItem.TIDAK_SESUAI, "penyok", "sj1/item1.jpg", waktu,
        )
        assertTrue(patch.get("flagged").asBoolean)
        assertEquals("rusak", patch.get("kondisi").asString)
        assertEquals("penyok", patch.get("catatan").asString)
    }

    /** Kolom turunan dan kolom yang tidak ditulis web tidak boleh ikut terkirim.
     *  `selisih` ditolak Postgres, `harga_snapshot` milik trigger, dan
     *  `verified_by` tidak pernah diisi web — bentuk baris harus identik. */
    @Test
    fun `kolom terlarang tidak pernah disebut`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            9.0, 10.0, KondisiItem.TIDAK_SESUAI, "kurang", "sj1/item1.jpg", waktu,
        )
        assertFalse(patch.has("selisih"))
        assertFalse(patch.has("harga_snapshot"))
        assertFalse(patch.has("verified_by"))
        assertFalse(patch.has("id"))
        assertFalse(patch.has("surat_jalan_id"))
        assertFalse(patch.has("qty_dikirim"))
    }

    @Test
    fun `patch memuat tepat enam kolom`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            10.0, 10.0, KondisiItem.BAIK, "", "sj1/item1.jpg", waktu,
        )
        assertEquals(
            setOf("qty_terima", "kondisi", "catatan", "flagged", "foto_path", "verified_at"),
            patch.keySet(),
        )
    }

    @Test
    fun `foto path null tetap ditulis null`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            10.0, 10.0, KondisiItem.BAIK, "", null, waktu,
        )
        assertTrue(patch.get("foto_path").isJsonNull)
    }

    @Test
    fun `peran tanda tangan persis seperti yang diterima RPC`() {
        assertEquals("Crew Penerima", SuratJalanRepository.PERAN_CREW)
        assertEquals("Supir", SuratJalanRepository.PERAN_SUPIR)
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*PatchVerifikasiTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: patchVerifikasi`.

- [ ] **Step 3: Tambahkan operasi tulis ke repository**

Tambahkan import berikut di bagian atas `SuratJalanRepository.kt`:

```kotlin
import com.google.gson.JsonNull
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
```

Lalu tambahkan blok berikut di dalam `object SuratJalanRepository`, setelah bagian pemetaan:

```kotlin
    // ------------------------------------------------------------ menulis

    /** Peran TTD penerimaan yang diterima RPC `sign_receipt_surat_jalan`.
     *  Nilai di luar keduanya ditolak server dengan exception. */
    const val PERAN_CREW = "Crew Penerima"
    const val PERAN_SUPIR = "Supir"

    /**
     * Membentuk badan PATCH untuk satu `surat_jalan_item`.
     *
     * Dipisah dari pemanggilan jaringan supaya bisa diuji: inilah satu-satunya
     * tempat yang menentukan kolom apa saja yang tersentuh, dan daftar itu harus
     * persis sama dengan yang ditulis `handleSubmit` di `VerifikasiForm.tsx`.
     *
     * `qtyTerimaDasar` dan `qtyDikirimDasar` keduanya dalam SATUAN DASAR — bandingan
     * `flagged` harus pada satuan yang sama, bukan mencampur satuan distribusi.
     */
    internal fun patchVerifikasi(
        qtyTerimaDasar: Double,
        qtyDikirimDasar: Double,
        kondisi: KondisiItem,
        catatan: String,
        fotoPath: String?,
        waktuIso: String,
    ): JsonObject {
        val patch = JsonObject()
        patch.addProperty("qty_terima", qtyTerimaDasar)
        patch.addProperty("kondisi", kondisi.nilaiDb)
        if (catatan.isBlank()) patch.add("catatan", JsonNull.INSTANCE)
        else patch.addProperty("catatan", catatan)
        patch.addProperty(
            "flagged",
            qtyTerimaDasar != qtyDikirimDasar || kondisi == KondisiItem.TIDAK_SESUAI,
        )
        if (fotoPath.isNullOrBlank()) patch.add("foto_path", JsonNull.INSTANCE)
        else patch.addProperty("foto_path", fotoPath)
        patch.addProperty("verified_at", waktuIso)
        return patch
    }

    suspend fun simpanVerifikasiItem(
        itemId: String,
        qtyTerimaDasar: Double,
        qtyDikirimDasar: Double,
        kondisi: KondisiItem,
        catatan: String,
        fotoPath: String?,
    ) {
        val waktu = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        Postgrest.update(
            "surat_jalan_item",
            listOf("id" to "eq.$itemId"),
            patchVerifikasi(qtyTerimaDasar, qtyDikirimDasar, kondisi, catatan, fotoPath, waktu),
        )
    }

    /**
     * Menyimpan satu tanda tangan penerimaan dan mengembalikan daftar terbaru
     * dari server. Daftar itulah yang harus jadi sumber kebenaran layar — bukan
     * salinan lokal — supaya TTD yang sudah tersimpan tetap terlihat setelah
     * aplikasi ditutup di tengah proses.
     *
     * RPC menolak status di luar penerimaan, peran di luar dua nilai yang sah,
     * dan tanda tangan ganda untuk peran yang sama. Pesan penolakannya sudah
     * berbahasa Indonesia dan diteruskan apa adanya oleh `distribusiErrorMessage`.
     */
    suspend fun tandaTanganPenerimaan(
        suratJalanId: String,
        nama: String,
        peran: String,
        gambar: String,
    ): List<TandaTangan> {
        val body = JsonObject()
        body.addProperty("p_surat_jalan_id", suratJalanId)
        body.addProperty("p_signed_by_name", nama)
        body.addProperty("p_role", peran)
        body.addProperty("p_signature_image", gambar)
        val hasil = Postgrest.rpc("sign_receipt_surat_jalan", body)
        invalidate()
        val obj = hasil.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
        return keTandaTangan(obj.optJsonArray("receipt_signatures"))
    }

    data class HasilFinalisasi(val sukses: Boolean, val pesan: String, val statusAkhir: String?)

    /**
     * Menutup verifikasi: RPC menulis `ledger_stok` dan menetapkan status akhir.
     * Aplikasi tidak pernah menghitung status akhir sendiri.
     *
     * Idempoten di sisi server: pemanggilan kedua untuk surat jalan yang sudah
     * diverifikasi mengembalikan `success:false` dengan pesan "sudah diverifikasi
     * sebelumnya". Pemanggil harus memperlakukan kasus itu sebagai berhasil —
     * artinya percobaan sebelumnya sampai ke server walau jaringannya putus.
     */
    suspend fun finalisasi(suratJalanId: String): HasilFinalisasi {
        val body = JsonObject()
        body.addProperty("p_surat_jalan_id", suratJalanId)
        val hasil = Postgrest.rpc("finalize_surat_jalan_and_ledger", body)
        invalidate()
        val obj = hasil.takeIf { it.isJsonObject }?.asJsonObject
            ?: return HasilFinalisasi(false, "Balasan server tidak dikenali.", null)
        return HasilFinalisasi(
            sukses = obj.optBoolean("success"),
            pesan = obj.optString("message").orEmpty(),
            statusAkhir = obj.optString("status"),
        )
    }

    /**
     * Menutup dokumen jadi `selesai`. Hanya area/regional manager yang boleh,
     * dan hanya untuk dokumen yang sudah diverifikasi outlet — kedua syarat itu
     * ditegakkan pemanggil lewat `DistribusiAkses` dan `bolehDitutup`.
     *
     * RLS `surat_jalan_update_scoped` adalah jaring pengaman terakhirnya di server.
     */
    suspend fun tutupDokumen(suratJalanId: String) {
        val patch = JsonObject()
        patch.addProperty("status", StatusSuratJalan.SELESAI.nilai)
        patch.addProperty(
            "updated_at",
            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
        Postgrest.update("surat_jalan", listOf("id" to "eq.$suratJalanId"), patch)
        invalidate()
    }
```

Tambahkan juga import `com.sukashawarma.superapp.data.remote.optBoolean` bila belum ada.

- [ ] **Step 4: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*PatchVerifikasiTest*"
```

Diharapkan: LULUS, 8 test.

- [ ] **Step 5: Jalankan seluruh test modul**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest
```

Diharapkan: LULUS, 73 test.

- [ ] **Step 6: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/data/SuratJalanRepository.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/data/PatchVerifikasiTest.kt
git commit -m "feat(distribusi): tulis verifikasi item, tanda tangan, dan finalisasi"
```

---

### Task 11: Komponen UI bersama

Empat layar menampilkan kartu surat jalan yang sama. Dikumpulkan di satu file supaya lencana status dan format tanggal tidak menyimpang antar layar.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/DistribusiComponents.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/ui/FormatTanggalTest.kt`

**Interfaces:**
- Consumes: `StatusSuratJalan` (Task 1), `SuratJalanRingkas` (Task 7).
- Produces:
  - `fun formatTanggal(iso: String?): String`
  - `@Composable fun LencanaStatus(status: StatusSuratJalan?, adaSelisih: Boolean)`
  - `@Composable fun KartuSuratJalan(baris: SuratJalanRingkas, aksiLabel: String?, onKlik: () -> Unit, onAksi: (() -> Unit)?)`
  - `@Composable fun LayarKosong(judul: String, keterangan: String)`
  - `@Composable fun LayarGalat(pesan: String, onCobaLagi: () -> Unit)`
  - `@Composable fun LayarMemuat()`

- [ ] **Step 1: Tulis test yang gagal**

Buat `FormatTanggalTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTanggalTest {

    @Test
    fun `iso dari postgrest jadi tanggal Indonesia`() {
        assertEquals("4 Sep 2026", formatTanggal("2026-09-04T03:15:00+00:00"))
    }

    @Test
    fun `iso dengan Z juga dikenali`() {
        assertEquals("4 Sep 2026", formatTanggal("2026-09-04T03:15:00Z"))
    }

    /** Baris lama bisa punya timestamp tanpa zona waktu. */
    @Test
    fun `timestamp tanpa zona tetap terbaca`() {
        assertEquals("4 Sep 2026", formatTanggal("2026-09-04T03:15:00"))
    }

    @Test
    fun `null dan teks rusak jadi tanda hubung, bukan lemparan`() {
        assertEquals("-", formatTanggal(null))
        assertEquals("-", formatTanggal(""))
        assertEquals("-", formatTanggal("bukan tanggal"))
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*FormatTanggalTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: formatTanggal`.

- [ ] **Step 3: Tulis implementasi**

Buat `DistribusiComponents.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.presentation.theme.SukaGray100
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BULAN = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

/**
 * Timestamp PostgREST -> "4 Sep 2026".
 *
 * Menerima tiga bentuk yang benar-benar muncul di data: dengan offset, dengan
 * "Z", dan tanpa zona sama sekali pada baris lama. Bentuk yang tak dikenali
 * menghasilkan tanda hubung — satu baris berformat aneh tidak boleh membuat
 * seluruh daftar gagal dirender.
 */
fun formatTanggal(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    val tanggal = try {
        OffsetDateTime.parse(iso).toLocalDate()
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
        } catch (e2: Exception) {
            return "-"
        }
    }
    return "${tanggal.dayOfMonth} ${BULAN[tanggal.monthValue - 1]} ${tanggal.year}"
}

private val HijauTeks = Color(0xFF0A7D2C)
private val HijauLatar = Color(0xFFE7F6EC)
private val BiruTeks = Color(0xFF1D4ED8)
private val BiruLatar = Color(0xFFE6EDFD)
private val MerahTeks = Color(0xFFB91C1C)
private val MerahLatar = Color(0xFFFDECEC)
private val AbuTeks = Color(0xFF6B7280)

@Composable
fun LencanaStatus(status: StatusSuratJalan?, adaSelisih: Boolean) {
    val (teks, warnaTeks, warnaLatar) = when {
        status == null -> Triple("Tidak Dikenal", AbuTeks, SukaGray100)
        adaSelisih && status.nilai.startsWith("diterima") ->
            Triple("Ada Selisih", MerahTeks, MerahLatar)
        status == StatusSuratJalan.SELESAI -> Triple(status.label, HijauTeks, HijauLatar)
        status == StatusSuratJalan.DITERIMA_LENGKAP -> Triple(status.label, HijauTeks, HijauLatar)
        status == StatusSuratJalan.DITERIMA_SEBAGIAN -> Triple(status.label, MerahTeks, MerahLatar)
        else -> Triple(status.label, BiruTeks, BiruLatar)
    }
    Surface(shape = RoundedCornerShape(50), color = warnaLatar) {
        Text(
            teks,
            Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            color = warnaTeks,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/**
 * Kartu satu surat jalan. `aksiLabel` dan `onAksi` mengisi tombol sekunder di
 * kaki kartu — dipakai dashboard untuk "Tutup Dokumen"; layar lain melewatkannya.
 */
@Composable
fun KartuSuratJalan(
    baris: SuratJalanRingkas,
    aksiLabel: String? = null,
    onKlik: () -> Unit,
    onAksi: (() -> Unit)? = null,
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SukaOrange.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SJ ${baris.nomorDokumen ?: baris.id.take(8).uppercase()}",
                    Modifier.weight(1f),
                    color = SukaOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LencanaStatus(baris.status, baris.adaSelisih)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                baris.namaOutlet ?: "Gudang Pusat",
                color = SukaGray500,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatTanggal(baris.dibuatPada), color = SukaGray500, fontSize = 11.sp)
            if (aksiLabel != null && onAksi != null) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onAksi, modifier = Modifier.fillMaxWidth()) {
                    Text(aksiLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
fun LayarKosong(judul: String, keterangan: String) {
    Column(
        Modifier.fillMaxSize().background(SukaSurface).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Inbox, null, tint = SukaGray500, modifier = Modifier.height(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(judul, color = SukaOnSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(keterangan, color = SukaGray500, fontSize = 12.sp)
    }
}

@Composable
fun LayarGalat(pesan: String, onCobaLagi: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(SukaSurface).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.WarningAmber, null, tint = MerahTeks, modifier = Modifier.height(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(pesan, color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onCobaLagi) { Text("Coba Lagi") }
    }
}

@Composable
fun LayarMemuat() {
    Box(Modifier.fillMaxSize().background(SukaSurface), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SukaOrange)
    }
}
```

- [ ] **Step 4: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*FormatTanggalTest*"
```

Diharapkan: LULUS, 4 test.

- [ ] **Step 5: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/DistribusiComponents.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/ui/FormatTanggalTest.kt
git commit -m "feat(distribusi): komponen UI bersama dan format tanggal"
```

---

### Task 12: Dashboard

Layar pertama modul. Semua role melihatnya; yang berbeda hanya luas outletnya (dari `accessible_outlet_ids()`) dan ada tidaknya tombol "Tutup Dokumen".

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/dashboard/DashboardViewModel.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/dashboard/DashboardScreen.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/ui/dashboard/SaringDaftarTest.kt`

**Interfaces:**
- Consumes: `SuratJalanRepository`, `SuratJalanRingkas`, `RentangTanggal` (Task 7, 10); `RingkasanDistribusi`, `HitunganStatus`, `BarisOutlet`, `DistribusiAkses`, `bolehDitutup`, `distribusiErrorMessage` (Task 1, 3, 5, 6); komponen Task 11.
- Produces:
  - `enum class TabStatus(val label: String) { SEMUA, DRAFT, DIKIRIM, BELUM_VERIF, SELISIH, SELESAI }`
  - `fun saringDaftar(sumber: List<SuratJalanRingkas>, tab: TabStatus, outlet: String?, cari: String): List<SuratJalanRingkas>`
  - `data class DashboardUiState(...)`
  - `class DashboardViewModel : ViewModel()`
  - `@Composable fun DashboardScreen(onKeluar: () -> Unit, onBukaInbox: () -> Unit, onBukaRiwayat: () -> Unit, onBukaDetail: (String) -> Unit, viewModel: DashboardViewModel = viewModel())`

- [ ] **Step 1: Tulis test yang gagal**

Buat `SaringDaftarTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.dashboard

import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import org.junit.Assert.assertEquals
import org.junit.Test

class SaringDaftarTest {

    private fun baris(
        id: String,
        status: StatusSuratJalan,
        outlet: String? = "Outlet A",
        nomor: String? = "SJ-001",
        selisih: Boolean = false,
    ) = SuratJalanRingkas(id, "o1", status, outlet, nomor, "2026-09-04T00:00:00Z", selisih)

    private val sumber = listOf(
        baris("1", StatusSuratJalan.DRAFT, nomor = "SJ-001"),
        baris("2", StatusSuratJalan.DIKIRIM, nomor = "SJ-002"),
        baris("3", StatusSuratJalan.DIKIRIM_LENGKAP, nomor = "SJ-003"),
        baris("4", StatusSuratJalan.DITERIMA_LENGKAP, nomor = "SJ-004"),
        baris("5", StatusSuratJalan.DITERIMA_SEBAGIAN, nomor = "SJ-005", selisih = true),
        baris("6", StatusSuratJalan.SELESAI, outlet = "Outlet B", nomor = "SJ-006"),
    )

    @Test
    fun `tab semua tidak menyaring apa pun`() {
        assertEquals(6, saringDaftar(sumber, TabStatus.SEMUA, null, "").size)
    }

    @Test
    fun `tab dikirim mencakup dikirim lengkap`() {
        val hasil = saringDaftar(sumber, TabStatus.DIKIRIM, null, "")
        assertEquals(listOf("2", "3"), hasil.map { it.id })
    }

    @Test
    fun `tab belum verifikasi hanya yang sudah diterima tapi belum ditutup`() {
        val hasil = saringDaftar(sumber, TabStatus.BELUM_VERIF, null, "")
        assertEquals(listOf("4", "5"), hasil.map { it.id })
    }

    @Test
    fun `tab selisih hanya yang bermasalah`() {
        assertEquals(listOf("5"), saringDaftar(sumber, TabStatus.SELISIH, null, "").map { it.id })
    }

    @Test
    fun `filter outlet cocok persis nama`() {
        assertEquals(listOf("6"), saringDaftar(sumber, TabStatus.SEMUA, "Outlet B", "").map { it.id })
    }

    @Test
    fun `pencarian mencocokkan nomor dokumen tanpa peduli besar kecil huruf`() {
        assertEquals(listOf("4"), saringDaftar(sumber, TabStatus.SEMUA, null, "sj-004").map { it.id })
    }

    @Test
    fun `pencarian juga mencocokkan nama outlet`() {
        assertEquals(listOf("6"), saringDaftar(sumber, TabStatus.SEMUA, null, "outlet b").map { it.id })
    }

    @Test
    fun `pencarian yang hanya spasi diabaikan`() {
        assertEquals(6, saringDaftar(sumber, TabStatus.SEMUA, null, "   ").size)
    }

    @Test
    fun `tab dan pencarian digabung, bukan saling menggantikan`() {
        val hasil = saringDaftar(sumber, TabStatus.BELUM_VERIF, null, "SJ-005")
        assertEquals(listOf("5"), hasil.map { it.id })
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*SaringDaftarTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: TabStatus`.

- [ ] **Step 3: Tulis ViewModel**

Buat `DashboardViewModel.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.BarisOutlet
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.HitunganStatus
import com.sukashawarma.superapp.feature.distribusi.domain.RingkasanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDitutup
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Tab status dashboard — cermin `StatusTab` di `app/dashboard/page.tsx`. */
enum class TabStatus(val label: String) {
    SEMUA("Semua"),
    DRAFT("Draft"),
    DIKIRIM("Dikirim"),
    BELUM_VERIF("Belum Diverifikasi"),
    SELISIH("Ada Selisih"),
    SELESAI("Selesai"),
}

/**
 * Penyaringan dipisah dari ViewModel supaya bisa diuji tanpa coroutine.
 * Ketiga filter digabung dengan AND, sama seperti `filteredShipments` di web.
 */
fun saringDaftar(
    sumber: List<SuratJalanRingkas>,
    tab: TabStatus,
    outlet: String?,
    cari: String,
): List<SuratJalanRingkas> {
    val kunci = cari.trim().lowercase()
    return sumber.filter { baris ->
        val cocokTab = when (tab) {
            TabStatus.SEMUA -> true
            TabStatus.DRAFT -> baris.status == StatusSuratJalan.DRAFT
            TabStatus.DIKIRIM -> baris.status == StatusSuratJalan.DIKIRIM ||
                baris.status == StatusSuratJalan.DIKIRIM_LENGKAP
            TabStatus.BELUM_VERIF -> baris.status?.bolehDitutup == true
            TabStatus.SELISIH -> baris.adaSelisih
            TabStatus.SELESAI -> baris.status == StatusSuratJalan.SELESAI
        }
        val cocokOutlet = outlet == null || baris.namaOutlet == outlet
        val cocokCari = kunci.isEmpty() ||
            (baris.nomorDokumen ?: baris.id).lowercase().contains(kunci) ||
            (baris.namaOutlet ?: "").lowercase().contains(kunci)
        cocokTab && cocokOutlet && cocokCari
    }
}

data class DashboardUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    val semua: List<SuratJalanRingkas> = emptyList(),
    val terlihat: List<SuratJalanRingkas> = emptyList(),
    val hitungan: HitunganStatus = HitunganStatus(0, 0, 0, 0),
    val akurasi: Int = 100,
    val rincianOutlet: List<BarisOutlet> = emptyList(),
    val rentang: RentangTanggal = RentangTanggal.SEMUA,
    val tab: TabStatus = TabStatus.SEMUA,
    val cari: String = "",
    val outletTerpilih: String? = null,
    val bolehTutupDokumen: Boolean = false,
    val sedangMenutup: String? = null,
    val namaPengguna: String = "",
)

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        val staff = AppSession.staff.value
        _state.value = _state.value.copy(
            bolehTutupDokumen = DistribusiAkses.bolehTutupDokumen(staff?.role),
            namaPengguna = staff?.name.orEmpty(),
        )
        muat()
    }

    fun muat(paksa: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            if (paksa) SuratJalanRepository.invalidate()
            try {
                val daftar = SuratJalanRepository.daftar(_state.value.rentang)
                _state.value = _state.value.copy(
                    memuat = false,
                    semua = daftar,
                    hitungan = RingkasanDistribusi.hitungStatus(daftar),
                    akurasi = RingkasanDistribusi.tingkatAkurasi(daftar),
                    rincianOutlet = RingkasanDistribusi.rincianOutlet(daftar, "Gudang Pusat"),
                )
                terapkanFilter()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    fun ubahRentang(rentang: RentangTanggal) {
        _state.value = _state.value.copy(rentang = rentang)
        muat(paksa = true)
    }

    fun ubahTab(tab: TabStatus) {
        _state.value = _state.value.copy(tab = tab)
        terapkanFilter()
    }

    fun ubahCari(teks: String) {
        _state.value = _state.value.copy(cari = teks)
        terapkanFilter()
    }

    /** Menekan outlet yang sama dua kali melepas filternya. */
    fun pilihOutlet(nama: String?) {
        val sekarang = _state.value.outletTerpilih
        _state.value = _state.value.copy(outletTerpilih = if (sekarang == nama) null else nama)
        terapkanFilter()
    }

    private fun terapkanFilter() {
        val s = _state.value
        _state.value = s.copy(terlihat = saringDaftar(s.semua, s.tab, s.outletTerpilih, s.cari))
    }

    /**
     * Menutup dokumen jadi `selesai`. Kedua syaratnya diperiksa di sini sebelum
     * menyentuh jaringan: role harus berhak, dan status harus sudah diverifikasi
     * outlet. RLS di server adalah jaring pengaman terakhir, bukan yang pertama.
     */
    fun tutupDokumen(baris: SuratJalanRingkas) {
        if (!_state.value.bolehTutupDokumen) return
        if (baris.status?.bolehDitutup != true) return
        viewModelScope.launch {
            _state.value = _state.value.copy(sedangMenutup = baris.id, error = null)
            try {
                SuratJalanRepository.tutupDokumen(baris.id)
                _state.value = _state.value.copy(
                    sedangMenutup = null,
                    pesan = "Dokumen ${baris.nomorDokumen ?: ""} ditutup.",
                )
                muat(paksa = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    sedangMenutup = null,
                    error = distribusiErrorMessage(e),
                )
            }
        }
    }

    fun bersihkanPesan() {
        _state.value = _state.value.copy(pesan = null, error = null)
    }
}
```

- [ ] **Step 4: Tulis layar**

Buat `DashboardScreen.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.ui.KartuSuratJalan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface

@Composable
fun DashboardScreen(
    onKeluar: () -> Unit,
    onBukaInbox: () -> Unit,
    onBukaRiwayat: () -> Unit,
    onBukaDetail: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var konfirmasiTutup by remember { mutableStateOf<SuratJalanRingkas?>(null) }

    if (state.memuat && state.semua.isEmpty()) { LayarMemuat(); return }
    if (state.error != null && state.semua.isEmpty()) {
        LayarGalat(state.error!!) { viewModel.muat(paksa = true) }
        return
    }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
            Column(Modifier.weight(1f)) {
                Text("Distribusi", color = SukaOnSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(state.namaPengguna, color = SukaGray500, fontSize = 11.sp)
            }
            IconButton(onClick = onBukaInbox) { Icon(Icons.Default.Inbox, "Inbox penerimaan") }
            IconButton(onClick = onBukaRiwayat) { Icon(Icons.Default.History, "Riwayat") }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KartuAngka("Dikirim", state.hitungan.dikirim, Modifier.weight(1f))
                    KartuAngka("Diterima", state.hitungan.diterima, Modifier.weight(1f))
                    KartuAngka("Selesai", state.hitungan.selesai, Modifier.weight(1f))
                    KartuAngka("Akurasi", state.akurasi, Modifier.weight(1f), akhiran = "%")
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(RentangTanggal.entries) { rentang ->
                        Pil(rentang.label, state.rentang == rentang) { viewModel.ubahRentang(rentang) }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TabStatus.entries) { tab ->
                        Pil(tab.label, state.tab == tab) { viewModel.ubahTab(tab) }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.cari,
                    onValueChange = viewModel::ubahCari,
                    label = { Text("Cari nomor SJ atau outlet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.rincianOutlet.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.rincianOutlet) { outlet ->
                            Pil(
                                "${outlet.nama} (${outlet.total})",
                                state.outletTerpilih == outlet.nama,
                            ) { viewModel.pilihOutlet(outlet.nama) }
                        }
                    }
                }
            }

            if (state.terlihat.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().height(220.dp)) {
                        LayarKosong("Tidak Ada Surat Jalan", "Tidak ada yang cocok dengan filter ini.")
                    }
                }
            } else {
                items(state.terlihat, key = { it.id }) { baris ->
                    val bolehTutup = state.bolehTutupDokumen &&
                        baris.status?.let { st -> st.nilai.startsWith("diterima") } == true
                    KartuSuratJalan(
                        baris = baris,
                        aksiLabel = if (bolehTutup) "Tutup Dokumen" else null,
                        onKlik = { onBukaDetail(baris.id) },
                        onAksi = if (bolehTutup) ({ konfirmasiTutup = baris }) else null,
                    )
                }
            }
        }
    }

    konfirmasiTutup?.let { baris ->
        AlertDialog(
            onDismissRequest = { konfirmasiTutup = null },
            title = { Text("Tutup dokumen?") },
            text = {
                Text(
                    "Surat jalan ${baris.nomorDokumen ?: baris.id.take(8)} akan ditandai selesai " +
                        "dan tidak bisa dibuka kembali dari aplikasi."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.tutupDokumen(baris)
                    konfirmasiTutup = null
                }) { Text("Tutup Dokumen") }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiTutup = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun KartuAngka(label: String, nilai: Int, modifier: Modifier = Modifier, akhiran: String = "") {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = Color.White) {
        Column(Modifier.padding(10.dp)) {
            Text("$nilai$akhiran", color = SukaOnSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = SukaGray500, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Pil(teks: String, aktif: Boolean, onKlik: () -> Unit) {
    Surface(
        Modifier.clickable(onClick = onKlik),
        shape = RoundedCornerShape(50),
        color = if (aktif) SukaOrange else Color.White,
    ) {
        Text(
            teks,
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (aktif) Color.White else SukaOnSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
```

- [ ] **Step 5: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*SaringDaftarTest*"
```

Diharapkan: LULUS, 9 test.

- [ ] **Step 6: Kompilasi modul**

```
.\gradlew.bat :feature:distribusi:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/dashboard/ feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/ui/dashboard/
git commit -m "feat(distribusi): dashboard pemantauan surat jalan"
```

---

### Task 13: Inbox penerimaan

Daftar kiriman yang menunggu diterima. Menekan kartu tidak langsung membuka verifikasi — selalu lewat gerbang QR lebih dulu.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/inbox/InboxViewModel.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/inbox/InboxScreen.kt`

**Interfaces:**
- Consumes: `SuratJalanRepository.inbox()` (Task 7), `DistribusiAkses` (Task 3), `distribusiErrorMessage` (Task 6), komponen Task 11.
- Produces:
  - `data class InboxUiState(val memuat: Boolean, val error: String?, val daftar: List<SuratJalanRingkas>, val bolehVerifikasi: Boolean, val namaOutlet: String)`
  - `class InboxViewModel : ViewModel()` dengan `muat(paksa: Boolean = false)`
  - `@Composable fun InboxScreen(onKeluar: () -> Unit, onBukaScan: () -> Unit, onBukaDetail: (String) -> Unit, viewModel: InboxViewModel = viewModel())`

- [ ] **Step 1: Tulis ViewModel**

Buat `InboxViewModel.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InboxUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val daftar: List<SuratJalanRingkas> = emptyList(),
    val bolehVerifikasi: Boolean = false,
    val namaOutlet: String = "",
)

class InboxViewModel : ViewModel() {

    private val _state = MutableStateFlow(InboxUiState())
    val state: StateFlow<InboxUiState> = _state

    init {
        val staff = AppSession.staff.value
        _state.value = _state.value.copy(
            bolehVerifikasi = DistribusiAkses.bolehVerifikasi(staff?.role),
            namaOutlet = staff?.outletName.orEmpty(),
        )
        muat()
    }

    fun muat(paksa: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            if (paksa) SuratJalanRepository.invalidate()
            try {
                _state.value = _state.value.copy(memuat = false, daftar = SuratJalanRepository.inbox())
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }
}
```

- [ ] **Step 2: Tulis layar**

Buat `InboxScreen.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.ui.KartuSuratJalan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurface

@Composable
fun InboxScreen(
    onKeluar: () -> Unit,
    onBukaScan: () -> Unit,
    onBukaDetail: (String) -> Unit,
    viewModel: InboxViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            // Tombol pindai hanya untuk yang berhak memverifikasi. Pengawas
            // membuka layar ini untuk memantau, bukan untuk menerima barang.
            if (state.bolehVerifikasi) {
                ExtendedFloatingActionButton(
                    onClick = onBukaScan,
                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                    text = { Text("Pindai QR") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().background(SukaSurface).padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
                Column {
                    Text(
                        "Penerimaan Barang",
                        color = SukaOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(state.namaOutlet, color = SukaGray500, fontSize = 11.sp)
                }
            }

            when {
                state.memuat && state.daftar.isEmpty() -> LayarMemuat()
                state.error != null && state.daftar.isEmpty() ->
                    LayarGalat(state.error!!) { viewModel.muat(paksa = true) }
                state.daftar.isEmpty() -> LayarKosong(
                    "Belum Ada Kiriman Masuk",
                    "Surat jalan yang dikirim gudang pusat akan muncul di sini.",
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.daftar, key = { it.id }) { baris ->
                        // Menekan kartu membuka DETAIL, bukan verifikasi. Jalan
                        // menuju verifikasi hanya lewat pemindai QR — itulah
                        // gerbang integritas dokumen fisiknya.
                        KartuSuratJalan(baris = baris, onKlik = { onBukaDetail(baris.id) })
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Kompilasi modul**

```
.\gradlew.bat :feature:distribusi:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/inbox/
git commit -m "feat(distribusi): inbox penerimaan barang"
```

---

### Task 14: Gerbang QR

Verifikasi tidak boleh dibuka tanpa memindai QR pada lembar surat jalan fisik yang dibawa supir. Kode manual enam karakter adalah jalur cadangan yang wajib ada: izin kamera bisa ditolak, dan kamera bisa rusak.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/scan/ScanQrViewModel.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/scan/ScanQrScreen.kt`

**Interfaces:**
- Consumes: `SuratJalanRepository.cariUntukVerifikasi()` (Task 7), `VerifikasiDraftStore.tandaiTerbuka()` (Task 8), `bolehDiverifikasi`, `sudahDiterima` (Task 1), `distribusiErrorMessage` (Task 6).
- Produces:
  - `sealed interface HasilPindai { data object Menunggu; data class Terbuka(val suratJalanId: String); data class Ditolak(val pesan: String) }`
  - `data class ScanUiState(val memproses: Boolean, val hasil: HasilPindai, val kodeManual: String, val kameraGagal: String?)`
  - `class ScanQrViewModel : ViewModel()` dengan `pindai(kode: String)`, `ubahKodeManual(teks: String)`, `kirimKodeManual()`, `tandaiKameraGagal(pesan: String)`, `reset()`
  - `@Composable fun ScanQrScreen(onKeluar: () -> Unit, onTerbuka: (String) -> Unit, viewModel: ScanQrViewModel = viewModel())`

- [ ] **Step 1: Tulis ViewModel**

Buat `ScanQrViewModel.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.VerifikasiDraftStore
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDiverifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import com.sukashawarma.superapp.feature.distribusi.domain.sudahDiterima
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface HasilPindai {
    data object Menunggu : HasilPindai
    data class Terbuka(val suratJalanId: String) : HasilPindai
    data class Ditolak(val pesan: String) : HasilPindai
}

data class ScanUiState(
    val memproses: Boolean = false,
    val hasil: HasilPindai = HasilPindai.Menunggu,
    val kodeManual: String = "",
    val kameraGagal: String? = null,
)

class ScanQrViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    fun ubahKodeManual(teks: String) {
        _state.value = _state.value.copy(kodeManual = teks.uppercase())
    }

    fun kirimKodeManual() {
        val kode = _state.value.kodeManual.trim()
        if (kode.isBlank()) {
            _state.value = _state.value.copy(
                hasil = HasilPindai.Ditolak("Ketik kode verifikasi terlebih dahulu."),
            )
            return
        }
        pindai(kode)
    }

    fun tandaiKameraGagal(pesan: String) {
        _state.value = _state.value.copy(kameraGagal = pesan)
    }

    fun reset() {
        _state.value = _state.value.copy(hasil = HasilPindai.Menunggu)
    }

    /**
     * Satu pemindaian pada satu waktu. Tanpa penjaga `memproses`, penganalisis
     * kamera akan memicu belasan pencarian untuk satu kode yang sama dalam
     * sekejap, dan navigasi bisa terjadi dua kali.
     */
    fun pindai(kode: String) {
        if (_state.value.memproses) return
        if (_state.value.hasil is HasilPindai.Terbuka) return
        viewModelScope.launch {
            _state.value = _state.value.copy(memproses = true, hasil = HasilPindai.Menunggu)
            try {
                val sj = SuratJalanRepository.cariUntukVerifikasi(kode)
                val hasil = when {
                    sj == null -> HasilPindai.Ditolak(
                        "Kode \"$kode\" tidak ditemukan. Periksa lembar surat jalan."
                    )
                    sj.status?.sudahDiterima == true -> HasilPindai.Ditolak(
                        "Surat jalan ini sudah diverifikasi sebelumnya. Lihat di Riwayat."
                    )
                    sj.status?.bolehDiverifikasi != true -> HasilPindai.Ditolak(
                        "Surat jalan ini belum dikirim gudang pusat, jadi belum bisa diterima."
                    )
                    else -> {
                        VerifikasiDraftStore.tandaiTerbuka(sj.id)
                        HasilPindai.Terbuka(sj.id)
                    }
                }
                _state.value = _state.value.copy(memproses = false, hasil = hasil)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    memproses = false,
                    hasil = HasilPindai.Ditolak(distribusiErrorMessage(e)),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Tulis layar**

Buat `ScanQrScreen.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun ScanQrScreen(
    onKeluar: () -> Unit,
    onTerbuka: (String) -> Unit,
    viewModel: ScanQrViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val konteks = LocalContext.current
    val pemilikDaurHidup = LocalLifecycleOwner.current

    var izinKamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(konteks, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val pemintaIzin = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diberi ->
        izinKamera = diberi
        if (!diberi) {
            viewModel.tandaiKameraGagal(
                "Izin kamera ditolak. Ketik kode verifikasi enam karakter di bawah."
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!izinKamera) pemintaIzin.launch(Manifest.permission.CAMERA)
    }

    val hasil = state.hasil
    LaunchedEffect(hasil) {
        if (hasil is HasilPindai.Terbuka) onTerbuka(hasil.suratJalanId)
    }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
            Text(
                "Pindai QR Surat Jalan",
                color = SukaOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Text(
            "Arahkan kamera ke kode QR pada lembar surat jalan yang dibawa kurir.",
            Modifier.padding(horizontal = 16.dp),
            color = SukaGray500,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))

        if (izinKamera && state.kameraGagal == null) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val tampilan = PreviewView(ctx)
                        val penyedia = ProcessCameraProvider.getInstance(ctx)
                        penyedia.addListener({
                            try {
                                val kamera = penyedia.get()
                                val pratinjau = Preview.Builder().build().also {
                                    it.setSurfaceProvider(tampilan.surfaceProvider)
                                }
                                val pemindai = BarcodeScanning.getClient()
                                val analisis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                    )
                                    .build()
                                analisis.setAnalyzer(Executors.newSingleThreadExecutor()) { bingkai ->
                                    val gambar = bingkai.image
                                    if (gambar == null) {
                                        bingkai.close()
                                        return@setAnalyzer
                                    }
                                    val masukan = InputImage.fromMediaImage(
                                        gambar,
                                        bingkai.imageInfo.rotationDegrees,
                                    )
                                    pemindai.process(masukan)
                                        .addOnSuccessListener { kode ->
                                            kode.firstOrNull {
                                                it.format == Barcode.FORMAT_QR_CODE
                                            }?.rawValue?.let { viewModel.pindai(it) }
                                        }
                                        .addOnCompleteListener { bingkai.close() }
                                }
                                kamera.unbindAll()
                                kamera.bindToLifecycle(
                                    pemilikDaurHidup,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    pratinjau,
                                    analisis,
                                )
                            } catch (e: Exception) {
                                viewModel.tandaiKameraGagal(
                                    "Kamera tidak bisa dibuka. Ketik kode verifikasi di bawah."
                                )
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        tampilan
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
            ) {
                Text(
                    state.kameraGagal
                        ?: "Kamera belum tersedia. Gunakan kode verifikasi enam karakter.",
                    Modifier.padding(14.dp),
                    color = SukaOnSurface,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.kodeManual,
                onValueChange = viewModel::ubahKodeManual,
                label = { Text("Kode verifikasi (6 karakter)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::kirimKodeManual,
                enabled = !state.memproses,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.memproses) "Memeriksa..." else "Buka Verifikasi")
            }
            (hasil as? HasilPindai.Ditolak)?.let {
                Text(it.pesan, color = Color(0xFFB91C1C), fontSize = 12.sp)
            }
        }
    }
}
```

- [ ] **Step 3: Kompilasi modul**

```
.\gradlew.bat :feature:distribusi:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/scan/
git commit -m "feat(distribusi): gerbang pindai QR dengan cadangan kode manual"
```

---

### Task 15: Canvas tanda tangan dan pengambilan foto bukti

Dua komponen yang berdiri sendiri, keduanya dipakai layar verifikasi di Task 16.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/ttd/TandaTanganCanvas.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/verifikasi/FotoCameraSheet.kt`
- Test: `feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/ui/ttd/BatasTandaTanganTest.kt`

**Interfaces:**
- Consumes: `FotoBuktiStore` (Task 9) — tidak langsung, hanya lewat pemanggil.
- Produces:
  - `const val BATAS_TANDA_TANGAN = 50_000`
  - `fun tandaTanganTerlaluBesar(dataUrl: String): Boolean`
  - `fun bitmapKeDataUrlPng(bitmap: android.graphics.Bitmap): String`
  - `@Composable fun TandaTanganCanvas(onSelesai: (String) -> Unit, onBatal: () -> Unit)`
  - `@Composable fun FotoCameraSheet(onDiambil: (android.graphics.Bitmap) -> Unit, onBatal: () -> Unit)`

- [ ] **Step 1: Tulis test yang gagal**

Buat `BatasTandaTanganTest.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.ttd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatasTandaTanganTest {

    /** RPC `sign_receipt_surat_jalan` menyimpan gambar ke kolom jsonb. Web
     *  membatasinya di 50.000 karakter; native harus memakai batas yang sama
     *  supaya baris yang dihasilkan tidak berbeda bentuk. */
    @Test
    fun `batas sama dengan web`() {
        assertEquals(50_000, BATAS_TANDA_TANGAN)
    }

    @Test
    fun `gambar wajar lolos`() {
        assertFalse(tandaTanganTerlaluBesar("data:image/png;base64," + "A".repeat(1000)))
    }

    @Test
    fun `gambar tepat di batas lolos`() {
        assertFalse(tandaTanganTerlaluBesar("A".repeat(BATAS_TANDA_TANGAN)))
    }

    @Test
    fun `gambar melewati batas ditolak`() {
        assertTrue(tandaTanganTerlaluBesar("A".repeat(BATAS_TANDA_TANGAN + 1)))
    }
}
```

- [ ] **Step 2: Jalankan test, pastikan gagal**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*BatasTandaTanganTest*"
```

Diharapkan: GAGAL kompilasi, `Unresolved reference: BATAS_TANDA_TANGAN`.

- [ ] **Step 3: Tulis canvas tanda tangan**

Buat `TandaTanganCanvas.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.ttd

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

/** Batas panjang data URL tanda tangan — cermin `MAX_SIGNATURE_SIZE` di web. */
const val BATAS_TANDA_TANGAN = 50_000

fun tandaTanganTerlaluBesar(dataUrl: String): Boolean = dataUrl.length > BATAS_TANDA_TANGAN

/** PNG -> data URL, format yang sama dengan `canvas.toDataURL()` di browser,
 *  supaya gambar dari HP bisa ditampilkan web tanpa penanganan khusus. */
fun bitmapKeDataUrlPng(bitmap: Bitmap): String {
    val keluaran = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, keluaran)
    val base64 = Base64.encodeToString(keluaran.toByteArray(), Base64.NO_WRAP)
    return "data:image/png;base64,$base64"
}

/**
 * Papan goresan tanda tangan. Jalur direkam sebagai daftar titik, lalu
 * dirender ulang ke `Bitmap` saat disimpan — merender dari data yang sama
 * dengan yang dilihat pengguna, bukan menangkap ulang layar.
 */
@Composable
fun TandaTanganCanvas(onSelesai: (String) -> Unit, onBatal: () -> Unit) {
    val jalur = remember { mutableStateListOf<MutableList<Offset>>() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(180.dp)
                .clip(RoundedCornerShape(14.dp)).background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { titik -> jalur.add(mutableListOf(titik)) },
                        onDrag = { perubahan, _ ->
                            perubahan.consume()
                            jalur.lastOrNull()?.add(perubahan.position)
                        },
                    )
                }
        ) {
            jalur.forEach { garis ->
                for (i in 1 until garis.size) {
                    drawLine(
                        color = Color.Black,
                        start = garis[i - 1],
                        end = garis[i],
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { jalur.clear() }, modifier = Modifier.weight(1f)) {
                Text("Hapus")
            }
            OutlinedButton(onClick = onBatal, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = { onSelesai(bitmapKeDataUrlPng(renderJalur(jalur, 600, 240))) },
                enabled = jalur.any { it.size > 1 },
                modifier = Modifier.weight(1f),
            ) { Text("Simpan") }
        }
    }
}

/** Menggambar ulang jalur ke bitmap berlatar putih pada ukuran tetap, supaya
 *  besar berkasnya dapat diperkirakan dan tidak bergantung ukuran layar. */
private fun renderJalur(jalur: List<List<Offset>>, lebar: Int, tinggi: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(lebar, tinggi, Bitmap.Config.ARGB_8888)
    val kanvas = android.graphics.Canvas(bitmap)
    kanvas.drawColor(android.graphics.Color.WHITE)

    val semuaTitik = jalur.flatten()
    if (semuaTitik.isEmpty()) return bitmap
    val maksX = semuaTitik.maxOf { it.x }.coerceAtLeast(1f)
    val maksY = semuaTitik.maxOf { it.y }.coerceAtLeast(1f)
    val skala = minOf(lebar / maksX, tinggi / maksY)

    val kuas = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 4f
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        isAntiAlias = true
    }
    jalur.forEach { garis ->
        for (i in 1 until garis.size) {
            kanvas.drawLine(
                garis[i - 1].x * skala, garis[i - 1].y * skala,
                garis[i].x * skala, garis[i].y * skala,
                kuas,
            )
        }
    }
    return bitmap
}
```

- [ ] **Step 4: Tulis pengambil foto**

Buat `FotoCameraSheet.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.verifikasi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

/**
 * Pengambilan foto bukti memakai CameraX di dalam aplikasi, bukan intent ke
 * aplikasi kamera bawaan. Alasannya dua: hasil intent `TakePicturePreview`
 * hanya thumbnail beresolusi rendah — tidak layak jadi bukti — dan jalur
 * `TakePicture` beresolusi penuh menuntut `FileProvider` beserta berkas
 * sementara yang harus dibersihkan sendiri.
 */
@Composable
fun FotoCameraSheet(onDiambil: (Bitmap) -> Unit, onBatal: () -> Unit) {
    val konteks = LocalContext.current
    val pemilikDaurHidup = LocalLifecycleOwner.current
    val penangkap = remember { ImageCapture.Builder().build() }
    var mengambil by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp)).background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val tampilan = PreviewView(ctx)
                    val penyedia = ProcessCameraProvider.getInstance(ctx)
                    penyedia.addListener({
                        val kamera = penyedia.get()
                        val pratinjau = Preview.Builder().build().also {
                            it.setSurfaceProvider(tampilan.surfaceProvider)
                        }
                        kamera.unbindAll()
                        kamera.bindToLifecycle(
                            pemilikDaurHidup,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            pratinjau,
                            penangkap,
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    tampilan
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBatal, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = {
                    mengambil = true
                    penangkap.takePicture(
                        ContextCompat.getMainExecutor(konteks),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(gambar: ImageProxy) {
                                val bitmap = gambar.keBitmap()
                                gambar.close()
                                mengambil = false
                                onDiambil(bitmap)
                            }

                            override fun onError(galat: ImageCaptureException) {
                                mengambil = false
                            }
                        },
                    )
                },
                enabled = !mengambil,
                modifier = Modifier.weight(1f),
            ) { Text(if (mengambil) "Mengambil..." else "Ambil Foto") }
        }
    }
}

/** `ImageProxy` datang dalam orientasi sensor. Tanpa rotasi ini, foto bukti
 *  tersimpan miring 90 derajat pada sebagian besar HP. */
private fun ImageProxy.keBitmap(): Bitmap {
    val penyangga = planes[0].buffer
    val bytes = ByteArray(penyangga.remaining())
    penyangga.get(bytes)
    val asal = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val derajat = imageInfo.rotationDegrees
    if (derajat == 0) return asal
    val matriks = Matrix().apply { postRotate(derajat.toFloat()) }
    return Bitmap.createBitmap(asal, 0, 0, asal.width, asal.height, matriks, true)
}
```

- [ ] **Step 5: Jalankan test, pastikan lulus**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest --tests "*BatasTandaTanganTest*"
```

Diharapkan: LULUS, 4 test.

- [ ] **Step 6: Kompilasi modul**

```
.\gradlew.bat :feature:distribusi:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/ttd/ feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/verifikasi/FotoCameraSheet.kt feature/distribusi/src/test/java/com/sukashawarma/superapp/feature/distribusi/ui/ttd/
git commit -m "feat(distribusi): canvas tanda tangan dan pengambilan foto bukti"
```

---

### Task 16: Layar verifikasi penerimaan

Inti modul. Tiga langkah dalam satu layar: kartu per item, ringkasan, lalu tanda tangan. Finalisasi menulis item satu per satu, baru memanggil RPC — urutannya tidak boleh dibalik, karena RPC membaca kolom yang baru ditulis.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/verifikasi/VerifikasiViewModel.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/verifikasi/VerifikasiScreen.kt`

**Interfaces:**
- Consumes: `SuratJalanRepository` (Task 7, 10), `VerifikasiDraftStore`, `DraftVerifikasi` (Task 8), `FotoBuktiStore` (Task 9), `ValidasiVerifikasi`, `IsianVerifikasi`, `KondisiItem`, `HasilValidasi` (Task 4), `SatuanDistribusi` (Task 2), `bolehDiverifikasi`, `sudahDiterima` (Task 1), `DistribusiAkses` (Task 3), `distribusiErrorMessage` (Task 6), `TandaTanganCanvas`, `BATAS_TANDA_TANGAN`, `tandaTanganTerlaluBesar` (Task 15), `FotoCameraSheet` (Task 15), komponen Task 11.
- Produces:
  - `enum class LangkahVerifikasi(val kunci: String) { KARTU("kartu"), RINGKASAN("ringkasan"), TTD("ttd") }`
  - `data class ItemTampil(val item: SuratJalanItem, val qtyDikirimTampil: Long, val satuan: String)`
  - `data class VerifikasiUiState(...)`
  - `class VerifikasiViewModel(val suratJalanId: String) : ViewModel()` beserta `Factory`
  - `@Composable fun VerifikasiScreen(suratJalanId: String, onKeluar: () -> Unit, onSelesai: () -> Unit)`

- [ ] **Step 1: Tulis ViewModel**

Buat `VerifikasiViewModel.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.verifikasi

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.DraftVerifikasi
import com.sukashawarma.superapp.feature.distribusi.data.FotoBuktiStore
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.VerifikasiDraftStore
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanItem
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.HasilValidasi
import com.sukashawarma.superapp.feature.distribusi.domain.IsianVerifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import com.sukashawarma.superapp.feature.distribusi.domain.SatuanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.ValidasiVerifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDiverifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import com.sukashawarma.superapp.feature.distribusi.domain.sudahDiterima
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class LangkahVerifikasi(val kunci: String) {
    KARTU("kartu"),
    RINGKASAN("ringkasan"),
    TTD("ttd"),
}

/** Item beserta qty kiriman yang sudah dikonversi ke satuan distribusi.
 *  Konversi dilakukan sekali di sini supaya tidak diulang tiap recomposition. */
data class ItemTampil(
    val item: SuratJalanItem,
    val qtyDikirimTampil: Long,
    val satuan: String,
)

data class VerifikasiUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val pesan: String? = null,
    /** Layar menolak dibuka tanpa gerbang QR, termasuk saat dinavigasi langsung. */
    val terkunci: Boolean = false,
    val tidakBerhak: Boolean = false,
    val sudahDiverifikasi: Boolean = false,
    val detail: SuratJalanDetail? = null,
    val items: List<ItemTampil> = emptyList(),
    val isian: Map<String, IsianVerifikasi> = emptyMap(),
    val indeksItem: Int = 0,
    val langkah: LangkahVerifikasi = LangkahVerifikasi.KARTU,
    val kondisiTerkonfirmasi: Boolean = false,
    val mengunggahFoto: Boolean = false,
    val ttdPenerimaan: List<TandaTangan> = emptyList(),
    val menandatangani: Boolean = false,
    val memfinalisasi: Boolean = false,
    val selesai: Boolean = false,
    val namaCrew: String = "",
) {
    val itemAktif: ItemTampil? get() = items.getOrNull(indeksItem)
    val isianAktif: IsianVerifikasi
        get() = itemAktif?.let { isian[it.item.id] }
            ?: IsianVerifikasi(null, KondisiItem.BAIK, "", null)
    val ttdLengkap: Boolean
        get() = ttdPenerimaan.any { it.peran == SuratJalanRepository.PERAN_CREW } &&
            ttdPenerimaan.any { it.peran == SuratJalanRepository.PERAN_SUPIR }
}

class VerifikasiViewModel(private val suratJalanId: String) : ViewModel() {

    class Factory(private val suratJalanId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VerifikasiViewModel(suratJalanId) as T
    }

    private val _state = MutableStateFlow(VerifikasiUiState())
    val state: StateFlow<VerifikasiUiState> = _state

    init {
        muat()
    }

    fun muat() {
        viewModelScope.launch {
            val staff = AppSession.staff.value
            if (!DistribusiAkses.bolehVerifikasi(staff?.role)) {
                _state.value = _state.value.copy(memuat = false, tidakBerhak = true)
                return@launch
            }
            if (!VerifikasiDraftStore.sudahTerbuka(suratJalanId)) {
                _state.value = _state.value.copy(memuat = false, terkunci = true)
                return@launch
            }
            _state.value = _state.value.copy(memuat = true, error = null, namaCrew = staff?.name.orEmpty())
            try {
                val detail = SuratJalanRepository.detail(suratJalanId)
                if (detail == null) {
                    _state.value = _state.value.copy(
                        memuat = false,
                        error = "Surat jalan tidak ditemukan.",
                    )
                    return@launch
                }
                if (detail.status?.sudahDiterima == true || detail.status?.bolehDiverifikasi != true) {
                    _state.value = _state.value.copy(memuat = false, sudahDiverifikasi = true)
                    return@launch
                }

                val items = detail.items.map { item ->
                    val meta = item.bahan
                    ItemTampil(
                        item = item,
                        qtyDikirimTampil = if (meta == null) Math.round(item.qtyDikirim)
                        else SatuanDistribusi.keTampilan(item.qtyDikirim, meta),
                        satuan = meta?.let { SatuanDistribusi.satuanTampil(it) } ?: "unit",
                    )
                }

                val draft = VerifikasiDraftStore.muat(suratJalanId)
                val isian = items.associate { tampil ->
                    tampil.item.id to (
                        draft?.isian?.get(tampil.item.id)
                            ?: IsianVerifikasi(null, KondisiItem.BAIK, "", null)
                        )
                }

                _state.value = _state.value.copy(
                    memuat = false,
                    detail = detail,
                    items = items,
                    isian = isian,
                    ttdPenerimaan = detail.ttdPenerimaan,
                    indeksItem = draft?.indeksItem?.takeIf { it in items.indices } ?: 0,
                    langkah = LangkahVerifikasi.entries.find { it.kunci == draft?.langkah }
                        ?: LangkahVerifikasi.KARTU,
                    kondisiTerkonfirmasi = draft?.kondisiTerkonfirmasi ?: false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    // ------------------------------------------------------------ isian

    private fun ubahIsian(ubah: (IsianVerifikasi) -> IsianVerifikasi) {
        val item = _state.value.itemAktif ?: return
        val baru = _state.value.isian.toMutableMap()
        baru[item.item.id] = ubah(_state.value.isianAktif)
        _state.value = _state.value.copy(isian = baru, kondisiTerkonfirmasi = false)
        simpanDraft()
    }

    fun ubahQty(teks: String) = ubahIsian { it.copy(qtyTerima = teks.toDoubleOrNull()) }

    fun ubahKondisi(kondisi: KondisiItem) = ubahIsian {
        // Kembali ke "Baik" berarti tidak ada keluhan lagi, jadi catatannya ikut hilang.
        if (kondisi == KondisiItem.BAIK) it.copy(kondisi = kondisi, catatan = "")
        else it.copy(kondisi = kondisi)
    }

    fun ubahCatatan(teks: String) = ubahIsian { it.copy(catatan = teks) }

    /** Mengisi qty dengan jumlah yang dikirim — tombol "Sesuai Kirim". */
    fun samakanQty() {
        val item = _state.value.itemAktif ?: return
        ubahIsian { it.copy(qtyTerima = item.qtyDikirimTampil.toDouble(), kondisi = KondisiItem.BAIK, catatan = "") }
    }

    fun konfirmasiKondisi() {
        val item = _state.value.itemAktif ?: return
        when (val hasil = ValidasiVerifikasi.konfirmasiKondisi(_state.value.isianAktif, item.qtyDikirimTampil)) {
            is HasilValidasi.Tolak -> _state.value = _state.value.copy(error = hasil.pesan)
            HasilValidasi.Lolos -> {
                _state.value = _state.value.copy(kondisiTerkonfirmasi = true, error = null)
                simpanDraft()
            }
        }
    }

    fun unggahFoto(bitmap: Bitmap) {
        val item = _state.value.itemAktif ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(mengunggahFoto = true, error = null)
            try {
                val path = FotoBuktiStore.unggah(suratJalanId, item.item.id, bitmap)
                val baru = _state.value.isian.toMutableMap()
                baru[item.item.id] = _state.value.isianAktif.copy(fotoPath = path)
                _state.value = _state.value.copy(mengunggahFoto = false, isian = baru)
                simpanDraft()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    mengunggahFoto = false,
                    error = distribusiErrorMessage(e),
                )
            }
        }
    }

    fun lanjut() {
        when (val hasil = ValidasiVerifikasi.bolehLanjut(_state.value.isianAktif)) {
            is HasilValidasi.Tolak -> _state.value = _state.value.copy(error = hasil.pesan)
            HasilValidasi.Lolos -> {
                val s = _state.value
                _state.value = if (s.indeksItem + 1 >= s.items.size) {
                    s.copy(langkah = LangkahVerifikasi.RINGKASAN, kondisiTerkonfirmasi = false, error = null)
                } else {
                    s.copy(indeksItem = s.indeksItem + 1, kondisiTerkonfirmasi = false, error = null)
                }
                simpanDraft()
            }
        }
    }

    fun mundur() {
        val s = _state.value
        _state.value = when {
            s.langkah == LangkahVerifikasi.TTD -> s.copy(langkah = LangkahVerifikasi.RINGKASAN)
            s.langkah == LangkahVerifikasi.RINGKASAN ->
                s.copy(langkah = LangkahVerifikasi.KARTU, indeksItem = (s.items.size - 1).coerceAtLeast(0))
            s.indeksItem > 0 -> s.copy(indeksItem = s.indeksItem - 1, kondisiTerkonfirmasi = false)
            else -> s
        }
        simpanDraft()
    }

    fun keTandaTangan() {
        _state.value = _state.value.copy(langkah = LangkahVerifikasi.TTD, error = null)
        simpanDraft()
    }

    fun bersihkanPesan() {
        _state.value = _state.value.copy(error = null, pesan = null)
    }

    private fun simpanDraft() {
        val s = _state.value
        VerifikasiDraftStore.simpan(
            suratJalanId,
            DraftVerifikasi(s.isian, s.indeksItem, s.langkah.kunci, s.kondisiTerkonfirmasi),
        )
    }

    // ------------------------------------------------------------ tanda tangan

    fun tandaTangan(peran: String, nama: String, gambar: String) {
        if (nama.isBlank()) {
            _state.value = _state.value.copy(error = "Nama penanda tangan harus diisi.")
            return
        }
        if (com.sukashawarma.superapp.feature.distribusi.ui.ttd.tandaTanganTerlaluBesar(gambar)) {
            _state.value = _state.value.copy(
                error = "Tanda tangan terlalu besar. Ulangi goresan dengan lebih sederhana.",
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(menandatangani = true, error = null)
            try {
                val daftar = SuratJalanRepository.tandaTanganPenerimaan(suratJalanId, nama, peran, gambar)
                _state.value = _state.value.copy(
                    menandatangani = false,
                    // Daftar dari server jadi sumber kebenaran, bukan salinan lokal:
                    // TTD yang sudah tersimpan harus tetap terlihat setelah app ditutup.
                    ttdPenerimaan = daftar,
                    pesan = "Tanda tangan $peran tersimpan.",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    menandatangani = false,
                    error = distribusiErrorMessage(e),
                )
            }
        }
    }

    // ------------------------------------------------------------ finalisasi

    /**
     * Dua langkah, urutannya mengikat: tulis tiap item lebih dulu, baru panggil
     * RPC — RPC membaca `qty_terima`, `kondisi`, dan `flagged` yang baru ditulis
     * untuk menyusun `ledger_stok` dan menetapkan status akhir.
     *
     * Bila satu item gagal ditulis, RPC TIDAK dipanggil dan draft dipertahankan,
     * supaya crew bisa mencoba ulang tanpa mengisi dari awal.
     */
    fun finalisasi() {
        val s = _state.value
        if (!s.ttdLengkap || s.memfinalisasi) return
        viewModelScope.launch {
            _state.value = s.copy(memfinalisasi = true, error = null)
            try {
                s.items.forEach { tampil ->
                    val isian = s.isian[tampil.item.id] ?: return@forEach
                    val meta = tampil.item.bahan
                    val qtyTampil = isian.qtyTerima ?: 0.0
                    val qtyDasar = if (meta == null) qtyTampil
                    else SatuanDistribusi.keDasar(qtyTampil, meta)
                    SuratJalanRepository.simpanVerifikasiItem(
                        itemId = tampil.item.id,
                        qtyTerimaDasar = qtyDasar,
                        qtyDikirimDasar = tampil.item.qtyDikirim,
                        kondisi = isian.kondisi,
                        catatan = isian.catatan,
                        fotoPath = isian.fotoPath,
                    )
                }

                val hasil = SuratJalanRepository.finalisasi(suratJalanId)
                // `success:false` dengan pesan "sudah diverifikasi sebelumnya"
                // berarti percobaan terdahulu sampai ke server walau jaringannya
                // putus. Itu keberhasilan, bukan kegagalan.
                val sudahPernah = !hasil.sukses && hasil.pesan.contains("sudah diverifikasi", true)
                if (hasil.sukses || sudahPernah) {
                    VerifikasiDraftStore.hapus(suratJalanId)
                    _state.value = _state.value.copy(memfinalisasi = false, selesai = true)
                } else {
                    _state.value = _state.value.copy(
                        memfinalisasi = false,
                        error = hasil.pesan.ifBlank { "Finalisasi gagal. Coba lagi." },
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    memfinalisasi = false,
                    error = distribusiErrorMessage(e),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Tulis layar**

Buat `VerifikasiScreen.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.verifikasi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.ttd.TandaTanganCanvas
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface

private val MerahTeks = Color(0xFFB91C1C)

@Composable
fun VerifikasiScreen(
    suratJalanId: String,
    onKeluar: () -> Unit,
    onSelesai: () -> Unit,
) {
    val viewModel: VerifikasiViewModel = viewModel(
        factory = VerifikasiViewModel.Factory(suratJalanId),
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.selesai) { if (state.selesai) onSelesai() }

    when {
        state.memuat -> { LayarMemuat(); return }
        state.tidakBerhak -> {
            LayarKosong(
                "Tidak Berwenang",
                "Verifikasi penerimaan dikerjakan crew atau leader di outlet tujuan.",
            ); return
        }
        state.terkunci -> {
            LayarKosong(
                "Verifikasi Terkunci",
                "Pindai kode QR pada lembar surat jalan fisik yang dibawa kurir terlebih dahulu.",
            ); return
        }
        state.sudahDiverifikasi -> {
            LayarKosong(
                "Sudah Diverifikasi",
                "Surat jalan ini sudah pernah diverifikasi. Lihat detailnya di Riwayat.",
            ); return
        }
        state.error != null && state.detail == null -> {
            LayarGalat(state.error!!) { viewModel.muat() }; return
        }
        state.items.isEmpty() -> {
            LayarKosong("Tidak Ada Item", "Surat jalan ini tidak memuat item apa pun."); return
        }
    }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (state.langkah == LangkahVerifikasi.KARTU && state.indeksItem == 0) onKeluar() else viewModel.mundur() }) {
                Icon(Icons.Default.ArrowBack, "Kembali")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "SJ ${state.detail?.nomorDokumen ?: ""}",
                    color = SukaOnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    when (state.langkah) {
                        LangkahVerifikasi.KARTU ->
                            "Item ${state.indeksItem + 1} dari ${state.items.size}"
                        LangkahVerifikasi.RINGKASAN -> "Ringkasan"
                        LangkahVerifikasi.TTD -> "Tanda tangan penerimaan"
                    },
                    color = SukaGray500,
                    fontSize = 11.sp,
                )
            }
        }

        LinearProgressIndicator(
            progress = { (state.indeksItem + 1f) / state.items.size },
            modifier = Modifier.fillMaxWidth(),
            color = SukaOrange,
        )

        state.error?.let {
            Text(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MerahTeks, fontSize = 12.sp)
        }

        when (state.langkah) {
            LangkahVerifikasi.KARTU -> KartuItem(state, viewModel)
            LangkahVerifikasi.RINGKASAN -> Ringkasan(state, viewModel)
            LangkahVerifikasi.TTD -> LangkahTtd(state, viewModel)
        }
    }
}

@Composable
private fun KartuItem(state: VerifikasiUiState, viewModel: VerifikasiViewModel) {
    val item = state.itemAktif ?: return
    val isian = state.isianAktif
    var kameraTerbuka by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        item.item.bahan?.nama ?: "Bahan tidak dikenal",
                        color = SukaOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Dikirim ${item.qtyDikirimTampil} ${item.satuan}",
                        color = SukaGray500,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = isian.qtyTerima?.let {
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                } ?: "",
                onValueChange = viewModel::ubahQty,
                label = { Text("Jumlah diterima (${item.satuan})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedButton(onClick = viewModel::samakanQty, modifier = Modifier.fillMaxWidth()) {
                Text("Sesuai Kirim (${item.qtyDikirimTampil} ${item.satuan})")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TombolKondisi("Baik", isian.kondisi == KondisiItem.BAIK, Modifier.weight(1f)) {
                    viewModel.ubahKondisi(KondisiItem.BAIK)
                }
                TombolKondisi("Tidak Sesuai", isian.kondisi == KondisiItem.TIDAK_SESUAI, Modifier.weight(1f)) {
                    viewModel.ubahKondisi(KondisiItem.TIDAK_SESUAI)
                }
            }
        }

        if (isian.kondisi == KondisiItem.TIDAK_SESUAI) {
            item {
                OutlinedTextField(
                    value = isian.catatan,
                    onValueChange = viewModel::ubahCatatan,
                    label = { Text("Catatan alasan (wajib)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Button(
                onClick = viewModel::konfirmasiKondisi,
                enabled = !state.kondisiTerkonfirmasi,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.kondisiTerkonfirmasi) "Kondisi Terkonfirmasi" else "Konfirmasi Kondisi")
            }
        }

        item {
            if (kameraTerbuka) {
                FotoCameraSheet(
                    onDiambil = { bitmap ->
                        kameraTerbuka = false
                        viewModel.unggahFoto(bitmap)
                    },
                    onBatal = { kameraTerbuka = false },
                )
            } else {
                OutlinedButton(
                    onClick = { kameraTerbuka = true },
                    enabled = !state.mengunggahFoto,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            state.mengunggahFoto -> "Mengunggah foto..."
                            isian.fotoPath != null -> "Foto bukti tersimpan — Ambil Ulang"
                            else -> "Ambil Foto Bukti (wajib)"
                        }
                    )
                }
            }
        }

        item {
            Button(
                onClick = viewModel::lanjut,
                enabled = state.kondisiTerkonfirmasi && isian.fotoPath != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.indeksItem + 1 >= state.items.size) "Lanjut ke Ringkasan" else "Item Berikutnya")
            }
        }
    }
}

@Composable
private fun Ringkasan(state: VerifikasiUiState, viewModel: VerifikasiViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.items, key = { it.item.id }) { tampil ->
            val isian = state.isian[tampil.item.id]
            val tidakSesuai = isian?.kondisi == KondisiItem.TIDAK_SESUAI ||
                (isian?.qtyTerima ?: 0.0) < tampil.qtyDikirimTampil
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        tampil.item.bahan?.nama ?: "-",
                        color = SukaOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${isian?.qtyTerima?.toLong() ?: 0} / ${tampil.qtyDikirimTampil} ${tampil.satuan}",
                        color = if (tidakSesuai) MerahTeks else SukaGray500,
                        fontSize = 12.sp,
                    )
                    if (!isian?.catatan.isNullOrBlank()) {
                        Text(isian!!.catatan, color = SukaGray500, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Button(onClick = viewModel::keTandaTangan, modifier = Modifier.fillMaxWidth()) {
                Text("Lanjut ke Tanda Tangan")
            }
        }
    }
}

@Composable
private fun LangkahTtd(state: VerifikasiUiState, viewModel: VerifikasiViewModel) {
    var peranAktif by remember { mutableStateOf<String?>(null) }
    var namaSupir by remember { mutableStateOf("") }

    val sudahCrew = state.ttdPenerimaan.any { it.peran == SuratJalanRepository.PERAN_CREW }
    val sudahSupir = state.ttdPenerimaan.any { it.peran == SuratJalanRepository.PERAN_SUPIR }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.ttdPenerimaan) { ttd ->
            Text(
                "${ttd.peran}: ${ttd.namaPenandaTangan}",
                color = SukaOnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (peranAktif == null) {
            item {
                OutlinedButton(
                    onClick = { peranAktif = SuratJalanRepository.PERAN_CREW },
                    enabled = !sudahCrew,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (sudahCrew) "Crew Penerima sudah tanda tangan" else "Tanda Tangan Crew Penerima") }
            }
            item {
                OutlinedButton(
                    onClick = { peranAktif = SuratJalanRepository.PERAN_SUPIR },
                    enabled = !sudahSupir,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (sudahSupir) "Supir sudah tanda tangan" else "Tanda Tangan Supir") }
            }
        } else {
            val peran = peranAktif!!
            // Nama crew diambil dari sesi dan tidak bisa diubah: yang menerima
            // barang adalah orang yang sedang login. Nama supir diketik karena
            // dia bukan pengguna aplikasi.
            if (peran == SuratJalanRepository.PERAN_SUPIR) {
                item {
                    OutlinedTextField(
                        value = namaSupir,
                        onValueChange = { namaSupir = it },
                        label = { Text("Nama supir / kurir") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item { Text("Nama: ${state.namaCrew}", color = SukaOnSurface, fontSize = 12.sp) }
            }
            item {
                TandaTanganCanvas(
                    onSelesai = { gambar ->
                        val nama = if (peran == SuratJalanRepository.PERAN_CREW) state.namaCrew else namaSupir
                        viewModel.tandaTangan(peran, nama, gambar)
                        peranAktif = null
                    },
                    onBatal = { peranAktif = null },
                )
            }
        }

        item {
            Button(
                onClick = viewModel::finalisasi,
                enabled = state.ttdLengkap && !state.memfinalisasi,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.memfinalisasi) "Menyimpan..." else "Selesaikan Penerimaan")
            }
        }
        if (!state.ttdLengkap) {
            item {
                Text(
                    "Kedua tanda tangan wajib lengkap sebelum penerimaan bisa diselesaikan.",
                    color = SukaGray500,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun TombolKondisi(teks: String, aktif: Boolean, modifier: Modifier, onKlik: () -> Unit) {
    if (aktif) Button(onClick = onKlik, modifier = modifier) { Text(teks) }
    else OutlinedButton(onClick = onKlik, modifier = modifier) { Text(teks) }
}
```

Catatan versi: varian `progress = { ... }` pada `LinearProgressIndicator` ada di
material3 1.2.0 (yang dibawa Compose BOM 2024.02.00). Bila kompilasi menolaknya,
ganti dengan varian nilai langsung:
`progress = (state.indeksItem + 1f) / state.items.size`.

- [ ] **Step 3: Kompilasi modul**

```
.\gradlew.bat :feature:distribusi:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/verifikasi/
git commit -m "feat(distribusi): layar verifikasi penerimaan per item"
```

---

### Task 17: Riwayat dan detail dokumen

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/riwayat/RiwayatViewModel.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/riwayat/RiwayatScreen.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/detail/DetailViewModel.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/detail/DetailSuratJalanScreen.kt`

**Interfaces:**
- Consumes: `SuratJalanRepository.riwayat()`, `.detail()` (Task 7), `FotoBuktiStore.ambil()` (Task 9), `SatuanDistribusi` (Task 2), `DistribusiAkses.bolehLihatKodeVerifikasi` (Task 3), `distribusiErrorMessage` (Task 6), komponen Task 11.
- Produces:
  - `class RiwayatViewModel : ViewModel()` dengan `state: StateFlow<RiwayatUiState>`, `muat(paksa: Boolean = false)`
  - `@Composable fun RiwayatScreen(onKeluar: () -> Unit, onBukaDetail: (String) -> Unit, viewModel: RiwayatViewModel = viewModel())`
  - `data class BarisItemDetail(val nama: String, val qtyDikirim: Long, val qtyTerima: Long?, val satuan: String, val kondisi: String?, val catatan: String?, val fotoPath: String?, val bermasalah: Boolean)`
  - `class DetailViewModel(val suratJalanId: String) : ViewModel()` beserta `Factory`, dengan `muatFoto(path: String)`
  - `@Composable fun DetailSuratJalanScreen(suratJalanId: String, onKeluar: () -> Unit)`

- [ ] **Step 1: Tulis ViewModel riwayat**

Buat `RiwayatViewModel.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RiwayatUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val daftar: List<SuratJalanRingkas> = emptyList(),
)

class RiwayatViewModel : ViewModel() {

    private val _state = MutableStateFlow(RiwayatUiState())
    val state: StateFlow<RiwayatUiState> = _state

    init { muat() }

    fun muat(paksa: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            if (paksa) SuratJalanRepository.invalidate()
            try {
                _state.value = RiwayatUiState(memuat = false, daftar = SuratJalanRepository.riwayat())
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }
}
```

- [ ] **Step 2: Tulis layar riwayat**

Buat `RiwayatScreen.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.ui.KartuSuratJalan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurface

@Composable
fun RiwayatScreen(
    onKeluar: () -> Unit,
    onBukaDetail: (String) -> Unit,
    viewModel: RiwayatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
            Text(
                "Riwayat Penerimaan",
                color = SukaOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        when {
            state.memuat && state.daftar.isEmpty() -> LayarMemuat()
            state.error != null && state.daftar.isEmpty() ->
                LayarGalat(state.error!!) { viewModel.muat(paksa = true) }
            state.daftar.isEmpty() -> LayarKosong(
                "Belum Ada Riwayat",
                "Penerimaan yang sudah diverifikasi dan ditandatangani akan tercatat di sini.",
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.daftar, key = { it.id }) { baris ->
                    KartuSuratJalan(baris = baris, onKlik = { onBukaDetail(baris.id) })
                }
            }
        }
    }
}
```

- [ ] **Step 3: Tulis ViewModel detail**

Buat `DetailViewModel.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.FotoBuktiStore
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.SatuanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Satu baris item, sudah dalam satuan distribusi dan siap dirender. */
data class BarisItemDetail(
    val nama: String,
    val qtyDikirim: Long,
    val qtyTerima: Long?,
    val satuan: String,
    val kondisi: String?,
    val catatan: String?,
    val fotoPath: String?,
    val bermasalah: Boolean,
)

data class DetailUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val detail: SuratJalanDetail? = null,
    val baris: List<BarisItemDetail> = emptyList(),
    val bolehLihatKode: Boolean = false,
    val foto: Map<String, Bitmap> = emptyMap(),
)

class DetailViewModel(private val suratJalanId: String) : ViewModel() {

    class Factory(private val suratJalanId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(suratJalanId) as T
    }

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    init { muat() }

    fun muat() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val detail = SuratJalanRepository.detail(suratJalanId)
                if (detail == null) {
                    _state.value = _state.value.copy(
                        memuat = false,
                        error = "Surat jalan tidak ditemukan.",
                    )
                    return@launch
                }
                _state.value = _state.value.copy(
                    memuat = false,
                    detail = detail,
                    bolehLihatKode = DistribusiAkses.bolehLihatKodeVerifikasi(
                        AppSession.staff.value?.role
                    ),
                    baris = detail.items.map { item ->
                        val meta = item.bahan
                        val kurang = item.qtyTerima != null && item.qtyTerima < item.qtyDikirim
                        BarisItemDetail(
                            nama = meta?.nama ?: "Bahan tidak dikenal",
                            qtyDikirim = if (meta == null) Math.round(item.qtyDikirim)
                            else SatuanDistribusi.keTampilan(item.qtyDikirim, meta),
                            qtyTerima = item.qtyTerima?.let {
                                if (meta == null) Math.round(it)
                                else SatuanDistribusi.keTampilan(it, meta)
                            },
                            satuan = meta?.let { SatuanDistribusi.satuanTampil(it) } ?: "unit",
                            kondisi = item.kondisi,
                            catatan = item.catatan,
                            fotoPath = item.fotoPath,
                            bermasalah = item.kondisi == "rusak" || kurang,
                        )
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    /**
     * Foto diambil sesuai permintaan, bukan sekaligus saat layar dibuka: satu
     * surat jalan bisa memuat belasan foto, dan menariknya semua di jaringan
     * outlet akan membuat layar terasa macet.
     *
     * Kegagalan satu foto sengaja diabaikan diam-diam — foto yang hilang tidak
     * boleh menutup akses ke sisa dokumen.
     */
    fun muatFoto(path: String) {
        if (_state.value.foto.containsKey(path)) return
        viewModelScope.launch {
            try {
                val bytes = FotoBuktiStore.ambil(path) ?: return@launch
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch
                _state.value = _state.value.copy(foto = _state.value.foto + (path to bitmap))
            } catch (e: Exception) {
                // diabaikan dengan sengaja, lihat komentar di atas
            }
        }
    }
}
```

- [ ] **Step 4: Tulis layar detail**

Buat `DetailSuratJalanScreen.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.LencanaStatus
import com.sukashawarma.superapp.feature.distribusi.ui.formatTanggal
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurface

private val MerahTeks = Color(0xFFB91C1C)

@Composable
fun DetailSuratJalanScreen(suratJalanId: String, onKeluar: () -> Unit) {
    val viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory(suratJalanId))
    val state by viewModel.state.collectAsState()

    if (state.memuat) { LayarMemuat(); return }
    val detail = state.detail
    if (detail == null) {
        LayarGalat(state.error ?: "Dokumen tidak bisa dibuka.") { viewModel.muat() }
        return
    }

    LaunchedEffect(state.baris) {
        state.baris.mapNotNull { it.fotoPath }.forEach { viewModel.muatFoto(it) }
    }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
            Column(Modifier.weight(1f)) {
                Text(
                    "SJ ${detail.nomorDokumen ?: detail.id.take(8).uppercase()}",
                    color = SukaOnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "${detail.namaOutlet ?: "Gudang Pusat"} • ${formatTanggal(detail.dibuatPada)}",
                    color = SukaGray500,
                    fontSize = 11.sp,
                )
            }
            LencanaStatus(detail.status, state.baris.any { it.bermasalah })
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Kode verifikasi hanya untuk pengawas — lihat DistribusiAkses.
            if (state.bolehLihatKode && detail.kodeVerifikasi != null) {
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Kode Verifikasi", color = SukaGray500, fontSize = 10.sp)
                            Text(
                                detail.kodeVerifikasi,
                                color = SukaOnSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }

            items(state.baris) { baris ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                    Column(Modifier.padding(12.dp)) {
                        Text(baris.nama, color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Dikirim ${baris.qtyDikirim} ${baris.satuan} • Diterima " +
                                "${baris.qtyTerima?.toString() ?: "-"} ${baris.satuan}",
                            color = if (baris.bermasalah) MerahTeks else SukaGray500,
                            fontSize = 12.sp,
                        )
                        if (!baris.catatan.isNullOrBlank()) {
                            Text(baris.catatan, color = SukaGray500, fontSize = 11.sp)
                        }
                        val bitmap = baris.fotoPath?.let { state.foto[it] }
                        if (bitmap != null) {
                            Spacer(Modifier.height(8.dp))
                            Image(
                                bitmap.asImageBitmap(),
                                contentDescription = "Foto bukti ${baris.nama}",
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                            )
                        }
                    }
                }
            }

            item { BlokTandaTangan("Tanda Tangan Pengirim", detail.ttdPengirim) }
            item { BlokTandaTangan("Tanda Tangan Penerimaan", detail.ttdPenerimaan) }
        }
    }
}

@Composable
private fun BlokTandaTangan(judul: String, daftar: List<TandaTangan>) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
        Column(Modifier.padding(12.dp)) {
            Text(judul, color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            if (daftar.isEmpty()) {
                Text("Belum ada tanda tangan.", color = SukaGray500, fontSize = 11.sp)
            } else {
                daftar.forEach { ttd ->
                    Text(
                        "${ttd.peran}: ${ttd.namaPenandaTangan}",
                        color = SukaOnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(formatTanggal(ttd.waktu), color = SukaGray500, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
```

Gambar tanda tangan berupa data URL base64 tidak dirender di Fase 1 — nama, peran, dan waktunya sudah cukup untuk membaca dokumen, dan menerjemahkan data URL menjadi bitmap menambah jalur yang belum diperlukan. Gambar goresan tetap tersimpan utuh di database dan tetap tampil di web.

- [ ] **Step 5: Kompilasi modul**

```
.\gradlew.bat :feature:distribusi:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/riwayat/ feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/detail/
git commit -m "feat(distribusi): riwayat penerimaan dan detail dokumen"
```

---

### Task 18: Navigasi modul dan perakitan ke aplikasi

Task terakhir. Setelah ini modul bisa dibuka dari layar utama dan seluruh alur bisa dijalankan di perangkat.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/DistribusiRoutes.kt`
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/DistribusiNavGraph.kt`
- Modify: `app/src/main/java/com/sukashawarma/superapp/presentation/MainActivity.kt`
- Modify: `feature/home/src/main/java/com/sukashawarma/superapp/feature/home/ui/HomeScreen.kt`

**Interfaces:**
- Consumes: seluruh layar Task 12–17, `DistribusiAkses.ROLE_MODUL` (Task 3).
- Produces:
  - `object DistribusiRoutes` dengan `DASHBOARD`, `INBOX`, `SCAN`, `RIWAYAT`, `VERIFIKASI`, `DETAIL`, `verifikasi(id)`, `detail(id)`
  - `@Composable fun DistribusiNavGraph(onExit: () -> Unit)`

`app/build.gradle.kts` sudah memuat `implementation(project(":feature:distribusi"))` pada kedua blok varian, jadi tidak perlu diubah.

- [ ] **Step 1: Tulis rute**

Buat `DistribusiRoutes.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi

/** Rute internal modul Distribusi — NavHost sendiri, dipasang di `Routes.DISTRIBUSI`. */
object DistribusiRoutes {
    const val DASHBOARD = "distribusi/dashboard"
    const val INBOX = "distribusi/inbox"
    const val SCAN = "distribusi/scan"
    const val RIWAYAT = "distribusi/riwayat"

    private const val VERIFIKASI_POLA = "distribusi/verifikasi"
    const val VERIFIKASI = "$VERIFIKASI_POLA/{suratJalanId}"

    private const val DETAIL_POLA = "distribusi/detail"
    const val DETAIL = "$DETAIL_POLA/{suratJalanId}"

    // Argumennya UUID, jadi tidak perlu di-encode.
    fun verifikasi(suratJalanId: String): String = "$VERIFIKASI_POLA/$suratJalanId"
    fun detail(suratJalanId: String): String = "$DETAIL_POLA/$suratJalanId"
}
```

- [ ] **Step 2: Tulis NavGraph**

Buat `DistribusiNavGraph.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sukashawarma.superapp.feature.distribusi.ui.dashboard.DashboardScreen
import com.sukashawarma.superapp.feature.distribusi.ui.detail.DetailSuratJalanScreen
import com.sukashawarma.superapp.feature.distribusi.ui.inbox.InboxScreen
import com.sukashawarma.superapp.feature.distribusi.ui.riwayat.RiwayatScreen
import com.sukashawarma.superapp.feature.distribusi.ui.scan.ScanQrScreen
import com.sukashawarma.superapp.feature.distribusi.ui.verifikasi.VerifikasiScreen

/**
 * Navigasi modul Distribusi. Pola yang sama dengan Absensi dan Stok: satu
 * NavHost bersarang yang dipasang pada satu rute di NavHost root.
 */
@Composable
fun DistribusiNavGraph(onExit: () -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DistribusiRoutes.DASHBOARD,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(280, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(280))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(280, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(280))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(280, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(280, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(280))
        },
    ) {
        composable(DistribusiRoutes.DASHBOARD) {
            DashboardScreen(
                onKeluar = onExit,
                onBukaInbox = { navController.navigate(DistribusiRoutes.INBOX) },
                onBukaRiwayat = { navController.navigate(DistribusiRoutes.RIWAYAT) },
                onBukaDetail = { id -> navController.navigate(DistribusiRoutes.detail(id)) },
            )
        }

        composable(DistribusiRoutes.INBOX) {
            InboxScreen(
                onKeluar = { navController.popBackStack() },
                onBukaScan = { navController.navigate(DistribusiRoutes.SCAN) },
                onBukaDetail = { id -> navController.navigate(DistribusiRoutes.detail(id)) },
            )
        }

        composable(DistribusiRoutes.SCAN) {
            ScanQrScreen(
                onKeluar = { navController.popBackStack() },
                onTerbuka = { id ->
                    // Pemindai dikeluarkan dari tumpukan: menekan Kembali dari
                    // layar verifikasi harus mendarat di inbox, bukan menyalakan
                    // kamera lagi.
                    navController.navigate(DistribusiRoutes.verifikasi(id)) {
                        popUpTo(DistribusiRoutes.SCAN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            DistribusiRoutes.VERIFIKASI,
            arguments = listOf(navArgument("suratJalanId") { type = NavType.StringType }),
        ) { entry ->
            VerifikasiScreen(
                suratJalanId = entry.arguments?.getString("suratJalanId").orEmpty(),
                onKeluar = { navController.popBackStack() },
                onSelesai = {
                    navController.navigate(DistribusiRoutes.RIWAYAT) {
                        popUpTo(DistribusiRoutes.DASHBOARD)
                    }
                },
            )
        }

        composable(DistribusiRoutes.RIWAYAT) {
            RiwayatScreen(
                onKeluar = { navController.popBackStack() },
                onBukaDetail = { id -> navController.navigate(DistribusiRoutes.detail(id)) },
            )
        }

        composable(
            DistribusiRoutes.DETAIL,
            arguments = listOf(navArgument("suratJalanId") { type = NavType.StringType }),
        ) { entry ->
            DetailSuratJalanScreen(
                suratJalanId = entry.arguments?.getString("suratJalanId").orEmpty(),
                onKeluar = { navController.popBackStack() },
            )
        }
    }
}
```

- [ ] **Step 3: Pasang rute di NavHost root**

Di `app/src/main/java/com/sukashawarma/superapp/presentation/MainActivity.kt`:

Tambahkan import di sebelah `import com.sukashawarma.superapp.feature.stok.StokNavGraph`:

```kotlin
import com.sukashawarma.superapp.feature.distribusi.DistribusiNavGraph
```

Tambahkan konstanta di dalam `object Routes`, setelah `const val STOK = "stok"`:

```kotlin
    const val DISTRIBUSI = "distribusi"
```

Tambahkan `onOpenDistribusi` pada pemanggilan `HomeScreen`:

```kotlin
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenAbsensi = { navController.navigate(Routes.ABSENSI) },
                    onOpenStok = { navController.navigate(Routes.STOK) },
                    onOpenDistribusi = { navController.navigate(Routes.DISTRIBUSI) },
                    onLoggedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
                )
            }
```

Tambahkan rute baru tepat setelah blok `composable(Routes.STOK)`:

```kotlin
            composable(Routes.DISTRIBUSI) {
                DistribusiNavGraph(onExit = { navController.popBackStack() })
            }
```

- [ ] **Step 4: Tambahkan kartu modul di layar utama**

Di `feature/home/src/main/java/com/sukashawarma/superapp/feature/home/ui/HomeScreen.kt`:

Tambahkan import ikon di sebelah `import androidx.compose.material.icons.filled.Inventory2`:

```kotlin
import androidx.compose.material.icons.filled.LocalShipping
```

Tambahkan daftar role setelah `STOK_ROLES`:

```kotlin
/**
 * Role yang boleh membuka modul Distribusi. Sama persis dengan
 * `DistribusiAkses.ROLE_MODUL`, disalin ke sini supaya `:feature:home` tidak
 * perlu bergantung pada `:feature:distribusi` hanya untuk satu himpunan.
 *
 * `kitchen` dan `admin` sengaja tidak masuk: penerbitan surat jalan tetap di web,
 * dan database memang hanya mengizinkan mereka menerbitkannya.
 */
private val DISTRIBUSI_ROLES = setOf(
    com.sukashawarma.superapp.domain.model.Role.CREW,
    com.sukashawarma.superapp.domain.model.Role.LEADER,
    com.sukashawarma.superapp.domain.model.Role.AREA_MANAGER,
    com.sukashawarma.superapp.domain.model.Role.REGIONAL_MANAGER,
)
```

Tambahkan parameter baru pada tanda tangan `HomeScreen`:

```kotlin
@Composable
fun HomeScreen(
    onOpenAbsensi: () -> Unit,
    onOpenStok: () -> Unit,
    onOpenDistribusi: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
```

Tambahkan kartu tepat setelah blok `if (staff?.role in STOK_ROLES) { ... }` dan sebelum `Spacer(Modifier.height(28.dp))`:

```kotlin
            if (staff?.role in DISTRIBUSI_ROLES) {
                Spacer(Modifier.height(14.dp))
                ModuleCard(
                    ModuleTile(
                        "Distribusi",
                        "Terima kiriman, verifikasi barang & riwayat surat jalan",
                        Icons.Default.LocalShipping,
                        onOpenDistribusi,
                        listOf(
                            Triple("PENERIMAAN", "Scan QR", SukaOnSurface),
                            Triple("VERIFIKASI", "Per Item", Color(0xFFEA580C)),
                            Triple("RIWAYAT", "Tersedia", Color(0xFF168451)),
                        ),
                    )
                )
            }
```

- [ ] **Step 5: Jalankan seluruh test dan bangun APK debug**

```
.\gradlew.bat :feature:distribusi:testDebugUnitTest
```

Diharapkan: LULUS, 90 test.

```
.\gradlew.bat :app:assembleDebug
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/DistribusiRoutes.kt feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/DistribusiNavGraph.kt app/src/main/java/com/sukashawarma/superapp/presentation/MainActivity.kt feature/home/src/main/java/com/sukashawarma/superapp/feature/home/ui/HomeScreen.kt
git commit -m "feat(distribusi): navigasi modul dan kartu di layar utama"
```

---

### Task 19: Penyegaran manual dan saat layar kembali aktif

Web berlangganan Supabase Realtime; `:core:realtime` native masih kosong, jadi spec §9 menggantinya dengan penyegaran manual plus penyegaran otomatis saat layar kembali aktif. Tanpa ini, crew yang baru menerima kiriman tidak akan melihat surat jalan baru sampai menutup dan membuka ulang modul.

**Files:**
- Create: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/SegarkanSaatAktif.kt`
- Modify: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/dashboard/DashboardScreen.kt`
- Modify: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/inbox/InboxScreen.kt`
- Modify: `feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/riwayat/RiwayatScreen.kt`

**Interfaces:**
- Consumes: `DashboardViewModel.muat`, `InboxViewModel.muat`, `RiwayatViewModel.muat` (Task 12, 13, 17).
- Produces: `@Composable fun SegarkanSaatAktif(onSegarkan: () -> Unit)`

- [ ] **Step 1: Tulis pengamat daur hidup**

Buat `SegarkanSaatAktif.kt`:

```kotlin
package com.sukashawarma.superapp.feature.distribusi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Memuat ulang setiap kali layar kembali ke depan.
 *
 * Pengganti langganan Realtime yang dipakai web: `:core:realtime` native masih
 * kosong, dan `ON_RESUME` menangkap kasus yang paling sering terjadi di
 * lapangan — crew berpindah ke aplikasi lain sebentar lalu kembali, atau supir
 * baru saja tiba sementara layar dibiarkan terbuka.
 *
 * `rememberUpdatedState` dipakai supaya pengamat yang sudah terdaftar tetap
 * memanggil lambda versi terbaru tanpa perlu mendaftar ulang tiap recomposition.
 */
@Composable
fun SegarkanSaatAktif(onSegarkan: () -> Unit) {
    val terkini by rememberUpdatedState(onSegarkan)
    val pemilik = LocalLifecycleOwner.current
    DisposableEffect(pemilik) {
        val pengamat = LifecycleEventObserver { _, peristiwa ->
            if (peristiwa == Lifecycle.Event.ON_RESUME) terkini()
        }
        pemilik.lifecycle.addObserver(pengamat)
        onDispose { pemilik.lifecycle.removeObserver(pengamat) }
    }
}
```

Tambahkan `import androidx.compose.runtime.getValue` di berkas yang sama bila belum terbawa.

- [ ] **Step 2: Pasang di dashboard**

Di `DashboardScreen.kt`, tambahkan import:

```kotlin
import androidx.compose.material.icons.filled.Refresh
import com.sukashawarma.superapp.feature.distribusi.ui.SegarkanSaatAktif
```

Tepat setelah baris `var konfirmasiTutup by remember { mutableStateOf<SuratJalanRingkas?>(null) }`, tambahkan:

```kotlin
    SegarkanSaatAktif { viewModel.muat(paksa = true) }
```

Di baris tombol header, tambahkan tombol segarkan sebelum `IconButton(onClick = onBukaInbox)`:

```kotlin
            IconButton(onClick = { viewModel.muat(paksa = true) }) {
                Icon(Icons.Default.Refresh, "Segarkan")
            }
```

- [ ] **Step 3: Pasang di inbox**

Di `InboxScreen.kt`, tambahkan import:

```kotlin
import androidx.compose.material.icons.filled.Refresh
import com.sukashawarma.superapp.feature.distribusi.ui.SegarkanSaatAktif
```

Tepat setelah `val state by viewModel.state.collectAsState()`, tambahkan:

```kotlin
    SegarkanSaatAktif { viewModel.muat(paksa = true) }
```

Di baris header, setelah blok `Column { ... }` yang memuat judul, tambahkan:

```kotlin
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.muat(paksa = true) }) {
                    Icon(Icons.Default.Refresh, "Segarkan")
                }
```

dan tambahkan import `androidx.compose.foundation.layout.Spacer` bila belum ada.

- [ ] **Step 4: Pasang di riwayat**

Di `RiwayatScreen.kt`, tambahkan import:

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Refresh
import com.sukashawarma.superapp.feature.distribusi.ui.SegarkanSaatAktif
```

Tepat setelah `val state by viewModel.state.collectAsState()`, tambahkan:

```kotlin
    SegarkanSaatAktif { viewModel.muat(paksa = true) }
```

Di baris header, setelah `Text("Riwayat Penerimaan", ...)`, tambahkan:

```kotlin
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.muat(paksa = true) }) {
                Icon(Icons.Default.Refresh, "Segarkan")
            }
```

- [ ] **Step 5: Kompilasi modul**

```
.\gradlew.bat :feature:distribusi:compileDebugKotlin
```

Diharapkan: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add feature/distribusi/src/main/java/com/sukashawarma/superapp/feature/distribusi/ui/
git commit -m "feat(distribusi): segarkan manual dan saat layar kembali aktif"
```

---

## Verifikasi Manual di Perangkat

Test unit tidak menyentuh database. Pemeriksaan berikut wajib dilakukan sebelum Fase 1 dinyatakan selesai, memakai surat jalan uji yang diterbitkan dari **web** ke outlet uji.

- [ ] **1. Cakupan outlet.** Masuk sebagai `crew` — dashboard hanya menampilkan surat jalan outletnya. Masuk sebagai `regional_manager` — tampil lintas outlet. Tidak ada daftar role di aplikasi yang menentukan ini; bila salah, periksa `accessible_outlet_ids()`, bukan kode Kotlin.

- [ ] **2. Gerbang QR menolak jalan pintas.** Dari inbox, tekan sebuah kartu — yang terbuka adalah detail, bukan verifikasi. Verifikasi hanya terbuka setelah memindai QR atau memasukkan kode enam karakter.

- [ ] **3. Kode salah dan surat jalan yang sudah selesai ditolak.** Masukkan kode acak: muncul pesan tidak ditemukan. Masukkan kode surat jalan yang sudah `selesai`: muncul pesan sudah diverifikasi.

- [ ] **4. Foto wajib.** Isi qty dan konfirmasi kondisi tanpa mengambil foto — tombol lanjut tetap mati.

- [ ] **5. Draft bertahan.** Isi dua item, tutup aplikasi dari daftar aplikasi terbaru, buka lagi, masuk kembali ke verifikasi surat jalan yang sama. Isian dan foto kedua item harus pulih, dan layar kembali ke item yang sedang dikerjakan.

- [ ] **6. Tanda tangan ganda ditolak server.** Tanda tangani sebagai Crew Penerima dua kali — RPC menolak, dan pesannya tampil apa adanya berbahasa Indonesia.

- [ ] **7. Finalisasi menulis ledger.** Selesaikan satu surat jalan dengan tiga item: satu pas, satu kurang, satu tidak sesuai. Lalu periksa di database:
  - `surat_jalan.status` menjadi `diterima_sebagian` (karena ada yang ditandai).
  - `ledger_stok` memuat baris `terima_kiriman` untuk tiap item ber-qty di atas nol, dan baris `rejected_kiriman` ber-qty 0 untuk yang kurang atau rusak.
  - `surat_jalan_item.selisih` terisi otomatis oleh Postgres, bukan oleh aplikasi.

- [ ] **8. Paritas dengan web — pemeriksaan penerimaan yang menegakkan batasan §1 spec.** Terbitkan dua surat jalan kembar dari web dengan item dan qty yang sama. Verifikasi satu lewat web, satu lewat native, dengan isian yang sama persis. Bandingkan baris yang dihasilkan:
  - `surat_jalan_item`: `qty_terima`, `kondisi`, `flagged`, `catatan` harus identik. `verified_by` harus sama-sama `NULL`.
  - `ledger_stok`: `tipe` dan `qty` harus identik.

  Selisih apa pun di sini berarti native menulis dengan bentuk berbeda dari web, dan harus diperbaiki sebelum rilis.

- [ ] **9. Tutup dokumen.** Masuk sebagai `area_manager`, buka tab "Belum Diverifikasi", tutup satu dokumen. Status berubah jadi `selesai` dan kartunya pindah ke tab Selesai. Masuk sebagai `crew` — tombol itu tidak ada.

- [ ] **10. Kode verifikasi tersembunyi.** Buka detail sebagai `crew` — tidak ada kode verifikasi di layar. Buka sebagai `regional_manager` — kodenya tampil.

- [ ] **11. Bahan bersatuan besar.** Verifikasi bahan yang `satuan_distribusi`-nya berbeda dari `satuan` (misalnya dikirim per kg sementara satuan dasarnya karung). Angka di layar harus dalam satuan distribusi, dan `qty_terima` di database harus dalam satuan dasar. Bandingkan dengan hasil web untuk bahan yang sama.

- [ ] **12. Izin kamera ditolak.** Tolak izin kamera saat diminta — layar pemindai tetap berguna lewat kode manual, tidak kosong dan tidak macet.
