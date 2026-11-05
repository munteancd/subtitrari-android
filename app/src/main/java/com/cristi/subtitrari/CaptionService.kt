package com.cristi.subtitrari

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import android.view.accessibility.AccessibilityEvent
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
        translator = newTranslator().also { client ->
            client.downloadModelIfNeeded(wifiOnly)
                .addOnSuccessListener { Log.i(TAG, "translation model ready") }
                .addOnFailureListener { Log.w(TAG, "translation model not ready", it) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
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

        gemini?.translate(
            text,
            earlier,
            SystemClock.uptimeMillis(),
            onResult = { deliver(request, text, it, "gemini") },
            onFailure = { translateOnDevice(request, text) },
        ) ?: translateOnDevice(request, text)
    }

    private fun translateOnDevice(request: Long, text: String) {
        val client = translator ?: return
        client.translate(text)
            .addOnSuccessListener { deliver(request, text, it, "mlkit") }
            .addOnFailureListener { Log.w(TAG, "translation failed", it) }
    }

    private fun deliver(request: Long, text: String, translated: String, source: String) {
        cache.put(text, translated)
        Log.d(TAG, "[$source] \"$text\" -> \"$translated\"")
        // A slower older result must not overwrite a newer caption.
        if (request == latestRequest && polling) overlay.show(translated, captionBounds)
    }
}
