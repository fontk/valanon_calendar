package com.valanon.calendar.ui.plantillas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.valanon.calendar.R
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.ui.icons.MisTurnosIcons
import com.valanon.calendar.ui.theme.MisTurnosColors

/** UI bits of each template type. Add a branch here when a new [TipoPlantilla] appears. */
val TipoPlantilla.icono: ImageVector
    get() = when (this) {
        TipoPlantilla.TRABAJADORES -> MisTurnosIcons.Trabajadores
    }

val TipoPlantilla.descripcion: String
    @Composable get() = when (this) {
        TipoPlantilla.TRABAJADORES -> stringResource(R.string.tipo_trabajadores_descripcion)
    }

/** 44 dp badge (radius 12, `surfaceAccent`) with the type icon. */
@Composable
fun InsigniaTipo(tipo: TipoPlantilla) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MisTurnosColors.SurfaceAccent, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(tipo.icono, contentDescription = null, tint = MisTurnosColors.AccentOnBackground, modifier = Modifier.size(22.dp))
    }
}
