package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantillaDao {

    /** All templates in creation order, each with its number of workers. */
    @Query(
        """
        SELECT p.*, COUNT(t.id) AS num_trabajadores
        FROM plantillas p LEFT JOIN trabajadores t ON t.plantilla_id = p.id
        GROUP BY p.id
        ORDER BY p.orden ASC, p.id ASC
        """,
    )
    fun observarTodas(): Flow<List<PlantillaConRecuento>>

    /** Emits `null` once the template no longer exists. */
    @Query("SELECT * FROM plantillas WHERE id = :id")
    fun observar(id: Long): Flow<Plantilla?>

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

    /** Also deletes its workers (ON DELETE CASCADE). */
    @Query("DELETE FROM plantillas WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("SELECT COUNT(*) FROM plantillas")
    suspend fun contar(): Int
}
