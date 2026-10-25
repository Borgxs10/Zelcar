package com.zelcar.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zelcar.app.R
import com.zelcar.app.data.CarEntity
import com.zelcar.app.domain.RankedReminder
import com.zelcar.app.domain.dmyy
import com.zelcar.app.domain.nkm
import com.zelcar.app.domain.rankReminders
import com.zelcar.app.domain.toDate
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.components.Hero
import com.zelcar.app.ui.theme.Z

/** Área rolável de uma tela (as ilustrações "hero" sangram até as bordas, então não há padding aqui). */
@Composable
fun ScreenScroll(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), content = content)
}

/** Conteúdo com as margens padrão do design (16dp nas laterais, 96dp embaixo para o FAB). */
@Composable
fun Body(modifier: Modifier = Modifier, top: Int = 20, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = top.dp, bottom = 96.dp),
        content = content,
    )
}

enum class HeroKind { Car, Checklist, Finance, Reminders, Logbook, Pairing }

/** Ilustração do topo de cada tela, na versão clara ou escura. */
@Composable
fun ScreenHero(kind: HeroKind) {
    val dark = Z.c.dark
    val hero: Triple<Int, Float, Color> = when (kind) {
        HeroKind.Car -> Triple(
            if (dark) R.drawable.hero_car_dark else R.drawable.hero_car_light,
            1376f / 632f, if (dark) Color(0xFF0B1A2D) else Color.White,
        )
        HeroKind.Checklist -> Triple(
            if (dark) R.drawable.hero_checklist_dark else R.drawable.hero_checklist_light,
            if (dark) 1376f / 768f else 1792f / 592f, if (dark) Color(0xFF091B2F) else Color(0xFFEDF7FD),
        )
        HeroKind.Finance -> Triple(
            if (dark) R.drawable.hero_finance_dark else R.drawable.hero_finance_light,
            1376f / 768f, if (dark) Color(0xFF0E1E2E) else Color.White,
        )
        HeroKind.Reminders -> Triple(
            if (dark) R.drawable.hero_reminders_dark else R.drawable.hero_reminders_light,
            1376f / 768f, if (dark) Color(0xFF091724) else Color.White,
        )
        HeroKind.Logbook -> Triple(
            if (dark) R.drawable.hero_logbook_dark else R.drawable.hero_logbook_light,
            1376f / 768f, if (dark) Color(0xFF0B1D31) else Color.White,
        )
        HeroKind.Pairing -> Triple(
            if (dark) R.drawable.hero_web_dark else R.drawable.hero_web_light,
            1792f / 592f, if (dark) Color(0xFF09233E) else Color(0xFFF4F9FD),
        )
    }
    Hero(res = hero.first, aspect = hero.second, imageBg = hero.third, fadeTo = Z.c.canvas)
}

/** Lembretes do carro, ordenados por urgência. */
fun remindersOf(state: AppState, car: CarEntity): List<RankedReminder> =
    rankReminders(state.data.reminders.filter { it.carId == car.id }, car.km, state.today)

/** "Pendências" = lembretes que vencem em até 45 dias ou já venceram. */
fun pendingOf(state: AppState, car: CarEntity): List<RankedReminder> =
    remindersOf(state, car).filter { it.days <= 45 }

fun dueText(r: RankedReminder): String = when {
    r.isKm && (r.remainKm ?: 0) < 0 -> "atrasado em ${nkm(-(r.remainKm ?: 0))} km"
    r.isKm -> "vence em ${nkm(r.remainKm ?: 0)} km (~${r.days} dias)"
    r.days < 0 -> "atrasado há ${-r.days} dias · ${r.reminder.dueEpochDay.toDate().dmyy()}"
    else -> "vence em ${r.days} dias · ${r.reminder.dueEpochDay.toDate().dmyy()}"
}
