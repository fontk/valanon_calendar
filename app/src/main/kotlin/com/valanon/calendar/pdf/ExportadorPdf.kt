package com.valanon.calendar.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.valanon.calendar.R
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.colores
import com.valanon.calendar.util.CalendarioMes
import com.valanon.calendar.util.FormatoFechas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * Renders one month as an A4 portrait PDF with [PdfDocument] (no third-party libraries):
 * title, coloured 7-column grid using the app's shift colours, and the legend below.
 * Files go to `cacheDir/pdf/turnos-AAAA-MM.pdf`, which `res/xml/file_paths.xml` exposes to FileProvider.
 */
class ExportadorPdf(private val context: Context) : GeneradorPdf {

    private val manrope: Typeface by lazy {
        runCatching { ResourcesCompat.getFont(context, R.font.manrope) }.getOrNull() ?: Typeface.SANS_SERIF
    }

    override suspend fun exportar(mes: YearMonth, turnos: Map<LocalDate, TipoTurno>): File =
        withContext(Dispatchers.IO) {
            val directorio = File(context.cacheDir, DIRECTORIO).apply { mkdirs() }
            val archivo = File(directorio, nombreArchivo(mes))
            val documento = PdfDocument()
            try {
                val pagina = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO_A4, ALTO_A4, 1).create())
                dibujarMes(pagina.canvas, mes, turnos)
                documento.finishPage(pagina)
                archivo.outputStream().use { documento.writeTo(it) }
            } finally {
                documento.close()
            }
            archivo
        }

    private fun dibujarMes(canvas: Canvas, mes: YearMonth, turnos: Map<LocalDate, TipoTurno>) {
        val anchoUtil = ANCHO_A4 - 2 * MARGEN

        // Overline + title
        canvas.drawText("MIS TURNOS", MARGEN, 64f, texto(9f, 700, MisTurnosColors.TextMuted, espaciado = 0.08f))
        canvas.drawText("Mis turnos — ${FormatoFechas.tituloMes(mes)}", MARGEN, 92f, texto(22f, 800, MisTurnosColors.Text))

        // Weekday header
        val anchoCelda = (anchoUtil - 6 * HUECO) / 7
        val cabecera = texto(10f, 700, MisTurnosColors.TextFaint, centrado = true)
        FormatoFechas.INICIALES_SEMANA.forEachIndexed { i, inicial ->
            canvas.drawText(inicial, MARGEN + i * (anchoCelda + HUECO) + anchoCelda / 2, 130f, cabecera)
        }

        // Grid
        val semanas = CalendarioMes.semanas(mes)
        val inicioRejilla = 142f
        val numero = texto(16f, 800, MisTurnosColors.Text, centrado = true)
        val etiqueta = texto(8.5f, 700, MisTurnosColors.Text, centrado = true, espaciado = 0.04f)
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f }

        semanas.forEachIndexed { fila, semana ->
            semana.forEachIndexed { columna, fecha ->
                if (fecha == null) return@forEachIndexed
                val x = MARGEN + columna * (anchoCelda + HUECO)
                val y = inicioRejilla + fila * (ALTO_CELDA + HUECO)
                val rect = RectF(x, y, x + anchoCelda, y + ALTO_CELDA)
                val tipo = turnos[fecha]
                val colores = tipo?.colores

                fondo.color = (colores?.fondo ?: Color.White).toArgb()
                borde.color = (colores?.borde ?: MisTurnosColors.LineSoft).toArgb()
                canvas.drawRoundRect(rect, RADIO_CELDA, RADIO_CELDA, fondo)
                canvas.drawRoundRect(rect, RADIO_CELDA, RADIO_CELDA, borde)

                val colorTexto = (colores?.texto ?: MisTurnosColors.Text).toArgb()
                numero.color = colorTexto
                val centroY = rect.centerY()
                if (tipo == null) {
                    canvas.drawText(fecha.dayOfMonth.toString(), rect.centerX(), centroY + 6f, numero)
                } else {
                    etiqueta.color = colorTexto
                    canvas.drawText(fecha.dayOfMonth.toString(), rect.centerX(), centroY, numero)
                    canvas.drawText(tipo.abreviatura, rect.centerX(), centroY + 15f, etiqueta)
                }
            }
        }

        // Legend under the grid
        val finRejilla = inicioRejilla + semanas.size * ALTO_CELDA + (semanas.size - 1) * HUECO
        dibujarLeyenda(canvas, finRejilla + 28f)
    }

    private fun dibujarLeyenda(canvas: Canvas, top: Float) {
        val textoChip = texto(10f, 700, MisTurnosColors.Text)
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG)
        val punto = Paint(Paint.ANTI_ALIAS_FLAG)
        val alto = 22f
        var x = MARGEN
        TipoTurno.entries.forEach { tipo ->
            val c = tipo.colores
            val ancho = 10f + 7f + 6f + textoChip.measureText(tipo.etiqueta) + 10f
            fondo.color = c.fondo.toArgb()
            canvas.drawRoundRect(RectF(x, top, x + ancho, top + alto), alto / 2, alto / 2, fondo)
            punto.color = c.punto.toArgb()
            canvas.drawCircle(x + 10f + 3.5f, top + alto / 2, 3.5f, punto)
            textoChip.color = c.texto.toArgb()
            canvas.drawText(tipo.etiqueta, x + 10f + 7f + 6f, top + alto / 2 + 3.5f, textoChip)
            x += ancho + 6f
        }
    }

    private fun texto(
        tamano: Float,
        peso: Int,
        color: Color,
        centrado: Boolean = false,
        espaciado: Float = 0f,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = manrope
        fontVariationSettings = "'wght' $peso"
        isFakeBoldText = manrope == Typeface.SANS_SERIF && peso >= 700
        textSize = tamano
        this.color = color.toArgb()
        letterSpacing = espaciado
        if (centrado) textAlign = Paint.Align.CENTER
    }

    companion object {
        const val DIRECTORIO = "pdf"

        /** A4 in PostScript points (1/72 inch). */
        private const val ANCHO_A4 = 595
        private const val ALTO_A4 = 842
        private const val MARGEN = 40f
        private const val HUECO = 6f
        private const val ALTO_CELDA = 86f
        private const val RADIO_CELDA = 10f

        /** `turnos-2026-10.pdf` */
        fun nombreArchivo(mes: YearMonth): String = "turnos-%04d-%02d.pdf".format(mes.year, mes.monthValue)
    }
}
