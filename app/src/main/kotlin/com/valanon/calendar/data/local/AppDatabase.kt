package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.valanon.calendar.data.local.dao.AjusteDao
import com.valanon.calendar.data.local.dao.AsignacionDao
import com.valanon.calendar.data.local.dao.CopiaSeguridadDao
import com.valanon.calendar.data.local.dao.PlantillaDao
import com.valanon.calendar.data.local.dao.TareaDao
import com.valanon.calendar.data.local.dao.TrabajadorDao
import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.local.entity.Turno

/**
 * Room database (SQLite). Mirrors mis-turnos-kit/db/schema.sql.
 *
 * - v1: `turnos`, `tareas`.
 * - v2: + `plantillas`, `trabajadores` ([MIGRATION_1_2]).
 * - v3: + `asignaciones`, `ajustes` ([MIGRATION_2_3]).
 *
 * When you change an entity:
 *  1. bump [VERSION],
 *  2. add a `Migration` in Migraciones.kt and append it to [TODAS_LAS_MIGRACIONES],
 *  3. build once so Room exports the new schema JSON to `app/schemas/` and commit it,
 *  4. extend MigrationTest.
 */
@Database(
    entities = [Turno::class, Tarea::class, Plantilla::class, Trabajador::class, Asignacion::class, Ajuste::class],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun turnoDao(): TurnoDao

    abstract fun tareaDao(): TareaDao

    abstract fun plantillaDao(): PlantillaDao

    abstract fun trabajadorDao(): TrabajadorDao

    abstract fun asignacionDao(): AsignacionDao

    abstract fun ajusteDao(): AjusteDao

    abstract fun copiaSeguridadDao(): CopiaSeguridadDao

    companion object {
        const val VERSION = 3
        const val NAME = "mis_turnos.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(*TODAS_LAS_MIGRACIONES)
                .build()
    }
}
