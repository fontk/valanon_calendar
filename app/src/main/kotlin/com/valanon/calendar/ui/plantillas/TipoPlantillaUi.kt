package com.valanon.calendar.ui.plantillas

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.model.ElementoPlantilla
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.ui.componentes.Avatar
import com.valanon.calendar.ui.componentes.AvatarNumero
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.MisTurnosColors

/** UI bits of each template type. Add a branch here when a new [TipoPlantilla] appears. */
val TipoPlantilla.icono: ImageVector
    get() = when (this) {
        TipoPlantilla.TRABAJADORES -> MisTurnosIcons.Trabajadores
        TipoPlantilla.TAREAS -> MisTurnosIcons.Checklist
    }

val TipoPlantilla.descripcion: String
    @Composable get() = when (this) {
        TipoPlantilla.TRABAJADORES -> stringResource(R.string.tipo_trabajadores_descripcion)
        TipoPlantilla.TAREAS -> stringResource(R.string.tipo_tareas_descripcion)
    }

/** Badge colours: the accent family for Trabajadores, violet for Tareas (DESIGN.md → tokens.templateTasks). */
private val TipoPlantilla.fondoInsignia: Color
    @Composable get() = when (this) {
        TipoPlantilla.TRABAJADORES -> MisTurnosColors.SurfaceAccent
        TipoPlantilla.TAREAS -> MisTurnosColors.TemplateTasksBg
    }

private val TipoPlantilla.colorInsignia: Color
    @Composable get() = when (this) {
        TipoPlantilla.TRABAJADORES -> MisTurnosColors.AccentOnBackground
        TipoPlantilla.TAREAS -> MisTurnosColors.TemplateTasksFg
    }

/** Badge (radius 12, 44 dp by default; 40 dp in the Checklist "Nueva lista" sheet) with the type icon. */
@Composable
fun InsigniaTipo(tipo: TipoPlantilla, tamano: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(tamano)
            .background(tipo.fondoInsignia, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(tipo.icono, contentDescription = null, tint = tipo.colorInsignia, modifier = Modifier.size(tamano / 2))
    }
}

/** Avatar of a detail row: initials in a circle for a worker, the position (1, 2, 3…) for a preset task. */
@Composable
fun AvatarElemento(tipo: TipoPlantilla, elemento: ElementoPlantilla, posicion: Int) = when (tipo) {
    TipoPlantilla.TRABAJADORES -> Avatar(elemento.texto)
    TipoPlantilla.TAREAS -> AvatarNumero(posicion)
}

/** Texts of the detail screen that depend on the type (spec 4b.3); the layout is shared. */
@Immutable
data class TextosTipoPlantilla(
    @StringRes val placeholder: Int,
    /** Accessible label of the add field and of the inline edit field. */
    @StringRes val campo: Int,
    /** "4 TRABAJADORES" / "5 TAREAS" section label, also the card subtitle. */
    @PluralsRes val contador: Int,
    @StringRes val vacio: Int,
    @StringRes val vacioAyuda: Int,
    @StringRes val editar: Int,
    @StringRes val eliminar: Int,
    /** Snackbar after deleting a row. */
    @StringRes val eliminado: Int,
    /** Delete-template dialog: "Se borrarán también sus N …" and the body when it is empty. */
    @PluralsRes val eliminarPlantillaElementos: Int,
    @StringRes val eliminarPlantillaVacia: Int,
    /** Extra sentence of the delete-template dialog, or 0 for none. */
    @StringRes val eliminarPlantillaNota: Int,
    val capitalizacion: KeyboardCapitalization,
)

val TipoPlantilla.textos: TextosTipoPlantilla
    get() = when (this) {
        TipoPlantilla.TRABAJADORES -> TextosTipoPlantilla(
            placeholder = R.string.nombre_trabajador_placeholder,
            campo = R.string.nombre_trabajador,
            contador = R.plurals.contador_trabajadores,
            vacio = R.string.sin_trabajadores,
            vacioAyuda = R.string.sin_trabajadores_ayuda,
            editar = R.string.editar_trabajador,
            eliminar = R.string.eliminar_trabajador,
            eliminado = R.string.trabajador_eliminado,
            eliminarPlantillaElementos = R.plurals.eliminar_plantilla_trabajadores,
            eliminarPlantillaVacia = R.string.eliminar_plantilla_sin_trabajadores,
            eliminarPlantillaNota = 0,
            capitalizacion = KeyboardCapitalization.Words,
        )
        TipoPlantilla.TAREAS -> TextosTipoPlantilla(
            placeholder = R.string.tarea_preset_placeholder,
            campo = R.string.texto_tarea,
            contador = R.plurals.contador_tareas,
            vacio = R.string.sin_tareas,
            vacioAyuda = R.string.sin_tareas_ayuda,
            editar = R.string.editar_tarea,
            eliminar = R.string.eliminar_tarea,
            eliminado = R.string.tarea_borrada,
            eliminarPlantillaElementos = R.plurals.eliminar_plantilla_tareas,
            eliminarPlantillaVacia = R.string.eliminar_plantilla_sin_tareas,
            eliminarPlantillaNota = R.string.eliminar_plantilla_tareas_nota,
            capitalizacion = KeyboardCapitalization.Sentences,
        )
    }
