package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.valanon.calendar.data.local.entity.Tarea
import kotlinx.coroutines.flow.Flow

/** Checklist tasks. Every query works on one list (`lista_id`). */
@Dao
interface TareaDao {

    @Query("SELECT * FROM tareas WHERE lista_id = :listaId ORDER BY orden ASC, id ASC")
    fun observarDeLista(listaId: Long): Flow<List<Tarea>>

    /** Appends a new unchecked task at the end of its list in a single statement. Returns its id. */
    @Query(
        """
        INSERT INTO tareas (lista_id, texto, hecha, orden)
        VALUES (
            :listaId,
            :texto,
            0,
            COALESCE((SELECT MAX(orden) FROM tareas WHERE lista_id = :listaId), 0) + 1
        )
        """,
    )
    suspend fun agregarAlFinal(listaId: Long, texto: String): Long

    /**
     * Puts back a deleted task with its id, state and position (undo). Does nothing if its list
     * was deleted in the meantime, instead of failing on the foreign key.
     */
    @Query(
        """
        INSERT INTO tareas (id, lista_id, texto, hecha, orden)
        SELECT :id, :listaId, :texto, :hecha, :orden
        WHERE EXISTS (SELECT 1 FROM listas WHERE id = :listaId)
        """,
    )
    suspend fun restaurar(id: Long, listaId: Long, texto: String, hecha: Boolean, orden: Int)

    /** Copies the tasks of a "Tareas" template into a list, unchecked and in the same order. */
    @Query(
        """
        INSERT INTO tareas (lista_id, texto, hecha, orden)
        SELECT :listaId, texto, 0, orden FROM tareas_plantilla
        WHERE plantilla_id = :plantillaId
        ORDER BY orden ASC, id ASC
        """,
    )
    suspend fun copiarDePlantilla(listaId: Long, plantillaId: Long)

    @Query("UPDATE tareas SET hecha = :hecha WHERE id = :id")
    suspend fun actualizarHecha(id: Long, hecha: Boolean)

    @Query("DELETE FROM tareas WHERE id = :id")
    suspend fun borrar(id: Long)

    /** "Reiniciar" button: unchecks the tasks of one list, deletes nothing. */
    @Query("UPDATE tareas SET hecha = 0 WHERE lista_id = :listaId")
    suspend fun reiniciarLista(listaId: Long)
}
