package com.valanon.calendar.data.local

import com.valanon.calendar.testing.Tipos
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.Turno
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
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(SEMBRAR_TIPOS_TURNO).build()
        dao = db.turnoDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsertInsertaYReemplaza() = runTest {
        dao.upsert(Turno("2026-10-17", Tipos.MANANA.id))
        dao.upsert(Turno("2026-10-17", Tipos.NOCHE.id))

        val turnos = dao.observarRango("2026-10-01", "2026-10-31").first()
        assertEquals(listOf(Turno("2026-10-17", Tipos.NOCHE.id)), turnos)
    }

    @Test
    fun rangoIncluyeLosExtremosYExcluyeOtrosMeses() = runTest {
        listOf("2026-09-30", "2026-10-01", "2026-10-15", "2026-10-31", "2026-11-01").forEach {
            dao.upsert(Turno(it, Tipos.TARDE.id))
        }

        val fechas = dao.obtenerRango("2026-10-01", "2026-10-31").map { it.fecha }
        assertEquals(listOf("2026-10-01", "2026-10-15", "2026-10-31"), fechas)
    }

    @Test
    fun borrarQuitaElTurnoDelDia() = runTest {
        dao.upsert(Turno("2026-10-17", Tipos.LIBRE.id))
        dao.borrar("2026-10-17")

        assertTrue(dao.obtenerRango("2026-10-01", "2026-10-31").isEmpty())
    }

    @Test
    fun guardaElTipoConLosValoresDelEsquema() = runTest {
        TIPOS_TURNO_POR_DEFECTO.forEachIndexed { i, tipo -> dao.upsert(Turno("2026-10-0${i + 1}", tipo.id)) }

        // Since schema v5 the row keeps the id of its `tipos_turno` row (ids 1–5 for the defaults).
        val valores = db.openHelper.readableDatabase.query("SELECT tipo_id FROM turnos ORDER BY fecha").use { c ->
            buildList { while (c.moveToNext()) add(c.getLong(0)) }
        }
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), valores)
    }
}
