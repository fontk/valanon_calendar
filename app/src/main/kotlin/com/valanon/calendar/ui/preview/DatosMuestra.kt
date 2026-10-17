package com.valanon.calendar.ui.preview

import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.model.TipoTurno
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

    fun tareas(): List<Tarea> = listOf(
        Tarea(id = 1, texto = "Preparar el uniforme", hecha = true, orden = 1),
        Tarea(id = 2, texto = "Revisar el cuadrante de la semana", hecha = true, orden = 2),
        Tarea(id = 3, texto = "Comprar comida para la noche", hecha = false, orden = 3),
        Tarea(id = 4, texto = "Cargar la batería de la linterna", hecha = false, orden = 4),
        Tarea(id = 5, texto = "Pedir cambio de turno del día 24", hecha = false, orden = 5),
    )
}
