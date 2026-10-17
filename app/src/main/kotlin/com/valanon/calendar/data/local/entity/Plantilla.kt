package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.valanon.calendar.data.model.TipoPlantilla

/** One row of `plantillas` (schema v2): a reusable data set. Lists are ordered by [orden]. */
@Entity(tableName = "plantillas")
data class Plantilla(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: TipoPlantilla,
    val nombre: String,
    val orden: Int,
)

/** A template plus how many workers it has, for the list cards. */
data class PlantillaConRecuento(
    @Embedded val plantilla: Plantilla,
    @ColumnInfo(name = "num_trabajadores") val numTrabajadores: Int,
)
