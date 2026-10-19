package com.valanon.calendar.ui.turnos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.ui.componentes.AccionesSelectorMes
import com.valanon.calendar.ui.componentes.CabeceraDiasSemana
import com.valanon.calendar.ui.componentes.CabeceraMes
import com.valanon.calendar.ui.componentes.EstadoVacioInsignia
import com.valanon.calendar.ui.componentes.LeyendaTurnos
import com.valanon.calendar.ui.componentes.RejillaMes
import com.valanon.calendar.ui.componentes.SelectorMes
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import com.valanon.calendar.ui.theme.colores
import com.valanon.calendar.util.FormatoFechas
import com.valanon.calendar.util.PuntosDia
import java.time.LocalDate

/** Stateful entry point of the Turnos tab. [onIrAPlantillas] switches to the Plantillas tab. */
@Composable
fun TurnosRoute(
    onIrAPlantillas: () -> Unit,
    viewModel: TurnosViewModel = viewModel(factory = TurnosViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // "Today" can change while the app sits in the background (midnight).
    LifecycleResumeEffect(viewModel) {
        viewModel.refrescarHoy()
        onPauseOrDispose { }
    }
    TurnosScreen(
        estado = estado,
        acciones = AccionesTurnos(
            abrirDia = viewModel::abrirDia,
            cerrarDia = viewModel::cerrarDia,
            empezarEdicion = viewModel::empezarEdicion,
            terminarEdicion = viewModel::terminarEdicion,
            abrirSelectorTrabajador = viewModel::abrirSelectorTrabajador,
            volverAEquipo = viewModel::volverAEquipo,
            asignar = viewModel::asignar,
            quitarTurno = viewModel::quitarTurno,
            abrirHojaPlantillas = viewModel::abrirHojaPlantillas,
            cerrarHojaPlantillas = viewModel::cerrarHojaPlantillas,
            elegirPlantilla = viewModel::elegirPlantilla,
            irAPlantillas = onIrAPlantillas,
            mesAnterior = viewModel::mesAnterior,
            mesSiguiente = viewModel::mesSiguiente,
            selectorMes = AccionesSelectorMes(
                abrir = viewModel::abrirSelectorMes,
                cerrar = viewModel::cerrarSelectorMes,
                anioAnterior = viewModel::anioSelectorAnterior,
                anioSiguiente = viewModel::anioSelectorSiguiente,
                irAMes = viewModel::irAMes,
                irAHoy = viewModel::irAHoy,
            ),
        ),
    )
}

/** Callbacks of the Turnos tab, grouped to keep the screen signature readable. */
data class AccionesTurnos(
    val abrirDia: (LocalDate) -> Unit,
    val cerrarDia: () -> Unit,
    val empezarEdicion: () -> Unit,
    val terminarEdicion: () -> Unit,
    val abrirSelectorTrabajador: (Long) -> Unit,
    val volverAEquipo: () -> Unit,
    val asignar: (TipoTurno) -> Unit,
    val quitarTurno: () -> Unit,
    val abrirHojaPlantillas: () -> Unit,
    val cerrarHojaPlantillas: () -> Unit,
    val elegirPlantilla: (Long) -> Unit,
    val irAPlantillas: () -> Unit,
    val mesAnterior: () -> Unit,
    val mesSiguiente: () -> Unit,
    val selectorMes: AccionesSelectorMes,
) {
    companion object {
        /** For previews. */
        val Vacias = AccionesTurnos(
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, AccionesSelectorMes.Vacias,
        )
    }
}

@Composable
fun TurnosScreen(estado: TurnosUiState, acciones: AccionesTurnos, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(MisTurnosColors.Background)) {
        // Without templates the empty state takes the free height to sit in the middle (spec 3b.1).
        val desplazable = if (estado.sinPlantillas) Modifier else Modifier.verticalScroll(rememberScrollState())
        Column(Modifier.fillMaxSize().then(desplazable)) {
            CabeceraMes(
                sobretitulo = stringResource(R.string.turnos_sobretitulo),
                mes = estado.mes,
                onMesAnterior = acciones.mesAnterior,
                onMesSiguiente = acciones.mesSiguiente,
                onAbrirSelectorMes = acciones.selectorMes.abrir,
            )
            val plantilla = estado.plantilla
            when {
                estado.sinPlantillas -> EstadoSinPlantillas(acciones.irAPlantillas, Modifier.weight(1f).fillMaxWidth())
                plantilla != null -> {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // The box takes the free width: chip on the left, summary pushed right.
                        Box(Modifier.weight(1f)) {
                            ChipPlantillaActiva(
                                nombre = plantilla.plantilla.nombre,
                                onClick = acciones.abrirHojaPlantillas,
                            )
                        }
                        val dias = estado.mes.lengthOfMonth()
                        // Measured first (no weight), so the template name gets the rest of the row.
                        Text(
                            pluralStringResource(R.plurals.dias_con_turnos, dias, estado.diasConTurnos, dias),
                            style = MisTurnosText.resumen,
                            color = MisTurnosColors.TextMuted,
                            maxLines = 1,
                        )
                    }
                    CabeceraDiasSemana(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
                    RejillaMes(mes = estado.mes, modifier = Modifier.padding(horizontal = 16.dp)) { fecha, celda ->
                        CeldaDiaPuntos(
                            fecha = fecha,
                            puntos = estado.puntosDe(fecha),
                            totalTrabajadores = estado.trabajadores.size,
                            esHoy = fecha == estado.hoy,
                            onClick = { acciones.abrirDia(fecha) },
                            modifier = celda,
                        )
                    }
                    LeyendaTurnos(
                        tipos = estado.tipos,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
                        paddingVertical = 4.dp,
                    )
                }
            }
        }
    }

    if (estado.diaAbierto != null && estado.plantilla != null) {
        EquipoDelDiaSheet(estado = estado, acciones = acciones)
    }
    if (estado.hojaPlantillasAbierta) {
        SelectorPlantillaActiva(
            plantillas = estado.plantillas,
            activaId = estado.plantilla?.plantilla?.id,
            onElegir = acciones.elegirPlantilla,
            onCerrar = acciones.cerrarHojaPlantillas,
        )
    }
    if (estado.selectorMesAbierto) {
        SelectorMes(
            anio = estado.anioSelector,
            mesActivo = estado.mes,
            hoy = estado.hoy,
            onAnioAnterior = acciones.selectorMes.anioAnterior,
            onAnioSiguiente = acciones.selectorMes.anioSiguiente,
            onElegirMes = acciones.selectorMes.irAMes,
            onHoy = acciones.selectorMes.irAHoy,
            onCerrar = acciones.selectorMes.cerrar,
        )
    }
}

