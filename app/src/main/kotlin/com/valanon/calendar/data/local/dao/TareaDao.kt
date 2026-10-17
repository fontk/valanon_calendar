package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.valanon.calendar.data.local.entity.Tarea
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {

    @Query("SELECT * FROM tareas ORDER BY orden ASC, id ASC")
    fun observarTodas(): Flow<List<Tarea>>

    /** Inserts [tarea] as given (keeps its id and orden if set). Used to undo a delete. */
    @Insert
    suspend fun insertar(tarea: Tarea): Long

    /** Appends a new unchecked task at the end of the list in a single statement. Returns its id. */
    @Query(
        """
        INSERT INTO tareas (texto, hecha, orden)
        VALUES (:texto, 0, COALESCE((SELECT MAX(orden) FROM tareas), 0) + 1)
        """,
    )
    suspend fun agregarAlFinal(texto: String): Long

    @Query("UPDATE tareas SET hecha = :hecha WHERE id = :id")
    suspend fun actualizarHecha(id: Long, hecha: Boolean)

    @Query("DELETE FROM tareas WHERE id = :id")
    suspend fun borrar(id: Long)

    /** Floating button: uncheck everything, delete nothing. */
    @Query("UPDATE tareas SET hecha = 0")
    suspend fun reiniciarTodas()

    @Query("SELECT MAX(orden) FROM tareas")
    suspend fun maxOrden(): Int?
}
