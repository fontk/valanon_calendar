package com.valanon.calendar.ui.preview

import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Tarea
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

    /** The two templates of Plantillas.dc.html. */
    fun plantillas(): List<PlantillaConRecuento> = listOf(
        PlantillaConRecuento(Plantilla(id = 1, tipo = TipoPlantilla.TRABAJADORES, nombre = "Equipo de planta", orden = 1), 4),
        PlantillaConRecuento(Plantilla(id = 2, tipo = TipoPlantilla.TRABAJADORES, nombre = "Refuerzos fin de semana", orden = 2), 2),
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
        plantillas = plantillas(),
        trabajadores = trabajadoresEquipo(),
        asignaciones = asignacionesEquipo(),
    )

    fun tareas(): List<Tarea> = listOf(
        Tarea(id = 1, texto = "Preparar el uniforme", hecha = true, orden = 1),
        Tarea(id = 2, texto = "Revisar el cuadrante de la semana", hecha = true, orden = 2),
        Tarea(id = 3, texto = "Comprar comida para la noche", hecha = false, orden = 3),
        Tarea(id = 4, texto = "Cargar la batería de la linterna", hecha = false, orden = 4),
        Tarea(id = 5, texto = "Pedir cambio de turno del día 24", hecha = false, orden = 5),
    )
}
