package com.koscribe.smoke

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference

/** One standard editor containing only synthetic fixture text; no send action. */
class SmokeEditorActivity : Activity() {
    companion object {
        var current = WeakReference<SmokeEditorActivity>(null)
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = WeakReference(this)
        // This window keeps the device awake only while the synthetic fixture is visible.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val density = resources.displayMetrics.density
        val margin = (20 * density + 0.5f).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margin, margin, margin, margin)
        }
        root.addView(TextView(this).apply {
            text = "KoScribe synthetic editor test"
            textSize = 18f
        })
        root.addView(EditText(this).apply {
            id = R.id.local_test_editor
            hint = "Synthetic text only"
            minLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }, LinearLayout.LayoutParams(-1, (160 * density + 0.5f).toInt()))
        root.addView(EditText(this).apply {
            id = R.id.local_protected_editor
            hint = "Synthetic protected field"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }, LinearLayout.LayoutParams(-1, -2))
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val left = margin + bars.left
            val top = margin + bars.top
            val right = margin + bars.right
            val bottom = margin + bars.bottom
            if (view.paddingLeft != left || view.paddingTop != top ||
                view.paddingRight != right || view.paddingBottom != bottom)
                view.setPadding(left, top, right, bottom)
            insets
        }
        setContentView(root)
    }

    override fun onDestroy() {
        if (current.get() === this) current = WeakReference(null)
        super.onDestroy()
    }
}