/** 36 dp pill with the active template (spec 3b.1); opens "¿Qué equipo quieres ver?". */
@Composable
private fun ChipPlantillaActiva(nombre: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val descripcion = stringResource(R.string.elegir_plantilla_trabajadores)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MisTurnosColors.Background,
        contentColor = MisTurnosColors.Text,
        border = BorderStroke(1.dp, MisTurnosColors.Line),
        modifier = modifier
            .heightIn(min = 36.dp)
            .widthIn(max = 240.dp)
            .semantics { contentDescription = descripcion },
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(MisTurnosColors.SurfaceAccent, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MisTurnosIcons.Trabajadores, null, tint = MisTurnosColors.AccentOnBackground, modifier = Modifier.size(15.dp))
            }
            Text(
                nombre,
                style = MisTurnosText.chipPlantilla,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(MisTurnosIcons.ChevronAbajo, null, tint = MisTurnosColors.TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Day cell of the team grid (spec 3b.2): day number and one 7 dp dot per assigned worker in
 * template order, at most 5 plus "+N". On very narrow cells the gap shrinks and fewer dots are
 * drawn so nothing is clipped; "+N" always counts the hidden ones.
 */
@Composable
fun CeldaDiaPuntos(
    fecha: LocalDate,
    puntos: List<TipoTurno>,
    totalTrabajadores: Int,
    esHoy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(12.dp)
    val descripcion = stringResource(
        R.string.descripcion_celda_equipo,
        FormatoFechas.diaYMes(fecha),
        pluralStringResource(R.plurals.asignados, totalTrabajadores, puntos.size, totalTrabajadores),
    )
    Column(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(forma)
            .background(if (puntos.isNotEmpty()) MisTurnosColors.DayWithData else MisTurnosColors.Background)
            .border(
                if (esHoy) BorderStroke(2.dp, MisTurnosColors.AccentOnBackground) else BorderStroke(1.dp, MisTurnosColors.LineSoft),
                forma,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion }
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(
            fecha.dayOfMonth.toString(),
            style = MisTurnosText.dayNumber,
            color = MisTurnosColors.Text,
            modifier = Modifier.clearAndSetSemantics { },
        )
        FilaPuntos(puntos, Modifier.clearAndSetSemantics { })
    }
}

@Composable
private fun FilaPuntos(puntos: List<TipoTurno>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().height(8.dp), contentAlignment = Alignment.Center) {
        val disposicion = PuntosDia.calcular(puntos.size, maxWidth.value)
        val ocultos = puntos.size - disposicion.visibles
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(disposicion.huecoDp.dp),
        ) {
            puntos.take(disposicion.visibles).forEach { tipo ->
                Box(
                    Modifier
                        .size(7.dp)
                        .background(tipo.colores.punto, CircleShape),
                )
            }
            if (ocultos > 0) {
                Text("+$ocultos", style = MisTurnosText.dotsExtra, color = MisTurnosColors.TextMuted, maxLines = 1)
            }
        }
    }
}

