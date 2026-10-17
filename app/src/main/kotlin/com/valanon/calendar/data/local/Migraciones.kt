package com.valanon.calendar.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema v1 → v2: adds the template tables (spec 4b, mis-turnos-kit/db/schema.sql). Only
 * creates tables and the index; `turnos` and `tareas` are untouched. The SQL is exactly what
 * Room exports in app/schemas/.../2.json, so Room's schema validation passes after migrating.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plantillas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`tipo` TEXT NOT NULL, `nombre` TEXT NOT NULL, `orden` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `trabajadores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`plantilla_id` INTEGER NOT NULL, `nombre` TEXT NOT NULL, `orden` INTEGER NOT NULL, " +
                "FOREIGN KEY(`plantilla_id`) REFERENCES `plantillas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_trabajadores_plantilla` ON `trabajadores` (`plantilla_id`, `orden`)",
        )
    }
}

/** Every migration, in order. Registered in [AppDatabase.build] and used by MigrationTest. */
val TODAS_LAS_MIGRACIONES: Array<Migration> = arrayOf(MIGRATION_1_2)
