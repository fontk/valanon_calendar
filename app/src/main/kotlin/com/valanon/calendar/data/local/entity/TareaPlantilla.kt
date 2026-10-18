package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of `tareas_plantilla` (schema v4): a task of a "Tareas" template (a preset), ordered by
 * [orden]. Checklist copies them into `tareas` when it creates a list; the copies are independent.
 */
@Entity(
    tableName = "tareas_plantilla",
    foreignKeys = [
        ForeignKey(
            entity = Plantilla::class,
            parentColumns = ["id"],
            childColumns = ["plantilla_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["plantilla_id", "orden"], name = "idx_tareas_plantilla")],
)
data class TareaPlantilla(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "plantilla_id")
    val plantillaId: Long,
    val texto: String,
    val orden: Int,
)