/** No "Trabajadores" template yet (spec 3b.1). */
@Composable
private fun EstadoSinPlantillas(onIrAPlantillas: () -> Unit, modifier: Modifier = Modifier) {
    // Spec 3b.1: replaces the chip row, grid and legend; the month header stays.
    EstadoVacioInsignia(
        icono = MisTurnosIcons.Trabajadores,
        titulo = stringResource(R.string.turnos_vacio_titulo),
        ayuda = stringResource(R.string.turnos_vacio_ayuda),
        modifier = modifier,
    ) {
        Surface(
            onClick = onIrAPlantillas,
            shape = RoundedCornerShape(14.dp),
            color = MisTurnosColors.Background,
            contentColor = MisTurnosColors.AccentOnBackground,
            border = BorderStroke(1.dp, MisTurnosColors.AccentOnBackground),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.ir_a_plantillas), style = MisTurnosText.addButton)
                Icon(MisTurnosIcons.ChevronDerecha, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Preview(name = "Turnos", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun TurnosPreview() = MuestraTurnos(oscuro = false, vacia = false)

@Preview(name = "Turnos oscuro", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun TurnosOscuroPreview() = MuestraTurnos(oscuro = true, vacia = false)

@Preview(name = "Turnos sin plantillas", widthDp = 390, heightDp = 772, showBackground = true)
@Composable
private fun TurnosVaciaPreview() = MuestraTurnos(oscuro = false, vacia = true)

@Composable
private fun MuestraTurnos(oscuro: Boolean, vacia: Boolean) {
    MisTurnosTheme(oscuro = oscuro) {
        TurnosScreen(
            estado = if (vacia) {
                TurnosUiState(mes = DatosMuestra.MES, hoy = DatosMuestra.HOY, cargando = false)
            } else {
                DatosMuestra.estadoTurnos()
            },
            acciones = AccionesTurnos.Vacias,
        )
    }
}
