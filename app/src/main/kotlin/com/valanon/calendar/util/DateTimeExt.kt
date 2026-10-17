package com.valanon.calendar.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Epoch millis of the first instant of this date in [zone]. */
fun LocalDate.toEpochMillisAtStartOfDay(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

/** The calendar date this epoch-millis instant falls on in [zone]. */
fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
