package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A calendar event stored in the `events` table.
 *
 * Instants are stored as epoch milliseconds (UTC) so they sort and range-query
 * cheaply in SQLite. Convert with the helpers in `util/DateTimeExt.kt`.
 */
@Entity(
    tableName = "events",
    indices = [Index(value = ["start_at"])],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val description: String = "",

    /** Start instant, epoch millis. */
    @ColumnInfo(name = "start_at")
    val startAt: Long,

    /** End instant (exclusive), epoch millis. */
    @ColumnInfo(name = "end_at")
    val endAt: Long,

    @ColumnInfo(name = "all_day")
    val allDay: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
