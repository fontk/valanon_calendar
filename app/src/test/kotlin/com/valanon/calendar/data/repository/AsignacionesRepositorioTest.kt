package com.valanon.calendar.data.repository

import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.testing.FakesPlantillas
import com.valanon.calendar.testing.Tipos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AsignacionesRepositorioTest {

    private val fakes = FakesPlantillas()
    private val plantillas = fakes.repositorioPlantillas()
    private val repo = fakes.repositorioAsignaciones()

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
        plantillas.agregarElemento(TipoPlantilla.TRABAJADORES, p, "Lucía")
        plantillas.agregarElemento(TipoPlantilla.TRABAJADORES, p, "Javier")
        val (lucia, javier) = repo.trabajadores(p).first().map { it.id }
        val dia = LocalDate.of(2026, 10, 17)

        repo.asignar(dia, lucia, Tipos.MANANA)
        repo.asignar(dia, lucia, Tipos.NOCHE) // same day and worker → replaced
        repo.asignar(dia, javier, Tipos.TARDE)
        repo.asignar(LocalDate.of(2026, 11, 1), javier, Tipos.LIBRE) // other month

        assertEquals(
            mapOf(dia to mapOf(lucia to Tipos.NOCHE, javier to Tipos.TARDE)),
            repo.asignacionesMes(p, YearMonth.of(2026, 10)).first(),
        )

        repo.quitar(dia, lucia)
        assertEquals(mapOf(dia to mapOf(javier to Tipos.TARDE)), repo.asignacionesMes(p, YearMonth.of(2026, 10)).first())
    }

    @Test
    fun `el mes solo incluye a los trabajadores de la plantilla pedida`() = runTest {
        val a = plantillas.crear(TipoPlantilla.TRABAJADORES)
        val b = plantillas.crear(TipoPlantilla.TRABAJADORES)
        plantillas.agregarElemento(TipoPlantilla.TRABAJADORES, a, "De A")
        plantillas.agregarElemento(TipoPlantilla.TRABAJADORES, b, "De B")
        val deA = repo.trabajadores(a).first().single().id
        val deB = repo.trabajadores(b).first().single().id
        val dia = LocalDate.of(2026, 10, 5)
        repo.asignar(dia, deA, Tipos.MANANA)
        repo.asignar(dia, deB, Tipos.NOCHE)

        assertEquals(mapOf(dia to mapOf(deA to Tipos.MANANA)), repo.asignacionesMes(a, YearMonth.of(2026, 10)).first())
    }
}
