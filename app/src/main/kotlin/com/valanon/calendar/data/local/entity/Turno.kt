package com.valanon.calendar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.valanon.calendar.data.model.TipoTurno

/**
 * One row of `turnos`: the shift assigned to a day.
 *
 * [fecha] is ISO 8601 `yyyy-MM-dd` (LocalDate.toString()), so text ordering and
 * `BETWEEN` work as date ordering. [tipo] is stored as its schema value via [com.valanon.calendar.data.local.Conversores].
 */
@Entity(tableName = "turnos")
data class Turno(
    @PrimaryKey
    val fecha: String,
    val tipo: TipoTurno,
)
