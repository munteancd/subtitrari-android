package com.cristi.subtitrari

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiClientTest {
    @Test fun `prompt carries the earlier captions and the current one`() {
        val prompt = captionPrompt("Hungarian", listOf("first", "second"), "third")
        assertTrue(prompt.contains("EARLIER: first second"))
        assertTrue(prompt.contains("CURRENT: third"))
        assertTrue(prompt.contains("natural Hungarian"))
    }

    @Test fun `reply text is extracted and trimmed`() {
        val json = """{"candidates":[{"content":{"parts":[{"text":" robinetul \n"}]}}]}"""
        assertEquals("robinetul", parseReply(json))
    }

    @Test fun `blocked answer without candidates gives null`() {
        assertNull(parseReply("""{"promptFeedback":{"blockReason":"OTHER"}}"""))
    }

    @Test fun `empty text gives null`() {
        assertNull(parseReply("""{"candidates":[{"content":{"parts":[{"text":"  "}]}}]}"""))
    }

    @Test fun `candidate without content gives null`() {
        assertNull(parseReply("""{"candidates":[{"finishReason":"SAFETY"}]}"""))
    }

    @Test fun `cooldown lasts for its pause and then ends`() {
        val cooldown = Cooldown(60_000)
        assertFalse(cooldown.active(1_000))
        cooldown.trip(1_000)
        assertTrue(cooldown.active(60_999))
        assertFalse(cooldown.active(61_000))
    }
}
