package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.TareaDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TareaDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TareaDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.tareaDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun agregarAlFinalNumeraElOrdenYEmpiezaSinMarcar() = runTest {
        assertNull(dao.maxOrden())
        dao.agregarAlFinal("A")
        dao.agregarAlFinal("B")
        dao.agregarAlFinal("C")

        val tareas = dao.observarTodas().first()
        assertEquals(listOf("A", "B", "C"), tareas.map { it.texto })
        assertEquals(listOf(1, 2, 3), tareas.map { it.orden })
        assertTrue(tareas.none { it.hecha })
        assertEquals(3, dao.maxOrden())
    }

    @Test
    fun trasBorrarLaUltimaLaSiguienteVaAlFinal() = runTest {
        dao.agregarAlFinal("A")
        val b = dao.agregarAlFinal("B")
        dao.borrar(b)
        dao.agregarAlFinal("C")

        assertEquals(listOf("A", "C"), dao.observarTodas().first().map { it.texto })
    }

    @Test
    fun actualizarHechaYReiniciarTodas() = runTest {
        val a = dao.agregarAlFinal("A")
        val b = dao.agregarAlFinal("B")
        dao.actualizarHecha(a, true)
        dao.actualizarHecha(b, true)
        assertTrue(dao.observarTodas().first().all { it.hecha })

        dao.actualizarHecha(b, false)
        assertEquals(listOf(true, false), dao.observarTodas().first().map { it.hecha })

        dao.reiniciarTodas()
        val tareas = dao.observarTodas().first()
        assertEquals(2, tareas.size)
        assertFalse(tareas.any { it.hecha })
    }

    @Test
    fun insertarRestauraUnaTareaBorradaEnSuSitio() = runTest {
        dao.agregarAlFinal("A")
        val b = dao.agregarAlFinal("B")
        dao.agregarAlFinal("C")
        dao.actualizarHecha(b, true)
        val borrada = dao.observarTodas().first().single { it.id == b }

        dao.borrar(b)
        dao.insertar(borrada)

        val tareas = dao.observarTodas().first()
        assertEquals(listOf("A", "B", "C"), tareas.map { it.texto })
        assertEquals(borrada, tareas[1])
    }
}
