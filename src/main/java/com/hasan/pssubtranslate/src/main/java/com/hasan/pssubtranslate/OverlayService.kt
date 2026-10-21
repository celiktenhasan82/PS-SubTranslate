package com.hasan.pssubtranslate

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var subtitleView: TextView

    private val subtitleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val text = intent?.getStringExtra("TEXT")
            if (!text.isNullOrBlank()) {
                subtitleView.text = text
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        subtitleView = TextView(this).apply {
            text = "Çeviri Bekleniyor..."
            setTextColor(Color.parseColor("#F5F5F5"))
            setBackgroundColor(Color.parseColor("#99000000"))
            textSize = 24f
            setPadding(30, 20, 30, 20)
            gravity = Gravity.CENTER
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        layoutParams.y = 100

        windowManager.addView(subtitleView, layoutParams)

        val filter = IntentFilter("UPDATE_SUBTITLE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(subtitleReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(subtitleReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(subtitleReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (::subtitleView.isInitialized) {
            windowManager.removeView(subtitleView)
        }
    }
}
