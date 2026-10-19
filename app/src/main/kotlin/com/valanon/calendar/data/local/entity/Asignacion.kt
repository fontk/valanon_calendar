package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of `asignaciones` (schema v3, spec 3b): the shift of a worker on a day, for the
 * Turnos tab. Unique per (fecha, trabajador). Deleting the worker — or its template — or the
 * shift type deletes its assignments (ON DELETE CASCADE). Never mixed with `turnos` (the user's
 * own shifts).
 *
 * The SQL `UNIQUE (fecha, trabajador_id)` table constraint is expressed as a unique index,
 * which SQLite enforces the same way.
 */
@Entity(
    tableName = "asignaciones",
    foreignKeys = [
        ForeignKey(
            entity = Trabajador::class,
            parentColumns = ["id"],
            childColumns = ["trabajador_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TipoTurno::class,
            parentColumns = ["id"],
            childColumns = ["tipo_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["fecha", "trabajador_id"], unique = true, name = "idx_asignaciones_fecha_trabajador"),
        Index(value = ["trabajador_id"], name = "idx_asignaciones_trabajador"),
        Index(value = ["tipo_id"], name = "idx_asignaciones_tipo"),
    ],
)
data class Asignacion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** ISO `yyyy-MM-dd`, like `turnos.fecha`. */
    val fecha: String,
    @ColumnInfo(name = "trabajador_id")
    val trabajadorId: Long,
    @ColumnInfo(name = "tipo_id")
    val tipoId: TipoTurnoId,
)
