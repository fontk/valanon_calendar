package com.valanon.calendar.ui.plantillas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.repository.PlantillasRepositorio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlantillaTrabajadoresUiState(
    val plantilla: Plantilla? = null,
    val trabajadores: List<Trabajador> = emptyList(),
    val cargando: Boolean = true,
    /** Worker row in inline edit mode; only one at a time. */
    val editandoId: Long? = null,
    val renombrando: Boolean = false,
    val confirmandoBorrado: Boolean = false,
    /** Team shifts (Turnos tab) of this template's workers: "y sus N turnos asignados". */
    val numeroAsignaciones: Int = 0,
) {
    /** The template was deleted (or never existed): the screen must go back to the list. */
    val cerrada: Boolean get() = !cargando && plantilla == null
}

/**
 * Detail of a "Trabajadores" template (spec 4b.3). The text fields are Compose state (so they
 * update synchronously) mirrored to [SavedStateHandle]; the rest of the UI state lives in the
 * handle too, so rotation and process death keep the open edit/rename/dialog.
 */
class PlantillaTrabajadoresViewModel(
    private val repositorio: PlantillasRepositorio,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Navigation argument `id` of `plantillas/{id}`. */
    val plantillaId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_ID)) { "Falta el argumento $ARG_ID" }

    private val editandoId: StateFlow<Long?> = savedStateHandle.getStateFlow(KEY_EDITANDO, null)
    private val renombrando: StateFlow<Boolean> = savedStateHandle.getStateFlow(
        KEY_RENOMBRANDO,
        // A freshly created template opens with its name in edit mode (navigation arg).
        savedStateHandle.get<Boolean>(ARG_RENOMBRAR) ?: false,
    )
    private val confirmandoBorrado: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_CONFIRMANDO, false)

    var borrador: String by mutableStateOf(savedStateHandle[KEY_BORRADOR] ?: "")
        private set
    var borradorEdicion: String by mutableStateOf(savedStateHandle[KEY_BORRADOR_EDICION] ?: "")
        private set
    var borradorNombre: String by mutableStateOf(savedStateHandle[KEY_BORRADOR_NOMBRE] ?: "")
        private set

    private val datos = combine(
        repositorio.plantilla(plantillaId),
        repositorio.trabajadores(plantillaId),
        repositorio.numeroAsignaciones(plantillaId),
    ) { p, t, n -> Triple(p, t, n) }

    /** Team shifts of each deleted worker, kept so "Deshacer" can restore them. */
    private val asignacionesEliminadas = mutableMapOf<Long, List<Asignacion>>()

    val uiState: StateFlow<PlantillaTrabajadoresUiState> =
        combine(datos, editandoId, renombrando, confirmandoBorrado) { (plantilla, trabajadores, asignaciones), editando, renom, conf ->
            PlantillaTrabajadoresUiState(
                plantilla = plantilla,
                trabajadores = trabajadores,
                cargando = false,
                // Drop an edit whose row no longer exists (e.g. deleted).
                editandoId = editando?.takeIf { id -> trabajadores.any { it.id == id } },
                renombrando = renom,
                confirmandoBorrado = conf,
                numeroAsignaciones = asignaciones,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = PlantillaTrabajadoresUiState(
                renombrando = renombrando.value,
                confirmandoBorrado = confirmandoBorrado.value,
            ),
        )

    init {
        // Opening straight into rename mode: prefill the field with the current name.
        if (renombrando.value && !savedStateHandle.contains(KEY_BORRADOR_NOMBRE)) {
            viewModelScope.launch {
                val nombre = repositorio.plantilla(plantillaId).filterNotNull().first().nombre
                actualizarBorradorNombre(nombre)
            }
        }
    }

    // --- Add workers ---

    fun actualizarBorrador(texto: String) {
        borrador = texto
        savedStateHandle[KEY_BORRADOR] = texto
    }

    /** Adds the draft (trimmed) at the end and clears the field. Blank drafts are ignored. */
    fun agregar() {
        val texto = borrador.trim()
        if (texto.isEmpty()) return
        actualizarBorrador("")
        viewModelScope.launch { repositorio.agregarTrabajador(plantillaId, texto) }
    }

    // --- Inline edit (one row at a time) ---

    /** Starts editing [trabajador]; any other row being edited is discarded without saving. */
    fun empezarEdicion(trabajador: Trabajador) {
        savedStateHandle[KEY_EDITANDO] = trabajador.id
        actualizarBorradorEdicion(trabajador.nombre)
    }

    fun actualizarBorradorEdicion(texto: String) {
        borradorEdicion = texto
        savedStateHandle[KEY_BORRADOR_EDICION] = texto
    }

    /** Saves the edited name; a blank name keeps the previous one. Leaves edit mode either way. */
    fun guardarEdicion() {
        val id = editandoId.value ?: return
        val texto = borradorEdicion
        cancelarEdicion()
        viewModelScope.launch { repositorio.renombrarTrabajador(id, texto) }
    }

    fun cancelarEdicion() {
        savedStateHandle[KEY_EDITANDO] = null
        actualizarBorradorEdicion("")
    }

    // --- Delete worker + undo ---

    fun eliminar(trabajador: Trabajador) {
        if (editandoId.value == trabajador.id) cancelarEdicion()
        viewModelScope.launch {
            asignacionesEliminadas[trabajador.id] = repositorio.eliminarTrabajador(trabajador.id)
        }
    }

    /** Undo of [eliminar]: puts the worker back with its id and position, and its team shifts. */
    fun deshacerEliminar(trabajador: Trabajador) {
        viewModelScope.launch {
            repositorio.restaurarTrabajador(trabajador, asignacionesEliminadas.remove(trabajador.id).orEmpty())
        }
    }

    // --- Rename template ---

    fun empezarRenombrar() {
        actualizarBorradorNombre(uiState.value.plantilla?.nombre ?: "")
        savedStateHandle[KEY_RENOMBRANDO] = true
    }

    fun actualizarBorradorNombre(texto: String) {
        borradorNombre = texto
        savedStateHandle[KEY_BORRADOR_NOMBRE] = texto
    }

    /** Saves the new name; a blank name keeps the previous one. Leaves rename mode either way. */
    fun guardarNombre() {
        val texto = borradorNombre
        savedStateHandle[KEY_RENOMBRANDO] = false
        viewModelScope.launch { repositorio.renombrarPlantilla(plantillaId, texto) }
    }

    // --- Delete template ---

    fun pedirBorrarPlantilla() {
        savedStateHandle[KEY_CONFIRMANDO] = true
    }

    fun cancelarBorrarPlantilla() {
        savedStateHandle[KEY_CONFIRMANDO] = false
    }

    /**
     * Deletes the template and its workers (cascade). The template flow then emits `null`,
     * [PlantillaTrabajadoresUiState.cerrada] becomes true and the screen navigates back.
     */
    fun borrarPlantilla() {
        savedStateHandle[KEY_CONFIRMANDO] = false
        viewModelScope.launch { repositorio.borrarPlantilla(plantillaId) }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_RENOMBRAR = "renombrar"
        private const val KEY_EDITANDO = "editandoId"
        private const val KEY_RENOMBRANDO = "renombrando"
        private const val KEY_CONFIRMANDO = "confirmandoBorrado"
        private const val KEY_BORRADOR = "borrador"
        private const val KEY_BORRADOR_EDICION = "borradorEdicion"
        private const val KEY_BORRADOR_NOMBRE = "borradorNombre"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                PlantillaTrabajadoresViewModel(app.container.plantillasRepositorio, createSavedStateHandle())
            }
        }
    }
}
