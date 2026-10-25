package com.zelcar.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.domain.CHECK_GROUPS
import com.zelcar.app.domain.EXPENSE_CATEGORIES
import com.zelcar.app.domain.brl
import com.zelcar.app.domain.costPerKm
import com.zelcar.app.domain.issueLine
import com.zelcar.app.domain.nkm
import com.zelcar.app.domain.parseInt2
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.CarMode
import com.zelcar.app.ui.Dest
import com.zelcar.app.ui.PhotoTarget
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.ZSheet
import com.zelcar.app.ui.components.Blueprint
import com.zelcar.app.ui.components.HintBox
import com.zelcar.app.ui.components.Kicker
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.SegControl
import com.zelcar.app.ui.components.SheetHeader
import com.zelcar.app.ui.components.WrapRow
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.ZCategoryChip
import com.zelcar.app.ui.components.ZCheckBox
import com.zelcar.app.ui.components.ZField
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle

// ─────────────────────────── Novo gasto ───────────────────────────
@Composable
fun AddExpenseSheet(state: AppState, vm: ZelcarViewModel) {
    val f = state.ui.form
    val car = state.activeCar
    val cpk = if (car != null) costPerKm(state.carExpenses, car.km, state.today) else 0.0
    ZSheet(onClose = { vm.back() }) { close ->
        SheetHeader("Novo gasto", "Adicionar gasto")
        WrapRow(Modifier.padding(top = 14.dp)) {
            EXPENSE_CATEGORIES.forEach { c -> ZCategoryChip(c, f.cat == c) { vm.form { it.copy(cat = c) } } }
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZField("Valor (R$)", f.value, { v -> vm.form { it.copy(value = v) } }, Modifier.weight(1f),
                placeholder = "0,00", keyboardType = KeyboardType.Decimal)
            ZField("Quilometragem", f.km, { v -> vm.form { it.copy(km = v) } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
        }
        ZField("Descrição", f.title, { v -> vm.form { it.copy(title = v) } }, Modifier.padding(top = 12.dp),
            placeholder = "Posto, oficina, órgão…")
        HintBox("Este gasto entra em ${f.cat} e recalcula o custo por km, hoje em ${brl(cpk)}.", Modifier.padding(top = 14.dp), 0.8f)
        SheetButtons("Salvar", close) { vm.saveExpense() }
    }
}

// ─────────────────────────── Nova nota ───────────────────────────
@Composable
fun AddNoteSheet(state: AppState, vm: ZelcarViewModel) {
    val f = state.ui.form
    ZSheet(onClose = { vm.back() }) { close ->
        SheetHeader("Nova nota", "Adicionar nota")
        ZField("Título", f.noteTitle, { v -> vm.form { it.copy(noteTitle = v) } }, Modifier.padding(top = 14.dp),
            placeholder = "Barulho, consumo, serviço…")
        ZField("O que aconteceu", f.noteBody, { v -> vm.form { it.copy(noteBody = v) } }, Modifier.padding(top = 12.dp),
            placeholder = "Descreva com suas palavras.", singleLine = false, minLines = 4)
        ZField("Quilometragem", f.noteKm, { v -> vm.form { it.copy(noteKm = v) } }, Modifier.padding(top = 12.dp),
            keyboardType = KeyboardType.Number)
        PhotoRow(f.notePhotos, { vm.attachPhotos(PhotoTarget.Note, it) }, { vm.removePhoto(PhotoTarget.Note, it) })
        SheetButtons("Salvar", close) { vm.saveNote() }
    }
}

// ─────────────────────────── Detalhar problema ───────────────────────────
@Composable
fun IssueSheet(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val f = state.ui.form
    val item = state.data.checks.firstOrNull { it.id == state.ui.selCheckId }
    ZSheet(onClose = { vm.back() }) { close ->
        SheetHeader("Detalhar problema", item?.label ?: "Problema", titleSize = 28)
        Muted(CHECK_GROUPS.firstOrNull { it.first == item?.grp }?.second ?: "", modifier = Modifier.padding(top = 2.dp))
        Muted("Gravidade", size = 12f, alpha = 0.7f, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("leve" to "Leve", "atencao" to "Atenção", "grave" to "Grave").forEach { (k, l) ->
                val on = f.issueSev == k
                Box(
                    Modifier.weight(1f).height(48.dp)
                        .background(if (on) z.a100 else Color.Transparent)
                        .border(1.dp, if (on) z.accent else z.divider)
                        .clickable { vm.form { it.copy(issueSev = k) } },
                    contentAlignment = Alignment.Center,
                ) { Text(l, style = bodyStyle(14.sp), color = if (on) z.a800 else z.text) }
            }
        }
        ZField("O que você encontrou", f.issueNote, { v -> vm.form { it.copy(issueNote = v) } }, Modifier.padding(top = 14.dp),
            placeholder = "Onde está, como percebeu, o que o vendedor disse…", singleLine = false, minLines = 3)
        ZField("Custo estimado de reparo (opcional)", f.issueCost, { v -> vm.form { it.copy(issueCost = v) } },
            Modifier.padding(top = 12.dp), placeholder = "R$ 0", keyboardType = KeyboardType.Decimal)
        PhotoRow(f.issuePhotos, { vm.attachPhotos(PhotoTarget.Issue, it) }, { vm.removePhoto(PhotoTarget.Issue, it) })
        SheetButtons("Salvar", close) { vm.saveIssue() }
    }
}

// ─────────────────────────── Quilometragem ───────────────────────────
@Composable
fun KmSheet(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val f = state.ui.form
    val car = state.activeCar
    val draft = parseInt2(f.kmDraft)
    val diff = draft - (car?.km ?: 0)
    val next = car?.let { remindersOfCar(state, it).firstOrNull() }
    ZSheet(onClose = { vm.back() }) { close ->
        SheetHeader("Quilometragem", "Atualizar o km")
        Blueprint(Modifier.padding(top = 16.dp).fillMaxWidth(), padding = 20.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Kicker("Novo valor", tracking = 0.12f)
                Text(nkm(draft), style = headingStyle(54.sp, 0f, 1f), color = z.accent)
                Muted(
                    "anterior: ${nkm(car?.km ?: 0)} km · ${if (diff >= 0) "+" else ""}${nkm(diff)} km",
                    alpha = 0.6f, modifier = Modifier.padding(top = 4.dp),
                )
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("-100" to -100, "+100" to 100, "+500" to 500, "+1.000" to 1000).forEach { (l, d) ->
                        ZButton(l, { vm.kmStep(d) }, Modifier.weight(1f))
                    }
                }
            }
        }
        ZField("Ou digite", f.kmDraft, { v -> vm.form { it.copy(kmDraft = v) } }, Modifier.padding(top = 14.dp),
            keyboardType = KeyboardType.Number)
        HintBox(
            if (next != null && next.isKm)
                "Salvar recalcula lembretes por distância e o custo por km. ${next.reminder.label} passa a vencer em " +
                    "${nkm(maxOf(0, next.reminder.lastKm + next.reminder.everyKm - draft))} km."
            else "Salvar recalcula o custo por km e o consumo médio.",
            Modifier.padding(top = 14.dp),
        )
        SheetButtons("Salvar", close) { vm.saveKm() }
    }
}

