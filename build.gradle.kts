plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}

// Compose setup shared by every module that applies the Compose compiler plugin
subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
        extensions.configure<org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension> {
            // Default from Kotlin 2.0.20; skips composables whose arguments did not change
            // even when their types are unstable
            enableStrongSkippingMode.set(true)
            stabilityConfigurationFile.set(rootProject.layout.projectDirectory.file("compose-stability.conf"))
        }
    }
}
