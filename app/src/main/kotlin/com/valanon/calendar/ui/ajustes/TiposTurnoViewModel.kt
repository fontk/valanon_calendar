package com.valanon.calendar.ui.ajustes

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
import com.valanon.calendar.data.local.MAX_TIPOS_TURNO
import com.valanon.calendar.data.local.dao.TipoTurnoConUsos
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.local.entity.TipoTurnoId
import com.valanon.calendar.data.repository.ErrorTipoTurno
import com.valanon.calendar.data.repository.TiposTurnoRepositorio
import com.valanon.calendar.ui.theme.PaletaTurnos
import com.valanon.calendar.util.MAX_ABREVIATURA_TIPO_TURNO
import com.valanon.calendar.util.MAX_NOMBRE_TIPO_TURNO
import com.valanon.calendar.util.abreviaturaDe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

/** The open editor sheet (create when [id] is null, edit otherwise). Texts live in the ViewModel. */
data class EditorTipoTurno(
    val id: TipoTurnoId?,
    val color: String,
    val error: ErrorTipoTurno? = null,
) {
    val esNuevo: Boolean get() = id == null
}

data class TiposTurnoUiState(
    val tipos: List<TipoTurnoConUsos> = emptyList(),
    val cargando: Boolean = true,
    val editor: EditorTipoTurno? = null,
    /** Type whose delete dialog is open, with its live usage counts. */
    val confirmandoBorrado: TipoTurnoConUsos? = null,
    val confirmandoRestablecer: Boolean = false,
) {
    val total: Int get() = tipos.size

    /** "Añadir tipo de turno" is disabled at 16 types. */
    val puedeAnadir: Boolean get() = !cargando && total < MAX_TIPOS_TURNO

    /** The bin is disabled when a single type is left (minimum one). */
    val puedeBorrar: Boolean get() = total > 1
}

/**
 * "Tipos de turno" (spec 3c.2–3c.5): list, editor sheet, delete and reset dialogs. The name and
 * abbreviation fields are Compose state (synchronous text fields) mirrored to [SavedStateHandle];
 * the rest of the UI state lives in the handle, so rotation keeps an open editor or dialog.
 */
