package com.valanon.calendar.ui.componentes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText

/**
 * Confirmation before deleting a template or a Checklist list (spec 4b.3, 4.6): title, body,
 * "Cancelar" as a text button and "Eliminar" filled with `danger` (DESIGN.md).
 */
@Composable
fun DialogoEliminar(
    titulo: String,
    cuerpo: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) = DialogoConfirmar(
    titulo = titulo,
    cuerpo = cuerpo,
    confirmar = stringResource(R.string.eliminar),
    onConfirmar = onConfirmar,
    onCancelar = onCancelar,
    destructivo = true,
)

/**
 * Same dialog for a non-destructive confirmation ("Restablecer"): the action is filled with
 * `accent` instead of `danger`.
 */
@Composable
fun DialogoConfirmar(
    titulo: String,
    cuerpo: String,
    confirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    destructivo: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo, style = MisTurnosText.sheetTitle) },
        text = { Text(cuerpo, style = MisTurnosText.body) },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                shape = RoundedCornerShape(12.dp),
                colors = if (destructivo) {
                    // The dark `danger` is light, so its label uses the (dark) background colour.
                    ButtonDefaults.buttonColors(containerColor = MisTurnosColors.Danger, contentColor = MisTurnosColors.Background)
                } else {
                    ButtonDefaults.buttonColors(containerColor = MisTurnosColors.Accent, contentColor = MisTurnosColors.OnAccent)
                },
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Text(confirmar, style = MisTurnosText.buttonSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.cancelar), style = MisTurnosText.buttonSmall, color = MisTurnosColors.AccentOnBackground)
            }
        },
        containerColor = MisTurnosColors.Sheet,
        titleContentColor = MisTurnosColors.Text,
        textContentColor = MisTurnosColors.TextMuted,
    )
}
