plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nexus.core.firebase"
    compileSdk = 35

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
}

dependencies {
    // Expose Firebase SDK classes to all dependent modules
    api(platform(libs.firebase.bom))
    api(libs.firebase.firestore)
    api(libs.firebase.storage)
    api(libs.firebase.auth)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
