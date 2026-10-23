plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.koscribe.smoke"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.koscribe.smoke"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-local-smoke"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.test:runner:1.7.0")
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("junit:junit:4.13.2")
}

// This package is a local device test tool, never a release artifact.
androidComponents.beforeVariants(androidComponents.selector().withBuildType("release")) {
    it.enable = false
}
