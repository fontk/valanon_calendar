package com.valanon.calendar.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.TrabajadorDao
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrabajadorDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TrabajadorDao
    private var plantilla = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.trabajadorDao()
        plantilla = db.plantillaDao().insertarAlFinal(TipoPlantilla.TRABAJADORES, "Equipo")
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun agregarAlFinalNumeraPorPlantilla() = runTest {
        val otra = db.plantillaDao().insertarAlFinal(TipoPlantilla.TRABAJADORES, "Otra")
        dao.agregarAlFinal(plantilla, "Lucía Martínez")
        dao.agregarAlFinal(otra, "Sara López")
        dao.agregarAlFinal(plantilla, "Javier Ortega")

        val lista = dao.observarDePlantilla(plantilla).first()
        assertEquals(listOf("Lucía Martínez", "Javier Ortega"), lista.map { it.nombre })
        assertEquals(listOf(1, 2), lista.map { it.orden })
        assertEquals(listOf(1), dao.observarDePlantilla(otra).first().map { it.orden })
    }

    @Test
    fun renombrar() = runTest {
        val id = dao.agregarAlFinal(plantilla, "Marcos")
        dao.renombrar(id, "Marcos Pérez")

        assertEquals("Marcos Pérez", dao.observarDePlantilla(plantilla).first().single().nombre)
    }

    @Test
    fun borrarEInsertarRestauraEnSuSitio() = runTest {
        dao.agregarAlFinal(plantilla, "A")
        val b = dao.agregarAlFinal(plantilla, "B")
        dao.agregarAlFinal(plantilla, "C")
        val borrado = dao.observarDePlantilla(plantilla).first().single { it.id == b }

        dao.borrar(b)
        assertEquals(listOf("A", "C"), dao.observarDePlantilla(plantilla).first().map { it.nombre })

        dao.insertar(borrado)
        assertEquals(listOf("A", "B", "C"), dao.observarDePlantilla(plantilla).first().map { it.nombre })
    }

    @Test
    fun noSePuedeAnadirAUnaPlantillaInexistente() = runTest {
        val error = runCatching { dao.insertar(Trabajador(plantillaId = 999, nombre = "Nadie", orden = 1)) }
        assertTrue(error.exceptionOrNull() is SQLiteConstraintException)
    }
}
