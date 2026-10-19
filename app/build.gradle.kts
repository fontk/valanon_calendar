import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

/**
 * Release signing key, kept OUTSIDE the repository so it is never committed:
 * ~/.mis-turnos/keystore.properties (storeFile, storePassword, keyAlias, keyPassword), or the
 * file named by the MIS_TURNOS_KEYSTORE_PROPERTIES environment variable.
 * Every release must be signed with this same key, otherwise Android refuses the update and the
 * app has to be uninstalled, losing its data. Without the file, release builds come out unsigned.
 */
val firmaRelease: Properties? = run {
    val ruta = System.getenv("MIS_TURNOS_KEYSTORE_PROPERTIES")
        ?: "${System.getProperty("user.home")}/.mis-turnos/keystore.properties"
    val archivo = file(ruta)
    if (archivo.isFile) Properties().apply { archivo.inputStream().use { load(it) } } else null
}
if (firmaRelease == null) {
    logger.warn("Mis turnos: no hay clave de release (~/.mis-turnos/keystore.properties); la APK de release saldrá sin firmar.")
}

android {
    namespace = "com.valanon.calendar"
    compileSdk = 36
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.valanon.calendar"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "0.8.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        firmaRelease?.let { p ->
            create("release") {
                storeFile = file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Ajustes shows BuildConfig.VERSION_NAME in its footer (spec 3c).
        buildConfig = true
    }
}

composeCompiler {
    // Immutable types the compiler cannot infer as stable (LocalDate, YearMonth, collections).
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

// Room exports the schema of every database version here. Commit this folder:
// it is what makes migrations testable (see androidx.room:room-testing).
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    constraints {
        // See libs.versions.toml → kotlinxSerialization (needed by MigrationTestHelper).
        implementation(libs.kotlinx.serialization.core)
    }

    // Jetpack Compose (versions managed by the BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Room: SQLite database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Unit tests (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests (device / emulator)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
