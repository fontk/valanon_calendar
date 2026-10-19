package com.valanon.calendar.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.testing.limpiarYSembrar
import com.valanon.calendar.util.FormatoFechas
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import java.time.LocalDate

/** Stories A1–A5 on the real app and database (tables wiped and the five types re-seeded first). */
@RunWith(AndroidJUnit4::class)
class AjustesAppTest {

    @get:Rule(order = 0)
    val limpiarBaseDeDatos = object : ExternalResource() {
        override fun before() = ApplicationProvider.getApplicationContext<CalendarApp>().container.database.limpiarYSembrar()
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun a1aA5_tiposDeTurnoDesdeAjustes() {
        // A1: Ajustes lists "Tipos de turno" with a mini chip per type and the version footer.
        pestana("Ajustes").performClick()
        esperarTexto("CALENDARIO")
        compose.onNodeWithText("Vacaciones").assertIsDisplayed()
        compose.onNode(hasText("Mis turnos · versión", substring = true)).assertIsDisplayed()

        // A2: the five defaults with their details.
        compose.onNode(hasText("Tipos de turno") and hasClickAction()).performClick()
        esperarTexto("5 de 16")
        compose.onNodeWithText("Por defecto · Índigo oscuro · sin usar").assertIsDisplayed()

        // A3: add "Guardia" — the abbreviation follows the name — in orange.
        compose.onNode(hasText("Añadir tipo de turno") and hasClickAction()).performScrollTo().performClick()
        esperarTexto("Nombre, abreviatura y color")
        val nombre = compose.onNode(hasSetTextAction() and hasContentDescription("Nombre"))
        nombre.performTextInput("Guardia")
        compose.onNode(hasSetTextAction() and hasContentDescription("Abreviatura")).assert(hasText("GUA"))
        compose.onNodeWithContentDescription("Naranja").performClick()
        compose.onNode(hasText("Añadir", substring = false) and hasClickAction()).performClick()
        esperarTexto("6 de 16")
        compose.onNodeWithText("Personalizado · Naranja · sin usar").assertIsDisplayed()

        // A3: a duplicate name shows the error and creates nothing.
        compose.onNode(hasText("Añadir tipo de turno") and hasClickAction()).performScrollTo().performClick()
        esperarTexto("Nombre, abreviatura y color")
        compose.onNode(hasSetTextAction() and hasContentDescription("Nombre")).performTextInput("noche")
        compose.onNode(hasText("Añadir", substring = false) and hasClickAction()).performClick()
        esperarTexto("Ya existe un turno con ese nombre.")
        compose.onNodeWithText("Cancelar").performClick()
        esperarAusente("Ya existe un turno con ese nombre.")

        // The new type is offered in Calendario's picker; mark today with it.
        pestana("Calendario").performClick()
        val dia = FormatoFechas.diaYMes(LocalDate.now())
        compose.onNodeWithContentDescription("$dia, sin turno").performClick()
        compose.onNode(hasText("Guardia") and hasClickAction()).performClick()
        esperarDescripcion("$dia, Guardia")

        // A4: renaming a type repaints the day already marked with it.
        pestana("Ajustes").performClick()
        esperarTexto("6 de 16")
        compose.onNodeWithContentDescription("Editar Guardia").performClick()
        esperarTexto("EDITAR TIPO DE TURNO")
        val campo = compose.onNode(hasSetTextAction() and hasContentDescription("Nombre"))
        campo.performTextClearance()
        campo.performTextInput("Guardia larga")
        compose.onNode(hasText("Guardar", substring = false) and hasClickAction()).performClick()
        esperarTexto("Personalizado · Naranja · 1 día, 0 turnos del equipo")
        pestana("Calendario").performClick()
        esperarDescripcion("$dia, Guardia larga")

        // A5: deleting shows the usage; the day is left without a shift.
        pestana("Ajustes").performClick()
        compose.onNodeWithContentDescription("Eliminar Guardia larga").performClick()
        compose.onNodeWithText("¿Eliminar el turno «Guardia larga»?").assertIsDisplayed()
        compose.onNodeWithText("Está marcado en 1 día de tu calendario. Esos días quedarán sin turno.").assertIsDisplayed()
        compose.onNode(hasText("Eliminar", substring = false) and hasClickAction()).performClick()
        esperarTexto("5 de 16")
        pestana("Calendario").performClick()
        esperarDescripcion("$dia, sin turno")

        // Back from "Tipos de turno" returns to Ajustes.
        pestana("Ajustes").performClick()
        esperarTexto("5 de 16")
        Espresso.pressBack()
        esperarTexto("CALENDARIO")
    }

    @Test
    fun a5_elUltimoTipoNoSePuedeEliminar() {
        val tipos = ApplicationProvider.getApplicationContext<CalendarApp>().container.database.tiposTurnoDao()
        kotlinx.coroutines.runBlocking { (1L..4L).forEach { tipos.borrar(it) } }
        pestana("Ajustes").performClick()
        compose.onNode(hasText("Tipos de turno") and hasClickAction()).performClick()
        esperarTexto("1 de 16")
        compose.onNodeWithContentDescription("Eliminar Vacaciones").assertIsNotEnabled()
    }

    private fun pestana(nombre: String): SemanticsNodeInteraction =
        compose.onNode(hasText(nombre) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    private fun esperarTexto(texto: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(texto, substring = false)).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarAusente(texto: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(texto, substring = false)).fetchSemanticsNodes().isEmpty() }

    private fun esperarDescripcion(descripcion: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription(descripcion)).fetchSemanticsNodes().isNotEmpty() }
}
