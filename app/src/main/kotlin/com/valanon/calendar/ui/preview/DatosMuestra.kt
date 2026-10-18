package com.valanon.calendar.ui.preview

import com.valanon.calendar.data.local.entity.Lista
import com.valanon.calendar.data.local.entity.ListaConProgreso
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.TareaPlantilla
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.ui.turnos.TurnosUiState
import java.time.LocalDate
import java.time.YearMonth

/**
 * Sample data from the mockups. ONLY for @Preview functions: production never preloads data
 * (AGENTS.md → criterios de aceptación).
 */
internal object DatosMuestra {

    val MES: YearMonth = YearMonth.of(2026, 10)
    val HOY: LocalDate = LocalDate.of(2026, 10, 17)

    /** Same pattern as Main.dc.html: M M T T N N L L…, vacation from the 26th. */
    fun turnosOctubre(): Map<LocalDate, TipoTurno> {
        val patron = listOf(
            TipoTurno.MANANA, TipoTurno.MANANA, TipoTurno.TARDE, TipoTurno.TARDE,
            TipoTurno.NOCHE, TipoTurno.NOCHE, TipoTurno.LIBRE, TipoTurno.LIBRE,
        )
        return (1..MES.lengthOfMonth()).associate { dia ->
            MES.atDay(dia) to if (dia >= 26) TipoTurno.VACACIONES else patron[(dia - 1) % patron.size]
        }
    }

    /** The templates of Plantillas.dc.html: two of workers and two task presets. */
    fun plantillas(): List<PlantillaConRecuento> = listOf(
        PlantillaConRecuento(Plantilla(id = 1, tipo = TipoPlantilla.TRABAJADORES, nombre = "Equipo de planta", orden = 1), 4),
        PlantillaConRecuento(Plantilla(id = 2, tipo = TipoPlantilla.TRABAJADORES, nombre = "Refuerzos fin de semana", orden = 2), 2),
        PlantillaConRecuento(Plantilla(id = 3, tipo = TipoPlantilla.TAREAS, nombre = "Rutina de apertura", orden = 3), 5),
        PlantillaConRecuento(Plantilla(id = 4, tipo = TipoPlantilla.TAREAS, nombre = "Cierre de turno", orden = 4), 3),
    )

    /** Workers of "Equipo de planta" in the mockup. */
    fun trabajadoresEquipo(): List<Trabajador> = listOf("Lucía Martínez", "Javier Ortega", "Ana Belén Ruiz", "Marcos Pérez")
        .mapIndexed { i, nombre -> Trabajador(id = i + 1L, plantillaId = 1, nombre = nombre, orden = i + 1) }

    /**
     * October assignments of Turnos.dc.html: worker i follows the 8-day pattern shifted by
     * 2·i, Ana is on vacation from the 26th and Marcos has nothing after the 20th.
     */
    fun asignacionesEquipo(): Map<LocalDate, Map<Long, TipoTurno>> {
        val patron = listOf(
            TipoTurno.MANANA, TipoTurno.MANANA, TipoTurno.TARDE, TipoTurno.TARDE,
            TipoTurno.NOCHE, TipoTurno.NOCHE, TipoTurno.LIBRE, TipoTurno.LIBRE,
        )
        return (1..MES.lengthOfMonth()).associate { dia ->
            MES.atDay(dia) to buildMap {
                for (i in 0 until 4) {
                    val id = i + 1L
                    when {
                        id == 3L && dia >= 26 -> put(id, TipoTurno.VACACIONES)
                        id == 4L && dia > 20 -> Unit
                        else -> put(id, patron[(dia - 1 + i * 2) % patron.size])
                    }
                }
            }
        }
    }

    /** Turnos tab state of the mockup (Equipo de planta, October 2026). */
    fun estadoTurnos() = TurnosUiState(
        mes = MES,
        hoy = HOY,
        cargando = false,
        plantilla = plantillas().first(),
        plantillas = plantillas().filter { it.plantilla.tipo == TipoPlantilla.TRABAJADORES },
        trabajadores = trabajadoresEquipo(),
        asignaciones = asignacionesEquipo(),
    )

    /** Preset of the "Rutina de apertura" task template (Checklist.dc.html). */
    private val RUTINA_APERTURA = listOf(
        "Revisar el cuadrante del día",
        "Comprobar la caja y el cambio",
        "Encender equipos y luces",
        "Repasar pedidos pendientes",
        "Limpiar la zona de atención",
    )

    fun tareasRutinaApertura(): List<TareaPlantilla> = RUTINA_APERTURA
        .mapIndexed { i, texto -> TareaPlantilla(id = i + 1L, plantillaId = 3, texto = texto, orden = i + 1) }

    /** Checklist lists of the mockup: "Rutina de apertura" (from the template, active) and "Mi checklist". */
    fun listas(): List<ListaConProgreso> = listOf(
        ListaConProgreso(Lista(id = 1, nombre = "Rutina de apertura", plantillaId = 3, orden = 1), "Rutina de apertura", total = 5, hechas = 2),
        ListaConProgreso(Lista(id = 2, nombre = "Mi checklist", plantillaId = null, orden = 2), null, total = 3, hechas = 1),
    )

    /** Tasks of the active list "Rutina de apertura": the first two done. */
    fun tareas(): List<Tarea> = RUTINA_APERTURA
        .mapIndexed { i, texto -> Tarea(id = i + 1L, listaId = 1, texto = texto, hecha = i < 2, orden = i + 1) }
}
