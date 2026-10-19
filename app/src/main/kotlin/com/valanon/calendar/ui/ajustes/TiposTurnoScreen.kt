package com.valanon.calendar.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.MAX_TIPOS_TURNO
import com.valanon.calendar.data.local.dao.TipoTurnoConUsos
import com.valanon.calendar.data.local.dao.UsosTipoTurno
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.ui.componentes.DialogoEliminar
import com.valanon.calendar.ui.componentes.DialogoConfirmar
import com.valanon.calendar.ui.componentes.Sobretitulo
import com.valanon.calendar.ui.componentes.bordeDiscontinuo
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.preview.DatosMuestra
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.ui.theme.MisTurnosTheme
import com.valanon.calendar.ui.theme.PaletaTurnos
import com.valanon.calendar.ui.theme.colores

/** Stateful entry point of "Tipos de turno". [onVolver] pops back to Ajustes. */
@Composable
fun TiposTurnoRoute(
    onVolver: () -> Unit,
    viewModel: TiposTurnoViewModel = viewModel(factory = TiposTurnoViewModel.Factory),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    TiposTurnoScreen(
        estado = estado,
        nombre = viewModel.nombre,
        abreviatura = viewModel.abreviatura,
        acciones = AccionesTiposTurno(
            volver = onVolver,
            nuevo = viewModel::nuevo,
            editar = viewModel::editar,
            cambiarNombre = viewModel::cambiarNombre,
            cambiarAbreviatura = viewModel::cambiarAbreviatura,
            elegirColor = viewModel::elegirColor,
            guardar = viewModel::guardar,
            cerrarEditor = viewModel::cerrarEditor,
            pedirBorrar = viewModel::pedirBorrar,
            cancelarBorrar = viewModel::cancelarBorrar,
            borrar = viewModel::borrar,
            pedirRestablecer = viewModel::pedirRestablecer,
            cancelarRestablecer = viewModel::cancelarRestablecer,
            restablecer = viewModel::restablecer,
        ),
    )
}

