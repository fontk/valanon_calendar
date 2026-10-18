package com.valanon.calendar.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.colores
import com.valanon.calendar.util.CalendarioMes
import com.valanon.calendar.util.FormatoFechas
import java.time.LocalDate
import java.time.YearMonth

/**
 * Month view pieces shared by Calendario (the user's shifts) and Turnos (the team's shifts):
 * header with arrows and month/year pill, weekday header, the 7-column grid with a cell slot,
 * and the shift legend.
 */

/** Callbacks of the month/year picker, grouped to keep screen signatures readable. */
data class AccionesSelectorMes(
    val abrir: () -> Unit,
    val cerrar: () -> Unit,
    val anioAnterior: () -> Unit,
    val anioSiguiente: () -> Unit,
    val irAMes: (anio: Int, mes: Int) -> Unit,
    val irAHoy: () -> Unit,
) {
    companion object {
        /** For previews. */
        val Vacias = AccionesSelectorMes({}, {}, {}, {}, { _, _ -> }, {})
    }
}

/** Overline + "‹ [Octubre 2026 ⌄] ›" (spec 3.1). [sobretitulo] is shown uppercase. */
@Composable
fun CabeceraMes(
    sobretitulo: String,
    mes: YearMonth,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onAbrirSelectorMes: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Sobretitulo(sobretitulo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonRedondo(MisTurnosIcons.ChevronIzquierda, stringResource(R.string.mes_anterior), onMesAnterior)
            BotonSelectorMes(mes, onAbrirSelectorMes)
            BotonRedondo(MisTurnosIcons.ChevronDerecha, stringResource(R.string.mes_siguiente), onMesSiguiente)
        }
    }
}

/** Month title as a pill button that opens the month/year picker (spec 3.1). */
@Composable
private fun BotonSelectorMes(mes: YearMonth, onClick: () -> Unit) {
    val descripcion = stringResource(R.string.elegir_mes_y_anio)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MisTurnosColors.Surface,
        contentColor = MisTurnosColors.Text,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .semantics { contentDescription = descripcion },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = FormatoFechas.tituloMes(mes),
                style = MisTurnosText.monthPicker,
                maxLines = 1,
                modifier = Modifier.semantics { heading() },
            )
            Icon(
                MisTurnosIcons.ChevronAbajo,
                contentDescription = null,
                tint = MisTurnosColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun BotonRedondo(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MisTurnosColors.Background,
        border = BorderStroke(1.dp, MisTurnosColors.Line),
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = MisTurnosColors.Text, modifier = Modifier.size(20.dp))
        }
    }
}

/** Five shift chips with their dot (spec 3.2). Turnos uses a 4 dp vertical padding (DESIGN.md). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeyendaTurnos(modifier: Modifier = Modifier, paddingVertical: Dp = 5.dp) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TipoTurno.entries.forEach { tipo ->
            val c = tipo.colores
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.fondo)
                    // Dark mode: outline keeps the "Noche" chip visible on the dark background.
                    .then(if (MisTurnosColors.EsOscuro) Modifier.border(1.dp, c.borde, CircleShape) else Modifier)
                    .padding(horizontal = 10.dp, vertical = paddingVertical),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(c.punto),
                )
                Text(tipo.etiqueta, style = MisTurnosText.chip, color = c.texto)
            }
        }
    }
}

/** L M X J V S D, Monday first. Hidden from accessibility (each cell describes its date). */
@Composable
fun CabeceraDiasSemana(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FormatoFechas.INICIALES_SEMANA.forEach { inicial ->
            Text(
                text = inicial,
                style = MisTurnosText.chip,
                color = MisTurnosColors.TextFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 7-column month grid (gap 6, cells ≥ 60 dp, Monday first). [celda] draws each day; it gets
 * a modifier with the cell's weight and full row height that it must apply.
 */
@Composable
fun RejillaMes(
    mes: YearMonth,
    modifier: Modifier = Modifier,
    celda: @Composable (fecha: LocalDate, modifier: Modifier) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CalendarioMes.semanas(mes).forEach { semana ->
            // IntrinsicSize.Min keeps every cell of a week the same height when text grows.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                semana.forEach { fecha ->
                    val modificador = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    if (fecha == null) {
                        Spacer(modificador.heightIn(min = 60.dp))
                    } else {
                        celda(fecha, modificador)
                    }
                }
            }
        }
    }
}
