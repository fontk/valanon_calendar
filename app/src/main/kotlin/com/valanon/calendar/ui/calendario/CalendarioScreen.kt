package com.valanon.calendar.ui.calendario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.pdf.compartirPdf
import com.valanon.calendar.ui.componentes.AccionesSelectorMes
import com.valanon.calendar.ui.copia.BotonCopiaSeguridad
import com.valanon.calendar.ui.copia.CopiaSeguridadHost
import com.valanon.calendar.ui.copia.CopiaSeguridadViewModel
import com.valanon.calendar.ui.componentes.SelectorMes
import com.valanon.calendar.ui.componentes.CabeceraDiasSemana
import com.valanon.calendar.ui.componentes.CabeceraMes
import com.valanon.calendar.ui.componentes.LeyendaTurnos
import com.valanon.calendar.ui.componentes.RejillaMes
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import com.valanon.calendar.ui.theme.colores
import com.valanon.calendar.util.FormatoFechas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** Stateful entry point: wires [CalendarioViewModel], the PDF share sheet and the snackbar. */
@Composable
fun CalendarioRoute(viewModel: CalendarioViewModel = viewModel(factory = CalendarioViewModel.Factory)) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    // "Today" can change while the app sits in the background (midnight).
    LifecycleResumeEffect(viewModel) {
        viewModel.refrescarHoy()
        onPauseOrDispose { }
    }
    val context = LocalContext.current
    val recursos = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val copia: CopiaSeguridadViewModel = viewModel(factory = CopiaSeguridadViewModel.Factory)
    CopiaSeguridadHost(snackbarHostState = snackbar, viewModel = copia)
    var exportando by remember { mutableStateOf(false) }

    CalendarioScreen(
        estado = estado,
        exportando = exportando,
        snackbarHostState = snackbar,
        onMesAnterior = viewModel::mesAnterior,
        onMesSiguiente = viewModel::mesSiguiente,
        onDiaPulsado = viewModel::seleccionarDia,
        onElegirTurno = viewModel::asignarTurno,
        onCerrarSelector = viewModel::cerrarSelector,
        accionCabecera = { BotonCopiaSeguridad(onClick = copia::abrirHoja) },
        selectorMes = AccionesSelectorMes(
            abrir = viewModel::abrirSelectorMes,
            cerrar = viewModel::cerrarSelectorMes,
            anioAnterior = viewModel::anioSelectorAnterior,
            anioSiguiente = viewModel::anioSelectorSiguiente,
            irAMes = viewModel::irAMes,
            irAHoy = viewModel::irAHoy,
        ),
        onExportar = {
            scope.launch {
                exportando = true
                val archivo: File? = try {
                    viewModel.exportarPdf()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                } finally {
                    exportando = false
                }
                if (archivo == null) {
                    snackbar.showSnackbar(recursos.getString(R.string.pdf_error))
                } else {
                    compartirPdf(context, archivo, recursos.getString(R.string.compartir_pdf))
                    snackbar.showSnackbar(recursos.getString(R.string.pdf_exportado))
                }
            }
        },
    )
}

