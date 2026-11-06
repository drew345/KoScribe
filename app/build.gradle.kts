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
val karrotServiceProbe = providers.gradleProperty("koscribeKarrotServiceProbe").orElse("false").get()
require(karrotServiceProbe == "true" || karrotServiceProbe == "false") {
    "koscribeKarrotServiceProbe must be true or false"
}
require(karrotServiceProbe != "true" ||
    (startupExperimentArm == "REVIEW" && visualTest == "false")) {
    "koscribeKarrotServiceProbe requires secure REVIEW and koscribeVisualTest=false"
}
val karrotEmptyConnectionProbe = providers.gradleProperty("koscribeKarrotEmptyConnectionProbe")
    .orElse("false").get()
require(karrotEmptyConnectionProbe == "true" || karrotEmptyConnectionProbe == "false") {
    "koscribeKarrotEmptyConnectionProbe must be true or false"
}
require(karrotEmptyConnectionProbe != "true" ||
    (startupExperimentArm == "REVIEW" && visualTest == "false")) {
    "koscribeKarrotEmptyConnectionProbe requires secure REVIEW and koscribeVisualTest=false"
}
val karrotNoReadProbe = providers.gradleProperty("koscribeKarrotNoReadProbe").orElse("false").get()
require(karrotNoReadProbe == "true" || karrotNoReadProbe == "false") {
    "koscribeKarrotNoReadProbe must be true or false"
}
require(karrotNoReadProbe != "true" ||
    (startupExperimentArm == "REVIEW" && visualTest == "false")) {
    "koscribeKarrotNoReadProbe requires secure REVIEW and koscribeVisualTest=false"
}
val reviewSurfaceProbe = providers.gradleProperty("koscribeReviewSurfaceProbe").orElse("OFF").get()
val stableOverlayAuto = providers.gradleProperty("koscribeStableOverlayAuto").orElse("false").get()
val relativeReadProbe = providers.gradleProperty("koscribeRelativeReadProbe").orElse("OFF").get()
require(relativeReadProbe in setOf("OFF", "FIXTURE", "KARROT", "FIXTURE_SELECTION", "KARROT_SELECTION"))
require(relativeReadProbe == "OFF" || (startupExperimentArm == "REVIEW" &&
    reviewSurfaceProbe == "OFF" && stableOverlayAuto == "false" &&
    listOf(visualTest, speechInput, translationInput, autoInsert, karrotServiceProbe,
        karrotEmptyConnectionProbe, karrotNoReadProbe).all { it == "false" })) {
    "Relative read probe requires secure REVIEW with every other feature and experiment disabled"
}
require(stableOverlayAuto in setOf("true", "false"))
require(stableOverlayAuto != "true" || (startupExperimentArm == "REVIEW" && reviewSurfaceProbe == "OFF" &&
    listOf(visualTest, speechInput, translationInput, autoInsert, karrotServiceProbe,
        karrotEmptyConnectionProbe, karrotNoReadProbe).all { it == "false" })) {
    "Stable overlay sample requires secure REVIEW with other experiments disabled"
}
require(reviewSurfaceProbe in setOf("OFF", "ACTIVITY", "OVERLAY")) {
    "koscribeReviewSurfaceProbe must be OFF, ACTIVITY, or OVERLAY"
}
require(reviewSurfaceProbe == "OFF" || (startupExperimentArm == "REVIEW" &&
    listOf(visualTest, speechInput, translationInput, autoInsert, karrotServiceProbe,
        karrotEmptyConnectionProbe, karrotNoReadProbe).all { it == "false" })) {
    "Review surface probe requires secure REVIEW with every other experiment disabled"
}

android {
    namespace = "com.koscribe.prototype"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.koscribe.prototype"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "0.2"
    }

    buildFeatures { buildConfig = true }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "STARTUP_EXPERIMENT_ARM", "\"$startupExperimentArm\"")
            buildConfigField("boolean", "VISUAL_TEST", visualTest)
            buildConfigField("boolean", "SPEECH_INPUT", speechInput)
            buildConfigField("boolean", "TRANSLATION_INPUT", translationInput)
            buildConfigField("boolean", "AUTO_INSERT", autoInsert)
            buildConfigField("boolean", "KARROT_SERVICE_PROBE", karrotServiceProbe)
            buildConfigField("boolean", "KARROT_EMPTY_CONNECTION_PROBE", karrotEmptyConnectionProbe)
            buildConfigField("boolean", "KARROT_NO_READ_PROBE", karrotNoReadProbe)
            buildConfigField("String", "REVIEW_SURFACE_PROBE", "\"$reviewSurfaceProbe\"")
            buildConfigField("boolean", "STABLE_OVERLAY_AUTO", stableOverlayAuto)
            buildConfigField("String", "RELATIVE_READ_PROBE_ARM", "\"$relativeReadProbe\"")
        }
        getByName("release") {
            buildConfigField("String", "STARTUP_EXPERIMENT_ARM", "\"OFF\"")
            buildConfigField("boolean", "VISUAL_TEST", "false")
            buildConfigField("boolean", "SPEECH_INPUT", "false")
            buildConfigField("boolean", "TRANSLATION_INPUT", "false")
            buildConfigField("boolean", "AUTO_INSERT", "false")
            buildConfigField("boolean", "KARROT_SERVICE_PROBE", "false")
            buildConfigField("boolean", "KARROT_EMPTY_CONNECTION_PROBE", "false")
            buildConfigField("boolean", "KARROT_NO_READ_PROBE", "false")
            buildConfigField("String", "REVIEW_SURFACE_PROBE", "\"OFF\"")
            buildConfigField("boolean", "STABLE_OVERLAY_AUTO", "false")
            buildConfigField("String", "RELATIVE_READ_PROBE_ARM", "\"OFF\"")
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
