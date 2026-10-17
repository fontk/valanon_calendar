package com.valanon.calendar.util

/**
 * Avatar initials (spec 4b.3): first letter of the first and of the last word, uppercase.
 * "Ana Belén Ruiz" → "AR", "Lucía" → "L", blank → "".
 */
fun iniciales(nombre: String): String {
    val partes = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (partes.isEmpty()) return ""
    val primera = partes.first().first()
    val ultima = if (partes.size > 1) partes.last().first().toString() else ""
    return (primera + ultima).uppercase()
}
