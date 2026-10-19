package com.valanon.calendar.ui.turnos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.repository.AsignacionesRepositorio
import com.valanon.calendar.ui.componentes.NavegacionMes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class TurnosUiState(
    val mes: YearMonth,
    val hoy: LocalDate,
    val cargando: Boolean = true,
    /** Active "Trabajadores" template (saved in `ajustes`, or the first one). */
    val plantilla: PlantillaConRecuento? = null,
    /** Every "Trabajadores" template, for the "¿Qué equipo quieres ver?" sheet. */
    val plantillas: List<PlantillaConRecuento> = emptyList(),
    val trabajadores: List<Trabajador> = emptyList(),
    /** Shifts of the visible month, by day and worker id. */
    val asignaciones: Map<LocalDate, Map<Long, TipoTurno>> = emptyMap(),
    /** Configured shift types, in order: legend and picker options (spec 3c). */
    val tipos: List<TipoTurno> = emptyList(),
    val diaAbierto: LocalDate? = null,
    val editando: Boolean = false,
    /** Worker whose shift picker replaces the "Equipo del día" content. */
    val trabajadorSeleccionadoId: Long? = null,
    val hojaPlantillasAbierta: Boolean = false,
    val selectorMesAbierto: Boolean = false,
    val anioSelector: Int = mes.year,
) {
    val sinPlantillas: Boolean get() = !cargando && plantilla == null

    /** "12 de 31 días con turnos". */
    val diasConTurnos: Int get() = asignaciones.count { (_, porTrabajador) -> porTrabajador.isNotEmpty() }

    fun turnosDe(fecha: LocalDate): Map<Long, TipoTurno> = asignaciones[fecha].orEmpty()

    /** Shifts of [fecha] in template order, unassigned workers skipped (the cell's dots). */
    fun puntosDe(fecha: LocalDate): List<TipoTurno> = turnosDe(fecha).let { dia -> trabajadores.mapNotNull { dia[it.id] } }

    val trabajadorSeleccionado: Trabajador? get() = trabajadores.firstOrNull { it.id == trabajadorSeleccionadoId }
}

/** Data of one template and month, tagged so a switch never shows another month's dots. */
private data class DatosEquipo(
    val plantillaId: Long?,
    val mes: YearMonth,
    val trabajadores: List<Trabajador> = emptyList(),
    val asignaciones: Map<LocalDate, Map<Long, TipoTurno>> = emptyMap(),
)

private data class EstadoHojas(
    val dia: String?,
    val editando: Boolean,
    val trabajador: Long?,
    val hojaPlantillas: Boolean,
)

