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
include(":core:network", ":core:auth", ":core:realtime", ":core:database", ":core:update", ":core:printer", ":core:camera", ":core:location", ":core:storage", ":core:ui", ":core:roles")
include(":feature:home", ":feature:absensi", ":feature:stok", ":feature:distribusi", ":feature:mitra", ":feature:manager")
