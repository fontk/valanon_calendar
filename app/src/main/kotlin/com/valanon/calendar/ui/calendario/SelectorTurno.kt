@file:OptIn(ExperimentalMaterial3Api::class)

package com.valanon.calendar.ui.calendario

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.ui.componentes.AsaHoja
import com.valanon.calendar.ui.componentes.ContenidoSelectorTurno
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.util.FormatoFechas
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Calendario's "Turno del día" bottom sheet: the shared [ContenidoSelectorTurno] in its own
 * sheet. Choosing an option saves it immediately and then the sheet animates out;
 * "Cancelar", the scrim or back close it without changes.
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
        ContenidoSelectorTurno(
            sobretitulo = stringResource(R.string.turno_del_dia),
            titulo = FormatoFechas.tituloDia(fecha),
            actual = actual,
            onElegir = { tipo ->
                onElegir(tipo)
                cerrarAnimado()
            },
            onCancelar = cerrarAnimado,
        )
    }
}
