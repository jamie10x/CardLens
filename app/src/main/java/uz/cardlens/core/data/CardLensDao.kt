package uz.cardlens.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CardLensDao {
    @Query("SELECT * FROM contacts WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeContacts(ownerId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id AND ownerId = :ownerId LIMIT 1")
    suspend fun getContact(id: String, ownerId: String): ContactEntity?

    @Query("SELECT * FROM follow_ups WHERE ownerId = :ownerId ORDER BY dueAt ASC")
    fun observeFollowUps(ownerId: String): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM contact_activities WHERE contactId = :contactId AND ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeActivities(contactId: String, ownerId: String): Flow<List<ContactActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowUp(followUp: FollowUpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowUps(followUps: List<FollowUpEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ContactActivityEntity)

    @Query("UPDATE follow_ups SET completed = 1, completedAt = :completedAt WHERE id = :id AND ownerId = :ownerId")
    suspend fun completeFollowUp(id: String, ownerId: String, completedAt: Long)

    @Query("UPDATE contacts SET status = :status, updatedAt = :updatedAt WHERE id = :contactId AND ownerId = :ownerId")
    suspend fun updateContactStatus(contactId: String, ownerId: String, status: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM contacts WHERE ownerId = :ownerId")
    suspend fun contactCount(ownerId: String): Int

    @Query("SELECT * FROM contacts WHERE ownerId = :ownerId")
    suspend fun getContacts(ownerId: String): List<ContactEntity>

    @Query("SELECT * FROM follow_ups WHERE ownerId = :ownerId")
    suspend fun getFollowUps(ownerId: String): List<FollowUpEntity>

    @Query("SELECT * FROM follow_ups WHERE id = :id AND ownerId = :ownerId LIMIT 1")
    suspend fun getFollowUp(id: String, ownerId: String): FollowUpEntity?

    @Transaction
    suspend fun cacheRemoteSnapshot(
        ownerId: String,
        contacts: List<ContactEntity>,
        followUps: List<FollowUpEntity>,
    ) {
        upsertContacts(contacts.map { it.copy(ownerId = ownerId) })
        upsertFollowUps(followUps.map { it.copy(ownerId = ownerId) })
    }
}
