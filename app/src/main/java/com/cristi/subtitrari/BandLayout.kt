package com.cristi.subtitrari

/** Where the translation band goes. [fromBottom] is the distance from the screen's bottom edge. */
data class BandPosition(val x: Int, val fromBottom: Int, val width: Int, val minHeight: Int)

/**
 * Places the band over the original caption so it hides it. Romanian usually runs longer
 * than English, so the band is at least [minWidthFraction] of the screen, centred on the caption.
 */
fun bandPosition(
    captionLeft: Int,
    captionTop: Int,
    captionRight: Int,
    captionBottom: Int,
    screenWidth: Int,
    screenHeight: Int,
    padding: Int,
    minWidthFraction: Float = 0.7f,
): BandPosition {
    val wanted = maxOf(captionRight - captionLeft + 2 * padding, (screenWidth * minWidthFraction).toInt())
    val width = minOf(wanted, screenWidth)
    val centre = (captionLeft + captionRight) / 2
    val x = (centre - width / 2).coerceIn(0, screenWidth - width)
    return BandPosition(
        x = x,
        fromBottom = (screenHeight - captionBottom - padding).coerceAtLeast(0),
        width = width,
        minHeight = captionBottom - captionTop + 2 * padding,
    )
}
