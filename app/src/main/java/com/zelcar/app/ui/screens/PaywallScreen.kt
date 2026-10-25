package com.zelcar.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zelcar.app.R
import com.zelcar.app.billing.BillingManager
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.PlanPick
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.components.pullUp
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import com.zelcar.app.ui.theme.kickerStyle

private val Ground = Color(0xFF081528)
private val Soft = Color(0xFF9FCBFA)

@Composable
fun PaywallScreen(state: AppState, vm: ZelcarViewModel) {
    val pro = state.isPro
    val prices by vm.prices.collectAsStateWithLifecycle()
    val monthly = prices[BillingManager.MONTHLY_ID] ?: "R$ 14,90"
    val annual = prices[BillingManager.ANNUAL_ID] ?: "R$ 149"
    val pick = state.ui.planPick

    Box(
        Modifier.fillMaxSize().background(Ground).drawBehind {
            drawRect(Brush.radialGradient(listOf(Color(0xFF3B8FF0).copy(alpha = 0.40f), Color.Transparent),
                center = Offset(size.width * 0.9f, 0f), radius = size.width * 1.1f))
            drawRect(Brush.radialGradient(listOf(Color(0xFF2F6FD0).copy(alpha = 0.32f), Color.Transparent),
                center = Offset(0f, size.height), radius = size.width))
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().aspectRatio(1376f / 768f).background(Color(0xFF0A1C2E)).pullUp(26.dp)) {
                Image(painterResource(R.drawable.hero_paywall), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.58f to Color.Transparent, 1f to Ground)))
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                Text("ASSINATURA", style = kickerStyle(), color = Soft)
                Text("Zelcar Pro", style = headingStyle(38.sp, -0.02f), color = Color(0xFFF2F2F3), modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                Text(
                    if (pro) "Sua assinatura está ativa. Gerencie na Play Store."
                    else "O básico é de graça para sempre. O Pro é para quem cuida de mais de um carro ou está comprando um.",
                    style = bodyStyle(14.sp), color = Color(0xFFDBE8F6),
                )
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Perk("Vários carros na garagem", "O plano grátis cobre dois.")
                    Perk("Checklist de compra completo", "23 itens, fotos por item e comparação FIPE.")
                    Perk("Relatório em PDF", "Para venda, prestação de contas ou oficina.")
                    Perk("Métricas avançadas", "Custo por km, projeção anual, comparativo entre carros.")
                }
                Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanCard("Mensal", monthly, pick == PlanPick.Monthly, Modifier.weight(1f)) { vm.setPlanPick(PlanPick.Monthly) }
                    PlanCard("Anual · 2 meses grátis", annual, pick == PlanPick.Annual, Modifier.weight(1f)) { vm.setPlanPick(PlanPick.Annual) }
                }
                ZButton(
                    text = if (pro) "Assinatura ativa"
                    else if (pick == PlanPick.Annual) "Assinar por $annual/ano" else "Assinar por $monthly/mês",
                    onClick = { vm.subscribe() },
                    modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
                    colorOverride = Soft to Color(0xFF1D2D3D),
                )
                Text(
                    if (pro) "Voltar" else "Continuar no plano grátis",
                    style = bodyStyle(13.sp), color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { vm.back() }.padding(vertical = 14.dp),
                )
                Text(
                    "Cobrança pelo Google Play. Gerencie em Play Store › Assinaturas.",
                    style = bodyStyle(10.5.sp), color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Perk(title: String, sub: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ZIcon(ZIcons.Check, tint = Soft)
        Column {
            Text(title, style = bodyStyle(15.sp), color = Color.White)
            Text(sub, style = bodyStyle(12.sp), color = Color(0xFFC9DCF0))
        }
    }
}

@Composable
private fun PlanCard(kicker: String, price: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val line = if (selected) Color(0xFF9FC8F1) else Color.White.copy(alpha = 0.3f)
    Column(
        modifier
            .background(if (selected) Color(0xFF9FC8F1).copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, line)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(kicker, style = bodyStyle(11.sp), color = if (selected) Color(0xFF9FC8F1) else Color.White.copy(alpha = 0.75f))
        Text(price, style = headingStyle(26.sp, 0f), color = Color.White)
    }
}
