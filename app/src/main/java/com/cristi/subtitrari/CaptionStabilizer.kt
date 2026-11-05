package com.cristi.subtitrari

/**
 * Decides when a caption is worth translating.
 *
 * Authored captions appear whole and then sit still, so they pass after [stableMs].
 * Auto-generated captions grow word by word and may never sit still, so [maxWaitMs]
 * forces out whatever has accumulated. Without this the band would flicker on each word.
 */
class CaptionStabilizer(
    private val stableMs: Long = 300,
    private val maxWaitMs: Long = 1500,
) {
    private var pending: String? = null
    private var lastChangeAt = 0L
    private var waitingSince: Long? = null
    private var lastEmitted: String? = null

    /** Feed the current on-screen text; returns text to translate now, or null. */
    fun update(text: String, now: Long): String? {
        if (text.isBlank()) {
            pending = null
            waitingSince = null
            lastEmitted = null
            return null
        }
        if (text != pending) {
            pending = text
            lastChangeAt = now
            if (waitingSince == null) waitingSince = now
        }
        if (text == lastEmitted) {
            waitingSince = null
            return null
        }
        val since = waitingSince ?: now
        if (now - lastChangeAt >= stableMs || now - since >= maxWaitMs) {
            lastEmitted = text
            waitingSince = null
            return text
        }
        return null
    }
}
