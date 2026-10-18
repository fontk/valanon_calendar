package com.valanon.calendar.ui.plantillas

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.TipoTurno
import com.valanon.calendar.data.repository.AsignacionesRepositorio
import java.time.LocalDate
import com.valanon.calendar.testing.FakesPlantillas
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

class PlantillaDetalleViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fakes = FakesPlantillas()
    private val trabajadoresDao = fakes.trabajadores
    private val repositorio = fakes.repositorioPlantillas()

    /** Creates a template (optionally with items) and a ViewModel on it, subscribed to its state. */
    private suspend fun TestScope.detalle(
        vararg nombres: String,
        renombrar: Boolean = false,
        tipo: TipoPlantilla = TipoPlantilla.TRABAJADORES,
    ): PlantillaDetalleViewModel {
        val id = repositorio.crear(tipo)
        nombres.forEach { repositorio.agregarElemento(tipo, id, it) }
        val handle = SavedStateHandle(
            mapOf(PlantillaDetalleViewModel.ARG_ID to id, PlantillaDetalleViewModel.ARG_RENOMBRAR to renombrar),
        )
        return PlantillaDetalleViewModel(repositorio, handle).also { it.uiState.launchIn(backgroundScope) }
    }

    private fun PlantillaDetalleViewModel.nombres() = uiState.value.elementos.map { it.texto }

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
        val (lucia, javier) = vm.uiState.value.elementos

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
        val marcos = vm.uiState.value.elementos.single()

        vm.empezarEdicion(marcos)
        vm.actualizarBorradorEdicion(" Marcos Pérez ")
        vm.guardarEdicion()
        assertEquals(listOf("Marcos Pérez"), vm.nombres())
        assertNull(vm.uiState.value.editandoId)

        vm.empezarEdicion(vm.uiState.value.elementos.single())
        vm.actualizarBorradorEdicion("  ")
        vm.guardarEdicion()
        assertEquals(listOf("Marcos Pérez"), vm.nombres())
    }

    @Test
    fun `P6 cancelar descarta los cambios`() = runTest(mainRule.dispatcher) {
        val vm = detalle("Sara")
        vm.empezarEdicion(vm.uiState.value.elementos.single())
        vm.actualizarBorradorEdicion("Otro")

        vm.cancelarEdicion()

        assertEquals(listOf("Sara"), vm.nombres())
        assertNull(vm.uiState.value.editandoId)
    }

    @Test
    fun `P7 eliminar y deshacer restaura el orden`() = runTest(mainRule.dispatcher) {
        val vm = detalle("A", "B", "C")
        val b = vm.uiState.value.elementos[1]

        vm.eliminar(b)
        assertEquals(listOf("A", "C"), vm.nombres())

        vm.deshacerEliminar(b)
        assertEquals(listOf("A", "B", "C"), vm.nombres())
    }

    @Test
    fun `T6 eliminar un trabajador con turnos y deshacer restaura tambien sus turnos`() = runTest(mainRule.dispatcher) {
        val vm = detalle("Lucía", "Javier")
        val lucia = vm.uiState.value.elementos.first()
        val asignaciones = AsignacionesRepositorio(fakes.asignaciones, fakes.ajustes, fakes.plantillas, fakes.trabajadores)
        asignaciones.asignar(LocalDate.of(2026, 10, 17), lucia.id, TipoTurno.NOCHE)
        asignaciones.asignar(LocalDate.of(2026, 10, 18), lucia.id, TipoTurno.LIBRE)
        assertEquals(2, vm.uiState.value.numeroAsignaciones)

        vm.eliminar(lucia)
        assertEquals(0, vm.uiState.value.numeroAsignaciones)

        vm.deshacerEliminar(lucia)
        assertEquals(listOf("Lucía", "Javier"), vm.nombres())
        assertEquals(2, vm.uiState.value.numeroAsignaciones)
        assertEquals(
            setOf("2026-10-17" to TipoTurno.NOCHE, "2026-10-18" to TipoTurno.LIBRE),
            fakes.asignaciones.filas.value.map { it.fecha to it.tipo }.toSet(),
        )
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

    // --- Tipo Tareas (spec 4b.3): same screen, items in tareas_plantilla ---

    @Test
    fun `L1 tareas del preset se agregan, editan y quedan en su tabla`() = runTest(mainRule.dispatcher) {
        val vm = detalle(tipo = TipoPlantilla.TAREAS)

        vm.actualizarBorrador(" Revisar el cuadrante del día ")
        vm.agregar()
        vm.actualizarBorrador("Cuadrar la caja")
        vm.agregar()
        vm.empezarEdicion(vm.uiState.value.elementos[1])
        vm.actualizarBorradorEdicion("Cuadrar la caja y el cambio")
        vm.guardarEdicion()

        assertEquals(listOf("Revisar el cuadrante del día", "Cuadrar la caja y el cambio"), vm.nombres())
        assertEquals(listOf(1, 2), vm.uiState.value.elementos.map { it.orden })
        assertEquals(2, fakes.tareasPlantilla.filas.value.size)
        assertTrue(trabajadoresDao.filas.value.isEmpty())
    }

    @Test
    fun `L1 eliminar una tarea del preset y deshacer la devuelve a su sitio`() = runTest(mainRule.dispatcher) {
        val vm = detalle("A", "B", "C", tipo = TipoPlantilla.TAREAS)
        val b = vm.uiState.value.elementos[1]

        vm.eliminar(b)
        assertEquals(listOf("A", "C"), vm.nombres())

        vm.deshacerEliminar(b)
        assertEquals(listOf("A", "B", "C"), vm.nombres())
        assertEquals(b.id, vm.uiState.value.elementos[1].id)
    }

    @Test
    fun `P2 una plantilla de tareas nueva se llama Tareas N`() = runTest(mainRule.dispatcher) {
        repositorio.crear(TipoPlantilla.TRABAJADORES)
        val vm = detalle(renombrar = true, tipo = TipoPlantilla.TAREAS)

        assertEquals(TipoPlantilla.TAREAS, vm.uiState.value.plantilla?.tipo)
        assertEquals("Tareas 2", vm.borradorNombre)
    }

    @Test
    fun `L8 borrar la plantilla de tareas no toca las listas creadas desde ella`() = runTest(mainRule.dispatcher) {
        val vm = detalle("Cuadrar la caja", "Apagar equipos", tipo = TipoPlantilla.TAREAS)
        val listas = fakes.repositorioListas()
        val lista = checkNotNull(listas.crearDesdePlantilla(vm.plantillaId))

        vm.pedirBorrarPlantilla()
        vm.borrarPlantilla()

        assertTrue(vm.uiState.value.cerrada)
        assertTrue(fakes.tareasPlantilla.filas.value.isEmpty()) // cascade
        val copia = fakes.listas.filas.value.single()
        assertEquals(lista, copia.id)
        assertNull(copia.plantillaId) // SET NULL
        assertEquals(listOf("Cuadrar la caja", "Apagar equipos"), fakes.tareas.filas.value.map { it.texto })
    }

    @Test
    fun `la edicion abierta sobrevive a la recreacion`() = runTest(mainRule.dispatcher) {
        val id = repositorio.crear(TipoPlantilla.TRABAJADORES)
        repositorio.agregarElemento(TipoPlantilla.TRABAJADORES, id, "Diego")
        val handle = SavedStateHandle(mapOf(PlantillaDetalleViewModel.ARG_ID to id))
        val vm = PlantillaDetalleViewModel(repositorio, handle)
        vm.uiState.launchIn(backgroundScope)
        vm.empezarEdicion(vm.uiState.value.elementos.single())
        vm.actualizarBorradorEdicion("Diego Ferrer")

        val recreado = PlantillaDetalleViewModel(repositorio, handle)
        recreado.uiState.launchIn(backgroundScope)

        assertEquals(vm.uiState.value.elementos.single().id, recreado.uiState.value.editandoId)
        assertEquals("Diego Ferrer", recreado.borradorEdicion)
    }
}
