package com.bilal.humanactivityrecognition.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {
    @Insert
    suspend fun insert(entity: PredictionEntity): Long

    @Update
    suspend fun update(entity: PredictionEntity)

    @Query("SELECT * FROM predictions ORDER BY endTime DESC LIMIT 1")
    suspend fun latest(): PredictionEntity?

    @Query("SELECT * FROM predictions ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE endTime > :fromMs AND startTime < :toMs ORDER BY startTime ASC")
    fun observeBetween(fromMs: Long, toMs: Long): Flow<List<PredictionEntity>>

    @Query("SELECT COUNT(*) FROM predictions")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM predictions")
    suspend fun clear()
}
