package com.valanon.calendar.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText
import com.valanon.calendar.util.iniciales

/**
 * 36 dp circle with the worker's initials (spec 4b.3, 3b.3). Decorative for accessibility:
 * the row's name is what TalkBack reads.
 */
@Composable
fun Avatar(nombre: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .background(MisTurnosColors.Surface, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales(nombre), style = MisTurnosText.avatar, color = MisTurnosColors.Text)
    }
}
