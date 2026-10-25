package com.zelcar.app.ui.sheets

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.zelcar.app.data.TransferTicket
import com.zelcar.app.domain.issueLine
import com.zelcar.app.qr.QrGenerator
import com.zelcar.app.report.ReportGenerator
import com.zelcar.app.report.ReportOptions
import com.zelcar.app.ui.AppState
import com.zelcar.app.ui.PdfState
import com.zelcar.app.ui.REPORT_SECTIONS
import com.zelcar.app.ui.ReportRange
import com.zelcar.app.ui.ZelcarViewModel
import com.zelcar.app.ui.ZSheet
import com.zelcar.app.ui.components.Blueprint
import com.zelcar.app.ui.components.Divider
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.SectionLabel
import com.zelcar.app.ui.components.SegControl
import com.zelcar.app.ui.components.SheetHeader
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.ZCheckBox
import com.zelcar.app.ui.components.ZRow
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import kotlinx.coroutines.delay
import java.io.File

// ─────────────────────────── Transferência via QR ───────────────────────────
@Composable
fun TransferSheet(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val car = state.activeCar
    val blocks = state.ui.transfer
    var ticket by remember { mutableStateOf<TransferTicket?>(null) }
    var left by remember { mutableIntStateOf(0) }

    // Gera um QR novo sempre que a seleção muda ou o anterior expira (uso único, 10 min).
    LaunchedEffect(car?.id, blocks) {
        if (car != null) {
            while (true) {
                val t = vm.createTicket(car.id, blocks)
                ticket = t
                left = t.ttlSeconds
                while (left > 0) {
                    delay(1000)
                    left -= 1
                }
            }
        }
    }
    val qr = remember(ticket) {
        ticket?.let { QrGenerator.bitmap(it.uri, 512, 0xFF10304F.toInt(), 0xFFFFFFFF.toInt()).asImageBitmap() }
    }
    val labels = listOf(
        "dados" to "Dados do veículo e quilometragem",
        "historico" to "Histórico de manutenção (${state.carNotes.size} registros)",
        "gastos" to "Gastos e valores pagos",
        "notas" to "Notas do diário de bordo",
    )
    ZSheet(onClose = { vm.back() }) { close ->
        SheetHeader(
            "Transferência", "Transferir o ${car?.name?.substringAfterLast(' ') ?: "carro"}",
            "O comprador escaneia este código e recebe tudo que você marcar. O carro sai da sua garagem e os lembretes dele param.",
        )
        SectionLabel("O que vai junto · ${blocks.size} de 4", top = 18.dp, bottom = 6.dp)
        Divider()
        labels.forEach { (k, l) ->
            ZRow(onClick = { vm.toggleTransfer(k) }) {
                ZCheckBox(if (k in blocks) "✓" else "", k in blocks, size = 20.dp)
                Text(l, style = bodyStyle(14.5.sp), color = z.text.copy(alpha = if (k in blocks) 1f else 0.72f))
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Blueprint(Modifier.size(168.dp).background(Color.White), padding = 12.dp) {
                val b = qr
                if (b != null) Image(b, "QR Code de transferência", Modifier.fillMaxWidth(), filterQuality = FilterQuality.None)
            }
            Muted(
                "Expira em %02d:%02d · uso único".format(left / 60, left % 60),
                size = 11f, alpha = 0.55f, modifier = Modifier.padding(top = 10.dp),
            )
        }
        SheetButtons("Confirmar", close) { vm.askTransfer() }
    }
}

// ─────────────────────────── Relatório em PDF ───────────────────────────
private fun sharePdf(ctx: Context, file: File) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(send, "Compartilhar relatório"))
}

