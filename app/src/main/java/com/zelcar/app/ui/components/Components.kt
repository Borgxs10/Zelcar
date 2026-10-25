package com.zelcar.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import com.zelcar.app.ui.theme.kickerStyle

// ─────────────────────────────────────────────────────────────
// Texto
// ─────────────────────────────────────────────────────────────

/** Rótulo de seção (10sp caixa-alta, cor accent-700). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, top: Dp = 24.dp, bottom: Dp = 4.dp) {
    Text(
        text.uppercase(),
        style = kickerStyle(),
        color = Z.c.a700,
        modifier = modifier.padding(top = top, bottom = bottom),
    )
}

/** Rótulo pequeno cinza (ex.: "KM ATUAL"). */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = Z.c.text.copy(alpha = 0.5f), tracking: Float = 0.1f) {
    Text(text.uppercase(), style = kickerStyle().copy(letterSpacing = tracking.em), color = color, modifier = modifier)
}

@Composable
fun ScreenTitle(text: String, size: Int = 34, modifier: Modifier = Modifier) {
    Text(text, style = headingStyle(size.sp, -0.02f), color = Z.c.text, modifier = modifier)
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: Float = 12f, alpha: Float = 0.6f) {
    Text(text, style = bodyStyle(size.sp), color = Z.c.text.copy(alpha = alpha), modifier = modifier)
}

// ─────────────────────────────────────────────────────────────
// Moldura blueprint (borda fina + marcas de registro nos cantos)
// ─────────────────────────────────────────────────────────────
@Composable
fun Blueprint(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val z = Z.c
    val mark = z.text.copy(alpha = 0.55f)
    Box(
        modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                val arm = 5.5.dp.toPx()
                val w = 1.dp.toPx()
                listOf(
                    Offset(0f, 0f), Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height),
                ).forEach { p ->
                    drawLine(mark, Offset(p.x - arm, p.y), Offset(p.x + arm, p.y), w)
                    drawLine(mark, Offset(p.x, p.y - arm), Offset(p.x, p.y + arm), w)
                }
            }
            .border(1.dp, z.divider)
            .padding(padding),
        content = content,
    )
}

// ─────────────────────────────────────────────────────────────
// Tags e chips
// ─────────────────────────────────────────────────────────────
enum class TagKind { Accent, Neutral, Outline, Ok, Warn, Bad, Info }

@Composable
fun ZTag(text: String, kind: TagKind = TagKind.Neutral, modifier: Modifier = Modifier) {
    val z = Z.c
    val n100 = if (z.dark) Color(0xFF2B2B2D) else Color(0xFFF5F5F8)
    val n800 = if (z.dark) Color(0xFFD4D4D7) else Color(0xFF424244)
    val (bg, fg, border) = when (kind) {
        TagKind.Accent -> Triple(z.a100, z.a800, null)
        TagKind.Neutral -> Triple(n100, n800, null)
        TagKind.Outline -> Triple(Color.Transparent, z.accent, z.accent)
        TagKind.Ok -> Triple(z.ok.copy(alpha = 0.14f), z.ok, z.ok)
        TagKind.Warn -> Triple(z.warn.copy(alpha = 0.16f), z.warn, z.warn)
        TagKind.Bad -> Triple(z.bad.copy(alpha = 0.14f), z.bad, z.bad)
        TagKind.Info -> Triple(z.info.copy(alpha = 0.12f), z.info, z.info)
    }
    Text(
        text,
        style = bodyStyle(11.sp).copy(letterSpacing = 0.02.em),
        color = fg,
        maxLines = 1,
        modifier = modifier
            .background(bg)
            .then(if (border != null) Modifier.border(1.dp, border) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Chip de filtro (Todos / Meus carros / …). */
@Composable
fun ZChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val z = Z.c
    Text(
        label,
        style = bodyStyle(11.sp).copy(letterSpacing = 0.02.em),
        color = if (selected) z.bg else z.a800,
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp)
            .border(1.dp, if (selected) z.accent else z.a400)
            .background(if (selected) z.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    )
}

/** Chip de categoria (formulário de gasto). */
@Composable
fun ZCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val z = Z.c
    Text(
        label,
        style = bodyStyle(13.sp),
        color = if (selected) z.bg else z.text,
        modifier = Modifier
            .border(1.dp, if (selected) z.accent else z.divider)
            .background(if (selected) z.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
    )
}

// ─────────────────────────────────────────────────────────────
// Botões, segmentado, checkbox, linha
// ─────────────────────────────────────────────────────────────
enum class BtnKind { Primary, Secondary, Ghost }

@Composable
fun ZButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Secondary,
    enabled: Boolean = true,
    colorOverride: Pair<Color, Color>? = null, // (fundo, texto) — ex.: paywall
) {
    val z = Z.c
    val bg = colorOverride?.first ?: if (kind == BtnKind.Primary) z.accent else Color.Transparent
    val fg = colorOverride?.second ?: when (kind) {
        BtnKind.Primary -> z.bg
        BtnKind.Secondary -> z.text
        BtnKind.Ghost -> z.accent
    }
    val border = when {
        colorOverride != null -> Color.Transparent
        kind == BtnKind.Primary -> z.accent
        kind == BtnKind.Secondary -> z.divider
        else -> Color.Transparent
    }
    Box(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(bg)
            .border(1.dp, border)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = headingStyle(16.sp, 0.01f),
            color = if (enabled) fg else fg.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
        )
    }
}

