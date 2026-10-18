package com.valanon.calendar.data.local

import androidx.room.RoomDatabase
import androidx.room.withTransaction

/**
 * Runs several DAO calls as one atomic unit: all of them are applied or none (a cancellation
 * or error in the middle rolls everything back). Repositories depend on this interface so
 * JVM tests can run the block directly with [SinTransaccion].
 */
interface Transacciones {
    suspend fun <R> ejecutar(bloque: suspend () -> R): R
}

/** Real implementation: a Room transaction. */
class TransaccionesRoom(private val database: RoomDatabase) : Transacciones {
    override suspend fun <R> ejecutar(bloque: suspend () -> R): R = database.withTransaction(bloque)
}

/** For fakes/tests without a database: runs the block as is. */
object SinTransaccion : Transacciones {
    override suspend fun <R> ejecutar(bloque: suspend () -> R): R = bloque()
}
