package com.cristi.subtitrari

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/** The band drawn over YouTube. It never takes touches, so the player works normally beneath it. */
class CaptionOverlay(service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val padding = (4 * service.resources.displayMetrics.density).toInt()
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    private val band = TextView(service).apply {
        // Fully opaque: the original caption showed through a translucent band.
        setBackgroundColor(0xFF000000.toInt())
        gravity = Gravity.CENTER
        setPadding(padding * 2, padding, padding * 2, padding)
    }
    private var attached = false

    fun setStyle(color: Int, sizeSp: Float) {
        band.setTextColor(color)
        band.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun show(text: String, caption: Rect) {
        band.text = text
        place(caption)
    }

    /** Follows the caption when the player moves it. Does nothing until a translation is showing. */
    fun follow(caption: Rect) {
        if (attached) place(caption)
    }

    fun hide() {
        if (attached) {
            windowManager.removeView(band)
            attached = false
        }
    }

    @Suppress("DEPRECATION")
    private fun place(caption: Rect) {
        val screen = Point().also { windowManager.defaultDisplay.getRealSize(it) }
        val position = bandPosition(
            caption.left, caption.top, caption.right, caption.bottom,
            screen.x, screen.y, padding,
        )
        band.minHeight = position.minHeight
        params.x = position.x
        params.y = position.fromBottom
        params.width = position.width
        if (attached) {
            windowManager.updateViewLayout(band, params)
        } else {
            windowManager.addView(band, params)
            attached = true
        }
    }
}
