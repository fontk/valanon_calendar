package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of `tareas`: a checklist item of a [Lista] (schema v4), ordered by [orden] inside its
 * list. Deleting the list deletes its tasks (ON DELETE CASCADE).
 */
@Entity(
    tableName = "tareas",
    foreignKeys = [
        ForeignKey(
            entity = Lista::class,
            parentColumns = ["id"],
            childColumns = ["lista_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["lista_id", "orden"], name = "idx_tareas_lista")],
)
data class Tarea(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "lista_id")
    val listaId: Long,
    val texto: String,
    @ColumnInfo(defaultValue = "0")
    val hecha: Boolean = false,
    val orden: Int,
)
