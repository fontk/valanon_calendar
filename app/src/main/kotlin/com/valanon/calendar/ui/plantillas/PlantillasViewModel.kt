package com.valanon.calendar.ui.plantillas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.repository.PlantillasRepositorio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PlantillasUiState(
    val plantillas: List<PlantillaConRecuento> = emptyList(),
    val cargando: Boolean = true,
    /** "¿Qué tipo de plantilla?" sheet (spec 4b.2). */
    val hojaTiposAbierta: Boolean = false,
)

/** Templates list (spec 4b.1) and creation through the type sheet (4b.2). */
class PlantillasViewModel(
    private val repositorio: PlantillasRepositorio,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val hojaTiposAbierta: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_HOJA_TIPOS, false)

    val uiState: StateFlow<PlantillasUiState> =
        combine(repositorio.plantillas, hojaTiposAbierta) { lista, hoja ->
            PlantillasUiState(plantillas = lista, cargando = false, hojaTiposAbierta = hoja)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = PlantillasUiState(hojaTiposAbierta = hojaTiposAbierta.value),
        )

    fun abrirHojaTipos() {
        savedStateHandle[KEY_HOJA_TIPOS] = true
    }

    fun cerrarHojaTipos() {
        savedStateHandle[KEY_HOJA_TIPOS] = false
    }

    /**
     * Creates a template of [tipo] named "Trabajadores N", closes the sheet and returns the new
     * id so the screen can open its detail with the name in edit mode.
     */
    suspend fun crear(tipo: TipoPlantilla): Long {
        cerrarHojaTipos()
        return repositorio.crear(tipo)
    }

    companion object {
        private const val KEY_HOJA_TIPOS = "hojaTiposAbierta"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                PlantillasViewModel(app.container.plantillasRepositorio, createSavedStateHandle())
            }
        }
    }
}
