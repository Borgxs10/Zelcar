package com.zelcar.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.Blueprint
import com.zelcar.app.ui.components.QrScannerView
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import com.zelcar.app.ui.theme.kickerStyle

@Composable
fun PairingScreen(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val context = LocalContext.current
    val paired = state.settings.paired
    val scanning = state.ui.scanning
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.startScan() else vm.toast("Permita a câmera para ler o QR Code.")
    }
    fun scan() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.startScan() else permission.launch(Manifest.permission.CAMERA)
    }

    ScreenScroll {
        ScreenHero(HeroKind.Pairing)
        Body(top = 0) {
            Text("CAMERAX · PAREAMENTO", style = kickerStyle(), color = z.a700)
            Text(
                if (paired) "Navegador vinculado" else "Escanear o QR do site",
                style = headingStyle(30.sp, -0.02f), color = z.text, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
            )
            Text(
                if (paired) "Uma sessão web está ativa, vinculada a este aparelho."
                else "Abra zelcar.app no computador e aponte a câmera para o código que aparece na tela.",
                style = bodyStyle(13.5.sp), color = z.text.copy(alpha = 0.7f),
            )
            Blueprint(Modifier.padding(vertical = 20.dp).fillMaxWidth().aspectRatio(1f).background(z.ground), padding = 0.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (scanning) QrScannerView({ vm.onPairCode(it) }, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxWidth(0.62f).aspectRatio(1f).border(1.dp, Color.White.copy(alpha = 0.55f))) {
                        Brackets(Modifier.fillMaxSize())
                        if (scanning) ScanLine()
                        if (paired) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ZIcon(ZIcons.Check, size = 64.dp, tint = Color(0xFF9FC8F1), strokeWidth = 1.5f)
                            }
                        }
                    }
                    Text(
                        if (scanning) "Procurando código…" else if (paired) "Vinculado" else "Câmera pronta · toque em escanear",
                        style = bodyStyle(12.sp), color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    )
                }
            }
            Column(Modifier.fillMaxWidth().border(1.dp, z.divider).padding(14.dp)) {
                Text("O QUE ACONTECE DEPOIS", style = kickerStyle(), color = z.text.copy(alpha = 0.5f))
                listOf(
                    "O navegador passa a mostrar os mesmos carros deste aparelho.",
                    "Edições feitas em qualquer lado aparecem no outro ao abrir ou puxar para atualizar.",
                    "Você pode revogar a sessão web a qualquer momento em Configurações.",
                ).forEachIndexed { i, t ->
                    Text("${i + 1}. $t", style = bodyStyle(12.5.sp), color = z.text.copy(alpha = 0.8f), modifier = Modifier.padding(top = 6.dp))
                }
            }
            ZButton(
                text = when { paired -> "Revogar sessão"; scanning -> "Cancelar leitura"; else -> "Escanear agora" },
                onClick = { if (paired) vm.askRevoke() else if (scanning) vm.stopScan() else scan() },
                modifier = Modifier.padding(top = 18.dp).fillMaxWidth(),
                kind = if (paired) BtnKind.Secondary else BtnKind.Primary,
            )
        }
    }
}

@Composable
private fun Brackets(modifier: Modifier) {
    val c = Color(0xFF9FC8F1)
    Canvas(modifier) {
        val len = 26.dp.toPx()
        val w = 3.dp.toPx()
        val h = w / 2
        val x1 = size.width
        val y1 = size.height
        fun l(a: Offset, b: Offset) = drawLine(c, a, b, w)
        l(Offset(0f, h), Offset(len, h)); l(Offset(h, 0f), Offset(h, len))
        l(Offset(x1, h), Offset(x1 - len, h)); l(Offset(x1 - h, 0f), Offset(x1 - h, len))
        l(Offset(0f, y1 - h), Offset(len, y1 - h)); l(Offset(h, y1), Offset(h, y1 - len))
        l(Offset(x1, y1 - h), Offset(x1 - len, y1 - h)); l(Offset(x1 - h, y1), Offset(x1 - h, y1 - len))
    }
}

@Composable
private fun ScanLine() {
    val t = rememberInfiniteTransition(label = "scan")
    val f by t.animateFloat(
        0.04f, 0.92f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scanY",
    )
    Canvas(Modifier.fillMaxSize()) {
        val y = size.height * f
        drawLine(Color(0xFF9FC8F1), Offset(size.width * 0.06f, y), Offset(size.width * 0.94f, y), 2.dp.toPx())
    }
}
