package com.zelcar.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.Blueprint
import com.zelcar.app.ui.components.Divider
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import com.zelcar.app.ui.theme.kickerStyle

@Composable
fun OnboardingScreen(vm: ZelcarViewModel) {
    val z = Z.c
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Text("BEM-VINDO", style = kickerStyle(), color = z.a700)
            Text(
                "Cuide do seu carro sem planilha.", style = headingStyle(38.sp, -0.02f), color = z.text,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )
            Text(
                "Sem cadastro, sem senha. Você entra e começa a usar — seus dados ficam no aparelho e sobem para a nuvem em segundo plano.",
                style = bodyStyle(14.sp), color = z.text.copy(alpha = 0.7f),
            )
        }
        Divider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Blueprint(Modifier.fillMaxWidth(), padding = 16.dp, onClick = { vm.finishOnboarding(false) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ZIcon(ZIcons.Car, size = 26.dp, tint = z.accent)
                    Column {
                        Text("Já tenho o carro", style = headingStyle(19.sp), color = z.text)
                        Muted("Vai direto para a garagem, lembretes e gastos.")
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().border(1.dp, z.divider).clickable { vm.finishOnboarding(true) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ZIcon(ZIcons.ChecklistTick, size = 26.dp, tint = z.accent)
                Column {
                    Text("Estou comprando um carro", style = headingStyle(19.sp), color = z.text)
                    Muted("Abre o checklist de vistoria, débitos e burocracia.")
                }
            }
        }
        ZButton("Explorar com dados de exemplo", { vm.loadDemo() }, kind = BtnKind.Ghost)
        Muted(
            "Quer usar no computador também? Abra zelcar.app no navegador e escaneie o QR Code com este aparelho.",
            size = 11f, alpha = 0.55f,
        )
    }
}
