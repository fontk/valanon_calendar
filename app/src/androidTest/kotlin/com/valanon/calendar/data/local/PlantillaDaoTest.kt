package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.PlantillaDao
import com.valanon.calendar.data.local.dao.TrabajadorDao
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantillaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PlantillaDao
    private lateinit var trabajadores: TrabajadorDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(SEMBRAR_TIPOS_TURNO).build()
        dao = db.plantillaDao()
        trabajadores = db.trabajadorDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertarAlFinalNumeraElOrdenYGuardaElTipo() = runTest {
        dao.insertarAlFinal(TipoPlantilla.TRABAJADORES, "A")
        dao.insertarAlFinal(TipoPlantilla.TRABAJADORES, "B")

        val lista = dao.observarTodas().first()
        assertEquals(listOf("A", "B"), lista.map { it.plantilla.nombre })
        assertEquals(listOf(1, 2), lista.map { it.plantilla.orden })
        assertEquals(2, dao.contar())

        val tipo = db.openHelper.readableDatabase.query("SELECT tipo FROM plantillas LIMIT 1").use {
            it.moveToFirst()
            it.getString(0)
        }
        assertEquals("trabajadores", tipo)
    }

    @Test
    fun observarTodasCuentaLosTrabajadoresIncluidasLasVacias() = runTest {
        val a = dao.insertarAlFinal(TipoPlantilla.TRABAJADORES, "Con gente")
        dao.insertarAlFinal(TipoPlantilla.TRABAJADORES, "Vacía")
        repeat(3) { trabajadores.agregarAlFinal(a, "T$it") }

        val recuentos = dao.observarTodas().first().associate { it.plantilla.nombre to it.numElementos }
        assertEquals(mapOf("Con gente" to 3, "Vacía" to 0), recuentos)
    }

    @Test
    fun renombrarYObservar() = runTest {
        val id = dao.insertarAlFinal(TipoPlantilla.TRABAJADORES, "Trabajadores 1")
        dao.renombrar(id, "Equipo de planta")

        assertEquals("Equipo de planta", dao.observar(id).first()?.nombre)
    }

    @Test
    fun borrarEmiteNullYBorraSusTrabajadoresEnCascada() = runTest {
        val id = dao.insertarAlFinal(TipoPlantilla.TRABAJADORES, "X")
        val otra = dao.insertarAlFinal(TipoPlantilla.TRABAJADORES, "Y")
        trabajadores.agregarAlFinal(id, "Uno")
        trabajadores.agregarAlFinal(id, "Dos")
        trabajadores.agregarAlFinal(otra, "Otro")

        dao.borrar(id)

        assertNull(dao.observar(id).first())
        assertEquals(emptyList<Any>(), trabajadores.observarDePlantilla(id).first())
        assertEquals(listOf("Otro"), trabajadores.observarDePlantilla(otra).first().map { it.nombre })
    }
}
