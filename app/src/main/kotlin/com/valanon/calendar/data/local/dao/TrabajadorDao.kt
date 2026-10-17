package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.valanon.calendar.data.local.entity.Trabajador
import kotlinx.coroutines.flow.Flow

@Dao
interface TrabajadorDao {

    @Query("SELECT * FROM trabajadores WHERE plantilla_id = :plantillaId ORDER BY orden ASC, id ASC")
    fun observarDePlantilla(plantillaId: Long): Flow<List<Trabajador>>

    /** Appends a worker at the end of its template in one statement. Returns its id. */
    @Query(
        """
        INSERT INTO trabajadores (plantilla_id, nombre, orden)
        VALUES (
            :plantillaId,
            :nombre,
            COALESCE((SELECT MAX(orden) FROM trabajadores WHERE plantilla_id = :plantillaId), 0) + 1
        )
        """,
    )
    suspend fun agregarAlFinal(plantillaId: Long, nombre: String): Long

    @Query("UPDATE trabajadores SET nombre = :nombre WHERE id = :id")
    suspend fun renombrar(id: Long, nombre: String)

    @Query("DELETE FROM trabajadores WHERE id = :id")
    suspend fun borrar(id: Long)

    /** Inserts [trabajador] as given (keeps id and orden). Used to undo a delete. */
    @Insert
    suspend fun insertar(trabajador: Trabajador): Long
}
