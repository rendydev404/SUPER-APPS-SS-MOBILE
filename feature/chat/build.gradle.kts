import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// Kunci API KLIPY (stiker) dibaca dari local.properties — tidak pernah di-commit.
// Kosong = tab Stiker menampilkan "belum dikonfigurasi", bukan crash.
val kunciKlipy: String = Properties().run {
    val f = rootProject.file("local.properties")
    if (f.isFile) f.inputStream().use { load(it) }
    getProperty("KLIPY_API_KEY").orEmpty().trim()
}

android {
    namespace = "com.sukashawarma.superapp.feature.chat"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "KLIPY_API_KEY", "\"$kunciKlipy\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(project(":core:ui"))
    implementation(project(":core:roles"))
    implementation(project(":core:network"))
    implementation(project(":core:storage"))
    // Kamera dalam aplikasi untuk opsi "Kamera" di lembar lampiran, sama seperti
    // yang dipakai Profil dan Stok.
    implementation(project(":core:camera"))
    // Foto di bubble chat dimuat dengan ImageLoader bersama milik core:ui
    // (bucket privat butuh Authorization); versi coil harus sama dengannya.
    implementation("io.coil-kt:coil-compose:2.5.0")
    // Memutar stiker WebP/GIF animasi dari KLIPY.
    implementation("io.coil-kt:coil-gif:2.5.0")
    // Klien HTTP terpisah untuk KLIPY (tanpa interceptor auth Supabase & tanpa log URL,
    // karena kunci API ada di path URL).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core-ktx:1.12.0")
    testImplementation("junit:junit:4.13.2")
}
