package com.valanon.calendar.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.ui.componentes.CabeceraPestana
import com.valanon.calendar.ui.componentes.CampoAlta
import com.valanon.calendar.ui.componentes.EstadoVacio
import com.valanon.calendar.ui.componentes.FabAccion
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
        snackbarHostState = snackbar,
        onBorradorCambia = viewModel::actualizarBorrador,
        onAgregar = viewModel::agregar,
        onMarcar = viewModel::marcar,
        onBorrar = { tarea ->
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
        onReiniciar = viewModel::reiniciarTodas,
    )
}

@Composable
fun ChecklistScreen(
    estado: ChecklistUiState,
    borrador: String,
    snackbarHostState: SnackbarHostState,
    onBorradorCambia: (String) -> Unit,
    onAgregar: () -> Unit,
    onMarcar: (Tarea, Boolean) -> Unit,
    onBorrar: (Tarea) -> Unit,
    onReiniciar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MisTurnosColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            CabeceraPestana(
                titulo = stringResource(R.string.checklist_titulo),
                resumen = if (estado.total > 0) {
                    pluralStringResource(R.plurals.progreso_tareas, estado.total, estado.hechas, estado.total)
                } else {
                    null
                },
            )
            CampoAlta(
                valor = borrador,
                onValorCambia = onBorradorCambia,
                onAgregar = onAgregar,
                placeholder = stringResource(R.string.nueva_tarea_placeholder),
                descripcion = stringResource(R.string.nueva_tarea),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
            if (!estado.cargando && estado.tareas.isEmpty()) {
                EstadoVacio(
                    titulo = stringResource(R.string.sin_tareas),
                    ayuda = stringResource(R.string.sin_tareas_ayuda),
                    icono = MisTurnosIcons.ChecklistVacio,
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp, start = 20.dp, end = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // 100 dp at the bottom so the FAB never covers the last row.
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(estado.tareas, key = { it.id }) { tarea ->
                        FilaTarea(
                            tarea = tarea,
                            onMarcar = { hecha -> onMarcar(tarea, hecha) },
                            onBorrar = { onBorrar(tarea) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        FabAccion(
            icono = MisTurnosIcons.Reiniciar,
            descripcion = stringResource(R.string.reiniciar_tareas),
            onClick = onReiniciar,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp),
        )
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

@Composable
private fun ChecklistMuestra(oscuro: Boolean) {
    MisTurnosTheme(oscuro = oscuro) {
        ChecklistScreen(
            estado = ChecklistUiState(tareas = DatosMuestra.tareas(), cargando = false),
            borrador = "",
            snackbarHostState = remember { SnackbarHostState() },
            onBorradorCambia = {},
            onAgregar = {},
            onMarcar = { _, _ -> },
            onBorrar = {},
            onReiniciar = {},
        )
    }
}

@Preview(name = "Checklist vacía", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun ChecklistVaciaPreview() {
    MisTurnosTheme {
        ChecklistScreen(
            estado = ChecklistUiState(tareas = emptyList(), cargando = false),
            borrador = "",
            snackbarHostState = remember { SnackbarHostState() },
            onBorradorCambia = {},
            onAgregar = {},
            onMarcar = { _, _ -> },
            onBorrar = {},
            onReiniciar = {},
        )
    }
}
