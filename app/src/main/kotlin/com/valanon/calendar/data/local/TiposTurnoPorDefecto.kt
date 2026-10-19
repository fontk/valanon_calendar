package com.valanon.calendar.data.local

import com.valanon.calendar.data.local.entity.TipoTurno

/**
 * The five default shift types (spec 3c.6, mis-turnos-kit/db/schema.sql), with fixed ids 1–5.
 * They are DATA: [MIGRATION_4_5] inserts them and "Restablecer los cinco por defecto" restores
 * them. Nothing else in the app may hardcode a shift name, abbreviation or colour.
 * `clave` is also the text that `turnos.tipo` / `asignaciones.tipo` stored up to schema v4.
 */
val TIPOS_TURNO_POR_DEFECTO: List<TipoTurno> = listOf(
    TipoTurno(id = 1, clave = "libre", nombre = "Libre", abreviatura = "LIBRE", color = "gris", orden = 1),
    TipoTurno(id = 2, clave = "manana", nombre = "Mañana", abreviatura = "MAÑ", color = "amarillo", orden = 2),
    TipoTurno(id = 3, clave = "tarde", nombre = "Tarde", abreviatura = "TARDE", color = "azul", orden = 3),
    TipoTurno(id = 4, clave = "noche", nombre = "Noche", abreviatura = "NOCHE", color = "indigo", orden = 4),
    TipoTurno(id = 5, clave = "vacaciones", nombre = "Vacaciones", abreviatura = "VAC", color = "verde", orden = 5),
)

/** Maximum number of shift types: one per palette entry (spec 3c.6). */
const val MAX_TIPOS_TURNO = 16
