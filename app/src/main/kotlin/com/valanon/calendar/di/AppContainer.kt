package com.valanon.calendar.di

import android.content.Context
import com.valanon.calendar.data.local.AppDatabase
import com.valanon.calendar.data.repository.EventRepository

/**
 * Manual dependency container. Everything is created lazily, so the database
 * file is only opened the first time a repository is used.
 *
 * If the app grows, this is the place to swap for Hilt/Koin.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val eventRepository: EventRepository by lazy { EventRepository(database.eventDao()) }
}
