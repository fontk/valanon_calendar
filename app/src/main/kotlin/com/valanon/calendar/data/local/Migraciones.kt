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

/** Name of the list that receives the tasks of v1–v3, which had a single checklist. */
const val NOMBRE_LISTA_MIGRADA = "Mi checklist"

/** `ajustes` key of the active Checklist list (spec 4.0). */
const val CLAVE_LISTA_CHECKLIST = "checklist.lista_id"

/**
 * Schema v3 → v4: several checklists and task templates (spec 4, 4b). Creates `listas` and
 * `tareas_plantilla`; if there were tasks, puts them in a new list "Mi checklist" and makes it the
 * active one. SQLite cannot add a NOT NULL foreign key column, so `tareas` is rebuilt (new table,
 * copy, drop, rename) keeping ids, text, `hecha` and `orden`. The SQL matches app/schemas/.../4.json.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `listas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`nombre` TEXT NOT NULL, `plantilla_id` INTEGER, `orden` INTEGER NOT NULL, " +
                "FOREIGN KEY(`plantilla_id`) REFERENCES `plantillas`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_listas_plantilla` ON `listas` (`plantilla_id`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tareas_plantilla` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`plantilla_id` INTEGER NOT NULL, `texto` TEXT NOT NULL, `orden` INTEGER NOT NULL, " +
                "FOREIGN KEY(`plantilla_id`) REFERENCES `plantillas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_tareas_plantilla` ON `tareas_plantilla` (`plantilla_id`, `orden`)",
        )

        db.execSQL(
            "INSERT INTO `listas` (`nombre`, `plantilla_id`, `orden`) " +
                "SELECT '$NOMBRE_LISTA_MIGRADA', NULL, 1 WHERE EXISTS (SELECT 1 FROM `tareas`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tareas_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`lista_id` INTEGER NOT NULL, `texto` TEXT NOT NULL, `hecha` INTEGER NOT NULL DEFAULT 0, " +
                "`orden` INTEGER NOT NULL, " +
                "FOREIGN KEY(`lista_id`) REFERENCES `listas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT INTO `tareas_nueva` (`id`, `lista_id`, `texto`, `hecha`, `orden`) " +
                "SELECT `id`, (SELECT `id` FROM `listas` ORDER BY `id` LIMIT 1), `texto`, `hecha`, `orden` FROM `tareas`",
        )
        db.execSQL("DROP TABLE `tareas`")
        db.execSQL("ALTER TABLE `tareas_nueva` RENAME TO `tareas`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_tareas_lista` ON `tareas` (`lista_id`, `orden`)")

        db.execSQL(
            "INSERT OR REPLACE INTO `ajustes` (`clave`, `valor`) " +
                "SELECT '$CLAVE_LISTA_CHECKLIST', CAST(`id` AS TEXT) FROM `listas` ORDER BY `id` LIMIT 1",
        )
    }
}

/** Every migration, in order. Registered in [AppDatabase.build] and used by MigrationTest. */
val TODAS_LAS_MIGRACIONES: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
