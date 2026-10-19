package com.valanon.calendar.ui.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.BuildConfig
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.ui.componentes.CabeceraPestana
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import com.valanon.calendar.ui.theme.colores

/** Stateful entry point of the Ajustes tab. [onAbrirTipos] opens "Tipos de turno". */
@Composable
fun AjustesRoute(
    onAbrirTipos: () -> Unit,
    viewModel: AjustesViewModel = viewModel(factory = AjustesViewModel.Factory),
) {
    val tipos by viewModel.tipos.collectAsStateWithLifecycle()
    AjustesScreen(tipos = tipos, version = BuildConfig.VERSION_NAME, onAbrirTipos = onAbrirTipos)
}

/** Settings grouped by section (spec 3c.1); the only one today is "Tipos de turno". */
@Composable
fun AjustesScreen(
    tipos: List<TipoTurno>,
    version: String,
    onAbrirTipos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MisTurnosColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        CabeceraPestana(titulo = stringResource(R.string.ajustes_titulo), resumen = null)
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Sobretitulo(stringResource(R.string.seccion_calendario), Modifier.padding(bottom = 4.dp))
            FilaTiposDeTurno(tipos, onAbrirTipos)
            Text(
                stringResource(R.string.version_app, version),
                style = MisTurnosText.listOrigin,
                color = MisTurnosColors.TextFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
    }
}

/** 72 dp card: tag badge, "Tipos de turno", one mini chip per type in its colours, chevron. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaTiposDeTurno(tipos: List<TipoTurno>, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MisTurnosColors.Background,
        contentColor = MisTurnosColors.Text,
        border = BorderStroke(1.dp, MisTurnosColors.LineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MisTurnosColors.SurfaceAccent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MisTurnosIcons.Etiqueta, contentDescription = null, tint = MisTurnosColors.AccentOnBackground, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.tipos_de_turno), style = MisTurnosText.cardTitle)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tipos.forEach { tipo ->
                        val c = tipo.colores
                        Text(
                            tipo.nombre,
                            style = MisTurnosText.pill.copy(fontWeight = MisTurnosText.chip.fontWeight),
                            color = c.texto,
                            maxLines = 1,
                            modifier = Modifier
                                .background(c.fondo, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Icon(MisTurnosIcons.ChevronDerecha, contentDescription = null, tint = MisTurnosColors.TextFaint, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(name = "Ajustes", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun AjustesPreview() = MuestraAjustes(oscuro = false)

@Preview(name = "Ajustes oscuro", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun AjustesOscuroPreview() = MuestraAjustes(oscuro = true)

@Composable
private fun MuestraAjustes(oscuro: Boolean) {
    MisTurnosTheme(oscuro = oscuro) {
        AjustesScreen(tipos = DatosMuestra.tipos(), version = "0.8.0", onAbrirTipos = {})
    }
}
