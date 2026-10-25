plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val startupExperimentArm = providers.gradleProperty("koscribeStartupArm").orElse("OFF").get()
require(startupExperimentArm in setOf("OFF", "HOLD", "COLLAPSE", "PANEL", "OVERLAY", "RESELECT", "REVIEW")) {
    "koscribeStartupArm must be OFF, HOLD, COLLAPSE, PANEL, OVERLAY, RESELECT, or REVIEW"
}
val visualTest = providers.gradleProperty("koscribeVisualTest").orElse("false").get()
require(visualTest == "true" || visualTest == "false") {
    "koscribeVisualTest must be true or false"
}
require(visualTest != "true" || startupExperimentArm == "REVIEW") {
    "koscribeVisualTest requires koscribeStartupArm=REVIEW"
}
val speechInput = providers.gradleProperty("koscribeSpeech").orElse("false").get()
require(speechInput == "true" || speechInput == "false") {
    "koscribeSpeech must be true or false"
}
require(speechInput != "true" || (startupExperimentArm == "REVIEW" && visualTest == "false")) {
    "koscribeSpeech requires secure REVIEW and koscribeVisualTest=false"
}
val translationInput = providers.gradleProperty("koscribeTranslation").orElse("false").get()
require(translationInput == "true" || translationInput == "false") {
    "koscribeTranslation must be true or false"
}
require(translationInput != "true" ||
    (speechInput == "true" && startupExperimentArm == "REVIEW" && visualTest == "false")) {
    "koscribeTranslation requires speech input, secure REVIEW, and koscribeVisualTest=false"
}
val autoInsert = providers.gradleProperty("koscribeAutoInsert").orElse("false").get()
require(autoInsert == "true" || autoInsert == "false") {
    "koscribeAutoInsert must be true or false"
}
require(autoInsert != "true" || (startupExperimentArm == "REVIEW" && visualTest == "false")) {
    "koscribeAutoInsert requires secure REVIEW and koscribeVisualTest=false"
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
            buildConfigField("boolean", "VISUAL_TEST", visualTest)
            buildConfigField("boolean", "SPEECH_INPUT", speechInput)
            buildConfigField("boolean", "TRANSLATION_INPUT", translationInput)
            buildConfigField("boolean", "AUTO_INSERT", autoInsert)
        }
        getByName("release") {
            buildConfigField("String", "STARTUP_EXPERIMENT_ARM", "\"OFF\"")
            buildConfigField("boolean", "VISUAL_TEST", "false")
            buildConfigField("boolean", "SPEECH_INPUT", "false")
            buildConfigField("boolean", "TRANSLATION_INPUT", "false")
            buildConfigField("boolean", "AUTO_INSERT", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
}
