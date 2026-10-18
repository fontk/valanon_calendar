package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.TareaDao
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TareaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TareaDao
    private var lista = 0L
    private var otra = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.tareaDao()
        lista = db.listaDao().insertarAlFinal("Mi checklist", null)
        otra = db.listaDao().insertarAlFinal("Otra", null)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun agregarAlFinalNumeraElOrdenPorListaYEmpiezaSinMarcar() = runTest {
        dao.agregarAlFinal(lista, "A")
        dao.agregarAlFinal(otra, "X")
        dao.agregarAlFinal(lista, "B")
        dao.agregarAlFinal(lista, "C")

        val tareas = dao.observarDeLista(lista).first()
        assertEquals(listOf("A", "B", "C"), tareas.map { it.texto })
        assertEquals(listOf(1, 2, 3), tareas.map { it.orden })
        assertTrue(tareas.none { it.hecha })
        assertEquals(listOf(1), dao.observarDeLista(otra).first().map { it.orden })
    }

    @Test
    fun trasBorrarLaUltimaLaSiguienteVaAlFinal() = runTest {
        dao.agregarAlFinal(lista, "A")
        val b = dao.agregarAlFinal(lista, "B")
        dao.borrar(b)
        dao.agregarAlFinal(lista, "C")

        assertEquals(listOf("A", "C"), dao.observarDeLista(lista).first().map { it.texto })
    }

    @Test
    fun reiniciarListaSoloDesmarcaEsaLista() = runTest {
        val a = dao.agregarAlFinal(lista, "A")
        val b = dao.agregarAlFinal(lista, "B")
        val x = dao.agregarAlFinal(otra, "X")
        listOf(a, b, x).forEach { dao.actualizarHecha(it, true) }

        dao.reiniciarLista(lista)

        val tareas = dao.observarDeLista(lista).first()
        assertEquals(2, tareas.size)
        assertFalse(tareas.any { it.hecha })
        assertTrue(dao.observarDeLista(otra).first().single().hecha)
    }

    @Test
    fun restaurarDevuelveUnaTareaBorradaASuSitio() = runTest {
        dao.agregarAlFinal(lista, "A")
        val b = dao.agregarAlFinal(lista, "B")
        dao.agregarAlFinal(lista, "C")
        dao.actualizarHecha(b, true)
        val borrada = dao.observarDeLista(lista).first().single { it.id == b }

        dao.borrar(b)
        dao.restaurar(borrada.id, borrada.listaId, borrada.texto, borrada.hecha, borrada.orden)

        val tareas = dao.observarDeLista(lista).first()
        assertEquals(listOf("A", "B", "C"), tareas.map { it.texto })
        assertEquals(borrada, tareas[1])
    }

    @Test
    fun restaurarNoHaceNadaSiLaListaYaNoExiste() = runTest {
        val a = dao.agregarAlFinal(lista, "A")
        val tarea = dao.observarDeLista(lista).first().single { it.id == a }
        db.listaDao().borrar(lista)

        // No foreign-key failure: the undo is simply dropped.
        dao.restaurar(tarea.id, tarea.listaId, tarea.texto, tarea.hecha, tarea.orden)

        assertTrue(dao.observarDeLista(lista).first().isEmpty())
    }

    @Test
    fun copiarDePlantillaCopiaTextosYOrdenSinMarcar() = runTest {
        val p = db.plantillaDao().insertarAlFinal(TipoPlantilla.TAREAS, "Cierre de turno")
        db.tareaPlantillaDao().agregarAlFinal(p, "Cuadrar la caja")
        db.tareaPlantillaDao().agregarAlFinal(p, "Apagar equipos")

        dao.copiarDePlantilla(otra, p)

        val tareas = dao.observarDeLista(otra).first()
        assertEquals(listOf("Cuadrar la caja", "Apagar equipos"), tareas.map { it.texto })
        assertEquals(listOf(1, 2), tareas.map { it.orden })
        assertTrue(tareas.none { it.hecha })
    }
}
