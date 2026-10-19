package com.valanon.calendar.ui.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.repository.ErrorTipoTurno
import com.valanon.calendar.ui.componentes.AsaHoja
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.ColoresTurno
import com.valanon.calendar.ui.theme.LocalPaleta
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.PaletaTurnos
import com.valanon.calendar.util.abreviaturaDe
import kotlinx.coroutines.launch

/** Columns of the colour grid (spec 3c.3): 16 entries in two rows. */
private const val COLUMNAS_PALETA = 8

/**
 * Create/edit sheet of a shift type (spec 3c.3): live cell preview, name and abbreviation fields,
 * 8-column palette grid and Cancelar / Añadir·Guardar. Saving closes it from the ViewModel once
 * the type is valid; otherwise the error shows under the fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTipoTurnoSheet(
    editor: EditorTipoTurno,
    nombre: String,
    abreviatura: String,
    acciones: AccionesTiposTurno,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val cancelar: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { acciones.cerrarEditor() }
    }
    val entrada = PaletaTurnos[editor.color]
    val colores = if (LocalPaleta.current.oscuro) entrada.oscuro else entrada.claro

    ModalBottomSheet(
        onDismissRequest = acciones.cerrarEditor,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MisTurnosColors.Sheet,
        contentColor = MisTurnosColors.Text,
        scrimColor = MisTurnosColors.Scrim,
        dragHandle = { AsaHoja() },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Sobretitulo(stringResource(if (editor.esNuevo) R.string.nuevo_tipo else R.string.editar_tipo_titulo))
                    Text(
                        when {
                            editor.esNuevo -> stringResource(R.string.editor_titulo_nuevo)
                            nombre.isBlank() -> stringResource(R.string.sin_nombre)
                            else -> nombre
                        },
                        style = MisTurnosText.sheetTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                VistaPreviaCelda(
                    abreviatura = abreviatura.ifBlank { abreviaturaDe(nombre) }.ifBlank { stringResource(R.string.abreviatura_vacia) },
                    colores = colores,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoEditor(
                    etiqueta = stringResource(R.string.nombre),
                    valor = nombre,
                    onCambia = acciones.cambiarNombre,
                    placeholder = stringResource(R.string.nombre_placeholder),
                    estilo = MisTurnosText.body,
                    capitalizacion = KeyboardCapitalization.Sentences,
                    conError = editor.error != null,
                    modifier = Modifier.weight(1f),
                )
                CampoEditor(
                    etiqueta = stringResource(R.string.abreviatura),
                    valor = abreviatura,
                    onCambia = acciones.cambiarAbreviatura,
                    placeholder = stringResource(R.string.abreviatura_placeholder),
                    estilo = MisTurnosText.abreviaturaCampo,
                    capitalizacion = KeyboardCapitalization.Characters,
                    conError = false,
                    ancho = 96.dp,
                )
            }
            editor.error?.let { textoError(it) }?.let { mensaje ->
                Text(mensaje, style = MisTurnosText.errorCampo, color = MisTurnosColors.Danger)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.color), style = MisTurnosText.etiquetaCampo, color = MisTurnosColors.TextMuted)
                RejillaColores(seleccionado = editor.color, onElegir = acciones.elegirColor)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonHoja(
                    texto = stringResource(R.string.cancelar),
                    relleno = false,
                    onClick = cancelar,
                    modifier = Modifier.weight(1f),
                )
                BotonHoja(
                    texto = stringResource(if (editor.esNuevo) R.string.anadir else R.string.guardar),
                    relleno = true,
                    onClick = acciones.guardar,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Message under the fields, or `null` for errors the UI already prevents (add disabled at 16, bin of the last type). */
@Composable
private fun textoError(error: ErrorTipoTurno): String? = when (error) {
    ErrorTipoTurno.NOMBRE_VACIO -> stringResource(R.string.error_nombre_vacio)
    ErrorTipoTurno.NOMBRE_REPETIDO -> stringResource(R.string.error_nombre_repetido)
    ErrorTipoTurno.LIMITE, ErrorTipoTurno.ULTIMO -> null
}

/** 56×60 calendar cell ("17" + abbreviation) painted with the chosen colour; decorative. */
@Composable
private fun VistaPreviaCelda(abreviatura: String, colores: ColoresTurno) {
    val forma = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(56.dp)
            .heightIn(min = 60.dp)
            .background(colores.fondo, forma)
            .border(1.dp, colores.borde, forma)
            .clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text("17", style = MisTurnosText.dayNumber, color = colores.texto)
        Text(abreviatura, style = MisTurnosText.dayTag, color = colores.texto, maxLines = 1)
    }
}

/** Labelled 48 dp field (radius 14, `surfaceInput`); red border when [conError]. */
@Composable
private fun CampoEditor(
    etiqueta: String,
    valor: String,
    onCambia: (String) -> Unit,
    placeholder: String,
    estilo: TextStyle,
    capitalizacion: KeyboardCapitalization,
    conError: Boolean,
    modifier: Modifier = Modifier,
    ancho: Dp? = null,
) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier.then(if (ancho != null) Modifier.width(ancho) else Modifier),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(etiqueta, style = MisTurnosText.etiquetaCampo, color = MisTurnosColors.TextMuted)
        BasicTextField(
            value = valor,
            onValueChange = onCambia,
            singleLine = true,
            textStyle = estilo.copy(color = MisTurnosColors.Text),
            cursorBrush = SolidColor(MisTurnosColors.AccentOnBackground),
            keyboardOptions = KeyboardOptions(capitalization = capitalizacion, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = etiqueta },
            decorationBox = { campo ->
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(forma)
                        .background(MisTurnosColors.SurfaceInput)
                        .border(1.dp, if (conError) MisTurnosColors.ErrorBorder else MisTurnosColors.Line, forma)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (valor.isEmpty()) {
                        Text(placeholder, style = estilo, color = MisTurnosColors.TextFaint, maxLines = 1)
                    }
                    campo()
                }
            },
        )
    }
}

/**
 * The 16 palette entries as 8-column square buttons painted with their `bg`; the chosen one has a
 * 3 dp accent border and a check in its `fg`. Radio group semantics, named after each colour.
 */
@Composable
private fun RejillaColores(seleccionado: String, onElegir: (String) -> Unit) {
    val oscuro = LocalPaleta.current.oscuro
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PaletaTurnos.entradas.chunked(COLUMNAS_PALETA).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { entrada ->
                    val c = if (oscuro) entrada.oscuro else entrada.claro
                    val activo = entrada.clave == seleccionado
                    val nombre = stringResource(entrada.nombre)
                    val forma = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(forma)
                            .background(c.fondo)
                            .border(
                                if (activo) BorderStroke(3.dp, MisTurnosColors.AccentOnBackground) else BorderStroke(1.dp, c.borde),
                                forma,
                            )
                            .selectable(selected = activo, role = Role.RadioButton, onClick = { onElegir(entrada.clave) })
                            .semantics { contentDescription = nombre },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (activo) Icon(MisTurnosIcons.Marca, contentDescription = null, tint = c.texto, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/** 48 dp sheet button: neutral `surface` or filled `accent`. */
@Composable
private fun BotonHoja(texto: String, relleno: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (relleno) MisTurnosColors.Accent else MisTurnosColors.SheetButton,
        contentColor = if (relleno) MisTurnosColors.OnAccent else MisTurnosColors.Text,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(texto, style = if (relleno) MisTurnosText.addButton else MisTurnosText.buttonSmall)
        }
    }
}
