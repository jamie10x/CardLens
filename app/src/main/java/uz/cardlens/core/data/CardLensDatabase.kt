package uz.cardlens.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContactEntity::class,
        FollowUpEntity::class,
        ContactActivityEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CardLensDatabase : RoomDatabase() {
    abstract fun dao(): CardLensDao
}
