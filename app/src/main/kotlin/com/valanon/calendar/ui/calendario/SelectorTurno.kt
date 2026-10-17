@file:OptIn(ExperimentalMaterial3Api::class)

package com.valanon.calendar.ui.calendario

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.colores
import com.valanon.calendar.util.FormatoFechas
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * "Turno del día" bottom sheet. Choosing an option saves it immediately and then the sheet
 * animates out; "Cancelar", the scrim or back close it without changes.
 */
@Composable
fun SelectorTurno(
    fecha: LocalDate,
    actual: TipoTurno?,
    onElegir: (TipoTurno) -> Unit,
    onCerrar: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val cerrarAnimado: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onCerrar() }
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
                .verticalScroll(rememberScrollState()) // landscape: the sheet is taller than the screen
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Sobretitulo(stringResource(R.string.turno_del_dia))
                Text(FormatoFechas.tituloDia(fecha), style = MisTurnosText.sheetTitle)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoTurno.entries.forEach { tipo ->
                    OpcionTurno(
                        tipo = tipo,
                        esActual = tipo == actual,
                        onClick = {
                            onElegir(tipo)
                            cerrarAnimado()
                        },
                    )
                }
            }
            Surface(
                onClick = cerrarAnimado,
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
