package com.valanon.calendar.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.repository.TiposTurnoRepositorio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Ajustes home (spec 3c.1): only needs the types for the mini chips of "Tipos de turno". */
class AjustesViewModel(repositorio: TiposTurnoRepositorio) : ViewModel() {

    val tipos: StateFlow<List<TipoTurno>> = repositorio.tipos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                AjustesViewModel(app.container.tiposTurnoRepositorio)
            }
        }
    }
}
