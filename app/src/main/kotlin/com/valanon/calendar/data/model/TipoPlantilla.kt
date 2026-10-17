package com.valanon.calendar.data.model

/**
 * Kinds of reusable template (spec 4b). [valor] is what `plantillas.tipo` stores
 * (mis-turnos-kit/db/schema.sql). v1 has only [TRABAJADORES]; this enum is the extension
 * point for new types — do not add any until asked (AGENTS.md → "Qué no hacer").
 *
 * [etiqueta] is also the base of the default name of a new template ("Trabajadores 2").
 */
enum class TipoPlantilla(val valor: String, val etiqueta: String) {
    TRABAJADORES("trabajadores", "Trabajadores"),
    ;

    companion object {
        fun desdeValor(valor: String): TipoPlantilla =
            entries.firstOrNull { it.valor == valor }
                ?: throw IllegalArgumentException("Tipo de plantilla desconocido: '$valor'")
    }
}
