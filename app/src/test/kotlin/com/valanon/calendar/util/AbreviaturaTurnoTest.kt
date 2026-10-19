package com.valanon.calendar.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AbreviaturaTurnoTest {

    @Test
    fun `deriva las tres primeras letras en mayusculas`() {
        assertEquals("GUA", abreviaturaDe("Guardia"))
        assertEquals("MAÑ", abreviaturaDe("  mañana "))
        assertEquals("NO", abreviaturaDe("No"))
        assertEquals("", abreviaturaDe("   "))
    }

    @Test
    fun `la final usa la escrita o deriva si esta vacia, en mayusculas y con 5 como maximo`() {
        assertEquals("NOC", abreviaturaFinal("noc", "Nocturno"))
        assertEquals("GUA", abreviaturaFinal("  ", "Guardia"))
        assertEquals("REFUE", abreviaturaFinal("refuerzo", "Refuerzo"))
    }
}
