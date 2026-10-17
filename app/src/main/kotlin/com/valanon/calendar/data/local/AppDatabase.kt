package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.valanon.calendar.data.local.dao.TareaDao
import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.Turno

/**
 * Room database (SQLite). Mirrors mis-turnos-kit/db/schema.sql, version 1.
 *
 * When you change an entity:
 *  1. bump [VERSION],
 *  2. add a `Migration` and register it in [build] with `.addMigrations(...)`,
 *  3. build once so Room exports the new schema JSON to `app/schemas/` and commit it.
 */
@Database(
    entities = [Turno::class, Tarea::class],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun turnoDao(): TurnoDao

    abstract fun tareaDao(): TareaDao

    companion object {
        const val VERSION = 1
        const val NAME = "mis_turnos.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .build()
    }
}
