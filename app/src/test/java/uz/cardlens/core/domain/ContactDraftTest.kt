package uz.cardlens.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactDraftTest {
    @Test
    fun `draft becomes follow up needed contact when reminder is selected`() {
        val contact = ContactDraft(
            fullName = "Aziz Karimov",
            company = "Local Eats",
            tagsText = "Lead, Website, Lead",
            followUpDueAt = 1_725_000_000_000
        ).toContact()

        assertEquals("Aziz Karimov", contact.fullName)
        assertEquals("Local Eats", contact.company)
        assertEquals(ContactStatus.FollowUpNeeded, contact.status)
        assertEquals(listOf("Lead", "Website"), contact.tags.map { it.name })
    }
}
