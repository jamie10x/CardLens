package uz.cardlens.core.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uz.cardlens.core.common.AppResult
import uz.cardlens.core.common.DataError
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.domain.FollowUp
import uz.cardlens.core.domain.Tag
import uz.cardlens.core.supabase.AuthRepository
import uz.cardlens.core.supabase.SupabaseClientRef
import java.time.Instant
import java.util.UUID

data class RemoteSnapshot(
    val ownerId: String,
    val contacts: List<Contact>,
    val followUps: List<FollowUp>,
)

class SupabaseRemoteDataSource(
    private val ref: SupabaseClientRef,
    private val authRepository: AuthRepository,
) {
    private val client: SupabaseClient? = ref.client
    private val isConfigured: Boolean
        get() = (client != null) && (authRepository.currentUser.value?.isDemo != true)

    suspend fun pullSnapshot(): AppResult<RemoteSnapshot, DataError.Network> {
        val supabase = client ?: return AppResult.Error(DataError.Network.UNAUTHORIZED)
        val user = authRepository.currentUser.value ?: return AppResult.Error(DataError.Network.UNAUTHORIZED)
        if (!isConfigured) return AppResult.Error(DataError.Network.UNAUTHORIZED)

        return try {
            val contacts = supabase.from("contacts")
                .select {
                    filter { eq("user_id", user.id) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<RemoteContactDto>()

            val followUps = supabase.from("follow_ups")
                .select {
                    filter { eq("user_id", user.id) }
                    order("due_date", Order.ASCENDING)
                }
                .decodeList<RemoteFollowUpDto>()

            val tags = supabase.from("tags")
                .select {
                    filter { eq("user_id", user.id) }
                }
                .decodeList<RemoteTagDto>()

            val contactTags = supabase.from("contact_tags")
                .select()
                .decodeList<RemoteContactTagDto>()

            AppResult.Success(
                RemoteSnapshot(
                    ownerId = user.id,
                    contacts = contacts.map { contact ->
                        contact.toContact(
                            tags = contactTags
                                .asSequence()
                                .filter { it.contactId == contact.id }
                                .mapNotNull { join -> tags.firstOrNull { it.id == join.tagId }?.toTag() }
                                .toList(),
                        )
                    },
                    followUps = followUps.map { it.toFollowUp() },
                ),
            )
        } catch (_: Exception) {
            AppResult.Error(DataError.Network.UNKNOWN)
        }
    }

    suspend fun pushContact(contact: Contact, followUp: FollowUp?) {
        val supabase = client ?: return
        val user = authRepository.currentUser.value ?: return
        if (!isConfigured) return

        runCatching {
            supabase.from("contacts").upsert(contact.toRemoteDto(user.id))
            val remoteTags = fetchTags(user.id).associateBy { it.name.lowercase() }.toMutableMap()
            contact.tags.forEach { tag ->
                val remoteTag = remoteTags[tag.name.lowercase()] ?: createTag(user.id, tag)
                remoteTags[tag.name.lowercase()] = remoteTag
                supabase.from("contact_tags").upsert(
                    listOf(
                        RemoteContactTagDto(
                            id = stableJoinId(contact.id, remoteTag.id),
                            contactId = contact.id,
                            tagId = remoteTag.id,
                        ),
                    ),
                ) {
                    onConflict = "contact_id,tag_id"
                }
            }
            followUp?.let {
                supabase.from("follow_ups").upsert(it.toRemoteDto(user.id))
            }
        }
    }

    suspend fun deleteContact(contactId: String) {
        val supabase = client ?: return
        val user = authRepository.currentUser.value ?: return
        if (!isConfigured) return

        runCatching {
            supabase.from("contacts").delete {
                filter {
                    eq("id", contactId)
                    eq("user_id", user.id)
                }
            }
        }
    }

    suspend fun completeFollowUp(followUpId: String, contactId: String) {
        val supabase = client ?: return
        val user = authRepository.currentUser.value ?: return
        if (!isConfigured) return

        runCatching {
            val completedAt = Instant.now().toString()
            supabase.from("follow_ups").update(
                {
                    set("completed", value = true)
                    set("completed_at", completedAt)
                },
            ) {
                filter {
                    eq("id", followUpId)
                    eq("user_id", user.id)
                }
            }
            supabase.from("contacts").update(
                {
                    set("status", ContactStatus.Contacted.label)
                    set("updated_at", completedAt)
                },
            ) {
                filter {
                    eq("id", contactId)
                    eq("user_id", user.id)
                }
            }
        }
    }

    private suspend fun fetchTags(userId: String): List<RemoteTagDto> {
        val supabase = client ?: return emptyList()
        return supabase.from("tags")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList()
    }

    private suspend fun createTag(userId: String, tag: Tag): RemoteTagDto {
        val supabase = client ?: error("Supabase client is not configured")
        val dto = RemoteTagDto(
            id = tag.id,
            userId = userId,
            name = tag.name,
            color = "#2563EB",
        )
        return supabase.from("tags")
            .insert(dto) {
                select()
            }
            .decodeSingle()
    }

    private fun stableJoinId(contactId: String, tagId: String): String {
        return UUID.nameUUIDFromBytes("$contactId:$tagId".encodeToByteArray()).toString()
    }
}

@Serializable
data class RemoteContactDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    val company: String = "",
    @SerialName("job_title") val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    val address: String = "",
    val notes: String = "",
    @SerialName("date_met") val dateMet: String? = null,
    @SerialName("location_met") val locationMet: String = "",
    val status: String = ContactStatus.New.label,
    @SerialName("card_image_url") val cardImageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class RemoteTagDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val color: String = "#2563EB",
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toTag(): Tag = Tag(id = id, name = name)
}

@Serializable
data class RemoteContactTagDto(
    val id: String,
    @SerialName("contact_id") val contactId: String,
    @SerialName("tag_id") val tagId: String,
)

@Serializable
data class RemoteFollowUpDto(
    val id: String,
    @SerialName("contact_id") val contactId: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("due_date") val dueDate: String,
    val completed: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

fun Contact.toRemoteDto(userId: String): RemoteContactDto = RemoteContactDto(
    id = id,
    userId = userId,
    fullName = fullName,
    company = company,
    jobTitle = jobTitle,
    email = email,
    phone = phone,
    website = website,
    address = address,
    notes = notes,
    dateMet = dateMet.toIsoInstant(),
    locationMet = locationMet,
    status = status.label,
    cardImageUrl = cardImageUri,
    createdAt = createdAt.toIsoInstant(),
    updatedAt = updatedAt.toIsoInstant(),
)

fun RemoteContactDto.toContact(tags: List<Tag>): Contact = Contact(
    id = id,
    fullName = fullName,
    company = company,
    jobTitle = jobTitle,
    email = email,
    phone = phone,
    website = website,
    address = address,
    notes = notes,
    tags = tags,
    status = status.toContactStatus(),
    dateMet = dateMet?.fromIsoInstant() ?: System.currentTimeMillis(),
    locationMet = locationMet,
    cardImageUri = cardImageUrl,
    createdAt = createdAt?.fromIsoInstant() ?: System.currentTimeMillis(),
    updatedAt = updatedAt?.fromIsoInstant() ?: System.currentTimeMillis(),
)

fun FollowUp.toRemoteDto(userId: String): RemoteFollowUpDto = RemoteFollowUpDto(
    id = id,
    contactId = contactId,
    userId = userId,
    title = title,
    dueDate = dueAt.toIsoInstant(),
    completed = completed,
    completedAt = completedAt?.toIsoInstant(),
    createdAt = createdAt.toIsoInstant(),
)

fun RemoteFollowUpDto.toFollowUp(): FollowUp = FollowUp(
    id = id,
    contactId = contactId,
    title = title,
    dueAt = dueDate.fromIsoInstant(),
    completed = completed,
    completedAt = completedAt?.fromIsoInstant(),
    createdAt = createdAt?.fromIsoInstant() ?: System.currentTimeMillis(),
)

private fun Long.toIsoInstant(): String = Instant.ofEpochMilli(this).toString()

private fun String.fromIsoInstant(): Long = runCatching {
    Instant.parse(this).toEpochMilli()
}.getOrDefault(System.currentTimeMillis())

private fun String.toContactStatus(): ContactStatus {
    return ContactStatus.entries.firstOrNull { (it.label == this) || (it.name == this) } ?: ContactStatus.New
}