/**
 * Turnos tab (spec 3b): the team's shifts of the active template, month by month. Has its
 * own visible month (keys prefixed "turnos"), independent from Calendario, and never touches
 * the `turnos` table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TurnosViewModel(
    private val repositorio: AsignacionesRepositorio,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    /** Re-read on every resume ([refrescarHoy]): tab ViewModels can outlive midnight. */
    private val hoy = MutableStateFlow(LocalDate.now(clock))
    private val navegacion = NavegacionMes(savedStateHandle, clock, prefijo = PREFIJO)

    private val hojas = combine(
        savedStateHandle.getStateFlow<String?>(KEY_DIA, null),
        savedStateHandle.getStateFlow(KEY_EDITANDO, false),
        savedStateHandle.getStateFlow<Long?>(KEY_TRABAJADOR, null),
        savedStateHandle.getStateFlow(KEY_HOJA_PLANTILLAS, false),
    ) { dia, editando, trabajador, hojaPlantillas -> EstadoHojas(dia, editando, trabajador, hojaPlantillas) }

    /** One subscription to the templates + `ajustes`, shared by the id below and the UI state. */
    private val catalogo = repositorio.catalogo
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), replay = 1)

    private val plantillaId = catalogo.map { it.activa?.plantilla?.id }.distinctUntilChanged()

    private val datos = combine(plantillaId, navegacion.mesTexto) { id, mes -> id to YearMonth.parse(mes) }
        .flatMapLatest { (id, mes) ->
            if (id == null) {
                flowOf(DatosEquipo(null, mes))
            } else {
                combine(repositorio.trabajadores(id), repositorio.asignacionesMes(id, mes)) { t, a ->
                    DatosEquipo(id, mes, t, a)
                }
            }
        }

    val uiState: StateFlow<TurnosUiState> =
        combine(
            catalogo,
            datos,
            hojas,
            navegacion.mesTexto,
            combine(navegacion.selector, hoy, repositorio.tipos) { selector, dia, tipos -> Triple(selector, dia, tipos) },
        ) { catalogo, d, h, mesTexto, (selector, hoyActual, tipos) ->
            val (selectorAbierto, anio) = selector
            val activa = catalogo.activa
            val mes = YearMonth.parse(mesTexto)
            val vigentes = d.plantillaId == activa?.plantilla?.id && d.mes == mes
            TurnosUiState(
                mes = mes,
                hoy = hoyActual,
                cargando = false,
                plantilla = activa,
                plantillas = catalogo.todas,
                trabajadores = if (vigentes) d.trabajadores else emptyList(),
                asignaciones = if (vigentes) d.asignaciones else emptyMap(),
                tipos = tipos,
                diaAbierto = h.dia?.let(LocalDate::parse),
                editando = h.editando,
                trabajadorSeleccionadoId = h.trabajador,
                hojaPlantillasAbierta = h.hojaPlantillas,
                selectorMesAbierto = selectorAbierto,
                anioSelector = anio,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = TurnosUiState(mes = navegacion.mes, hoy = hoy.value),
        )

    // --- Month navigation (same behaviour as Calendario, own month) ---

    /** Called when the screen resumes, so "today" is right after midnight or days in background. */
    fun refrescarHoy() {
        hoy.value = LocalDate.now(clock)
    }

    fun mesAnterior() = navegacion.mesAnterior()
    fun mesSiguiente() = navegacion.mesSiguiente()
    fun abrirSelectorMes() = navegacion.abrirSelector()
    fun cerrarSelectorMes() = navegacion.cerrarSelector()
    fun anioSelectorAnterior() = navegacion.anioAnterior()
    fun anioSelectorSiguiente() = navegacion.anioSiguiente()
    fun irAMes(anio: Int, mes: Int) = navegacion.irAMes(anio, mes)
    fun irAHoy() = navegacion.irAHoy()

    // --- "Equipo del día" sheet (spec 3b.3) ---

    /** Opens the day in read mode. */
    fun abrirDia(fecha: LocalDate) {
        savedStateHandle[KEY_DIA] = fecha.toString()
        savedStateHandle[KEY_EDITANDO] = false
        savedStateHandle[KEY_TRABAJADOR] = null
    }

    /** Closing the sheet (button, swipe, scrim, back) also leaves edit mode. */
    fun cerrarDia() {
        savedStateHandle[KEY_DIA] = null
        savedStateHandle[KEY_EDITANDO] = false
        savedStateHandle[KEY_TRABAJADOR] = null
    }

    fun empezarEdicion() {
        savedStateHandle[KEY_EDITANDO] = true
    }

    /** "Listo": back to read mode. Changes were already saved when picked. */
    fun terminarEdicion() {
        savedStateHandle[KEY_EDITANDO] = false
        savedStateHandle[KEY_TRABAJADOR] = null
    }

    /** Swaps the sheet content for [trabajadorId]'s shift picker. */
    fun abrirSelectorTrabajador(trabajadorId: Long) {
        savedStateHandle[KEY_TRABAJADOR] = trabajadorId
    }

    /** Back to "Equipo del día" (still in edit mode) without changes. */
    fun volverAEquipo() {
        savedStateHandle[KEY_TRABAJADOR] = null
    }

    /** Saves [tipo] for the selected worker on the open day immediately, then back to the team. */
    fun asignar(tipo: TipoTurno) {
        val dia = savedStateHandle.get<String>(KEY_DIA)?.let(LocalDate::parse) ?: return
        val trabajador = savedStateHandle.get<Long>(KEY_TRABAJADOR) ?: return
        volverAEquipo()
        viewModelScope.launch { repositorio.asignar(dia, trabajador, tipo) }
    }

    /** "Quitar turno": the selected worker is left unassigned that day. */
    fun quitarTurno() {
        val dia = savedStateHandle.get<String>(KEY_DIA)?.let(LocalDate::parse) ?: return
        val trabajador = savedStateHandle.get<Long>(KEY_TRABAJADOR) ?: return
        volverAEquipo()
        viewModelScope.launch { repositorio.quitar(dia, trabajador) }
    }

    // --- Active template (spec 3b.4) ---

    fun abrirHojaPlantillas() {
        savedStateHandle[KEY_HOJA_PLANTILLAS] = true
    }

    fun cerrarHojaPlantillas() {
        savedStateHandle[KEY_HOJA_PLANTILLAS] = false
    }

    /** Activates [plantillaId], remembered in `ajustes` across app restarts. */
    fun elegirPlantilla(plantillaId: Long) {
        cerrarHojaPlantillas()
        viewModelScope.launch { repositorio.elegirPlantilla(plantillaId) }
    }

    companion object {
        private const val PREFIJO = "turnos"
        private const val KEY_DIA = "turnosDiaAbierto"
        private const val KEY_EDITANDO = "turnosEditando"
        private const val KEY_TRABAJADOR = "turnosTrabajadorSeleccionado"
        private const val KEY_HOJA_PLANTILLAS = "turnosHojaPlantillas"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                TurnosViewModel(app.container.asignacionesRepositorio, createSavedStateHandle())
            }
        }
    }
}
