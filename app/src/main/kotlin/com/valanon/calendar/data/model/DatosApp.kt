package com.valanon.calendar.data.model

import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Lista
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.TareaPlantilla
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.local.entity.Turno

/** Every row of the app's database: what a backup file contains. */
data class DatosApp(
    val tiposTurno: List<TipoTurno> = emptyList(),
    val turnos: List<Turno> = emptyList(),
    val listas: List<Lista> = emptyList(),
    val tareas: List<Tarea> = emptyList(),
    val plantillas: List<Plantilla> = emptyList(),
    val trabajadores: List<Trabajador> = emptyList(),
    val tareasPlantilla: List<TareaPlantilla> = emptyList(),
    val asignaciones: List<Asignacion> = emptyList(),
    val ajustes: List<Ajuste> = emptyList(),
)
