package com.valanon.calendar.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Stroke icons from the mockups (24×24 viewBox, round caps and joins). The SVG path data is
 * copied verbatim from mis-turnos-kit/design/mockups. Paint them with `Icon(tint = …)`.
 */
object MisTurnosIcons {

    val Calendario: ImageVector by lazy {
        icono("Calendario", 2f, rect(3f, 5f, 18f, 16f, 2f), "M3 10h18M8 3v4M16 3v4")
    }

    val Checklist: ImageVector by lazy {
        icono("Checklist", 2f, "M9 11l2 2 4-4", rect(3f, 4f, 18f, 16f, 2f))
    }

    /** Thinner variant used in the checklist empty state. */
    val ChecklistVacio: ImageVector by lazy {
        icono("ChecklistVacio", 1.6f, "M9 11l2 2 4-4", rect(3f, 4f, 18f, 16f, 2f))
    }

    val ChevronIzquierda: ImageVector by lazy { icono("ChevronIzquierda", 2.2f, "M15 6l-6 6 6 6") }

    val ChevronDerecha: ImageVector by lazy { icono("ChevronDerecha", 2.2f, "M9 6l6 6-6 6") }

    /** Month/year picker pill (DESIGN.md → Iconos). */
    val ChevronAbajo: ImageVector by lazy { icono("ChevronAbajo", 2.4f, "M6 9l6 6 6-6") }

    val DocumentoDescarga: ImageVector by lazy {
        icono(
            "DocumentoDescarga",
            2f,
            "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z",
            "M14 3v5h5",
            "M12 11v6",
            "M9 14l3 3 3-3",
        )
    }

    val Mas: ImageVector by lazy { icono("Mas", 2.4f, "M12 5v14M5 12h14") }

    val Papelera: ImageVector by lazy {
        icono("Papelera", 2f, "M3 6h18", "M8 6V4h8v2", "M19 6l-1 14H6L5 6", "M10 11v6M14 11v6")
    }

    val Reiniciar: ImageVector by lazy {
        icono(
            "Reiniciar",
            2.2f,
            "M3 12a9 9 0 0 1 15.5-6.3L21 8",
            "M21 3v5h-5",
            "M21 12a9 9 0 0 1-15.5 6.3L3 16",
            "M3 21v-5h5",
        )
    }

    /** Clock: Turnos tab (DESIGN.md → Iconos). */
    val Turnos: ImageVector by lazy { icono("Turnos", 2f, circulo(12f, 12f, 9f), "M12 7v5l3 2") }

    /** Archive box: backup (export/import). Same stroke style as the design's icons. */
    val Copia: ImageVector by lazy {
        icono("Copia", 2f, rect(2f, 3f, 20f, 5f, 1f), "M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8", "M10 12h4")
    }

    // --- Plantillas (DESIGN.md → Iconos; paths from design/mockups/Plantillas.dc.html) ---

    private val capas = arrayOf("M12 3l9 5-9 5-9-5 9-5z", "M3 13l9 5 9-5", "M3 17l9 5 9-5")

    /** Layers: Plantillas tab. */
    val Plantillas: ImageVector by lazy { icono("Plantillas", 2f, *capas) }

    /** Thinner variant used in the templates empty state. */
    val PlantillasVacio: ImageVector by lazy { icono("PlantillasVacio", 1.6f, *capas) }

    /** Two people: "Trabajadores" template type. */
    val Trabajadores: ImageVector by lazy {
        icono(
            "Trabajadores",
            2f,
            circulo(9f, 8f, 3.5f),
            "M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6",
            circulo(17f, 9f, 2.5f),
            "M16 14.5c3 0 5.5 2 5.5 5",
        )
    }

    val Lapiz: ImageVector by lazy { icono("Lapiz", 2f, "M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z") }

    val FlechaAtras: ImageVector by lazy { icono("FlechaAtras", 2.2f, "M19 12H5", "M11 6l-6 6 6 6") }

    val Cerrar: ImageVector by lazy { icono("Cerrar", 2.2f, "M6 6l12 12M18 6L6 18") }

    /** Bin without the inner lines, used by the "Eliminar plantilla" button. */
    val PapeleraSimple: ImageVector by lazy { icono("PapeleraSimple", 2f, "M3 6h18", "M8 6V4h8v2", "M19 6l-1 14H6L5 6") }

    private fun icono(nombre: String, trazo: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = nombre,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = trazo,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    /** SVG `<circle cx cy r>` as path data (two half arcs). */
    private fun circulo(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 -${2 * r} 0z"

    /** SVG `<rect x y width height rx>` as path data. */
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val dx = w - 2 * r
        val dy = h - 2 * r
        return "M${x + r} ${y}h${dx}a$r $r 0 0 1 $r ${r}v${dy}a$r $r 0 0 1 -$r ${r}h-${dx}" +
            "a$r $r 0 0 1 -$r -${r}v-${dy}a$r $r 0 0 1 $r -${r}z"
    }
}
