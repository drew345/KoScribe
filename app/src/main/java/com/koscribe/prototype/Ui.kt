package com.koscribe.prototype

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

fun Context.dp(value: Int): Int = (resources.displayMetrics.density * value + 0.5f).toInt()

fun Context.column(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(20), dp(20), dp(20), dp(20))
}

fun Context.insetScroll(content: LinearLayout): ScrollView = ScrollView(this).apply {
    isFillViewport = true
    addView(content)
    setOnApplyWindowInsetsListener { view, insets ->
        val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val ime = insets.getInsets(WindowInsets.Type.ime())
        view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
        insets
    }
}

fun Context.caption(value: String, bold: Boolean = false): TextView = TextView(this).apply {
    text = value
    textSize = if (bold) 20f else 15f
    setTextColor(Color.rgb(30, 30, 30))
    if (bold) setTypeface(null, Typeface.BOLD)
    setPadding(0, dp(4), 0, dp(12))
}

fun Context.action(value: String, handler: (View) -> Unit): Button = Button(this).apply {
    text = value
    setOnClickListener(handler)
}
