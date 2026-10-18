package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.valanon.calendar.data.local.entity.Ajuste
import kotlinx.coroutines.flow.Flow

@Dao
interface AjusteDao {

    /** Emits `null` while the setting has never been saved. */
    @Query("SELECT valor FROM ajustes WHERE clave = :clave")
    fun observar(clave: String): Flow<String?>

    @Query("SELECT valor FROM ajustes WHERE clave = :clave")
    suspend fun obtener(clave: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(ajuste: Ajuste)
}
