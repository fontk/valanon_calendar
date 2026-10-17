package com.valanon.calendar.ui

import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.valanon.calendar.R
import com.valanon.calendar.ui.calendario.CalendarioRoute
import com.valanon.calendar.ui.checklist.ChecklistRoute
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.plantillas.PlantillaTrabajadoresRoute
import com.valanon.calendar.ui.plantillas.PlantillaTrabajadoresViewModel
import com.valanon.calendar.ui.plantillas.PlantillasRoute
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText

/**
 * The three bottom-bar destinations, in bar order. [ruta] is the Navigation Compose route of
 * the tab: a screen for Calendario/Checklist, a nested graph for Plantillas (list + detail).
 */
enum class Destino(val ruta: String, @param:StringRes val etiqueta: Int, val icono: ImageVector) {
    CALENDARIO("calendario", R.string.tab_calendario, MisTurnosIcons.Calendario),
    CHECKLIST("checklist", R.string.tab_checklist, MisTurnosIcons.Checklist),
    PLANTILLAS("plantillas_tab", R.string.tab_plantillas, MisTurnosIcons.Plantillas),
}

/** Routes inside the Plantillas tab graph. */
object RutasPlantillas {
    const val LISTA = "plantillas"
    const val DETALLE =
        "plantillas/{${PlantillaTrabajadoresViewModel.ARG_ID}}?${PlantillaTrabajadoresViewModel.ARG_RENOMBRAR}={${PlantillaTrabajadoresViewModel.ARG_RENOMBRAR}}"

    fun detalle(id: Long, renombrar: Boolean) = "plantillas/$id?${PlantillaTrabajadoresViewModel.ARG_RENOMBRAR}=$renombrar"
}

@Composable
fun MisTurnosApp(navController: NavHostController = rememberNavController()) {
    val entradaActual by navController.currentBackStackEntryAsState()
    // The tab whose route is the current destination or one of its parent graphs.
    val destinoActual = Destino.entries.firstOrNull { destino ->
        entradaActual?.destination?.hierarchy?.any { it.route == destino.ruta } == true
    }

    Scaffold(
        containerColor = MisTurnosColors.Background,
        bottomBar = {
            BarraInferior(
                destinoActual = destinoActual,
                onSeleccionar = { destino ->
                    // Re-selecting the current tab does nothing (keeps an open template).
                    if (destino == destinoActual) return@BarraInferior
                    navController.navigate(destino.ruta) {
                        // Keep one entry per tab and restore each tab's state (visible month, draft…).
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destino.CALENDARIO.ruta,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Destino.CALENDARIO.ruta) { CalendarioRoute() }
            composable(Destino.CHECKLIST.ruta) { ChecklistRoute() }
            navigation(startDestination = RutasPlantillas.LISTA, route = Destino.PLANTILLAS.ruta) {
                composable(RutasPlantillas.LISTA) {
                    PlantillasRoute(onAbrirPlantilla = { id, renombrar ->
                        navController.navigate(RutasPlantillas.detalle(id, renombrar)) { launchSingleTop = true }
                    })
                }
                composable(
                    route = RutasPlantillas.DETALLE,
                    arguments = listOf(
                        navArgument(PlantillaTrabajadoresViewModel.ARG_ID) { type = NavType.LongType },
                        navArgument(PlantillaTrabajadoresViewModel.ARG_RENOMBRAR) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) {
                    // System Back also pops to the list (default NavHost behaviour).
                    PlantillaTrabajadoresRoute(onVolver = {
                        navController.popBackStack(RutasPlantillas.LISTA, inclusive = false)
                    })
                }
            }
        }
    }
}

@Composable
private fun BarraInferior(destinoActual: Destino?, onSeleccionar: (Destino) -> Unit) {
    Column(
        Modifier
            .background(MisTurnosColors.Background)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        HorizontalDivider(thickness = 1.dp, color = MisTurnosColors.TabBarLine)
        Row(
            Modifier
                .fillMaxWidth()
                .height(71.dp)
                .selectableGroup(),
        ) {
            Destino.entries.forEach { destino ->
                ItemBarra(
                    destino = destino,
                    activo = destinoActual == destino,
                    onClick = { onSeleccionar(destino) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ItemBarra(destino: Destino, activo: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (activo) MisTurnosColors.AccentOnBackground else MisTurnosColors.TextMuted
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = activo, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .background(if (activo) MisTurnosColors.SurfaceAccent else Color.Transparent, CircleShape)
                .padding(horizontal = 18.dp, vertical = 4.dp),
        ) {
            Icon(destino.icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(
            text = stringResource(destino.etiqueta),
            style = if (activo) MisTurnosText.tabLabelActive else MisTurnosText.tabLabel,
            color = color,
        )
    }
}
