package com.valanon.calendar.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText

/**
 * Building blocks shared by the list tabs (Checklist, Plantillas), so both follow DESIGN.md
 * with the same measures.
 */

/** "MIS TURNOS" + 22 sp title, with an optional counter aligned on the baseline. */
@Composable
fun CabeceraPestana(titulo: String, resumen: String?) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Sobretitulo(stringResource(R.string.sobretitulo))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = titulo,
                style = MisTurnosText.title,
                color = MisTurnosColors.Text,
                modifier = Modifier
                    .alignByBaseline()
                    .semantics { heading() },
            )
            if (resumen != null) {
                Text(
                    text = resumen,
                    style = MisTurnosText.caption,
                    color = MisTurnosColors.TextMuted,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

/** 50 dp input + "Agregar" button. The button and IME Done both call [onAgregar]. */
@Composable
fun CampoAlta(
    valor: String,
    onValorCambia: (String) -> Unit,
    onAgregar: () -> Unit,
    placeholder: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    val forma = RoundedCornerShape(14.dp)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = valor,
            onValueChange = onValorCambia,
            singleLine = true,
            textStyle = MisTurnosText.body.copy(color = MisTurnosColors.Text),
            cursorBrush = SolidColor(MisTurnosColors.AccentOnBackground),
            keyboardOptions = KeyboardOptions(capitalization = capitalizacion, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAgregar() }),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp)
                .semantics { contentDescription = descripcion },
            decorationBox = { campo ->
                Box(
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .clip(forma)
                        .background(MisTurnosColors.SurfaceInput)
                        .border(1.dp, MisTurnosColors.Line, forma)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (valor.isEmpty()) {
                        Text(placeholder, style = MisTurnosText.body, color = MisTurnosColors.TextFaint, maxLines = 1)
                    }
                    campo()
                }
            },
        )
        Button(
            onClick = onAgregar,
            shape = forma,
            colors = ButtonDefaults.buttonColors(
                containerColor = MisTurnosColors.Accent,
                contentColor = MisTurnosColors.OnAccent,
            ),
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier.heightIn(min = 50.dp),
        ) {
            Icon(MisTurnosIcons.Mas, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.agregar), style = MisTurnosText.addButton)
        }
    }
}

/** Centered empty state: optional grey icon, title and help text. */
@Composable
fun EstadoVacio(
    titulo: String,
    ayuda: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    anchoAyuda: Dp = 220.dp,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = MisTurnosColors.EmptyIcon, modifier = Modifier.size(44.dp))
        }
        Text(titulo, style = MisTurnosText.emptyTitle, color = MisTurnosColors.TextFaint)
        Text(
            ayuda,
            style = MisTurnosText.emptyHelp,
            color = MisTurnosColors.TextFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = anchoAyuda),
        )
    }
}

/**
 * Centered empty state of a whole tab (Checklist without lists, Turnos without templates): 64 dp
 * badge (radius 20, `surface`) with a 32 dp icon, 17 sp title, 14 sp help and an action [boton].
 * The caller gives it the free height (e.g. `Modifier.weight(1f)`) so it sits in the middle.
 */
@Composable
fun EstadoVacioInsignia(
    icono: ImageVector,
    titulo: String,
    ayuda: String,
    modifier: Modifier = Modifier,
    boton: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(start = 40.dp, end = 40.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MisTurnosColors.Surface, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = MisTurnosColors.EmptyBadgeIcon, modifier = Modifier.size(32.dp))
        }
        Text(
            titulo,
            style = MisTurnosText.emptyStateTitle,
            color = MisTurnosColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(ayuda, style = MisTurnosText.emptyStateHelp, color = MisTurnosColors.TextFaint, textAlign = TextAlign.Center)
        Box(Modifier.padding(top = 8.dp)) { boton() }
    }
}

/** 60 dp accent FAB (radius 18), placed by the caller 20 dp from the edges. */
@Composable
fun FabAccion(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tamanoIcono: Dp = 26.dp,
) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        containerColor = MisTurnosColors.Accent,
        contentColor = MisTurnosColors.OnAccent,
        modifier = modifier.size(60.dp),
    ) {
        Icon(icono, contentDescription = descripcion, modifier = Modifier.size(tamanoIcono))
    }
}
