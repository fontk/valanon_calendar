@file:OptIn(ExperimentalMaterial3Api::class)

package com.valanon.calendar.ui.plantillas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.ui.calendario.AsaHoja
import com.valanon.calendar.ui.componentes.CabeceraPestana
import com.valanon.calendar.ui.componentes.EstadoVacio
import com.valanon.calendar.ui.componentes.FabAccion
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import kotlinx.coroutines.launch

/**
 * Stateful entry point of the templates list. [onAbrirPlantilla] navigates to the detail;
 * `renombrar = true` right after creating one (spec 4b.2).
 */
@Composable
fun PlantillasRoute(
    onAbrirPlantilla: (id: Long, renombrar: Boolean) -> Unit,
    viewModel: PlantillasViewModel = viewModel(factory = PlantillasViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    PlantillasScreen(
        estado = estado,
        onAbrir = { onAbrirPlantilla(it, false) },
        onAnadir = viewModel::abrirHojaTipos,
        onElegirTipo = { tipo ->
            scope.launch { onAbrirPlantilla(viewModel.crear(tipo), true) }
        },
        onCerrarHoja = viewModel::cerrarHojaTipos,
    )
}

@Composable
fun PlantillasScreen(
    estado: PlantillasUiState,
    onAbrir: (Long) -> Unit,
    onAnadir: () -> Unit,
    onElegirTipo: (TipoPlantilla) -> Unit,
    onCerrarHoja: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MisTurnosColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            val total = estado.plantillas.size
            CabeceraPestana(
                titulo = stringResource(R.string.plantillas_titulo),
                resumen = if (total > 0) pluralStringResource(R.plurals.contador_plantillas, total, total) else null,
            )
            if (!estado.cargando && estado.plantillas.isEmpty()) {
                EstadoVacio(
                    titulo = stringResource(R.string.sin_plantillas),
                    ayuda = stringResource(R.string.sin_plantillas_ayuda),
                    icono = MisTurnosIcons.PlantillasVacio,
                    anchoAyuda = 240.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp, start = 20.dp, end = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // 100 dp at the bottom so the FAB never covers the last card.
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(estado.plantillas, key = { it.plantilla.id }) { item ->
                        TarjetaPlantilla(item, onClick = { onAbrir(item.plantilla.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }

        FabAccion(
            icono = MisTurnosIcons.Mas,
            descripcion = stringResource(R.string.anadir_plantilla),
            onClick = onAnadir,
            tamanoIcono = 28.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        )
    }

    if (estado.hojaTiposAbierta) {
        SelectorTipoPlantilla(onElegir = onElegirTipo, onCerrar = onCerrarHoja)
    }
}

@Composable
private fun TarjetaPlantilla(item: PlantillaConRecuento, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val plantilla = item.plantilla
    val subtitulo = stringResource(
        R.string.subtitulo_plantilla,
        plantilla.tipo.etiqueta,
        pluralStringResource(R.plurals.contador_trabajadores, item.numTrabajadores, item.numTrabajadores),
    )
    TarjetaConInsignia(
        tipo = plantilla.tipo,
        titulo = plantilla.nombre,
        subtitulo = subtitulo,
        borde = MisTurnosColors.LineSoft,
        onClick = onClick,
        modifier = modifier,
    )
}

/** 72 dp card: type badge, title (one line, ellipsis), subtitle and chevron. */
@Composable
private fun TarjetaConInsignia(
    tipo: TipoPlantilla,
    titulo: String,
    subtitulo: String,
    borde: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MisTurnosColors.Background,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color,
        contentColor = MisTurnosColors.Text,
        border = BorderStroke(1.dp, borde),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InsigniaTipo(tipo)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MisTurnosText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitulo, style = MisTurnosText.cardSubtitle, color = MisTurnosColors.TextMuted)
            }
            Icon(
                MisTurnosIcons.ChevronDerecha,
                contentDescription = null,
                tint = MisTurnosColors.TextFaint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** "¿Qué tipo de plantilla?" sheet (spec 4b.2): one card per [TipoPlantilla] + "Cancelar". */
@Composable
private fun SelectorTipoPlantilla(onElegir: (TipoPlantilla) -> Unit, onCerrar: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val cerrarY: (() -> Unit) -> Unit = { accion ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { accion() }
    }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Sobretitulo(stringResource(R.string.nueva_plantilla))
                Text(stringResource(R.string.que_tipo_plantilla), style = MisTurnosText.sheetTitle)
            }
            TipoPlantilla.entries.forEach { tipo ->
                TarjetaConInsignia(
                    tipo = tipo,
                    titulo = tipo.etiqueta,
                    subtitulo = tipo.descripcion,
                    borde = MisTurnosColors.Line,
                    color = MisTurnosColors.Sheet,
                    onClick = { cerrarY { onElegir(tipo) } },
                )
            }
            Surface(
                onClick = { cerrarY(onCerrar) },
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

@Preview(name = "Plantillas", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun PlantillasPreview() = MuestraPlantillas(oscuro = false, vacia = false)

@Preview(name = "Plantillas oscuro", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun PlantillasOscuroPreview() = MuestraPlantillas(oscuro = true, vacia = false)

@Preview(name = "Plantillas vacía", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun PlantillasVaciaPreview() = MuestraPlantillas(oscuro = false, vacia = true)

@Composable
private fun MuestraPlantillas(oscuro: Boolean, vacia: Boolean) {
    MisTurnosTheme(oscuro = oscuro) {
        PlantillasScreen(
            estado = PlantillasUiState(
                plantillas = if (vacia) emptyList() else DatosMuestra.plantillas(),
                cargando = false,
            ),
            onAbrir = {},
            onAnadir = {},
            onElegirTipo = {},
            onCerrarHoja = {},
        )
    }
}
