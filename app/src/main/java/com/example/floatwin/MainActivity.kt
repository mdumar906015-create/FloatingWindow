package com.example.floatwin

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            buildScreen()
        } catch (t: Throwable) {
            val tv = TextView(this)
            tv.text = "Error:\n" + android.util.Log.getStackTraceString(t)
            tv.setPadding(30, 60, 30, 30)
            tv.setTextIsSelectable(true)
            setContentView(ScrollView(this).apply { addView(tv) })
        }
    }

    private fun buildScreen() {
        val d = resources.displayMetrics.density
        val pad = (20 * d).toInt()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        fun label(t: String) = TextView(this).apply { text = t; textSize = 14f; setPadding(0, pad / 2, 0, 0) }
        fun edit(v: String, num: Boolean = false) = EditText(this).apply {
            setText(v); setSingleLine()
            if (num) inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        col.addView(TextView(this).apply { text = "फ्लोटिंग विंडो"; textSize = 24f; gravity = Gravity.CENTER })

        col.addView(label("विंडो में क्या दिखाना है?"))
        val mode = Spinner(this)
        mode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("ब्राउज़र", "नोट्स"))
        col.addView(mode)

        col.addView(label("वेबसाइट (URL)"))
        val url = edit("https://www.google.com"); col.addView(url)

        col.addView(label("चौड़ाई (dp)"))
        val w = edit("300", true); col.addView(w)

        col.addView(label("ऊँचाई (dp)"))
        val h = edit("400", true); col.addView(h)

        col.addView(label("पारदर्शिता: विंडो कितनी साफ़ दिखे (30–100)"))
        val op = SeekBar(this).apply { max = 70; progress = 60 }
        col.addView(op)

        col.addView(Button(this).apply {
            text = "फ्लोटिंग विंडो शुरू करें"
            setOnClickListener {
                try {
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        Toast.makeText(context, "पहले 'दूसरे ऐप्स के ऊपर दिखाएँ' की अनुमति दें", Toast.LENGTH_LONG).show()
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        return@setOnClickListener
                    }
                    if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                val i = Intent(context, FloatingService::class.java)
                    .putExtra("mode", if (mode.selectedItemPosition == 0) "browser" else "notes")
                    .putExtra("url", url.text.toString().ifBlank { "https://www.google.com" })
                    .putExtra("w", w.text.toString().toIntOrNull() ?: 300)
                    .putExtra("h", h.text.toString().toIntOrNull() ?: 400)
                    .putExtra("alpha", (op.progress + 30) / 100f)
                startForegroundService(i)
            } catch (t: Throwable) {
                Toast.makeText(context, "Error: " + t.toString(), Toast.LENGTH_LONG).show()
            }
        }
    })
    col.addView(Button(this).apply {
        text = "बंद करें"
        setOnClickListener { stopService(Intent(context, FloatingService::class.java)) }
    })
    setContentView(ScrollView(this).apply { addView(col) })
}
}
