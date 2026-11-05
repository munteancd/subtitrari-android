package com.cristi.subtitrari

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/** The on-device fallback for when Gemini cannot answer. It understands English only. */
fun newTranslator(): Translator = Translation.getClient(
    TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.ROMANIAN)
        .build()
)

val wifiOnly: DownloadConditions = DownloadConditions.Builder().requireWifi().build()
