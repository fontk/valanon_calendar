package com.valanon.calendar.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.CalendarApp
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

/** Stories P1–P9 (spec 4b) on the real app and database (tables wiped before each test). */
@RunWith(AndroidJUnit4::class)
class PlantillasAppTest {

    @get:Rule(order = 0)
    val limpiarBaseDeDatos = object : ExternalResource() {
        override fun before() {
            ApplicationProvider.getApplicationContext<CalendarApp>().container.database.clearAllTables()
        }
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun p1aP7_crearRenombrarYGestionarTrabajadores() {
        pestana("Plantillas").performClick()

        // P1: empty list, no counter.
        compose.onNodeWithText("Sin plantillas todavía").assertIsDisplayed()
        compose.onNodeWithText("Pulsa + para crear tu primera plantilla.").assertIsDisplayed()

        // P2: + → type sheet → Trabajadores creates "Trabajadores 1" and opens it renaming.
        compose.onNodeWithContentDescription("Añadir plantilla").performClick()
        compose.onNodeWithText("¿Qué tipo de plantilla?").assertIsDisplayed()
        compose.onNode(hasText("Trabajadores") and hasClickAction()).performClick()
        val campoNombre = compose.onNode(hasSetTextAction() and hasContentDescription("Nombre de la plantilla"))
        esperar { campoNombre.fetchSemanticsNode() }
        campoNombre.assert(hasText("Trabajadores 1"))
        compose.onNodeWithText("0 TRABAJADORES").assertIsDisplayed()

        // P4: rename.
        campoNombre.performTextClearance()
        campoNombre.performTextInput("Equipo de planta")
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Equipo de planta").assertIsDisplayed()

        // P5: add with the button and with IME Done; blanks ignored.
        val alta = compose.onNode(hasSetTextAction() and hasContentDescription("Nombre del trabajador"))
        alta.performTextInput("   ")
        compose.onNodeWithText("Agregar").performClick()
        compose.onNodeWithText("Sin trabajadores todavía").assertIsDisplayed()
        alta.performTextInput("Lucía Martínez")
        compose.onNodeWithText("Agregar").performClick()
        alta.performTextInput("Javier Ortega")
        alta.performImeAction()
        esperarTexto("2 TRABAJADORES")

        // P6: inline edit of the second worker.
        compose.onAllNodes(hasContentDescription("Editar trabajador"))[1].performClick()
        // Two fields share the description: the add field (first) and the inline edit (second).
        val edicion = compose.onAllNodes(hasSetTextAction() and hasContentDescription("Nombre del trabajador"))[1]
        edicion.assert(hasText("Javier Ortega"))
        edicion.performTextClearance()
        edicion.performTextInput("Javier Ortega Ruiz")
        compose.onNodeWithText("Guardar").performClick()
        esperarTexto("Javier Ortega Ruiz")

        // P7: delete + undo.
        compose.onAllNodes(hasContentDescription("Eliminar trabajador"))[0].performClick()
        esperarTexto("1 TRABAJADOR")
        compose.onNodeWithText("Deshacer").performClick()
        esperarTexto("2 TRABAJADORES")
        compose.onNodeWithText("Lucía Martínez").assertIsDisplayed()

        // P3: back arrow → list with the card, subtitle and counter.
        compose.onNodeWithContentDescription("Volver a plantillas").performClick()
        compose.onNodeWithText("Equipo de planta").assertIsDisplayed()
        compose.onNodeWithText("Trabajadores · 2 trabajadores").assertIsDisplayed()
        compose.onNodeWithText("1 plantilla").assertIsDisplayed()
    }

    @Test
    fun p3p8p9_navegacionPersistenciaYBorrado() {
        crearPlantillaConTrabajadores("Equipo de planta", "Lucía Martínez", "Javier Ortega")

        // Switching tabs keeps the open template.
        compose.onNodeWithText("Equipo de planta").performClick()
        compose.onNodeWithText("PLANTILLA · TRABAJADORES").assertIsDisplayed()
        pestana("Calendario").performClick()
        pestana("Plantillas").performClick()
        compose.onNodeWithText("PLANTILLA · TRABAJADORES").assertIsDisplayed()

        // P3: system Back returns to the list.
        Espresso.pressBack()
        compose.onNodeWithText("1 plantilla").assertIsDisplayed()

        // P9: survives recreation.
        compose.activityRule.scenario.recreate()
        pestana("Plantillas").performClick()
        compose.onNodeWithText("Trabajadores · 2 trabajadores").assertIsDisplayed()

        // P8: cancel keeps it; confirm deletes it and goes back to the list.
        compose.onNodeWithText("Equipo de planta").performClick()
        compose.onNodeWithText("Eliminar plantilla").performClick()
        compose.onNodeWithText("¿Eliminar la plantilla «Equipo de planta»?").assertIsDisplayed()
        compose.onNodeWithText("Se borrarán también sus 2 trabajadores.").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("PLANTILLA · TRABAJADORES").assertIsDisplayed()

        compose.onNodeWithText("Eliminar plantilla").performClick()
        compose.onNode(hasText("Eliminar", substring = false) and hasClickAction()).performClick()
        esperarTexto("Sin plantillas todavía")
    }

    /** P2 + P10 + L7: a task template is a preset; lists made from it are independent copies. */
    @Test
    fun p10l7_plantillaDeTareasComoPreset() {
        pestana("Plantillas").performClick()

        // P2: the type sheet offers Tareas; it creates "Tareas 1" and opens it renaming.
        compose.onNodeWithContentDescription("Añadir plantilla").performClick()
        compose.onNodeWithText("Preset de tareas para crear listas en Checklist").assertIsDisplayed()
        compose.onNode(hasText("Tareas") and hasClickAction()).performClick()
        val campoNombre = compose.onNode(hasSetTextAction() and hasContentDescription("Nombre de la plantilla"))
        esperar { campoNombre.fetchSemanticsNode() }
        campoNombre.assert(hasText("Tareas 1"))
        compose.onNodeWithText("PLANTILLA · TAREAS").assertIsDisplayed()
        campoNombre.performTextClearance()
        campoNombre.performTextInput("Rutina de apertura")
        campoNombre.performImeAction()

        // P10: add tasks to the preset; rows are numbered.
        val alta = compose.onNode(hasSetTextAction() and hasContentDescription("Texto de la tarea"))
        listOf("Revisar el cuadrante", "Comprobar la caja", "Encender equipos").forEach {
            alta.performTextInput(it)
            alta.performImeAction()
        }
        esperarTexto("3 TAREAS")
        compose.onAllNodes(hasContentDescription("Eliminar tarea"))[1].performClick()
        esperarTexto("2 TAREAS")
        compose.onNodeWithContentDescription("Volver a plantillas").performClick()
        esperarTexto("Tareas · 2 tareas")

        // L7: Checklist → Nueva lista → the preset → a list with its tasks unchecked, active.
        pestana("Checklist").performClick()
        compose.onNode(hasText("Nueva lista") and hasClickAction()).performClick()
        esperarTexto("¿Desde qué plantilla de tareas?")
        compose.onNodeWithText("2 tareas").assertIsDisplayed()
        compose.onNode(hasText("Rutina de apertura") and hasClickAction()).performClick()
        esperarTexto("Desde la plantilla «Rutina de apertura»")
        compose.onNodeWithText("0 de 2 hechas").assertIsDisplayed()
        compose.onNodeWithText("Encender equipos").assertIsDisplayed()

        // P10: deleting the template says lists are not affected, and they are not.
        pestana("Plantillas").performClick()
        compose.onNodeWithText("Rutina de apertura").performClick()
        compose.onNodeWithText("Eliminar plantilla").performClick()
        compose.onNodeWithText(
            "Se borrarán también sus 2 tareas. Las listas ya creadas en Checklist no se ven afectadas.",
        ).assertIsDisplayed()
        compose.onNode(hasText("Eliminar", substring = false) and hasClickAction()).performClick()
        esperarTexto("Sin plantillas todavía")
        pestana("Checklist").performClick()
        esperarTexto("Encender equipos")
        compose.onNodeWithText("Lista en blanco").assertIsDisplayed() // source gone → informative only
        compose.onNodeWithText("0 de 2 hechas").assertIsDisplayed()
    }

    private fun crearPlantillaConTrabajadores(nombre: String, vararg trabajadores: String) {
        pestana("Plantillas").performClick()
        compose.onNodeWithContentDescription("Añadir plantilla").performClick()
        compose.onNode(hasText("Trabajadores") and hasClickAction()).performClick()
        val campoNombre = compose.onNode(hasSetTextAction() and hasContentDescription("Nombre de la plantilla"))
        esperar { campoNombre.fetchSemanticsNode() }
        campoNombre.performTextClearance()
        campoNombre.performTextInput(nombre)
        campoNombre.performImeAction()
        val alta = compose.onNode(hasSetTextAction() and hasContentDescription("Nombre del trabajador"))
        trabajadores.forEach {
            alta.performTextInput(it)
            alta.performImeAction()
        }
        esperarTexto("${trabajadores.size} TRABAJADORES")
        compose.onNodeWithContentDescription("Volver a plantillas").performClick()
        esperarTexto(nombre)
    }

    private fun pestana(nombre: String): SemanticsNodeInteraction =
        compose.onNode(hasText(nombre) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    private fun esperarTexto(texto: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(texto, substring = false)).fetchSemanticsNodes().isNotEmpty() }

    private fun esperar(bloque: () -> Unit) =
        compose.waitUntil(5_000) { runCatching(bloque).isSuccess }
}
