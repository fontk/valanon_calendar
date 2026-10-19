package com.valanon.calendar.data.repository

import com.valanon.calendar.testing.Tipos
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.AppDatabase
import com.valanon.calendar.data.local.SEMBRAR_TIPOS_TURNO
import com.valanon.calendar.data.local.TransaccionesRoom
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/** The multi-step writes of [PlantillasRepositorio] on real SQLite, inside real transactions. */
@RunWith(AndroidJUnit4::class)
class PlantillasRepositorioTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PlantillasRepositorio
    private lateinit var asignaciones: AsignacionesRepositorio

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(SEMBRAR_TIPOS_TURNO).build()
        repo = PlantillasRepositorio(db.plantillaDao(), db.trabajadorDao(), db.tareaPlantillaDao(), db.asignacionDao(), TransaccionesRoom(db))
        asignaciones = AsignacionesRepositorio(db.asignacionDao(), db.ajusteDao(), db.plantillaDao(), db.trabajadorDao(), db.tiposTurnoDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun eliminarYRestaurarUnTrabajadorConSusTurnos() = runTest {
        val p = repo.crear(TipoPlantilla.TRABAJADORES)
        repo.agregarElemento(TipoPlantilla.TRABAJADORES, p, "Lucía")
        repo.agregarElemento(TipoPlantilla.TRABAJADORES, p, "Javier")
        val (lucia, javier) = repo.elementos(TipoPlantilla.TRABAJADORES, p).first()
        val dia = LocalDate.of(2026, 10, 17)
        asignaciones.asignar(dia, lucia.id, Tipos.NOCHE)
        asignaciones.asignar(dia.plusDays(1), lucia.id, Tipos.LIBRE)
        asignaciones.asignar(dia, javier.id, Tipos.MANANA)

        val borradas = repo.eliminarElemento(TipoPlantilla.TRABAJADORES, lucia.id)
        assertEquals(2, borradas.size)
        assertEquals(1, repo.numeroAsignaciones(p).first())

        repo.restaurarElemento(TipoPlantilla.TRABAJADORES, lucia, borradas)
        assertEquals(listOf("Lucía", "Javier"), repo.elementos(TipoPlantilla.TRABAJADORES, p).first().map { it.texto })
        assertEquals(
            mapOf(
                dia to mapOf(lucia.id to Tipos.NOCHE, javier.id to Tipos.MANANA),
                dia.plusDays(1) to mapOf(lucia.id to Tipos.LIBRE),
            ),
            asignaciones.asignacionesMes(p, YearMonth.of(2026, 10)).first(),
        )
    }

    @Test
    fun crearNumeraLasPlantillas() = runTest {
        repo.crear(TipoPlantilla.TRABAJADORES)
        repo.crear(TipoPlantilla.TRABAJADORES)

        assertEquals(listOf("Trabajadores 1", "Trabajadores 2"), repo.plantillas.first().map { it.plantilla.nombre })
    }
}
