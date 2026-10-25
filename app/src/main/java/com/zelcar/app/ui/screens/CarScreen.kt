package com.zelcar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.domain.brl
import com.zelcar.app.domain.brl0
import com.zelcar.app.domain.costPerKm
import com.zelcar.app.domain.dmy
import com.zelcar.app.domain.monthTotal
import com.zelcar.app.domain.nkm
import com.zelcar.app.domain.toDate
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.CarMode
import com.zelcar.app.ui.Dest
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.EmptyState
import com.zelcar.app.ui.components.Kicker
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.ScreenTitle
import com.zelcar.app.ui.components.SectionLabel
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.components.ZRow
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle

@Composable
fun CarScreen(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val car = state.activeCar
    if (car == null) {
        ScreenScroll { Body { EmptyState("Nenhum carro", "Adicione um carro para ver a ficha.", "Adicionar carro", { vm.openAddCar(CarMode.Owned) }) } }
        return
    }
    val now = state.today
    val exp = state.carExpenses
    val cur = monthTotal(exp, now.year, now.monthValue)
    val prevD = now.minusMonths(1)
    val prev = monthTotal(exp, prevD.year, prevD.monthValue)
    val delta = if (prev > 0) Math.round((cur - prev) / prev * 100).toInt() else null
    val ranked = remindersOf(state, car)
    val next = ranked.firstOrNull()
    val pending = ranked.filter { it.days <= 45 }
    val cpk = if (exp.isEmpty()) "—" else brl(costPerKm(exp, car.km, now))

    ScreenScroll {
        ScreenHero(HeroKind.Car)
        Body(top = 0) {
            ScreenTitle(car.name, 32)
            Muted(
                car.year.toString() + (if (car.color != "—") " · ${car.color}" else "") + " · ${car.plate} · atualizado hoje",
                size = 12.5f,
            )
            Box(Modifier.padding(top = 20.dp).fillMaxWidth().border(1.dp, z.divider).background(z.divider).padding(1.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        Metric("Gasto no mês", brl0(cur), if (delta == null) "sem mês anterior" else (if (delta >= 0) "+" else "") + "$delta% vs. mês anterior", true,
                            Modifier.weight(1f)) { vm.go(Dest.Finance) }
                        Metric("Custo por km", cpk, "últimos 90 dias", false, Modifier.weight(1f))
                    }
                    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        Metric("Quilometragem", nkm(car.km), "Atualizar agora", true, Modifier.weight(1f)) { vm.go(Dest.Km, push = true) }
                        Metric(
                            "Próxima revisão",
                            next?.let { if (it.isKm) nkm(maxOf(0, it.remainKm ?: 0)) + " km" else "${it.days} dias" } ?: "—",
                            next?.reminder?.label ?: "Sem lembretes", false, Modifier.weight(1f),
                        ) { vm.go(Dest.Reminders) }
                    }
                }
            }
            SectionLabel("Verificações pendentes · ${pending.size}")
            pending.forEach { p ->
                ZRow(onClick = { vm.go(Dest.Reminders) }) {
                    ZIcon(ZIcons.Warn, tint = z.accent)
                    Column(Modifier.weight(1f)) {
                        Text(p.reminder.label, style = bodyStyle(15.sp), color = z.text)
                        Muted(dueText(p), size = 11.5f)
                    }
                    ZIcon(ZIcons.Chevron, tint = z.text.copy(alpha = 0.35f))
                }
            }
            SectionLabel("Últimos registros")
            exp.take(2).forEach { e ->
                ZRow(onClick = { vm.go(Dest.Finance) }) {
                    Column(Modifier.weight(1f)) {
                        Text("${e.cat} · ${e.title}", style = bodyStyle(15.sp), color = z.text)
                        Muted("${e.epochDay.toDate().dmy()} · ${nkm(e.km)} km", size = 11.5f)
                    }
                    Text(brl(e.value), style = headingStyle(18.sp, 0f), color = z.text)
                }
            }
            state.carNotes.take(1).forEach { n ->
                ZRow(onClick = { vm.go(Dest.Logbook) }) {
                    Column(Modifier.weight(1f)) {
                        Text("Nota · ${n.title}", style = bodyStyle(15.sp), color = z.text)
                        Muted("${n.epochDay.toDate().dmy()} · ${nkm(n.km)} km", size = 11.5f)
                    }
                    Text("›", style = headingStyle(18.sp, 0f), color = z.text)
                }
            }
            Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZButton("Transferir", { vm.go(Dest.Transfer, push = true) }, Modifier.weight(1f))
                ZButton("Relatório", { vm.go(Dest.Report, push = true) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(
    label: String, value: String, sub: String, accentSub: Boolean, modifier: Modifier,
    onClick: (() -> Unit)? = null,
) {
    val z = Z.c
    Column(
        modifier.background(z.bg).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp),
    ) {
        Kicker(label)
        Text(value, style = headingStyle(28.sp, 0f), color = z.text, maxLines = 1)
        Text(sub, style = bodyStyle(11.sp), color = if (accentSub) z.a700 else z.text.copy(alpha = 0.55f), maxLines = 2)
    }
}
