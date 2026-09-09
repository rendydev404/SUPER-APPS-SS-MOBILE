plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // KSP menggantikan kapt: kapt lebih dulu menghasilkan stub Java untuk SELURUH
    // sumber Kotlin satu modul sebelum prosesor jalan, KSP membaca Kotlin langsung.
    // Versi terikat ke versi Kotlin — naikkan keduanya bersamaan.
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
