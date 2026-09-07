package uz.cardlens.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardLensDao {
    @Query("SELECT * FROM contacts WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeContacts(ownerId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM follow_ups WHERE ownerId = :ownerId ORDER BY dueAt ASC")
    fun observeFollowUps(ownerId: String): Flow<List<FollowUpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowUp(followUp: FollowUpEntity)

    @Query("UPDATE follow_ups SET completed = 1, completedAt = :completedAt WHERE id = :id AND ownerId = :ownerId")
    suspend fun completeFollowUp(id: String, ownerId: String, completedAt: Long)

    @Query("UPDATE contacts SET status = :status, updatedAt = :updatedAt WHERE id = :contactId AND ownerId = :ownerId")
    suspend fun updateContactStatus(contactId: String, ownerId: String, status: String, updatedAt: Long)

    @Query("DELETE FROM contacts WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteContact(id: String, ownerId: String)

    @Query("DELETE FROM follow_ups WHERE contactId = :contactId AND ownerId = :ownerId")
    suspend fun deleteFollowUpsByContactId(contactId: String, ownerId: String)

    @Query("DELETE FROM contact_activities WHERE contactId = :contactId AND ownerId = :ownerId")
    suspend fun deleteActivitiesByContactId(contactId: String, ownerId: String)
}