/** Callbacks of the screen and its editor sheet, grouped to keep signatures readable. */
data class AccionesTiposTurno(
    val volver: () -> Unit,
    val nuevo: () -> Unit,
    val editar: (TipoTurno) -> Unit,
    val cambiarNombre: (String) -> Unit,
    val cambiarAbreviatura: (String) -> Unit,
    val elegirColor: (String) -> Unit,
    val guardar: () -> Unit,
    val cerrarEditor: () -> Unit,
    val pedirBorrar: (TipoTurno) -> Unit,
    val cancelarBorrar: () -> Unit,
    val borrar: () -> Unit,
    val pedirRestablecer: () -> Unit,
    val cancelarRestablecer: () -> Unit,
    val restablecer: () -> Unit,
) {
    companion object {
        /** For previews. */
        val Vacias = AccionesTiposTurno({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun TiposTurnoScreen(
    estado: TiposTurnoUiState,
    nombre: String,
    abreviatura: String,
    acciones: AccionesTiposTurno,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MisTurnosColors.Background)) {
        BarraTipos(total = estado.total, onVolver = acciones.volver)
        Text(
            stringResource(R.string.tipos_ayuda),
            style = MisTurnosText.emptyHelp,
            color = MisTurnosColors.TextMuted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(estado.tipos, key = { it.tipo.id }) { item ->
                FilaTipoTurno(
                    item = item,
                    puedeBorrar = estado.puedeBorrar,
                    onEditar = { acciones.editar(item.tipo) },
                    onBorrar = { acciones.pedirBorrar(item.tipo) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "anadir") {
                BotonAnadir(habilitado = estado.puedeAnadir, onClick = acciones.nuevo, modifier = Modifier.padding(top = 4.dp))
            }
            item(key = "restablecer") { BotonRestablecer(acciones.pedirRestablecer) }
        }
    }

    estado.editor?.let { editor ->
        EditorTipoTurnoSheet(
            editor = editor,
            nombre = nombre,
            abreviatura = abreviatura,
            acciones = acciones,
        )
    }
    estado.confirmandoBorrado?.let { item ->
        DialogoEliminar(
            titulo = stringResource(R.string.eliminar_tipo_titulo, item.tipo.nombre),
            cuerpo = cuerpoEliminar(item.usos),
            onConfirmar = acciones.borrar,
            onCancelar = acciones.cancelarBorrar,
        )
    }
    if (estado.confirmandoRestablecer) {
        DialogoConfirmar(
            titulo = stringResource(R.string.restablecer_titulo),
            cuerpo = stringResource(R.string.restablecer_cuerpo),
            confirmar = stringResource(R.string.restablecer),
            onConfirmar = acciones.restablecer,
            onCancelar = acciones.cancelarRestablecer,
        )
    }
}

/** "Está marcado en 6 días de tu calendario y en 18 turnos del equipo…" (spec 3c.4). */
@Composable
private fun cuerpoEliminar(usos: UsosTipoTurno): String {
    val dias = pluralStringResource(R.plurals.dias_uso, usos.dias, usos.dias)
    val equipo = pluralStringResource(R.plurals.turnos_equipo_uso, usos.equipo, usos.equipo)
    return when {
        usos.dias > 0 && usos.equipo > 0 -> stringResource(R.string.eliminar_tipo_cuerpo_ambos, dias, equipo)
        usos.dias > 0 -> stringResource(R.string.eliminar_tipo_cuerpo_dias, dias)
        usos.equipo > 0 -> stringResource(R.string.eliminar_tipo_cuerpo_equipo, equipo)
        else -> stringResource(R.string.eliminar_tipo_sin_uso)
    }
}

@Composable
private fun BarraTipos(total: Int, onVolver: () -> Unit) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 14.dp, end = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onVolver, modifier = Modifier.size(44.dp)) {
            Icon(
                MisTurnosIcons.FlechaAtras,
                contentDescription = stringResource(R.string.volver_a_ajustes),
                tint = MisTurnosColors.Text,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Sobretitulo(stringResource(R.string.sobretitulo_tipos))
            Text(
                stringResource(R.string.tipos_de_turno),
                style = MisTurnosText.detailTitle,
                color = MisTurnosColors.Text,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            stringResource(R.string.contador_tipos, total, MAX_TIPOS_TURNO),
            style = MisTurnosText.caption,
            color = MisTurnosColors.TextMuted,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

/** 64 dp row: 44 dp swatch with the abbreviation, name, "Por defecto · Amarillo · 9 días…", edit and delete. */
@Composable
private fun FilaTipoTurno(
    item: TipoTurnoConUsos,
    puedeBorrar: Boolean,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tipo = item.tipo
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .border(1.dp, MisTurnosColors.LineSoft, forma)
            .padding(start = 10.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MuestraTipo(tipo)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tipo.nombre, style = MisTurnosText.button, color = MisTurnosColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detalleTipo(item), style = MisTurnosText.listOrigin, color = MisTurnosColors.TextMuted)
        }
        IconButton(onClick = onEditar, modifier = Modifier.size(44.dp)) {
            Icon(
                MisTurnosIcons.Lapiz,
                contentDescription = stringResource(R.string.editar_tipo, tipo.nombre),
                tint = MisTurnosColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onBorrar, enabled = puedeBorrar, modifier = Modifier.size(44.dp)) {
            Icon(
                MisTurnosIcons.Papelera,
                contentDescription = stringResource(R.string.eliminar_tipo, tipo.nombre),
                tint = MisTurnosColors.Danger,
                modifier = Modifier
                    .size(20.dp)
                    .alpha(if (puedeBorrar) 1f else 0.4f),
            )
        }
    }
}

/** "Por defecto · Amarillo · 9 días, 22 turnos del equipo" or "Personalizado · Naranja · sin usar". */
@Composable
private fun detalleTipo(item: TipoTurnoConUsos): String {
    val (dias, equipo) = item.usos
    return stringResource(
        R.string.detalle_tipo,
        stringResource(if (item.tipo.porDefecto) R.string.por_defecto else R.string.personalizado),
        stringResource(PaletaTurnos[item.tipo.color].nombre),
        if (item.usos.enUso) {
            stringResource(
                R.string.usos_tipo,
                pluralStringResource(R.plurals.dias_uso, dias, dias),
                pluralStringResource(R.plurals.turnos_equipo_uso, equipo, equipo),
            )
        } else {
            stringResource(R.string.sin_usar)
        },
    )
}

/** 44 dp swatch (radius 12) in the type's colours with its abbreviation. Decorative: the row's name is read. */
@Composable
private fun MuestraTipo(tipo: TipoTurno) {
    val c = tipo.colores
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(c.fondo, forma)
            .border(1.dp, c.borde, forma)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(tipo.abreviatura, style = MisTurnosText.abreviaturaMuestra, color = c.texto, maxLines = 1)
    }
}

/** 52 dp "+ Añadir tipo de turno" with a dashed accent outline; at 16 types it is disabled (opacity 0.4). */
@Composable
private fun BotonAnadir(habilitado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .alpha(if (habilitado) 1f else 0.4f)
            .clip(forma)
            .bordeDiscontinuo(MisTurnosColors.AccentOnBackground, radio = 14.dp)
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(MisTurnosIcons.Mas, contentDescription = null, tint = MisTurnosColors.AccentOnBackground, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.anadir_tipo), style = MisTurnosText.addButton, color = MisTurnosColors.AccentOnBackground)
    }
}

/** 48 dp text button "Restablecer los cinco por defecto" with a circular-arrow icon. */
@Composable
private fun BotonRestablecer(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(MisTurnosIcons.Reiniciar, contentDescription = null, tint = MisTurnosColors.TextMuted, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.restablecer_defecto), style = MisTurnosText.inlineButton.copy(fontWeight = MisTurnosText.button.fontWeight), color = MisTurnosColors.TextMuted)
    }
}

@Preview(name = "Tipos de turno", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TiposPreview() = MuestraTipos(oscuro = false)

@Preview(name = "Tipos de turno oscuro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TiposOscuroPreview() = MuestraTipos(oscuro = true)

@Composable
private fun MuestraTipos(oscuro: Boolean) {
    val usos = listOf(8 to 6, 9 to 22, 8 to 20, 6 to 18, 0 to 6, 0 to 0)
    MisTurnosTheme(oscuro = oscuro) {
        TiposTurnoScreen(
            estado = TiposTurnoUiState(
                tipos = DatosMuestra.tipos().zip(usos) { t, (d, e) -> TipoTurnoConUsos(t, UsosTipoTurno(d, e)) },
                cargando = false,
            ),
            nombre = "",
            abreviatura = "",
            acciones = AccionesTiposTurno.Vacias,
        )
    }
}
