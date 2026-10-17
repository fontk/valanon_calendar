package com.valanon.calendar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row of `ajustes` (schema v3): app key/value settings, e.g. `turnos.plantilla_id`.
 * Replaces DataStore/SharedPreferences, which stay forbidden (AGENTS.md).
 */
@Entity(tableName = "ajustes")
data class Ajuste(
    @PrimaryKey
    val clave: String,
    val valor: String,
)
