package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.valanon.calendar.data.local.entity.Asignacion
import kotlinx.coroutines.flow.Flow

@Dao
interface AsignacionDao {

    /** Assignments of the workers of [plantillaId] with `desde <= fecha <= hasta`. */
    @Query(
        """
        SELECT a.* FROM asignaciones a
        JOIN trabajadores t ON t.id = a.trabajador_id
        WHERE t.plantilla_id = :plantillaId AND a.fecha BETWEEN :desde AND :hasta
        """,
    )
    fun observarMes(plantillaId: Long, desde: String, hasta: String): Flow<List<Asignacion>>

    /** `INSERT OR REPLACE`: the unique (fecha, trabajador_id) index replaces the previous shift. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asignacion: Asignacion)

    /** "Quitar turno": the worker is left unassigned that day. */
    @Query("DELETE FROM asignaciones WHERE fecha = :fecha AND trabajador_id = :trabajadorId")
    suspend fun borrar(fecha: String, trabajadorId: Long)

    /** Assignments of a template, for the "Eliminar plantilla" dialog. */
    @Query(
        """
        SELECT COUNT(*) FROM asignaciones a
        JOIN trabajadores t ON t.id = a.trabajador_id
        WHERE t.plantilla_id = :plantillaId
        """,
    )
    fun observarContarDePlantilla(plantillaId: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM asignaciones a
        JOIN trabajadores t ON t.id = a.trabajador_id
        WHERE t.plantilla_id = :plantillaId
        """,
    )
    suspend fun contarDePlantilla(plantillaId: Long): Int

    /** Read before deleting a worker so "Deshacer" can put its assignments back. */
    @Query("SELECT * FROM asignaciones WHERE trabajador_id = :trabajadorId ORDER BY fecha")
    suspend fun obtenerDeTrabajador(trabajadorId: Long): List<Asignacion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(asignaciones: List<Asignacion>)
}
