package com.valanon.calendar.ui.componentes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText

/** Small uppercase label above titles ("MIS TURNOS", "TURNO DEL DÍA"). */
@Composable
fun Sobretitulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MisTurnosText.overline,
        color = MisTurnosColors.TextMuted,
        modifier = modifier,
    )
}
