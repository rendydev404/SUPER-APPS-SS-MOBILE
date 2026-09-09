plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

/**
 * ABI yang dibangun untuk varian debug, dari properti `superapp.debugAbi`.
 *
 * ncnn/ArcFace di modul ini dibangun lewat CMake, dan AGP membuat satu pasang
 * task configure+build untuk SETIAP ABI. Empat ABI berarti delapan task native
 * yang ikut jalan di tiap build padahal saat ngoprek hanya satu yang benar-benar
 * dipasang ke perangkat.
 *
 * Kosongkan nilainya di `gradle.properties` untuk kembali membangun semua ABI —
 * perlu kalau memakai emulator x86_64. Varian release TIDAK tersentuh, jadi APK
 * rilis tetap membawa keempat ABI.
 */
val debugAbi: List<String> = providers.gradleProperty("superapp.debugAbi").orNull
    ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    ?: emptyList()

android {
    namespace = "com.sukashawarma.superapp.core.camera"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
    }
    ndkVersion = "28.2.13676358"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    buildTypes {
        debug {
            if (debugAbi.isNotEmpty()) {
                ndk { abiFilters += debugAbi }
            }
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    val cameraVersion = "1.3.4"
    api("androidx.camera:camera-camera2:$cameraVersion")
    api("androidx.camera:camera-lifecycle:$cameraVersion")
    api("androidx.camera:camera-view:$cameraVersion")
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-ktx:1.12.0")
    testImplementation("junit:junit:4.13.2")
}
