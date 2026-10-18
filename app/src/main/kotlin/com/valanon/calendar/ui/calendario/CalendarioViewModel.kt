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
import com.valanon.calendar.ui.componentes.NavegacionMes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    /** Re-read on every resume ([refrescarHoy]): tab ViewModels can outlive midnight. */
    private val hoy = MutableStateFlow(LocalDate.now(clock))

    // Visible month, picker and open sheet survive rotation and process death.
    private val navegacion = NavegacionMes(savedStateHandle, clock)
    private val diaSeleccionado: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_DIA, null)

    /** Shifts tagged with the month they belong to, so a month switch never shows stale colours. */
    private val turnosDelMes = navegacion.mesTexto.flatMapLatest { valor ->
        val m = YearMonth.parse(valor)
        repositorio.observarMes(m).map { m to it }
    }

    val uiState: StateFlow<CalendarioUiState> =
        combine(navegacion.mesTexto, diaSeleccionado, turnosDelMes, navegacion.selector, hoy) {
                mesTexto, dia, (mesTurnos, turnos), (abierto, anio), hoyActual ->
            val m = YearMonth.parse(mesTexto)
            CalendarioUiState(
                mes = m,
                hoy = hoyActual,
                turnos = if (mesTurnos == m) turnos else emptyMap(),
                diaSeleccionado = dia?.let(LocalDate::parse),
                selectorMesAbierto = abierto,
                anioSelector = anio,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CalendarioUiState(
                mes = navegacion.mes,
                hoy = hoy.value,
                selectorMesAbierto = navegacion.selectorAbierto.value,
                anioSelector = navegacion.anioSelector.value,
            ),
        )

    /** Called when the screen resumes, so "today" is right after midnight or days in background. */
    fun refrescarHoy() {
        hoy.value = LocalDate.now(clock)
    }

    fun mesAnterior() = navegacion.mesAnterior()

    fun mesSiguiente() = navegacion.mesSiguiente()

    // --- Month/year picker (spec 3.1.1, story C2b), shared with Turnos via NavegacionMes ---

    fun abrirSelectorMes() = navegacion.abrirSelector()

    fun cerrarSelectorMes() = navegacion.cerrarSelector()

    fun anioSelectorAnterior() = navegacion.anioAnterior()

    fun anioSelectorSiguiente() = navegacion.anioSiguiente()

    /** Makes [anio]-[mes] (1..12) the visible month and closes the picker; shifts reload by themselves. */
    fun irAMes(anio: Int, mes: Int) = navegacion.irAMes(anio, mes)

    fun irAHoy() = navegacion.irAHoy()

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
        val m = navegacion.mes
        return generadorPdf.exportar(m, repositorio.obtenerMes(m))
    }

    companion object {
        private const val KEY_DIA = "dia"

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
