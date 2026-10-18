package com.valanon.calendar.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText

/**
 * 40 dp single-line field that takes focus when it appears, for renaming in place (template
 * name, template item, Checklist list name). Enter/Done confirms; Escape (hardware keyboard)
 * cancels when [onCancelar] is given.
 */
@Composable
fun CampoEnLinea(
    valor: String,
    onValorCambia: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: (() -> Unit)?,
    descripcion: String,
    capitalizacion: KeyboardCapitalization,
    estilo: TextStyle,
    fondo: Color,
    borde: BorderStroke?,
    modifier: Modifier = Modifier,
) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    val forma = RoundedCornerShape(12.dp)
    BasicTextField(
        value = valor,
        onValueChange = onValorCambia,
        singleLine = true,
        textStyle = estilo.copy(color = MisTurnosColors.Text),
        cursorBrush = SolidColor(MisTurnosColors.AccentOnBackground),
        keyboardOptions = KeyboardOptions(capitalization = capitalizacion, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onConfirmar() }),
        modifier = modifier
            .heightIn(min = 40.dp)
            .focusRequester(foco)
            .onPreviewKeyEvent { evento ->
                if (onCancelar != null && evento.type == KeyEventType.KeyUp && evento.key == Key.Escape) {
                    onCancelar()
                    true
                } else {
                    false
                }
            }
            .semantics { contentDescription = descripcion },
        decorationBox = { campo ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp) // grows with large system fonts instead of clipping
                    .clip(forma)
                    .background(fondo)
                    .then(if (borde != null) Modifier.border(borde, forma) else Modifier)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) { campo() }
        },
    )
}

/** 40 dp accent "Guardar" button next to a [CampoEnLinea]. */
@Composable
fun BotonGuardar(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MisTurnosColors.Accent, contentColor = MisTurnosColors.OnAccent),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Text(stringResource(R.string.guardar), style = MisTurnosText.inlineButton)
    }
}
