package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.TareaPlantillaDao
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tasks of a "Tareas" template on real SQLite (same operations as the workers). */
@RunWith(AndroidJUnit4::class)
class TareaPlantillaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TareaPlantillaDao
    private var plantilla = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.tareaPlantillaDao()
        plantilla = db.plantillaDao().insertarAlFinal(TipoPlantilla.TAREAS, "Rutina de apertura")
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun agregarAlFinalNumeraPorPlantilla() = runTest {
        val otra = db.plantillaDao().insertarAlFinal(TipoPlantilla.TAREAS, "Cierre de turno")
        dao.agregarAlFinal(plantilla, "Revisar el cuadrante")
        dao.agregarAlFinal(otra, "Cuadrar la caja")
        dao.agregarAlFinal(plantilla, "Encender equipos")

        val tareas = dao.observarDePlantilla(plantilla).first()
        assertEquals(listOf("Revisar el cuadrante", "Encender equipos"), tareas.map { it.texto })
        assertEquals(listOf(1, 2), tareas.map { it.orden })
        assertEquals(2, db.plantillaDao().observarTodas().first().first { it.plantilla.id == plantilla }.numElementos)
    }

    @Test
    fun cambiarTextoYBorrarConDeshacer() = runTest {
        dao.agregarAlFinal(plantilla, "A")
        val b = dao.agregarAlFinal(plantilla, "B")
        dao.agregarAlFinal(plantilla, "C")
        dao.cambiarTexto(b, "B bis")
        val borrada = dao.observarDePlantilla(plantilla).first().single { it.id == b }

        dao.borrar(b)
        assertEquals(listOf("A", "C"), dao.observarDePlantilla(plantilla).first().map { it.texto })

        dao.insertar(borrada)
        assertEquals(listOf("A", "B bis", "C"), dao.observarDePlantilla(plantilla).first().map { it.texto })
    }

    @Test
    fun borrarLaPlantillaBorraSusTareas() = runTest {
        dao.agregarAlFinal(plantilla, "A")
        db.plantillaDao().borrar(plantilla)
        assertTrue(dao.observarDePlantilla(plantilla).first().isEmpty())
    }
}
