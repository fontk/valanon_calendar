package com.valanon.calendar.ui.componentes

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 1 dp dashed outline (CSS `1px dashed`), which `BorderStroke` cannot draw. [radio] `null` means
 * a pill (half the height). Used by "Sin asignar", the "+ Nueva" chip and "Lista en blanco".
 */
fun Modifier.bordeDiscontinuo(color: Color, radio: Dp? = null): Modifier = drawBehind {
    val r = radio?.toPx() ?: (size.height / 2)
    val grosor = 1.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(grosor / 2, grosor / 2),
        size = androidx.compose.ui.geometry.Size(size.width - grosor, size.height - grosor),
        cornerRadius = CornerRadius(r),
        style = Stroke(width = grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}
