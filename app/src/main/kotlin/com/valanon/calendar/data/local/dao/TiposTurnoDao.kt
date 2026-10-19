package com.valanon.calendar.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.local.entity.TipoTurnoId
import kotlinx.coroutines.flow.Flow

/** How many rows use a shift type: days of the own calendar and team-shift assignments. */
data class UsosTipoTurno(
    @ColumnInfo(name = "dias") val dias: Int,
    @ColumnInfo(name = "equipo") val equipo: Int,
) {
    val enUso: Boolean get() = dias > 0 || equipo > 0
}

/** A shift type with its usage, for the "Tipos de turno" list (spec 3c.2). */
data class TipoTurnoConUsos(
    @Embedded val tipo: TipoTurno,
    @Embedded val usos: UsosTipoTurno,
)

@Dao
interface TiposTurnoDao {

    /** Every type in display order. All the screens and the PDF observe this. */
    @Query("SELECT * FROM tipos_turno ORDER BY orden ASC, id ASC")
    fun observarTodos(): Flow<List<TipoTurno>>

    @Query("SELECT * FROM tipos_turno ORDER BY orden ASC, id ASC")
    suspend fun obtenerTodos(): List<TipoTurno>

    @Query(
        """
        SELECT t.*,
            (SELECT COUNT(*) FROM turnos WHERE tipo_id = t.id) AS dias,
            (SELECT COUNT(*) FROM asignaciones WHERE tipo_id = t.id) AS equipo
        FROM tipos_turno t
        ORDER BY t.orden ASC, t.id ASC
        """,
    )
    fun observarConUsos(): Flow<List<TipoTurnoConUsos>>

    /** Appends a type at the end in one statement. Returns its id. */
    @Query(
        """
        INSERT INTO tipos_turno (clave, nombre, abreviatura, color, orden)
        VALUES (:clave, :nombre, :abreviatura, :color, COALESCE((SELECT MAX(orden) FROM tipos_turno), 0) + 1)
        """,
    )
    suspend fun insertarAlFinal(clave: String?, nombre: String, abreviatura: String, color: String): Long

    @Query("UPDATE tipos_turno SET nombre = :nombre, abreviatura = :abreviatura, color = :color WHERE id = :id")
    suspend fun actualizar(id: TipoTurnoId, nombre: String, abreviatura: String, color: String)

    /** Also deletes its days in `turnos` and `asignaciones` (ON DELETE CASCADE). */
    @Query("DELETE FROM tipos_turno WHERE id = :id")
    suspend fun borrar(id: TipoTurnoId)

    @Query("SELECT COUNT(*) FROM tipos_turno")
    suspend fun contar(): Int

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM turnos WHERE tipo_id = :id) AS dias,
            (SELECT COUNT(*) FROM asignaciones WHERE tipo_id = :id) AS equipo
        """,
    )
    suspend fun usos(id: TipoTurnoId): UsosTipoTurno

    @Query("SELECT * FROM tipos_turno WHERE clave = :clave")
    suspend fun obtenerPorClave(clave: String): TipoTurno?
}
