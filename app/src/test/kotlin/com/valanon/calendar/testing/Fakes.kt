package com.valanon.calendar.testing

import com.valanon.calendar.data.local.dao.TareaDao
import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.Turno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** In-memory [TurnoDao] with the same semantics as the SQL (BETWEEN inclusive, INSERT OR REPLACE). */
class FakeTurnoDao : TurnoDao {
    val filas = MutableStateFlow<Map<String, Turno>>(emptyMap())

    override fun observarRango(desde: String, hasta: String): Flow<List<Turno>> =
        filas.map { m -> m.values.filter { it.fecha in desde..hasta }.sortedBy { it.fecha } }

    override suspend fun obtenerRango(desde: String, hasta: String): List<Turno> =
        filas.value.values.filter { it.fecha in desde..hasta }.sortedBy { it.fecha }

    override suspend fun upsert(turno: Turno) = filas.update { it + (turno.fecha to turno) }

    override suspend fun borrar(fecha: String) = filas.update { it - fecha }
}

/** In-memory [TareaDao] mirroring the SQL in the real DAO. */
class FakeTareaDao : TareaDao {
    val filas = MutableStateFlow<List<Tarea>>(emptyList())
    private var siguienteId = 1L

    override fun observarTodas(): Flow<List<Tarea>> =
        filas.map { l -> l.sortedWith(compareBy({ it.orden }, { it.id })) }

    override suspend fun insertar(tarea: Tarea): Long {
        val id = if (tarea.id == 0L) siguienteId++ else tarea.id
        siguienteId = maxOf(siguienteId, id + 1)
        filas.update { it + tarea.copy(id = id) }
        return id
    }

    override suspend fun agregarAlFinal(texto: String): Long =
        insertar(Tarea(texto = texto, hecha = false, orden = (maxOrden() ?: 0) + 1))

    override suspend fun actualizarHecha(id: Long, hecha: Boolean) =
        filas.update { l -> l.map { if (it.id == id) it.copy(hecha = hecha) else it } }

    override suspend fun borrar(id: Long) = filas.update { l -> l.filterNot { it.id == id } }

    override suspend fun reiniciarTodas() = filas.update { l -> l.map { it.copy(hecha = false) } }

    override suspend fun maxOrden(): Int? = filas.value.maxOfOrNull { it.orden }
}

/** Replaces Dispatchers.Main (used by viewModelScope) with a test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
