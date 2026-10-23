plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val startupExperimentArm = providers.gradleProperty("koscribeStartupArm").orElse("OFF").get()
require(startupExperimentArm in setOf("OFF", "HOLD", "COLLAPSE", "PANEL", "OVERLAY", "RESELECT")) {
    "koscribeStartupArm must be OFF, HOLD, COLLAPSE, PANEL, OVERLAY, or RESELECT"
}

android {
    namespace = "com.koscribe.prototype"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.koscribe.prototype"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-phase1"
    }

    buildFeatures { buildConfig = true }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "STARTUP_EXPERIMENT_ARM", "\"$startupExperimentArm\"")
        }
        getByName("release") {
            buildConfigField("String", "STARTUP_EXPERIMENT_ARM", "\"OFF\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
