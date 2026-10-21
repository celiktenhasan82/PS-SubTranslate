package com.hasan.pssubtranslate

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val captureIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = "START_CAPTURE"
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
            }
            startService(captureIntent)
            startService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Motor Çalıştı! Uzaktan Oynatma uygulamasına geçebilirsiniz.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Ekran kaydı izni verilmedi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                Toast.makeText(this, "Lütfen 'Diğer uygulamaların üzerinde göster' iznini açın.", Toast.LENGTH_LONG).show()
            } else {
                val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                screenCaptureLauncher.launch(captureIntent)
            }
        }

        btnStop.setOnClickListener {
            val intentCapture = Intent(this, ScreenCaptureService::class.java).apply { action = "STOP_CAPTURE" }
            startService(intentCapture)
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Sistem Durduruldu.", Toast.LENGTH_SHORT).show()
        }
    }
}
