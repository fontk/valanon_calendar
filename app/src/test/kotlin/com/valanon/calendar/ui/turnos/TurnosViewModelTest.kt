package com.valanon.calendar.ui.turnos

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.repository.AsignacionesRepositorio
import com.valanon.calendar.testing.FakesPlantillas
import com.valanon.calendar.testing.MainDispatcherRule
import com.valanon.calendar.testing.Tipos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class TurnosViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fakes = FakesPlantillas()
    private val plantillas = fakes.repositorioPlantillas()
    private val repo = fakes.repositorioAsignaciones()
    private val zona = ZoneId.of("Europe/Madrid")
    private val hoy = LocalDate.of(2026, 10, 17)
    private val reloj = Clock.fixed(hoy.atStartOfDay(zona).toInstant(), zona)

    private fun TestScope.crear(handle: SavedStateHandle = SavedStateHandle()) =
        TurnosViewModel(repo, handle, reloj).also { it.uiState.launchIn(backgroundScope) }

    /** "Equipo de planta" with Lucía and Javier; returns (template, lucía, javier) ids. */
    private suspend fun equipo(nombre: String = "Equipo de planta", vararg personas: String = arrayOf("Lucía", "Javier")): List<Long> {
        val id = plantillas.crear(TipoPlantilla.TRABAJADORES)
        plantillas.renombrarPlantilla(id, nombre)
        personas.forEach { plantillas.agregarElemento(TipoPlantilla.TRABAJADORES, id, it) }
        return listOf(id) + repo.trabajadores(id).first().map { it.id }
    }

    @Test
    fun `T2 sin plantillas muestra el estado vacio`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        assertTrue(vm.uiState.value.sinPlantillas)
    }

    @Test
    fun `T1 usa la primera plantilla, el mes actual y cuenta los dias con turnos`() = runTest(mainRule.dispatcher) {
        val (p, lucia, javier) = equipo()
        repo.asignar(hoy, lucia, Tipos.NOCHE)
        repo.asignar(hoy, javier, Tipos.MANANA)
        repo.asignar(hoy.plusDays(1), lucia, Tipos.LIBRE)
        repo.asignar(LocalDate.of(2026, 11, 3), lucia, Tipos.TARDE) // other month

        val vm = crear()
        val estado = vm.uiState.value
        assertEquals(YearMonth.of(2026, 10), estado.mes)
        assertEquals(p, estado.plantilla?.plantilla?.id)
        assertEquals(2, estado.diasConTurnos)
        assertEquals(listOf(Tipos.NOCHE, Tipos.MANANA), estado.puntosDe(hoy)) // template order
    }

    @Test
    fun `T1 el mes de Turnos es independiente y usa el mismo selector de mes`() = runTest(mainRule.dispatcher) {
        equipo()
        val handle = SavedStateHandle()
        val vm = crear(handle)

        vm.mesSiguiente()
        vm.abrirSelectorMes()
        vm.anioSelectorSiguiente()
        vm.irAMes(vm.uiState.value.anioSelector, 3)

        assertEquals(YearMonth.of(2027, 3), vm.uiState.value.mes)
        assertFalse(vm.uiState.value.selectorMesAbierto)
        // Calendario's key ("mes") is untouched: Turnos uses its own.
        assertNull(handle.get<String>("mes"))
        assertEquals("2027-03", handle.get<String>("turnosMes"))
    }

    @Test
    fun `T3 T4 abrir dia, editar, elegir turno persiste y vuelve al equipo en edicion`() = runTest(mainRule.dispatcher) {
        val (p, _, javier) = equipo()
        val vm = crear()

        vm.abrirDia(hoy)
        assertEquals(hoy, vm.uiState.value.diaAbierto)
        assertFalse(vm.uiState.value.editando)

        vm.empezarEdicion()
        vm.abrirSelectorTrabajador(javier)
        assertEquals(javier, vm.uiState.value.trabajadorSeleccionado?.id)

        vm.asignar(Tipos.NOCHE)

        assertNull(vm.uiState.value.trabajadorSeleccionadoId)
        assertTrue(vm.uiState.value.editando)
        assertEquals(mapOf(javier to Tipos.NOCHE), vm.uiState.value.turnosDe(hoy))
        // Persisted, not just in memory:
        assertEquals(mapOf(hoy to mapOf(javier to Tipos.NOCHE)), repo.asignacionesMes(p, YearMonth.of(2026, 10)).first())
    }

    @Test
    fun `T4 quitar turno borra la asignacion y cancelar no cambia nada`() = runTest(mainRule.dispatcher) {
        val (_, lucia) = equipo()
        repo.asignar(hoy, lucia, Tipos.TARDE)
        val vm = crear()
        vm.abrirDia(hoy)
        vm.empezarEdicion()

        vm.abrirSelectorTrabajador(lucia)
        vm.volverAEquipo() // Cancelar / back arrow
        assertEquals(mapOf(lucia to Tipos.TARDE), vm.uiState.value.turnosDe(hoy))

        vm.abrirSelectorTrabajador(lucia)
        vm.quitarTurno()
        assertTrue(vm.uiState.value.turnosDe(hoy).isEmpty())
        assertNull(vm.uiState.value.trabajadorSeleccionadoId)
    }

    @Test
    fun `T4 Listo vuelve a lectura y cerrar la hoja sale de edicion`() = runTest(mainRule.dispatcher) {
        equipo()
        val vm = crear()
        vm.abrirDia(hoy)
        vm.empezarEdicion()

        vm.terminarEdicion()
        assertFalse(vm.uiState.value.editando)

        vm.empezarEdicion()
        vm.cerrarDia()
        assertNull(vm.uiState.value.diaAbierto)
        vm.abrirDia(hoy)
        assertFalse(vm.uiState.value.editando)
    }

    @Test
    fun `T5 cambiar de plantilla recarga y se recuerda`() = runTest(mainRule.dispatcher) {
        val (a, lucia) = equipo("Equipo de planta", "Lucía")
        val (b, sara) = equipo("Refuerzos fin de semana", "Sara")
        repo.asignar(hoy, lucia, Tipos.MANANA)
        repo.asignar(hoy, sara, Tipos.NOCHE)
        val vm = crear()
        assertEquals(a, vm.uiState.value.plantilla?.plantilla?.id)

        vm.abrirHojaPlantillas()
        vm.elegirPlantilla(b)

        assertFalse(vm.uiState.value.hojaPlantillasAbierta)
        assertEquals(b, vm.uiState.value.plantilla?.plantilla?.id)
        assertEquals(listOf("Sara"), vm.uiState.value.trabajadores.map { it.nombre })
        assertEquals(mapOf(sara to Tipos.NOCHE), vm.uiState.value.turnosDe(hoy))
        assertEquals("$b", fakes.ajustes.filas.value[AsignacionesRepositorio.CLAVE_PLANTILLA_TURNOS])
    }

    @Test
    fun `T6 borrar la plantilla activa pasa a la siguiente y luego al estado vacio`() = runTest(mainRule.dispatcher) {
        val (a) = equipo("A", "Uno")
        val (b) = equipo("B", "Dos")
        val vm = crear()
        vm.elegirPlantilla(b)

        plantillas.borrarPlantilla(b)
        assertEquals(a, vm.uiState.value.plantilla?.plantilla?.id)

        plantillas.borrarPlantilla(a)
        assertTrue(vm.uiState.value.sinPlantillas)
    }

    @Test
    fun `T6 un trabajador nuevo aparece sin asignar`() = runTest(mainRule.dispatcher) {
        val (p) = equipo()
        val vm = crear()

        plantillas.agregarElemento(TipoPlantilla.TRABAJADORES, p, "Ana")

        assertEquals(listOf("Lucía", "Javier", "Ana"), vm.uiState.value.trabajadores.map { it.nombre })
        assertTrue(vm.uiState.value.turnosDe(hoy).isEmpty())
    }

    @Test
    fun `la hoja abierta en el selector sobrevive a la recreacion`() = runTest(mainRule.dispatcher) {
        val (_, lucia) = equipo()
        val handle = SavedStateHandle()
        crear(handle).apply {
            abrirDia(hoy)
            empezarEdicion()
            abrirSelectorTrabajador(lucia)
        }

        val recreado = crear(handle)
        assertEquals(hoy, recreado.uiState.value.diaAbierto)
        assertTrue(recreado.uiState.value.editando)
        assertEquals(lucia, recreado.uiState.value.trabajadorSeleccionado?.id)
    }
}
