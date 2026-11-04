package com.koscribe.prototype

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Key entry only. Never displays or returns a saved credential. */
class ApiKeyActivity : Activity() {
    private lateinit var editor: EditText
    private lateinit var status: TextView
    private lateinit var save: Button
    private lateinit var delete: Button
    private var busy = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ReviewSurfaceProbeMode.block()) { finish(); return }
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            isSaveEnabled = false
            isSaveFromParentEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        content.addView(TextView(this).apply {
            text = "KoScribe API key"
            textSize = 22f
        })
        content.addView(TextView(this).apply {
            text = "Paste your dedicated key on this phone. Keep it out of chats. " +
                "Store your backup in Cryptomator DeveloperStuff. Saved keys cannot be shown here."
        })
        editor = EditText(this).apply {
            id = R.id.api_key_entry
            hint = "API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            isSaveEnabled = false
            isSaveFromParentEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO
        }
        content.addView(editor, LinearLayout.LayoutParams(-1, -2))
        status = TextView(this).apply {
            id = R.id.api_key_status
            text = "Checking saved key…"
        }
        content.addView(status)
        save = Button(this).apply {
            id = R.id.api_key_save
            text = "Save key"
            setOnClickListener { saveKey() }
        }
        content.addView(save)
        delete = Button(this).apply {
            id = R.id.api_key_delete
            text = "Delete saved key"
            setOnClickListener { deleteKey() }
        }
        content.addView(delete)
        content.addView(Button(this).apply {
            id = R.id.api_key_close
            text = "Close"
            setOnClickListener { finish() }
        })
        setContentView(ScrollView(this).apply {
            isSaveEnabled = false
            isSaveFromParentEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
            addView(content)
        })
        setBusy(true)
        Thread {
            val vault = ApiKeyStore(this)
            val entry = vault.hasStoredEntry()
            val present = entry && vault.hasKey()
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    status.text = when {
                        present -> "A key is saved on this device."
                        entry -> "A saved key entry is unusable. Delete it before saving a new key."
                        else -> "No key is saved on this device."
                    }
                    setBusy(false)
                }
            }
        }.start()
    }

    private fun saveKey() {
        if (busy) return
        val value = editor.text.toString()
        clearEntry()
        KoScribeService.active()?.cancelSpeechForCredentialChange()
        setBusy(true)
        status.text = "Saving…"
        Thread {
            val saved = ApiKeyStore(this).saveKey(value)
            runOnUiThread {
                if (saved) KoScribeService.active()?.cancelSpeechForCredentialChange()
                if (!isFinishing && !isDestroyed) {
                    status.text = if (saved) "Key saved on this device." else
                        "Key not saved. Check the entry or delete an unusable saved key first."
                    setBusy(false)
                }
            }
        }.start()
    }

    private fun deleteKey() {
        if (busy) return
        clearEntry()
        KoScribeService.active()?.cancelSpeechForCredentialChange()
        setBusy(true)
        status.text = "Deleting…"
        Thread {
            val deleted = ApiKeyStore(this).deleteKey()
            runOnUiThread {
                if (deleted) KoScribeService.active()?.cancelSpeechForCredentialChange()
                if (!isFinishing && !isDestroyed) {
                    status.text = if (deleted) "Saved key deleted." else
                        "Could not finish deleting the key. Try again."
                    setBusy(false)
                }
            }
        }.start()
    }

    private fun setBusy(value: Boolean) {
        busy = value
        save.isEnabled = !value
        delete.isEnabled = !value
    }

    private fun clearEntry() {
        if (::editor.isInitialized) editor.text?.clear()
    }

    override fun onStop() {
        clearEntry()
        super.onStop()
    }

    override fun onDestroy() {
        clearEntry()
        super.onDestroy()
    }
}
