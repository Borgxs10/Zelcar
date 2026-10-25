package com.zelcar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ícones de traço (viewBox 24, stroke 1.5, pontas redondas) — os mesmos paths do protótipo. */
object ZIcons {
    private fun circle(cx: Double, cy: Double, r: Double): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    val Back = listOf("M20 12H4M10 6l-6 6 6 6")
    val Menu = listOf("M4 7h16M4 12h16M4 17h16")
    val Sync = listOf("M4 12a8 8 0 0 1 13.7-5.7M20 12a8 8 0 0 1-13.7 5.7M18 3v4h-4M6 21v-4h4")
    val Dots = listOf(circle(12.0, 5.0, 1.4), circle(12.0, 12.0, 1.4), circle(12.0, 19.0, 1.4))
    val Car = listOf(
        "M3 11h18M5 11l1.5-4.5A2 2 0 0 1 8.4 5h7.2a2 2 0 0 1 1.9 1.5L19 11M4 11h16v5H4z",
        circle(7.5, 16.5, 1.5), circle(16.5, 16.5, 1.5),
    )
    val TabGarage = listOf("M3 11h18M4 11h16v5H4zM5 11l1.5-4.5h11L19 11")
    val TabFinance = listOf("M3 6h18v13H3zM3 10h18M16 14h2")
    val Bell = listOf("M6 16V10a6 6 0 0 1 12 0v6M4 16h16M10 19a2 2 0 0 0 4 0")
    val TabLogbook = listOf("M5 4h14v16H5zM9 4v16M12 9h4M12 13h4")
    val TabSettings = listOf("M4 8h16M4 16h16")
    val ChecklistTick = listOf("M4 12l4 4 8-9", "M20 6v14H8")
    val Chevron = listOf("M9 6l6 6-6 6")
    val Warn = listOf("M12 4l9 16H3zM12 10v4M12 17h.01")
    val Lock = listOf("M5 11h14v9H5z", "M8 11V8a4 4 0 0 1 8 0v3")
    val Plus = listOf("M12 5v14M5 12h14")
    val Odometer = listOf("M4 17a8 8 0 1 1 16 0", "M12 17l4-5")
    val Qr = listOf("M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3z", "M14 14h3v3h-3zM19 19h2v2h-2M14 20h2")
    val Close = listOf("M6 6l12 12M18 6L6 18")
    val Transfer = listOf("M4 12h16M14 6l6 6-6 6")
    val Pdf = listOf("M14 3H6v18h12V7zM14 3v4h4M12 11v6M9 14l3 3 3-3")
    val Check = listOf("M4 12l5 5L20 6")
}

@Composable
fun ZIcon(
    paths: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
    strokeWidth: Float = 1.5f,
) {
    val parsed = remember(paths) { paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            parsed.forEach { path ->
                drawPath(path, tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
