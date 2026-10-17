package com.valanon.calendar.ui.calendario

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.data.repository.TurnosRepositorio
import com.valanon.calendar.pdf.GeneradorPdf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class CalendarioUiState(
    val mes: YearMonth,
    val hoy: LocalDate,
    val turnos: Map<LocalDate, TipoTurno> = emptyMap(),
    val diaSeleccionado: LocalDate? = null,
    /** Month/year picker sheet (spec 3.1.1). */
    val selectorMesAbierto: Boolean = false,
    /** Year shown in the picker; its arrows change only this, not [mes]. */
    val anioSelector: Int = mes.year,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarioViewModel(
    private val repositorio: TurnosRepositorio,
    private val generadorPdf: GeneradorPdf,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val hoy: LocalDate = LocalDate.now(clock)

    // Visible month and open sheets survive rotation and process death.
    private val mes: StateFlow<String> = savedStateHandle.getStateFlow(KEY_MES, YearMonth.from(hoy).toString())
    private val diaSeleccionado: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_DIA, null)
    private val selectorMesAbierto: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_SELECTOR_MES, false)
    private val anioSelector: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_ANIO_SELECTOR, hoy.year)

    /** Shifts tagged with the month they belong to, so a month switch never shows stale colours. */
    private val turnosDelMes = mes.flatMapLatest { valor ->
        val m = YearMonth.parse(valor)
        repositorio.observarMes(m).map { m to it }
    }

    val uiState: StateFlow<CalendarioUiState> =
        combine(mes, diaSeleccionado, turnosDelMes, selectorMesAbierto, anioSelector) {
                mesTexto, dia, (mesTurnos, turnos), abierto, anio ->
            val m = YearMonth.parse(mesTexto)
            CalendarioUiState(
                mes = m,
                hoy = hoy,
                turnos = if (mesTurnos == m) turnos else emptyMap(),
                diaSeleccionado = dia?.let(LocalDate::parse),
                selectorMesAbierto = abierto,
                anioSelector = anio,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CalendarioUiState(
                mes = YearMonth.parse(mes.value),
                hoy = hoy,
                selectorMesAbierto = selectorMesAbierto.value,
                anioSelector = anioSelector.value,
            ),
        )

    fun mesAnterior() = cambiarMes(-1)

    fun mesSiguiente() = cambiarMes(1)

    private fun cambiarMes(delta: Long) {
        savedStateHandle[KEY_MES] = YearMonth.parse(mes.value).plusMonths(delta).toString()
    }

    // --- Month/year picker (spec 3.1.1, story C2b) ---

    /** Opens the picker showing the year of the visible month. */
    fun abrirSelectorMes() {
        savedStateHandle[KEY_ANIO_SELECTOR] = YearMonth.parse(mes.value).year
        savedStateHandle[KEY_SELECTOR_MES] = true
    }

    fun cerrarSelectorMes() {
        savedStateHandle[KEY_SELECTOR_MES] = false
    }

    fun anioSelectorAnterior() {
        savedStateHandle[KEY_ANIO_SELECTOR] = anioSelector.value - 1
    }

    fun anioSelectorSiguiente() {
        savedStateHandle[KEY_ANIO_SELECTOR] = anioSelector.value + 1
    }

    /**
     * Makes [anio]-[mes] (1..12) the visible month and closes the picker. Writes the same
     * [KEY_MES] as the arrows, so `turnosDelMes` reloads the shifts by itself.
     */
    fun irAMes(anio: Int, mes: Int) {
        savedStateHandle[KEY_MES] = YearMonth.of(anio, mes).toString()
        cerrarSelectorMes()
    }

    /** "Hoy": back to the device's current month, and close the picker. */
    fun irAHoy() {
        val actual = YearMonth.now(clock)
        irAMes(actual.year, actual.monthValue)
    }

    fun seleccionarDia(fecha: LocalDate) {
        savedStateHandle[KEY_DIA] = fecha.toString()
    }

    fun cerrarSelector() {
        savedStateHandle[KEY_DIA] = null
    }

    /** Saves the shift of [fecha] (`INSERT OR REPLACE`). The sheet closes itself afterwards. */
    fun asignarTurno(fecha: LocalDate, tipo: TipoTurno) {
        viewModelScope.launch { repositorio.asignar(fecha, tipo) }
    }

    /** Generates the PDF of the visible month and returns the file. */
    suspend fun exportarPdf(): File {
        val m = YearMonth.parse(mes.value)
        return generadorPdf.exportar(m, repositorio.obtenerMes(m))
    }

    companion object {
        private const val KEY_MES = "mes"
        private const val KEY_DIA = "dia"
        private const val KEY_SELECTOR_MES = "selectorMesAbierto"
        private const val KEY_ANIO_SELECTOR = "anioSelector"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                CalendarioViewModel(
                    repositorio = app.container.turnosRepositorio,
                    generadorPdf = app.container.exportadorPdf,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
