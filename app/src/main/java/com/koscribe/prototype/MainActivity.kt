package com.koscribe.prototype

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.view.accessibility.AccessibilityManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.app.Activity

class MainActivity : Activity() {
    private lateinit var status: android.widget.TextView
    private lateinit var diagnosticStatus: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (BuildConfig.SPEECH_INPUT) root.addView(action("API key settings") {
            startActivity(Intent(this, ApiKeyActivity::class.java))
        }.apply { id = R.id.open_api_key_settings })
        val editorPanel = column()
        editorPanel.addView(caption("KoScribe · local test editor", true))
        editorPanel.addView(caption("Focus this ordinary text field, then choose a mode from the bubble. SwiftKey stays selected."))
        val testEditor = EditText(this).apply {
            id = R.id.local_test_editor
            hint = "Try text before and after the caret"
            minLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        editorPanel.addView(testEditor, LinearLayout.LayoutParams(-1, dp(160)))
        root.addView(editorPanel, LinearLayout.LayoutParams(-1, -2))

        // Keep the fixture outside the long settings scroll and its changing
        // viewport when the keyboard opens or a recording dialog returns.
        val content = column()
        content.addView(caption(when {
            BuildConfig.TRANSLATION_INPUT ->
                "Speech input: choose EN, 요, or 반말 from the bubble to start recording. Tap Stop, then edit and confirm the English transcript. Korean modes translate after confirmation. Audio is sent to OpenAI after Stop; KoScribe does not save recordings or transcripts."
            BuildConfig.SPEECH_INPUT ->
                "Speech input: choose EN from the bubble to start recording. Tap Stop, then edit and confirm the English transcript. Audio is sent to OpenAI after Stop; KoScribe does not save recordings or transcripts."
            else -> "Phase 1 uses fixed samples. Audio is discarded; no recognition or translation runs."
        }))
        status = caption("")
        content.addView(status)
        content.addView(action("Open Accessibility settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(action("Request microphone permission") {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 11)
        })
        content.addView(caption("Test with ordinary text only. The app never presses Send. A failed verification leaves the sample available from the bubble for deliberate Copy."))
        content.addView(action("Refresh diagnostic codes") {
            diagnosticStatus.text = KoScribeService.active()?.diagnostics()
                ?: "Accessibility service is not running."
        })
        diagnosticStatus = caption("Diagnostic codes stay in memory and contain no editor text or app names.")
        content.addView(diagnosticStatus)
        root.addView(ScrollView(this).apply { addView(content) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            if (view.paddingLeft != bars.left || view.paddingTop != bars.top ||
                view.paddingRight != bars.right || view.paddingBottom != bars.bottom)
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val connected = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName && it.resolveInfo.serviceInfo.name == KoScribeService::class.java.name }
        val mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val summary = "Accessibility: ${if (connected) "on" else "off"}\nMicrophone permission: ${if (mic) "on" else "off"}\nKeyboard: remains your selected keyboard"
        if (KoScribeService.active()?.hasPendingSession() != true && status.text.toString() != summary)
            status.text = summary
    }
}
