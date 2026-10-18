package com.valanon.calendar.ui.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.ui.componentes.AsaHoja
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.componentes.bordeDiscontinuo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.plantillas.InsigniaTipo
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import kotlinx.coroutines.launch

/**
 * "Nueva lista" sheet (spec 4.5): one card per "Tareas" template, a notice when there is none,
 * "Lista en blanco" and "Cancelar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorPlantillaTareas(
    plantillas: List<PlantillaConRecuento>,
    onElegir: (Long) -> Unit,
    onEnBlanco: () -> Unit,
    onCerrar: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // One choice per sheet: a second tap during the hide animation would create a second list.
    var elegido by remember { mutableStateOf(false) }
    val cerrarY: (() -> Unit) -> Unit = { accion ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { accion() }
    }
    val elegir: (() -> Unit) -> Unit = { accion ->
        if (!elegido) {
            elegido = true
            cerrarY(accion)
        }
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
                Sobretitulo(stringResource(R.string.nueva_lista))
                Text(stringResource(R.string.desde_que_plantilla), style = MisTurnosText.sheetTitle)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plantillas.forEach { item ->
                    TarjetaPreset(item, onClick = { elegir { onElegir(item.plantilla.id) } })
                }
                if (plantillas.isEmpty()) {
                    Text(
                        stringResource(R.string.sin_plantillas_tareas),
                        style = MisTurnosText.cardSubtitle,
                        color = MisTurnosColors.TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MisTurnosColors.SurfaceInput, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                OpcionEnBlanco(onClick = { elegir(onEnBlanco) })
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

/** 64 dp card of a "Tareas" template: violet badge, name, "5 tareas" and chevron. */
@Composable
private fun TarjetaPreset(item: PlantillaConRecuento, onClick: () -> Unit) {
    val n = item.numElementos
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MisTurnosColors.Sheet,
        contentColor = MisTurnosColors.Text,
        border = BorderStroke(1.dp, MisTurnosColors.Line),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InsigniaTipo(TipoPlantilla.TAREAS, tamano = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.plantilla.nombre, style = MisTurnosText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.contador_tareas, n, n),
                    style = MisTurnosText.cardSubtitle,
                    color = MisTurnosColors.TextMuted,
                )
            }
            Icon(MisTurnosIcons.ChevronDerecha, contentDescription = null, tint = MisTurnosColors.TextFaint, modifier = Modifier.size(20.dp))
        }
    }
}

/** 56 dp "Lista en blanco" option with a dashed outline and a neutral + badge. */
@Composable
private fun OpcionEnBlanco(onClick: () -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(forma)
            .bordeDiscontinuo(MisTurnosColors.UnassignedBorder, radio = 16.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MisTurnosColors.Surface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(MisTurnosIcons.Mas, contentDescription = null, tint = MisTurnosColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        Text(stringResource(R.string.lista_en_blanco), style = MisTurnosText.button)
    }
}
