package com.cristi.subtitrari

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import android.view.accessibility.AccessibilityEvent
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator

private const val TAG = "Subtitrari"
private const val YOUTUBE = "com.google.android.youtube"
private const val CAPTION_ID = "$YOUTUBE:id/subtitle_window_identifier"
private const val POLL_MS = 150L
private const val HIDE_AFTER_MS = 600L
private const val CONTEXT_CAPTIONS = 2
private const val MAX_NO_ROOT_TICKS = 7

/**
 * Polls while YouTube is in front, because caption changes do not reliably raise an event.
 * A window change that leaves YouTube in front starts polling; polling stops itself once YouTube
 * leaves the front, so nothing runs in the background the rest of the time.
 */
class CaptionService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val stabilizer = CaptionStabilizer()
    private val cache = LruCache<String, String>(200)
    private lateinit var overlay: CaptionOverlay
    private var translator: Translator? = null
    private var gemini: GeminiClient? = null
    private val recentCaptions = ArrayDeque<String>()
    private var polling = false
    private var noRootTicks = 0
    private var lastCaptionAt = 0L
    private var latestRequest = 0L
    private val captionBounds = Rect()
    private val languageId by lazy { LanguageIdentification.getClient() }
    private lateinit var settings: CaptionSettings
    private var language = LANGUAGES[0]

    /** Kept in a field: SharedPreferences holds listeners weakly. */
    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        applyStyle()
        if (key == CaptionSettings.KEY_LANGUAGE) useLanguage(settings.language)
    }

    private val poll = object : Runnable {
        override fun run() {
            if (!polling) return
            if (tick()) handler.postDelayed(this, POLL_MS) else stopPolling()
        }
    }

    override fun onServiceConnected() {
        releaseAll()
        overlay = CaptionOverlay(this)
        gemini = GeminiClient(BuildConfig.GEMINI_API_KEY)
        settings = CaptionSettings(this)
        settings.prefs.registerOnSharedPreferenceChangeListener(settingsListener)
        applyStyle()
        useLanguage(settings.language)
        // YouTube may already be in front, and then no window change will announce it.
        startPollingIfYouTube()
    }

    private fun applyStyle() = overlay.setStyle(settings.color.argb, settings.size.sp)

    private fun useLanguage(chosen: CaptionLanguage) {
        language = chosen
        cache.evictAll()
        recentCaptions.clear()
        latestRequest++
        translator?.close()
        translator = newTranslator(chosen)
        translator?.downloadModelIfNeeded(wifiOnly)
            ?.addOnSuccessListener { Log.i(TAG, "translation model ready: ${chosen.code}") }
            ?.addOnFailureListener { Log.w(TAG, "translation model not ready", it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = startPollingIfYouTube()

    private fun startPollingIfYouTube() {
        if (!polling && rootInActiveWindow?.packageName?.toString() == YOUTUBE) {
            polling = true
            noRootTicks = 0
            handler.post(poll)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        releaseAll()
        super.onDestroy()
    }

    /** The system can reconnect a running service, so this also runs before each new setup. */
    private fun releaseAll() {
        stopPolling()
        if (::settings.isInitialized) settings.prefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
        translator?.close()
        translator = null
        gemini?.close()
        gemini = null
    }

    private fun stopPolling() {
        polling = false
        handler.removeCallbacks(poll)
        if (::overlay.isInitialized) overlay.hide()
    }

    /** Returns false once YouTube is no longer in front. */
    private fun tick(): Boolean {
        val root = rootInActiveWindow
        if (root == null) {
            // Brief gaps happen during window changes; a long one means YouTube is gone.
            return ++noRootTicks < MAX_NO_ROOT_TICKS
        }
        noRootTicks = 0
        if (root.packageName?.toString() != YOUTUBE) return false

        val now = SystemClock.uptimeMillis()
        val lines = root.findAccessibilityNodeInfosByViewId(CAPTION_ID)
            .filter { !it.text.isNullOrBlank() && it.isVisibleToUser }

        if (lines.isEmpty()) {
            stabilizer.update("", now)
            if (now - lastCaptionAt > HIDE_AFTER_MS) overlay.hide()
            return true
        }

        lastCaptionAt = now
        val bounds = Rect()
        captionBounds.setEmpty()
        lines.forEach {
            it.getBoundsInScreen(bounds)
            captionBounds.union(bounds)
        }
        overlay.follow(captionBounds)
        val text = lines.joinToString(" ") { it.text.toString().trim() }
        stabilizer.update(text, now)?.let(::translate)
        return true
    }

    private fun translate(text: String) {
        val request = ++latestRequest
        cache.get(text)?.let {
            overlay.show(it, captionBounds)
            return
        }
        val earlier = recentCaptions.toList()
        recentCaptions.addLast(text)
        if (recentCaptions.size > CONTEXT_CAPTIONS) recentCaptions.removeFirst()

        val target = language
        gemini?.translate(
            target.englishName,
            text,
            earlier,
            SystemClock.uptimeMillis(),
            onResult = { deliver(request, target, text, it, "gemini") },
            onFailure = { translateOnDevice(request, target, text) },
        ) ?: translateOnDevice(request, target, text)
    }

    /** ML Kit here only knows English. For anything else the original caption is left showing. */
    private fun translateOnDevice(request: Long, target: CaptionLanguage, text: String) {
        val client = translator
        if (client == null || target != language) return showOriginal(request)
        languageId.identifyLanguage(text)
            .addOnSuccessListener { source ->
                if (source != TranslateLanguage.ENGLISH && source != "und") {
                    showOriginal(request)
                    return@addOnSuccessListener
                }
                client.translate(text)
                    .addOnSuccessListener { deliver(request, target, text, it, "mlkit") }
                    .addOnFailureListener { Log.w(TAG, "translation failed", it) }
            }
            .addOnFailureListener { Log.w(TAG, "language check failed", it) }
    }

    private fun showOriginal(request: Long) {
        if (request == latestRequest) overlay.hide()
    }

    private fun deliver(request: Long, target: CaptionLanguage, text: String, translated: String, source: String) {
        // The language may have changed while this was in flight.
        if (target != language) return
        cache.put(text, translated)
        Log.d(TAG, "[$source] \"$text\" -> \"$translated\"")
        // A slower older result must not overwrite a newer caption.
        if (request == latestRequest && polling) overlay.show(translated, captionBounds)
    }
}
