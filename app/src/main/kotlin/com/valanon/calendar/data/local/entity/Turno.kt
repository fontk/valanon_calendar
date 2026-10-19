package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of `turnos`: the shift assigned to a day.
 *
 * [fecha] is ISO 8601 `yyyy-MM-dd` (LocalDate.toString()), so text ordering and `BETWEEN` work
 * as date ordering. [tipoId] points to `tipos_turno` (schema v5); deleting the type deletes the
 * days that used it (ON DELETE CASCADE).
 */
@Entity(
    tableName = "turnos",
    foreignKeys = [
        ForeignKey(
            entity = TipoTurno::class,
            parentColumns = ["id"],
            childColumns = ["tipo_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tipo_id"], name = "idx_turnos_tipo")],
)
data class Turno(
    @PrimaryKey
    val fecha: String,
    @ColumnInfo(name = "tipo_id")
    val tipoId: TipoTurnoId,
)
