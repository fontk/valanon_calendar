package com.valanon.calendar.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class DateTimeExtTest {

    private val madrid: ZoneId = ZoneId.of("Europe/Madrid")

    @Test
    fun `round trip keeps the same date in the same zone`() {
        val dates = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 29), // DST change in Europe
            LocalDate.of(2026, 10, 25), // DST change in Europe
            LocalDate.of(2026, 12, 31),
        )
        for (date in dates) {
            val millis = date.toEpochMillisAtStartOfDay(madrid)
            assertEquals(date, millis.toLocalDate(madrid))
        }
    }

    @Test
    fun `start of day in UTC matches epoch day arithmetic`() {
        val date = LocalDate.of(2026, 10, 17)
        val expected = date.toEpochDay() * 24 * 60 * 60 * 1000
        assertEquals(expected, date.toEpochMillisAtStartOfDay(ZoneOffset.UTC))
    }
}
