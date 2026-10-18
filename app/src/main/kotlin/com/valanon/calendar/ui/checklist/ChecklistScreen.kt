package com.valanon.calendar.ui.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.ListaConProgreso
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.ui.componentes.BotonGuardar
import com.valanon.calendar.ui.componentes.CabeceraPestana
import com.valanon.calendar.ui.componentes.CampoAlta
import com.valanon.calendar.ui.componentes.CampoEnLinea
import com.valanon.calendar.ui.componentes.DialogoEliminar
import com.valanon.calendar.ui.componentes.EstadoVacio
import com.valanon.calendar.ui.componentes.EstadoVacioInsignia
import com.valanon.calendar.ui.componentes.bordeDiscontinuo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import kotlinx.coroutines.launch

/** Stateful entry point: wires [ChecklistViewModel] and the "Tarea borrada / Deshacer" snackbar. */
@Composable
fun ChecklistRoute(viewModel: ChecklistViewModel = viewModel(factory = ChecklistViewModel.Factory)) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val recursos = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    ChecklistScreen(
        estado = estado,
        borrador = viewModel.borrador,
        borradorNombre = viewModel.borradorNombre,
        snackbarHostState = snackbar,
        acciones = AccionesChecklist(
            activar = viewModel::activar,
            abrirNueva = viewModel::abrirHojaNueva,
            cerrarNueva = viewModel::cerrarHojaNueva,
            crearDesdePlantilla = viewModel::crearDesdePlantilla,
            crearEnBlanco = { viewModel.crearEnBlanco { n -> recursos.getString(R.string.lista_n, n) } },
            empezarRenombrar = viewModel::empezarRenombrar,
            cambiarNombre = viewModel::actualizarBorradorNombre,
            guardarNombre = viewModel::guardarNombre,
            reiniciar = viewModel::reiniciar,
            pedirBorrarLista = viewModel::pedirBorrarLista,
            cancelarBorrarLista = viewModel::cancelarBorrarLista,
            borrarLista = viewModel::borrarLista,
            cambiarBorrador = viewModel::actualizarBorrador,
            agregar = viewModel::agregar,
            marcar = viewModel::marcar,
            borrar = { tarea ->
                viewModel.borrar(tarea)
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val resultado = snackbar.showSnackbar(
                        message = recursos.getString(R.string.tarea_borrada),
                        actionLabel = recursos.getString(R.string.deshacer),
                        duration = SnackbarDuration.Short,
                    )
                    if (resultado == SnackbarResult.ActionPerformed) viewModel.restaurar(tarea)
                }
            },
        ),
    )
}

