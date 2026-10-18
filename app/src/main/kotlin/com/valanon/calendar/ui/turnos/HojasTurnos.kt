@file:OptIn(ExperimentalMaterial3Api::class)

package com.valanon.calendar.ui.turnos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.ui.componentes.AccionSecundaria
import com.valanon.calendar.ui.componentes.AsaHoja
import com.valanon.calendar.ui.componentes.Avatar
import com.valanon.calendar.ui.componentes.ContenidoSelectorTurno
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import com.valanon.calendar.ui.theme.colorPuntoCelda
import com.valanon.calendar.ui.theme.colores
import com.valanon.calendar.util.FormatoFechas
import kotlinx.coroutines.launch

/**
 * "Equipo del día" (spec 3b.3) and the worker's shift picker (3b.3.1) in ONE bottom sheet:
 * the content swaps with an animation, two sheets are never stacked. While the picker is
 * shown, swipe/scrim/Back return to the team list instead of closing; otherwise they close
 * the sheet (which also leaves edit mode).
 */
@Composable
fun EquipoDelDiaSheet(estado: TurnosUiState, acciones: AccionesTurnos) {
    val dia = estado.diaAbierto ?: return
    val selectorAbierto by rememberUpdatedState(estado.trabajadorSeleccionado != null)
    val volverAEquipo by rememberUpdatedState(acciones.volverAEquipo)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { destino ->
            if (destino == SheetValue.Hidden && selectorAbierto) {
                volverAEquipo()
                false
            } else {
                true
            }
        },
    )
    val scope = rememberCoroutineScope()
    val cerrarAnimado: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { acciones.cerrarDia() }
    }

    ModalBottomSheet(
        onDismissRequest = acciones.cerrarDia,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MisTurnosColors.Sheet,
        contentColor = MisTurnosColors.Text,
        scrimColor = MisTurnosColors.Scrim,
        dragHandle = { AsaHoja() },
        // Back is handled below: Material's own back handling would close the whole sheet
        // even while the worker's picker is shown (it does not consult confirmValueChange).
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        BackHandler {
            if (selectorAbierto) volverAEquipo() else cerrarAnimado()
        }
        AnimatedContent(
            targetState = estado.trabajadorSeleccionado,
            transitionSpec = {
                val haciaSelector = targetState != null
                (slideInHorizontally { if (haciaSelector) it / 4 else -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (haciaSelector) -it / 4 else it / 4 } + fadeOut())
            },
            contentKey = { it?.id },
            label = "equipoDelDia",
        ) { trabajador ->
            if (trabajador == null) {
                EquipoDelDia(estado = estado, acciones = acciones, onCerrar = cerrarAnimado)
            } else {
                val actual = estado.turnosDe(dia)[trabajador.id]
                ContenidoSelectorTurno(
                    sobretitulo = FormatoFechas.tituloDia(dia),
                    titulo = stringResource(R.string.turno_de, trabajador.nombre),
                    actual = actual,
                    onElegir = acciones.asignar,
                    onCancelar = acciones.volverAEquipo,
                    onVolver = acciones.volverAEquipo,
                    accionSecundaria = if (actual != null) {
                        AccionSecundaria(stringResource(R.string.quitar_turno), acciones.quitarTurno)
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun EquipoDelDia(estado: TurnosUiState, acciones: AccionesTurnos, onCerrar: () -> Unit) {
    val dia = estado.diaAbierto ?: return
    val turnos = estado.turnosDe(dia)
    val total = estado.trabajadores.size
    val asignados = estado.trabajadores.count { it.id in turnos }
    // ≈ 85 % of the screen; the list scrolls inside.
    val altoMaximo = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * 0.85f).toDp() }

    Column(
        modifier = Modifier
            .heightIn(max = altoMaximo)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Sobretitulo(stringResource(R.string.equipo_del_dia))
                Text(FormatoFechas.tituloDia(dia), style = MisTurnosText.sheetTitle)
                Text(
                    stringResource(
                        R.string.subtitulo_equipo,
                        estado.plantilla?.plantilla?.nombre.orEmpty(),
                        pluralStringResource(R.plurals.asignados, total, asignados, total),
                    ),
                    style = MisTurnosText.cardSubtitle,
                    color = MisTurnosColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BotonModo(editando = estado.editando, onEditar = acciones.empezarEdicion, onListo = acciones.terminarEdicion)
        }
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(estado.trabajadores, key = { it.id }) { trabajador ->
                FilaTrabajadorTurno(
                    trabajador = trabajador,
                    tipo = turnos[trabajador.id],
                    editando = estado.editando,
                    onAbrirSelector = { acciones.abrirSelectorTrabajador(trabajador.id) },
                )
            }
        }
        BotonHoja(stringResource(R.string.cerrar), onCerrar)
    }
}

/** "Editar" (outlined, pencil) in read mode, "Listo" (filled) in edit mode. */
@Composable
private fun BotonModo(editando: Boolean, onEditar: () -> Unit, onListo: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    if (editando) {
        Button(
            onClick = onListo,
            shape = forma,
            colors = ButtonDefaults.buttonColors(containerColor = MisTurnosColors.Accent, contentColor = MisTurnosColors.OnAccent),
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier.height(40.dp),
        ) {
            Text(stringResource(R.string.listo), style = MisTurnosText.inlineButton)
        }
    } else {
        OutlinedButton(
            onClick = onEditar,
            shape = forma,
            border = BorderStroke(1.dp, MisTurnosColors.AccentOnBackground),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MisTurnosColors.AccentOnBackground),
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier.height(40.dp),
        ) {
            Icon(MisTurnosIcons.Lapiz, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.editar), style = MisTurnosText.inlineButton)
        }
    }
}

