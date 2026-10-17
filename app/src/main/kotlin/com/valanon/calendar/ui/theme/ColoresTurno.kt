package com.valanon.calendar.ui.theme

import androidx.compose.ui.graphics.Color
import com.valanon.calendar.data.model.TipoTurno

/** Colours of one shift type (tokens.json → `shift`). Shared by the UI and the PDF export. */
data class ColoresTurno(
    val fondo: Color,
    val texto: Color,
    val borde: Color,
    val muestra: Color,
    val punto: Color,
)

val TipoTurno.colores: ColoresTurno
    get() = when (this) {
        TipoTurno.LIBRE -> ColoresTurno(
            fondo = Color(0xFFF2F2EF),
            texto = Color(0xFF5B5B57),
            borde = Color(0xFFE4E4E0),
            muestra = Color(0xFFD9D9D4),
            punto = Color(0xFF9A9A95),
        )
        TipoTurno.MANANA -> ColoresTurno(
            fondo = Color(0xFFFFE9A8),
            texto = Color(0xFF6B4A00),
            borde = Color(0xFFF3D77A),
            muestra = Color(0xFFFFD766),
            punto = Color(0xFFD99E00),
        )
        TipoTurno.TARDE -> ColoresTurno(
            fondo = Color(0xFFCFE4FF),
            texto = Color(0xFF12407A),
            borde = Color(0xFFA9CBF5),
            muestra = Color(0xFF8FBDF5),
            punto = Color(0xFF2F7BD8),
        )
        TipoTurno.NOCHE -> ColoresTurno(
            fondo = Color(0xFF2E2A6B),
            texto = Color(0xFFFFFFFF),
            borde = Color(0xFF2E2A6B),
            muestra = Color(0xFF2E2A6B),
            punto = Color(0xFFB9B4FF),
        )
        TipoTurno.VACACIONES -> ColoresTurno(
            fondo = Color(0xFFCDEFD5),
            texto = Color(0xFF145229),
            borde = Color(0xFFA6DDB4),
            muestra = Color(0xFF8EDBA4),
            punto = Color(0xFF2E9E55),
        )
    }
