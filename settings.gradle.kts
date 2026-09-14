pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SukaSuperapp"
include(":app")
// Modul alat, tidak ikut ke APK pengguna.
include(":baselineprofile")
include(":core:network", ":core:auth", ":core:realtime", ":core:database", ":core:update", ":core:printer", ":core:camera", ":core:location", ":core:storage", ":core:ui", ":core:roles")
include(":feature:home", ":feature:absensi", ":feature:stok", ":feature:distribusi", ":feature:mitra", ":feature:manager", ":feature:leader", ":feature:profil", ":feature:chat")
