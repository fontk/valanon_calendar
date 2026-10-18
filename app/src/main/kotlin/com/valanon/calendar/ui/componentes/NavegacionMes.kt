package com.valanon.calendar.ui.componentes

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.YearMonth

/**
 * Visible month + month/year picker state (spec 3.1, 3.1.1), shared by the Calendario and
 * Turnos ViewModels. Everything lives in [SavedStateHandle] under keys built from [prefijo]
 * so each tab keeps its own month ("" for Calendario's historical keys, "turnos" for Turnos).
 */
class NavegacionMes(
    private val handle: SavedStateHandle,
    private val clock: Clock,
    prefijo: String = "",
) {
    private val claveMes = clave(prefijo, "mes")
    private val claveAbierto = clave(prefijo, "selectorMesAbierto")
    private val claveAnio = clave(prefijo, "anioSelector")

    /** Visible month as `YearMonth.toString()` ("2026-10"). */
    val mesTexto: StateFlow<String> = handle.getStateFlow(claveMes, YearMonth.now(clock).toString())
    val selectorAbierto: StateFlow<Boolean> = handle.getStateFlow(claveAbierto, false)
    val anioSelector: StateFlow<Int> = handle.getStateFlow(claveAnio, YearMonth.now(clock).year)

    val mes: YearMonth get() = YearMonth.parse(mesTexto.value)

    /** Picker state as one flow: (open, shown year). */
    val selector: Flow<Pair<Boolean, Int>> = combine(selectorAbierto, anioSelector) { a, y -> a to y }

    fun mesAnterior() = irA(mes.minusMonths(1))

    fun mesSiguiente() = irA(mes.plusMonths(1))

    /** Opens the picker showing the year of the visible month. */
    fun abrirSelector() {
        handle[claveAnio] = mes.year
        handle[claveAbierto] = true
    }

    fun cerrarSelector() {
        handle[claveAbierto] = false
    }

    /** The picker's year arrows change only the shown year, never the visible month. */
    fun anioAnterior() {
        handle[claveAnio] = anioSelector.value - 1
    }

    fun anioSiguiente() {
        handle[claveAnio] = anioSelector.value + 1
    }

    /** Makes [anio]-[mesDelAnio] (1..12) the visible month and closes the picker. */
    fun irAMes(anio: Int, mesDelAnio: Int) {
        irA(YearMonth.of(anio, mesDelAnio))
        cerrarSelector()
    }

    /** "Hoy": back to the device's current month, and close the picker. */
    fun irAHoy() {
        val actual = YearMonth.now(clock)
        irAMes(actual.year, actual.monthValue)
    }

    private fun irA(destino: YearMonth) {
        handle[claveMes] = destino.toString()
    }

    private companion object {
        fun clave(prefijo: String, nombre: String) =
            if (prefijo.isEmpty()) nombre else prefijo + nombre.replaceFirstChar { it.uppercaseChar() }
    }
}
