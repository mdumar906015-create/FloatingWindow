package com.example.floatwin

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var root: View? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        startFg()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        root?.let { wm.removeView(it) }
        val mode = i?.getStringExtra("mode") ?: "browser"
        val url = i?.getStringExtra("url") ?: "https://www.google.com"
        build(mode, url, i?.getIntExtra("w", 300) ?: 300, i?.getIntExtra("h", 400) ?: 400, i?.getFloatExtra("alpha", 0.9f) ?: 0.9f)
        return START_NOT_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("fw", "Floating Window", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "fw")
            .setContentTitle("फ्लोटिंग विंडो चालू है")
            .setSmallIcon(android.R.drawable.ic_menu_view).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun build(mode: String, url: String, wDp: Int, hDp: Int, alpha: Float) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val p = WindowManager.LayoutParams(
            dp(wDp), dp(hDp),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(30); y = dp(150); this.alpha = alpha
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }

        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#37474F"))
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), 0, 0, 0)
        }
        bar.addView(TextView(this).apply {
            text = if (mode == "browser") "ब्राउज़र" else "नोट्स"; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(40), 1f).also { it.gravity = Gravity.CENTER_VERTICAL })

        val content = FrameLayout(this)
        val handle = TextView(this).apply { text = "◢"; textSize = 20f; gravity = Gravity.END; setTextColor(Color.GRAY) }
        var fullH = p.height
        var minimized = false

        fun barBtn(t: String, click: () -> Unit) = Button(this).apply {
            text = t; setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT)
            minWidth = 0; minimumWidth = dp(44); setOnClickListener { click() }
        }
        bar.addView(barBtn("—") {
            minimized = !minimized
            content.visibility = if (minimized) View.GONE else View.VISIBLE
            handle.visibility = content.visibility
            if (minimized) fullH = p.height
            p.height = if (minimized) dp(40) else fullH
            wm.updateViewLayout(box, p)
        }, LinearLayout.LayoutParams(dp(44), dp(40)))
        bar.addView(barBtn("✕") { stopSelf() }, LinearLayout.LayoutParams(dp(44), dp(40)))

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        bar.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y }
                MotionEvent.ACTION_MOVE -> {
                    p.x = ox + (e.rawX - sx).toInt(); p.y = oy + (e.rawY - sy).toInt()
                    wm.updateViewLayout(box, p)
                }
            }
            true
        }

        if (mode == "browser") {
            content.addView(WebView(this).apply {
                settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                webViewClient = WebViewClient(); loadUrl(url)
            })
        } else {
            val prefs = getSharedPreferences("notes", MODE_PRIVATE)
            content.addView(EditText(this).apply {
                gravity = Gravity.TOP; hint = "यहाँ लिखें..."; setText(prefs.getString("t", ""))
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) { prefs.edit().putString("t", s.toString()).apply() }
                    override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                    override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                })
            })
        }

        var rw = 0; var rh = 0
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; rw = p.width; rh = p.height }
                MotionEvent.ACTION_MOVE -> {
                    p.width = (rw + (e.rawX - sx)).toInt().coerceAtLeast(dp(160))
                    p.height = (rh + (e.rawY - sy)).toInt().coerceAtLeast(dp(120))
                    wm.updateViewLayout(box, p)
                }
            }
            true
        }

        box.addView(bar, LinearLayout.LayoutParams(-1, dp(40)))
        box.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        box.addView(handle, LinearLayout.LayoutParams(-1, dp(28)))
        wm.addView(box, p)
        root = box
    }

    override fun onDestroy() { root?.let { wm.removeView(it) }; root = null; super.onDestroy() }
}
