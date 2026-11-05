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

    @Test fun `prompt protects names`() {
        assertTrue(captionPrompt("Romanian", emptyList(), "x").contains("copied letter by letter"))
    }

    @Test fun `sound labels are removed wherever they are`() {
        assertEquals("otros si te apetece", withoutSoundLabels("[Música] otros si te apetece"))
        assertEquals("reposar unos 3 minutos", withoutSoundLabels("reposar unos 3 minutos [Música]"))
        assertEquals("a b", withoutSoundLabels("a [Music] b"))
    }

    @Test fun `caption with only a sound label is empty`() {
        assertEquals("", withoutSoundLabels("[Music]"))
    }

    @Test fun `laughter in round brackets and ordinary text stay`() {
        assertEquals("(Laughter)", withoutSoundLabels("(Laughter)"))
        assertEquals("the tap is leaking", withoutSoundLabels("the tap is leaking"))
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

    @Test fun `retry delay is read from the error details`() {
        val body = """{"error":{"code":429,"details":[{"@type":"QuotaFailure"},{"retryDelay":"59495s"}]}}"""
        assertEquals(59_495_000L, parseRetryDelayMs(body))
    }

    @Test fun `fractional retry delay is kept`() {
        val body = """{"error":{"details":[{"retryDelay":"4.5s"}]}}"""
        assertEquals(4_500L, parseRetryDelayMs(body))
    }

    @Test fun `missing or unreadable retry delay gives null`() {
        assertNull(parseRetryDelayMs("""{"error":{"code":429}}"""))
        assertNull(parseRetryDelayMs("not json"))
    }

    @Test fun `cooldown lasts for its pause and then ends`() {
        val cooldown = Cooldown()
        assertFalse(cooldown.active(1_000))
        cooldown.trip(1_000, 60_000)
        assertTrue(cooldown.active(60_999))
        assertFalse(cooldown.active(61_000))
    }
}
