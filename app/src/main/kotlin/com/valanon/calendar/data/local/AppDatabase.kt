package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.valanon.calendar.data.local.dao.EventDao
import com.valanon.calendar.data.local.entity.EventEntity

/**
 * Room database (SQLite) for the app.
 *
 * When you change an entity:
 *  1. bump [VERSION] (the `version` below),
 *  2. add a `Migration` and register it in [build] with `.addMigrations(...)`,
 *  3. build once so Room exports the new schema JSON to `app/schemas/` and commit it.
 */
@Database(
    entities = [EventEntity::class],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        const val VERSION = 1
        const val NAME = "valanon_calendar.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .build()
    }
}
