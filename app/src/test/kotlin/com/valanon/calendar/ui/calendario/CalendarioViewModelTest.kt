package com.valanon.calendar.ui.calendario

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.repository.TurnosRepositorio
import com.valanon.calendar.testing.FakeTiposTurnoDao
import com.valanon.calendar.testing.FakeTurnoDao
import com.valanon.calendar.testing.MainDispatcherRule
import com.valanon.calendar.testing.Tipos
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val dao = FakeTurnoDao()
    private val tiposDao = FakeTiposTurnoDao(dao)
    private val hoy = LocalDate.of(2026, 10, 17)
    private val reloj = Clock.fixed(hoy.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant(), ZoneId.of("Europe/Madrid"))
    private var exportado: Pair<YearMonth, Map<LocalDate, TipoTurno>>? = null

    private fun crear(handle: SavedStateHandle = SavedStateHandle()) = CalendarioViewModel(
        repositorio = TurnosRepositorio(dao, tiposDao),
        generadorPdf = { mes, turnos, _ ->
            exportado = mes to turnos
            File("turnos.pdf")
        },
        savedStateHandle = handle,
        clock = reloj,
    )

    @Test
    fun `C1 abre en el mes actual con hoy marcado`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)

        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.mes)
        assertEquals(hoy, vm.uiState.value.hoy)
    }

    @Test
    fun `C2 navega entre meses cruzando el cambio de anio`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)

        repeat(3) { vm.mesSiguiente() }
        assertEquals(YearMonth.of(2027, 1), vm.uiState.value.mes)

        repeat(4) { vm.mesAnterior() }
        assertEquals(YearMonth.of(2026, 9), vm.uiState.value.mes)
    }

    @Test
    fun `C2 muestra los turnos guardados del mes destino y solo esos`() = runTest(mainRule.dispatcher) {
        dao.upsert(Turno("2026-10-31", Tipos.NOCHE.id))
        dao.upsert(Turno("2026-11-01", Tipos.LIBRE.id))
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)

        assertEquals(mapOf(LocalDate.of(2026, 10, 31) to Tipos.NOCHE), vm.uiState.value.turnos)

        vm.mesSiguiente()
        assertEquals(mapOf(LocalDate.of(2026, 11, 1) to Tipos.LIBRE), vm.uiState.value.turnos)
    }

    @Test
    fun `C3 seleccionar dia, elegir turno y cerrar`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        val dia = LocalDate.of(2026, 10, 20)

        vm.seleccionarDia(dia)
        assertEquals(dia, vm.uiState.value.diaSeleccionado)

        vm.asignarTurno(dia, Tipos.TARDE)
        vm.cerrarSelector()
        assertEquals(Tipos.TARDE, vm.uiState.value.turnos[dia])
        assertNull(vm.uiState.value.diaSeleccionado)

        vm.seleccionarDia(dia)
        vm.asignarTurno(dia, Tipos.VACACIONES)
        assertEquals(Tipos.VACACIONES, vm.uiState.value.turnos[dia])
    }

    @Test
    fun `el mes visible sobrevive a la recreacion del ViewModel`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle()
        crear(handle).mesSiguiente()

        val recreado = crear(handle)
        recreado.uiState.launchIn(backgroundScope)
        assertEquals(YearMonth.of(2026, 11), recreado.uiState.value.mes)
    }

    // --- C2b: selector de mes y año ---

    @Test
    fun `C2b abrir el selector fija el anio del mes visible`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        repeat(3) { vm.mesSiguiente() } // enero 2027

        vm.abrirSelectorMes()

        assertTrue(vm.uiState.value.selectorMesAbierto)
        assertEquals(2027, vm.uiState.value.anioSelector)
    }

    @Test
    fun `C2b las flechas cambian solo el anio mostrado`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        vm.abrirSelectorMes()

        vm.anioSelectorSiguiente()
        vm.anioSelectorSiguiente()
        assertEquals(2028, vm.uiState.value.anioSelector)
        vm.anioSelectorAnterior()
        assertEquals(2027, vm.uiState.value.anioSelector)

        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.mes)
        assertTrue(vm.uiState.value.selectorMesAbierto)
    }

    @Test
    fun `C2b irAMes cambia el mes visible, carga sus turnos y cierra`() = runTest(mainRule.dispatcher) {
        dao.upsert(Turno("2028-03-10", Tipos.VACACIONES.id))
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        vm.abrirSelectorMes()
        vm.anioSelectorSiguiente()
        vm.anioSelectorSiguiente()

        vm.irAMes(2028, 3)

        assertEquals(YearMonth.of(2028, 3), vm.uiState.value.mes)
        assertEquals(mapOf(LocalDate.of(2028, 3, 10) to Tipos.VACACIONES), vm.uiState.value.turnos)
        assertFalse(vm.uiState.value.selectorMesAbierto)
    }

    @Test
    fun `C2b irAHoy vuelve al mes actual y cierra`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        vm.irAMes(2030, 5)
        vm.abrirSelectorMes()

        vm.irAHoy()

        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.mes)
        assertFalse(vm.uiState.value.selectorMesAbierto)
    }

    @Test
    fun `C2b cerrar no cambia el mes visible`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        vm.abrirSelectorMes()
        vm.anioSelectorSiguiente()

        vm.cerrarSelectorMes()

        assertEquals(YearMonth.of(2026, 10), vm.uiState.value.mes)
        assertFalse(vm.uiState.value.selectorMesAbierto)
    }

    @Test
    fun `C2b la hoja abierta y su anio sobreviven a la recreacion`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle()
        crear(handle).apply {
            abrirSelectorMes()
            anioSelectorAnterior()
        }

        val recreado = crear(handle)
        recreado.uiState.launchIn(backgroundScope)
        assertTrue(recreado.uiState.value.selectorMesAbierto)
        assertEquals(2025, recreado.uiState.value.anioSelector)
    }

    @Test
    fun `C5 exporta el mes visible con sus turnos`() = runTest(mainRule.dispatcher) {
        dao.upsert(Turno("2026-11-05", Tipos.MANANA.id))
        val vm = crear()
        vm.mesSiguiente()

        vm.exportarPdf()

        assertEquals(YearMonth.of(2026, 11) to mapOf(LocalDate.of(2026, 11, 5) to Tipos.MANANA), exportado)
    }
}
