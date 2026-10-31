package com.koscribe.prototype

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Setup and owned test editor. The editor stays outside the settings scroll while SwiftKey is open. */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var diagnosticStatus: TextView
    private lateinit var accessibilityAction: Button
    private lateinit var microphoneAction: Button
    private val ink = 0xFF203B36.toInt()
    private val muted = 0xFF536A5F.toInt()
    private val accent = 0xFF2E7569.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.windowInsetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFBFCFA.toInt())
            isFocusableInTouchMode = true
            requestFocus()
        }
        val intro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(8))
        }
        intro.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ko_spoken_k)
                contentDescription = null
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                background = rounded(0xFFDADDE1.toInt(), 9)
            }, LinearLayout.LayoutParams(dp(34), dp(34)))
            addView(text("KoScribe", 18f, true, ink).apply {
                setPadding(dp(10), 0, 0, 0)
            })
        })
        intro.addView(text("Ready when you are.", 24f, true, ink),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        intro.addView(text("Open a text field and use the floating K. SwiftKey stays your keyboard.",
            13f, false, muted), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })

        val statusCard = card()
        statusCard.addView(text("CURRENT STATUS", 11f, true, accent).apply { letterSpacing = 0.09f })
        status = text("Checking permissions…", 14f, false, ink).apply {
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        statusCard.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        accessibilityAction = styledButton("Open accessibility settings", false) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        statusCard.addView(accessibilityAction, LinearLayout.LayoutParams(-1, dp(42)).apply {
            topMargin = dp(8)
        })
        microphoneAction = styledButton("Request microphone permission", false) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 11)
        }
        statusCard.addView(microphoneAction, LinearLayout.LayoutParams(-1, dp(42)).apply {
            topMargin = dp(6)
        })
        intro.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        root.addView(intro)

        val testCard = card().apply {
            addView(text("TEST MESSAGE", 11f, true, accent).apply { letterSpacing = 0.09f })
            addView(text("Owned sample editor · no conversation data", 12f, false, muted),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
        }
        val testEditor = EditText(this).apply {
            id = R.id.local_test_editor
            hint = "Try text before and after the caret"
            minLines = 2
            maxLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            textSize = 16f
            setTextColor(ink)
            setHintTextColor(0xFF73847C.toInt())
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(0xFFFBFDFB.toInt(), 9).apply {
                setStroke(dp(1), 0xFFD8E4DC.toInt())
            }
        }
        testCard.addView(testEditor, LinearLayout.LayoutParams(-1, dp(92)).apply { topMargin = dp(9) })
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), dp(8))
            addView(styledButton("Open test message", true) {
                testEditor.requestFocus()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(testEditor, InputMethodManager.SHOW_IMPLICIT)
            }, LinearLayout.LayoutParams(-1, dp(48)))
            addView(testCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        })
        testEditor.setOnFocusChangeListener { _, focused ->
            // Collapse setup while editing so the owned field remains above SwiftKey.
            intro.visibility = if (focused) View.GONE else View.VISIBLE
        }

        val settings = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(6), dp(18), dp(18))
        }
        settings.addView(text(when {
            BuildConfig.TRANSLATION_INPUT ->
                "After Stop, dictated audio goes to OpenAI for English transcription. Only confirmed English is sent for Korean translation. KoScribe does not save recordings or transcripts."
            BuildConfig.SPEECH_INPUT ->
                "After Stop, dictated audio goes to OpenAI for English transcription. KoScribe does not save recordings or transcripts."
            else -> "Sample mode discards audio. No recognition or translation runs."
        }, 12f, false, muted))
        if (BuildConfig.SPEECH_INPUT) settings.addView(styledButton("API key settings", false) {
            startActivity(Intent(this, ApiKeyActivity::class.java))
        }.apply { id = R.id.open_api_key_settings }, LinearLayout.LayoutParams(-1, dp(46)).apply {
            topMargin = dp(14)
        })
        val advanced = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val advancedButton = styledButton("Advanced & diagnostics  ▾", false) {
            advanced.visibility = if (advanced.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        advancedButton.setOnClickListener {
            advanced.visibility = if (advanced.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            advancedButton.text = if (advanced.visibility == View.VISIBLE)
                "Hide advanced & diagnostics  ▴" else "Advanced & diagnostics  ▾"
        }
        settings.addView(advancedButton, LinearLayout.LayoutParams(-1, dp(46)).apply {
            topMargin = dp(8)
        })
        advanced.addView(text("Choose EN, 요, or 반말 from the bubble to begin. Stop, edit the English, then confirm. Check the intended field and cursor before Insert. KoScribe never presses Send.",
            12f, false, muted), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        advanced.addView(styledButton("Refresh diagnostic codes", false) {
            diagnosticStatus.text = KoScribeService.active()?.diagnostics()
                ?: "Accessibility service is not running."
        }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(10) })
        diagnosticStatus = text("Diagnostic codes stay in memory and contain no editor text or app names.",
            12f, false, muted)
        advanced.addView(diagnosticStatus, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })
        settings.addView(advanced)
        root.addView(ScrollView(this).apply {
            isFillViewport = false
            addView(settings)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
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
        refreshStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 11) refreshStatus()
    }

    private fun refreshStatus() {
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val connected = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName &&
                it.resolveInfo.serviceInfo.name == KoScribeService::class.java.name }
        val mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val summary = "Accessibility service  •  ${if (connected) "On" else "Off"}\n" +
            "Microphone permission  •  ${if (mic) "On" else "Off"}\n" +
            "Keyboard  •  unchanged"
        if (KoScribeService.active()?.hasPendingSession() != true && status.text.toString() != summary)
            status.text = summary
        accessibilityAction.visibility = if (connected) View.GONE else View.VISIBLE
        microphoneAction.visibility = if (mic) View.GONE else View.VISIBLE
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = rounded(Color.WHITE, 12).apply { setStroke(dp(1), 0xFFE5ECE6.toInt()) }
        elevation = dp(2).toFloat()
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun text(value: String, size: Float, bold: Boolean, color: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun styledButton(value: String, primary: Boolean, action: () -> Unit): Button =
        Button(this).apply {
            text = value
            isAllCaps = false
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (primary) Color.WHITE else muted)
            background = rounded(if (primary) accent else Color.WHITE, 10).apply {
                if (!primary) setStroke(dp(1), 0xFFDCE6DF.toInt())
            }
            setOnClickListener { action() }
        }
}
