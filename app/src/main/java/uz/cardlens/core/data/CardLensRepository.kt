package uz.cardlens.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import uz.cardlens.core.common.DataError
import uz.cardlens.core.common.EmptyResult
import uz.cardlens.core.common.AppResult
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.domain.ContactActivity
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.domain.FollowUp
import java.util.UUID

interface CardLensRepository {
    fun setActiveOwner(ownerId: String)
    fun observeContacts(): Flow<List<Contact>>
    fun observeFollowUps(): Flow<List<FollowUp>>
    fun observeActivities(contactId: String): Flow<List<ContactActivity>>
    suspend fun saveContact(contact: Contact, followUp: FollowUp?): EmptyResult<DataError.Local>
    suspend fun completeFollowUp(followUpId: String, contactId: String): EmptyResult<DataError.Local>
    suspend fun syncNow(): EmptyResult<DataError.Network>
    suspend fun seedDemoDataIfNeeded()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoomCardLensRepository(
    private val dao: CardLensDao,
) : CardLensRepository {
    private val activeOwnerId = MutableStateFlow(DEMO_OWNER_ID)

    override fun setActiveOwner(ownerId: String) {
        activeOwnerId.value = ownerId
    }

    override fun observeContacts(): Flow<List<Contact>> = activeOwnerId.flatMapLatest { ownerId ->
        dao.observeContacts(ownerId).map { contacts -> contacts.map { it.toContact() } }
    }

    override fun observeFollowUps(): Flow<List<FollowUp>> = activeOwnerId.flatMapLatest { ownerId ->
        dao.observeFollowUps(ownerId).map { followUps -> followUps.map { it.toFollowUp() } }
    }

    override fun observeActivities(contactId: String): Flow<List<ContactActivity>> {
        return activeOwnerId.flatMapLatest { ownerId ->
            dao.observeActivities(contactId, ownerId).map { activities -> activities.map { it.toActivity() } }
        }
    }

    override suspend fun saveContact(contact: Contact, followUp: FollowUp?): EmptyResult<DataError.Local> {
        return try {
            val ownerId = activeOwnerId.value
            dao.upsertContact(contact.toEntity(ownerId))
            dao.insertActivity(
                ContactActivity(
                    contactId = contact.id,
                    type = "contact_saved",
                    description = "Contact saved from scanned card"
                ).toEntity(ownerId)
            )
            if (followUp != null) {
                dao.upsertFollowUp(followUp.toEntity(ownerId))
                dao.insertActivity(
                    ContactActivity(
                        contactId = contact.id,
                        type = "reminder_created",
                        description = followUp.title
                    ).toEntity(ownerId)
                )
            }
            AppResult.Success(Unit)
        } catch (_: Exception) {
            AppResult.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun completeFollowUp(followUpId: String, contactId: String): EmptyResult<DataError.Local> {
        return try {
            val ownerId = activeOwnerId.value
            val now = System.currentTimeMillis()
            dao.completeFollowUp(followUpId, ownerId, now)
            dao.updateContactStatus(contactId, ownerId, ContactStatus.Contacted.name, now)
            dao.insertActivity(
                ContactActivity(
                    contactId = contactId,
                    type = "reminder_completed",
                    description = "Follow-up marked done"
                ).toEntity(ownerId)
            )
            AppResult.Success(Unit)
        } catch (_: Exception) {
            AppResult.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun syncNow(): EmptyResult<DataError.Network> {
        return AppResult.Success(Unit)
    }

    suspend fun currentLocalSnapshot(): Pair<List<Contact>, List<FollowUp>> {
        val ownerId = activeOwnerId.value
        return dao.getContacts(ownerId).map { it.toContact() } to
            dao.getFollowUps(ownerId).map { it.toFollowUp() }
    }

    override suspend fun seedDemoDataIfNeeded() {
        setActiveOwner(DEMO_OWNER_ID)
        if (dao.contactCount(DEMO_OWNER_ID) > 0) return
        val now = System.currentTimeMillis()
        val contact = Contact(
            id = UUID.randomUUID().toString(),
            fullName = "Aziz Karimov",
            jobTitle = "Founder",
            company = "Local Eats",
            email = "aziz@localeats.com",
            phone = "+998 90 123 45 67",
            website = "localeats.com",
            notes = "Met at startup meetup. Interested in website redesign and asked for pricing next week.",
            tags = listOf(uz.cardlens.core.domain.Tag(name = "Lead"), uz.cardlens.core.domain.Tag(name = "Website")),
            status = ContactStatus.FollowUpNeeded,
            dateMet = now,
            locationMet = "Tashkent Startup Meetup",
            createdAt = now,
            updatedAt = now
        )
        saveContact(
            contact = contact,
            followUp = FollowUp(
                contactId = contact.id,
                title = "Follow up about restaurant website proposal",
                dueAt = now + (24 * 60 * 60 * 1000),
            ),
        )
    }

    companion object {
        const val DEMO_OWNER_ID = "demo"
    }
}

class SyncingCardLensRepository(
    private val localRepository: RoomCardLensRepository,
    private val remoteDataSource: SupabaseRemoteDataSource
) : CardLensRepository {

    override fun setActiveOwner(ownerId: String) {
        localRepository.setActiveOwner(ownerId)
    }

    override fun observeContacts(): Flow<List<Contact>> = localRepository.observeContacts()

    override fun observeFollowUps(): Flow<List<FollowUp>> = localRepository.observeFollowUps()

    override fun observeActivities(contactId: String): Flow<List<ContactActivity>> {
        return localRepository.observeActivities(contactId)
    }

    override suspend fun saveContact(contact: Contact, followUp: FollowUp?): EmptyResult<DataError.Local> {
        val localResult = localRepository.saveContact(contact, followUp)
        if (localResult is AppResult.Success) {
            remoteDataSource.pushContact(contact, followUp)
        }
        return localResult
    }

    override suspend fun completeFollowUp(followUpId: String, contactId: String): EmptyResult<DataError.Local> {
        val result = localRepository.completeFollowUp(followUpId, contactId)
        if (result is AppResult.Success) {
            remoteDataSource.completeFollowUp(followUpId, contactId)
        }
        return result
    }

    override suspend fun syncNow(): EmptyResult<DataError.Network> {
        val (localContacts, localFollowUps) = localRepository.currentLocalSnapshot()
        localContacts.forEach { contact ->
            val followUps = localFollowUps.filter { it.contactId == contact.id }
            if (followUps.isEmpty()) {
                remoteDataSource.pushContact(contact, null)
            } else {
                followUps.forEach { followUp ->
                    remoteDataSource.pushContact(contact, followUp)
                }
            }
        }
        val result = remoteDataSource.pullSnapshot()
        if (result is AppResult.Success) {
            localRepository.cacheRemoteSnapshot(result.data)
        }
        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }

    override suspend fun seedDemoDataIfNeeded() {
        localRepository.seedDemoDataIfNeeded()
    }
}

suspend fun RoomCardLensRepository.cacheRemoteSnapshot(snapshot: RemoteSnapshot) {
    setActiveOwner(snapshot.ownerId)
    snapshot.contacts.forEach { contact ->
        saveContact(contact, null)
    }
    snapshot.followUps.forEach { followUp ->
        saveContact(
            contact = snapshot.contacts.firstOrNull { it.id == followUp.contactId } ?: return@forEach,
            followUp = followUp
        )
    }
}