@Composable
fun ReportSheet(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val ctx = LocalContext.current
    val ui = state.ui
    val car = state.activeCar
    val pages = 2 + ui.report.size
    val fileName = car?.let { ReportGenerator.fileName(it, ReportOptions(ui.report, ui.reportRange == ReportRange.SixMonths)) } ?: ""

    LaunchedEffect(ui.reportFile) {
        val f = ui.reportFile
        if (ui.pdf == PdfState.Ready && f != null) sharePdf(ctx, f)
    }
    ZSheet(onClose = { vm.back() }) { close ->
        SheetHeader(
            "Relatório", "Montar o PDF",
            "Escolha o que entra. Serve para vender o carro, prestar contas ou levar ao mecânico.",
        )
        SegControl(
            ReportRange.entries.map { it.label }, ReportRange.entries.indexOf(ui.reportRange),
            { vm.setReportRange(ReportRange.entries[it]) }, Modifier.padding(top = 16.dp),
        )
        SectionLabel("Seções · ${ui.report.size} de 5", top = 18.dp, bottom = 6.dp)
        Divider()
        REPORT_SECTIONS.forEach { (k, l) ->
            ZRow(onClick = { vm.toggleReport(k) }) {
                ZCheckBox(if (k in ui.report) "✓" else "", k in ui.report, size = 20.dp)
                Text(l, style = bodyStyle(14.5.sp), color = z.text.copy(alpha = if (k in ui.report) 1f else 0.72f))
            }
        }
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.size(width = 72.dp, height = 96.dp).border(1.dp, z.divider).padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.fillMaxWidth(0.7f).height(7.dp).background(z.accent))
                Box(Modifier.fillMaxWidth().height(3.dp).background(z.n300))
                Box(Modifier.fillMaxWidth(0.88f).height(3.dp).background(z.n300))
                Box(Modifier.fillMaxWidth().height(20.dp).padding(top = 4.dp).background(z.a200))
                Box(Modifier.fillMaxWidth(0.76f).height(3.dp).background(z.n300))
                Box(Modifier.fillMaxWidth(0.9f).height(3.dp).background(z.n300))
            }
            Column {
                Text("Prévia · $pages páginas", style = headingStyle(19.sp), color = z.text)
                Muted(fileName, size = 12f, alpha = 0.65f)
                Muted("Compartilhado pelo Android Sharesheet.", size = 11f, alpha = 0.5f, modifier = Modifier.padding(top = 4.dp))
            }
        }
        val label = when {
            ui.pdf == PdfState.Generating -> "Gerando…"
            ui.pdf == PdfState.Ready -> "Compartilhar"
            state.isPro -> "Gerar PDF"
            else -> "Gerar PDF (Pro)"
        }
        SheetButtons(label, close) {
            val f = ui.reportFile
            if (ui.pdf == PdfState.Ready && f != null) sharePdf(ctx, f) else vm.generatePdf()
        }
    }
}

// ─────────────────────────── Seletor de estado do item ───────────────────────────
@Composable
fun CheckPickSheet(state: AppState, vm: ZelcarViewModel) {
    val z = Z.c
    val item = state.data.checks.firstOrNull { it.id == state.ui.pickId }
    ZSheet(onClose = { vm.closePick() }, roundedTop = true) { _ ->
        Text(
            item?.label ?: "", style = bodyStyle(14.sp), color = z.text.copy(alpha = 0.65f),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        val cur = item?.state ?: "unset"
        listOf(
            Triple("ok", "OK", "Conferido, sem ressalvas"),
            Triple(
                "issue", if (cur == "issue") "Editar problema" else "Problema",
                if (cur == "issue" && item != null) issueLine(item) else "Em seguida você detalha o que encontrou",
            ),
            Triple("unset", "Não verificado", "Ainda não conferiu este item"),
        ).forEach { (k, label, sub) ->
            Row(
                Modifier.fillMaxWidth().height(64.dp).clickable { vm.choose(k) }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier.size(20.dp).border(2.dp, if (cur == k) z.accent else z.n400, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (cur == k) Box(Modifier.size(10.dp).background(z.accent, CircleShape)) }
                Column(Modifier.weight(1f)) {
                    Text(label, style = bodyStyle(16.sp), color = z.text)
                    Text(sub, style = bodyStyle(12.5.sp), color = z.text.copy(alpha = 0.6f), maxLines = 1)
                }
            }
        }
    }
}
