package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.MAX_TIPOS_TURNO
import com.valanon.calendar.data.local.TIPOS_TURNO_POR_DEFECTO
import com.valanon.calendar.data.local.Transacciones
import com.valanon.calendar.data.local.dao.TipoTurnoConUsos
import com.valanon.calendar.data.local.dao.TiposTurnoDao
import com.valanon.calendar.data.local.dao.UsosTipoTurno
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.local.entity.TipoTurnoId
import com.valanon.calendar.util.MAX_NOMBRE_TIPO_TURNO
import com.valanon.calendar.util.abreviaturaFinal
import kotlinx.coroutines.flow.Flow

/** Why a shift type could not be saved or deleted. */
enum class ErrorTipoTurno {
    /** "Escribe un nombre para el turno." */
    NOMBRE_VACIO,

    /** "Ya existe un turno con ese nombre." (case-insensitive) */
    NOMBRE_REPETIDO,

    /** Already [MAX_TIPOS_TURNO] types. */
    LIMITE,

    /** It is the last type left (minimum one). */
    ULTIMO,
}

/**
 * Configurable shift types (spec 3c). Every screen and the PDF observe [tipos]; changes show up
 * at once. Validation lives here so the UI and the tests share it.
 */
class TiposTurnoRepositorio(
    private val dao: TiposTurnoDao,
    /** Multi-step writes below run atomically (all or nothing). */
    private val transacciones: Transacciones,
) {
    val tipos: Flow<List<TipoTurno>> = dao.observarTodos()

    /** The types with how many days and team shifts use each one ("Tipos de turno" list). */
    val tiposConUsos: Flow<List<TipoTurnoConUsos>> = dao.observarConUsos()

    suspend fun usos(id: TipoTurnoId): UsosTipoTurno = dao.usos(id)

    /**
     * Creates (when [id] is null) or updates a type. The name is trimmed and capped at 20
     * characters; a blank abbreviation is derived from the name. Returns `null` on success.
     */
    suspend fun guardar(id: TipoTurnoId?, nombre: String, abreviatura: String, color: String): ErrorTipoTurno? =
        transacciones.ejecutar {
            val limpio = nombre.trim().take(MAX_NOMBRE_TIPO_TURNO)
            if (limpio.isEmpty()) return@ejecutar ErrorTipoTurno.NOMBRE_VACIO
            val todos = dao.obtenerTodos()
            if (todos.any { it.id != id && it.nombre.equals(limpio, ignoreCase = true) }) {
                return@ejecutar ErrorTipoTurno.NOMBRE_REPETIDO
            }
            val abrev = abreviaturaFinal(abreviatura, limpio)
            if (id == null) {
                if (todos.size >= MAX_TIPOS_TURNO) return@ejecutar ErrorTipoTurno.LIMITE
                dao.insertarAlFinal(clave = null, nombre = limpio, abreviatura = abrev, color = color)
            } else {
                dao.actualizar(id, limpio, abrev, color)
            }
            null
        }

    /** Deletes the type and, by cascade, the days and team shifts that use it. Refuses the last one. */
    suspend fun borrar(id: TipoTurnoId): ErrorTipoTurno? = transacciones.ejecutar {
        if (dao.contar() <= 1) return@ejecutar ErrorTipoTurno.ULTIMO
        dao.borrar(id)
        null
    }

    /**
     * "Restablecer los cinco por defecto" (spec 3c.5): each default that still exists gets its
     * original name, abbreviation and colour back; a deleted one is created again at the end (while
     * there is room under the limit). Custom types are left alone.
     */
    suspend fun restablecer() = transacciones.ejecutar {
        var total = dao.contar()
        TIPOS_TURNO_POR_DEFECTO.forEach { defecto ->
            val actual = dao.obtenerPorClave(checkNotNull(defecto.clave))
            if (actual != null) {
                dao.actualizar(actual.id, defecto.nombre, defecto.abreviatura, defecto.color)
            } else if (total < MAX_TIPOS_TURNO) {
                dao.insertarAlFinal(defecto.clave, defecto.nombre, defecto.abreviatura, defecto.color)
                total++
            }
        }
    }
}
