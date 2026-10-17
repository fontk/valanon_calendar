package com.valanon.calendar

import android.app.Application
import com.valanon.calendar.di.AppContainer

/**
 * Application entry point. Holds the [AppContainer] so that every screen can
 * reach the database and repositories without a DI framework.
 */
class CalendarApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
