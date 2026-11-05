package com.cristi.subtitrari

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptionStabilizerTest {
    private val s = CaptionStabilizer(stableMs = 300, maxWaitMs = 1500)

    @Test fun `whole caption passes once it sits still`() {
        assertNull(s.update("Hello there", 0))
        assertNull(s.update("Hello there", 299))
        assertEquals("Hello there", s.update("Hello there", 300))
    }

    @Test fun `same caption is emitted only once`() {
        s.update("Hello", 0)
        assertEquals("Hello", s.update("Hello", 300))
        assertNull(s.update("Hello", 450))
        assertNull(s.update("Hello", 5000))
    }

    @Test fun `word by word caption is forced out at max wait`() {
        var t = 0L
        var text = "w"
        while (t < 1500) {
            assertNull(s.update(text, t))
            t += 200
            text += " w"
        }
        assertEquals(text, s.update(text, t))
    }

    @Test fun `growth continues after a forced emit`() {
        s.update("a", 0)
        s.update("a b", 200)
        assertEquals("a b c", s.update("a b c", 1500))
        assertNull(s.update("a b c d", 1600))
        assertEquals("a b c d", s.update("a b c d", 1900))
    }

    @Test fun `blank resets so the same caption can return`() {
        s.update("Hi", 0)
        assertEquals("Hi", s.update("Hi", 300))
        assertNull(s.update("", 400))
        assertNull(s.update("Hi", 500))
        assertEquals("Hi", s.update("Hi", 800))
    }
}
