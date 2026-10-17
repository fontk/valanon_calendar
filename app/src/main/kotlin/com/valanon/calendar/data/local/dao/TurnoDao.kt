package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.valanon.calendar.data.local.entity.Turno
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnoDao {

    /** Shifts with `desde <= fecha <= hasta` (ISO dates, both inclusive). Re-emits on every change. */
    @Query("SELECT * FROM turnos WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha")
    fun observarRango(desde: String, hasta: String): Flow<List<Turno>>

    /** One-shot read of the same range (used by the PDF export). */
    @Query("SELECT * FROM turnos WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha")
    suspend fun obtenerRango(desde: String, hasta: String): List<Turno>

    /** `INSERT OR REPLACE`: assigns or changes the shift of a day. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(turno: Turno)

    @Query("DELETE FROM turnos WHERE fecha = :fecha")
    suspend fun borrar(fecha: String)
}
