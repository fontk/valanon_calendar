package com.valanon.calendar.data.model

/**
 * The five possible shifts. [valor] is what is stored in `turnos.tipo`
 * (lowercase, no accents, see mis-turnos-kit/db/schema.sql).
 * The declaration order is the order shown in the legend and the selection sheet.
 */
enum class TipoTurno(
    val valor: String,
    val etiqueta: String,
    val abreviatura: String,
) {
    LIBRE("libre", "Libre", "LIBRE"),
    MANANA("manana", "Mañana", "MAÑ"),
    TARDE("tarde", "Tarde", "TARDE"),
    NOCHE("noche", "Noche", "NOCHE"),
    VACACIONES("vacaciones", "Vacaciones", "VAC"),
    ;

    companion object {
        fun desdeValor(valor: String): TipoTurno =
            entries.firstOrNull { it.valor == valor }
                ?: throw IllegalArgumentException("Tipo de turno desconocido: '$valor'")
    }
}
