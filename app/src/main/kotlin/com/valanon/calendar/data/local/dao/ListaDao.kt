package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.valanon.calendar.data.local.entity.ListaConProgreso
import kotlinx.coroutines.flow.Flow

/** Checklist lists (spec 4.0). */
@Dao
interface ListaDao {

    /** Every list in order, with its progress and the current name of its source template. */
    @Query(
        """
        SELECT l.*, p.nombre AS nombre_plantilla,
            (SELECT COUNT(*) FROM tareas t WHERE t.lista_id = l.id) AS total,
            (SELECT COUNT(*) FROM tareas t WHERE t.lista_id = l.id AND t.hecha = 1) AS hechas
        FROM listas l LEFT JOIN plantillas p ON p.id = l.plantilla_id
        ORDER BY l.orden ASC, l.id ASC
        """,
    )
    fun observarTodas(): Flow<List<ListaConProgreso>>

    /** Appends a list at the end in one statement. Returns its id. */
    @Query(
        """
        INSERT INTO listas (nombre, plantilla_id, orden)
        VALUES (:nombre, :plantillaId, COALESCE((SELECT MAX(orden) FROM listas), 0) + 1)
        """,
    )
    suspend fun insertarAlFinal(nombre: String, plantillaId: Long?): Long

    @Query("UPDATE listas SET nombre = :nombre WHERE id = :id")
    suspend fun renombrar(id: Long, nombre: String)

    /** Also deletes its tasks (ON DELETE CASCADE). */
    @Query("DELETE FROM listas WHERE id = :id")
    suspend fun borrar(id: Long)

    /** Ids in display order, to pick the neighbour when one is deleted. */
    @Query("SELECT id FROM listas ORDER BY orden ASC, id ASC")
    suspend fun ids(): List<Long>
}
