package com.valanon.calendar.testing

import com.valanon.calendar.data.local.SinTransaccion
import com.valanon.calendar.data.local.dao.AjusteDao
import com.valanon.calendar.data.repository.PlantillasRepositorio
import com.valanon.calendar.data.local.dao.AsignacionDao
import com.valanon.calendar.data.local.dao.PlantillaDao
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.dao.ListaDao
import com.valanon.calendar.data.local.dao.TareaDao
import com.valanon.calendar.data.local.dao.TareaPlantillaDao
import com.valanon.calendar.data.local.dao.TrabajadorDao
import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.Lista
import com.valanon.calendar.data.local.entity.ListaConProgreso
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.TareaPlantilla
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.local.TIPOS_TURNO_POR_DEFECTO
import com.valanon.calendar.data.local.dao.TipoTurnoConUsos
import com.valanon.calendar.data.local.dao.TiposTurnoDao
import com.valanon.calendar.data.local.dao.UsosTipoTurno
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.local.entity.TipoTurnoId
import com.valanon.calendar.data.repository.AsignacionesRepositorio
import com.valanon.calendar.data.repository.ListasRepositorio
import com.valanon.calendar.data.repository.TareasRepositorio
import com.valanon.calendar.data.repository.TiposTurnoRepositorio
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

/**
 * In-memory [TareaDao] mirroring the SQL in the real DAO. [existeLista] enforces the foreign key
 * to `listas` (wired by [FakesPlantillas]); [presets] is where [copiarDePlantilla] reads from.
 */
class FakeTareaDao(private val presets: FakeTareaPlantillaDao = FakeTareaPlantillaDao()) : TareaDao {
    val filas = MutableStateFlow<List<Tarea>>(emptyList())
    private var siguienteId = 1L
    var existeLista: (Long) -> Boolean = { true }

    override fun observarDeLista(listaId: Long): Flow<List<Tarea>> =
        filas.map { l -> l.filter { it.listaId == listaId }.sortedWith(compareBy({ it.orden }, { it.id })) }

    private fun insertar(tarea: Tarea): Long {
        check(existeLista(tarea.listaId)) { "FK rota: no existe la lista ${tarea.listaId}" }
        check(filas.value.none { it.id == tarea.id }) { "PK duplicada en tareas: ${tarea.id}" }
        val id = if (tarea.id == 0L) siguienteId++ else tarea.id
        siguienteId = maxOf(siguienteId, id + 1)
        filas.update { it + tarea.copy(id = id) }
        return id
    }

    private fun maxOrden(listaId: Long): Int = filas.value.filter { it.listaId == listaId }.maxOfOrNull { it.orden } ?: 0

    override suspend fun agregarAlFinal(listaId: Long, texto: String): Long =
        insertar(Tarea(listaId = listaId, texto = texto, hecha = false, orden = maxOrden(listaId) + 1))

    override suspend fun restaurar(id: Long, listaId: Long, texto: String, hecha: Boolean, orden: Int) {
        // INSERT … WHERE EXISTS: silently nothing if the list is gone.
        if (existeLista(listaId)) insertar(Tarea(id, listaId, texto, hecha, orden))
    }

    override suspend fun copiarDePlantilla(listaId: Long, plantillaId: Long) {
        presets.filas.value.filter { it.plantillaId == plantillaId }.sortedWith(compareBy({ it.orden }, { it.id }))
            .forEach { insertar(Tarea(listaId = listaId, texto = it.texto, hecha = false, orden = it.orden)) }
    }

    override suspend fun actualizarHecha(id: Long, hecha: Boolean) =
        filas.update { l -> l.map { if (it.id == id) it.copy(hecha = hecha) else it } }

    override suspend fun borrar(id: Long) = filas.update { l -> l.filterNot { it.id == id } }

    override suspend fun reiniciarLista(listaId: Long) =
        filas.update { l -> l.map { if (it.listaId == listaId) it.copy(hecha = false) else it } }
}

/** In-memory [TareaPlantillaDao]; [FakePlantillaDao.borrar] cascades into it like the real FK. */
class FakeTareaPlantillaDao : TareaPlantillaDao {
    val filas = MutableStateFlow<List<TareaPlantilla>>(emptyList())
    private var siguienteId = 1L

    override fun observarDePlantilla(plantillaId: Long): Flow<List<TareaPlantilla>> =
        filas.map { l -> l.filter { it.plantillaId == plantillaId }.sortedWith(compareBy({ it.orden }, { it.id })) }

    override suspend fun agregarAlFinal(plantillaId: Long, texto: String): Long {
        val orden = (filas.value.filter { it.plantillaId == plantillaId }.maxOfOrNull { it.orden } ?: 0) + 1
        return insertar(TareaPlantilla(plantillaId = plantillaId, texto = texto, orden = orden))
    }

