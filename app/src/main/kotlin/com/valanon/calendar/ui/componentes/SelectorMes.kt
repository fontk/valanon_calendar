@file:OptIn(ExperimentalMaterial3Api::class)

package com.valanon.calendar.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import com.valanon.calendar.util.FormatoFechas
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * "Ir a mes" bottom sheet (spec 3.1.1). Shows [anio] with ±1 arrows, the 12 months in a 3×4
 * grid and "Hoy" / "Cancelar". Every way of leaving animates the sheet out first and then
 * runs the action; swiping down, the scrim or back count as "Cancelar".
 */
@Composable
fun SelectorMes(
    anio: Int,
    mesActivo: YearMonth,
    hoy: LocalDate,
    onAnioAnterior: () -> Unit,
    onAnioSiguiente: () -> Unit,
    onElegirMes: (anio: Int, mes: Int) -> Unit,
    onHoy: () -> Unit,
    onCerrar: () -> Unit,
) {
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
        ContenidoSelectorMes(
            anio = anio,
            mesActivo = mesActivo,
            hoy = hoy,
            onAnioAnterior = onAnioAnterior,
            onAnioSiguiente = onAnioSiguiente,
            onElegirMes = { a, m -> cerrarY { onElegirMes(a, m) } },
            onHoy = { cerrarY(onHoy) },
            onCancelar = { cerrarY(onCerrar) },
        )
    }
}

/** Sheet body, separate from [ModalBottomSheet] so it can be previewed. */
@Composable
private fun ContenidoSelectorMes(
    anio: Int,
    mesActivo: YearMonth,
    hoy: LocalDate,
    onAnioAnterior: () -> Unit,
    onAnioSiguiente: () -> Unit,
    onElegirMes: (anio: Int, mes: Int) -> Unit,
    onHoy: () -> Unit,
    onCancelar: () -> Unit,
) {
    val abreviaturas = stringArrayResource(R.array.meses_abreviados)
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState()) // landscape: the sheet is taller than the screen
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Sobretitulo(stringResource(R.string.ir_a_mes))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonCircular(MisTurnosIcons.ChevronIzquierda, stringResource(R.string.anio_anterior), onAnioAnterior)
            Text(
                text = anio.toString(),
                style = MisTurnosText.yearLabel,
                modifier = Modifier.semantics { heading() },
            )
            BotonCircular(MisTurnosIcons.ChevronDerecha, stringResource(R.string.anio_siguiente), onAnioSiguiente)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..12).chunked(3).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fila.forEach { mes ->
                        BotonMes(
                            abreviatura = abreviaturas[mes - 1],
                            descripcion = stringResource(R.string.mes_de_anio, FormatoFechas.nombreMes(mes), anio),
                            activo = mesActivo.year == anio && mesActivo.monthValue == mes,
                            esMesDeHoy = hoy.year == anio && hoy.monthValue == mes,
                            onClick = { onElegirMes(anio, mes) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val forma = RoundedCornerShape(14.dp)
            Surface(
                onClick = onHoy,
                shape = forma,
                color = MisTurnosColors.Sheet,
                contentColor = MisTurnosColors.AccentOnBackground,
                border = BorderStroke(1.dp, MisTurnosColors.AccentOnBackground),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.hoy), style = MisTurnosText.buttonSmall)
                }
            }
            Surface(
                onClick = onCancelar,
                shape = forma,
                color = MisTurnosColors.SheetButton,
                contentColor = MisTurnosColors.Text,
                modifier = Modifier
                    .weight(1f)
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
private fun BotonMes(
    abreviatura: String,
    descripcion: String,
    activo: Boolean,
    esMesDeHoy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borde = when {
        activo -> BorderStroke(2.dp, MisTurnosColors.Accent)
        esMesDeHoy -> BorderStroke(2.dp, MisTurnosColors.TodayRing)
        else -> BorderStroke(1.dp, MisTurnosColors.Line)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (activo) MisTurnosColors.Accent else MisTurnosColors.Sheet,
        contentColor = if (activo) MisTurnosColors.OnAccent else MisTurnosColors.Text,
        border = borde,
        modifier = modifier
            .heightIn(min = 52.dp)
            .semantics {
                contentDescription = descripcion
                selected = activo
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = abreviatura,
                style = MisTurnosText.monthCell,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun BotonCircular(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MisTurnosColors.Sheet,
        border = BorderStroke(1.dp, MisTurnosColors.Line),
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = MisTurnosColors.Text, modifier = Modifier.size(20.dp))
        }
    }
}

/** 36×4 drag handle shared by the bottom sheets. */
@Composable
fun AsaHoja() {
    Box(
        Modifier
            .padding(top = 12.dp)
            .size(width = 36.dp, height = 4.dp)
            .background(MisTurnosColors.Handle, RoundedCornerShape(2.dp)),
    )
}

@Preview(name = "Selector de mes", widthDp = 390, showBackground = true)
@Composable
private fun SelectorMesPreview() = MuestraSelectorMes(oscuro = false)

@Preview(name = "Selector de mes oscuro", widthDp = 390, showBackground = true, backgroundColor = 0xFF1F1F1D)
@Composable
private fun SelectorMesOscuroPreview() = MuestraSelectorMes(oscuro = true)

@Composable
private fun MuestraSelectorMes(oscuro: Boolean) {
    MisTurnosTheme(oscuro = oscuro) {
        Box(Modifier.background(MisTurnosColors.Sheet)) {
            ContenidoSelectorMes(
                anio = 2026,
                // Visible month March 2026, today in October: shows both "activo" and "hoy" states.
                mesActivo = YearMonth.of(2026, 3),
                hoy = LocalDate.of(2026, 10, 17),
                onAnioAnterior = {},
                onAnioSiguiente = {},
                onElegirMes = { _, _ -> },
                onHoy = {},
                onCancelar = {},
            )
        }
    }
}
