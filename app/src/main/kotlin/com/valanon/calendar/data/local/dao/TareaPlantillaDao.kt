package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.valanon.calendar.data.local.entity.TareaPlantilla
import kotlinx.coroutines.flow.Flow

/** Tasks of a "Tareas" template (preset). Same operations as [TrabajadorDao]. */
@Dao
interface TareaPlantillaDao {

    @Query("SELECT * FROM tareas_plantilla WHERE plantilla_id = :plantillaId ORDER BY orden ASC, id ASC")
    fun observarDePlantilla(plantillaId: Long): Flow<List<TareaPlantilla>>

    /** Appends a task at the end of its template in one statement. Returns its id. */
    @Query(
        """
        INSERT INTO tareas_plantilla (plantilla_id, texto, orden)
        VALUES (
            :plantillaId,
            :texto,
            COALESCE((SELECT MAX(orden) FROM tareas_plantilla WHERE plantilla_id = :plantillaId), 0) + 1
        )
        """,
    )
    suspend fun agregarAlFinal(plantillaId: Long, texto: String): Long

    @Query("UPDATE tareas_plantilla SET texto = :texto WHERE id = :id")
    suspend fun cambiarTexto(id: Long, texto: String)

    @Query("DELETE FROM tareas_plantilla WHERE id = :id")
    suspend fun borrar(id: Long)

    /** Inserts [tarea] as given (keeps id and orden). Used to undo a delete. */
    @Insert
    suspend fun insertar(tarea: TareaPlantilla): Long
}