    override suspend fun cambiarTexto(id: Long, texto: String) =
        filas.update { l -> l.map { if (it.id == id) it.copy(texto = texto) else it } }

    override suspend fun borrar(id: Long) = filas.update { l -> l.filterNot { it.id == id } }

    override suspend fun insertar(tarea: TareaPlantilla): Long {
        check(filas.value.none { it.id == tarea.id }) { "PK duplicada en tareas_plantilla: ${tarea.id}" }
        val id = if (tarea.id == 0L) siguienteId++ else tarea.id
        siguienteId = maxOf(siguienteId, id + 1)
        filas.update { it + tarea.copy(id = id) }
        return id
    }
}

/** In-memory [ListaDao] with the progress subqueries, the LEFT JOIN and the cascade into tasks. */
class FakeListaDao(private val tareas: FakeTareaDao, private val plantillas: FakePlantillaDao) : ListaDao {
    val filas = MutableStateFlow<List<Lista>>(emptyList())
    private var siguienteId = 1L

    override fun observarTodas(): Flow<List<ListaConProgreso>> =
        combine(filas, tareas.filas, plantillas.filas) { ls, ts, ps ->
            ls.sortedWith(compareBy({ it.orden }, { it.id })).map { l ->
                val propias = ts.filter { it.listaId == l.id }
                ListaConProgreso(l, ps.firstOrNull { it.id == l.plantillaId }?.nombre, propias.size, propias.count { it.hecha })
            }
        }

    override suspend fun insertarAlFinal(nombre: String, plantillaId: Long?): Long {
        check(plantillaId == null || plantillas.filas.value.any { it.id == plantillaId }) { "FK rota: plantilla $plantillaId" }
        val id = siguienteId++
        val orden = (filas.value.maxOfOrNull { it.orden } ?: 0) + 1
        filas.update { it + Lista(id = id, nombre = nombre, plantillaId = plantillaId, orden = orden) }
        return id
    }

    override suspend fun renombrar(id: Long, nombre: String) =
        filas.update { l -> l.map { if (it.id == id) it.copy(nombre = nombre) else it } }

    override suspend fun borrar(id: Long) {
        filas.update { l -> l.filterNot { it.id == id } }
        tareas.filas.update { l -> l.filterNot { it.listaId == id } }
    }

    override suspend fun ids(): List<Long> = filas.value.sortedWith(compareBy({ it.orden }, { it.id })).map { it.id }

