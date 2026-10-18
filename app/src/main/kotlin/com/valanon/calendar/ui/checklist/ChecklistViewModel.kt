package com.valanon.calendar.ui.checklist

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
import com.valanon.calendar.data.local.entity.ListaConProgreso
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.repository.ListasChecklist
import com.valanon.calendar.data.repository.ListasRepositorio
import com.valanon.calendar.data.repository.PlantillasRepositorio
import com.valanon.calendar.data.repository.TareasRepositorio
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val listas: List<ListaConProgreso> = emptyList(),
    val activa: ListaConProgreso? = null,
    /** Tasks of [activa], in order. */
    val tareas: List<Tarea> = emptyList(),
    /** "Tareas" templates offered by the "Nueva lista" sheet. */
    val plantillasTareas: List<PlantillaConRecuento> = emptyList(),
    val cargando: Boolean = true,
    val renombrando: Boolean = false,
    val hojaNuevaAbierta: Boolean = false,
    val confirmandoBorrado: Boolean = false,
) {
    val total: Int get() = tareas.size
    val hechas: Int get() = tareas.count { it.hecha }

    /** No lists at all: the tab shows only the "Sin listas todavía" empty state. */
    val sinListas: Boolean get() = !cargando && listas.isEmpty()
}

/**
 * Checklist tab with several lists (spec 4). Every task action works on the active list. The
 * text fields are Compose state (so they update synchronously) mirrored to [SavedStateHandle];
 * the open sheet, dialog and rename survive rotation and process death too.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModel(
    private val listasRepositorio: ListasRepositorio,
    private val tareasRepositorio: TareasRepositorio,
    plantillasRepositorio: PlantillasRepositorio,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Text of the "Nueva tarea…" field. */
    var borrador: String by mutableStateOf(savedStateHandle[KEY_BORRADOR] ?: "")
        private set

    /** Text of the list-name field while renaming. */
    var borradorNombre: String by mutableStateOf(savedStateHandle[KEY_BORRADOR_NOMBRE] ?: "")
        private set

    /** Lists + active one: one query shared by the UI state, the tasks and the actions. */
    private val listas: Flow<ListasChecklist> = listasRepositorio.listas
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /**
     * Tasks of the active list, tagged with its id so a list switch never shows the previous
     * list's tasks under the new chip for a frame.
     */
    private val tareas: Flow<Pair<Long?, List<Tarea>>> = listas
        .map { it.activa?.lista?.id }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) flowOf(null to emptyList()) else tareasRepositorio.tareas(id).map { id to it }
        }

    private val plantillasTareas: Flow<List<PlantillaConRecuento>> =
        plantillasRepositorio.plantillas.map { l -> l.filter { it.plantilla.tipo == TipoPlantilla.TAREAS } }

    private val interfaz = combine(
        savedStateHandle.getStateFlow(KEY_RENOMBRANDO, false),
        savedStateHandle.getStateFlow(KEY_HOJA_NUEVA, false),
        savedStateHandle.getStateFlow(KEY_CONFIRMANDO, false),
    ) { renombrando, hoja, confirmando -> Triple(renombrando, hoja, confirmando) }

    val uiState: StateFlow<ChecklistUiState> =
        combine(listas, tareas, plantillasTareas, interfaz) { l, (idTareas, t), presets, (renombrando, hoja, confirmando) ->
            val activa = l.activa
            ChecklistUiState(
                listas = l.todas,
                activa = activa,
                tareas = if (idTareas == activa?.lista?.id) t else emptyList(),
                plantillasTareas = presets,
                cargando = idTareas != activa?.lista?.id,
                renombrando = renombrando && activa != null,
                hojaNuevaAbierta = hoja,
                confirmandoBorrado = confirmando && activa != null,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ChecklistUiState(),
        )

    /** Pending task deletes by id, so "Deshacer" waits for the delete instead of racing it. */
    private val eliminaciones = mutableMapOf<Long, Deferred<Unit>>()

    private suspend fun activa(): ListaConProgreso? = listas.first().activa

    // --- Lists ---

    /** Tapping a chip: makes it active and drops what was typed in the new-task field. */
    fun activar(listaId: Long) {
        actualizarBorrador("")
        savedStateHandle[KEY_RENOMBRANDO] = false
        viewModelScope.launch { listasRepositorio.activar(listaId) }
    }

    fun abrirHojaNueva() {
        savedStateHandle[KEY_HOJA_NUEVA] = true
    }

    fun cerrarHojaNueva() {
        savedStateHandle[KEY_HOJA_NUEVA] = false
    }

    /** New list from a "Tareas" template (copies its tasks unchecked); it becomes the active one. */
    fun crearDesdePlantilla(plantillaId: Long) = crear { listasRepositorio.crearDesdePlantilla(plantillaId) }

    /** New empty list "Lista N" ([nombre] formats N); it becomes the active one. */
    fun crearEnBlanco(nombre: (Int) -> String) = crear { listasRepositorio.crearEnBlanco(nombre) }

    private fun crear(accion: suspend () -> Unit) {
        cerrarHojaNueva()
        actualizarBorrador("")
        savedStateHandle[KEY_RENOMBRANDO] = false
        viewModelScope.launch { accion() }
    }

    fun empezarRenombrar() {
        actualizarBorradorNombre(uiState.value.activa?.lista?.nombre ?: "")
        savedStateHandle[KEY_RENOMBRANDO] = true
    }

    fun actualizarBorradorNombre(texto: String) {
        borradorNombre = texto
        savedStateHandle[KEY_BORRADOR_NOMBRE] = texto
    }

    /** Saves the new name of the active list; a blank name keeps the previous one. */
    fun guardarNombre() {
        val texto = borradorNombre
        savedStateHandle[KEY_RENOMBRANDO] = false
        viewModelScope.launch { activa()?.let { listasRepositorio.renombrar(it.lista.id, texto) } }
    }

    /** "Reiniciar": unchecks the active list's tasks only. */
    fun reiniciar() {
        viewModelScope.launch { activa()?.let { listasRepositorio.reiniciar(it.lista.id) } }
    }

    fun pedirBorrarLista() {
        savedStateHandle[KEY_CONFIRMANDO] = true
    }

    fun cancelarBorrarLista() {
        savedStateHandle[KEY_CONFIRMANDO] = false
    }

    /** Deletes the active list and its tasks; the neighbouring list becomes active. */
    fun borrarLista() {
        savedStateHandle[KEY_CONFIRMANDO] = false
        savedStateHandle[KEY_RENOMBRANDO] = false
        actualizarBorrador("")
        viewModelScope.launch { activa()?.let { listasRepositorio.borrar(it.lista.id) } }
    }

    // --- Tasks of the active list ---

    fun actualizarBorrador(texto: String) {
        borrador = texto
        savedStateHandle[KEY_BORRADOR] = texto
    }

    /** Adds the draft (trimmed) at the end of the active list and clears the field. Blank drafts are ignored. */
    fun agregar() {
        val texto = borrador.trim()
        if (texto.isEmpty()) return
        actualizarBorrador("")
        viewModelScope.launch { activa()?.let { tareasRepositorio.agregar(it.lista.id, texto) } }
    }

    fun marcar(tarea: Tarea, hecha: Boolean) {
        viewModelScope.launch { tareasRepositorio.marcar(tarea.id, hecha) }
    }

    fun borrar(tarea: Tarea) {
        eliminaciones[tarea.id] = viewModelScope.async { tareasRepositorio.borrar(tarea.id) }
    }

    /** Undo of [borrar]: puts the task back with its id, state and position (if its list still exists). */
    fun restaurar(tarea: Tarea) {
        val eliminacion = eliminaciones.remove(tarea.id) ?: return
        viewModelScope.launch {
            eliminacion.await()
            tareasRepositorio.restaurar(tarea)
        }
    }

    companion object {
        private const val KEY_BORRADOR = "borrador"
        private const val KEY_BORRADOR_NOMBRE = "borradorNombreLista"
        private const val KEY_RENOMBRANDO = "renombrandoLista"
        private const val KEY_HOJA_NUEVA = "hojaNuevaLista"
        private const val KEY_CONFIRMANDO = "confirmandoBorrarLista"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                ChecklistViewModel(
                    app.container.listasRepositorio,
                    app.container.tareasRepositorio,
                    app.container.plantillasRepositorio,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
