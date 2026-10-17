package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of `trabajadores` (schema v2): a worker of a "Trabajadores" template, ordered by
 * [orden] inside its template. Deleting the template deletes its workers (ON DELETE CASCADE).
 */
@Entity(
    tableName = "trabajadores",
    foreignKeys = [
        ForeignKey(
            entity = Plantilla::class,
            parentColumns = ["id"],
            childColumns = ["plantilla_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["plantilla_id", "orden"], name = "idx_trabajadores_plantilla")],
)
data class Trabajador(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "plantilla_id")
    val plantillaId: Long,
    val nombre: String,
    val orden: Int,
)
