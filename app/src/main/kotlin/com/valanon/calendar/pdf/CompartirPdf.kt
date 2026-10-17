package com.valanon.calendar.pdf

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Opens the system share sheet (`ACTION_SEND`, `application/pdf`) for a file under cacheDir/pdf. */
fun compartirPdf(context: Context, archivo: File, tituloSelector: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, archivo.name)
        // ClipData lets the chooser preview the file and carries the read grant to the target.
        clipData = ClipData.newRawUri(archivo.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(envio, tituloSelector))
}
