package com.valanon.calendar.util

import java.util.Locale

private val ESPANOL: Locale = Locale.forLanguageTag("es")

/** Longest shift-type name (spec 3c.3). */
const val MAX_NOMBRE_TIPO_TURNO = 20

/** Longest shift-type abbreviation (spec 3c.3). */
const val MAX_ABREVIATURA_TIPO_TURNO = 5

/** Abbreviation suggested while the user has not typed one: the first three letters, upper case ("Guardia" → "GUA"). */
fun abreviaturaDe(nombre: String): String = nombre.trim().take(3).uppercase(ESPANOL)

/** What gets saved: the typed abbreviation (or the derived one if blank), upper case, at most 5 characters. */
fun abreviaturaFinal(abreviatura: String, nombre: String): String =
    abreviatura.trim().ifEmpty { abreviaturaDe(nombre) }.uppercase(ESPANOL).take(MAX_ABREVIATURA_TIPO_TURNO)
