package com.valanon.calendar.ui.calendario

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.data.repository.TurnosRepositorio
import com.valanon.calendar.testing.FakeTurnoDao
import com.valanon.calendar.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private val hoy = LocalDate.of(2026, 10, 17)
    private val reloj = Clock.fixed(hoy.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant(), ZoneId.of("Europe/Madrid"))
    private var exportado: Pair<YearMonth, Map<LocalDate, TipoTurno>>? = null

    private fun crear(handle: SavedStateHandle = SavedStateHandle()) = CalendarioViewModel(
        repositorio = TurnosRepositorio(dao),
        generadorPdf = { mes, turnos ->
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
        dao.upsert(Turno("2026-10-31", TipoTurno.NOCHE))
        dao.upsert(Turno("2026-11-01", TipoTurno.LIBRE))
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)

        assertEquals(mapOf(LocalDate.of(2026, 10, 31) to TipoTurno.NOCHE), vm.uiState.value.turnos)

        vm.mesSiguiente()
        assertEquals(mapOf(LocalDate.of(2026, 11, 1) to TipoTurno.LIBRE), vm.uiState.value.turnos)
    }

    @Test
    fun `C3 seleccionar dia, elegir turno y cerrar`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        val dia = LocalDate.of(2026, 10, 20)

        vm.seleccionarDia(dia)
        assertEquals(dia, vm.uiState.value.diaSeleccionado)

        vm.asignarTurno(dia, TipoTurno.TARDE)
        vm.cerrarSelector()
        assertEquals(TipoTurno.TARDE, vm.uiState.value.turnos[dia])
        assertNull(vm.uiState.value.diaSeleccionado)

        vm.seleccionarDia(dia)
        vm.asignarTurno(dia, TipoTurno.VACACIONES)
        assertEquals(TipoTurno.VACACIONES, vm.uiState.value.turnos[dia])
    }

    @Test
    fun `el mes visible sobrevive a la recreacion del ViewModel`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle()
        crear(handle).mesSiguiente()

        val recreado = crear(handle)
        recreado.uiState.launchIn(backgroundScope)
        assertEquals(YearMonth.of(2026, 11), recreado.uiState.value.mes)
    }

    @Test
    fun `C5 exporta el mes visible con sus turnos`() = runTest(mainRule.dispatcher) {
        dao.upsert(Turno("2026-11-05", TipoTurno.MANANA))
        val vm = crear()
        vm.mesSiguiente()

        vm.exportarPdf()

        assertEquals(YearMonth.of(2026, 11) to mapOf(LocalDate.of(2026, 11, 5) to TipoTurno.MANANA), exportado)
    }
}
