package uz.cardlens.core.domain

import java.util.UUID
import com.neopulsar.cardlens.R

enum class ContactStatus(val label: String, val displayResId: Int) {
    New("New", R.string.status_new),
    FollowUpNeeded("Follow-up Needed", R.string.status_follow_up_needed),
    Contacted("Contacted", R.string.status_contacted),
    MeetingScheduled("Meeting Scheduled", R.string.status_meeting_scheduled),
    Converted("Converted", R.string.status_converted),
    Archived("Archived", R.string.status_archived)
}

data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Long = 0xFF2563EB
)

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val company: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    val address: String = "",
    val notes: String = "",
    val tags: List<Tag> = emptyList(),
    val status: ContactStatus = ContactStatus.New,
    val dateMet: Long = System.currentTimeMillis(),
    val locationMet: String = "",
    val cardImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ContactDraft(
    val id: String? = null,
    val fullName: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    val address: String = "",
    val notes: String = "",
    val tagsText: String = "",
    val status: ContactStatus = ContactStatus.New,
    val dateMet: Long = System.currentTimeMillis(),
    val locationMet: String = "",
    val cardImageUri: String? = null,
    val followUpTitle: String = "",
    val followUpDueAt: Long? = null
) {
    fun toContact(): Contact {
        val now = System.currentTimeMillis()
        val tags = tagsText.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .map { Tag(name = it) }

        return Contact(
            id = id ?: UUID.randomUUID().toString(),
            fullName = fullName.ifBlank { "Untitled contact" },
            company = company,
            jobTitle = jobTitle,
            email = email,
            phone = phone,
            website = website,
            address = address,
            notes = notes,
            tags = tags,
            status = if (followUpDueAt != null && status == ContactStatus.New) {
                ContactStatus.FollowUpNeeded
            } else {
                status
            },
            dateMet = dateMet,
            locationMet = locationMet,
            cardImageUri = cardImageUri,
            createdAt = now,
            updatedAt = now
        )
    }
}

data class FollowUp(
    val id: String = UUID.randomUUID().toString(),
    val contactId: String,
    val title: String,
    val dueAt: Long,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ContactActivity(
    val id: String = UUID.randomUUID().toString(),
    val contactId: String,
    val type: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)