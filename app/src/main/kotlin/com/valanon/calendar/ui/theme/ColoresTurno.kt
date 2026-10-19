package com.valanon.calendar.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.valanon.calendar.R
import com.valanon.calendar.data.local.entity.TipoTurno

/** Colours of one shift type: cell background, text, border, picker swatch and dot (Turnos grid). */
@Immutable
data class ColoresTurno(
    val fondo: Color,
    val texto: Color,
    val borde: Color,
    val muestra: Color,
    val punto: Color,
    /** Dot inside the type's own legend chip; differs from [punto] only when that would vanish on [fondo]. */
    val puntoChip: Color = punto,
)

/** One of the 16 palette entries a shift type can use (spec 3c.3, tokens.json → `shiftPalette`). */
@Immutable
data class EntradaPaleta(
    /** What `tipos_turno.color` stores. */
    val clave: String,
    /** Colour name, also the accessible name of its button in the editor. */
    @StringRes val nombre: Int,
    val claro: ColoresTurno,
    val oscuro: ColoresTurno,
)

/**
 * The shift palette. It lives in code, not in the database: `tipos_turno.color` keeps the key.
 * Light values are the mockup's (`Ajustes.dc.html`) except five dots darkened to reach 3:1 on
 * white (gris, amarillo, ámbar, celeste, lima; approved by the user, tokens.json updated). Dark values follow one rule —
 * background = the dot over the dark background, text = the light pastel, border = a stronger
 * mix — except "indigo", which is already dark and only gets the lighter border DESIGN.md asks
 * for (#6E68C9) and a visible dot.
 */
object PaletaTurnos {

    val entradas: List<EntradaPaleta> = listOf(
        // The five defaults keep the swatch of tokens.json → shift (selector squares).
        entrada("gris", R.string.color_gris, 0xFFF2F2EF, 0xFF5B5B57, 0xFFE4E4E0, 0xFF92928E, muestra = 0xFFD9D9D4),
        entrada("amarillo", R.string.color_amarillo, 0xFFFFE9A8, 0xFF6B4A00, 0xFFF3D77A, 0xFFBD8900, muestra = 0xFFFFD766),
        entrada("ambar", R.string.color_ambar, 0xFFFFDDB0, 0xFF7A3E00, 0xFFF5C98A, 0xFFDA7800),
        entrada("naranja", R.string.color_naranja, 0xFFFFD3C2, 0xFF8A2E1F, 0xFFF5B9A3, 0xFFE05A2B),
        entrada("rojo", R.string.color_rojo, 0xFFFFC9CF, 0xFF8B1A2B, 0xFFF3A9B3, 0xFFD63A4F),
        entrada("rosa", R.string.color_rosa, 0xFFFFD1E8, 0xFF8A1E57, 0xFFF3B0D4, 0xFFD63A8F),
        entrada("fucsia", R.string.color_fucsia, 0xFFF3CCFF, 0xFF6B1A8A, 0xFFE2AEF5, 0xFFB23AD6),
        entrada("violeta", R.string.color_violeta, 0xFFEDE9FF, 0xFF4B43B8, 0xFFD4CCFA, 0xFF7B6CF0),
        EntradaPaleta(
            clave = "indigo",
            nombre = R.string.color_indigo,
            // The legend chip keeps the lilac dot of tokens.json → shift.noche (#B9B4FF).
            claro = ColoresTurno(
                Color(0xFF2E2A6B), Color.White, Color(0xFF2E2A6B), Color(0xFF2E2A6B), Color(0xFF2E2A6B),
                puntoChip = Color(0xFFB9B4FF),
            ),
            oscuro = ColoresTurno(Color(0xFF2E2A6B), Color.White, Color(0xFF6E68C9), Color(0xFF6E68C9), Color(0xFFB9B4FF)),
        ),
        entrada("azul", R.string.color_azul, 0xFFCFE4FF, 0xFF12407A, 0xFFA9CBF5, 0xFF2F7BD8, muestra = 0xFF8FBDF5),
        entrada("celeste", R.string.color_celeste, 0xFFCFF3FF, 0xFF0B4F63, 0xFFA4E3F5, 0xFF199EC3),
        entrada("turquesa", R.string.color_turquesa, 0xFFCFF5EC, 0xFF0C5A4A, 0xFFA6E6D8, 0xFF17A48A),
        entrada("verde", R.string.color_verde, 0xFFCDEFD5, 0xFF145229, 0xFFA6DDB4, 0xFF2E9E55, muestra = 0xFF8EDBA4),
        entrada("lima", R.string.color_lima, 0xFFE6F5B8, 0xFF3F5A00, 0xFFCFE88F, 0xFF779F16),
        entrada("marron", R.string.color_marron, 0xFFEBDCCB, 0xFF5A3A1E, 0xFFD9C3A8, 0xFF9C6B3F),
        entrada("pizarra", R.string.color_pizarra, 0xFFD9DEE5, 0xFF2F3B4D, 0xFFBFC7D2, 0xFF6B7A90),
    )

    private val porClave: Map<String, EntradaPaleta> = entradas.associateBy { it.clave }

    /** The entry for [clave]; unknown keys (e.g. from a newer backup) fall back to `gris`. */
    operator fun get(clave: String): EntradaPaleta = porClave[clave] ?: entradas.first()

    /** First colour no type uses yet, preselected when creating a type (colours may repeat). */
    fun primeraLibre(usadas: Collection<String>): String =
        entradas.firstOrNull { it.clave !in usadas }?.clave ?: entradas.first().clave
}

private val FONDO_OSCURO = Color(0xFF141413)

private fun entrada(
    clave: String,
    @StringRes nombre: Int,
    bg: Long,
    fg: Long,
    border: Long,
    dot: Long,
    muestra: Long = border,
): EntradaPaleta {
    val punto = Color(dot)
    return EntradaPaleta(
        clave = clave,
        nombre = nombre,
        claro = ColoresTurno(Color(bg), Color(fg), Color(border), Color(muestra), punto),
        oscuro = ColoresTurno(
            fondo = lerp(FONDO_OSCURO, punto, 0.30f),
            texto = Color(bg),
            borde = lerp(FONDO_OSCURO, punto, 0.55f),
            muestra = punto,
            punto = punto,
        ),
    )
}

/** Colours of this type in the current theme. */
val TipoTurno.colores: ColoresTurno
    @Composable @ReadOnlyComposable
    get() = PaletaTurnos[color].let { if (LocalPaleta.current.oscuro) it.oscuro else it.claro }

/** Colours of the light design. Used outside composition, e.g. by the PDF, which is always light. */
fun TipoTurno.coloresClaros(): ColoresTurno = PaletaTurnos[color].claro
