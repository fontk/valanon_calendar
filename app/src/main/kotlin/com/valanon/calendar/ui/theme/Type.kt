package com.valanon.calendar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.valanon.calendar.R

/** Manrope variable font (res/font/manrope.ttf, SIL OFL 1.1) instanced at the weights the design uses. */
@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(
    listOf(500, 600, 700, 800).map { weight ->
        Font(
            resId = R.font.manrope,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

/** Text styles from tokens.json → `typography.styles`. */
object MisTurnosText {
    val overline = TextStyle(
        fontFamily = Manrope,
        fontSize = 12.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.08.em,
    )
    val title = TextStyle(fontFamily = Manrope, fontSize = 22.sp, fontWeight = FontWeight.W800, letterSpacing = (-0.01).em)
    val sheetTitle = TextStyle(fontFamily = Manrope, fontSize = 20.sp, fontWeight = FontWeight.W800)
    val body = TextStyle(fontFamily = Manrope, fontSize = 16.sp, fontWeight = FontWeight.W600)
    val button = TextStyle(fontFamily = Manrope, fontSize = 16.sp, fontWeight = FontWeight.W700)
    val buttonSmall = TextStyle(fontFamily = Manrope, fontSize = 15.sp, fontWeight = FontWeight.W700)
    val dayNumber = TextStyle(fontFamily = Manrope, fontSize = 15.sp, fontWeight = FontWeight.W800)
    val dayTag = TextStyle(fontFamily = Manrope, fontSize = 10.sp, fontWeight = FontWeight.W700, letterSpacing = 0.04.em)
    val chip = TextStyle(fontFamily = Manrope, fontSize = 12.sp, fontWeight = FontWeight.W700)
    val caption = TextStyle(fontFamily = Manrope, fontSize = 13.sp, fontWeight = FontWeight.W700)
    val tabLabel = TextStyle(fontFamily = Manrope, fontSize = 12.sp, fontWeight = FontWeight.W700)
    val tabLabelActive = TextStyle(fontFamily = Manrope, fontSize = 12.sp, fontWeight = FontWeight.W800)
    val emptyTitle = TextStyle(fontFamily = Manrope, fontSize = 15.sp, fontWeight = FontWeight.W700)
    val emptyHelp = TextStyle(fontFamily = Manrope, fontSize = 13.sp, fontWeight = FontWeight.W500)
}

/** Material 3 typography mapped onto Manrope so default components also use it. */
val Typography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Manrope),
        displayMedium = base.displayMedium.copy(fontFamily = Manrope),
        displaySmall = base.displaySmall.copy(fontFamily = Manrope),
        headlineLarge = base.headlineLarge.copy(fontFamily = Manrope),
        headlineMedium = base.headlineMedium.copy(fontFamily = Manrope),
        headlineSmall = base.headlineSmall.copy(fontFamily = Manrope),
        titleLarge = MisTurnosText.title,
        titleMedium = base.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.W700),
        titleSmall = base.titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.W700),
        bodyLarge = MisTurnosText.body,
        bodyMedium = base.bodyMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.W600),
        bodySmall = base.bodySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.W500),
        labelLarge = MisTurnosText.buttonSmall,
        labelMedium = MisTurnosText.chip,
        labelSmall = base.labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.W700),
    )
}
