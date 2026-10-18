package com.valanon.calendar.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.colores

/** Optional destructive action next to "Cancelar" (e.g. "Quitar turno"). */
data class AccionSecundaria(val texto: String, val onClick: () -> Unit)

/**
 * Body of the shift picker (spec 3.4), shared by Calendario ("Turno del día") and Turnos
 * ("Turno de <nombre>", spec 3b.3.1). Hosts place it inside their own bottom sheet:
 * - [onVolver] adds the 44 dp back button of the Turnos variant before the titles.
 * - [accionSecundaria] adds a second 48 dp button ("Quitar turno") to the left of "Cancelar".
 * The options always show the five shifts in the same order, with "Actual" on [actual].
 */
@Composable
fun ContenidoSelectorTurno(
    sobretitulo: String,
    titulo: String,
    actual: TipoTurno?,
    onElegir: (TipoTurno) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
    accionSecundaria: AccionSecundaria? = null,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()) // landscape: the sheet is taller than the screen
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onVolver != null) {
                Surface(
                    onClick = onVolver,
                    shape = CircleShape,
                    color = MisTurnosColors.SheetButton,
                    contentColor = MisTurnosColors.Text,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MisTurnosIcons.FlechaAtras,
                            contentDescription = stringResource(R.string.volver_al_equipo),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Sobretitulo(sobretitulo)
                Text(titulo, style = MisTurnosText.sheetTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoTurno.entries.forEach { tipo ->
                OpcionTurno(tipo = tipo, esActual = tipo == actual, onClick = { onElegir(tipo) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val forma = RoundedCornerShape(14.dp)
            if (accionSecundaria != null) {
                Surface(
                    onClick = accionSecundaria.onClick,
                    shape = forma,
                    color = MisTurnosColors.Sheet,
                    contentColor = MisTurnosColors.Danger,
                    border = BorderStroke(1.dp, MisTurnosColors.DangerBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(accionSecundaria.texto, style = MisTurnosText.buttonSmall)
                    }
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
private fun OpcionTurno(tipo: TipoTurno, esActual: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MisTurnosColors.Sheet,
        contentColor = MisTurnosColors.Text,
        border = if (esActual) {
            BorderStroke(2.dp, MisTurnosColors.AccentOnBackground)
        } else {
            BorderStroke(1.dp, MisTurnosColors.Line)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics {
                role = Role.RadioButton
                selected = esActual
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val formaMuestra = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .size(28.dp)
                    .background(tipo.colores.muestra, formaMuestra)
                    // In dark mode the "noche" swatch would vanish against the sheet.
                    .then(if (MisTurnosColors.EsOscuro) Modifier.border(1.dp, tipo.colores.borde, formaMuestra) else Modifier),
            )
            Text(tipo.etiqueta, style = MisTurnosText.button, modifier = Modifier.weight(1f))
            if (esActual) {
                Text(stringResource(R.string.turno_actual), style = MisTurnosText.caption, color = MisTurnosColors.AccentOnBackground)
            }
        }
    }
}
