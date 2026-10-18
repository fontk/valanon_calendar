package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantillaDao {

    /**
     * All templates in creation order, each with its number of items. Each type keeps its items
     * in its own table, so both are counted (the other one is always 0).
     */
    @Query(
        """
        SELECT p.*,
            (SELECT COUNT(*) FROM trabajadores t WHERE t.plantilla_id = p.id)
            + (SELECT COUNT(*) FROM tareas_plantilla tp WHERE tp.plantilla_id = p.id) AS num_elementos
        FROM plantillas p
        ORDER BY p.orden ASC, p.id ASC
        """,
    )
    fun observarTodas(): Flow<List<PlantillaConRecuento>>

    /** Emits `null` once the template no longer exists. */
    @Query("SELECT * FROM plantillas WHERE id = :id")
    fun observar(id: Long): Flow<Plantilla?>

    @Query("SELECT * FROM plantillas WHERE id = :id")
    suspend fun obtener(id: Long): Plantilla?

    /** Appends a template at the end of the list in one statement. Returns its id. */
    @Query(
        """
        INSERT INTO plantillas (tipo, nombre, orden)
        VALUES (:tipo, :nombre, COALESCE((SELECT MAX(orden) FROM plantillas), 0) + 1)
        """,
    )
    suspend fun insertarAlFinal(tipo: TipoPlantilla, nombre: String): Long

    @Query("UPDATE plantillas SET nombre = :nombre WHERE id = :id")
    suspend fun renombrar(id: Long, nombre: String)

    /**
     * Also deletes its workers or preset tasks (ON DELETE CASCADE). Checklist lists copied from
     * it stay, with `plantilla_id` set to NULL.
     */
    @Query("DELETE FROM plantillas WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("SELECT COUNT(*) FROM plantillas")
    suspend fun contar(): Int
}
