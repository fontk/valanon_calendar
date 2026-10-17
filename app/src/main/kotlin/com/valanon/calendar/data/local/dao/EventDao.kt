package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valanon.calendar.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    /** Emits again every time the table changes. */
    @Query("SELECT * FROM events ORDER BY start_at ASC, id ASC")
    fun observeAll(): Flow<List<EventEntity>>

    /** Events starting in [from, to). Useful for a day/week/month view. */
    @Query(
        """
        SELECT * FROM events
        WHERE start_at >= :from AND start_at < :to
        ORDER BY start_at ASC, id ASC
        """,
    )
    fun observeBetween(from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    /** Returns the generated row id. */
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
