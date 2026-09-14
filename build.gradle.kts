plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // KSP menggantikan kapt: kapt lebih dulu menghasilkan stub Java untuk SELURUH
    // sumber Kotlin satu modul sebelum prosesor jalan, KSP membaca Kotlin langsung.
    // Versi terikat ke versi Kotlin — naikkan keduanya bersamaan.
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    // Membangkitkan Baseline Profile: daftar kode yang dikompilasi AOT lebih dulu
    // supaya layar pertama tidak diterjemahkan sambil jalan. Versi 1.2.x cocok
    // dengan AGP 8.2.
    id("androidx.baselineprofile") version "1.2.4" apply false
}
