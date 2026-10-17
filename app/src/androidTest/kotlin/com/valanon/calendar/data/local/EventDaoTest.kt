package com.valanon.calendar.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.data.local.dao.EventDao
import com.valanon.calendar.data.local.entity.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against a real in-memory SQLite database on a device or emulator. */
@RunWith(AndroidJUnit4::class)
class EventDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EventDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.eventDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserve() = runTest {
        val id = dao.insert(EventEntity(title = "Test", startAt = 10, endAt = 20))

        val events = dao.observeAll().first()
        assertEquals(1, events.size)
        assertEquals(id, events.first().id)
        assertEquals("Test", events.first().title)
    }

    @Test
    fun observeBetweenIsHalfOpen() = runTest {
        dao.insert(EventEntity(title = "a", startAt = 100, endAt = 101))
        dao.insert(EventEntity(title = "b", startAt = 200, endAt = 201))

        val titles = dao.observeBetween(from = 100, to = 200).first().map { it.title }
        assertEquals(listOf("a"), titles)
    }

    @Test
    fun deleteRemovesRow() = runTest {
        val id = dao.insert(EventEntity(title = "gone", startAt = 1, endAt = 2))
        val stored = checkNotNull(dao.getById(id))

        dao.delete(stored)

        assertNull(dao.getById(id))
    }
}
