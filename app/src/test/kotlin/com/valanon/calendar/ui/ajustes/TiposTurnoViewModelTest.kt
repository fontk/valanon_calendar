package com.valanon.calendar.ui.ajustes

import androidx.lifecycle.SavedStateHandle
import com.valanon.calendar.data.local.MAX_TIPOS_TURNO
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.repository.ErrorTipoTurno
import com.valanon.calendar.data.repository.TurnosRepositorio
import com.valanon.calendar.testing.FakesPlantillas
import com.valanon.calendar.testing.MainDispatcherRule
import com.valanon.calendar.testing.Tipos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TiposTurnoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val fakes = FakesPlantillas()

    private fun TestScope.crear(handle: SavedStateHandle = SavedStateHandle()) =
        TiposTurnoViewModel(fakes.repositorioTipos(), handle).also { it.uiState.launchIn(backgroundScope) }

    private fun TiposTurnoViewModel.nombres() = uiState.value.tipos.map { it.tipo.nombre }

    private fun TiposTurnoViewModel.crearTipo(nombre: String, color: String? = null) {
        nuevo()
        cambiarNombre(nombre)
        if (color != null) elegirColor(color)
        guardar()
    }

    @Test
    fun `A2 una instalacion nueva tiene los cinco por defecto`() = runTest(mainRule.dispatcher) {
        val vm = crear()

        assertEquals(listOf("Libre", "Mañana", "Tarde", "Noche", "Vacaciones"), vm.nombres())
        assertTrue(vm.uiState.value.tipos.all { it.tipo.porDefecto })
        assertEquals(5, vm.uiState.value.total)
        assertTrue(vm.uiState.value.puedeAnadir)
    }

    @Test
    fun `A3 nuevo abre vacio con el primer color libre y la abreviatura sigue al nombre`() = runTest(mainRule.dispatcher) {
        val vm = crear()

        vm.nuevo()
        val editor = vm.uiState.value.editor!!
        assertTrue(editor.esNuevo)
        assertEquals("ambar", editor.color)
        assertEquals("", vm.nombre)

        vm.cambiarNombre("Guardia")
        assertEquals("GUA", vm.abreviatura)
        vm.elegirColor("naranja")
        vm.guardar()

        assertNull(vm.uiState.value.editor)
        val guardia = vm.uiState.value.tipos.last().tipo
        assertEquals(listOf("Guardia", "GUA", "naranja"), listOf(guardia.nombre, guardia.abreviatura, guardia.color))
        assertFalse(guardia.porDefecto)
        assertEquals(6, guardia.orden)
    }

    @Test
    fun `A3 nombre vacio o repetido muestra el error y no crea nada`() = runTest(mainRule.dispatcher) {
        val vm = crear()

        vm.nuevo()
        vm.cambiarNombre("   ")
        vm.guardar()
        assertEquals(ErrorTipoTurno.NOMBRE_VACIO, vm.uiState.value.editor?.error)

        vm.cambiarNombre("noche")
        assertNull(vm.uiState.value.editor?.error) // typing clears it
        vm.guardar()
        assertEquals(ErrorTipoTurno.NOMBRE_REPETIDO, vm.uiState.value.editor?.error)
        assertEquals(5, vm.uiState.value.total)
    }

    @Test
    fun `A3 el nombre se corta a 20 caracteres y la abreviatura a 5 en mayusculas`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.nuevo()

        vm.cambiarNombre("Refuerzo de fin de semana largo")
        vm.cambiarAbreviatura("refuerzo")

        assertEquals(20, vm.nombre.length)
        assertEquals("REFUE", vm.abreviatura)
    }

    @Test
    fun `A3 con 16 tipos no se puede anadir`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        (1..MAX_TIPOS_TURNO - 5).forEach { vm.crearTipo("Tipo $it") }
        assertEquals(MAX_TIPOS_TURNO, vm.uiState.value.total)
        assertFalse(vm.uiState.value.puedeAnadir)

        vm.crearTipo("Uno más")
        assertEquals(ErrorTipoTurno.LIMITE, vm.uiState.value.editor?.error)
        assertEquals(MAX_TIPOS_TURNO, vm.uiState.value.total)
    }

    @Test
    fun `A4 editar conserva las asignaciones y la abreviatura escrita a mano no se sobrescribe`() = runTest(mainRule.dispatcher) {
        fakes.turnos.upsert(Turno("2026-10-17", Tipos.NOCHE.id))
        val turnos = TurnosRepositorio(fakes.turnos, fakes.tipos)
        val vm = crear()

        vm.editar(Tipos.NOCHE)
        assertFalse(vm.uiState.value.editor!!.esNuevo)
        vm.cambiarNombre("Nocturno")
        assertEquals("NOCHE", vm.abreviatura) // editing: the saved abbreviation counts as typed
        vm.cambiarAbreviatura("noc")
        vm.cambiarNombre("Nocturna")
        assertEquals("NOC", vm.abreviatura)
        vm.elegirColor("pizarra")
        vm.guardar()

        val dia = turnos.observarMes(YearMonth.of(2026, 10)).first()[LocalDate.of(2026, 10, 17)]!!
        assertEquals(Tipos.NOCHE.id, dia.id)
        assertEquals(listOf("Nocturna", "NOC", "pizarra"), listOf(dia.nombre, dia.abreviatura, dia.color))
    }

    @Test
    fun `A5 eliminar muestra los usos, cancelar no borra y eliminar borra en cascada`() = runTest(mainRule.dispatcher) {
        val p = fakes.repositorioPlantillas().crear(TipoPlantilla.TRABAJADORES)
        fakes.repositorioPlantillas().agregarElemento(TipoPlantilla.TRABAJADORES, p, "Lucía")
        val lucia = fakes.trabajadores.filas.value.single().id
        fakes.turnos.upsert(Turno("2026-10-17", Tipos.NOCHE.id))
        fakes.turnos.upsert(Turno("2026-10-18", Tipos.NOCHE.id))
        fakes.turnos.upsert(Turno("2026-10-19", Tipos.LIBRE.id))
        fakes.asignaciones.upsert(Asignacion(fecha = "2026-10-17", trabajadorId = lucia, tipoId = Tipos.NOCHE.id))
        val vm = crear()

        vm.pedirBorrar(Tipos.NOCHE)
        val dialogo = vm.uiState.value.confirmandoBorrado!!
        assertEquals(2 to 1, dialogo.usos.dias to dialogo.usos.equipo)
        vm.cancelarBorrar()
        assertNull(vm.uiState.value.confirmandoBorrado)
        assertEquals(5, vm.uiState.value.total)

        vm.pedirBorrar(Tipos.NOCHE)
        vm.borrar()

        assertEquals(listOf("Libre", "Mañana", "Tarde", "Vacaciones"), vm.nombres())
        assertEquals(listOf("2026-10-19"), fakes.turnos.filas.value.keys.toList())
        assertTrue(fakes.asignaciones.filas.value.isEmpty())
    }

    @Test
    fun `A5 el ultimo tipo no se puede eliminar`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        listOf(Tipos.LIBRE, Tipos.MANANA, Tipos.TARDE, Tipos.NOCHE).forEach {
            vm.pedirBorrar(it)
            vm.borrar()
        }
        assertEquals(listOf("Vacaciones"), vm.nombres())
        assertFalse(vm.uiState.value.puedeBorrar)

        vm.pedirBorrar(Tipos.VACACIONES)
        assertNull(vm.uiState.value.confirmandoBorrado)
        assertEquals(ErrorTipoTurno.ULTIMO, fakes.repositorioTipos().borrar(Tipos.VACACIONES.id))
        assertEquals(1, vm.uiState.value.total)
    }

    @Test
    fun `A6 restablecer recupera los cinco y conserva los personalizados`() = runTest(mainRule.dispatcher) {
        val vm = crear()
        vm.editar(Tipos.NOCHE)
        vm.cambiarNombre("Nocturno")
        vm.cambiarAbreviatura("NOC")
        vm.elegirColor("pizarra")
        vm.guardar()
        vm.pedirBorrar(Tipos.VACACIONES)
        vm.borrar()
        vm.crearTipo("Guardia", "naranja")

        vm.pedirRestablecer()
        assertTrue(vm.uiState.value.confirmandoRestablecer)
        vm.restablecer()

        assertFalse(vm.uiState.value.confirmandoRestablecer)
        assertEquals(listOf("Libre", "Mañana", "Tarde", "Noche", "Guardia", "Vacaciones"), vm.nombres())
        val noche = vm.uiState.value.tipos.first { it.tipo.clave == "noche" }.tipo
        assertEquals(Tipos.NOCHE, noche) // same id, name, abbreviation and colour as the default
        assertEquals("naranja", vm.uiState.value.tipos.first { it.tipo.nombre == "Guardia" }.tipo.color)
        assertEquals("vacaciones", vm.uiState.value.tipos.last().tipo.clave)
    }

    @Test
    fun `el editor abierto sobrevive a la recreacion`() = runTest(mainRule.dispatcher) {
        val handle = SavedStateHandle()
        val vm = crear(handle)
        vm.nuevo()
        vm.cambiarNombre("Guardia")
        vm.elegirColor("rojo")

        val recreado = crear(handle)

        assertEquals("Guardia", recreado.nombre)
        assertEquals("GUA", recreado.abreviatura)
        assertEquals("rojo", recreado.uiState.value.editor?.color)
    }
}
