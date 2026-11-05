package com.cristi.subtitrari

/** Where the translation band goes. [fromBottom] is the distance from the screen's bottom edge. */
data class BandPosition(val x: Int, val fromBottom: Int, val width: Int, val minHeight: Int)

/**
 * Places the band over the original caption so it hides it. Translations often run longer than
 * the original. In portrait the player is short, so the band takes nearly the full width to use
 * fewer lines and hide less of the picture. In landscape there is room, so it stays narrower.
 */
fun bandPosition(
    captionLeft: Int,
    captionTop: Int,
    captionRight: Int,
    captionBottom: Int,
    screenWidth: Int,
    screenHeight: Int,
    padding: Int,
): BandPosition {
    val minWidthFraction = if (screenHeight > screenWidth) 0.95f else 0.7f
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
