package com.zelcar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.data.splitPaths
import com.zelcar.app.domain.dmyy
import com.zelcar.app.domain.nkm
import com.zelcar.app.domain.toDate
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.CarMode
import com.zelcar.app.ui.Dest
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.Blueprint
import com.zelcar.app.ui.components.EmptyState
import com.zelcar.app.ui.components.HatchedThumb
import com.zelcar.app.ui.components.Kicker
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.PhotoThumb
import com.zelcar.app.ui.components.ScreenTitle
import com.zelcar.app.ui.components.TagKind
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.components.ZTag
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle

@Composable
fun LogbookScreen(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val car = state.activeCar
    if (car == null) {
        ScreenScroll { Body { EmptyState("Sem carro na garagem", "Adicione um carro para usar o diário de bordo.", "Adicionar carro", { vm.openAddCar(CarMode.Owned) }) } }
        return
    }
    val notes = state.carNotes
    ScreenScroll {
        ScreenHero(HeroKind.Logbook)
        Body(top = 0) {
            ScreenTitle("Diário de bordo")
            Muted("${car.name} · ${notes.size} registros", modifier = Modifier.padding(top = 2.dp))
            Blueprint(Modifier.padding(top = 20.dp).fillMaxWidth(), padding = 16.dp, onClick = { vm.go(Dest.Km, push = true) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ZIcon(ZIcons.Odometer, size = 26.dp, tint = z.accent)
                    Column(Modifier.weight(1f)) {
                        Kicker("Quilometragem atual", tracking = 0.12f)
                        Text("${nkm(car.km)} km", style = headingStyle(30.sp, 0f, 1f), color = z.text)
                    }
                    ZTag("Atualizar", TagKind.Outline)
                }
            }
            if (notes.isEmpty()) {
                Column(Modifier.padding(top = 24.dp)) {
                    EmptyState("Diário vazio", "Registre barulhos, consumo e serviços. Toque em Nota.")
                }
            }
            val line = z.divider
            Column(
                Modifier
                    .padding(top = 24.dp)
                    .drawBehind { drawLine(line, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx()) }
                    .padding(start = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                notes.forEachIndexed { i, n ->
                    Box {
                        Box(
                            Modifier.offset(x = (-22.5).dp, y = 6.dp).size(7.dp)
                                .background(if (i == 0) z.accent else z.n400),
                        )
                        Column {
                            Muted("${n.epochDay.toDate().dmyy()} · ${nkm(n.km)} km", size = 11f, alpha = 0.55f)
                            Text(n.title, style = headingStyle(19.sp), color = z.text, modifier = Modifier.padding(top = 2.dp))
                            if (n.body.isNotBlank()) {
                                Text(n.body, style = bodyStyle(13.sp), color = z.text.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))
                            }
                            val paths = splitPaths(n.photoPaths)
                            if (paths.isNotEmpty() || n.photos > 0) {
                                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (paths.isNotEmpty()) paths.forEach { PhotoThumb(it) }
                                    else repeat(n.photos.coerceAtMost(4)) { HatchedThumb() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
