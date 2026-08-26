import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.sukashawarma.superapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sukashawarma.superapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "SUPABASE_URL", "\"https://khpkoreaaucvyqfhynfq.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtocGtvcmVhYXVjdnlxZmh5bmZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA5NjMyOTIsImV4cCI6MjA5NjUzOTI5Mn0.RdsvP6OKs6aiRnqqd02BYiv5gzbh4uGqO88dapo0Gso\"")
        buildConfigField("String", "ABSENSI_WEB_URL", "\"https://absensi.sukashawarma.com\"")
    }

    signingConfigs {
        val storeFile = localProps.getProperty("RELEASE_STORE_FILE")
        val hasSigning = listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
            .all { !localProps.getProperty(it).isNullOrBlank() } && !storeFile.isNullOrBlank() && rootProject.file(storeFile).isFile
        if (hasSigning) {
            create("release") {
                this.storeFile = rootProject.file(storeFile!!)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lint { checkReleaseBuilds = false }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Offline-first
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Camera + face detection (deteksi wajah; ekstraksi embedding menunggu model spike)
    val cameraVersion = "1.3.4"
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    implementation("com.google.mlkit:face-detection:16.1.6")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    implementation("io.coil-kt:coil-compose:2.5.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(project(":core:network"))
    implementation(project(":core:auth"))
    implementation(project(":core:realtime"))
    implementation(project(":core:database"))
    implementation(project(":core:update"))
    implementation(project(":core:printer"))
    implementation(project(":core:camera"))
    implementation(project(":core:location"))
    implementation(project(":core:storage"))
    implementation(project(":core:ui"))
    implementation(project(":core:roles"))
    implementation(project(":feature:home"))
    implementation(project(":feature:absensi"))
    implementation(project(":feature:stok"))
    implementation(project(":feature:distribusi"))
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
}
    dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:auth"))
    implementation(project(":core:realtime"))
    implementation(project(":core:database"))
    implementation(project(":core:update"))
    implementation(project(":core:printer"))
    implementation(project(":core:camera"))
    implementation(project(":core:location"))
    implementation(project(":core:storage"))
    implementation(project(":core:ui"))
    implementation(project(":core:roles"))
    implementation(project(":feature:home"))
    implementation(project(":feature:absensi"))
    implementation(project(":feature:stok"))
    implementation(project(":feature:distribusi"))
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
}


