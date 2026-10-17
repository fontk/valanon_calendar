package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row of `tareas`: a reusable checklist item. Lists are ordered by [orden] ascending. */
@Entity(
    tableName = "tareas",
    indices = [Index(value = ["orden"], name = "idx_tareas_orden")],
)
data class Tarea(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val texto: String,
    @ColumnInfo(defaultValue = "0")
    val hecha: Boolean = false,
    val orden: Int,
)
