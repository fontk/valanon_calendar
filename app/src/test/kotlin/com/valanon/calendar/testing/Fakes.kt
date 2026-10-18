package com.valanon.calendar.testing

import com.valanon.calendar.data.local.SinTransaccion
import com.valanon.calendar.data.local.dao.AjusteDao
import com.valanon.calendar.data.repository.PlantillasRepositorio
import com.valanon.calendar.data.local.dao.AsignacionDao
import com.valanon.calendar.data.local.dao.PlantillaDao
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.dao.TareaDao
import com.valanon.calendar.data.local.dao.TrabajadorDao
import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.combine
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

/** In-memory [TrabajadorDao]; [FakePlantillaDao.borrar] cascades into it like the real FK. */
class FakeTrabajadorDao : TrabajadorDao {
    val filas = MutableStateFlow<List<Trabajador>>(emptyList())
    private var siguienteId = 1L

    override fun observarDePlantilla(plantillaId: Long): Flow<List<Trabajador>> =
        filas.map { l -> l.filter { it.plantillaId == plantillaId }.sortedWith(compareBy({ it.orden }, { it.id })) }

    override suspend fun agregarAlFinal(plantillaId: Long, nombre: String): Long {
        val orden = (filas.value.filter { it.plantillaId == plantillaId }.maxOfOrNull { it.orden } ?: 0) + 1
        return insertar(Trabajador(plantillaId = plantillaId, nombre = nombre, orden = orden))
    }

    override suspend fun renombrar(id: Long, nombre: String) =
        filas.update { l -> l.map { if (it.id == id) it.copy(nombre = nombre) else it } }

    /** Cascade hook (FakeAsignacionDao deletes the worker's assignments). */
    var alBorrar: (Long) -> Unit = {}

    override suspend fun borrar(id: Long) {
        filas.update { l -> l.filterNot { it.id == id } }
        alBorrar(id)
    }

    override suspend fun insertar(trabajador: Trabajador): Long {
        // Same as SQLite: an explicit id that already exists is a primary-key conflict.
        check(filas.value.none { it.id == trabajador.id }) { "PK duplicada en trabajadores: ${trabajador.id}" }
        val id = if (trabajador.id == 0L) siguienteId++ else trabajador.id
        siguienteId = maxOf(siguienteId, id + 1)
        filas.update { it + trabajador.copy(id = id) }
        return id
    }
}

/** In-memory [PlantillaDao] with the JOIN count and the ON DELETE CASCADE of the real schema. */
class FakePlantillaDao(private val trabajadores: FakeTrabajadorDao) : PlantillaDao {
    val filas = MutableStateFlow<List<Plantilla>>(emptyList())
    private var siguienteId = 1L

    override fun observarTodas(): Flow<List<PlantillaConRecuento>> =
        combine(filas, trabajadores.filas) { ps, ts ->
            ps.sortedWith(compareBy({ it.orden }, { it.id }))
                .map { p -> PlantillaConRecuento(p, ts.count { it.plantillaId == p.id }) }
        }

    override fun observar(id: Long): Flow<Plantilla?> = filas.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun insertarAlFinal(tipo: TipoPlantilla, nombre: String): Long {
        val id = siguienteId++
        val orden = (filas.value.maxOfOrNull { it.orden } ?: 0) + 1
        filas.update { it + Plantilla(id = id, tipo = tipo, nombre = nombre, orden = orden) }
        return id
    }

    override suspend fun renombrar(id: Long, nombre: String) =
        filas.update { l -> l.map { if (it.id == id) it.copy(nombre = nombre) else it } }

    override suspend fun borrar(id: Long) {
        filas.update { l -> l.filterNot { it.id == id } }
        trabajadores.filas.value.filter { it.plantillaId == id }.forEach { trabajadores.borrar(it.id) }
    }

    override suspend fun contar(): Int = filas.value.size
}

/** In-memory [AsignacionDao]: unique per (fecha, trabajador), cascade from [FakeTrabajadorDao]. */
class FakeAsignacionDao(private val trabajadores: FakeTrabajadorDao) : AsignacionDao {
    val filas = MutableStateFlow<List<Asignacion>>(emptyList())
    private var siguienteId = 1L

    init {
        trabajadores.alBorrar = { id -> filas.update { l -> l.filterNot { it.trabajadorId == id } } }
    }

    private fun idsDePlantilla(plantillaId: Long, ts: List<Trabajador>) =
        ts.filter { it.plantillaId == plantillaId }.map { it.id }.toSet()

    override fun observarMes(plantillaId: Long, desde: String, hasta: String): Flow<List<Asignacion>> =
        combine(filas, trabajadores.filas) { asig, ts ->
            val ids = idsDePlantilla(plantillaId, ts)
            asig.filter { it.trabajadorId in ids && it.fecha in desde..hasta }
        }

    override suspend fun upsert(asignacion: Asignacion) {
        // Same as SQLite: the worker must exist (foreign key).
        check(trabajadores.filas.value.any { it.id == asignacion.trabajadorId }) {
            "FK rota: no existe el trabajador ${asignacion.trabajadorId}"
        }
        val id =if (asignacion.id == 0L) siguienteId++ else asignacion.id
        filas.update { l ->
            l.filterNot { it.fecha == asignacion.fecha && it.trabajadorId == asignacion.trabajadorId } + asignacion.copy(id = id)
        }
    }

    override suspend fun borrar(fecha: String, trabajadorId: Long) =
        filas.update { l -> l.filterNot { it.fecha == fecha && it.trabajadorId == trabajadorId } }

    override fun observarContarDePlantilla(plantillaId: Long): Flow<Int> =
        combine(filas, trabajadores.filas) { asig, ts -> asig.count { it.trabajadorId in idsDePlantilla(plantillaId, ts) } }

    override suspend fun contarDePlantilla(plantillaId: Long): Int =
        filas.value.count { it.trabajadorId in idsDePlantilla(plantillaId, trabajadores.filas.value) }

    override suspend fun obtenerDeTrabajador(trabajadorId: Long): List<Asignacion> =
        filas.value.filter { it.trabajadorId == trabajadorId }.sortedBy { it.fecha }

    override suspend fun insertarTodas(asignaciones: List<Asignacion>) = asignaciones.forEach { upsert(it) }
}

/** In-memory [AjusteDao]. */
class FakeAjusteDao : AjusteDao {
    val filas = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observar(clave: String): Flow<String?> = filas.map { it[clave] }

    override suspend fun guardar(ajuste: Ajuste) = filas.update { it + (ajuste.clave to ajuste.valor) }
}

/** The four related fakes wired together, as the real database is. */
class FakesPlantillas {
    val trabajadores = FakeTrabajadorDao()
    val plantillas = FakePlantillaDao(trabajadores)
    val asignaciones = FakeAsignacionDao(trabajadores)
    val ajustes = FakeAjusteDao()

    fun repositorioPlantillas() = PlantillasRepositorio(plantillas, trabajadores, asignaciones, SinTransaccion)
}

/** Replaces Dispatchers.Main (used by viewModelScope) with a test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
