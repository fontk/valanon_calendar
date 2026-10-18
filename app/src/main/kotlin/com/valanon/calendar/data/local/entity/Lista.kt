package com.valanon.calendar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of `listas` (schema v4): a checklist of the Checklist tab, ordered by [orden].
 * [plantillaId] is the task template it was copied from, for information only: deleting the
 * template sets it to NULL and leaves the list and its tasks alone.
 */
@Entity(
    tableName = "listas",
    foreignKeys = [
        ForeignKey(
            entity = Plantilla::class,
            parentColumns = ["id"],
            childColumns = ["plantilla_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["plantilla_id"], name = "idx_listas_plantilla")],
)
data class Lista(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    @ColumnInfo(name = "plantilla_id")
    val plantillaId: Long? = null,
    val orden: Int,
)

/** A list plus its progress and the current name of its source template, for the chips and card. */
data class ListaConProgreso(
    @Embedded val lista: Lista,
    /** `null` for a blank list or when the source template was deleted. */
    @ColumnInfo(name = "nombre_plantilla") val nombrePlantilla: String?,
    val total: Int,
    val hechas: Int,
)
