@file:OptIn(ExperimentalMaterial3Api::class)

package com.valanon.calendar.ui.copia

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.ui.componentes.AsaHoja
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 44 dp icon button that opens the backup sheet. */
@Composable
fun BotonCopiaSeguridad(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            MisTurnosIcons.Copia,
            contentDescription = stringResource(R.string.copia_seguridad),
            tint = MisTurnosColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Backup sheet, file pickers (Storage Access Framework), import confirmation and result
 * snackbar. Place it once in a screen and open it with [CopiaSeguridadViewModel.abrirHoja].
 */
@Composable
fun CopiaSeguridadHost(
    snackbarHostState: SnackbarHostState,
    viewModel: CopiaSeguridadViewModel = viewModel(factory = CopiaSeguridadViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val recursos = LocalResources.current

    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportar(uri)
    }
    // Some file managers label JSON as text or binary; validation happens on import anyway.
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.pedirImportar(uri)
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(estado.resultado) {
        val resultado = estado.resultado ?: return@LaunchedEffect
        val mensaje = when (resultado) {
            is ResultadoCopia.Exportada -> recursos.getString(R.string.copia_exportada)
            is ResultadoCopia.Importada -> recursos.getString(R.string.copia_importada)
            is ResultadoCopia.Error -> resultado.motivo ?: recursos.getString(R.string.copia_error)
        }
        // Shown in its own scope: clearing the result changes this effect's key and would
        // otherwise cancel the snackbar before it appears.
        scope.launch { snackbarHostState.showSnackbar(mensaje) }
        viewModel.resultadoMostrado()
    }

    if (estado.hojaAbierta) {
        HojaCopiaSeguridad(
            trabajando = estado.trabajando,
            onExportar = {
                viewModel.cerrarHoja()
                exportar.launch(recursos.getString(R.string.copia_nombre_archivo, LocalDate.now().toString()))
            },
            onImportar = {
                viewModel.cerrarHoja()
                importar.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            },
            onCerrar = viewModel::cerrarHoja,
        )
    }

    if (estado.importacionPendiente != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarImportar,
            title = { Text(stringResource(R.string.importar_confirmar_titulo), style = MisTurnosText.sheetTitle) },
            text = { Text(stringResource(R.string.importar_confirmar_cuerpo), style = MisTurnosText.body) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarImportar) {
                    Text(stringResource(R.string.importar), style = MisTurnosText.buttonSmall, color = MisTurnosColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarImportar) {
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
private fun HojaCopiaSeguridad(
    trabajando: Boolean,
    onExportar: () -> Unit,
    onImportar: () -> Unit,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Sobretitulo(stringResource(R.string.copia_seguridad))
                Text(stringResource(R.string.copia_titulo), style = MisTurnosText.sheetTitle)
            }
            Text(stringResource(R.string.copia_explicacion), style = MisTurnosText.cardSubtitle, color = MisTurnosColors.TextMuted)
            BotonHoja(
                texto = stringResource(R.string.exportar_datos),
                relleno = true,
                habilitado = !trabajando,
                onClick = onExportar,
            )
            BotonHoja(
                texto = stringResource(R.string.importar_datos),
                relleno = false,
                habilitado = !trabajando,
                onClick = onImportar,
            )
            Surface(
                onClick = onCerrar,
                shape = RoundedCornerShape(14.dp),
                color = MisTurnosColors.SheetButton,
                contentColor = MisTurnosColors.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.cancelar), style = MisTurnosText.buttonSmall)
                }
            }
        }
    }
}

@Composable
private fun BotonHoja(texto: String, relleno: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        color = if (relleno) MisTurnosColors.Accent else MisTurnosColors.Sheet,
        contentColor = if (relleno) MisTurnosColors.OnAccent else MisTurnosColors.AccentOnBackground,
        border = if (relleno) null else BorderStroke(1.dp, MisTurnosColors.AccentOnBackground),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(texto, style = MisTurnosText.button)
        }
    }
}
