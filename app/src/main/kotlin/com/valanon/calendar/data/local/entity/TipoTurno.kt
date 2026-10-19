package com.valanon.calendar.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Id of a row of `tipos_turno`, as stored in `turnos.tipo_id` and `asignaciones.tipo_id`. */
typealias TipoTurnoId = Long

/**
 * One row of `tipos_turno` (schema v5, spec 3c): a shift type the user can rename, recolour,
 * add or delete. [color] is a key of `PaletaTurnos` (the palette lives in code, not in the
 * database). The five defaults carry a [clave] (`libre`, `manana`, …) so "Restablecer" can find
 * them again; custom types have `clave = null`. Lists are ordered by [orden].
 *
 * The SQL column constraint `clave UNIQUE` is a unique index; SQLite allows many NULLs in it.
 */
@Entity(
    tableName = "tipos_turno",
    indices = [Index(value = ["clave"], unique = true, name = "idx_tipos_turno_clave")],
)
data class TipoTurno(
    @PrimaryKey(autoGenerate = true)
    val id: TipoTurnoId = 0,
    val clave: String? = null,
    val nombre: String,
    val abreviatura: String,
    val color: String,
    val orden: Int,
) {
    /** One of the five default types (they can still be edited or deleted). */
    val porDefecto: Boolean get() = clave != null
}
