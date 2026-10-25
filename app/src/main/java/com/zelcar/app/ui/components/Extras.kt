package com.zelcar.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.data.PhotoStore
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Borda tracejada (carro "em compra" na Garagem). */
fun Modifier.dashedBorder(color: Color, width: Dp = 1.dp, dash: Dp = 5.dp, gap: Dp = 4.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))),
    )
}

/** Interruptor 52×32 do design (trilho redondo, bolinha que cresce quando ligado). */
@Composable
fun ZSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val z = Z.c
    val knobX by animateDpAsState(if (checked) 24.dp else 6.dp, label = "knobX")
    val knobY by animateDpAsState(if (checked) 4.dp else 6.dp, label = "knobY")
    val knobSize by animateDpAsState(if (checked) 24.dp else 20.dp, label = "knobSize")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(shape)
            .background(if (checked) z.accent else z.n200)
            .border(1.dp, if (checked) z.accent else z.n400, shape)
            .clickable { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(knobX, knobY)
                .size(knobSize)
                .clip(CircleShape)
                .background(if (checked) z.bg else z.n500),
        )
    }
}

/** Barra de progresso quadrada com segmentos (fração, cor). */
@Composable
fun ProgressBar(
    parts: List<Pair<Float, Color>>,
    height: Dp,
    modifier: Modifier = Modifier,
    track: Color = Z.c.n200,
    bordered: Boolean = false,
) {
    val used = parts.sumOf { it.first.toDouble() }.toFloat().coerceIn(0f, 1f)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(track)
            .then(if (bordered) Modifier.border(1.dp, Z.c.divider) else Modifier),
    ) {
        parts.forEach { (f, c) ->
            if (f > 0f) Box(Modifier.fillMaxHeight().weight(f).background(c))
        }
        val rest = 1f - used
        if (rest > 0.0001f) Box(Modifier.fillMaxHeight().weight(rest))
    }
}

/** Quadradinho hachurado: marcador de foto quando só há a contagem (dados de exemplo). */
@Composable
fun HatchedThumb(size: Dp = 54.dp) {
    val z = Z.c
    Canvas(Modifier.size(size).border(1.dp, z.divider)) {
        val step = 8.dp.toPx()
        val stroke = 2.5.dp.toPx()
        val color = z.accent.copy(alpha = 0.22f)
        clipRect {
            var x = 0f
            while (x < this.size.width + this.size.height) {
                drawLine(color, Offset(x, 0f), Offset(x - this.size.height, this.size.height), stroke)
                x += step
            }
        }
    }
}

/** Foto real do app (miniatura) com "×" opcional para remover. */
@Composable
fun PhotoThumb(path: String, size: Dp = 54.dp, onRemove: (() -> Unit)? = null) {
    val z = Z.c
    val bmp: ImageBitmap? by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { PhotoStore.decodeThumb(path)?.asImageBitmap() }
    }
    Box(Modifier.size(size).border(1.dp, z.divider)) {
        val b = bmp
        if (b != null) {
            Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        }
        if (onRemove != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .background(z.ground.copy(alpha = 0.8f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                ZIcon(ZIcons.Close, size = 14.dp, tint = Color.White)
            }
        }
    }
}

/** Caixa tracejada para listas vazias, com ação opcional. */
@Composable
fun EmptyState(title: String, body: String, cta: String? = null, onCta: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val z = Z.c
    Column(
        modifier
            .fillMaxWidth()
            .dashedBorder(z.divider)
            .padding(horizontal = 16.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = headingStyle(22.sp), color = z.text, textAlign = TextAlign.Center)
        Text(
            body, style = bodyStyle(13.sp), color = z.text.copy(alpha = 0.65f), textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (cta != null && onCta != null) {
            ZButton(cta, onCta, kind = BtnKind.Primary, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
