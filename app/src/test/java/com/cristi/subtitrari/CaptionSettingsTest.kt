package com.cristi.subtitrari

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptionSettingsTest {
    @Test fun `language defaults to Romanian when nothing or something unknown is saved`() {
        assertEquals("ro", languageFor(null).code)
        assertEquals("ro", languageFor("xx").code)
    }

    @Test fun `saved language is found`() {
        assertEquals("Hungarian", languageFor("hu").englishName)
    }

    @Test fun `language codes are unique`() {
        assertEquals(LANGUAGES.size, LANGUAGES.map { it.code }.toSet().size)
    }

    @Test fun `unknown colour falls back to lime`() {
        assertEquals("Lime", colorFor(0x12345678).name)
    }

    @Test fun `unknown size falls back to medium`() {
        assertEquals("Mediu", sizeFor(99f).name)
    }
}