@Composable
private fun FilaTrabajadorTurno(trabajador: Trabajador, tipo: TipoTurno?, editando: Boolean, onAbrirSelector: () -> Unit) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border(1.dp, MisTurnosColors.LineSoft, forma)
            .padding(start = 10.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(trabajador.nombre)
        Text(
            trabajador.nombre,
            style = MisTurnosText.teamName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (editando) {
            BotonChipTurno(trabajador.nombre, tipo, onAbrirSelector)
        } else {
            ChipTurno(tipo)
        }
    }
}

/** Read-mode chip: shift colours, or "Sin asignar" with a dashed outline. */
@Composable
private fun ChipTurno(tipo: TipoTurno?) {
    val modificador = if (tipo != null) {
        val c = tipo.colores
        Modifier
            .background(c.fondo, CircleShape)
            .border(1.dp, c.borde, CircleShape)
    } else {
        val borde = MisTurnosColors.UnassignedBorder
        Modifier
            .background(MisTurnosColors.Sheet, CircleShape)
            .drawBehind {
                drawRoundRect(
                    color = borde,
                    cornerRadius = CornerRadius(size.height / 2),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
    }
    Text(
        text = tipo?.etiqueta ?: stringResource(R.string.sin_asignar),
        style = MisTurnosText.chipTurno,
        color = tipo?.colores?.texto ?: MisTurnosColors.TextFaint,
        maxLines = 1,
        modifier = modificador.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Edit-mode chip: a 40 dp button with the shift colours and a chevron; opens the picker. */
@Composable
private fun BotonChipTurno(nombre: String, tipo: TipoTurno?, onClick: () -> Unit) {
    val etiqueta = tipo?.etiqueta ?: stringResource(R.string.sin_asignar)
    val descripcion = stringResource(
        R.string.cambiar_turno_de,
        nombre,
        tipo?.etiqueta ?: stringResource(R.string.sin_asignar_descripcion),
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = tipo?.colores?.fondo ?: MisTurnosColors.Sheet,
        contentColor = tipo?.colores?.texto ?: MisTurnosColors.TextMuted,
        border = BorderStroke(2.dp, tipo?.colorPuntoCelda ?: MisTurnosColors.Line),
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 112.dp)
            .semantics { contentDescription = descripcion },
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            Text(etiqueta, style = MisTurnosText.chipTurnoBoton, maxLines = 1)
            Icon(MisTurnosIcons.ChevronDerecha, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

/** "¿Qué equipo quieres ver?" (spec 3b.4): one row per "Trabajadores" template. */
@Composable
fun SelectorPlantillaActiva(
    plantillas: List<PlantillaConRecuento>,
    activaId: Long?,
    onElegir: (Long) -> Unit,
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
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Sobretitulo(stringResource(R.string.plantilla_de_trabajadores))
                Text(stringResource(R.string.que_equipo), style = MisTurnosText.sheetTitle)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plantillas.forEach { item ->
                    val activa = item.plantilla.id == activaId
                    Surface(
                        onClick = { cerrarY { onElegir(item.plantilla.id) } },
                        shape = RoundedCornerShape(14.dp),
                        color = MisTurnosColors.Sheet,
                        contentColor = MisTurnosColors.Text,
                        border = if (activa) {
                            BorderStroke(2.dp, MisTurnosColors.AccentOnBackground)
                        } else {
                            BorderStroke(1.dp, MisTurnosColors.Line)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .semantics {
                                role = Role.RadioButton
                                selected = activa
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                item.plantilla.nombre,
                                style = MisTurnosText.button,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                pluralStringResource(R.plurals.contador_trabajadores, item.numTrabajadores, item.numTrabajadores),
                                style = MisTurnosText.cardSubtitle,
                                color = MisTurnosColors.TextMuted,
                            )
                        }
                    }
                }
            }
            BotonHoja(stringResource(R.string.cancelar)) { cerrarY(onCerrar) }
        }
    }
}

/** 48 dp neutral button at the foot of a sheet ("Cerrar", "Cancelar"). */
@Composable
private fun BotonHoja(texto: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MisTurnosColors.SheetButton,
        contentColor = MisTurnosColors.Text,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(texto, style = MisTurnosText.buttonSmall)
        }
    }
}

@Preview(name = "Equipo del día", widthDp = 390, showBackground = true)
@Composable
private fun EquipoDelDiaPreview() = MuestraEquipo(oscuro = false, editando = false)

@Preview(name = "Equipo del día, editando, oscuro", widthDp = 390, showBackground = true, backgroundColor = 0xFF1F1F1D)
@Composable
private fun EquipoDelDiaEditandoPreview() = MuestraEquipo(oscuro = true, editando = true)

@Composable
private fun MuestraEquipo(oscuro: Boolean, editando: Boolean) {
    MisTurnosTheme(oscuro = oscuro) {
        Box(Modifier.background(MisTurnosColors.Sheet)) {
            EquipoDelDia(
                estado = DatosMuestra.estadoTurnos().copy(diaAbierto = DatosMuestra.HOY, editando = editando),
                acciones = AccionesTurnos.Vacias,
                onCerrar = {},
            )
        }
    }
}
