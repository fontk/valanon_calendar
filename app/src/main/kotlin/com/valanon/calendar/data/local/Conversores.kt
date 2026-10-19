package com.valanon.calendar.data.local

import androidx.room.TypeConverter
import com.valanon.calendar.data.model.TipoPlantilla

/**
 * Stores [TipoPlantilla] as its schema value ('trabajadores', 'tareas') in a TEXT column.
 * Shift types are rows of `tipos_turno` since schema v5, so they need no converter.
 */
class Conversores {
    @TypeConverter
    fun tipoPlantillaAValor(tipo: TipoPlantilla): String = tipo.valor

    @TypeConverter
    fun valorATipoPlantilla(valor: String): TipoPlantilla = TipoPlantilla.desdeValor(valor)
}
