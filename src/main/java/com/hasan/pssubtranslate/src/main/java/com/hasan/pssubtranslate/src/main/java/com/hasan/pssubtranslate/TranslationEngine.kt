package com.hasan.pssubtranslate

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TranslationEngine {

    // İngilizce'den Türkçe'ye çeviri ayarları
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.TURKISH)
        .build()
    
    private val englishTurkishTranslator = Translation.getClient(options)
    
    // Metin okuma (OCR) motoru
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Çeviri paketini cihazına indirir (Sadece ilk kullanımda indirir, sonra internetsiz çalışır)
    fun downloadModelIfNeeded(onSuccess: () -> Unit) {
        englishTurkishTranslator.downloadModelIfNeeded()
            .addOnSuccessListener {
                Log.d("PS-Translate", "Türkçe çeviri modeli hazır!")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("PS-Translate", "Model indirilemedi", exception)
            }
    }

    // Ekrandan alınan resmi oku ve çevir
    fun processFrameAndTranslate(bitmap: Bitmap, onTranslated: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        // 1. Aşama: Resimdeki yazıyı oku
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val originalText = visionText.text
                if (originalText.isNotBlank()) {
                    // 2. Aşama: Okunan yazıyı Türkçeye çevir
                    translateText(originalText, onTranslated)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PS-Translate", "Yazı okunamadı", e)
            }
    }

    // Çeviri işlemini yapan alt fonksiyon
    private fun translateText(text: String, onTranslated: (String) -> Unit) {
        englishTurkishTranslator.translate(text)
            .addOnSuccessListener { translatedText ->
                // Çeviri başarılı olursa sonucu şeffaf ekrana yolla
                onTranslated(translatedText)
            }
            .addOnFailureListener { exception ->
                Log.e("PS-Translate", "Çeviri hatası", exception)
            }
    }
}
