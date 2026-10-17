package com.valanon.calendar.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.ui.componentes.Sobretitulo
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
    val context = LocalContext.current
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
                    message = context.getString(R.string.tarea_borrada),
                    actionLabel = context.getString(R.string.deshacer),
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
            Cabecera(estado)
            AltaTarea(
                borrador = borrador,
                onBorradorCambia = onBorradorCambia,
                onAgregar = onAgregar,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
            if (!estado.cargando && estado.tareas.isEmpty()) {
                EstadoVacio(Modifier.fillMaxWidth().padding(top = 80.dp, start = 20.dp, end = 20.dp))
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

        FloatingActionButton(
            onClick = onReiniciar,
            shape = RoundedCornerShape(18.dp),
            containerColor = MisTurnosColors.Accent,
            contentColor = MisTurnosColors.OnAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(60.dp),
        ) {
            Icon(
                MisTurnosIcons.Reiniciar,
                contentDescription = stringResource(R.string.reiniciar_tareas),
                modifier = Modifier.size(26.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp),
        )
    }
}

@Composable
private fun Cabecera(estado: ChecklistUiState) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Sobretitulo(stringResource(R.string.sobretitulo))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.checklist_titulo),
                style = MisTurnosText.title,
                color = MisTurnosColors.Text,
                modifier = Modifier
                    .alignByBaseline()
                    .semantics { heading() },
            )
            if (estado.total > 0) {
                Text(
                    text = stringResource(R.string.progreso_tareas, estado.hechas, estado.total),
                    style = MisTurnosText.caption,
                    color = MisTurnosColors.TextMuted,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
private fun AltaTarea(
    borrador: String,
    onBorradorCambia: (String) -> Unit,
    onAgregar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(14.dp)
    val descripcionCampo = stringResource(R.string.nueva_tarea)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = borrador,
            onValueChange = onBorradorCambia,
            singleLine = true,
            textStyle = MisTurnosText.body.copy(color = MisTurnosColors.Text),
            cursorBrush = SolidColor(MisTurnosColors.Accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onAgregar() }),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp)
                .semantics { contentDescription = descripcionCampo },
            decorationBox = { campo ->
                Box(
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .clip(forma)
                        .background(MisTurnosColors.SurfaceInput)
                        .border(1.dp, MisTurnosColors.Line, forma)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (borrador.isEmpty()) {
                        Text(
                            stringResource(R.string.nueva_tarea_placeholder),
                            style = MisTurnosText.body,
                            color = MisTurnosColors.TextFaint,
                        )
                    }
                    campo()
                }
            },
        )
        Button(
            onClick = onAgregar,
            shape = forma,
            colors = ButtonDefaults.buttonColors(
                containerColor = MisTurnosColors.Accent,
                contentColor = MisTurnosColors.OnAccent,
            ),
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier.heightIn(min = 50.dp),
        ) {
            Icon(MisTurnosIcons.Mas, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.agregar), style = MisTurnosText.buttonSmall.copy(fontWeight = MisTurnosText.title.fontWeight))
        }
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
                    checkedColor = MisTurnosColors.Accent,
                    uncheckedColor = MisTurnosColors.TextMuted,
                    checkmarkColor = MisTurnosColors.OnAccent,
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

@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            MisTurnosIcons.ChecklistVacio,
            contentDescription = null,
            tint = MisTurnosColors.EmptyIcon,
            modifier = Modifier.size(44.dp),
        )
        Text(stringResource(R.string.sin_tareas), style = MisTurnosText.emptyTitle, color = MisTurnosColors.TextFaint)
        Text(
            stringResource(R.string.sin_tareas_ayuda),
            style = MisTurnosText.emptyHelp,
            color = MisTurnosColors.TextFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 220.dp),
        )
    }
}

@Preview(name = "Checklist", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun ChecklistScreenPreview() {
    MisTurnosTheme {
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
