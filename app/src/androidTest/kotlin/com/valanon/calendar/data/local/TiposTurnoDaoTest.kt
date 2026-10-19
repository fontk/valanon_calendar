package com.valanon.calendar.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.TiposTurnoDao
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.repository.TiposTurnoRepositorio
import com.valanon.calendar.testing.Tipos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** `tipos_turno` on real SQLite: unique `clave`, usage counts, cascade and "Restablecer". */
@RunWith(AndroidJUnit4::class)
class TiposTurnoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TiposTurnoDao
    private lateinit var repo: TiposTurnoRepositorio

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(SEMBRAR_TIPOS_TURNO).build()
        dao = db.tiposTurnoDao()
        repo = TiposTurnoRepositorio(dao, TransaccionesRoom(db))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun nuevosAlFinalYLaClaveEsUnica() = runTest {
        val id = dao.insertarAlFinal(null, "Guardia", "GUA", "naranja")
        dao.insertarAlFinal(null, "Refuerzo", "REF", "rojo") // NULL claves do not collide

        val tipos = dao.obtenerTodos()
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), tipos.map { it.orden })
        assertEquals(id, tipos[5].id)
        assertNull(tipos[5].clave)
        try {
            dao.insertarAlFinal("noche", "Otra noche", "ON", "gris")
            fail("La clave debe ser única")
        } catch (e: SQLiteConstraintException) {
            // expected
        }
    }

    @Test
    fun usosCuentaDiasYTurnosDelEquipoConUsosEnLaLista() = runTest {
        val p = db.plantillaDao().insertarAlFinal(TipoPlantilla.TRABAJADORES, "Equipo")
        val lucia = db.trabajadorDao().agregarAlFinal(p, "Lucía")
        db.turnoDao().upsert(Turno("2026-10-17", Tipos.NOCHE.id))
        db.turnoDao().upsert(Turno("2026-10-18", Tipos.NOCHE.id))
        db.asignacionDao().upsert(Asignacion(fecha = "2026-10-17", trabajadorId = lucia, tipoId = Tipos.NOCHE.id))

        assertEquals(2 to 1, dao.usos(Tipos.NOCHE.id).let { it.dias to it.equipo })
        val conUsos = dao.observarConUsos().first().associate { it.tipo.clave to (it.usos.dias to it.usos.equipo) }
        assertEquals(2 to 1, conUsos["noche"])
        assertEquals(0 to 0, conUsos["libre"])
    }

    @Test
    fun restablecerRecuperaLosCincoYDejaLosPersonalizados() = runTest {
        dao.actualizar(Tipos.NOCHE.id, "Nocturno", "NOC", "pizarra")
        dao.borrar(Tipos.VACACIONES.id)
        dao.insertarAlFinal(null, "Guardia", "GUA", "naranja")

        repo.restablecer()

        val tipos = dao.obtenerTodos()
        assertEquals(listOf("Libre", "Mañana", "Tarde", "Noche", "Guardia", "Vacaciones"), tipos.map { it.nombre })
        assertEquals(Tipos.NOCHE, tipos.first { it.clave == "noche" })
        assertEquals(listOf("VAC", "verde"), tipos.last().let { listOf(it.abreviatura, it.color) })
        assertEquals("naranja", tipos.first { it.nombre == "Guardia" }.color)
    }
}