class TiposTurnoViewModel(
    private val repositorio: TiposTurnoRepositorio,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    var nombre: String by mutableStateOf(savedStateHandle[KEY_NOMBRE] ?: "")
        private set
    var abreviatura: String by mutableStateOf(savedStateHandle[KEY_ABREVIATURA] ?: "")
        private set

    /** Once the user types an abbreviation it stops following the name. */
    private var abreviaturaManual: Boolean
        get() = savedStateHandle[KEY_ABREVIATURA_MANUAL] ?: false
        set(valor) {
            savedStateHandle[KEY_ABREVIATURA_MANUAL] = valor
        }

    /** One save at a time: a double tap on "Añadir" would otherwise create the type twice. */
    private var guardando = false

    private val editor = combine(
        savedStateHandle.getStateFlow(KEY_EDITOR_ABIERTO, false),
        savedStateHandle.getStateFlow<Long?>(KEY_EDITANDO_ID, null),
        savedStateHandle.getStateFlow(KEY_COLOR, PaletaTurnos.entradas.first().clave),
        savedStateHandle.getStateFlow<String?>(KEY_ERROR, null),
    ) { abierto, id, color, error ->
        if (abierto) EditorTipoTurno(id, color, error?.let(ErrorTipoTurno::valueOf)) else null
    }

    private val dialogos = combine(
        savedStateHandle.getStateFlow<Long?>(KEY_BORRAR_ID, null),
        savedStateHandle.getStateFlow(KEY_RESTABLECER, false),
    ) { borrarId, restablecer -> borrarId to restablecer }

    val uiState: StateFlow<TiposTurnoUiState> =
        combine(repositorio.tiposConUsos, editor, dialogos) { tipos, ed, (borrarId, restablecer) ->
            TiposTurnoUiState(
                tipos = tipos,
                cargando = false,
                editor = ed,
                confirmandoBorrado = borrarId?.let { id -> tipos.firstOrNull { it.tipo.id == id } },
                confirmandoRestablecer = restablecer,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = TiposTurnoUiState(),
        )

    // --- Editor ---

    /** Opens the editor empty, with the first palette colour no type uses yet (spec 3c.3). */
    fun nuevo() {
        actualizarNombre("")
        actualizarAbreviatura("")
        abreviaturaManual = false
        savedStateHandle[KEY_EDITANDO_ID] = null
        savedStateHandle[KEY_ERROR] = null
        savedStateHandle[KEY_EDITOR_ABIERTO] = true
        viewModelScope.launch {
            val usadas = repositorio.tipos.first().map { it.color }
            savedStateHandle[KEY_COLOR] = PaletaTurnos.primeraLibre(usadas)
        }
    }

    /** Opens the editor on [tipo]; its abbreviation counts as typed, so renaming keeps it. */
    fun editar(tipo: TipoTurno) {
        actualizarNombre(tipo.nombre)
        actualizarAbreviatura(tipo.abreviatura)
        abreviaturaManual = true
        savedStateHandle[KEY_EDITANDO_ID] = tipo.id
        savedStateHandle[KEY_COLOR] = tipo.color
        savedStateHandle[KEY_ERROR] = null
        savedStateHandle[KEY_EDITOR_ABIERTO] = true
    }

    /** At most 20 characters; the abbreviation follows ("Guardia" → "GUA") until typed by hand. */
    fun cambiarNombre(texto: String) {
        actualizarNombre(texto.take(MAX_NOMBRE_TIPO_TURNO))
        if (!abreviaturaManual) actualizarAbreviatura(abreviaturaDe(nombre))
        savedStateHandle[KEY_ERROR] = null
    }

    /** Upper case, at most 5 characters. From now on the name no longer overwrites it. */
    fun cambiarAbreviatura(texto: String) {
        actualizarAbreviatura(texto.uppercase(ESPANOL).take(MAX_ABREVIATURA_TIPO_TURNO))
        abreviaturaManual = true
    }

    fun elegirColor(clave: String) {
        savedStateHandle[KEY_COLOR] = clave
    }

    /** Validates and saves; closes the editor on success, shows the error under the fields otherwise. */
    fun guardar() {
        if (guardando || savedStateHandle.get<Boolean>(KEY_EDITOR_ABIERTO) != true) return
        guardando = true
        val id = savedStateHandle.get<Long>(KEY_EDITANDO_ID)
        val color = savedStateHandle.get<String>(KEY_COLOR) ?: PaletaTurnos.entradas.first().clave
        viewModelScope.launch {
            try {
                val error = repositorio.guardar(id, nombre, abreviatura, color)
                if (error == null) cerrarEditor() else savedStateHandle[KEY_ERROR] = error.name
            } finally {
                guardando = false
            }
        }
    }

    fun cerrarEditor() {
        savedStateHandle[KEY_EDITOR_ABIERTO] = false
        savedStateHandle[KEY_ERROR] = null
    }

    // --- Delete ---

    /** Ignored for the last type left (its bin is disabled; the repository refuses it too). */
    fun pedirBorrar(tipo: TipoTurno) {
        if (uiState.value.puedeBorrar) savedStateHandle[KEY_BORRAR_ID] = tipo.id
    }

    fun cancelarBorrar() {
        savedStateHandle[KEY_BORRAR_ID] = null
    }

    /** Deletes the type and, by cascade, its days in Calendario and Turnos (no undo: the dialog shows the counts). */
    fun borrar() {
        val id = savedStateHandle.get<Long>(KEY_BORRAR_ID) ?: return
        savedStateHandle[KEY_BORRAR_ID] = null
        viewModelScope.launch { repositorio.borrar(id) }
    }

    // --- Reset the five defaults ---

    fun pedirRestablecer() {
        savedStateHandle[KEY_RESTABLECER] = true
    }

    fun cancelarRestablecer() {
        savedStateHandle[KEY_RESTABLECER] = false
    }

    fun restablecer() {
        savedStateHandle[KEY_RESTABLECER] = false
        viewModelScope.launch { repositorio.restablecer() }
    }

    private fun actualizarNombre(texto: String) {
        nombre = texto
        savedStateHandle[KEY_NOMBRE] = texto
    }

    private fun actualizarAbreviatura(texto: String) {
        abreviatura = texto
        savedStateHandle[KEY_ABREVIATURA] = texto
    }

    companion object {
        private val ESPANOL = Locale.forLanguageTag("es")
        private const val KEY_EDITOR_ABIERTO = "tipoEditorAbierto"
        private const val KEY_EDITANDO_ID = "tipoEditandoId"
        private const val KEY_NOMBRE = "tipoNombre"
        private const val KEY_ABREVIATURA = "tipoAbreviatura"
        private const val KEY_ABREVIATURA_MANUAL = "tipoAbreviaturaManual"
        private const val KEY_COLOR = "tipoColor"
        private const val KEY_ERROR = "tipoError"
        private const val KEY_BORRAR_ID = "tipoBorrarId"
        private const val KEY_RESTABLECER = "tiposRestablecer"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                TiposTurnoViewModel(app.container.tiposTurnoRepositorio, createSavedStateHandle())
            }
        }
    }
}
