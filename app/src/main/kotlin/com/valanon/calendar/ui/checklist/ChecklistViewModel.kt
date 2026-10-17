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
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.repository.TareasRepositorio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val tareas: List<Tarea> = emptyList(),
    val cargando: Boolean = true,
) {
    val total: Int get() = tareas.size
    val hechas: Int get() = tareas.count { it.hecha }
}

class ChecklistViewModel(
    private val repositorio: TareasRepositorio,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * Text of the "Nueva tarea…" field. Compose state (not a Flow) so the text field updates
     * synchronously; mirrored to SavedStateHandle so it survives process death and tab switches.
     */
    var borrador: String by mutableStateOf(savedStateHandle[KEY_BORRADOR] ?: "")
        private set

    val uiState: StateFlow<ChecklistUiState> = repositorio.tareas
        .map { ChecklistUiState(tareas = it, cargando = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ChecklistUiState(),
        )

    fun actualizarBorrador(texto: String) {
        borrador = texto
        savedStateHandle[KEY_BORRADOR] = texto
    }

    /** Adds the draft (trimmed) at the end of the list and clears the field. Blank drafts are ignored. */
    fun agregar() {
        val texto = borrador.trim()
        if (texto.isEmpty()) return
        actualizarBorrador("")
        viewModelScope.launch { repositorio.agregar(texto) }
    }

    fun marcar(tarea: Tarea, hecha: Boolean) {
        viewModelScope.launch { repositorio.marcar(tarea.id, hecha) }
    }

    fun borrar(tarea: Tarea) {
        viewModelScope.launch { repositorio.borrar(tarea.id) }
    }

    /** Undo of [borrar]: puts the task back with its id, state and position. */
    fun restaurar(tarea: Tarea) {
        viewModelScope.launch { repositorio.restaurar(tarea) }
    }

    /** Floating button: unchecks every task, deletes nothing. */
    fun reiniciarTodas() {
        viewModelScope.launch { repositorio.reiniciarTodas() }
    }

    companion object {
        private const val KEY_BORRADOR = "borrador"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                ChecklistViewModel(app.container.tareasRepositorio, createSavedStateHandle())
            }
        }
    }
}
