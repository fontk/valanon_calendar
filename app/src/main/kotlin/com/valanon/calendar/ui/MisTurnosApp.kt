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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.valanon.calendar.R
import com.valanon.calendar.ui.calendario.CalendarioRoute
import com.valanon.calendar.ui.checklist.ChecklistRoute
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.MisTurnosColors
import com.valanon.calendar.ui.theme.MisTurnosText

/** The two bottom-bar destinations. [ruta] is the Navigation Compose route. */
enum class Destino(val ruta: String, @param:StringRes val etiqueta: Int, val icono: ImageVector) {
    CALENDARIO("calendario", R.string.tab_calendario, MisTurnosIcons.Calendario),
    CHECKLIST("checklist", R.string.tab_checklist, MisTurnosIcons.Checklist),
}

@Composable
fun MisTurnosApp(navController: NavHostController = rememberNavController()) {
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    Scaffold(
        containerColor = MisTurnosColors.Background,
        bottomBar = {
            BarraInferior(
                rutaActual = rutaActual,
                onSeleccionar = { destino ->
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
        }
    }
}

@Composable
private fun BarraInferior(rutaActual: String?, onSeleccionar: (Destino) -> Unit) {
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
                    activo = rutaActual == destino.ruta,
                    onClick = { onSeleccionar(destino) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ItemBarra(destino: Destino, activo: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (activo) MisTurnosColors.Accent else MisTurnosColors.TextMuted
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
