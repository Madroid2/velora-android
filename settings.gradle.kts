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

// ── Composite build: ApexAd SDK ───────────────────────────────────────────────
// Gradle treats apex-ad-sdk-android as part of this build graph, so all SDK
// module sources are compiled together with Velora — no publishing step needed.
// To switch to a published AAR later, remove this block and add the Maven coords.
includeBuild("../apex-ad-sdk-android") {
    dependencySubstitution {
        substitute(module("com.apexads:sdk-core")).using(project(":sdk-core"))
        substitute(module("com.apexads:sdk-banner")).using(project(":sdk-banner"))
        substitute(module("com.apexads:sdk-interstitial")).using(project(":sdk-interstitial"))
        substitute(module("com.apexads:sdk-native")).using(project(":sdk-native"))
        substitute(module("com.apexads:sdk-video")).using(project(":sdk-video"))
        substitute(module("com.apexads:sdk-appopen")).using(project(":sdk-appopen"))
        substitute(module("com.apexads:sdk-wallet")).using(project(":sdk-wallet"))
    }
}

rootProject.name = "Velora"
include(":app")
