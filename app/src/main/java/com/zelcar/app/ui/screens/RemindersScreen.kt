package com.zelcar.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.domain.RankedReminder
import com.zelcar.app.domain.dmyy
import com.zelcar.app.domain.nkm
import com.zelcar.app.domain.toDate
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.CarMode
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.Blueprint
import com.zelcar.app.ui.components.EmptyState
import com.zelcar.app.ui.components.Kicker
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.ProgressBar
import com.zelcar.app.ui.components.ScreenTitle
import com.zelcar.app.ui.components.SectionLabel
import com.zelcar.app.ui.components.TagKind
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.ZField
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.components.ZRow
import com.zelcar.app.ui.components.ZTag
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import com.zelcar.app.ui.theme.kickerStyle

private fun levelKind(r: RankedReminder) = when (r.level) {
    RankedReminder.Level.Overdue -> TagKind.Bad
    RankedReminder.Level.Soon -> TagKind.Warn
    RankedReminder.Level.Near -> TagKind.Info
    RankedReminder.Level.Ok -> TagKind.Ok
}

@Composable
fun RemindersScreen(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val car = state.activeCar
    if (car == null) {
        ScreenScroll { Body { EmptyState("Sem carro na garagem", "Adicione um carro para criar lembretes.", "Adicionar carro", { vm.openAddCar(CarMode.Owned) }) } }
        return
    }
    val ranked = remindersOf(state, car)
    val next = ranked.firstOrNull()
    val form = state.ui.form
    val selected = ranked.firstOrNull { it.reminder.id == state.ui.selReminderId } ?: next

    ScreenScroll {
        ScreenHero(HeroKind.Reminders)
        Body(top = 0) {
            ScreenTitle("Lembretes")
            Muted("${car.name} · ${nkm(car.km)} km", modifier = Modifier.padding(top = 2.dp))
            if (next == null) {
                Column(Modifier.padding(top = 20.dp)) {
                    EmptyState("Nenhum lembrete", "Toque em Lembrete para criar o primeiro.", "Criar lembrete", { vm.addReminder() })
                }
                return@Body
            }
            Blueprint(Modifier.padding(top = 20.dp).fillMaxWidth(), padding = 16.dp) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Kicker("Próximo", color = z.a700, tracking = 0.12f)
                            Text(next.reminder.label, style = headingStyle(26.sp, -0.015f, 1.15f), color = z.text, modifier = Modifier.padding(top = 4.dp))
                        }
                        ZTag(
                            next.tag,
                            if (next.days <= 14) TagKind.Outline else if (next.days <= 45) TagKind.Accent else TagKind.Neutral,
                        )
                    }
                    ProgressBar(listOf(next.pct / 100f to z.accent), 6.dp, Modifier.padding(top = 14.dp, bottom = 8.dp))
                    val r = next.reminder
                    Text(
                        if (next.isKm) "${nkm(car.km - r.lastKm)} de ${nkm(r.everyKm)} km rodados desde a última em ${nkm(r.lastKm)} km."
                        else "Data fixa em ${r.dueEpochDay.toDate().dmyy()} · faltam ${next.days} dias.",
                        style = bodyStyle(12.sp), color = z.text.copy(alpha = 0.7f),
                    )
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZButton("Marcar como feito", { vm.doneNext() }, Modifier.weight(1f), BtnKind.Primary)
                        ZButton("Adiar", { vm.askPostpone() }, Modifier.weight(1f))
                    }
                }
            }
            SectionLabel("Fila completa")
            ranked.forEach { r ->
                ZRow(onClick = { vm.selectReminder(r.reminder.id) }) {
                    ZIcon(ZIcons.Bell, tint = if (r.reminder.id == selected?.reminder?.id) z.a900 else z.accent)
                    Column(Modifier.weight(1f)) {
                        Text(r.reminder.label, style = bodyStyle(15.sp), color = z.text)
                        Muted(r.sub, size = 11.5f)
                    }
                    ZTag(r.tag, levelKind(r))
                }
            }
            if (selected != null) {
                Column(Modifier.padding(top = 20.dp).fillMaxWidth().border(1.dp, z.divider).padding(14.dp)) {
                    Text("INTERVALO DE ${selected.reminder.label.uppercase()}", style = kickerStyle(), color = z.text.copy(alpha = 0.5f))
                    ZField("Nome", form.reminderLabel, { v -> vm.form { it.copy(reminderLabel = v) } }, Modifier.padding(top = 10.dp))
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ZField("Quilometragem", form.intervalKm, { v -> vm.form { it.copy(intervalKm = v) } },
                            Modifier.weight(1f), keyboardType = KeyboardType.Number)
                        ZField("Tempo (meses)", form.intervalMonths, { v -> vm.form { it.copy(intervalMonths = v) } },
                            Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                    ZButton("Aplicar intervalo", { vm.saveInterval() }, Modifier.padding(top = 10.dp).fillMaxWidth())
                    ZButton("Excluir lembrete", { vm.askDeleteSelectedReminder() }, Modifier.fillMaxWidth(), BtnKind.Ghost)
                    Muted(
                        "Dispara no que vencer primeiro. Notificações próximas são agrupadas em um só aviso.",
                        size = 11f, alpha = 0.55f, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
