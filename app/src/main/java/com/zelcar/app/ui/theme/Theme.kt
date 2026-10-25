package com.zelcar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zelcar.app.R

/**
 * Tokens do design system Zelcar ("Industry"), nos dois temas.
 * Valores copiados do protótipo (.zlight / .zdark).
 */
@Immutable
class ZColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val text: Color,
    val divider: Color,
    val accent: Color,
    val a100: Color, val a200: Color, val a300: Color, val a400: Color, val a500: Color,
    val a600: Color, val a700: Color, val a800: Color, val a900: Color,
    val n200: Color, val n300: Color, val n400: Color, val n500: Color,
    val ok: Color, val warn: Color, val bad: Color, val info: Color,
    /** Fundo "blueprint" escuro usado no scanner e nos toasts (#1D2D3D). */
    val ground: Color = Color(0xFF1D2D3D),
    /** Fundo do app (sem o brilho radial). */
    val canvas: Color,
)

val LightZ = ZColors(
    dark = false,
    bg = Color(0xFFF5F8FC), surface = Color(0xFFE8EFF8), text = Color(0xFF10202F),
    divider = Color(0x2E2F78C8), accent = Color(0xFF3B82D6),
    a100 = Color(0xFFE3EFFC), a200 = Color(0xFFCBE1F9), a300 = Color(0xFF9FC8F1), a400 = Color(0xFF6AA8E6),
    a500 = Color(0xFF3B82D6), a600 = Color(0xFF2F6FBD), a700 = Color(0xFF1F5A9E), a800 = Color(0xFF174577),
    a900 = Color(0xFF10304F),
    n200 = Color(0xFFE7E7EA), n300 = Color(0xFFD4D4D7), n400 = Color(0xFFB7B7BA), n500 = Color(0xFF98989B),
    ok = Color(0xFF1C8A4E), warn = Color(0xFF9A6200), bad = Color(0xFFC0392B), info = Color(0xFF1F5A9E),
    canvas = Color(0xFFF5F8FC),
)

val DarkZ = ZColors(
    dark = true,
    bg = Color(0xFF0D1F33), surface = Color(0xFF17314D), text = Color(0xFFF1F5F9),
    divider = Color(0x429CC9F5), accent = Color(0xFF4F9AEB),
    a100 = Color(0xFF13304F), a200 = Color(0xFF1A4068), a300 = Color(0xFF26578A), a400 = Color(0xFF3A7CC0),
    a500 = Color(0xFF4F9AEB), a600 = Color(0xFF74B4F5), a700 = Color(0xFF9FCBFA), a800 = Color(0xFFC4E0FD),
    a900 = Color(0xFFE3F0FE),
    n200 = Color(0xFF38383A), n300 = Color(0xFF4A4A4D), n400 = Color(0xFF5D5D60), n500 = Color(0xFF7A7A7D),
    ok = Color(0xFF4FD994), warn = Color(0xFFFFC94D), bad = Color(0xFFFF8577), info = Color(0xFF8FC4FB),
    canvas = Color(0xFF08121F),
)

val LocalZ = staticCompositionLocalOf { LightZ }

/** Acesso curto: `Z.accent`, `Z.divider`… */
object Z {
    val c: ZColors @Composable @ReadOnlyComposable get() = LocalZ.current
}

object ZFont {
    val body = FontFamily(
        Font(R.font.barlow_regular, FontWeight.Normal),
        Font(R.font.barlow_medium, FontWeight.Medium),
        Font(R.font.barlow_semibold, FontWeight.SemiBold),
    )
    val heading = FontFamily(
        Font(R.font.barlow_condensed_medium, FontWeight.Medium),
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    )
}

/** Títulos usam Barlow Condensed 600; corpo usa Barlow. */
fun headingStyle(size: TextUnit, tracking: Float = -0.015f, lineHeight: Float = 1.12f) = TextStyle(
    fontFamily = ZFont.heading,
    fontWeight = FontWeight.SemiBold,
    fontSize = size,
    letterSpacing = tracking.em,
    lineHeight = size * lineHeight,
)

fun bodyStyle(size: TextUnit = 15.sp, weight: FontWeight = FontWeight.Normal, lineHeight: TextUnit = TextUnit.Unspecified) =
    TextStyle(fontFamily = ZFont.body, fontWeight = weight, fontSize = size, lineHeight = lineHeight)

/** Rótulo de seção: 10sp, caixa-alta, espaçamento largo. */
fun kickerStyle() = TextStyle(
    fontFamily = ZFont.body, fontSize = 10.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.Normal,
)

private fun scheme(z: ZColors): ColorScheme {
    val base = if (z.dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = z.accent, onPrimary = z.bg,
        primaryContainer = z.a200, onPrimaryContainer = z.a900,
        secondaryContainer = z.a200, onSecondaryContainer = z.a900,
        background = z.bg, onBackground = z.text,
        surface = z.bg, onSurface = z.text,
        surfaceVariant = z.surface, onSurfaceVariant = z.text.copy(alpha = 0.7f),
        surfaceContainer = z.surface, surfaceContainerHigh = z.surface, surfaceContainerHighest = z.surface,
        outline = z.divider, outlineVariant = z.divider,
        inverseSurface = z.ground, inverseOnSurface = Color(0xFFF2F2F3), inversePrimary = z.a300,
        error = z.bad,
    )
}

@Composable
fun ZelcarTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val z = if (dark) DarkZ else LightZ
    CompositionLocalProvider(LocalZ provides z) {
        MaterialTheme(colorScheme = scheme(z), content = content)
    }
}