    /** ON DELETE SET NULL from `plantillas`. */
    fun plantillaBorrada(plantillaId: Long) =
        filas.update { l -> l.map { if (it.plantillaId == plantillaId) it.copy(plantillaId = null) else it } }
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

/** In-memory [PlantillaDao] with the item count and the ON DELETE CASCADE / SET NULL of the real schema. */
class FakePlantillaDao(
    private val trabajadores: FakeTrabajadorDao,
    private val tareasPlantilla: FakeTareaPlantillaDao = FakeTareaPlantillaDao(),
) : PlantillaDao {
    val filas = MutableStateFlow<List<Plantilla>>(emptyList())
    private var siguienteId = 1L

    /** SET NULL hook (FakeListaDao clears `plantilla_id` of the lists copied from it). */
    var alBorrar: (Long) -> Unit = {}

    override fun observarTodas(): Flow<List<PlantillaConRecuento>> =
        combine(filas, trabajadores.filas, tareasPlantilla.filas) { ps, ts, tps ->
            ps.sortedWith(compareBy({ it.orden }, { it.id }))
                .map { p -> PlantillaConRecuento(p, ts.count { it.plantillaId == p.id } + tps.count { it.plantillaId == p.id }) }
        }

    override fun observar(id: Long): Flow<Plantilla?> = filas.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun obtener(id: Long): Plantilla? = filas.value.firstOrNull { it.id == id }

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
        tareasPlantilla.filas.update { l -> l.filterNot { it.plantillaId == id } }
        alBorrar(id)
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

/** The five default shift types (ids 1–5), as a fresh database has them. */
object Tipos {
    val LIBRE = TIPOS_TURNO_POR_DEFECTO[0]
    val MANANA = TIPOS_TURNO_POR_DEFECTO[1]
    val TARDE = TIPOS_TURNO_POR_DEFECTO[2]
    val NOCHE = TIPOS_TURNO_POR_DEFECTO[3]
    val VACACIONES = TIPOS_TURNO_POR_DEFECTO[4]
}

/**
 * In-memory [TiposTurnoDao] seeded with the five defaults. Deleting a type cascades into the
 * given turnos/asignaciones fakes like the real ON DELETE CASCADE; usage counts read them too.
 */
class FakeTiposTurnoDao(
    private val turnos: FakeTurnoDao? = null,
    private val asignaciones: FakeAsignacionDao? = null,
) : TiposTurnoDao {
    val filas = MutableStateFlow(TIPOS_TURNO_POR_DEFECTO)
    private var siguienteId = TIPOS_TURNO_POR_DEFECTO.size + 1L

    private fun ordenados(l: List<TipoTurno>) = l.sortedWith(compareBy({ it.orden }, { it.id }))

    override fun observarTodos(): Flow<List<TipoTurno>> = filas.map(::ordenados)

    override suspend fun obtenerTodos(): List<TipoTurno> = ordenados(filas.value)

    override fun observarConUsos(): Flow<List<TipoTurnoConUsos>> =
        combine(
            filas,
            turnos?.filas ?: MutableStateFlow(emptyMap()),
            asignaciones?.filas ?: MutableStateFlow(emptyList()),
        ) { tipos, ts, asig ->
            ordenados(tipos).map { t ->
                TipoTurnoConUsos(t, UsosTipoTurno(ts.values.count { it.tipoId == t.id }, asig.count { it.tipoId == t.id }))
            }
        }

    override suspend fun insertarAlFinal(clave: String?, nombre: String, abreviatura: String, color: String): Long {
        check(clave == null || filas.value.none { it.clave == clave }) { "UNIQUE rota en tipos_turno.clave: $clave" }
        val id = siguienteId++
        val orden = (filas.value.maxOfOrNull { it.orden } ?: 0) + 1
        filas.update { it + TipoTurno(id, clave, nombre, abreviatura, color, orden) }
        return id
    }

    override suspend fun actualizar(id: TipoTurnoId, nombre: String, abreviatura: String, color: String) =
        filas.update { l -> l.map { if (it.id == id) it.copy(nombre = nombre, abreviatura = abreviatura, color = color) else it } }

    override suspend fun borrar(id: TipoTurnoId) {
        filas.update { l -> l.filterNot { it.id == id } }
        turnos?.filas?.update { m -> m.filterValues { it.tipoId != id } }
        asignaciones?.filas?.update { l -> l.filterNot { it.tipoId == id } }
    }

    override suspend fun contar(): Int = filas.value.size

    override suspend fun usos(id: TipoTurnoId): UsosTipoTurno = UsosTipoTurno(
        turnos?.filas?.value?.values?.count { it.tipoId == id } ?: 0,
        asignaciones?.filas?.value?.count { it.tipoId == id } ?: 0,
    )

    override suspend fun obtenerPorClave(clave: String): TipoTurno? = filas.value.firstOrNull { it.clave == clave }
}

/** In-memory [AjusteDao]. */
class FakeAjusteDao : AjusteDao {
    val filas = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observar(clave: String): Flow<String?> = filas.map { it[clave] }

    override suspend fun obtener(clave: String): String? = filas.value[clave]

    override suspend fun guardar(ajuste: Ajuste) = filas.update { it + (ajuste.clave to ajuste.valor) }
}

/** The related fakes (templates, their items, team shifts, Checklist lists and tasks) wired together as the real database is. */
class FakesPlantillas {
    val trabajadores = FakeTrabajadorDao()
    val tareasPlantilla = FakeTareaPlantillaDao()
    val plantillas = FakePlantillaDao(trabajadores, tareasPlantilla)
    val asignaciones = FakeAsignacionDao(trabajadores)
    val ajustes = FakeAjusteDao()
    val tareas = FakeTareaDao(tareasPlantilla)
    val listas = FakeListaDao(tareas, plantillas)
    val turnos = FakeTurnoDao()
    val tipos = FakeTiposTurnoDao(turnos, asignaciones)

    init {
        tareas.existeLista = { id -> listas.filas.value.any { it.id == id } }
        plantillas.alBorrar = listas::plantillaBorrada
    }

    fun repositorioPlantillas() =
        PlantillasRepositorio(plantillas, trabajadores, tareasPlantilla, asignaciones, SinTransaccion)

    fun repositorioListas() = ListasRepositorio(listas, tareas, plantillas, ajustes, SinTransaccion)

    fun repositorioTareas() = TareasRepositorio(tareas)

    fun repositorioAsignaciones() = AsignacionesRepositorio(asignaciones, ajustes, plantillas, trabajadores, tipos)

    fun repositorioTipos() = TiposTurnoRepositorio(tipos, SinTransaccion)
}

/** Replaces Dispatchers.Main (used by viewModelScope) with a test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
