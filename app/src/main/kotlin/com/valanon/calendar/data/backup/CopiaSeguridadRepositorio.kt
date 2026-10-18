package com.valanon.calendar.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.valanon.calendar.data.local.dao.CopiaSeguridadDao
import com.valanon.calendar.data.model.DatosApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import java.time.Instant

/**
 * Export/import of all the user's data to a file the user picks (Storage Access Framework),
 * independent from Android's own backup. Used to keep data safe across reinstalls, phone
 * changes and the one-time switch to the release signing key.
 */
class CopiaSeguridadRepositorio(
    private val dao: CopiaSeguridadDao,
    private val contentResolver: ContentResolver,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Writes every table to [destino]. Returns what was written. */
    suspend fun exportar(destino: Uri): DatosApp {
        val datos = dao.leerTodo()
        val json = FormatoCopia.aJson(datos, Instant.now(clock))
        withContext(ioDispatcher) {
            val salida = contentResolver.openOutputStream(destino, "wt") ?: throw IOException("No se pudo abrir el archivo")
            salida.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        }
        return datos
    }

    /**
     * Reads and validates [origen], then replaces ALL current data with it in one transaction.
     * Throws [CopiaInvalidaException] (nothing is changed) if the file is not a valid backup.
     */
    suspend fun importar(origen: Uri): DatosApp {
        val texto = withContext(ioDispatcher) {
            val entrada = contentResolver.openInputStream(origen) ?: throw IOException("No se pudo abrir el archivo")
            entrada.use { flujo ->
                val bytes = flujo.readNBytesCompat(TAMANO_MAXIMO + 1)
                if (bytes.size > TAMANO_MAXIMO) throw CopiaInvalidaException("El archivo es demasiado grande para ser una copia")
                String(bytes, Charsets.UTF_8)
            }
        }
        val datos = FormatoCopia.desdeJson(texto)
        dao.reemplazarTodo(datos)
        return datos
    }

    private fun java.io.InputStream.readNBytesCompat(limite: Int): ByteArray {
        // InputStream.readNBytes(int) is API 33+; minSdk is 26.
        val salida = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (salida.size() < limite) {
            val leidos = read(buffer, 0, minOf(buffer.size, limite - salida.size()))
            if (leidos < 0) break
            salida.write(buffer, 0, leidos)
        }
        return salida.toByteArray()
    }

    companion object {
        /** A year of shifts for a big team is a few hundred KB; anything above is not ours. */
        const val TAMANO_MAXIMO = 10 * 1024 * 1024
    }
}
