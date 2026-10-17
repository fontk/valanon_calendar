package com.valanon.calendar.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.valanon.calendar.CalendarApp
import com.valanon.calendar.util.FormatoFechas
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/**
 * End-to-end checks of the user stories on the real app and the real database
 * (tables are wiped before each test).
 */
@RunWith(AndroidJUnit4::class)
class MisTurnosAppTest {

    @get:Rule(order = 0)
    val limpiarBaseDeDatos = object : org.junit.rules.ExternalResource() {
        override fun before() {
            ApplicationProvider.getApplicationContext<CalendarApp>().container.database.clearAllTables()
        }
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val hoy = LocalDate.now()

    @Before
    fun esperarCarga() = compose.waitForIdle()

    @Test
    fun c1_abreEnElMesActual() {
        compose.onNodeWithText(FormatoFechas.tituloMes(YearMonth.now())).assertIsDisplayed()
        compose.onNodeWithText("MIS TURNOS").assertIsDisplayed()
        pestana("Calendario").assertIsDisplayed()
    }

    @Test
    fun c2_navegaAlMesSiguienteYVuelve() {
        compose.onNodeWithContentDescription("Mes siguiente").performClick()
        compose.onNodeWithText(FormatoFechas.tituloMes(YearMonth.now().plusMonths(1))).assertIsDisplayed()
        compose.onNodeWithContentDescription("Mes anterior").performClick()
        compose.onNodeWithText(FormatoFechas.tituloMes(YearMonth.now())).assertIsDisplayed()
    }

    @Test
    fun c3c4_marcarTurnoYQueSigaTrasRecrear() {
        val dia = FormatoFechas.diaYMes(hoy)
        compose.onNodeWithContentDescription("$dia, sin turno").performClick()
        compose.onNodeWithText("TURNO DEL DÍA").assertIsDisplayed()
        compose.onNodeWithText(FormatoFechas.tituloDia(hoy)).assertIsDisplayed()
        compose.onNode(hasText("Noche") and hasClickAction()).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasContentDescription("$dia, Noche")).fetchSemanticsNodes().isNotEmpty()
        }

        // Reopen: the current option is marked "Actual". Cancel leaves it unchanged.
        compose.onNodeWithContentDescription("$dia, Noche").performClick()
        compose.onNodeWithText("Actual").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTextExactly("TURNO DEL DÍA")).fetchSemanticsNodes().isEmpty() }

        compose.activityRule.scenario.recreate()
        compose.onNodeWithContentDescription("$dia, Noche").assertIsDisplayed()
    }

    @Test
    fun l1aL6_checklistCompleta() {
        pestana("Checklist").performClick()

        // L6: empty state, no progress counter
        compose.onNodeWithText("Sin tareas todavía").assertIsDisplayed()
        compose.onNodeWithText("Escribe una tarea arriba y pulsa Agregar.").assertIsDisplayed()

        // L1: add with the button and with IME Done; blank text is ignored
        val campo = compose.onNode(hasSetTextAction())
        campo.performTextInput("   ")
        compose.onNodeWithText("Agregar").performClick()
        compose.onNodeWithText("Sin tareas todavía").assertIsDisplayed()

        campo.performTextInput("Preparar el uniforme")
        compose.onNodeWithText("Agregar").performClick()
        campo.performTextInput("Comprar comida")
        campo.performImeAction()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTextExactly("0 de 2 hechas")).fetchSemanticsNodes().isNotEmpty() }

        // L2: toggle
        compose.onNodeWithText("Preparar el uniforme").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTextExactly("1 de 2 hechas")).fetchSemanticsNodes().isNotEmpty() }

        // L4: reset unchecks without deleting
        compose.onNodeWithContentDescription("Reiniciar todas las tareas").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTextExactly("0 de 2 hechas")).fetchSemanticsNodes().isNotEmpty() }

        // L5: survives recreation
        compose.activityRule.scenario.recreate()
        pestana("Checklist").performClick()
        compose.onNodeWithText("Comprar comida").assertIsDisplayed()

        // L3: delete both
        repeat(2) {
            compose.onAllNodes(hasContentDescription("Borrar tarea"))[0].performClick()
            compose.waitForIdle()
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTextExactly("Sin tareas todavía")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun c2b_selectorDeMesYAnio() {
        val actual = YearMonth.now()
        val mesHoy = FormatoFechas.nombreMes(actual.monthValue)

        // Opens on the visible year with the visible month selected.
        compose.onNodeWithContentDescription("Elegir mes y año").performClick()
        compose.onNodeWithText("IR A MES").assertIsDisplayed()
        compose.onNodeWithText(actual.year.toString()).assertIsDisplayed()
        compose.onNodeWithContentDescription("$mesHoy de ${actual.year}").assertIsSelected()

        // Year arrows change only the year shown: nothing is selected two years ahead.
        compose.onNodeWithContentDescription("Año siguiente").performClick()
        compose.onNodeWithContentDescription("Año siguiente").performClick()
        val destino = actual.year + 2
        compose.onNodeWithText(destino.toString()).assertIsDisplayed()
        compose.onNodeWithContentDescription("$mesHoy de $destino").assertIsNotSelected()

        // Picking March jumps there and closes the sheet.
        compose.onNodeWithContentDescription("marzo de $destino").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTextExactly("IR A MES")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Marzo $destino").assertIsDisplayed()

        // Rotation-equivalent: the open sheet and its year survive recreation.
        compose.onNodeWithContentDescription("Elegir mes y año").performClick()
        compose.onNodeWithContentDescription("Año anterior").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("IR A MES").assertIsDisplayed()
        compose.onNodeWithText((destino - 1).toString()).assertIsDisplayed()

        // "Cancelar" keeps the visible month.
        compose.onNodeWithText("Cancelar").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTextExactly("IR A MES")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Marzo $destino").assertIsDisplayed()

        // "Hoy" goes back to the current month.
        compose.onNodeWithContentDescription("Elegir mes y año").performClick()
        compose.onNodeWithText("Hoy").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTextExactly("IR A MES")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText(FormatoFechas.tituloMes(actual)).assertIsDisplayed()
    }

    @Test
    fun cambiarDePestanaConservaElMesVisible() {
        compose.onNodeWithContentDescription("Mes siguiente").performClick()
        pestana("Checklist").performClick()
        pestana("Calendario").performClick()
        compose.onNodeWithText(FormatoFechas.tituloMes(YearMonth.now().plusMonths(1))).assertIsDisplayed()
    }

    private fun hasTextExactly(texto: String) = hasText(texto, substring = false)

    /** Bottom-bar tab (the Checklist screen also has a "Checklist" title). */
    private fun pestana(nombre: String) =
        compose.onNode(hasText(nombre) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
}
