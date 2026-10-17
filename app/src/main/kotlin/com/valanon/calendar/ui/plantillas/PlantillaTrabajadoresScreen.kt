package com.valanon.calendar.ui.plantillas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.ui.componentes.CampoAlta
import com.valanon.calendar.ui.componentes.EstadoVacio
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import com.valanon.calendar.util.iniciales
import kotlinx.coroutines.launch

/** Stateful entry point of a template's detail. [onVolver] pops back to the list. */
@Composable
fun PlantillaTrabajadoresRoute(
    onVolver: () -> Unit,
    viewModel: PlantillaTrabajadoresViewModel = viewModel(factory = PlantillaTrabajadoresViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val recursos = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Deleted (here, after confirming) or missing template: back to the list.
    LaunchedEffect(estado.cerrada) { if (estado.cerrada) onVolver() }

    PlantillaTrabajadoresScreen(
        estado = estado,
        borrador = viewModel.borrador,
        borradorEdicion = viewModel.borradorEdicion,
        borradorNombre = viewModel.borradorNombre,
        snackbarHostState = snackbar,
        acciones = AccionesDetalle(
            volver = onVolver,
            empezarRenombrar = viewModel::empezarRenombrar,
            cambiarNombre = viewModel::actualizarBorradorNombre,
            guardarNombre = viewModel::guardarNombre,
            cambiarBorrador = viewModel::actualizarBorrador,
            agregar = viewModel::agregar,
            empezarEdicion = viewModel::empezarEdicion,
            cambiarEdicion = viewModel::actualizarBorradorEdicion,
            guardarEdicion = viewModel::guardarEdicion,
            cancelarEdicion = viewModel::cancelarEdicion,
            eliminar = { trabajador ->
                viewModel.eliminar(trabajador)
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val resultado = snackbar.showSnackbar(
                        message = recursos.getString(R.string.trabajador_eliminado),
                        actionLabel = recursos.getString(R.string.deshacer),
                        duration = SnackbarDuration.Short,
                    )
                    if (resultado == SnackbarResult.ActionPerformed) viewModel.deshacerEliminar(trabajador)
                }
            },
            pedirBorrarPlantilla = viewModel::pedirBorrarPlantilla,
            cancelarBorrarPlantilla = viewModel::cancelarBorrarPlantilla,
            borrarPlantilla = viewModel::borrarPlantilla,
        ),
    )
}