/** Controle segmentado de largura total (Mês / Trimestre / Ano). */
@Composable
fun SegControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val z = Z.c
    Row(modifier.fillMaxWidth().border(1.dp, z.divider)) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .weight(1f)
                    .background(if (i == selected) z.accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = bodyStyle(13.sp), color = if (i == selected) z.bg else z.text)
            }
        }
    }
}

/** Caixa quadrada de marcação (checklist, transferência, relatório). */
@Composable
fun ZCheckBox(mark: String, on: Boolean, issue: Boolean = false, size: Dp = 22.dp) {
    val z = Z.c
    val bg = if (on) z.accent else Color.Transparent
    val border = when {
        on -> z.accent
        issue -> z.a700
        else -> z.divider
    }
    val fg = if (on) z.bg else z.a700
    Box(
        Modifier.size(size).background(bg).border(1.5.dp, border),
        contentAlignment = Alignment.Center,
    ) {
        Text(mark, style = bodyStyle(13.sp), color = fg)
    }
}

/** Linha de lista com divisor inferior (mín. 48dp de toque). */
@Composable
fun ZRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val z = Z.c
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                drawLine(z.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Z.c.divider))
}

// ─────────────────────────────────────────────────────────────
// Campos
// ─────────────────────────────────────────────────────────────
@Composable
fun ZField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val z = Z.c
    Column(modifier) {
        Text(label, style = bodyStyle(12.sp), color = z.text.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = bodyStyle(14.sp).copy(color = z.text),
            cursorBrush = SolidColor(z.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .background(z.surface)
                        .border(1.dp, z.divider)
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = bodyStyle(14.sp), color = z.text.copy(alpha = 0.4f))
                    }
                    inner()
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Imagens "hero" (ilustrações blueprint com degradê para o fundo)
// ─────────────────────────────────────────────────────────────

/** Faz o filho ocupar [amount] a menos de altura, para o título "subir" sobre a imagem. */
fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    val h = (p.height - amount.roundToPx()).coerceAtLeast(0)
    layout(p.width, h) { p.place(0, 0) }
}

@Composable
fun Hero(
    @DrawableRes res: Int,
    aspect: Float,
    imageBg: Color,
    fadeTo: Color = Z.c.bg,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .background(imageBg)
            .pullUp(26.dp),
    ) {
        Image(
            painterResource(res), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.58f to Color.Transparent, 1f to fadeTo),
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Folha inferior: cabeçalho comum
// ─────────────────────────────────────────────────────────────
@Composable
fun SheetHeader(kicker: String, title: String, subtitle: String? = null, titleSize: Int = 30) {
    Column {
        Text(kicker.uppercase(), style = kickerStyle(), color = Z.c.a700)
        Text(title, style = headingStyle(titleSize.sp, -0.02f), color = Z.c.text, modifier = Modifier.padding(top = 6.dp))
        if (subtitle != null) Muted(subtitle, size = 13.5f, alpha = 0.7f, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Caixa de nota/hint com borda fina. */
@Composable
fun HintBox(text: String, modifier: Modifier = Modifier, alpha: Float = 0.85f) {
    Text(
        text,
        style = bodyStyle(12.5.sp),
        color = Z.c.text.copy(alpha = alpha),
        modifier = modifier.fillMaxWidth().border(1.dp, Z.c.divider).padding(14.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrapRow(
    modifier: Modifier = Modifier,
    gap: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
        content()
    }
}

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HGap(w: Dp) = Spacer(Modifier.width(w))

/** Valor grande em Barlow Condensed (ex.: "R$ 1.234,00"). */
@Composable
fun BigNumber(text: String, size: Int, color: Color = Z.c.text, modifier: Modifier = Modifier) {
    Text(text, style = headingStyle(size.sp, 0f, 1f), color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Clip)
}

@Composable
fun BorderBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.border(BorderStroke(1.dp, Z.c.divider)), content = content)
}

@Composable
fun BoldBody(text: String, size: Float = 15f, modifier: Modifier = Modifier, color: Color = Z.c.text) {
    Text(text, style = bodyStyle(size.sp, FontWeight.Normal), color = color, modifier = modifier)
}
