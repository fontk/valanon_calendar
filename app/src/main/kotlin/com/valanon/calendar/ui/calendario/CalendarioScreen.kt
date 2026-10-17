package com.valanon.calendar.ui.calendario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.pdf.compartirPdf
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import com.valanon.calendar.ui.theme.colores
import com.valanon.calendar.util.CalendarioMes
import com.valanon.calendar.util.FormatoFechas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** Stateful entry point: wires [CalendarioViewModel], the PDF share sheet and the snackbar. */
@Composable
fun CalendarioRoute(viewModel: CalendarioViewModel = viewModel(factory = CalendarioViewModel.Factory)) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val recursos = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
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
                CabeceraMes(estado.mes, onMesAnterior, onMesSiguiente, selectorMes.abrir)
                LeyendaTurnos(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp))
                CabeceraDiasSemana(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
                RejillaMes(
                    mes = estado.mes,
                    hoy = estado.hoy,
                    turnos = estado.turnos,
                    onDiaPulsado = onDiaPulsado,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
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

/** Callbacks of the month/year picker, grouped to keep [CalendarioScreen]'s signature readable. */
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

@Composable
private fun CabeceraMes(
    mes: YearMonth,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onAbrirSelectorMes: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Sobretitulo(stringResource(R.string.sobretitulo))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeyendaTurnos(modifier: Modifier = Modifier) {
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
                    .padding(horizontal = 10.dp, vertical = 5.dp),
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

@Composable
private fun CabeceraDiasSemana(modifier: Modifier = Modifier) {
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

@Composable
private fun RejillaMes(
    mes: YearMonth,
    hoy: LocalDate,
    turnos: Map<LocalDate, TipoTurno>,
    onDiaPulsado: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
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
                    val celda = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    if (fecha == null) {
                        Spacer(celda.heightIn(min = 60.dp))
                    } else {
                        CeldaDia(
                            fecha = fecha,
                            tipo = turnos[fecha],
                            esHoy = fecha == hoy,
                            onClick = { onDiaPulsado(fecha) },
                            modifier = celda,
                        )
                    }
                }
            }
        }
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
    val descripcion = "${FormatoFechas.diaYMes(fecha)}, ${tipo?.etiqueta ?: stringResource(R.string.sin_turno)}"

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