/** Callbacks of the Checklist tab, grouped to keep the screen's signature readable. */
data class AccionesChecklist(
    val activar: (Long) -> Unit,
    val abrirNueva: () -> Unit,
    val cerrarNueva: () -> Unit,
    val crearDesdePlantilla: (Long) -> Unit,
    val crearEnBlanco: () -> Unit,
    val empezarRenombrar: () -> Unit,
    val cambiarNombre: (String) -> Unit,
    val guardarNombre: () -> Unit,
    val reiniciar: () -> Unit,
    val pedirBorrarLista: () -> Unit,
    val cancelarBorrarLista: () -> Unit,
    val borrarLista: () -> Unit,
    val cambiarBorrador: (String) -> Unit,
    val agregar: () -> Unit,
    val marcar: (Tarea, Boolean) -> Unit,
    val borrar: (Tarea) -> Unit,
) {
    companion object {
        /** For previews. */
        val Vacias = AccionesChecklist({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {})
    }
}

@Composable
fun ChecklistScreen(
    estado: ChecklistUiState,
    borrador: String,
    borradorNombre: String,
    snackbarHostState: SnackbarHostState,
    acciones: AccionesChecklist,
    modifier: Modifier = Modifier,
) {
    val activa = estado.activa
    Box(modifier.fillMaxSize().background(MisTurnosColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            CabeceraPestana(
                titulo = stringResource(R.string.checklist_titulo),
                // Progress of the active list; hidden when it has no tasks (spec 4.1).
                resumen = if (estado.total > 0) {
                    pluralStringResource(R.plurals.progreso_tareas, estado.total, estado.hechas, estado.total)
                } else {
                    null
                },
            )
            when {
                estado.sinListas -> EstadoVacioInsignia(
                    icono = MisTurnosIcons.Checklist,
                    titulo = stringResource(R.string.sin_listas),
                    ayuda = stringResource(R.string.sin_listas_ayuda),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    BotonNuevaLista(acciones.abrirNueva)
                }
                activa != null -> {
                    FilaChipsListas(
                        listas = estado.listas,
                        activaId = activa.lista.id,
                        onActivar = acciones.activar,
                        onNueva = acciones.abrirNueva,
                    )
                    TarjetaListaActiva(
                        lista = activa,
                        renombrando = estado.renombrando,
                        borradorNombre = borradorNombre,
                        acciones = acciones,
                    )
                    CampoAlta(
                        valor = borrador,
                        onValorCambia = acciones.cambiarBorrador,
                        onAgregar = acciones.agregar,
                        placeholder = stringResource(R.string.nueva_tarea_placeholder),
                        descripcion = stringResource(R.string.nueva_tarea),
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                    )
                    ListaTareas(estado, acciones, Modifier.weight(1f))
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

    if (estado.hojaNuevaAbierta) {
        SelectorPlantillaTareas(
            plantillas = estado.plantillasTareas,
            onElegir = acciones.crearDesdePlantilla,
            onEnBlanco = acciones.crearEnBlanco,
            onCerrar = acciones.cerrarNueva,
        )
    }
    if (estado.confirmandoBorrado && activa != null) {
        val n = activa.total
        DialogoEliminar(
            titulo = stringResource(R.string.eliminar_lista_titulo, activa.lista.nombre),
            cuerpo = if (n == 0) {
                stringResource(R.string.eliminar_lista_vacia)
            } else {
                pluralStringResource(R.plurals.eliminar_lista_cuerpo, n, n)
            },
            onConfirmar = acciones.borrarLista,
            onCancelar = acciones.cancelarBorrarLista,
        )
    }
}

/** Horizontal row of list chips (active one filled) ending with the dashed "+ Nueva" chip (spec 4.0). */
@Composable
private fun FilaChipsListas(
    listas: List<ListaConProgreso>,
    activaId: Long,
    onActivar: (Long) -> Unit,
    onNueva: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        // List chips reserve a 48 dp touch area; keep "+ Nueva" on the same centre line.
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(listas, key = { it.lista.id }) { lista ->
            ChipLista(
                lista = lista,
                activa = lista.lista.id == activaId,
                onClick = { if (lista.lista.id != activaId) onActivar(lista.lista.id) },
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "nueva") {
            val forma = RoundedCornerShape(50)
            val descripcion = stringResource(R.string.nueva_lista)
            Row(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(forma)
                    .background(MisTurnosColors.Background)
                    .bordeDiscontinuo(MisTurnosColors.AccentOnBackground)
                    .clickable(role = Role.Button, onClick = onNueva)
                    // TalkBack reads "Nueva lista" (the mockup's aria-label), not just "Nueva".
                    .semantics { contentDescription = descripcion }
                    .padding(start = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(MisTurnosIcons.Mas, contentDescription = null, tint = MisTurnosColors.AccentOnBackground, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.nueva),
                    style = MisTurnosText.chipNueva,
                    color = MisTurnosColors.AccentOnBackground,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

@Composable
private fun ChipLista(lista: ListaConProgreso, activa: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val progreso = pluralStringResource(R.plurals.progreso_tareas, lista.total, lista.hechas, lista.total)
    val descripcion = stringResource(
        if (activa) R.string.chip_lista_activa else R.string.chip_lista,
        lista.lista.nombre,
        progreso,
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (activa) MisTurnosColors.Accent else MisTurnosColors.Background,
        contentColor = if (activa) MisTurnosColors.OnAccent else MisTurnosColors.Text,
        border = BorderStroke(1.dp, if (activa) MisTurnosColors.Accent else MisTurnosColors.Line),
        modifier = modifier
            .heightIn(min = 36.dp)
            .semantics {
                contentDescription = descripcion
                selected = activa
            },
    ) {
        Row(
            // The chip's description replaces the visible texts for TalkBack.
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(lista.lista.nombre, style = MisTurnosText.chipPlantilla, maxLines = 1)
            Text(
                stringResource(R.string.progreso_corto, lista.hechas, lista.total),
                style = MisTurnosText.pill,
                color = if (activa) MisTurnosColors.OnAccent else MisTurnosColors.TextMuted,
                modifier = Modifier
                    .background(
                        if (activa) MisTurnosColors.OnAccent.copy(alpha = 0.22f) else MisTurnosColors.Surface,
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

/** Card of the active list: editable name, origin, "Reiniciar" and "Eliminar lista" (spec 4.0). */
@Composable
private fun TarjetaListaActiva(
    lista: ListaConProgreso,
    renombrando: Boolean,
    borradorNombre: String,
    acciones: AccionesChecklist,
) {
    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .fillMaxWidth()
            .background(MisTurnosColors.SurfaceInput, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (renombrando) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CampoEnLinea(
                        valor = borradorNombre,
                        onValorCambia = acciones.cambiarNombre,
                        onConfirmar = acciones.guardarNombre,
                        onCancelar = null,
                        descripcion = stringResource(R.string.nombre_lista),
                        capitalizacion = KeyboardCapitalization.Sentences,
                        estilo = MisTurnosText.nameInput,
                        fondo = MisTurnosColors.Background,
                        borde = BorderStroke(2.dp, MisTurnosColors.AccentOnBackground),
                        modifier = Modifier.weight(1f),
                    )
                    BotonGuardar(acciones.guardarNombre)
                }
            } else {
                val descripcion = stringResource(R.string.cambiar_nombre_lista)
                Row(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .clickable(role = Role.Button, onClick = acciones.empezarRenombrar)
                        .semantics(mergeDescendants = true) { contentDescription = descripcion },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        lista.lista.nombre,
                        style = MisTurnosText.cardTitle,
                        color = MisTurnosColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .semantics { heading() },
                    )
                    Icon(MisTurnosIcons.Lapiz, contentDescription = null, tint = MisTurnosColors.TextMuted, modifier = Modifier.size(14.dp))
                }
                Text(
                    lista.nombrePlantilla?.let { stringResource(R.string.desde_la_plantilla, it) }
                        ?: stringResource(R.string.lista_en_blanco),
                    style = MisTurnosText.listOrigin,
                    color = MisTurnosColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = acciones.reiniciar, modifier = Modifier.size(44.dp)) {
            Icon(
                MisTurnosIcons.Reiniciar,
                contentDescription = stringResource(R.string.reiniciar_lista),
                tint = MisTurnosColors.AccentOnBackground,
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = acciones.pedirBorrarLista, modifier = Modifier.size(44.dp)) {
            Icon(
                MisTurnosIcons.Papelera,
                contentDescription = stringResource(R.string.eliminar_lista),
                tint = MisTurnosColors.Danger,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ListaTareas(estado: ChecklistUiState, acciones: AccionesChecklist, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!estado.cargando && estado.tareas.isEmpty()) {
            item(key = "vacia") {
                EstadoVacio(
                    titulo = stringResource(R.string.lista_vacia),
                    ayuda = stringResource(R.string.sin_tareas_ayuda),
                    icono = MisTurnosIcons.ChecklistVacio,
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                )
            }
        }
        items(estado.tareas, key = { it.id }) { tarea ->
            FilaTarea(
                tarea = tarea,
                onMarcar = { hecha -> acciones.marcar(tarea, hecha) },
                onBorrar = { acciones.borrar(tarea) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/** Filled 48 dp "+ Nueva lista" button of the "Sin listas todavía" state. */
@Composable
private fun BotonNuevaLista(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MisTurnosColors.Accent, contentColor = MisTurnosColors.OnAccent),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Icon(MisTurnosIcons.Mas, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.nueva_lista), style = MisTurnosText.addButton, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun FilaTarea(
    tarea: Tarea,
    onMarcar: (Boolean) -> Unit,
    onBorrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(forma)
            .background(if (tarea.hecha) MisTurnosColors.SurfaceInput else MisTurnosColors.Background)
            .border(1.dp, MisTurnosColors.LineSoft, forma)
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Checkbox + text form a single toggle, like <label for> in the mockup.
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .toggleable(value = tarea.hecha, role = Role.Checkbox, onValueChange = onMarcar),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(
                checked = tarea.hecha,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MisTurnosColors.AccentOnBackground,
                    uncheckedColor = MisTurnosColors.TextMuted,
                    checkmarkColor = MisTurnosColors.Background,
                ),
            )
            Text(
                text = tarea.texto,
                style = MisTurnosText.body,
                color = if (tarea.hecha) MisTurnosColors.TextFaint else MisTurnosColors.Text,
                textDecoration = if (tarea.hecha) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        IconButton(onClick = onBorrar, modifier = Modifier.size(44.dp)) {
            Icon(
                MisTurnosIcons.Papelera,
                contentDescription = stringResource(R.string.borrar_tarea),
                tint = MisTurnosColors.Danger,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(name = "Checklist", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun ChecklistScreenPreview() = ChecklistMuestra(oscuro = false)

@Preview(name = "Checklist oscuro", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun ChecklistScreenOscuroPreview() = ChecklistMuestra(oscuro = true)

@Preview(name = "Checklist nueva lista", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun ChecklistNuevaPreview() = ChecklistMuestra(oscuro = false, hojaNueva = true)

@Preview(name = "Checklist sin listas", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun ChecklistSinListasPreview() {
    MisTurnosTheme {
        ChecklistScreen(
            estado = ChecklistUiState(cargando = false),
            borrador = "",
            borradorNombre = "",
            snackbarHostState = remember { SnackbarHostState() },
            acciones = AccionesChecklist.Vacias,
        )
    }
}

@Composable
private fun ChecklistMuestra(oscuro: Boolean, hojaNueva: Boolean = false) {
    val listas = DatosMuestra.listas()
    MisTurnosTheme(oscuro = oscuro) {
        ChecklistScreen(
            estado = ChecklistUiState(
                listas = listas,
                activa = listas.first(),
                tareas = DatosMuestra.tareas(),
                plantillasTareas = DatosMuestra.plantillas().filter { it.plantilla.tipo == TipoPlantilla.TAREAS },
                cargando = false,
                hojaNuevaAbierta = hojaNueva,
            ),
            borrador = "",
            borradorNombre = "",
            snackbarHostState = remember { SnackbarHostState() },
            acciones = AccionesChecklist.Vacias,
        )
    }
}
