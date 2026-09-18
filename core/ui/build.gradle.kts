plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sukashawarma.superapp.core.ui"
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
    // CloudOff dipakai pita/keadaan mode offline; ikon itu ada di paket extended,
    // bukan di set inti yang ikut material3.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-ktx:1.12.0")
    // RealtimeRefresh menyalurkan Realtime (core:network) ke lifecycle layar.
    api(project(":core:network"))
    // AvatarStaf memuat foto dari bucket privat lewat OkHttp client Supabase.
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    // NavAman & TransisiNav dipakai lintas modul, jadi tipe navigasi dan animasinya
    // harus ikut terbawa ke classpath pemakai.
    api(composeBom)
    api("androidx.compose.animation:animation")
    api("androidx.navigation:navigation-compose:2.7.7")

    testImplementation("junit:junit:4.13.2")
}