@Composable
fun CalendarioScreen(
    estado: CalendarioUiState,
    exportando: Boolean,
    snackbarHostState: SnackbarHostState,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onDiaPulsado: (LocalDate) -> Unit,
    onElegirTurno: (LocalDate, TipoTurno) -> Unit,
    onCerrarSelector: () -> Unit,
    onExportar: () -> Unit,
    selectorMes: AccionesSelectorMes,
    accionCabecera: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(MisTurnosColors.Background)) {
        // Short screens (landscape): the export button scrolls with the grid instead of
        // staying pinned, so the grid is not squeezed into a sliver.
        val compacta = maxHeight < 480.dp
        val scroll = rememberScrollState()
        val botonExportar = @Composable {
            BotonExportar(
                habilitado = !exportando,
                onClick = onExportar,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
            )
        }
        Column(Modifier.fillMaxSize().then(if (compacta) Modifier.verticalScroll(scroll) else Modifier)) {
            // Scrolls only when it does not fit (130 % font size, small phones).
            Column(if (compacta) Modifier else Modifier.weight(1f).verticalScroll(scroll)) {
                CabeceraMes(
                    sobretitulo = stringResource(R.string.sobretitulo),
                    mes = estado.mes,
                    onMesAnterior = onMesAnterior,
                    onMesSiguiente = onMesSiguiente,
                    onAbrirSelectorMes = selectorMes.abrir,
                    accion = accionCabecera,
                )
                LeyendaTurnos(estado.tipos, Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp))
                CabeceraDiasSemana(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
                RejillaMes(mes = estado.mes, modifier = Modifier.padding(horizontal = 16.dp)) { fecha, celda ->
                    CeldaDia(
                        fecha = fecha,
                        tipo = estado.turnos[fecha],
                        esHoy = fecha == estado.hoy,
                        onClick = { onDiaPulsado(fecha) },
                        modifier = celda,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            botonExportar()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 74.dp),
        )
    }

    estado.diaSeleccionado?.let { dia ->
        SelectorTurno(
            fecha = dia,
            tipos = estado.tipos,
            actual = estado.turnos[dia],
            onElegir = { tipo -> onElegirTurno(dia, tipo) },
            onCerrar = onCerrarSelector,
        )
    }

    if (estado.selectorMesAbierto) {
        SelectorMes(
            anio = estado.anioSelector,
            mesActivo = estado.mes,
            hoy = estado.hoy,
            onAnioAnterior = selectorMes.anioAnterior,
            onAnioSiguiente = selectorMes.anioSiguiente,
            onElegirMes = selectorMes.irAMes,
            onHoy = selectorMes.irAHoy,
            onCerrar = selectorMes.cerrar,
        )
    }
}

@Composable
fun CeldaDia(
    fecha: LocalDate,
    tipo: TipoTurno?,
    esHoy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = tipo?.colores
    val forma = RoundedCornerShape(12.dp)
    val borde = when {
        esHoy -> BorderStroke(2.dp, MisTurnosColors.AccentOnBackground)
        colores != null -> BorderStroke(1.dp, colores.borde)
        else -> BorderStroke(1.dp, MisTurnosColors.LineSoft)
    }
    val colorTexto = colores?.texto ?: MisTurnosColors.Text
    val descripcion = "${FormatoFechas.diaYMes(fecha)}, ${tipo?.nombre ?: stringResource(R.string.sin_turno)}"

    Column(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(forma)
            .background(colores?.fondo ?: MisTurnosColors.Background)
            .border(borde, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = fecha.dayOfMonth.toString(),
            style = MisTurnosText.dayNumber,
            color = colorTexto,
            modifier = Modifier.clearAndSetSemantics { },
        )
        if (tipo != null) {
            // Shrinks on narrow phones or large system font instead of clipping "NOCHE"/"TARDE".
            BasicText(
                text = tipo.abreviatura,
                style = MisTurnosText.dayTag.copy(color = colorTexto),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = MisTurnosText.dayTag.fontSize),
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun BotonExportar(habilitado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MisTurnosColors.Accent,
            contentColor = MisTurnosColors.OnAccent,
            disabledContainerColor = MisTurnosColors.Accent.copy(alpha = 0.6f),
            disabledContentColor = MisTurnosColors.OnAccent,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Icon(MisTurnosIcons.DocumentoDescarga, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.exportar_pdf), style = MisTurnosText.button)
    }
}

@Preview(name = "Calendario", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun CalendarioScreenPreview() = CalendarioMuestra(oscuro = false)

@Preview(name = "Calendario oscuro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun CalendarioScreenOscuroPreview() = CalendarioMuestra(oscuro = true)

@Composable
private fun CalendarioMuestra(oscuro: Boolean) {
    MisTurnosTheme(oscuro = oscuro) {
        CalendarioScreen(
            estado = CalendarioUiState(
                mes = DatosMuestra.MES,
                hoy = DatosMuestra.HOY,
                turnos = DatosMuestra.turnosOctubre(),
                tipos = DatosMuestra.tipos(),
            ),
            exportando = false,
            snackbarHostState = remember { SnackbarHostState() },
            onMesAnterior = {},
            onMesSiguiente = {},
            onDiaPulsado = {},
            onElegirTurno = { _, _ -> },
            onCerrarSelector = {},
            selectorMes = AccionesSelectorMes.Vacias,
            onExportar = {},
        )
    }
}
