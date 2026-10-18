package com.valanon.calendar.ui.copia

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.data.backup.CopiaInvalidaException
import com.valanon.calendar.data.backup.CopiaSeguridadRepositorio
import com.valanon.calendar.data.model.DatosApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/** Outcome of an export/import, shown once in a snackbar and then cleared. */
sealed interface ResultadoCopia {
    data class Exportada(val datos: DatosApp) : ResultadoCopia
    data class Importada(val datos: DatosApp) : ResultadoCopia
    data class Error(val motivo: String?) : ResultadoCopia
}

data class CopiaSeguridadUiState(
    val hojaAbierta: Boolean = false,
    /** File picked for import, waiting for the "replace all data?" confirmation. */
    val importacionPendiente: Uri? = null,
    val trabajando: Boolean = false,
    val resultado: ResultadoCopia? = null,
)

/**
 * Backup sheet: export all data to a user-picked file and import it back. The work runs in
 * viewModelScope so rotating the phone does not cancel it.
 */
class CopiaSeguridadViewModel(
    private val repositorio: CopiaSeguridadRepositorio,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val trabajando = MutableStateFlow(false)
    private val resultado = MutableStateFlow<ResultadoCopia?>(null)

    val uiState: StateFlow<CopiaSeguridadUiState> = combine(
        savedStateHandle.getStateFlow(KEY_HOJA, false),
        savedStateHandle.getStateFlow<String?>(KEY_PENDIENTE, null),
        trabajando.asStateFlow(),
        resultado.asStateFlow(),
    ) { hoja, pendiente, ocupado, res ->
        CopiaSeguridadUiState(hoja, pendiente?.let(Uri::parse), ocupado, res)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CopiaSeguridadUiState())

    fun abrirHoja() {
        savedStateHandle[KEY_HOJA] = true
    }

    fun cerrarHoja() {
        savedStateHandle[KEY_HOJA] = false
    }

    fun exportar(destino: Uri) = trabajar { ResultadoCopia.Exportada(repositorio.exportar(destino)) }

    /** Import replaces everything, so it waits for confirmation. */
    fun pedirImportar(origen: Uri) {
        savedStateHandle[KEY_PENDIENTE] = origen.toString()
    }

    fun cancelarImportar() {
        savedStateHandle[KEY_PENDIENTE] = null
    }

    fun confirmarImportar() {
        val origen = savedStateHandle.get<String>(KEY_PENDIENTE)?.let(Uri::parse) ?: return
        savedStateHandle[KEY_PENDIENTE] = null
        trabajar { ResultadoCopia.Importada(repositorio.importar(origen)) }
    }

    fun resultadoMostrado() {
        resultado.value = null
    }

    private fun trabajar(accion: suspend () -> ResultadoCopia) {
        if (trabajando.value) return
        trabajando.value = true
        viewModelScope.launch {
            resultado.value = try {
                accion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: CopiaInvalidaException) {
                ResultadoCopia.Error(e.message)
            } catch (e: IOException) {
                ResultadoCopia.Error(null)
            } catch (e: android.database.SQLException) {
                ResultadoCopia.Error(null)
            } finally {
                trabajando.value = false
            }
        }
    }

    companion object {
        private const val KEY_HOJA = "copiaHojaAbierta"
        private const val KEY_PENDIENTE = "copiaImportacionPendiente"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                CopiaSeguridadViewModel(app.container.copiaSeguridadRepositorio, createSavedStateHandle())
            }
        }
    }
}