/** Callbacks of the detail screen, grouped to keep its signature readable. */
data class AccionesDetalle(
    val volver: () -> Unit,
    val empezarRenombrar: () -> Unit,
    val cambiarNombre: (String) -> Unit,
    val guardarNombre: () -> Unit,
    val cambiarBorrador: (String) -> Unit,
    val agregar: () -> Unit,
    val empezarEdicion: (Trabajador) -> Unit,
    val cambiarEdicion: (String) -> Unit,
    val guardarEdicion: () -> Unit,
    val cancelarEdicion: () -> Unit,
    val eliminar: (Trabajador) -> Unit,
    val pedirBorrarPlantilla: () -> Unit,
    val cancelarBorrarPlantilla: () -> Unit,
    val borrarPlantilla: () -> Unit,
) {
    companion object {
        /** For previews. */
        val Vacias = AccionesDetalle({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun PlantillaTrabajadoresScreen(
    estado: PlantillaTrabajadoresUiState,
    borrador: String,
    borradorEdicion: String,
    borradorNombre: String,
    snackbarHostState: SnackbarHostState,
    acciones: AccionesDetalle,
    modifier: Modifier = Modifier,
) {
    val plantilla = estado.plantilla
    Box(modifier.fillMaxSize().background(MisTurnosColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            BarraDetalle(
                tipo = plantilla?.tipo ?: TipoPlantilla.TRABAJADORES,
                nombre = plantilla?.nombre.orEmpty(),
                renombrando = estado.renombrando,
                borradorNombre = borradorNombre,
                acciones = acciones,
            )
            CampoAlta(
                valor = borrador,
                onValorCambia = acciones.cambiarBorrador,
                onAgregar = acciones.agregar,
                placeholder = stringResource(R.string.nombre_trabajador_placeholder),
                descripcion = stringResource(R.string.nombre_trabajador),
                capitalizacion = KeyboardCapitalization.Words,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
            )
            val n = estado.trabajadores.size
            Sobretitulo(
                texto = pluralStringResource(R.plurals.contador_trabajadores, n, n),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!estado.cargando && estado.trabajadores.isEmpty()) {
                    item(key = "vacio") {
                        EstadoVacio(
                            titulo = stringResource(R.string.sin_trabajadores),
                            ayuda = stringResource(R.string.sin_trabajadores_ayuda),
                            anchoAyuda = 240.dp,
                            modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 40.dp),
                        )
                    }
                }
                items(estado.trabajadores, key = { it.id }) { trabajador ->
                    FilaTrabajador(
                        trabajador = trabajador,
                        editando = estado.editandoId == trabajador.id,
                        borradorEdicion = borradorEdicion,
                        acciones = acciones,
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "eliminar") {
                    BotonEliminarPlantilla(acciones.pedirBorrarPlantilla, Modifier.padding(top = 4.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }

    if (estado.confirmandoBorrado && plantilla != null) {
        val n = estado.trabajadores.size
        AlertDialog(
            onDismissRequest = acciones.cancelarBorrarPlantilla,
            title = { Text(stringResource(R.string.eliminar_plantilla_titulo, plantilla.nombre), style = MisTurnosText.sheetTitle) },
            text = {
                Text(
                    if (n == 0) {
                        stringResource(R.string.eliminar_plantilla_sin_trabajadores)
                    } else {
                        pluralStringResource(R.plurals.eliminar_plantilla_cuerpo, n, n)
                    },
                    style = MisTurnosText.body,
                )
            },
            confirmButton = {
                TextButton(onClick = acciones.borrarPlantilla) {
                    Text(stringResource(R.string.eliminar), style = MisTurnosText.buttonSmall, color = MisTurnosColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = acciones.cancelarBorrarPlantilla) {
                    Text(stringResource(R.string.cancelar), style = MisTurnosText.buttonSmall, color = MisTurnosColors.Text)
                }
            },
            containerColor = MisTurnosColors.Sheet,
            titleContentColor = MisTurnosColors.Text,
            textContentColor = MisTurnosColors.TextMuted,
        )
    }
}

@Composable
private fun BarraDetalle(
    tipo: TipoPlantilla,
    nombre: String,
    renombrando: Boolean,
    borradorNombre: String,
    acciones: AccionesDetalle,
) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 14.dp, end = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = acciones.volver, modifier = Modifier.size(44.dp)) {
            Icon(
                MisTurnosIcons.FlechaAtras,
                contentDescription = stringResource(R.string.volver_a_plantillas),
                tint = MisTurnosColors.Text,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Sobretitulo(stringResource(R.string.sobretitulo_plantilla, tipo.etiqueta))
            if (renombrando) {
                val descripcion = stringResource(R.string.nombre_plantilla)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CampoEnLinea(
                        valor = borradorNombre,
                        onValorCambia = acciones.cambiarNombre,
                        onConfirmar = acciones.guardarNombre,
                        onCancelar = null,
                        descripcion = descripcion,
                        estilo = MisTurnosText.nameInput,
                        fondo = MisTurnosColors.Background,
                        borde = BorderStroke(2.dp, MisTurnosColors.AccentOnBackground),
                        modifier = Modifier.weight(1f),
                    )
                    BotonGuardar(acciones.guardarNombre)
                }
            } else {
                val descripcion = stringResource(R.string.cambiar_nombre_plantilla)
                Row(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .clickable(role = Role.Button, onClick = acciones.empezarRenombrar)
                        .semantics(mergeDescendants = true) { contentDescription = descripcion },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        nombre,
                        style = MisTurnosText.detailTitle,
                        color = MisTurnosColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .semantics { heading() },
                    )
                    Icon(MisTurnosIcons.Lapiz, contentDescription = null, tint = MisTurnosColors.TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun FilaTrabajador(
    trabajador: Trabajador,
    editando: Boolean,
    borradorEdicion: String,
    acciones: AccionesDetalle,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(forma)
            .background(MisTurnosColors.Background)
            .border(
                if (editando) BorderStroke(2.dp, MisTurnosColors.AccentOnBackground) else BorderStroke(1.dp, MisTurnosColors.LineSoft),
                forma,
            )
            .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(trabajador.nombre)
        if (editando) {
            CampoEnLinea(
                valor = borradorEdicion,
                onValorCambia = acciones.cambiarEdicion,
                onConfirmar = acciones.guardarEdicion,
                onCancelar = acciones.cancelarEdicion,
                descripcion = stringResource(R.string.nombre_trabajador),
                estilo = MisTurnosText.body,
                fondo = MisTurnosColors.SurfaceInput,
                borde = null,
                modifier = Modifier.weight(1f),
            )
            BotonGuardar(acciones.guardarEdicion)
            Surface(
                onClick = acciones.cancelarEdicion,
                shape = RoundedCornerShape(12.dp),
                color = MisTurnosColors.Surface,
                contentColor = MisTurnosColors.Text,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        MisTurnosIcons.Cerrar,
                        contentDescription = stringResource(R.string.cancelar_edicion),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else {
            Text(
                trabajador.nombre,
                style = MisTurnosText.body,
                color = MisTurnosColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { acciones.empezarEdicion(trabajador) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    MisTurnosIcons.Lapiz,
                    contentDescription = stringResource(R.string.editar_trabajador),
                    tint = MisTurnosColors.TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { acciones.eliminar(trabajador) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    MisTurnosIcons.Papelera,
                    contentDescription = stringResource(R.string.eliminar_trabajador),
                    tint = MisTurnosColors.Danger,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** 36 dp circle with the initials (spec 4b.3); decorative for accessibility (the name is read). */
@Composable
private fun Avatar(nombre: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MisTurnosColors.Surface, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales(nombre), style = MisTurnosText.avatar, color = MisTurnosColors.Text)
    }
}

/**
 * 40 dp single-line field that takes focus when it appears. Enter/Done confirms; Escape
 * (hardware keyboard) cancels when [onCancelar] is given.
 */
@Composable
private fun CampoEnLinea(
    valor: String,
    onValorCambia: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: (() -> Unit)?,
    descripcion: String,
    estilo: TextStyle,
    fondo: Color,
    borde: BorderStroke?,
    modifier: Modifier = Modifier,
) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    val forma = RoundedCornerShape(12.dp)
    BasicTextField(
        value = valor,
        onValueChange = onValorCambia,
        singleLine = true,
        textStyle = estilo.copy(color = MisTurnosColors.Text),
        cursorBrush = SolidColor(MisTurnosColors.AccentOnBackground),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onConfirmar() }),
        modifier = modifier
            .height(40.dp)
            .focusRequester(foco)
            .onPreviewKeyEvent { evento ->
                if (onCancelar != null && evento.type == KeyEventType.KeyUp && evento.key == Key.Escape) {
                    onCancelar()
                    true
                } else {
                    false
                }
            }
            .semantics { contentDescription = descripcion },
        decorationBox = { campo ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(forma)
                    .background(fondo)
                    .then(if (borde != null) Modifier.border(borde, forma) else Modifier)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) { campo() }
        },
    )
}

@Composable
private fun BotonGuardar(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MisTurnosColors.Accent, contentColor = MisTurnosColors.OnAccent),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(40.dp),
    ) {
        Text(stringResource(R.string.guardar), style = MisTurnosText.inlineButton)
    }
}

@Composable
private fun BotonEliminarPlantilla(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MisTurnosColors.Background,
        contentColor = MisTurnosColors.Danger,
        border = BorderStroke(1.dp, MisTurnosColors.DangerBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(MisTurnosIcons.PapeleraSimple, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.eliminar_plantilla), style = MisTurnosText.buttonSmall)
        }
    }
}

@Preview(name = "Detalle de plantilla", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun DetallePreview() = MuestraDetalle(oscuro = false, editando = false)

@Preview(name = "Detalle editando, oscuro", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun DetalleEditandoOscuroPreview() = MuestraDetalle(oscuro = true, editando = true)

@Composable
private fun MuestraDetalle(oscuro: Boolean, editando: Boolean) {
    val trabajadores = DatosMuestra.trabajadoresEquipo()
    MisTurnosTheme(oscuro = oscuro) {
        PlantillaTrabajadoresScreen(
            estado = PlantillaTrabajadoresUiState(
                plantilla = DatosMuestra.plantillas().first().plantilla,
                trabajadores = trabajadores,
                cargando = false,
                editandoId = if (editando) trabajadores[1].id else null,
            ),
            borrador = "",
            borradorEdicion = trabajadores[1].nombre,
            borradorNombre = "",
            snackbarHostState = remember { SnackbarHostState() },
            acciones = AccionesDetalle.Vacias,
        )
    }
}
