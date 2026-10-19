package com.valanon.calendar.data.repository

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** [ListasRepositorio] on real SQLite: copying a preset inside a transaction and the active list. */
@RunWith(AndroidJUnit4::class)
class ListasRepositorioTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ListasRepositorio
    private lateinit var plantillas: PlantillasRepositorio

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(SEMBRAR_TIPOS_TURNO).build()
        val transacciones = TransaccionesRoom(db)
        repo = ListasRepositorio(db.listaDao(), db.tareaDao(), db.plantillaDao(), db.ajusteDao(), transacciones)
        plantillas = PlantillasRepositorio(
            db.plantillaDao(), db.trabajadorDao(), db.tareaPlantillaDao(), db.asignacionDao(), transacciones,
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun crearDesdePlantillaCopiaLasTareasYLaActiva() = runTest {
        val p = plantillas.crear(TipoPlantilla.TAREAS)
        plantillas.renombrarPlantilla(p, "Rutina de apertura")
        listOf("Revisar el cuadrante", "Comprobar la caja", "Encender equipos")
            .forEach { plantillas.agregarElemento(TipoPlantilla.TAREAS, p, it) }
        repo.crearEnBlanco { "Lista $it" }

        val id = repo.crearDesdePlantilla(p)!!

        val listas = repo.listas.first()
        assertEquals(id, listas.activa?.lista?.id)
        assertEquals("Rutina de apertura", listas.activa?.lista?.nombre)
        assertEquals("Rutina de apertura", listas.activa?.nombrePlantilla)
        val tareas = db.tareaDao().observarDeLista(id).first()
        assertEquals(listOf("Revisar el cuadrante", "Comprobar la caja", "Encender equipos"), tareas.map { it.texto })
        assertEquals(listOf(1, 2, 3), tareas.map { it.orden })
        assertTrue(tareas.none { it.hecha })
    }

    @Test
    fun crearDesdeUnaPlantillaDeTrabajadoresNoHaceNada() = runTest {
        val p = plantillas.crear(TipoPlantilla.TRABAJADORES)

        assertNull(repo.crearDesdePlantilla(p))
        assertTrue(repo.listas.first().todas.isEmpty())
    }

    @Test
    fun laActivaSeResuelveYBorrarActivaLaVecina() = runTest {
        val a = repo.crearEnBlanco { "Lista $it" }
        val b = repo.crearEnBlanco { "Lista $it" }
        val c = repo.crearEnBlanco { "Lista $it" }
        assertEquals(listOf("Lista 1", "Lista 2", "Lista 3"), repo.listas.first().todas.map { it.lista.nombre })
        assertEquals(c, repo.listaActiva.first()?.id)

        repo.activar(b)
        repo.borrar(b)
        assertEquals(c, repo.listaActiva.first()?.id)

        repo.borrar(c)
        assertEquals(a, repo.listaActiva.first()?.id)

        // Deleting a list that is not the active one keeps the active one.
        val d = repo.crearEnBlanco { "Lista $it" }
        repo.activar(a)
        repo.borrar(d)
        assertEquals(a, repo.listaActiva.first()?.id)

        repo.borrar(a)
        assertNull(repo.listaActiva.first())
    }
}
