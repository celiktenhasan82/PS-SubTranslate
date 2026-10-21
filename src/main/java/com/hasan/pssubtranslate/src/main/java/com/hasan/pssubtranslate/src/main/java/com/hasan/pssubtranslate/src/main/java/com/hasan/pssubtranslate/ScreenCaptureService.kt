package com.hasan.pssubtranslate

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.*

class ScreenCaptureService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val translationEngine = TranslationEngine()
    private val captureScope = CoroutineScope(Dispatchers.Default + Job())
    private var isCapturing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        translationEngine.downloadModelIfNeeded {
            // Dil paketi hazır
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_CAPTURE") {
            val resultCode = intent.getIntExtra("RESULT_CODE", 0)
            val data = intent.getParcelableExtra<Intent>("DATA")
            
            if (resultCode != 0 && data != null) {
                startScreenCapture(resultCode, data)
            }
        } else if (intent?.action == "STOP_CAPTURE") {
            stopScreenCapture()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("WrongConstant")
    private fun startScreenCapture(resultCode: Int, data: Intent) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val screenDensity = metrics.densityDpi

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "PS-SubTranslate_Screen",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        isCapturing = true
        startCaptureLoop(screenWidth, screenHeight)
    }

    private fun startCaptureLoop(screenWidth: Int, screenHeight: Int) {
        captureScope.launch {
            while (isCapturing) {
                delay(500) // Saniyede 2 kare al
                
                val image: Image? = imageReader?.acquireLatestImage()
                if (image != null) {
                    val bitmap = imageToBitmap(image, screenWidth, screenHeight)
                    image.close()
                    
                    if (bitmap != null) {
                        // Altyazı bölgesini (ekranın alt %20'si) kırp
                        val roiHeight = (screenHeight * 0.20).toInt()
                        val roiY = screenHeight - roiHeight - 50 
                        
                        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, roiY, screenWidth, roiHeight)
                        
                        translationEngine.processFrameAndTranslate(croppedBitmap) { translatedText ->
                            updateOverlayText(translatedText)
                        }
                    }
                }
            }
        }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        
        val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        
        return Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }

    private fun updateOverlayText(text: String) {
        val intent = Intent("UPDATE_SUBTITLE")
        intent.putExtra("TEXT", text)
        sendBroadcast(intent)
    }

    private fun stopScreenCapture() {
        isCapturing = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        captureScope.cancel()
    }
}
