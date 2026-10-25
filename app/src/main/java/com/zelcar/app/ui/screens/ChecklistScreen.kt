package com.zelcar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.domain.CHECK_GROUPS
import com.zelcar.app.domain.brl0
import com.zelcar.app.domain.issueLine
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.CarMode
import com.zelcar.app.ui.Dest
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.Blueprint
import com.zelcar.app.ui.components.EmptyState
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.ProgressBar
import com.zelcar.app.ui.components.ScreenTitle
import com.zelcar.app.ui.components.TagKind
import com.zelcar.app.ui.components.WrapRow
import com.zelcar.app.ui.components.ZChip
import com.zelcar.app.ui.components.ZCheckBox
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.components.ZTag
import com.zelcar.app.ui.components.ZRow
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.kickerStyle

@Composable
fun ChecklistScreen(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val car = state.buyingCar
    if (car == null) {
        ScreenScroll {
            Body {
                EmptyState(
                    "Nenhum carro em compra", "Adicione o carro que você está avaliando para abrir o checklist de vistoria.",
                    "Adicionar carro em compra", { vm.openAddCar(CarMode.Buying) },
                )
            }
        }
        return
    }
    val items = state.checkItems
    val total = items.size.coerceAtLeast(1)
    val ok = items.count { it.state == "ok" }
    val issue = items.count { it.state == "issue" }
    val unset = items.size - ok - issue
    val repair = items.filter { it.state == "issue" && it.cost > 0 }
    val repairTotal = repair.sumOf { it.cost }
    val filter = state.ui.checkFilter

    ScreenScroll {
        ScreenHero(HeroKind.Checklist)
        Body(top = 0) {
            ScreenTitle("Checklist de compra", 32)
            Muted(car.name + if (car.price > 0) " · usado · ${brl0(car.price.toDouble())}" else " · usado", size = 12.5f)
            Blueprint(Modifier.padding(top = 18.dp).fillMaxWidth()) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progresso", style = bodyStyle(12.sp), color = z.text)
                        Text("${ok + issue} de ${items.size} itens", style = bodyStyle(12.sp), color = z.text.copy(alpha = 0.6f))
                    }
                    ProgressBar(
                        listOf(ok.toFloat() / total to z.accent, issue.toFloat() / total to z.a300),
                        8.dp, Modifier.padding(top = 8.dp), bordered = true,
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("$ok OK", style = bodyStyle(11.sp), color = z.text)
                        Text("$issue com problema", style = bodyStyle(11.sp), color = z.a700)
                        Text("$unset não verificados", style = bodyStyle(11.sp), color = z.text.copy(alpha = 0.55f))
                    }
                    if (repairTotal > 0) {
                        Text(
                            "Reparos estimados: ${brl0(repairTotal)} em ${repair.size} ${if (repair.size > 1) "itens" else "item"}",
                            style = bodyStyle(12.sp), color = z.a800, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Muted("O app não recomenda comprar ou não — o julgamento é seu.", size = 11f, alpha = 0.55f, modifier = Modifier.padding(top = 8.dp))
                }
            }
            WrapRow(Modifier.padding(top = 18.dp)) {
                ZChip("Todos", filter == "todos", { vm.setCheckFilter("todos") })
                CHECK_GROUPS.forEach { (k, t) -> ZChip(t, filter == k, { vm.setCheckFilter(k) }) }
            }
            CHECK_GROUPS.filter { filter == "todos" || filter == it.first }.forEach { (g, title) ->
                val group = items.filter { it.grp == g }
                Row(
                    Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title.uppercase(), style = kickerStyle(), color = z.a700)
                    ZTag("${group.count { it.state != "unset" }} / ${group.size}", TagKind.Neutral)
                }
                group.forEach { c ->
                    ZRow(onClick = { vm.pickCheck(c.id) }) {
                        ZCheckBox(
                            mark = when (c.state) { "ok" -> "✓"; "issue" -> "!"; else -> "" },
                            on = c.state == "ok", issue = c.state == "issue",
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                c.label, style = bodyStyle(14.5.sp),
                                color = z.text.copy(alpha = if (c.state == "unset") 0.72f else 1f),
                            )
                            if (c.state == "issue") Text(issueLine(c), style = bodyStyle(11.5.sp), color = z.a700)
                        }
                        when (c.state) {
                            "ok" -> ZTag("OK", TagKind.Ok)
                            "issue" -> ZTag("Problema", TagKind.Bad)
                            else -> ZTag("Verificar", TagKind.Neutral)
                        }
                    }
                }
            }
            Row(
                Modifier.padding(top = 20.dp).fillMaxWidth().border(1.dp, z.a400).background(z.a100)
                    .clickable { if (!state.isPro) vm.go(Dest.Paywall, push = true) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ZIcon(ZIcons.Lock, tint = z.a700)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.isPro) "Comparação com a tabela FIPE liberada"
                        else "Comparação com a tabela FIPE e 12 itens de inspeção mecânica",
                        style = bodyStyle(14.sp), color = z.a800,
                    )
                    Text(
                        if (state.isPro) "Informe o preço pedido para comparar (consulta FIPE em breve)." else "Disponível no Zelcar Pro",
                        style = bodyStyle(11.5.sp), color = z.a700,
                    )
                }
            }
        }
    }
}
