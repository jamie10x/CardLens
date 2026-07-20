package uz.cardlens.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.domain.ContactActivity
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.domain.FollowUp
import uz.cardlens.core.domain.Tag

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val fullName: String,
    val company: String,
    val jobTitle: String,
    val email: String,
    val phone: String,
    val website: String,
    val address: String,
    val notes: String,
    val tagsCsv: String,
    val status: String,
    val dateMet: Long,
    val locationMet: String,
    val cardImageUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "follow_ups")
data class FollowUpEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val contactId: String,
    val title: String,
    val dueAt: Long,
    val completed: Boolean,
    val completedAt: Long?,
    val createdAt: Long
)

@Entity(tableName = "contact_activities")
data class ContactActivityEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val contactId: String,
    val type: String,
    val description: String,
    val createdAt: Long
)

fun Contact.toEntity(ownerId: String): ContactEntity = ContactEntity(
    id = id,
    ownerId = ownerId,
    fullName = fullName,
    company = company,
    jobTitle = jobTitle,
    email = email,
    phone = phone,
    website = website,
    address = address,
    notes = notes,
    tagsCsv = tags.joinToString("|") { it.name },
    status = status.name,
    dateMet = dateMet,
    locationMet = locationMet,
    cardImageUri = cardImageUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ContactEntity.toContact(): Contact = Contact(
    id = id,
    fullName = fullName,
    company = company,
    jobTitle = jobTitle,
    email = email,
    phone = phone,
    website = website,
    address = address,
    notes = notes,
    tags = tagsCsv.split("|")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { Tag(name = it) },
    status = runCatching { ContactStatus.valueOf(status) }.getOrDefault(ContactStatus.New),
    dateMet = dateMet,
    locationMet = locationMet,
    cardImageUri = cardImageUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun FollowUp.toEntity(ownerId: String): FollowUpEntity = FollowUpEntity(
    id = id,
    ownerId = ownerId,
    contactId = contactId,
    title = title,
    dueAt = dueAt,
    completed = completed,
    completedAt = completedAt,
    createdAt = createdAt
)

fun FollowUpEntity.toFollowUp(): FollowUp = FollowUp(
    id = id,
    contactId = contactId,
    title = title,
    dueAt = dueAt,
    completed = completed,
    completedAt = completedAt,
    createdAt = createdAt
)

fun ContactActivity.toEntity(ownerId: String): ContactActivityEntity = ContactActivityEntity(
    id = id,
    ownerId = ownerId,
    contactId = contactId,
    type = type,
    description = description,
    createdAt = createdAt
)

fun ContactActivityEntity.toActivity(): ContactActivity = ContactActivity(
    id = id,
    contactId = contactId,
    type = type,
    description = description,
    createdAt = createdAt
)
