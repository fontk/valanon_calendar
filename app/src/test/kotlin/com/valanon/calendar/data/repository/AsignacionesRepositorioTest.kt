package com.valanon.calendar.data.repository

import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.testing.FakesPlantillas
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AsignacionesRepositorioTest {

    private val fakes = FakesPlantillas()
    private val plantillas = PlantillasRepositorio(fakes.plantillas, fakes.trabajadores, fakes.asignaciones)
    private val repo = AsignacionesRepositorio(fakes.asignaciones, fakes.ajustes, fakes.plantillas, fakes.trabajadores)

    @Test
    fun `sin plantillas no hay plantilla activa`() = runTest {
        assertNull(repo.plantillaActiva.first())
    }

    @Test
    fun `la activa es la guardada y si se borra pasa a la primera disponible`() = runTest {
        val a = plantillas.crear(TipoPlantilla.TRABAJADORES)
        val b = plantillas.crear(TipoPlantilla.TRABAJADORES)
        assertEquals(a, repo.plantillaActiva.first()?.plantilla?.id) // nothing saved yet → first

        repo.elegirPlantilla(b)
        assertEquals(b, repo.plantillaActiva.first()?.plantilla?.id)
        assertEquals("$b", fakes.ajustes.filas.value[AsignacionesRepositorio.CLAVE_PLANTILLA_TURNOS])

        plantillas.borrarPlantilla(b)
        assertEquals(a, repo.plantillaActiva.first()?.plantilla?.id)
    }

    @Test
    fun `asignar reemplaza, quitar borra y el mes agrupa por dia y trabajador`() = runTest {
        val p = plantillas.crear(TipoPlantilla.TRABAJADORES)
        plantillas.agregarTrabajador(p, "Lucía")
        plantillas.agregarTrabajador(p, "Javier")
        val (lucia, javier) = repo.trabajadores(p).first().map { it.id }
        val dia = LocalDate.of(2026, 10, 17)

        repo.asignar(dia, lucia, TipoTurno.MANANA)
        repo.asignar(dia, lucia, TipoTurno.NOCHE) // same day and worker → replaced
        repo.asignar(dia, javier, TipoTurno.TARDE)
        repo.asignar(LocalDate.of(2026, 11, 1), javier, TipoTurno.LIBRE) // other month

        assertEquals(
            mapOf(dia to mapOf(lucia to TipoTurno.NOCHE, javier to TipoTurno.TARDE)),
            repo.asignacionesMes(p, YearMonth.of(2026, 10)).first(),
        )

        repo.quitar(dia, lucia)
        assertEquals(mapOf(dia to mapOf(javier to TipoTurno.TARDE)), repo.asignacionesMes(p, YearMonth.of(2026, 10)).first())
    }

    @Test
    fun `el mes solo incluye a los trabajadores de la plantilla pedida`() = runTest {
        val a = plantillas.crear(TipoPlantilla.TRABAJADORES)
        val b = plantillas.crear(TipoPlantilla.TRABAJADORES)
        plantillas.agregarTrabajador(a, "De A")
        plantillas.agregarTrabajador(b, "De B")
        val deA = repo.trabajadores(a).first().single().id
        val deB = repo.trabajadores(b).first().single().id
        val dia = LocalDate.of(2026, 10, 5)
        repo.asignar(dia, deA, TipoTurno.MANANA)
        repo.asignar(dia, deB, TipoTurno.NOCHE)

        assertEquals(mapOf(dia to mapOf(deA to TipoTurno.MANANA)), repo.asignacionesMes(a, YearMonth.of(2026, 10)).first())
    }
}
