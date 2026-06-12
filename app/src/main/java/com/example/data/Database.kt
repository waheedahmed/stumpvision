package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionById(id: Int): Flow<SessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)
}

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries WHERE sessionId = :sessionId ORDER BY deliveryNum ASC")
    fun getDeliveriesForSession(sessionId: Int): Flow<List<DeliveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity): Long

    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)

    @Delete
    suspend fun deleteDelivery(delivery: DeliveryEntity)
}

@Database(entities = [SessionEntity::class, DeliveryEntity::class], version = 1, exportSchema = false)
abstract class StumpDatabase : RoomDatabase() {
    abstract val sessionDao: SessionDao
    abstract val deliveryDao: DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: StumpDatabase? = null

        fun getDatabase(context: android.content.Context): StumpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StumpDatabase::class.java,
                    "stumpvision_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
