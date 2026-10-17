package com.valanon.calendar.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.TipoTurno
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P9: upgrading from schema v1 keeps shifts and tasks, and adds empty template tables whose
 * structure matches what Room expects (validated against app/schemas/.../2.json).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val nombreDb = "migracion-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migra1a2ConservandoTurnosYTareas() = runTest {
        helper.createDatabase(nombreDb, 1).use { db ->
            db.execSQL("INSERT INTO turnos (fecha, tipo) VALUES ('2026-10-17', 'noche')")
            db.execSQL("INSERT INTO tareas (texto, hecha, orden) VALUES ('Preparar el uniforme', 1, 1)")
        }

        // Runs MIGRATION_1_2 and validates the resulting schema against 2.json.
        helper.runMigrationsAndValidate(nombreDb, 2, true, MIGRATION_1_2).close()

        // Open it the way the app does and read through the DAOs.
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val app = Room.databaseBuilder(contexto, AppDatabase::class.java, nombreDb)
            .addMigrations(*TODAS_LAS_MIGRACIONES)
            .build()
        try {
            val turnos = app.turnoDao().obtenerRango("2026-10-01", "2026-10-31")
            assertEquals(TipoTurno.NOCHE, turnos.single().tipo)

            val tarea = app.tareaDao().observarTodas().first().single()
            assertEquals("Preparar el uniforme", tarea.texto)
            assertTrue(tarea.hecha)

            assertTrue(app.plantillaDao().observarTodas().first().isEmpty())

            // The new tables work, including the cascade.
            val id = app.plantillaDao().insertarAlFinal(TipoPlantilla.TRABAJADORES, "Equipo")
            app.trabajadorDao().agregarAlFinal(id, "Lucía")
            app.plantillaDao().borrar(id)
            assertTrue(app.trabajadorDao().observarDePlantilla(id).first().isEmpty())
        } finally {
            app.close()
            contexto.deleteDatabase(nombreDb)
        }
    }
}
