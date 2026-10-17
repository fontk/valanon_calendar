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
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarioViewModel(
    private val repositorio: TurnosRepositorio,
    private val generadorPdf: GeneradorPdf,
    private val savedStateHandle: SavedStateHandle,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val hoy: LocalDate = LocalDate.now(clock)

    // Visible month and open sheet survive rotation and process death.
    private val mes: StateFlow<String> = savedStateHandle.getStateFlow(KEY_MES, YearMonth.from(hoy).toString())
    private val diaSeleccionado: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_DIA, null)

    /** Shifts tagged with the month they belong to, so a month switch never shows stale colours. */
    private val turnosDelMes = mes.flatMapLatest { valor ->
        val m = YearMonth.parse(valor)
        repositorio.observarMes(m).map { m to it }
    }

    val uiState: StateFlow<CalendarioUiState> =
        combine(mes, diaSeleccionado, turnosDelMes) { mesTexto, dia, (mesTurnos, turnos) ->
            val m = YearMonth.parse(mesTexto)
            CalendarioUiState(
                mes = m,
                hoy = hoy,
                turnos = if (mesTurnos == m) turnos else emptyMap(),
                diaSeleccionado = dia?.let(LocalDate::parse),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CalendarioUiState(mes = YearMonth.parse(mes.value), hoy = hoy),
        )

    fun mesAnterior() = cambiarMes(-1)

    fun mesSiguiente() = cambiarMes(1)

    private fun cambiarMes(delta: Long) {
        savedStateHandle[KEY_MES] = YearMonth.parse(mes.value).plusMonths(delta).toString()
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
