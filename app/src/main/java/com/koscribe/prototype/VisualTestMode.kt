package com.koscribe.prototype

/** Build-only synthetic visual checkpoint. Ordinary and release builds stay secure. */
internal object VisualTestMode {
    val enabled: Boolean get() = BuildConfig.DEBUG && BuildConfig.VISUAL_TEST &&
        StartupExperiment.configured == StartupExperiment.REVIEW
}
