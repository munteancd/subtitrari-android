package com.cristi.subtitrari

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * The on-device fallback for when Gemini cannot answer. It understands English only,
 * so there is nothing for it to do when English is the chosen language.
 */
fun newTranslator(language: CaptionLanguage): Translator? {
    if (language.code == TranslateLanguage.ENGLISH) return null
    return Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(language.code)
            .build()
    )
}

val wifiOnly: DownloadConditions = DownloadConditions.Builder().requireWifi().build()
