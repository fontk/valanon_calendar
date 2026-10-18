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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.model.ElementoPlantilla
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.comoElemento
import com.valanon.calendar.ui.componentes.BotonGuardar
import com.valanon.calendar.ui.componentes.CampoAlta
import com.valanon.calendar.ui.componentes.CampoEnLinea
import com.valanon.calendar.ui.componentes.DialogoEliminar
import com.valanon.calendar.ui.componentes.EstadoVacio
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import kotlinx.coroutines.launch

/** Stateful entry point of a template's detail (any type). [onVolver] pops back to the list. */
@Composable
fun PlantillaDetalleRoute(
    onVolver: () -> Unit,
    viewModel: PlantillaDetalleViewModel = viewModel(factory = PlantillaDetalleViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val recursos = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Deleted (here, after confirming) or missing template: back to the list.
    LaunchedEffect(estado.cerrada) { if (estado.cerrada) onVolver() }

    PlantillaDetalleScreen(
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
            eliminar = { elemento ->
                val mensaje = recursos.getString((estado.plantilla?.tipo ?: TipoPlantilla.TRABAJADORES).textos.eliminado)
                viewModel.eliminar(elemento)
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val resultado = snackbar.showSnackbar(
                        message = mensaje,
                        actionLabel = recursos.getString(R.string.deshacer),
                        duration = SnackbarDuration.Short,
                    )
                    if (resultado == SnackbarResult.ActionPerformed) viewModel.deshacerEliminar(elemento)
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
    val empezarEdicion: (ElementoPlantilla) -> Unit,
    val cambiarEdicion: (String) -> Unit,
    val guardarEdicion: () -> Unit,
    val cancelarEdicion: () -> Unit,
    val eliminar: (ElementoPlantilla) -> Unit,
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
fun PlantillaDetalleScreen(
    estado: PlantillaDetalleUiState,
    borrador: String,
    borradorEdicion: String,
    borradorNombre: String,
    snackbarHostState: SnackbarHostState,
    acciones: AccionesDetalle,
    modifier: Modifier = Modifier,
) {
    val plantilla = estado.plantilla
    val tipo = plantilla?.tipo ?: TipoPlantilla.TRABAJADORES
    val textos = tipo.textos
    Box(modifier.fillMaxSize().background(MisTurnosColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            BarraDetalle(
                tipo = tipo,
                nombre = plantilla?.nombre.orEmpty(),
                renombrando = estado.renombrando,
                borradorNombre = borradorNombre,
                acciones = acciones,
            )
            CampoAlta(
                valor = borrador,
                onValorCambia = acciones.cambiarBorrador,
                onAgregar = acciones.agregar,
                placeholder = stringResource(textos.placeholder),
                descripcion = stringResource(textos.campo),
                capitalizacion = textos.capitalizacion,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
            )
            val n = estado.elementos.size
            Sobretitulo(
                texto = pluralStringResource(textos.contador, n, n),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!estado.cargando && estado.elementos.isEmpty()) {
                    item(key = "vacio") {
                        EstadoVacio(
                            titulo = stringResource(textos.vacio),
                            ayuda = stringResource(textos.vacioAyuda),
                            anchoAyuda = 240.dp,
                            modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 40.dp),
                        )
                    }
                }
                itemsIndexed(estado.elementos, key = { _, elemento -> elemento.id }) { indice, elemento ->
                    FilaElemento(
                        tipo = tipo,
                        elemento = elemento,
                        posicion = indice + 1,
                        editando = estado.editandoId == elemento.id,
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
        val n = estado.elementos.size
        val turnos = estado.numeroAsignaciones
        val principal = when {
            n == 0 -> stringResource(textos.eliminarPlantillaVacia)
            turnos == 0 -> stringResource(R.string.frase, pluralStringResource(textos.eliminarPlantillaElementos, n, n))
            // Spec 3b.5: "… y sus N turnos asignados" when the Turnos tab has assignments.
            else -> stringResource(
                R.string.frase_y,
                pluralStringResource(textos.eliminarPlantillaElementos, n, n),
                pluralStringResource(R.plurals.eliminar_plantilla_turnos, turnos, turnos),
            )
        }
        // Tareas: "Las listas ya creadas en Checklist no se ven afectadas."
        val nota = if (textos.eliminarPlantillaNota != 0) stringResource(textos.eliminarPlantillaNota) else null
        DialogoEliminar(
            titulo = stringResource(R.string.eliminar_plantilla_titulo, plantilla.nombre),
            cuerpo = listOfNotNull(principal, nota).joinToString(" "),
            onConfirmar = acciones.borrarPlantilla,
            onCancelar = acciones.cancelarBorrarPlantilla,
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
                        capitalizacion = KeyboardCapitalization.Sentences,
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
                        .heightIn(min = 44.dp)
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

/** A worker or a preset task; [posicion] (1-based) is the avatar of a task. */
@Composable
fun FilaElemento(
    tipo: TipoPlantilla,
    elemento: ElementoPlantilla,
    posicion: Int,
    editando: Boolean,
    borradorEdicion: String,
    acciones: AccionesDetalle,
    modifier: Modifier = Modifier,
) {
    val textos = tipo.textos
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
        AvatarElemento(tipo, elemento, posicion)
        if (editando) {
            CampoEnLinea(
                valor = borradorEdicion,
                onValorCambia = acciones.cambiarEdicion,
                onConfirmar = acciones.guardarEdicion,
                onCancelar = acciones.cancelarEdicion,
                descripcion = stringResource(textos.campo),
                capitalizacion = textos.capitalizacion,
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
                elemento.texto,
                style = MisTurnosText.body,
                color = MisTurnosColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { acciones.empezarEdicion(elemento) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    MisTurnosIcons.Lapiz,
                    contentDescription = stringResource(textos.editar),
                    tint = MisTurnosColors.TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { acciones.eliminar(elemento) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    MisTurnosIcons.Papelera,
                    contentDescription = stringResource(textos.eliminar),
                    tint = MisTurnosColors.Danger,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
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

@Preview(name = "Detalle de plantilla de tareas", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun DetalleTareasPreview() = MuestraDetalle(oscuro = false, editando = false, tipo = TipoPlantilla.TAREAS)

@Composable
private fun MuestraDetalle(oscuro: Boolean, editando: Boolean, tipo: TipoPlantilla = TipoPlantilla.TRABAJADORES) {
    val plantilla = DatosMuestra.plantillas().first { it.plantilla.tipo == tipo }.plantilla
    val elementos = when (tipo) {
        TipoPlantilla.TRABAJADORES -> DatosMuestra.trabajadoresEquipo().map { it.comoElemento() }
        TipoPlantilla.TAREAS -> DatosMuestra.tareasRutinaApertura().map { it.comoElemento() }
    }
    MisTurnosTheme(oscuro = oscuro) {
        PlantillaDetalleScreen(
            estado = PlantillaDetalleUiState(
                plantilla = plantilla,
                elementos = elementos,
                cargando = false,
                editandoId = if (editando) elementos[1].id else null,
            ),
            borrador = "",
            borradorEdicion = elementos[1].texto,
            borradorNombre = "",
            snackbarHostState = remember { SnackbarHostState() },
            acciones = AccionesDetalle.Vacias,
        )
    }
}
