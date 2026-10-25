package com.zelcar.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.data.CarEntity
import com.zelcar.app.domain.brl
import com.zelcar.app.domain.brl0
import com.zelcar.app.domain.costPerKm
import com.zelcar.app.domain.monthTotal
import com.zelcar.app.domain.nkm
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.CarMode
import com.zelcar.app.ui.GarageFilter
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.EmptyState
import com.zelcar.app.ui.components.Kicker
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.ScreenTitle
import com.zelcar.app.ui.components.TagKind
import com.zelcar.app.ui.components.WrapRow
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.ZChip
import com.zelcar.app.ui.components.ZTag
import com.zelcar.app.ui.components.dashedBorder
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GarageScreen(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val ui = state.ui
    val all = state.data.cars
    val active = state.activeCar
    val cars = all.filter {
        when (ui.garageFilter) {
            GarageFilter.All -> true
            GarageFilter.Mine -> !it.buying
            GarageFilter.Buying -> it.buying
        }
    }
    ScreenScroll {
        Body {
            ScreenTitle("Sua garagem")
            Text(
                buildAnnotatedString {
                    append("${all.size} ${if (all.size == 1) "veículo" else "veículos"} · plano ")
                    withStyle(SpanStyle(color = z.a700)) { append(if (state.isPro) "Pro" else "Grátis") }
                    append(" · " + if (ui.syncing) "sincronizando…" else "sincronizado agora")
                },
                style = bodyStyle(12.sp), color = z.text.copy(alpha = 0.6f), modifier = Modifier.padding(top = 2.dp),
            )
            WrapRow(Modifier.padding(top = 16.dp)) {
                GarageFilter.entries.forEach { f ->
                    ZChip(f.label, ui.garageFilter == f, { vm.setGarageFilter(f) })
                }
            }
            Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (cars.isEmpty()) {
                    EmptyState(
                        "Sua garagem está vazia",
                        "Adicione seu primeiro carro ou explore o app com dados de exemplo.",
                        "Adicionar carro", { vm.openAddCar(CarMode.Owned) },
                    )
                    ZButton("Carregar dados de exemplo", { vm.loadDemo() }, kind = BtnKind.Ghost)
                }
                cars.forEach { c -> CarCard(state, vm, c, isActive = !c.buying && c.id == active?.id) }
            }
            if (cars.isNotEmpty()) {
                Muted(
                    "Toque e segure uma linha para transferir ou excluir.",
                    size = 11f, alpha = 0.55f, modifier = Modifier.padding(top = 22.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarCard(state: AppState, vm: ZelcarViewModel, c: CarEntity, isActive: Boolean) {
    val z = Z.c
    val pend = pendingOf(state, c).size
    val checked = state.data.checks.count { it.carId == c.id && it.state != "unset" }
    val border = if (c.buying) Modifier.dashedBorder(z.a400) else Modifier.border(1.dp, z.divider)
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (isActive) z.a100 else Color.Transparent)
            .then(border)
            .combinedClickable(
                onClick = { if (c.buying) vm.openBuyingChecklist() else vm.openCar(c) },
                onLongClick = { vm.askCarActions(c) },
            )
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Kicker(
                    if (c.buying) "Em compra" else if (c.id == state.mainCarId) "Principal" else "Na garagem",
                    color = z.a700, tracking = 0.12f,
                )
                Text(c.name, style = headingStyle(24.sp, -0.015f, 1.1f), color = z.text, modifier = Modifier.padding(top = 4.dp))
                Muted(
                    if (c.buying) "${c.year} · checklist $checked/23" else "${c.year} · ${nkm(c.km)} km · ${c.plate}",
                    size = 12.5f, alpha = 0.65f,
                )
            }
            when {
                c.buying -> ZTag("Checklist", TagKind.Info)
                pend > 0 -> ZTag(if (pend == 1) "1 pendência" else "$pend pendências", TagKind.Warn)
                else -> ZTag("Em dia", TagKind.Ok)
            }
        }
        if (isActive) {
            val exp = state.data.expenses.filter { it.carId == c.id }
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Stat("Km atual", nkm(c.km))
                Stat("Custo / km", brl(costPerKm(exp, c.km, state.today)))
                Stat("Mês", brl0(monthTotal(exp, state.today.year, state.today.monthValue)))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Kicker(label)
        Text(value, style = headingStyle(22.sp, 0f), color = Z.c.text)
    }
}
