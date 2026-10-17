package com.valanon.calendar.ui.plantillas

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.repository.PlantillasRepositorio
import com.valanon.calendar.testing.FakePlantillaDao
import com.valanon.calendar.testing.FakeTrabajadorDao
import com.valanon.calendar.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlantillaTrabajadoresViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val trabajadoresDao = FakeTrabajadorDao()
    private val plantillasDao = FakePlantillaDao(trabajadoresDao)
    private val repositorio = PlantillasRepositorio(plantillasDao, trabajadoresDao)

    /** Creates a template (optionally with workers) and a ViewModel on it, subscribed to its state. */
    private suspend fun TestScope.detalle(vararg nombres: String, renombrar: Boolean = false): PlantillaTrabajadoresViewModel {
        val id = repositorio.crear(TipoPlantilla.TRABAJADORES)
        nombres.forEach { repositorio.agregarTrabajador(id, it) }
        val handle = SavedStateHandle(
            mapOf(PlantillaTrabajadoresViewModel.ARG_ID to id, PlantillaTrabajadoresViewModel.ARG_RENOMBRAR to renombrar),
        )
        return PlantillaTrabajadoresViewModel(repositorio, handle).also { it.uiState.launchIn(backgroundScope) }
    }

    private fun PlantillaTrabajadoresViewModel.nombres() = uiState.value.trabajadores.map { it.nombre }

    @Test
    fun `P5 agrega al final, recorta, vacia el campo e ignora vacios`() = runTest(mainRule.dispatcher) {
        val vm = detalle()

        vm.actualizarBorrador("  Lucía Martínez ")
        vm.agregar()
        vm.actualizarBorrador("Javier Ortega")
        vm.agregar()
        vm.actualizarBorrador("   ")
        vm.agregar()

        assertEquals(listOf("Lucía Martínez", "Javier Ortega"), vm.nombres())
        assertEquals("   ", vm.borrador)
    }

    @Test
    fun `P6 solo una fila en edicion y cambiar de fila descarta la primera`() = runTest(mainRule.dispatcher) {
        val vm = detalle("Lucía", "Javier")
        val (lucia, javier) = vm.uiState.value.trabajadores

        vm.empezarEdicion(lucia)
        vm.actualizarBorradorEdicion("Cambio sin guardar")
        vm.empezarEdicion(javier)

        assertEquals(javier.id, vm.uiState.value.editandoId)
        assertEquals("Javier", vm.borradorEdicion)
        assertEquals(listOf("Lucía", "Javier"), vm.nombres())
    }

    @Test
    fun `P6 guardar cambia el nombre y guardar vacio conserva el anterior`() = runTest(mainRule.dispatcher) {
        val vm = detalle("Marcos")
        val marcos = vm.uiState.value.trabajadores.single()

        vm.empezarEdicion(marcos)
        vm.actualizarBorradorEdicion(" Marcos Pérez ")
        vm.guardarEdicion()
        assertEquals(listOf("Marcos Pérez"), vm.nombres())
        assertNull(vm.uiState.value.editandoId)

        vm.empezarEdicion(vm.uiState.value.trabajadores.single())
        vm.actualizarBorradorEdicion("  ")
        vm.guardarEdicion()
        assertEquals(listOf("Marcos Pérez"), vm.nombres())
    }

    @Test
    fun `P6 cancelar descarta los cambios`() = runTest(mainRule.dispatcher) {
        val vm = detalle("Sara")
        vm.empezarEdicion(vm.uiState.value.trabajadores.single())
        vm.actualizarBorradorEdicion("Otro")

        vm.cancelarEdicion()

        assertEquals(listOf("Sara"), vm.nombres())
        assertNull(vm.uiState.value.editandoId)
    }

    @Test
    fun `P7 eliminar y deshacer restaura el orden`() = runTest(mainRule.dispatcher) {
        val vm = detalle("A", "B", "C")
        val b = vm.uiState.value.trabajadores[1]

        vm.eliminar(b)
        assertEquals(listOf("A", "C"), vm.nombres())

        vm.deshacerEliminar(b)
        assertEquals(listOf("A", "B", "C"), vm.nombres())
    }

    @Test
    fun `P4 renombrar con texto y con vacio`() = runTest(mainRule.dispatcher) {
        val vm = detalle()

        vm.empezarRenombrar()
        assertTrue(vm.uiState.value.renombrando)
        assertEquals("Trabajadores 1", vm.borradorNombre)
        vm.actualizarBorradorNombre("Equipo de planta")
        vm.guardarNombre()
        assertFalse(vm.uiState.value.renombrando)
        assertEquals("Equipo de planta", vm.uiState.value.plantilla?.nombre)

        vm.empezarRenombrar()
        vm.actualizarBorradorNombre("")
        vm.guardarNombre()
        assertEquals("Equipo de planta", vm.uiState.value.plantilla?.nombre)
    }

    @Test
    fun `P2 una plantilla recien creada abre con el nombre en edicion y relleno`() = runTest(mainRule.dispatcher) {
        val vm = detalle(renombrar = true)

        assertTrue(vm.uiState.value.renombrando)
        assertEquals("Trabajadores 1", vm.borradorNombre)
    }

    @Test
    fun `P8 cancelar el dialogo no borra y confirmar cierra el detalle`() = runTest(mainRule.dispatcher) {
        val vm = detalle("Lucía", "Javier")

        vm.pedirBorrarPlantilla()
        assertTrue(vm.uiState.value.confirmandoBorrado)
        vm.cancelarBorrarPlantilla()
        assertFalse(vm.uiState.value.confirmandoBorrado)
        assertFalse(vm.uiState.value.cerrada)

        vm.pedirBorrarPlantilla()
        vm.borrarPlantilla()

        assertTrue(vm.uiState.value.cerrada)
        assertTrue(trabajadoresDao.filas.value.isEmpty()) // cascade
    }

    @Test
    fun `la edicion abierta sobrevive a la recreacion`() = runTest(mainRule.dispatcher) {
        val id = repositorio.crear(TipoPlantilla.TRABAJADORES)
        repositorio.agregarTrabajador(id, "Diego")
        val handle = SavedStateHandle(mapOf(PlantillaTrabajadoresViewModel.ARG_ID to id))
        val vm = PlantillaTrabajadoresViewModel(repositorio, handle)
        vm.uiState.launchIn(backgroundScope)
        vm.empezarEdicion(vm.uiState.value.trabajadores.single())
        vm.actualizarBorradorEdicion("Diego Ferrer")

        val recreado = PlantillaTrabajadoresViewModel(repositorio, handle)
        recreado.uiState.launchIn(backgroundScope)

        assertEquals(vm.uiState.value.trabajadores.single().id, recreado.uiState.value.editandoId)
        assertEquals("Diego Ferrer", recreado.borradorEdicion)
    }
}
