package com.valanon.calendar.di

import android.content.Context
import com.valanon.calendar.data.backup.CopiaSeguridadRepositorio
import com.valanon.calendar.data.local.AppDatabase
import com.valanon.calendar.data.local.TransaccionesRoom
import com.valanon.calendar.data.repository.AsignacionesRepositorio
import com.valanon.calendar.data.repository.ListasRepositorio
import com.valanon.calendar.data.repository.PlantillasRepositorio
import com.valanon.calendar.data.repository.TareasRepositorio
import com.valanon.calendar.data.repository.TurnosRepositorio
import com.valanon.calendar.pdf.ExportadorPdf

/**
 * Manual dependency container. Everything is created lazily, so the database
 * file is only opened the first time a repository is used.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val turnosRepositorio: TurnosRepositorio by lazy { TurnosRepositorio(database.turnoDao()) }

    val tareasRepositorio: TareasRepositorio by lazy { TareasRepositorio(database.tareaDao()) }

    val listasRepositorio: ListasRepositorio by lazy {
        ListasRepositorio(
            database.listaDao(),
            database.tareaDao(),
            database.plantillaDao(),
            database.ajusteDao(),
            TransaccionesRoom(database),
        )
    }

    val plantillasRepositorio: PlantillasRepositorio by lazy {
        PlantillasRepositorio(
            database.plantillaDao(),
            database.trabajadorDao(),
            database.tareaPlantillaDao(),
            database.asignacionDao(),
            TransaccionesRoom(database),
        )
    }

    val asignacionesRepositorio: AsignacionesRepositorio by lazy {
        AsignacionesRepositorio(database.asignacionDao(), database.ajusteDao(), database.plantillaDao(), database.trabajadorDao())
    }

    val exportadorPdf: ExportadorPdf by lazy { ExportadorPdf(appContext) }

    val copiaSeguridadRepositorio: CopiaSeguridadRepositorio by lazy {
        CopiaSeguridadRepositorio(database.copiaSeguridadDao(), appContext.contentResolver)
    }
}
