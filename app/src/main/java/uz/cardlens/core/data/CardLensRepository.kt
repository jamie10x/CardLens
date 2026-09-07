package uz.cardlens.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.cardlens.core.common.DataError
import uz.cardlens.core.common.EmptyResult
import uz.cardlens.core.common.AppResult
import uz.cardlens.core.domain.Contact
import uz.cardlens.core.domain.ContactStatus
import uz.cardlens.core.domain.FollowUp

interface CardLensRepository {
    fun observeContacts(): Flow<List<Contact>>
    fun observeFollowUps(): Flow<List<FollowUp>>
    suspend fun saveContact(contact: Contact, followUp: FollowUp?): EmptyResult<DataError.Local>
    suspend fun deleteContact(contactId: String): EmptyResult<DataError.Local>
    suspend fun completeFollowUp(followUpId: String, contactId: String): EmptyResult<DataError.Local>
}

class RoomCardLensRepository(
    private val dao: CardLensDao,
) : CardLensRepository {

    override fun observeContacts(): Flow<List<Contact>> =
        dao.observeContacts(LOCAL_OWNER_ID).mapContacts()

    override fun observeFollowUps(): Flow<List<FollowUp>> =
        dao.observeFollowUps(LOCAL_OWNER_ID).mapFollowUps()

    override suspend fun saveContact(contact: Contact, followUp: FollowUp?): EmptyResult<DataError.Local> {
        return try {
            dao.upsertContact(contact.toEntity(LOCAL_OWNER_ID))
            if (followUp != null) {
                dao.upsertFollowUp(followUp.toEntity(LOCAL_OWNER_ID))
            }
            AppResult.Success(Unit)
        } catch (_: Exception) {
            AppResult.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun deleteContact(contactId: String): EmptyResult<DataError.Local> {
        return try {
            dao.deleteActivitiesByContactId(contactId, LOCAL_OWNER_ID)
            dao.deleteFollowUpsByContactId(contactId, LOCAL_OWNER_ID)
            dao.deleteContact(contactId, LOCAL_OWNER_ID)
            AppResult.Success(Unit)
        } catch (_: Exception) {
            AppResult.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun completeFollowUp(followUpId: String, contactId: String): EmptyResult<DataError.Local> {
        return try {
            val now = System.currentTimeMillis()
            dao.completeFollowUp(followUpId, LOCAL_OWNER_ID, now)
            dao.updateContactStatus(contactId, LOCAL_OWNER_ID, ContactStatus.Contacted.name, now)
            AppResult.Success(Unit)
        } catch (_: Exception) {
            AppResult.Error(DataError.Local.UNKNOWN)
        }
    }

    private fun Flow<List<ContactEntity>>.mapContacts(): Flow<List<Contact>> =
        map { list -> list.map { it.toContact() } }

    private fun Flow<List<FollowUpEntity>>.mapFollowUps(): Flow<List<FollowUp>> =
        map { list -> list.map { it.toFollowUp() } }

    companion object {
        const val LOCAL_OWNER_ID = "local"
    }
}
