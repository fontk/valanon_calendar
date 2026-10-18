package com.valanon.calendar.ui.plantillas

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.testing.FakesPlantillas
import com.valanon.calendar.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlantillasViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fakes = FakesPlantillas()
    private val repositorio = fakes.repositorioPlantillas()

    private fun crear(handle: SavedStateHandle = SavedStateHandle()) = PlantillasViewModel(repositorio, handle)

    @Test
    fun `P1 sin plantillas la lista esta vacia y cargada`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)

        assertFalse(vm.uiState.value.cargando)
        assertTrue(vm.uiState.value.plantillas.isEmpty())
    }

    @Test
    fun `P2 crear nombra Trabajadores N, cierra la hoja y devuelve el id`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)

        vm.abrirHojaTipos()
        assertTrue(vm.uiState.value.hojaTiposAbierta)
        val primera = vm.crear(TipoPlantilla.TRABAJADORES)
        assertFalse(vm.uiState.value.hojaTiposAbierta)
        val segunda = vm.crear(TipoPlantilla.TRABAJADORES)

        assertEquals(listOf("Trabajadores 1", "Trabajadores 2"), vm.uiState.value.plantillas.map { it.plantilla.nombre })
        assertEquals(listOf(primera, segunda), vm.uiState.value.plantillas.map { it.plantilla.id })
    }

    @Test
    fun `P1 la lista lleva el recuento de trabajadores`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.uiState.launchIn(backgroundScope)
        val id = vm.crear(TipoPlantilla.TRABAJADORES)
        repositorio.agregarTrabajador(id, "Lucía")
        repositorio.agregarTrabajador(id, "Javier")

        assertEquals(2, vm.uiState.value.plantillas.single().numTrabajadores)
    }

    @Test
    fun `P2 cancelar no crea nada y la hoja sobrevive a la recreacion`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle()
        crear(handle).abrirHojaTipos()

        val recreado = crear(handle)
        recreado.uiState.launchIn(backgroundScope)
        assertTrue(recreado.uiState.value.hojaTiposAbierta)

        recreado.cerrarHojaTipos()
        assertFalse(recreado.uiState.value.hojaTiposAbierta)
        assertTrue(recreado.uiState.value.plantillas.isEmpty())
    }
}
