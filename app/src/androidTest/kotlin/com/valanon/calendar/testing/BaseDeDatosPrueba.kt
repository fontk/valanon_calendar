package com.valanon.calendar.testing

import com.valanon.calendar.data.local.AppDatabase
import com.valanon.calendar.data.local.TIPOS_TURNO_POR_DEFECTO
import com.valanon.calendar.data.local.insertarTiposTurnoPorDefecto

/** The five default shift types (ids 1–5), as every database starts with them. */
object Tipos {
    val LIBRE = TIPOS_TURNO_POR_DEFECTO[0]
    val MANANA = TIPOS_TURNO_POR_DEFECTO[1]
    val TARDE = TIPOS_TURNO_POR_DEFECTO[2]
    val NOCHE = TIPOS_TURNO_POR_DEFECTO[3]
    val VACACIONES = TIPOS_TURNO_POR_DEFECTO[4]
}

/**
 * Empties every table, then puts back the five default shift types: `clearAllTables()` also
 * empties `tipos_turno`, and a real install always has them (seeded on creation or by migration).
 */
fun AppDatabase.limpiarYSembrar() {
    clearAllTables()
    insertarTiposTurnoPorDefecto(openHelper.writableDatabase)
}
