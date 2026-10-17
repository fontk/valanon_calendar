package com.valanon.calendar.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.valanon.calendar.ui.events.EventsRoute
import com.valanon.calendar.ui.theme.ValanonCalendarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ValanonCalendarTheme {
                EventsRoute()
            }
        }
    }
}
