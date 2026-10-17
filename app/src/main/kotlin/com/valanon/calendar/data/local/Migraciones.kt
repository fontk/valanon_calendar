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

/**
 * Schema v2 → v3: team shifts (spec 3b). Creates `asignaciones` (unique per day and worker,
 * cascade from `trabajadores`) and the `ajustes` key/value table. Existing data is untouched.
 * Kept separate from [MIGRATION_1_2] because v2 already shipped (APK 0.4.0).
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `asignaciones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`fecha` TEXT NOT NULL, `trabajador_id` INTEGER NOT NULL, `tipo` TEXT NOT NULL, " +
                "FOREIGN KEY(`trabajador_id`) REFERENCES `trabajadores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `idx_asignaciones_fecha_trabajador` ON `asignaciones` (`fecha`, `trabajador_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_asignaciones_trabajador` ON `asignaciones` (`trabajador_id`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ajustes` (`clave` TEXT NOT NULL, `valor` TEXT NOT NULL, PRIMARY KEY(`clave`))",
        )
    }
}

/** Every migration, in order. Registered in [AppDatabase.build] and used by MigrationTest. */
val TODAS_LAS_MIGRACIONES: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
