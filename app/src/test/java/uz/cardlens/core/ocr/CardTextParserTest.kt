package uz.cardlens.core.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class CardTextParserTest {
    @Test
    fun `parse extracts common business card fields`() {
        val text = """
            Aziz Karimov
            Founder
            Local Eats
            aziz@localeats.com
            +998 90 123 45 67
            localeats.com
            Tashkent, Uzbekistan
        """.trimIndent()

        val draft = CardTextParser.parse(text)

        assertEquals("Aziz Karimov", draft.fullName)
        assertEquals("Founder", draft.jobTitle)
        assertEquals("Local Eats", draft.company)
        assertEquals("aziz@localeats.com", draft.email)
        assertEquals("+998 90 123 45 67", draft.phone)
        assertEquals("localeats.com", draft.website)
        assertEquals("Tashkent, Uzbekistan", draft.address)
    }
}
