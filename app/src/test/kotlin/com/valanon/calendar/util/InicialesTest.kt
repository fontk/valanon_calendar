package com.valanon.calendar.util

import org.junit.Assert.assertEquals
import org.junit.Test

class InicialesTest {

    @Test
    fun `primera letra del primer y del ultimo nombre`() {
        assertEquals("LM", iniciales("Lucía Martínez"))
        assertEquals("AR", iniciales("Ana Belén Ruiz"))
    }

    @Test
    fun `un solo nombre da una letra`() = assertEquals("S", iniciales("Sara"))

    @Test
    fun `ignora espacios sobrantes y pasa a mayusculas`() = assertEquals("ÁÑ", iniciales("  álvaro   ñúñez "))

    @Test
    fun `vacio da cadena vacia`() = assertEquals("", iniciales("   "))
}
