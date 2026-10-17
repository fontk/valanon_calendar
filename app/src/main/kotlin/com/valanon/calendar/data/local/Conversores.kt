package com.valanon.calendar.data.local

import androidx.room.TypeConverter
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.TipoTurno

/** Stores the enums as their schema values ('libre', 'trabajadores', …) in TEXT columns. */
class Conversores {
    @TypeConverter
    fun tipoTurnoAValor(tipo: TipoTurno): String = tipo.valor

    @TypeConverter
    fun valorATipoTurno(valor: String): TipoTurno = TipoTurno.desdeValor(valor)

    @TypeConverter
    fun tipoPlantillaAValor(tipo: TipoPlantilla): String = tipo.valor

    @TypeConverter
    fun valorATipoPlantilla(valor: String): TipoPlantilla = TipoPlantilla.desdeValor(valor)
}
