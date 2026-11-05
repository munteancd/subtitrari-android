package com.cristi.subtitrari

import org.junit.Assert.assertEquals
import org.junit.Test

class BandLayoutTest {
    @Test fun `portrait band takes nearly the full width`() {
        val p = bandPosition(400, 500, 680, 560, 1080, 2400, 10)
        assertEquals(1026, p.width)
        assertEquals(27, p.x)
    }

    @Test fun `landscape band stays narrower and centred on the caption`() {
        val p = bandPosition(1000, 800, 1400, 860, 2400, 1080, 10)
        assertEquals(1680, p.width)
        assertEquals(360, p.x)
    }

    @Test fun `band covers the caption vertically`() {
        val p = bandPosition(144, 499, 937, 615, 1080, 2400, 10)
        assertEquals(1775, p.fromBottom)
        assertEquals(136, p.minHeight)
    }

    @Test fun `wide caption keeps its width plus padding`() {
        val p = bandPosition(100, 800, 2000, 860, 2400, 1080, 10)
        assertEquals(1920, p.width)
    }

    @Test fun `width never exceeds the screen and band stays on it`() {
        val p = bandPosition(0, 0, 1080, 60, 1080, 2400, 20)
        assertEquals(1080, p.width)
        assertEquals(0, p.x)
    }

    @Test fun `off-centre caption is pushed back onto the screen`() {
        val p = bandPosition(900, 0, 1000, 60, 1080, 2400, 10)
        assertEquals(1080 - p.width, p.x)
    }
}
