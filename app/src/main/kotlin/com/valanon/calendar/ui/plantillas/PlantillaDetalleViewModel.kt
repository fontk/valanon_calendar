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
import com.valanon.calendar.data.model.ElementoPlantilla
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.repository.PlantillasRepositorio
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlantillaDetalleUiState(
    val plantilla: Plantilla? = null,
    /** Workers or preset tasks, depending on the template's type. */
    val elementos: List<ElementoPlantilla> = emptyList(),
    val cargando: Boolean = true,
    /** Row in inline edit mode; only one at a time. */
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
 * Detail of a template of any type (spec 4b.3): the same screen lists workers or preset tasks.
 * The text fields are Compose state (so they update synchronously) mirrored to
 * [SavedStateHandle]; the rest of the UI state lives in the handle too, so rotation and process
 * death keep the open edit/rename/dialog.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlantillaDetalleViewModel(
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

    /** One query for the template, shared by the UI state, its items and the actions' type. */
    private val plantilla: Flow<Plantilla?> = repositorio.plantilla(plantillaId)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** The items live in a table per type, known once the template is read. */
    private val elementos: Flow<List<ElementoPlantilla>> = plantilla
        .map { it?.tipo }
        .distinctUntilChanged()
        .flatMapLatest { tipo -> if (tipo == null) flowOf(emptyList()) else repositorio.elementos(tipo, plantillaId) }

    private val datos = combine(plantilla, elementos, repositorio.numeroAsignaciones(plantillaId)) { p, e, n -> Triple(p, e, n) }

    /**
     * Pending deletes by item id. The job's result is the worker's team shifts, read in the same
     * transaction as the delete, so "Deshacer" can wait for it and restore them.
     */
    private val eliminaciones = mutableMapOf<Long, Deferred<List<Asignacion>>>()

    val uiState: StateFlow<PlantillaDetalleUiState> =
        combine(datos, editandoId, renombrando, confirmandoBorrado) { (plantilla, elementos, asignaciones), editando, renom, conf ->
            PlantillaDetalleUiState(
                plantilla = plantilla,
                elementos = elementos,
                cargando = false,
                // Drop an edit whose row no longer exists (e.g. deleted).
                editandoId = editando?.takeIf { id -> elementos.any { it.id == id } },
                renombrando = renom,
                confirmandoBorrado = conf,
                numeroAsignaciones = asignaciones,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = PlantillaDetalleUiState(
                renombrando = renombrando.value,
                confirmandoBorrado = confirmandoBorrado.value,
            ),
        )

    init {
        // Opening straight into rename mode: prefill the field with the current name.
        if (renombrando.value && !savedStateHandle.contains(KEY_BORRADOR_NOMBRE)) {
            viewModelScope.launch { actualizarBorradorNombre(plantilla.filterNotNull().first().nombre) }
        }
    }

    /** Type of this template, waiting for the first read if needed. */
    private suspend fun tipo(): TipoPlantilla = plantilla.filterNotNull().first().tipo

    // --- Add items ---

    fun actualizarBorrador(texto: String) {
        borrador = texto
        savedStateHandle[KEY_BORRADOR] = texto
    }

    /** Adds the draft (trimmed) at the end and clears the field. Blank drafts are ignored. */
    fun agregar() {
        val texto = borrador.trim()
        if (texto.isEmpty()) return
        actualizarBorrador("")
        viewModelScope.launch { repositorio.agregarElemento(tipo(), plantillaId, texto) }
    }

    // --- Inline edit (one row at a time) ---

    /** Starts editing [elemento]; any other row being edited is discarded without saving. */
    fun empezarEdicion(elemento: ElementoPlantilla) {
        savedStateHandle[KEY_EDITANDO] = elemento.id
        actualizarBorradorEdicion(elemento.texto)
    }

    fun actualizarBorradorEdicion(texto: String) {
        borradorEdicion = texto
        savedStateHandle[KEY_BORRADOR_EDICION] = texto
    }

    /** Saves the edited text; a blank text keeps the previous one. Leaves edit mode either way. */
    fun guardarEdicion() {
        val id = editandoId.value ?: return
        val texto = borradorEdicion
        cancelarEdicion()
        viewModelScope.launch { repositorio.cambiarTextoElemento(tipo(), id, texto) }
    }

    fun cancelarEdicion() {
        savedStateHandle[KEY_EDITANDO] = null
        actualizarBorradorEdicion("")
    }

    // --- Delete item + undo ---

    fun eliminar(elemento: ElementoPlantilla) {
        if (editandoId.value == elemento.id) cancelarEdicion()
        eliminaciones[elemento.id] = viewModelScope.async { repositorio.eliminarElemento(tipo(), elemento.id) }
    }

    /** Undo of [eliminar]: puts the item back with its id and position, and a worker's team shifts. */
    fun deshacerEliminar(elemento: ElementoPlantilla) {
        val eliminacion = eliminaciones.remove(elemento.id) ?: return
        viewModelScope.launch {
            // Waits for the delete if it is still running, instead of racing it.
            repositorio.restaurarElemento(tipo(), elemento, eliminacion.await())
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
     * Deletes the template and its items (cascade). The template flow then emits `null`,
     * [PlantillaDetalleUiState.cerrada] becomes true and the screen navigates back.
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
                PlantillaDetalleViewModel(app.container.plantillasRepositorio, createSavedStateHandle())
            }
        }
    }
}
