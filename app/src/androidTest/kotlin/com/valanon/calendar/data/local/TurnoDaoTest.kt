package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.TipoTurno
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** C4: insert, replace and range query against a real in-memory SQLite database. */
@RunWith(AndroidJUnit4::class)
class TurnoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TurnoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.turnoDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsertInsertaYReemplaza() = runTest {
        dao.upsert(Turno("2026-10-17", TipoTurno.MANANA))
        dao.upsert(Turno("2026-10-17", TipoTurno.NOCHE))

        val turnos = dao.observarRango("2026-10-01", "2026-10-31").first()
        assertEquals(listOf(Turno("2026-10-17", TipoTurno.NOCHE)), turnos)
    }

    @Test
    fun rangoIncluyeLosExtremosYExcluyeOtrosMeses() = runTest {
        listOf("2026-09-30", "2026-10-01", "2026-10-15", "2026-10-31", "2026-11-01").forEach {
            dao.upsert(Turno(it, TipoTurno.TARDE))
        }

        val fechas = dao.obtenerRango("2026-10-01", "2026-10-31").map { it.fecha }
        assertEquals(listOf("2026-10-01", "2026-10-15", "2026-10-31"), fechas)
    }

    @Test
    fun borrarQuitaElTurnoDelDia() = runTest {
        dao.upsert(Turno("2026-10-17", TipoTurno.LIBRE))
        dao.borrar("2026-10-17")

        assertTrue(dao.obtenerRango("2026-10-01", "2026-10-31").isEmpty())
    }

    @Test
    fun guardaElTipoConLosValoresDelEsquema() = runTest {
        TipoTurno.entries.forEachIndexed { i, tipo -> dao.upsert(Turno("2026-10-0${i + 1}", tipo)) }

        val valores = db.openHelper.readableDatabase.query("SELECT tipo FROM turnos ORDER BY fecha").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        assertEquals(listOf("libre", "manana", "tarde", "noche", "vacaciones"), valores)
    }
}
