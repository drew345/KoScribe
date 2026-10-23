package com.koscribe.prototype

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.EditText
import android.widget.LinearLayout
import android.app.Activity

class MainActivity : Activity() {
    private lateinit var status: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = column()
        content.addView(caption("KoScribe · Phase 1", true))
        content.addView(caption("Microphone and safe insertion feasibility test. Audio is discarded. The English and Korean phrases are fixed samples; no recognition or translation runs."))
        status = caption("")
        content.addView(status)
        content.addView(action("Open Accessibility settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(action("Request microphone permission") {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 11)
        })
        content.addView(caption("Local test editor: focus below, then tap the floating bubble. SwiftKey should remain selected."))
        val testEditor = EditText(this).apply {
            id = android.view.View.generateViewId()
            hint = "Try text before and after the caret"
            minLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        content.addView(testEditor, LinearLayout.LayoutParams(-1, dp(160)))
        content.addView(caption("Test with ordinary text only. The app never presses Send. A failed verification leaves the sample available from the bubble for deliberate Copy."))
        setContentView(insetScroll(content))
    }

    override fun onResume() {
        super.onResume()
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val connected = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName && it.resolveInfo.serviceInfo.name == KoScribeService::class.java.name }
        val mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        status.text = "Accessibility: ${if (connected) "on" else "off"}\nMicrophone permission: ${if (mic) "on" else "off"}\nKeyboard: remains your selected keyboard"
    }
}
