package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.dao.EventDao
import com.valanon.calendar.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for event data. Screens and ViewModels talk to this class,
 * never to the DAO directly, so the storage can change without touching the UI.
 */
class EventRepository(private val eventDao: EventDao) {

    val events: Flow<List<EventEntity>> = eventDao.observeAll()

    fun eventsBetween(from: Long, to: Long): Flow<List<EventEntity>> =
        eventDao.observeBetween(from, to)

    suspend fun getById(id: Long): EventEntity? = eventDao.getById(id)

    suspend fun add(event: EventEntity): Long = eventDao.insert(event)

    suspend fun update(event: EventEntity) = eventDao.update(event)

    suspend fun remove(event: EventEntity) = eventDao.delete(event)
}
