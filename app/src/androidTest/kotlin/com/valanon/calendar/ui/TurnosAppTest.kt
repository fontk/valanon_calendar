package com.valanon.calendar.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.util.FormatoFechas
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import java.time.LocalDate

/** Stories T1–T7 (spec 3b) on the real app and database (tables wiped before each test). */
@RunWith(AndroidJUnit4::class)
class TurnosAppTest {

    private val app get() = ApplicationProvider.getApplicationContext<CalendarApp>()

    @get:Rule(order = 0)
    val limpiarBaseDeDatos = object : ExternalResource() {
        override fun before() = app.container.database.clearAllTables()
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val hoy = LocalDate.now()
    private val dia = FormatoFechas.diaYMes(hoy)

    /** Creates a "Trabajadores" template through the repository. */
    private fun crearPlantilla(nombre: String, vararg trabajadores: String): Long = runBlocking {
        val repo = app.container.plantillasRepositorio
        val id = repo.crear(TipoPlantilla.TRABAJADORES)
        repo.renombrarPlantilla(id, nombre)
        trabajadores.forEach { repo.agregarTrabajador(id, it) }
        id
    }

    @Test
    fun t2_sinPlantillasLlevaAPlantillas() {
        pestana("Turnos").performClick()
        compose.onNodeWithText("Aún no hay ninguna plantilla de trabajadores").assertIsDisplayed()

        compose.onNodeWithText("Ir a Plantillas").performClick()
        compose.onNodeWithText("Sin plantillas todavía").assertIsDisplayed()
    }

    @Test
    fun t1aT4_editarLosTurnosDeUnDia() {
        crearPlantilla("Equipo de planta", "Lucía Martínez", "Javier Ortega")
        pestana("Turnos").performClick()

        // T1: active template chip and the day cell with nobody assigned.
        esperarVisible("Equipo de planta")
        compose.onNodeWithText("TURNOS DEL EQUIPO").assertIsDisplayed()

        // T3: open the day in read mode.
        compose.onNodeWithContentDescription("$dia, 0 de 2 asignados").performClick()
        compose.onNodeWithText("EQUIPO DEL DÍA").assertIsDisplayed()
        compose.onNodeWithText("Equipo de planta · 0 de 2 asignados").assertIsDisplayed()

        // T4: edit → Javier's chip → picker (same as Calendario) → Noche.
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithContentDescription("Cambiar turno de Javier Ortega, sin asignar").performClick()
        compose.onNodeWithText("Turno de Javier Ortega").assertIsDisplayed()
        compose.onAllNodes(hasText("Quitar turno")).assertCountEquals(0)
        compose.onNode(hasText("Noche") and hasClickAction() and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .performClick()
        esperarDescripcion("Cambiar turno de Javier Ortega, Noche")
        compose.onNodeWithText("Equipo de planta · 1 de 2 asignados").assertIsDisplayed()

        // Back inside the picker returns to the team list (the sheet stays open).
        compose.onNodeWithContentDescription("Cambiar turno de Javier Ortega, Noche").performClick()
        compose.onNodeWithText("Turno de Javier Ortega").assertIsDisplayed()
        Espresso.pressBack()
        esperarDescripcion("Cambiar turno de Javier Ortega, Noche")

        // "Quitar turno" removes it.
        compose.onNodeWithContentDescription("Cambiar turno de Javier Ortega, Noche").performClick()
        compose.onNodeWithText("Quitar turno").performClick()
        esperarDescripcion("Cambiar turno de Javier Ortega, sin asignar")

        // Assign again, then "Listo" → read mode chips.
        compose.onNodeWithContentDescription("Cambiar turno de Javier Ortega, sin asignar").performClick()
        compose.onNode(hasText("Tarde") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)).performClick()
        esperarDescripcion("Cambiar turno de Javier Ortega, Tarde")
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Sin asignar").assertIsDisplayed() // Lucía

        // Close: the cell reflects the change.
        compose.onNodeWithText("Cerrar").performClick()
        esperarDescripcion("$dia, 1 de 2 asignados")

        // T7: survives recreation.
        compose.activityRule.scenario.recreate()
        pestana("Turnos").performClick()
        esperarDescripcion("$dia, 1 de 2 asignados")

        // Spec 3b.5: the delete-template dialog mentions the assigned shifts.
        pestana("Plantillas").performClick()
        compose.onNodeWithText("Equipo de planta").performClick()
        compose.onNodeWithText("Eliminar plantilla").performClick()
        compose.onNodeWithText("Se borrarán también sus 2 trabajadores y su turno asignado.").assertIsDisplayed()
    }

    @Test
    fun t5t6_cambiarDePlantillaYBorrarLaActiva() {
        crearPlantilla("Equipo de planta", "Lucía Martínez")
        crearPlantilla("Refuerzos fin de semana", "Sara López", "Diego Ferrer")
        pestana("Turnos").performClick()
        esperarVisible("Equipo de planta")

        // T5: pick the other team; remembered after recreation.
        compose.onNodeWithContentDescription("Elegir plantilla de trabajadores").performClick()
        compose.onNodeWithText("¿Qué equipo quieres ver?").assertIsDisplayed()
        compose.onNode(hasText("Refuerzos fin de semana") and hasClickAction()).performClick()
        esperarDescripcion("$dia, 0 de 2 asignados")
        compose.activityRule.scenario.recreate()
        pestana("Turnos").performClick()
        esperarVisible("Refuerzos fin de semana")

        // T6: delete the active template in Plantillas → Turnos falls back to the other one.
        pestana("Plantillas").performClick()
        compose.onNodeWithText("Refuerzos fin de semana").performClick()
        compose.onNodeWithText("Eliminar plantilla").performClick()
        compose.onNode(hasText("Eliminar", substring = false) and hasClickAction()).performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("1 plantilla")).fetchSemanticsNodes().isNotEmpty() }
        pestana("Turnos").performClick()
        esperarVisible("Equipo de planta")
        esperarDescripcion("$dia, 0 de 1 asignado")
    }

    private fun pestana(nombre: String): SemanticsNodeInteraction =
        compose.onNode(hasText(nombre) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    /**
     * Room emits on a background thread that Compose's idling does not track, so right after
     * a tab switch the node may not be laid out yet: wait until it is actually displayed.
     */
    private fun esperarVisible(texto: String) =
        compose.waitUntil(5_000) { runCatching { compose.onNodeWithText(texto).assertIsDisplayed() }.isSuccess }

    private fun esperarDescripcion(descripcion: String) =
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasContentDescription(descripcion)).fetchSemanticsNodes().isNotEmpty()
        }
}
