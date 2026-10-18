package com.valanon.calendar.ui.checklist

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.local.CLAVE_LISTA_CHECKLIST
import com.valanon.calendar.data.model.TipoPlantilla
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

class ChecklistViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fakes = FakesPlantillas()
    private val plantillas = fakes.repositorioPlantillas()

    private fun TestScope.crear(handle: SavedStateHandle = SavedStateHandle()) =
        ChecklistViewModel(fakes.repositorioListas(), fakes.repositorioTareas(), plantillas, handle)
            .also { it.uiState.launchIn(backgroundScope) }

    private fun ChecklistViewModel.escribirYAgregar(texto: String) {
        actualizarBorrador(texto)
        agregar()
    }

    private fun ChecklistViewModel.enBlanco() = crearEnBlanco { n -> "Lista $n" }

    private fun ChecklistViewModel.textos() = uiState.value.tareas.map { it.texto }

    private fun ChecklistViewModel.activa() = uiState.value.activa?.lista?.nombre

    /** A "Tareas" template with [tareas] in order. Returns its id. */
    private suspend fun preset(nombre: String, vararg tareas: String): Long {
        val id = plantillas.crear(TipoPlantilla.TAREAS)
        plantillas.renombrarPlantilla(id, nombre)
        tareas.forEach { plantillas.agregarElemento(TipoPlantilla.TAREAS, id, it) }
        return id
    }

    // --- L6 / L7: empty states and creating lists ---

    @Test
    fun `L6 sin listas muestra el estado vacio y sin progreso`() = runTest(mainRule.dispatcher) {
        val vm = crear()

        assertTrue(vm.uiState.value.sinListas)
        assertNull(vm.uiState.value.activa)
        assertEquals(0, vm.uiState.value.total)
    }

    @Test
    fun `L7 lista en blanco se llama Lista N, queda activa y vacia, y cierra la hoja`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.abrirHojaNueva()
        assertTrue(vm.uiState.value.hojaNuevaAbierta)

        vm.enBlanco()
        assertFalse(vm.uiState.value.hojaNuevaAbierta)
        assertEquals("Lista 1", vm.activa())
        assertNull(vm.uiState.value.activa?.nombrePlantilla)
        assertTrue(vm.uiState.value.tareas.isEmpty())
        assertFalse(vm.uiState.value.cargando)

        vm.enBlanco()
        assertEquals(listOf("Lista 1", "Lista 2"), vm.uiState.value.listas.map { it.lista.nombre })
        assertEquals("Lista 2", vm.activa())
    }

    @Test
    fun `L7 crear desde plantilla copia sus tareas sin marcar, en orden, y la activa`() = runTest(mainRule.dispatcher) {
        val id = preset("Rutina de apertura", "Revisar el cuadrante", "Comprobar la caja", "Encender equipos")
        val vm = crear()
        vm.enBlanco()

        vm.crearDesdePlantilla(id)

        assertEquals("Rutina de apertura", vm.activa())
        assertEquals("Rutina de apertura", vm.uiState.value.activa?.nombrePlantilla)
        assertEquals(listOf("Revisar el cuadrante", "Comprobar la caja", "Encender equipos"), vm.textos())
        assertTrue(vm.uiState.value.tareas.none { it.hecha })
        assertEquals(listOf(1, 2, 3), vm.uiState.value.tareas.map { it.orden })
    }

    @Test
    fun `L7 las tareas son copias - editar la plantilla despues no cambia la lista`() = runTest(mainRule.dispatcher) {
        val id = preset("Cierre de turno", "Cuadrar la caja", "Apagar equipos")
        val vm = crear()
        vm.crearDesdePlantilla(id)
        val tareaPreset = fakes.tareasPlantilla.filas.value.first()

        plantillas.cambiarTextoElemento(TipoPlantilla.TAREAS, tareaPreset.id, "Otra cosa")
        plantillas.agregarElemento(TipoPlantilla.TAREAS, id, "Nueva del preset")

        assertEquals(listOf("Cuadrar la caja", "Apagar equipos"), vm.textos())
    }

    @Test
    fun `L7 la hoja ofrece solo plantillas de tipo Tareas`() = runTest(mainRule.dispatcher) {
        plantillas.crear(TipoPlantilla.TRABAJADORES)
        preset("Rutina de apertura", "A")
        val vm = crear()

        assertEquals(listOf("Rutina de apertura"), vm.uiState.value.plantillasTareas.map { it.plantilla.nombre })
        assertEquals(1, vm.uiState.value.plantillasTareas.single().numElementos)
    }

    // --- L0: several lists, one active ---

    @Test
    fun `L0 activar cambia tareas y progreso, descarta el borrador y se recuerda`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle()
        val vm = crear(handle)
        vm.enBlanco()
        vm.escribirYAgregar("De la primera")
        val primera = vm.uiState.value.activa!!.lista.id
        vm.enBlanco()
        vm.escribirYAgregar("De la segunda")
        vm.actualizarBorrador("a medias")

        vm.activar(primera)

        assertEquals("Lista 1", vm.activa())
        assertEquals(listOf("De la primera"), vm.textos())
        assertEquals("", vm.borrador)
        assertEquals(primera.toString(), fakes.ajustes.filas.value[CLAVE_LISTA_CHECKLIST])
        // Reopening the app: a new ViewModel keeps the same active list.
        assertEquals("Lista 1", crear().activa())
    }

    @Test
    fun `L0 renombrar la lista activa y un nombre vacio conserva el anterior`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.enBlanco()

        vm.empezarRenombrar()
        assertTrue(vm.uiState.value.renombrando)
        assertEquals("Lista 1", vm.borradorNombre)
        vm.actualizarBorradorNombre("  Compras  ")
        vm.guardarNombre()
        assertFalse(vm.uiState.value.renombrando)
        assertEquals("Compras", vm.activa())

        vm.empezarRenombrar()
        vm.actualizarBorradorNombre("   ")
        vm.guardarNombre()
        assertEquals("Compras", vm.activa())
    }

    // --- L1–L4: tasks of the active list ---

    @Test
    fun `L1 agrega al final de la activa, recorta, vacia el campo y no toca otras listas`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.enBlanco()
        vm.escribirYAgregar("Otra lista")
        vm.enBlanco()

        vm.escribirYAgregar("  Primera  ")
        vm.escribirYAgregar("Segunda")

        assertEquals(listOf("Primera", "Segunda"), vm.textos())
        assertTrue(vm.uiState.value.tareas.none { it.hecha })
        assertEquals("", vm.borrador)
        assertEquals(listOf(1, 2), vm.uiState.value.listas.map { it.total })
    }

    @Test
    fun `L1 ignora textos vacios o solo espacios`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.enBlanco()

        vm.escribirYAgregar("")
        vm.escribirYAgregar("    ")

        assertTrue(vm.uiState.value.tareas.isEmpty())
        assertEquals("    ", vm.borrador)
    }

    @Test
    fun `L2 marcar y desmarcar actualiza el progreso de la lista y del chip`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.enBlanco()
        vm.escribirYAgregar("A")
        vm.escribirYAgregar("B")
        val a = vm.uiState.value.tareas.first()

        vm.marcar(a, true)
        assertEquals(1, vm.uiState.value.hechas)
        assertEquals(2, vm.uiState.value.total)
        assertEquals(1, vm.uiState.value.activa?.hechas)

        vm.marcar(a, false)
        assertEquals(0, vm.uiState.value.hechas)
    }

    @Test
    fun `L3 borrar y deshacer recupera la tarea en su sitio y con su estado`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.enBlanco()
        listOf("A", "B", "C").forEach { vm.escribirYAgregar(it) }
        vm.marcar(vm.uiState.value.tareas[1], true)
        val b = vm.uiState.value.tareas[1]

        vm.borrar(b)
        assertEquals(listOf("A", "C"), vm.textos())

        vm.restaurar(b)
        assertEquals(listOf("A", "B", "C"), vm.textos())
        assertTrue(vm.uiState.value.tareas[1].hecha)
    }

    @Test
    fun `L3 deshacer tras borrar la lista no falla ni resucita la tarea`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.enBlanco()
        vm.escribirYAgregar("A")
        val a = vm.uiState.value.tareas.single()

        vm.borrar(a)
        vm.borrarLista()
        vm.restaurar(a)

        assertTrue(fakes.tareas.filas.value.isEmpty())
        assertTrue(vm.uiState.value.sinListas)
    }

    @Test
    fun `L4 reiniciar desmarca solo la lista activa y no borra nada`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.enBlanco()
        vm.escribirYAgregar("Otra")
        vm.marcar(vm.uiState.value.tareas.single(), true)
        vm.enBlanco()
        listOf("A", "B", "C").forEach { vm.escribirYAgregar(it) }
        vm.uiState.value.tareas.forEach { vm.marcar(it, true) }
        assertEquals(3, vm.uiState.value.hechas)

        vm.reiniciar()

        assertEquals(3, vm.uiState.value.total)
        assertEquals(0, vm.uiState.value.hechas)
        assertEquals(listOf(1, 0), vm.uiState.value.listas.map { it.hechas })
    }

    // --- L8: delete a list ---

    @Test
    fun `L8 cancelar no borra y eliminar borra la lista con sus tareas y activa la siguiente`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        repeat(3) { vm.enBlanco() }
        val (primera, segunda) = vm.uiState.value.listas.map { it.lista.id }
        vm.activar(segunda)
        vm.escribirYAgregar("Se va")

        vm.pedirBorrarLista()
        assertTrue(vm.uiState.value.confirmandoBorrado)
        vm.cancelarBorrarLista()
        assertEquals(3, vm.uiState.value.listas.size)

        vm.pedirBorrarLista()
        vm.borrarLista()

        assertFalse(vm.uiState.value.confirmandoBorrado)
        assertEquals(listOf("Lista 1", "Lista 3"), vm.uiState.value.listas.map { it.lista.nombre })
        assertEquals("Lista 3", vm.activa())
        assertTrue(fakes.tareas.filas.value.none { it.texto == "Se va" })
        assertEquals(primera, vm.uiState.value.listas.first().lista.id)
    }

    @Test
    fun `L8 borrar la ultima de la fila activa la anterior y borrar la unica deja el estado vacio`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.enBlanco()
        vm.enBlanco()

        vm.borrarLista()
        assertEquals("Lista 1", vm.activa())

        vm.borrarLista()
        assertTrue(vm.uiState.value.sinListas)
        assertNull(vm.uiState.value.activa)
    }

    @Test
    fun `el borrador, la hoja y el dialogo sobreviven a la recreacion del ViewModel`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle()
        val vm = crear(handle)
        vm.enBlanco()
        vm.actualizarBorrador("a medias")
        vm.abrirHojaNueva()

        val recreado = crear(handle)

        assertEquals("a medias", recreado.borrador)
        assertTrue(recreado.uiState.value.hojaNuevaAbierta)
    }
}
