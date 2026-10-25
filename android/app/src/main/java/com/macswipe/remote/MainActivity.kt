package com.macswipe.remote

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val bg = Color.rgb(14, 15, 24)
    private val panel = Color.rgb(24, 26, 41)
    private val border = Color.rgb(59, 57, 89)
    private val bright = Color.rgb(170, 150, 255)
    private val subtle = Color.rgb(157, 159, 181)
    private lateinit var remote: RemoteSocket
    private lateinit var ipField: EditText
    private lateinit var tokenField: EditText
    private lateinit var connectButton: Button
    private lateinit var status: TextView
    private var isConnected = false

    private fun dp(v: Int): Int = (resources.displayMetrics.density * v).toInt()
    private fun shape(color: Int, r: Float = 17f, stroke: Int = border) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(r.toInt()).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun text(label: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = label
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            includeFontPadding = false
        }

    private fun field(hint: String, value: String, secret: Boolean = false) =
        EditText(this).apply {
            setSingleLine(true)
            setText(value)
            this.hint = hint
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(114, 113, 137))
            textSize = 14f
            inputType = if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSelectAllOnFocus(true)
            background = shape(panel, 12f)
            setPadding(dp(12), 0, dp(12), 0)
        }

    private fun action(label: String, command: String): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.rgb(230, 227, 255))
            backgroundTintList = null
            background = shape(panel, 13f)
            setPadding(dp(2), 0, dp(2), 0)
            setOnClickListener { remote.send(command) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val prefs = getSharedPreferences("connection", MODE_PRIVATE)
        remote = RemoteSocket { ready, info ->
            isConnected = ready
            status.text = (if (ready) "●  " else "○  ") + info
            status.setTextColor(if (ready) Color.rgb(126, 231, 189) else subtle)
            connectButton.text = if (ready) "Disconnect" else "Connect"
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(16), dp(18), dp(14))
        }
        setContentView(root)

        root.addView(text("MACSWIPE", 12f, bright, true).apply { letterSpacing = 0.22f },
            LinearLayout.LayoutParams(-1, dp(23)))
        root.addView(text("Your Mac, at your fingertips.", 22f, Color.WHITE, true),
            LinearLayout.LayoutParams(-1, dp(40)))
        root.addView(text("Connect over the same Wi-Fi network", 12f, subtle),
            LinearLayout.LayoutParams(-1, dp(28)))

        val addressRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        ipField = field("Mac IP · 192.168.x.x", prefs.getString("ip", "") ?: "")
        addressRow.addView(ipField, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(8) })
        connectButton = Button(this).apply {
            text = "Connect"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 13f
            backgroundTintList = null
            background = shape(Color.rgb(103, 79, 214), 12f, Color.rgb(103, 79, 214))
        }
        addressRow.addView(connectButton, LinearLayout.LayoutParams(dp(108), dp(48)))
        root.addView(addressRow, LinearLayout.LayoutParams(-1, dp(48)))

        tokenField = field("Token shown on the Mac", prefs.getString("token", "") ?: "", true)
        root.addView(tokenField, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        status = text("○  Not connected", 12f, subtle)
        root.addView(status, LinearLayout.LayoutParams(-1, dp(35)).apply { topMargin = dp(10) })

        connectButton.setOnClickListener {
            if (isConnected) {
                remote.disconnect()
            } else {
                val host = ipField.text.toString().trim()
                val token = tokenField.text.toString().trim()
                if (host.isEmpty() || token.isEmpty()) {
                    status.text = "Enter Mac IP and pairing token"
                    return@setOnClickListener
                }
                prefs.edit().putString("ip", host).putString("token", token).apply()
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(ipField.windowToken, 0)
                ipField.clearFocus(); tokenField.clearFocus()
                status.text = "○  Connecting…"
                remote.connect(host, token)
            }
        }

        root.addView(text("QUICK ACTIONS", 11f, subtle, true).apply { letterSpacing = 0.12f },
            LinearLayout.LayoutParams(-1, dp(28)))
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val actions1 = listOf("← Desktop" to "space_left", "Mission Ctrl" to "mission_control", "Desktop →" to "space_right")
        actions1.forEachIndexed { index, (label, command) ->
            row1.addView(action(label, command), LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                if (index > 0) leftMargin = dp(6)
            })
        }
        root.addView(row1, LinearLayout.LayoutParams(-1, dp(48)))
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val actions2 = listOf("App Exposé" to "app_expose", "Copy" to "copy", "Paste" to "paste", "Esc" to "escape")
        actions2.forEachIndexed { index, (label, command) ->
            row2.addView(action(label, command), LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                if (index > 0) leftMargin = dp(6)
            })
        }
        root.addView(row2, LinearLayout.LayoutParams(-1, dp(43)).apply { topMargin = dp(6) })

        root.addView(text("TRACKPAD", 11f, subtle, true).apply { letterSpacing = 0.12f },
            LinearLayout.LayoutParams(-1, dp(32)).apply { topMargin = dp(15) })
        val pad = TrackpadView(this) { command, dx, dy -> remote.send(command, dx, dy) }.apply {
            background = shape(panel, 20f, Color.rgb(67, 61, 107))
            contentDescription = "Gesture trackpad for controlling Mac"
        }
        root.addView(pad, LinearLayout.LayoutParams(-1, 0, 1f))
        val hints = text("TAP  click      TWO-FINGER TAP  right-click      HOLD  drag\nTHREE-FINGER SWIPE  switch desktops / window overview", 11f, subtle)
        hints.gravity = Gravity.CENTER
        hints.textAlignment = View.TEXT_ALIGNMENT_CENTER
        root.addView(hints, LinearLayout.LayoutParams(-1, dp(60)).apply { topMargin = dp(3) })
    }

    override fun onDestroy() {
        remote.shutdown()
        super.onDestroy()
    }
}
