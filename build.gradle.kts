// Top-level build file. Plugin versions live in gradle/libs.versions.toml.
// Kotlin is provided by AGP's built-in Kotlin support (AGP >= 9.0), so the
// org.jetbrains.kotlin.android plugin must NOT be applied.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
