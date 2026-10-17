package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.entity.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventRepositoryTest {

    private val repository = EventRepository(FakeEventDao())

    private fun event(title: String, startAt: Long) =
        EventEntity(title = title, startAt = startAt, endAt = startAt + 1)

    @Test
    fun `events are emitted ordered by start`() = runTest {
        repository.add(event("later", startAt = 200))
        repository.add(event("earlier", startAt = 100))

        assertEquals(listOf("earlier", "later"), repository.events.first().map { it.title })
    }

    @Test
    fun `eventsBetween filters by start instant`() = runTest {
        repository.add(event("in range", startAt = 150))
        repository.add(event("out of range", startAt = 300))

        val titles = repository.eventsBetween(from = 100, to = 200).first().map { it.title }
        assertEquals(listOf("in range"), titles)
    }

    @Test
    fun `remove deletes the event`() = runTest {
        val id = repository.add(event("to delete", startAt = 1))
        val stored = checkNotNull(repository.getById(id))

        repository.remove(stored)

        assertNull(repository.getById(id))
    }
}
