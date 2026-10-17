package com.valanon.calendar.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.ui.theme.colores
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** C5: the PDF is a valid one-page A4 document whose cells use the app's shift colours. */
@RunWith(AndroidJUnit4::class)
class ExportadorPdfTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mes = YearMonth.of(2026, 10)

    @Test
    fun generaUnA4DeUnaPaginaConElNombreEsperado() = runTest {
        val archivo = ExportadorPdf(context).exportar(mes, emptyMap())

        assertEquals(File(File(context.cacheDir, "pdf"), "turnos-2026-10.pdf"), archivo)
        assertEquals("%PDF", archivo.inputStream().use { String(it.readNBytes(4)) })
        renderizar(archivo) { renderer ->
            assertEquals(1, renderer.pageCount)
            renderer.openPage(0).use { pagina ->
                assertEquals(595, pagina.width)
                assertEquals(842, pagina.height)
            }
        }
    }

    @Test
    fun lasCeldasUsanLosColoresDeTurno() = runTest {
        // 1 Oct 2026 is a Thursday → column 3 of the first row; 5 Oct is Monday → column 0 of row 1.
        val turnos = mapOf(
            LocalDate.of(2026, 10, 1) to TipoTurno.NOCHE,
            LocalDate.of(2026, 10, 5) to TipoTurno.MANANA,
        )
        val archivo = ExportadorPdf(context).exportar(mes, turnos)

        renderizar(archivo) { renderer ->
            renderer.openPage(0).use { pagina ->
                val bitmap = Bitmap.createBitmap(pagina.width, pagina.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                // Same geometry as ExportadorPdf: margin 40, gap 6, grid top 142, cell height 86.
                val anchoCelda = (595 - 80 - 36) / 7f
                fun muestra(columna: Int, fila: Int) = bitmap.getPixel(
                    (40 + columna * (anchoCelda + 6) + 8).toInt(),
                    (142 + fila * (86 + 6) + 8),
                )
                assertEquals(TipoTurno.NOCHE.colores.fondo.toArgb(), muestra(3, 0))
                assertEquals(TipoTurno.MANANA.colores.fondo.toArgb(), muestra(0, 1))
                assertEquals(Color.WHITE, muestra(1, 1)) // 6 Oct, no shift
            }
        }
    }

    private fun renderizar(archivo: File, bloque: (PdfRenderer) -> Unit) {
        ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use(bloque)
        }
    }
}
