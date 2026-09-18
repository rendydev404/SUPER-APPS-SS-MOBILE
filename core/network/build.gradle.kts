plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sukashawarma.superapp.core.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "SUPABASE_URL", "\"https://khpkoreaaucvyqfhynfq.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtocGtvcmVhYXVjdnlxZmh5bmZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA5NjMyOTIsImV4cCI6MjA5NjUzOTI5Mn0.RdsvP6OKs6aiRnqqd02BYiv5gzbh4uGqO88dapo0Gso\"")
        buildConfigField("String", "ABSENSI_WEB_URL", "\"https://absensi.sukashawarma.com\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = false
    }
    testOptions {
        unitTests {
            // android.util.Log tidak ada di JVM. Tanpa ini, satu baris Log.i di jalur yang
            // sedang diuji membuat tesnya gagal karena alasan yang sama sekali bukan
            // perilakunya.
            isReturnDefaultValues = true
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    api("com.squareup.retrofit2:retrofit:2.9.0")
    api("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    api("com.google.code.gson:gson:2.10.1")
    // `api`, bukan `implementation`: Realtime.updates() mengembalikan Flow, jadi
    // coroutines ikut jadi bagian API modul ini. Sebelumnya ikut menumpang lewat
    // hilt-android — kebetulan yang pecah begitu Hilt dilepas.
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.core:core-ktx:1.12.0")
    // Cache baca dan antrean tulis offline bersandar pada Room. Arahnya sengaja
    // core:network -> core:database dan tidak sebaliknya; core:storage sudah bergantung
    // pada modul ini, jadi unggahan lampiran dipasang lewat hook Outbox.unggahLampiran
    // dari layer app alih-alih dipanggil langsung (kalau langsung, dependensinya melingkar).
    implementation(project(":core:database"))

    testImplementation("junit:junit:4.13.2")
}




