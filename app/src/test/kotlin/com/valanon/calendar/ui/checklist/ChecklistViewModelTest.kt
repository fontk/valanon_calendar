package com.valanon.calendar.ui.checklist

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.repository.TareasRepositorio
import com.valanon.calendar.testing.FakeTareaDao
import com.valanon.calendar.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChecklistViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val dao = FakeTareaDao()

    private fun crear(handle: SavedStateHandle = SavedStateHandle()) =
        ChecklistViewModel(TareasRepositorio(dao), handle)

    private fun ChecklistViewModel.escribirYAgregar(texto: String) {
        actualizarBorrador(texto)
        agregar()
    }

    @Test
    fun `L1 agrega al final, recorta el texto y vacia el campo`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)

        vm.escribirYAgregar("  Primera  ")
        vm.escribirYAgregar("Segunda")

        assertEquals(listOf("Primera", "Segunda"), vm.uiState.value.tareas.map { it.texto })
        assertTrue(vm.uiState.value.tareas.none { it.hecha })
        assertEquals("", vm.borrador)
    }

    @Test
    fun `L1 ignora textos vacios o solo espacios`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)

        vm.escribirYAgregar("")
        vm.escribirYAgregar("    ")

        assertTrue(vm.uiState.value.tareas.isEmpty())
        assertEquals("    ", vm.borrador)
    }

    @Test
    fun `L2 marcar y desmarcar actualiza el progreso`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        vm.escribirYAgregar("A")
        vm.escribirYAgregar("B")
        val a = vm.uiState.value.tareas.first()

        vm.marcar(a, true)
        assertEquals(1, vm.uiState.value.hechas)
        assertEquals(2, vm.uiState.value.total)

        vm.marcar(a, false)
        assertEquals(0, vm.uiState.value.hechas)
    }

    @Test
    fun `L3 borrar y deshacer recupera la tarea en su sitio`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        listOf("A", "B", "C").forEach { vm.escribirYAgregar(it) }
        val b = vm.uiState.value.tareas[1]
        vm.marcar(b, true)
        val bMarcada = vm.uiState.value.tareas[1]

        vm.borrar(bMarcada)
        assertEquals(listOf("A", "C"), vm.uiState.value.tareas.map { it.texto })

        vm.restaurar(bMarcada)
        assertEquals(listOf("A", "B", "C"), vm.uiState.value.tareas.map { it.texto })
        assertTrue(vm.uiState.value.tareas[1].hecha)
    }

    @Test
    fun `L4 reiniciar desmarca todo y no borra nada`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        listOf("A", "B", "C").forEach { vm.escribirYAgregar(it) }
        vm.uiState.value.tareas.forEach { vm.marcar(it, true) }
        assertEquals(3, vm.uiState.value.hechas)

        vm.reiniciarTodas()

        assertEquals(3, vm.uiState.value.total)
        assertEquals(0, vm.uiState.value.hechas)
    }

    @Test
    fun `el borrador sobrevive a la recreacion del ViewModel`() {
        val handle = SavedStateHandle()
        crear(handle).actualizarBorrador("a medias")

        assertEquals("a medias", crear(handle).borrador)
    }
}