private fun remindersOfCar(state: AppState, car: com.zelcar.app.data.CarEntity) =
    com.zelcar.app.ui.screens.remindersOf(state, car)

// ─────────────────────────── Novo carro ───────────────────────────
@Composable
fun AddCarSheet(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val f = state.ui.form
    val buying = state.ui.carMode == CarMode.Buying
    val gate = !state.isPro && state.data.cars.size >= com.zelcar.app.domain.FREE_CAR_LIMIT
    ZSheet(onClose = { vm.back() }) { close ->
        SheetHeader("Novo carro", "Adicionar carro")
        SegControl(
            CarMode.entries.map { it.label }, CarMode.entries.indexOf(state.ui.carMode),
            { vm.setCarMode(CarMode.entries[it]) }, Modifier.padding(top = 14.dp),
        )
        if (buying) {
            Blueprint(Modifier.padding(top = 14.dp).fillMaxWidth()) {
                Text(
                    "Depois de salvar, você vai direto para o checklist de compra. Placa, km e carro principal ficam para quando você fechar negócio.",
                    style = bodyStyle(12.5.sp), color = z.text,
                )
            }
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZField("Marca", f.carBrand, { v -> vm.form { it.copy(carBrand = v) } }, Modifier.weight(1f), placeholder = "Ex.: Volkswagen")
            ZField("Cor", f.carColor, { v -> vm.form { it.copy(carColor = v) } }, Modifier.weight(1f), placeholder = "Ex.: Prata")
        }
        ZField("Modelo", f.carName, { v -> vm.form { it.copy(carName = v) } }, Modifier.padding(top = 12.dp), placeholder = "Ex.: Polo TSI")
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZField("Ano", f.carYear, { v -> vm.form { it.copy(carYear = v) } }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
            if (buying) {
                ZField("Preço pedido (opcional)", f.carPrice, { v -> vm.form { it.copy(carPrice = v) } }, Modifier.weight(1f),
                    placeholder = "R$", keyboardType = KeyboardType.Number)
            } else {
                ZField("Placa", f.carPlate, { v -> vm.form { it.copy(carPlate = v) } }, Modifier.weight(1f), placeholder = "ABC-1D23")
            }
        }
        if (!buying) {
            ZField("Quilometragem", f.carKmNew, { v -> vm.form { it.copy(carKmNew = v) } }, Modifier.padding(top = 12.dp),
                keyboardType = KeyboardType.Number)
            Row(
                Modifier.padding(top = 14.dp).fillMaxWidth().clickable { vm.form { it.copy(carMain = !it.carMain) } },
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top,
            ) {
                ZCheckBox(if (f.carMain) "✓" else "", f.carMain, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Carro principal", style = bodyStyle(14.5.sp), color = z.text)
                    Muted("Aparece primeiro na Garagem e recebe o resumo mensal e os avisos. Só um carro por vez.", size = 12f, alpha = 0.65f)
                }
            }
        }
        if (gate) {
            Text(
                "O plano grátis guarda até 2 carros. Este seria o terceiro — assine o Pro para salvar.",
                style = bodyStyle(12.5.sp), color = z.a800,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth().background(z.a100).border(1.dp, z.a400)
                    .clickable { vm.go(Dest.Paywall, push = true) }.padding(14.dp),
            )
        }
        SheetButtons(if (buying) "Começar checklist" else "Salvar", close) { vm.saveCar() }
    }
}
