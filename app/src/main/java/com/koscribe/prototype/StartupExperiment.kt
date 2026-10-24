package com.koscribe.prototype

/** Build-time Phase 1 startup isolation. Release builds always use OFF. */
internal enum class StartupExperiment {
    OFF, HOLD, COLLAPSE, PANEL, OVERLAY, RESELECT, REVIEW;

    companion object {
        val configured: StartupExperiment by lazy {
            if (BuildConfig.DEBUG) valueOf(BuildConfig.STARTUP_EXPERIMENT_ARM) else OFF
        }
    }
}
