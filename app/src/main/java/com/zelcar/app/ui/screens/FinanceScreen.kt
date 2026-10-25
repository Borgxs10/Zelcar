package com.zelcar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.domain.MONTH_FULL
import com.zelcar.app.domain.Period
import com.zelcar.app.domain.brl
import com.zelcar.app.domain.dmy
import com.zelcar.app.domain.lastSixMonths
import com.zelcar.app.domain.nkm
import com.zelcar.app.domain.periodExpenses
import com.zelcar.app.domain.toDate
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.CarMode
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.Blueprint
import com.zelcar.app.ui.components.EmptyState
import com.zelcar.app.ui.components.Kicker
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.ScreenTitle
import com.zelcar.app.ui.components.SectionLabel
import com.zelcar.app.ui.components.SegControl
import com.zelcar.app.ui.components.ZRow
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle

@Composable
fun FinanceScreen(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val car = state.activeCar
    if (car == null) {
        ScreenScroll { Body { EmptyState("Sem carro na garagem", "Adicione um carro para lançar gastos.", "Adicionar carro", { vm.openAddCar(CarMode.Owned) }) } }
        return
    }
    val now = state.today
    val period = state.ui.period
    val exp = periodExpenses(state.carExpenses, period, now)
    val total = exp.sumOf { it.value }
    val byCat = exp.groupBy { it.cat }.mapValues { (_, l) -> l.sumOf { it.value } }
    val catMax = (byCat.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val bars = lastSixMonths(state.carExpenses, now)
    val barMax = bars.maxOf { it.value }.coerceAtLeast(1.0)
    val label = when (period) {
        Period.Month -> "${MONTH_FULL[now.monthValue - 1]} de ${now.year}"
        Period.Quarter -> "últimos 3 meses"
        Period.Year -> "ano de ${now.year}"
    }
    ScreenScroll {
        ScreenHero(HeroKind.Finance)
        Body(top = 0) {
            ScreenTitle("Gastos")
            Muted("${car.name} · $label", modifier = Modifier.padding(top = 2.dp))
            SegControl(
                Period.entries.map { it.label }, Period.entries.indexOf(period),
                { vm.setPeriod(Period.entries[it]) }, Modifier.padding(top = 16.dp),
            )
            Blueprint(Modifier.padding(top = 20.dp).fillMaxWidth(), padding = 16.dp) {
                Column {
                    Kicker("Total · $label", tracking = 0.12f)
                    Text(brl(total), style = headingStyle(44.sp, 0f, 1f), color = z.text)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 18.dp).height(74.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom,
                    ) {
                        bars.forEach { b ->
                            val frac = (b.value / barMax).toFloat().coerceAtLeast(0.03f)
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                                Box(
                                    Modifier.fillMaxWidth().fillMaxHeight(frac)
                                        .background(if (b.current) z.accent else z.a300)
                                        .clickable { vm.setPeriod(if (b.current) Period.Month else Period.Quarter) },
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        bars.forEach { b ->
                            Text(
                                b.label, Modifier.weight(1f), style = bodyStyle(10.sp),
                                color = z.text.copy(alpha = 0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
            SectionLabel("Por categoria", bottom = 8.dp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                byCat.entries.sortedByDescending { it.value }.forEach { (name, v) ->
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, style = bodyStyle(13.5.sp), color = z.text)
                            Text(brl(v), style = headingStyle(16.sp, 0f), color = z.text)
                        }
                        Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(6.dp).background(z.n200)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((v / catMax).toFloat()).background(z.accent))
                        }
                    }
                }
            }
            SectionLabel("Lançamentos · ${exp.size}")
            exp.forEach { e ->
                ZRow(onClick = { vm.askDeleteExpense(e) }) {
                    Column(Modifier.weight(1f)) {
                        Text("${e.cat} · ${e.title}", style = bodyStyle(15.sp), color = z.text)
                        Muted(
                            e.epochDay.toDate().dmy() + (if (e.extra.isNotBlank()) " · ${e.extra}" else "") + " · ${nkm(e.km)} km",
                            size = 11.5f,
                        )
                    }
                    Text(brl(e.value), style = headingStyle(18.sp, 0f), color = z.text)
                }
            }
            if (exp.isEmpty()) {
                EmptyState("Nenhum lançamento neste período.", "Toque em Gasto para registrar o primeiro.")
            } else {
                Muted("Toque em um lançamento para apagar.", size = 11f, alpha = 0.55f, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}
