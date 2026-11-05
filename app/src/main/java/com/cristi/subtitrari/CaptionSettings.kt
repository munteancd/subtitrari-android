package com.cristi.subtitrari

import android.content.Context

/** [code] is both the Gemini-facing language tag and the ML Kit language constant. */
data class CaptionLanguage(val code: String, val label: String, val englishName: String)

data class ColorChoice(val name: String, val argb: Int)

data class SizeChoice(val name: String, val sp: Float)

val LANGUAGES = listOf(
    CaptionLanguage("ro", "Română", "Romanian"),
    CaptionLanguage("en", "English", "English"),
    CaptionLanguage("hu", "Magyar", "Hungarian"),
    CaptionLanguage("de", "Deutsch", "German"),
    CaptionLanguage("it", "Italiano", "Italian"),
    CaptionLanguage("fr", "Français", "French"),
    CaptionLanguage("es", "Español", "Spanish"),
    CaptionLanguage("pt", "Português", "Portuguese"),
    CaptionLanguage("nl", "Nederlands", "Dutch"),
    CaptionLanguage("sv", "Svenska", "Swedish"),
    CaptionLanguage("pl", "Polski", "Polish"),
    CaptionLanguage("cs", "Čeština", "Czech"),
    CaptionLanguage("sk", "Slovenčina", "Slovak"),
    CaptionLanguage("hr", "Hrvatski", "Croatian"),
    CaptionLanguage("bg", "Български", "Bulgarian"),
    CaptionLanguage("el", "Ελληνικά", "Greek"),
    CaptionLanguage("tr", "Türkçe", "Turkish"),
    CaptionLanguage("ru", "Русский", "Russian"),
    CaptionLanguage("uk", "Українська", "Ukrainian"),
    CaptionLanguage("ar", "العربية", "Arabic"),
    CaptionLanguage("zh", "中文", "Chinese"),
    CaptionLanguage("ja", "日本語", "Japanese"),
)

val COLORS = listOf(
    ColorChoice("Lime", 0xFFB6FF00.toInt()),
    ColorChoice("Alb", 0xFFFFFFFF.toInt()),
    ColorChoice("Galben", 0xFFFFEB3B.toInt()),
    ColorChoice("Turcoaz", 0xFF40E0FF.toInt()),
    ColorChoice("Portocaliu", 0xFFFF9E40.toInt()),
    ColorChoice("Roz", 0xFFFF80C0.toInt()),
)

val SIZES = listOf(
    SizeChoice("Mic", 14f),
    SizeChoice("Mediu", 17f),
    SizeChoice("Mare", 21f),
)

fun languageFor(code: String?): CaptionLanguage = LANGUAGES.find { it.code == code } ?: LANGUAGES[0]

fun colorFor(argb: Int): ColorChoice = COLORS.find { it.argb == argb } ?: COLORS[0]

fun sizeFor(sp: Float): SizeChoice = SIZES.find { it.sp == sp } ?: SIZES[1]

/** The choices made on the setup screen; the service reacts to them as soon as they change. */
class CaptionSettings(context: Context) {
    val prefs = context.getSharedPreferences("caption_settings", Context.MODE_PRIVATE)

    var language: CaptionLanguage
        get() = languageFor(prefs.getString(KEY_LANGUAGE, null))
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.code).apply()

    var color: ColorChoice
        get() = colorFor(prefs.getInt(KEY_COLOR, COLORS[0].argb))
        set(value) = prefs.edit().putInt(KEY_COLOR, value.argb).apply()

    var size: SizeChoice
        get() = sizeFor(prefs.getFloat(KEY_SIZE, SIZES[1].sp))
        set(value) = prefs.edit().putFloat(KEY_SIZE, value.sp).apply()

    companion object {
        const val KEY_LANGUAGE = "language"
        private const val KEY_COLOR = "color"
        private const val KEY_SIZE = "size"
    }
}
