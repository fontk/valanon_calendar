package com.valanon.calendar.data.backup

import com.valanon.calendar.data.local.CLAVE_LISTA_CHECKLIST
import com.valanon.calendar.data.local.NOMBRE_LISTA_MIGRADA
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Lista
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.TareaPlantilla
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
 * { "formato": "mis-turnos-copia", "version": 2, "creado": "2026-10-18T10:00:00Z",
 *   "turnos": [{"fecha","tipo"}],
 *   "listas": [{"id","nombre","plantillaId"?,"orden"}], "tareas": [{"id","listaId","texto","hecha","orden"}],
 *   "plantillas": [{"id","tipo","nombre","orden"}], "trabajadores": [{"id","plantillaId","nombre","orden"}],
 *   "tareasPlantilla": [{"id","plantillaId","texto","orden"}],
 *   "asignaciones": [{"fecha","trabajadorId","tipo"}], "ajustes": [{"clave","valor"}] }
 * ```
 * Enum values are the schema values ("manana", "trabajadores"). Missing arrays mean "empty", so
 * a future version can add tables without breaking this reader.
 *
 * Version 1 (app 0.6.0) had a single checklist: no "listas" and no "listaId". Its tasks are put in
 * one list "Mi checklist", as MIGRATION_3_4 does with the database.
 */
object FormatoCopia {
    const val FORMATO = "mis-turnos-copia"

    /** Bump when the JSON changes incompatibly; older versions must still be readable. */
    const val VERSION = 2

    fun aJson(datos: DatosApp, creado: Instant): String = JSONObject().apply {
        put("formato", FORMATO)
        put("version", VERSION)
        put("creado", creado.toString())
        put("turnos", JSONArray(datos.turnos.map { JSONObject().put("fecha", it.fecha).put("tipo", it.tipo.valor) }))
        put("listas", JSONArray(datos.listas.map {
            JSONObject().put("id", it.id).put("nombre", it.nombre).put("plantillaId", it.plantillaId ?: JSONObject.NULL)
                .put("orden", it.orden)
        }))
        put("tareas", JSONArray(datos.tareas.map {
            JSONObject().put("id", it.id).put("listaId", it.listaId).put("texto", it.texto).put("hecha", it.hecha)
                .put("orden", it.orden)
        }))
        put("plantillas", JSONArray(datos.plantillas.map {
            JSONObject().put("id", it.id).put("tipo", it.tipo.valor).put("nombre", it.nombre).put("orden", it.orden)
        }))
        put("trabajadores", JSONArray(datos.trabajadores.map {
            JSONObject().put("id", it.id).put("plantillaId", it.plantillaId).put("nombre", it.nombre).put("orden", it.orden)
        }))
        put("tareasPlantilla", JSONArray(datos.tareasPlantilla.map {
            JSONObject().put("id", it.id).put("plantillaId", it.plantillaId).put("texto", it.texto).put("orden", it.orden)
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
            val base = DatosApp(
                turnos = raiz.objetos("turnos").map { Turno(fecha(it.getString("fecha")), TipoTurno.desdeValor(it.getString("tipo"))) },
                listas = raiz.objetos("listas").map {
                    Lista(it.getLong("id"), it.getString("nombre"), it.longONulo("plantillaId"), it.getInt("orden"))
                },
                plantillas = raiz.objetos("plantillas").map {
                    Plantilla(it.getLong("id"), TipoPlantilla.desdeValor(it.getString("tipo")), it.getString("nombre"), it.getInt("orden"))
                },
                trabajadores = raiz.objetos("trabajadores").map {
                    Trabajador(it.getLong("id"), it.getLong("plantillaId"), it.getString("nombre"), it.getInt("orden"))
                },
                tareasPlantilla = raiz.objetos("tareasPlantilla").map {
                    TareaPlantilla(it.getLong("id"), it.getLong("plantillaId"), it.getString("texto"), it.getInt("orden"))
                },
                asignaciones = raiz.objetos("asignaciones").map {
                    Asignacion(fecha = fecha(it.getString("fecha")), trabajadorId = it.getLong("trabajadorId"), tipo = TipoTurno.desdeValor(it.getString("tipo")))
                },
                ajustes = raiz.objetos("ajustes").map { Ajuste(it.getString("clave"), it.getString("valor")) },
            )
            if (version == 1) conListaUnica(base, raiz.objetos("tareas")) else base.copy(tareas = tareas(raiz))
        } catch (e: JSONException) {
            throw CopiaInvalidaException("La copia está incompleta o dañada", e)
        } catch (e: IllegalArgumentException) {
            throw CopiaInvalidaException("La copia contiene valores no válidos", e)
        }
        validarReferencias(datos)
        return datos.sinOrigenesBorrados()
    }

    private fun tareas(raiz: JSONObject): List<Tarea> = raiz.objetos("tareas").map {
        Tarea(it.getLong("id"), it.getLong("listaId"), it.getString("texto"), it.getBoolean("hecha"), it.getInt("orden"))
    }

    /** Version 1: the single checklist becomes the list "Mi checklist", which is made active. */
    private fun conListaUnica(base: DatosApp, tareas: List<JSONObject>): DatosApp {
        if (tareas.isEmpty()) return base
        val lista = Lista(id = 1, nombre = NOMBRE_LISTA_MIGRADA, plantillaId = null, orden = 1)
        return base.copy(
            listas = listOf(lista),
            tareas = tareas.map {
                Tarea(it.getLong("id"), lista.id, it.getString("texto"), it.getBoolean("hecha"), it.getInt("orden"))
            },
            ajustes = base.ajustes.filterNot { it.clave == CLAVE_LISTA_CHECKLIST } +
                Ajuste(CLAVE_LISTA_CHECKLIST, lista.id.toString()),
        )
    }

    /** Checks what the database would reject, before touching it. */
    private fun validarReferencias(d: DatosApp) {
        val plantillas = d.plantillas.map { it.id }.toSet()
        val trabajadores = d.trabajadores.map { it.id }.toSet()
        val listas = d.listas.map { it.id }.toSet()
        val problema = when {
            plantillas.size != d.plantillas.size || trabajadores.size != d.trabajadores.size ||
                listas.size != d.listas.size || d.tareas.map { it.id }.toSet().size != d.tareas.size ||
                d.tareasPlantilla.map { it.id }.toSet().size != d.tareasPlantilla.size -> "ids repetidos"
            d.turnos.map { it.fecha }.toSet().size != d.turnos.size -> "días repetidos en tus turnos"
            d.tareas.any { it.listaId !in listas } -> "tareas sin lista"
            d.trabajadores.any { it.plantillaId !in plantillas } -> "trabajadores sin plantilla"
            d.tareasPlantilla.any { it.plantillaId !in plantillas } -> "tareas de plantilla sin plantilla"
            d.asignaciones.any { it.trabajadorId !in trabajadores } -> "turnos de trabajadores inexistentes"
            d.asignaciones.map { it.fecha to it.trabajadorId }.toSet().size != d.asignaciones.size -> "turnos del equipo repetidos"
            else -> null
        }
        if (problema != null) throw CopiaInvalidaException("La copia está dañada ($problema)")
    }

    /**
     * A list's source template is informative only (ON DELETE SET NULL): if it is not in the file,
     * the list is kept as a blank list instead of rejecting the whole backup.
     */
    private fun DatosApp.sinOrigenesBorrados(): DatosApp {
        val plantillas = plantillas.map { it.id }.toSet()
        return copy(listas = listas.map { if (it.plantillaId != null && it.plantillaId !in plantillas) it.copy(plantillaId = null) else it })
    }

    /** ISO date, validated (the database stores it as text). */
    private fun fecha(valor: String): String = try {
        LocalDate.parse(valor).toString()
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Fecha no válida: $valor", e)
    }

    private fun JSONObject.longONulo(nombre: String): Long? = if (isNull(nombre)) null else getLong(nombre)

    private fun JSONObject.objetos(nombre: String): List<JSONObject> {
        val array = optJSONArray(nombre) ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
}
