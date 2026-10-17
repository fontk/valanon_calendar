package com.valanon.calendar.data.local

import androidx.room.TypeConverter
import com.valanon.calendar.data.model.TipoTurno

/** Stores [TipoTurno] as its schema value ('libre', 'manana', …) in a TEXT column. */
class Conversores {
    @TypeConverter
    fun tipoTurnoAValor(tipo: TipoTurno): String = tipo.valor

    @TypeConverter
    fun valorATipoTurno(valor: String): TipoTurno = TipoTurno.desdeValor(valor)
}
