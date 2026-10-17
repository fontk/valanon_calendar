package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.AsignacionDao
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.TipoTurno
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Team shifts on real SQLite: unique per day and worker, filters and cascades (spec 3b.5). */
@RunWith(AndroidJUnit4::class)
class AsignacionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AsignacionDao
    private var equipo = 0L
    private var otra = 0L
    private var lucia = 0L
    private var javier = 0L
    private var sara = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.asignacionDao()
        equipo = db.plantillaDao().insertarAlFinal(TipoPlantilla.TRABAJADORES, "Equipo de planta")
        otra = db.plantillaDao().insertarAlFinal(TipoPlantilla.TRABAJADORES, "Refuerzos")
        lucia = db.trabajadorDao().agregarAlFinal(equipo, "Lucía")
        javier = db.trabajadorDao().agregarAlFinal(equipo, "Javier")
        sara = db.trabajadorDao().agregarAlFinal(otra, "Sara")
    }

    @After
    fun tearDown() = db.close()

    private fun asignacion(fecha: String, trabajador: Long, tipo: TipoTurno) =
        Asignacion(fecha = fecha, trabajadorId = trabajador, tipo = tipo)

    private suspend fun mes(plantilla: Long) = dao.observarMes(plantilla, "2026-10-01", "2026-10-31").first()

    @Test
    fun upsertEsUnicoPorDiaYTrabajador() = runTest {
        dao.upsert(asignacion("2026-10-17", lucia, TipoTurno.MANANA))
        dao.upsert(asignacion("2026-10-17", lucia, TipoTurno.NOCHE))

        assertEquals(listOf(TipoTurno.NOCHE), mes(equipo).map { it.tipo })
    }

    @Test
    fun observarMesFiltraPorPlantillaYRango() = runTest {
        dao.upsert(asignacion("2026-10-01", lucia, TipoTurno.LIBRE))
        dao.upsert(asignacion("2026-10-31", javier, TipoTurno.TARDE))
        dao.upsert(asignacion("2026-11-01", javier, TipoTurno.TARDE)) // other month
        dao.upsert(asignacion("2026-10-15", sara, TipoTurno.NOCHE)) // other template

        assertEquals(setOf("2026-10-01", "2026-10-31"), mes(equipo).map { it.fecha }.toSet())
        assertEquals(listOf(sara), mes(otra).map { it.trabajadorId })
    }

    @Test
    fun borrarQuitaSoloEseDiaYTrabajador() = runTest {
        dao.upsert(asignacion("2026-10-17", lucia, TipoTurno.MANANA))
        dao.upsert(asignacion("2026-10-17", javier, TipoTurno.MANANA))

        dao.borrar("2026-10-17", lucia)

        assertEquals(listOf(javier), mes(equipo).map { it.trabajadorId })
    }

    @Test
    fun borrarTrabajadorBorraSusAsignacionesYSePuedenReinsertar() = runTest {
        dao.upsert(asignacion("2026-10-17", lucia, TipoTurno.NOCHE))
        dao.upsert(asignacion("2026-10-18", lucia, TipoTurno.NOCHE))
        dao.upsert(asignacion("2026-10-17", javier, TipoTurno.LIBRE))
        val deLucia = dao.obtenerDeTrabajador(lucia)
        val trabajadorLucia = db.trabajadorDao().observarDePlantilla(equipo).first().first { it.id == lucia }

        db.trabajadorDao().borrar(lucia)
        assertEquals(1, dao.contarDePlantilla(equipo))

        // Undo: worker first (FK), then its shifts.
        db.trabajadorDao().insertar(trabajadorLucia)
        dao.insertarTodas(deLucia)
        assertEquals(3, dao.observarContarDePlantilla(equipo).first())
    }

    @Test
    fun borrarPlantillaBorraTrabajadoresYAsignaciones() = runTest {
        dao.upsert(asignacion("2026-10-17", lucia, TipoTurno.NOCHE))
        dao.upsert(asignacion("2026-10-17", sara, TipoTurno.NOCHE))

        db.plantillaDao().borrar(equipo)

        assertTrue(mes(equipo).isEmpty())
        assertEquals(0, dao.contarDePlantilla(equipo))
        assertEquals(1, dao.contarDePlantilla(otra))
    }

    @Test
    fun ajustesGuardaYReemplaza() = runTest {
        val ajustes = db.ajusteDao()
        assertNull(ajustes.observar("turnos.plantilla_id").first())

        ajustes.guardar(Ajuste("turnos.plantilla_id", "$equipo"))
        ajustes.guardar(Ajuste("turnos.plantilla_id", "$otra"))

        assertEquals("$otra", ajustes.observar("turnos.plantilla_id").first())
    }
}
