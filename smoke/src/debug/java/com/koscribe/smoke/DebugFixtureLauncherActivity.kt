package com.koscribe.smoke

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Debug launcher for manual testing of the internal synthetic editor. */
class DebugFixtureLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Construct a fresh explicit Intent; never forward caller extras, data, or actions.
        startActivity(Intent(this, SmokeEditorActivity::class.java).apply {
            putExtra(SmokeEditorActivity.EXTRA_MANUAL_FIXTURE, true)
        })
        finish()
    }
}
