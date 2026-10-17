package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.dao.EventDao
import com.valanon.calendar.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [EventDao] for JVM unit tests (no SQLite needed). */
class FakeEventDao : EventDao {

    private val events = MutableStateFlow<List<EventEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<EventEntity>> =
        events.map { list -> list.sortedWith(compareBy({ it.startAt }, { it.id })) }

    override fun observeBetween(from: Long, to: Long): Flow<List<EventEntity>> =
        observeAll().map { list -> list.filter { it.startAt >= from && it.startAt < to } }

    override suspend fun getById(id: Long): EventEntity? = events.value.firstOrNull { it.id == id }

    override suspend fun insert(event: EventEntity): Long {
        val id = nextId++
        events.update { it + event.copy(id = id) }
        return id
    }

    override suspend fun update(event: EventEntity) {
        events.update { list -> list.map { if (it.id == event.id) event else it } }
    }

    override suspend fun delete(event: EventEntity) {
        events.update { list -> list.filterNot { it.id == event.id } }
    }

    override suspend fun deleteAll() {
        events.value = emptyList()
    }
}
