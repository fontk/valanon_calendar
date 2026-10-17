@file:OptIn(ExperimentalMaterial3Api::class)

package com.valanon.calendar.ui.events

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.EventEntity
import com.valanon.calendar.ui.theme.ValanonCalendarTheme
import com.valanon.calendar.util.toEpochMillisAtStartOfDay
import com.valanon.calendar.util.toLocalDate
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Stateful entry point: wires the ViewModel to the stateless [EventsScreen]. */
@Composable
fun EventsRoute(
    viewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EventsScreen(
        uiState = uiState,
        onAddEvent = viewModel::addEvent,
        onDeleteEvent = viewModel::deleteEvent,
    )
}

@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onAddEvent: (title: String, date: LocalDate) -> Unit,
    onDeleteEvent: (EventEntity) -> Unit,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
            }
        },
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.events.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                items(uiState.events, key = { it.id }) { event ->
                    EventRow(event = event, onDelete = { onDeleteEvent(event) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, date ->
                onAddEvent(title, date)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun EventRow(event: EventEntity, onDelete: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    val date = event.startAt.toLocalDate()
    val subtitle = buildString {
        append(date.format(formatter))
        if (event.allDay) append(" · ").append(stringResource(R.string.all_day))
    }
    ListItem(
        headlineContent = { Text(event.title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_event))
            }
        },
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.no_events),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, date: LocalDate) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var epochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(epochDay)
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_event)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.event_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.event_date))
                    Spacer(Modifier.width(8.dp))
                    Text(date.format(formatter))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onConfirm(title, date) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (showDatePicker) {
        // DatePicker works in UTC: it reports midnight UTC of the selected day.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochMillisAtStartOfDay(ZoneOffset.UTC),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { epochDay = it.toLocalDate(ZoneOffset.UTC).toEpochDay() }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventsScreenPreview() {
    val today = LocalDate.now()
    ValanonCalendarTheme {
        EventsScreen(
            uiState = EventsUiState(
                isLoading = false,
                events = listOf(
                    EventEntity(
                        id = 1,
                        title = "Reunión de equipo",
                        startAt = today.toEpochMillisAtStartOfDay(),
                        endAt = today.plusDays(1).toEpochMillisAtStartOfDay(),
                        allDay = true,
                    ),
                    EventEntity(
                        id = 2,
                        title = "Entrega del proyecto",
                        startAt = today.plusDays(3).toEpochMillisAtStartOfDay(),
                        endAt = today.plusDays(4).toEpochMillisAtStartOfDay(),
                        allDay = true,
                    ),
                ),
            ),
            onAddEvent = { _, _ -> },
            onDeleteEvent = {},
        )
    }
}
