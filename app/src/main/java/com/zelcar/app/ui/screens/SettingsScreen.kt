package com.zelcar.app.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelcar.app.BuildConfig
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.Dest
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.ScreenTitle
import com.zelcar.app.ui.components.SectionLabel
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.components.ZRow
import com.zelcar.app.ui.components.ZSwitch
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import com.zelcar.app.ui.theme.kickerStyle

@Composable
fun SettingsScreen(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val s = state.settings
    ScreenScroll {
        Body(top = 20) {
            ScreenTitle("Configurações")
            Column(
                Modifier.padding(top = 18.dp).fillMaxWidth().background(z.a100).border(1.dp, z.accent)
                    .clickable { vm.go(Dest.Paywall, push = true) }.padding(16.dp),
            ) {
                Text("PLANO", style = kickerStyle(), color = z.a700)
                Text("Zelcar ${if (state.isPro) "Pro" else "Grátis"}", style = headingStyle(24.sp), color = z.a900)
                Text(
                    if (state.isPro) "Assinatura ativa · vários carros, checklist completo e PDF liberados"
                    else "Até 2 carros, lembretes e gastos. Assine para liberar o resto.",
                    style = bodyStyle(12.sp), color = z.a800,
                )
            }
            SectionLabel("Dados")
            NavRow(ZIcons.Qr, "Vincular com o navegador", if (s.paired) "1 sessão web ativa" else "nenhuma sessão vinculada") {
                vm.go(Dest.Pairing, push = true)
            }
            if (s.paired) {
                ZRow(onClick = { vm.askRevoke() }) {
                    ZIcon(ZIcons.Close, tint = z.a700)
                    Column(Modifier.weight(1f)) {
                        Text("Revogar sessão web", style = bodyStyle(15.sp), color = z.a800)
                        Muted("o navegador para de ver seus carros", size = 11.5f)
                    }
                }
            }
            NavRow(ZIcons.Transfer, "Transferir um carro", "gera QR Code com o histórico") { vm.go(Dest.Transfer, push = true) }
            NavRow(ZIcons.Pdf, "Exportar relatório em PDF", "você escolhe o que entra") { vm.go(Dest.Report, push = true) }

            SectionLabel("Notificações")
            Toggle("Lembretes de manutenção", "agrupados, 1 aviso por dia", s.notifReminders) { vm.setNotif("lembretes", it) }
            Toggle("Resumo mensal de gastos", "todo dia 1º", s.notifSummary) { vm.setNotif("resumo", it) }
            Toggle("Sincronizar em rede móvel", "desligado usa só Wi-Fi", s.notifSync) { vm.setNotif("sync", it) }

            SectionLabel("Suporte")
            ZRow(onClick = { vm.toast("Em breve.") }) { Text("Falar com o suporte", style = bodyStyle(15.sp), color = z.text) }
            ZRow(onClick = { vm.toast("Em breve.") }) { Text("Privacidade e LGPD", style = bodyStyle(15.sp), color = z.text) }
            ZRow(onClick = { vm.askReset() }) {
                Text("Apagar todos os dados do aparelho", style = bodyStyle(15.sp), color = z.a800)
            }
            Muted(
                "Zelcar ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Android ${Build.VERSION.RELEASE}",
                size = 11f, alpha = 0.45f, modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

@Composable
private fun NavRow(icon: List<String>, title: String, sub: String, onClick: () -> Unit) {
    val z = Z.c
    ZRow(onClick = onClick) {
        ZIcon(icon, tint = z.accent)
        Column(Modifier.weight(1f)) {
            Text(title, style = bodyStyle(15.sp), color = z.text)
            Muted(sub, size = 11.5f)
        }
        ZIcon(ZIcons.Chevron, tint = z.text.copy(alpha = 0.35f))
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    ZRow(onClick = { onChange(!on) }) {
        Column(Modifier.weight(1f)) {
            Text(title, style = bodyStyle(15.sp), color = Z.c.text)
            Muted(sub, size = 11.5f)
        }
        ZSwitch(on, onChange)
    }
}
