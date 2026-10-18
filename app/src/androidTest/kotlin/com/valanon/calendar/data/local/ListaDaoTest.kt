package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.ListaDao
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checklist lists on real SQLite: progress, cascade into tasks and SET NULL from templates. */
@RunWith(AndroidJUnit4::class)
class ListaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ListaDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.listaDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun observarTodasEnOrdenConProgresoYOrigen() = runTest {
        val p = db.plantillaDao().insertarAlFinal(TipoPlantilla.TAREAS, "Rutina de apertura")
        val a = dao.insertarAlFinal("Rutina de apertura", p)
        val b = dao.insertarAlFinal("Mi checklist", null)
        val vacia = dao.insertarAlFinal("Vacía", null)
        repeat(5) { db.tareaDao().agregarAlFinal(a, "T$it") }
        db.tareaDao().observarDeLista(a).first().take(2).forEach { db.tareaDao().actualizarHecha(it.id, true) }
        db.tareaDao().agregarAlFinal(b, "Uno")

        val listas = dao.observarTodas().first()

        assertEquals(listOf(a, b, vacia), listas.map { it.lista.id })
        assertEquals(listOf(1, 2, 3), listas.map { it.lista.orden })
        assertEquals(listOf(5 to 2, 1 to 0, 0 to 0), listas.map { it.total to it.hechas })
        assertEquals(listOf("Rutina de apertura", null, null), listas.map { it.nombrePlantilla })
        assertEquals(listOf(a, b, vacia), dao.ids())
    }

    @Test
    fun renombrar() = runTest {
        val id = dao.insertarAlFinal("Lista 1", null)
        dao.renombrar(id, "Compras")
        assertEquals("Compras", dao.observarTodas().first().single().lista.nombre)
    }

    @Test
    fun borrarLaListaBorraSusTareasYNoLasDeOtras() = runTest {
        val a = dao.insertarAlFinal("A", null)
        val b = dao.insertarAlFinal("B", null)
        db.tareaDao().agregarAlFinal(a, "De A")
        db.tareaDao().agregarAlFinal(b, "De B")

        dao.borrar(a)

        assertEquals(listOf(b), dao.ids())
        assertTrue(db.tareaDao().observarDeLista(a).first().isEmpty())
        assertEquals("De B", db.tareaDao().observarDeLista(b).first().single().texto)
    }

    @Test
    fun borrarLaPlantillaDeOrigenDejaLaListaYSusTareas() = runTest {
        val p = db.plantillaDao().insertarAlFinal(TipoPlantilla.TAREAS, "Cierre de turno")
        db.tareaPlantillaDao().agregarAlFinal(p, "Cuadrar la caja")
        val id = dao.insertarAlFinal("Cierre de turno", p)
        db.tareaDao().copiarDePlantilla(id, p)

        db.plantillaDao().borrar(p)

        val lista = dao.observarTodas().first().single()
        assertNull(lista.lista.plantillaId)
        assertNull(lista.nombrePlantilla)
        assertEquals(1, lista.total)
        assertTrue(db.tareaPlantillaDao().observarDePlantilla(p).first().isEmpty())
    }
}
