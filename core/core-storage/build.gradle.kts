import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localPropsFile = rootProject.file("local.properties")
val localProps = Properties().also { props ->
    if (localPropsFile.exists()) FileInputStream(localPropsFile).use { props.load(it) }
}

android {
    namespace = "com.nexus.core.storage"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        // Supabase project that stores the media (see SETUP.md). The anon key is a public,
        // publishable key: what protects the bucket are its policies.
        // String.valueOf keeps them out of compile-time constants: Kotlin would otherwise copy
        // the values into the classes that read them, and an incremental build that only
        // regenerates BuildConfig would keep shipping the old (empty) keys.
        buildConfigField("String", "SUPABASE_URL",
            "String.valueOf(\"${localProps.getProperty("SUPABASE_URL", "")}\")")
        buildConfigField("String", "SUPABASE_ANON_KEY",
            "String.valueOf(\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\")")
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
    }
}

dependencies {
    implementation(project(":core:core-common"))
    // Firebase Auth issues the token the bucket policies check
    implementation(project(":core:core-firebase"))

    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
