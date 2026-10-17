package com.valanon.calendar.data.model

import com.valanon.calendar.data.local.Conversores
import org.junit.Assert.assertEquals
import org.junit.Test

class TipoTurnoTest {

    @Test
    fun `los valores coinciden con el CHECK de schema sql`() {
        assertEquals(
            listOf("libre", "manana", "tarde", "noche", "vacaciones"),
            TipoTurno.entries.map { it.valor },
        )
    }

    @Test
    fun `etiquetas y abreviaturas de la especificacion`() {
        assertEquals(listOf("Libre", "Mañana", "Tarde", "Noche", "Vacaciones"), TipoTurno.entries.map { it.etiqueta })
        assertEquals(listOf("LIBRE", "MAÑ", "TARDE", "NOCHE", "VAC"), TipoTurno.entries.map { it.abreviatura })
    }

    @Test
    fun `el conversor de Room ida y vuelta`() {
        val conversores = Conversores()
        TipoTurno.entries.forEach { tipo ->
            assertEquals(tipo, conversores.valorATipoTurno(conversores.tipoTurnoAValor(tipo)))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `un valor desconocido falla`() {
        TipoTurno.desdeValor("mañana")
    }
}
