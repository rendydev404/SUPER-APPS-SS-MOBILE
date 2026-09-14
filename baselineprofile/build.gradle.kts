plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    id("androidx.baselineprofile")
}

/**
 * Modul PEMBANGKIT Baseline Profile — tidak ikut terkirim ke pengguna.
 *
 * Isinya satu uji instrumentasi yang menjalankan aplikasi di perangkat sungguhan
 * sambil merekam kode mana yang benar-benar dipakai saat dibuka. Hasilnya sebuah
 * daftar yang dibundel ke APK, dan Android memakainya untuk mengkompilasi bagian
 * itu lebih dulu (AOT) alih-alih menerjemahkannya sambil jalan.
 */
android {
    namespace = "com.sukashawarma.superapp.baselineprofile"
    compileSdk = 34

    defaultConfig {
        // Macrobenchmark menuntut API 28 ke atas; pembangkitan tanpa root
        // sendiri baru bisa sejak API 33, dan perangkat uji sudah jauh di atasnya.
        minSdk = 28
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    targetProjectPath = ":app"
}

baselineProfile {
    // Dijalankan di perangkat yang tersambung, bukan emulator terkelola: proyek
    // ini tidak punya definisi Gradle Managed Device, dan perangkat aslinya
    // memang yang paling mewakili pengguna.
    useConnectedDevices = true
}

// Plugin baseline profile menyematkan versi benchmark miliknya sendiri, jadi
// menaikkan angka di blok dependencies saja tidak berpengaruh. Dipaksa di sini.
configurations.all {
    resolutionStrategy {
        force(
            "androidx.benchmark:benchmark-macro-junit4:1.3.4",
            "androidx.benchmark:benchmark-macro:1.3.4",
            "androidx.benchmark:benchmark-common:1.3.4",
        )
    }
}

dependencies {
    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    // Pustaka runtime dinaikkan sendiri, terpisah dari plugin Gradle-nya: 1.2.4
    // belum mengenal keluaran `pm dump-profiles` milik Android 16 ("Waiting for
    // app processes to flush profiles..."), sementara plugin versi 1.3 menuntut
    // AGP yang lebih baru daripada yang dipakai proyek ini.
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.4")
}
