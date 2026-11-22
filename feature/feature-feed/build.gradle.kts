import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localPropsFile = rootProject.file("local.properties")
val localProps = Properties().also { props ->
    if (localPropsFile.exists()) FileInputStream(localPropsFile).use { props.load(it) }
}

// Room exports each schema version here: it is the reference for writing and testing migrations
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.nexus.feature.feed"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    // A client secret inside an APK can be extracted by anyone, so it is only compiled
    // into debug builds. Release falls back to iTunes until the Spotify token is
    // requested from a backend (e.g. a Cloud Function).
    buildTypes {
        debug {
            buildConfigField("String", "SPOTIFY_CLIENT_ID",
                "String.valueOf(\"${localProps.getProperty("SPOTIFY_CLIENT_ID", "")}\")")
            buildConfigField("String", "SPOTIFY_CLIENT_SECRET",
                "String.valueOf(\"${localProps.getProperty("SPOTIFY_CLIENT_SECRET", "")}\")")
        }
        release {
            buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"\"")
            buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"\"")
        }
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-firebase"))
    implementation(project(":core:core-storage"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.core)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Room + Paging
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Other
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
