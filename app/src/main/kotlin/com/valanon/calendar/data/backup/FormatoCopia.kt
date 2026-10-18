package com.valanon.calendar.data.backup

import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.DatosApp
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.TipoTurno
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** The file is not a valid Mis turnos backup (wrong format, newer version or broken data). */
class CopiaInvalidaException(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/**
 * JSON backup format, independent from the Room schema so old files stay readable after future
 * migrations:
 *
 * ```
 * { "formato": "mis-turnos-copia", "version": 1, "creado": "2026-10-18T10:00:00Z",
 *   "turnos": [{"fecha","tipo"}], "tareas": [{"id","texto","hecha","orden"}],
 *   "plantillas": [{"id","tipo","nombre","orden"}], "trabajadores": [{"id","plantillaId","nombre","orden"}],
 *   "asignaciones": [{"fecha","trabajadorId","tipo"}], "ajustes": [{"clave","valor"}] }
 * ```
 * Enum values are the schema values ("manana", "trabajadores"). Missing arrays mean "empty", so
 * a future version can add tables without breaking this reader.
 */
object FormatoCopia {
    const val FORMATO = "mis-turnos-copia"

    /** Bump when the JSON changes incompatibly; older versions must still be readable. */
    const val VERSION = 1

    fun aJson(datos: DatosApp, creado: Instant): String = JSONObject().apply {
        put("formato", FORMATO)
        put("version", VERSION)
        put("creado", creado.toString())
        put("turnos", JSONArray(datos.turnos.map { JSONObject().put("fecha", it.fecha).put("tipo", it.tipo.valor) }))
        put("tareas", JSONArray(datos.tareas.map {
            JSONObject().put("id", it.id).put("texto", it.texto).put("hecha", it.hecha).put("orden", it.orden)
        }))
        put("plantillas", JSONArray(datos.plantillas.map {
            JSONObject().put("id", it.id).put("tipo", it.tipo.valor).put("nombre", it.nombre).put("orden", it.orden)
        }))
        put("trabajadores", JSONArray(datos.trabajadores.map {
            JSONObject().put("id", it.id).put("plantillaId", it.plantillaId).put("nombre", it.nombre).put("orden", it.orden)
        }))
        put("asignaciones", JSONArray(datos.asignaciones.map {
            JSONObject().put("fecha", it.fecha).put("trabajadorId", it.trabajadorId).put("tipo", it.tipo.valor)
        }))
        put("ajustes", JSONArray(datos.ajustes.map { JSONObject().put("clave", it.clave).put("valor", it.valor) }))
    }.toString(2)

    /** Parses and validates a backup. Throws [CopiaInvalidaException] with a user-safe reason. */
    fun desdeJson(texto: String): DatosApp {
        val raiz = try {
            JSONObject(texto)
        } catch (e: JSONException) {
            throw CopiaInvalidaException("El archivo no es una copia de Mis turnos", e)
        }
        if (raiz.optString("formato") != FORMATO) throw CopiaInvalidaException("El archivo no es una copia de Mis turnos")
        val version = raiz.optInt("version", -1)
        if (version < 1) throw CopiaInvalidaException("La copia no indica una versión válida")
        if (version > VERSION) throw CopiaInvalidaException("La copia es de una versión más nueva de la app")

        val datos = try {
            DatosApp(
                turnos = raiz.objetos("turnos").map { Turno(fecha(it.getString("fecha")), TipoTurno.desdeValor(it.getString("tipo"))) },
                tareas = raiz.objetos("tareas").map {
                    Tarea(it.getLong("id"), it.getString("texto"), it.getBoolean("hecha"), it.getInt("orden"))
                },
                plantillas = raiz.objetos("plantillas").map {
                    Plantilla(it.getLong("id"), TipoPlantilla.desdeValor(it.getString("tipo")), it.getString("nombre"), it.getInt("orden"))
                },
                trabajadores = raiz.objetos("trabajadores").map {
                    Trabajador(it.getLong("id"), it.getLong("plantillaId"), it.getString("nombre"), it.getInt("orden"))
                },
                asignaciones = raiz.objetos("asignaciones").map {
                    Asignacion(fecha = fecha(it.getString("fecha")), trabajadorId = it.getLong("trabajadorId"), tipo = TipoTurno.desdeValor(it.getString("tipo")))
                },
                ajustes = raiz.objetos("ajustes").map { Ajuste(it.getString("clave"), it.getString("valor")) },
            )
        } catch (e: JSONException) {
            throw CopiaInvalidaException("La copia está incompleta o dañada", e)
        } catch (e: IllegalArgumentException) {
            throw CopiaInvalidaException("La copia contiene valores no válidos", e)
        }
        validarReferencias(datos)
        return datos
    }

    /** Checks what the database would reject, before touching it. */
    private fun validarReferencias(d: DatosApp) {
        val plantillas = d.plantillas.map { it.id }.toSet()
        val trabajadores = d.trabajadores.map { it.id }.toSet()
        val problema = when {
            plantillas.size != d.plantillas.size || trabajadores.size != d.trabajadores.size ||
                d.tareas.map { it.id }.toSet().size != d.tareas.size -> "ids repetidos"
            d.turnos.map { it.fecha }.toSet().size != d.turnos.size -> "días repetidos en tus turnos"
            d.trabajadores.any { it.plantillaId !in plantillas } -> "trabajadores sin plantilla"
            d.asignaciones.any { it.trabajadorId !in trabajadores } -> "turnos de trabajadores inexistentes"
            d.asignaciones.map { it.fecha to it.trabajadorId }.toSet().size != d.asignaciones.size -> "turnos del equipo repetidos"
            else -> null
        }
        if (problema != null) throw CopiaInvalidaException("La copia está dañada ($problema)")
    }

    /** ISO date, validated (the database stores it as text). */
    private fun fecha(valor: String): String = try {
        LocalDate.parse(valor).toString()
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Fecha no válida: $valor", e)
    }

    private fun JSONObject.objetos(nombre: String): List<JSONObject> {
        val array = optJSONArray(nombre) ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
}
