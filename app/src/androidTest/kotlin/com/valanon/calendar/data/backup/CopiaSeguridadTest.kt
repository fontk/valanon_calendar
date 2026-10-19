package com.valanon.calendar.data.backup

import com.valanon.calendar.data.local.TIPOS_TURNO_POR_DEFECTO
import com.valanon.calendar.testing.Tipos
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.AppDatabase
import com.valanon.calendar.data.local.SEMBRAR_TIPOS_TURNO
import com.valanon.calendar.data.local.CLAVE_LISTA_CHECKLIST
import com.valanon.calendar.data.local.NOMBRE_LISTA_MIGRADA
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.DatosApp
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Export/import of the whole database on real SQLite through file URIs. */
@RunWith(AndroidJUnit4::class)
class CopiaSeguridadTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var copia: CopiaSeguridadRepositorio
    private val archivo = File(context.cacheDir, "copia-test.json")
    private val uri get() = Uri.fromFile(archivo)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(SEMBRAR_TIPOS_TURNO).build()
        copia = CopiaSeguridadRepositorio(
            db.copiaSeguridadDao(),
            context.contentResolver,
            Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() {
        db.close()
        archivo.delete()
    }

    /** Id of the blank list filled by [rellenar]. */
    private var lista = 0L

    /** Some data in every table. */
    private suspend fun rellenar() {
        db.turnoDao().upsert(Turno("2026-10-17", Tipos.NOCHE.id))
        db.turnoDao().upsert(Turno("2026-10-18", Tipos.LIBRE.id))
        lista = db.listaDao().insertarAlFinal("Mi checklist", null)
        db.tareaDao().agregarAlFinal(lista, "Preparar el uniforme")
        val tarea = db.tareaDao().agregarAlFinal(lista, "Comprar comida")
        db.tareaDao().actualizarHecha(tarea, true)
        val p = db.plantillaDao().insertarAlFinal(TipoPlantilla.TRABAJADORES, "Equipo de planta")
        val lucia = db.trabajadorDao().agregarAlFinal(p, "Lucía Martínez")
        db.trabajadorDao().agregarAlFinal(p, "Javier Ortega")
        db.asignacionDao().upsert(Asignacion(fecha = "2026-10-17", trabajadorId = lucia, tipoId = Tipos.MANANA.id))
        db.ajusteDao().guardar(Ajuste("turnos.plantilla_id", "$p"))
        val preset = db.plantillaDao().insertarAlFinal(TipoPlantilla.TAREAS, "Cierre de turno")
        db.tareaPlantillaDao().agregarAlFinal(preset, "Cuadrar la caja")
        db.tareaPlantillaDao().agregarAlFinal(preset, "Apagar equipos")
        val copiada = db.listaDao().insertarAlFinal("Cierre de turno", preset)
        db.tareaDao().copiarDePlantilla(copiada, preset)
        db.ajusteDao().guardar(Ajuste(CLAVE_LISTA_CHECKLIST, "$copiada"))
    }

    @Test
    fun exportarEImportarDevuelveExactamenteLosMismosDatos() = runTest {
        rellenar()
        val antes = db.copiaSeguridadDao().leerTodo()

        copia.exportar(uri)
        db.clearAllTables()
        assertTrue(db.copiaSeguridadDao().leerTodo().turnos.isEmpty())

        copia.importar(uri)

        // Assignments get new row ids on insert; everything users see is identical.
        val despues = db.copiaSeguridadDao().leerTodo()
        assertEquals(antes.copy(asignaciones = emptyList()), despues.copy(asignaciones = emptyList()))
        assertEquals(
            antes.asignaciones.map { Triple(it.fecha, it.trabajadorId, it.tipoId) },
            despues.asignaciones.map { Triple(it.fecha, it.trabajadorId, it.tipoId) },
        )
        // The app keeps working after an import: new rows go after the imported ones.
        val nueva = db.tareaDao().agregarAlFinal(lista, "Nueva")
        assertTrue(nueva > antes.tareas.maxOf { it.id })
        assertEquals(3, db.tareaDao().observarDeLista(lista).first().last().orden)
        assertTrue(db.listaDao().insertarAlFinal("Otra", null) > antes.listas.maxOf { it.id })
    }

    /**
     * A7: a backup made by 0.7.0 (format 2, the five fixed shifts stored as text) still imports: it
     * gets the five default types and each day points to the matching one.
     */
    @Test
    fun importarUnaCopiaVersion2MapeaLosTurnosALosTiposPorDefecto() = runTest {
        archivo.writeText(
            """
            {"formato":"mis-turnos-copia","version":2,
             "turnos":[{"fecha":"2026-10-17","tipo":"noche"},{"fecha":"2026-10-18","tipo":"vacaciones"}],
             "plantillas":[{"id":1,"tipo":"trabajadores","nombre":"Equipo","orden":1}],
             "trabajadores":[{"id":1,"plantillaId":1,"nombre":"Lucía","orden":1}],
             "asignaciones":[{"fecha":"2026-10-17","trabajadorId":1,"tipo":"manana"}]}
            """.trimIndent(),
        )

        copia.importar(uri)

        val datos = db.copiaSeguridadDao().leerTodo()
        assertEquals(TIPOS_TURNO_POR_DEFECTO, datos.tiposTurno)
        assertEquals(listOf(Tipos.NOCHE.id, Tipos.VACACIONES.id), datos.turnos.map { it.tipoId })
        assertEquals(listOf(Tipos.MANANA.id), datos.asignaciones.map { it.tipoId })
    }

    /** A backup made by 0.6.0 (format 1, single checklist) still imports: its tasks land in "Mi checklist". */
    @Test
    fun importarUnaCopiaVersion1PoneLasTareasEnMiChecklist() = runTest {
        archivo.writeText(
            """
            {"formato":"mis-turnos-copia","version":1,"creado":"2026-10-01T10:00:00Z",
             "turnos":[{"fecha":"2026-10-17","tipo":"noche"}],
             "tareas":[{"id":3,"texto":"Preparar el uniforme","hecha":true,"orden":1},
                       {"id":5,"texto":"Comprar comida","hecha":false,"orden":2}],
             "plantillas":[{"id":1,"tipo":"trabajadores","nombre":"Equipo","orden":1}],
             "trabajadores":[{"id":1,"plantillaId":1,"nombre":"Lucía","orden":1}],
             "asignaciones":[], "ajustes":[{"clave":"turnos.plantilla_id","valor":"1"}]}
            """.trimIndent(),
        )

        copia.importar(uri)

        val listas = db.listaDao().observarTodas().first()
        val miChecklist = listas.single()
        assertEquals(NOMBRE_LISTA_MIGRADA, miChecklist.lista.nombre)
        assertEquals(2 to 1, miChecklist.total to miChecklist.hechas)
        assertEquals(miChecklist.lista.id.toString(), db.ajusteDao().obtener(CLAVE_LISTA_CHECKLIST))
        assertEquals("1", db.ajusteDao().obtener("turnos.plantilla_id"))
        val tareas = db.tareaDao().observarDeLista(miChecklist.lista.id).first()
        assertEquals(listOf(3L, 5L), tareas.map { it.id })
        assertEquals(listOf(true, false), tareas.map { it.hecha })
    }

    @Test
    fun importarReemplazaLoQueHabia() = runTest {
        rellenar()
        copia.exportar(uri)
        db.turnoDao().upsert(Turno("2026-12-25", Tipos.VACACIONES.id)) // not in the backup

        copia.importar(uri)

        assertEquals(listOf("2026-10-17", "2026-10-18"), db.copiaSeguridadDao().leerTodo().turnos.map { it.fecha })
    }

    @Test
    fun unArchivoInvalidoNoTocaLosDatos() = runTest {
        rellenar()
        val antes = db.copiaSeguridadDao().leerTodo()
        val casos = listOf(
            "esto no es json",
            """{"formato":"otra-app","version":1}""",
            """{"formato":"mis-turnos-copia","version":99}""",
            """{"formato":"mis-turnos-copia","version":1,"turnos":[{"fecha":"2026-10-17","tipo":"siesta"}]}""",
            """{"formato":"mis-turnos-copia","version":3,"tiposTurno":[{"id":1,"clave":null,"nombre":"A","abreviatura":"A","color":"gris","orden":1}],"turnos":[{"fecha":"2026-10-17","tipoId":9}]}""",
            """{"formato":"mis-turnos-copia","version":1,"trabajadores":[{"id":1,"plantillaId":7,"nombre":"X","orden":1}]}""",
            """{"formato":"mis-turnos-copia","version":2,"tareas":[{"id":1,"listaId":9,"texto":"X","hecha":false,"orden":1}]}""",
            """{"formato":"mis-turnos-copia","version":2,"tareas":[{"id":1,"texto":"Sin listaId","hecha":false,"orden":1}]}""",
        )
        for (contenido in casos) {
            archivo.writeText(contenido)
            try {
                copia.importar(uri)
                fail("Debería rechazar: $contenido")
            } catch (e: CopiaInvalidaException) {
                assertTrue(e.message!!.isNotBlank())
            }
            assertEquals(antes, db.copiaSeguridadDao().leerTodo())
        }
    }

    @Test
    fun formatoToleraTablasAusentes() {
        // A minimal file (e.g. from a future version that dropped a table) reads as empty lists;
        // an old file (no shift types) gets the five defaults so its days can still be resolved.
        val datos = FormatoCopia.desdeJson("""{"formato":"mis-turnos-copia","version":1}""")
        assertEquals(DatosApp(tiposTurno = TIPOS_TURNO_POR_DEFECTO), datos)
    }

    @Test
    fun elArchivoEsJsonLegibleConVersion() = runTest {
        rellenar()
        copia.exportar(uri)
        val texto = archivo.readText()
        assertTrue(texto.contains("\"formato\": \"mis-turnos-copia\""))
        assertTrue(texto.contains("\"version\": 3"))
        assertTrue(texto.contains("\"tiposTurno\""))
        assertTrue(texto.contains("\"tareasPlantilla\""))
        assertTrue(texto.contains("\"creado\": \"2026-10-18T10:00:00Z\""))
        assertTrue(texto.contains("Lucía Martínez"))
    }
}
