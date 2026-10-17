package com.valanon.calendar.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.data.local.entity.EventEntity
import com.valanon.calendar.data.repository.EventRepository
import com.valanon.calendar.util.toEpochMillisAtStartOfDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EventsUiState(
    val events: List<EventEntity> = emptyList(),
    val isLoading: Boolean = true,
)

class EventsViewModel(private val repository: EventRepository) : ViewModel() {

    val uiState: StateFlow<EventsUiState> = repository.events
        .map { EventsUiState(events = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = EventsUiState(),
        )

    /** Creates an all-day event on [date]. */
    fun addEvent(title: String, date: LocalDate) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.add(
                EventEntity(
                    title = trimmed,
                    startAt = date.toEpochMillisAtStartOfDay(),
                    endAt = date.plusDays(1).toEpochMillisAtStartOfDay(),
                    allDay = true,
                ),
            )
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch { repository.remove(event) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalendarApp
                EventsViewModel(app.container.eventRepository)
            }
        }
    }
}
