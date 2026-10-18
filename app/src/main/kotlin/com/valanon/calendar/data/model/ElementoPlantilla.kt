package com.valanon.calendar.data.model

import com.valanon.calendar.data.local.entity.TareaPlantilla
import com.valanon.calendar.data.local.entity.Trabajador

/**
 * One item of a template, whatever its type: a worker's name or a preset task's text (spec 4b.3).
 * Both types share the detail screen, so it works with this instead of the two entities.
 */
data class ElementoPlantilla(
    val id: Long,
    val plantillaId: Long,
    val texto: String,
    val orden: Int,
)

fun Trabajador.comoElemento() = ElementoPlantilla(id, plantillaId, nombre, orden)

fun TareaPlantilla.comoElemento() = ElementoPlantilla(id, plantillaId, texto, orden)
