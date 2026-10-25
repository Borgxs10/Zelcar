package com.zelcar.app.report

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.zelcar.app.R
import com.zelcar.app.data.CarEntity
import com.zelcar.app.data.ExpenseEntity
import com.zelcar.app.data.NoteEntity
import com.zelcar.app.data.ReminderEntity
import com.zelcar.app.data.splitPaths
import com.zelcar.app.domain.brl
import com.zelcar.app.domain.costPerKm
import com.zelcar.app.domain.dmyy
import com.zelcar.app.domain.nkm
import com.zelcar.app.domain.rankReminders
import com.zelcar.app.domain.toDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Opções escolhidas na folha "Montar o PDF" (RF11). */
data class ReportOptions(
    val sections: Set<String>,
    val sixMonthsOnly: Boolean,
)

/** Gera o relatório A4 com android.graphics.pdf.PdfDocument e devolve o arquivo em cache/reports. */
object ReportGenerator {
    private const val W = 595
    private const val H = 842
    private const val M = 40

    suspend fun generate(
        context: Context,
        car: CarEntity,
        expenses: List<ExpenseEntity>,
        reminders: List<ReminderEntity>,
        notes: List<NoteEntity>,
        options: ReportOptions,
        now: LocalDate,
    ): File = withContext(Dispatchers.IO) {
        val cutoff = if (options.sixMonthsOnly) now.minusMonths(6) else LocalDate.MIN
        val exp = expenses.filter { it.epochDay.toDate() >= cutoff }.sortedByDescending { it.epochDay }
        val nts = notes.filter { it.epochDay.toDate() >= cutoff }.sortedByDescending { it.epochDay }

        val heading = ResourcesCompat.getFont(context, R.font.barlow_condensed_semibold) ?: Typeface.DEFAULT_BOLD
        val body = ResourcesCompat.getFont(context, R.font.barlow_regular) ?: Typeface.DEFAULT
        val w = PageWriter(heading, body)

        // Cabeçalho
        w.text("ZELCAR · RELATÓRIO DO VEÍCULO", 10f, body, Color.rgb(31, 90, 158), spacing = 1.4f)
        w.gap(6f)
        w.text(car.name, 30f, heading, Color.rgb(16, 32, 47))
        val sub = listOf(car.year.toString(), car.color.takeIf { it != "—" }, car.plate.takeIf { it != "—" }, "${nkm(car.km)} km")
            .filterNotNull().joinToString(" · ")
        w.text(sub, 12f, body, Color.GRAY)
        w.text("Gerado em ${now.dmyy()}" + if (options.sixMonthsOnly) " · últimos 6 meses" else " · histórico completo", 10f, body, Color.GRAY)
        w.rule()

        if ("ficha" in options.sections) {
            w.section("Ficha do veículo")
            w.kv("Modelo", car.name)
            w.kv("Ano", car.year.toString())
            w.kv("Cor", car.color)
            w.kv("Placa", car.plate)
            w.kv("Quilometragem", "${nkm(car.km)} km")
        }

        if ("manut" in options.sections) {
            w.section("Histórico de manutenção")
            val maint = exp.filter { it.cat == "Manutenção" }
            if (maint.isEmpty()) w.text("Nenhum serviço de manutenção no período.", 11f, body, Color.GRAY)
            maint.forEach { w.kv("${it.epochDay.toDate().dmyy()} · ${nkm(it.km)} km", "${it.title} — ${brl(it.value)}") }
            val ranked = rankReminders(reminders, car.km, now)
            if (ranked.isNotEmpty()) {
                w.gap(8f)
                w.text("PRÓXIMOS LEMBRETES", 9f, body, Color.rgb(31, 90, 158), spacing = 1.2f)
                ranked.forEach { w.kv(it.reminder.label, "${it.tag} · ${it.sub}") }
            }
            if (nts.isNotEmpty()) {
                w.gap(8f)
                w.text("DIÁRIO DE BORDO", 9f, body, Color.rgb(31, 90, 158), spacing = 1.2f)
                nts.forEach { n ->
                    w.text("${n.epochDay.toDate().dmyy()} · ${nkm(n.km)} km — ${n.title}", 12f, heading, Color.rgb(16, 32, 47))
                    if (n.body.isNotBlank()) w.paragraph(n.body, 11f, body, Color.DKGRAY)
                    w.gap(4f)
                }
            }
        }

        if ("gastos" in options.sections) {
            w.section("Gastos por categoria")
            val byCat = exp.groupBy { it.cat }.mapValues { (_, v) -> v.sumOf { it.value } }.toList().sortedByDescending { it.second }
            val total = byCat.sumOf { it.second }
            val max = byCat.maxOfOrNull { it.second } ?: 1.0
            byCat.forEach { (cat, v) -> w.bar(cat, brl(v), (v / max).toFloat()) }
            w.gap(4f)
            w.kv("Total no período", brl(total))
        }

        if ("custo" in options.sections) {
            w.section("Custo por km e projeções")
            val cpk = costPerKm(expenses, car.km, now)
            val q = expenses.filter { it.epochDay.toDate() >= now.minusMonths(3) }.sumOf { it.value }
            w.kv("Custo por km (90 dias)", brl(cpk))
            w.kv("Média mensal (90 dias)", brl(q / 3))
            w.kv("Projeção anual", brl(q / 3 * 12))
        }

        if ("fotos" in options.sections) {
            w.section("Fotos do diário de bordo")
            val files = nts.flatMap { splitPaths(it.photoPaths) }.filter { File(it).exists() }
            if (files.isEmpty()) w.text("Nenhuma foto anexada às notas.", 11f, body, Color.GRAY)
            else w.photos(files)
        }

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName(car, options))
        file.outputStream().use { w.finish(it) }
        file
    }

    fun fileName(car: CarEntity, options: ReportOptions): String =
        car.name.replace(' ', '-') + "_" + (if (options.sixMonthsOnly) "ultimos6m" else "completo") + ".pdf"

    private class PageWriter(private val heading: Typeface, private val body: Typeface) {
        private val doc = PdfDocument()
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = M.toFloat()
        private var number = 0

        init { newPage() }

        private fun newPage() {
            page?.let { doc.finishPage(it) }
            number++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, number).create())
            page = p
            canvas = p.canvas
            y = M.toFloat()
        }

        private fun ensure(h: Float) { if (y + h > H - M) newPage() }

        private fun paint(size: Float, face: Typeface, color: Int, spacing: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; typeface = face; this.color = color; letterSpacing = spacing / size
        }

        fun gap(h: Float) { y += h }

        fun text(s: String, size: Float, face: Typeface, color: Int, spacing: Float = 0f) {
            ensure(size + 6)
            y += size
            canvas!!.drawText(s, M.toFloat(), y, paint(size, face, color, spacing))
            y += 4f
        }

        fun paragraph(s: String, size: Float, face: Typeface, color: Int) {
            val p = paint(size, face, color)
            val maxW = (W - 2 * M).toFloat()
            var line = StringBuilder()
            val lines = mutableListOf<String>()
            s.split(' ').forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (p.measureText(candidate) > maxW && line.isNotEmpty()) { lines += line.toString(); line = StringBuilder(word) }
                else line = StringBuilder(candidate)
            }
            if (line.isNotEmpty()) lines += line.toString()
            lines.forEach { l ->
                ensure(size + 4)
                y += size
                canvas!!.drawText(l, M.toFloat(), y, p)
                y += 3f
            }
        }

        fun rule() {
            ensure(16f)
            y += 8f
            canvas!!.drawLine(M.toFloat(), y, (W - M).toFloat(), y, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f })
            y += 8f
        }

        fun section(title: String) {
            ensure(60f)
            y += 14f
            text(title.uppercase(), 11f, body, Color.rgb(31, 90, 158), spacing = 1.4f)
            canvas!!.drawLine(M.toFloat(), y, (W - M).toFloat(), y, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.6f })
            y += 6f
        }

        fun kv(k: String, v: String) {
            ensure(18f)
            y += 12f
            val kp = paint(11f, body, Color.GRAY)
            val vp = paint(11.5f, heading, Color.rgb(16, 32, 47))
            canvas!!.drawText(k, M.toFloat(), y, kp)
            val vw = vp.measureText(v)
            canvas!!.drawText(v, (W - M) - vw, y, vp)
            y += 5f
        }

        fun bar(label: String, value: String, frac: Float) {
            ensure(26f)
            y += 11f
            canvas!!.drawText(label, M.toFloat(), y, paint(11f, body, Color.rgb(16, 32, 47)))
            val vp = paint(11.5f, heading, Color.rgb(16, 32, 47))
            canvas!!.drawText(value, (W - M) - vp.measureText(value), y, vp)
            y += 5f
            val full = (W - 2 * M).toFloat()
            canvas!!.drawRect(M.toFloat(), y, M + full, y + 4f, Paint().apply { color = Color.rgb(231, 231, 234) })
            canvas!!.drawRect(M.toFloat(), y, M + full * frac.coerceIn(0f, 1f), y + 4f, Paint().apply { color = Color.rgb(59, 130, 214) })
            y += 9f
        }

        fun photos(paths: List<String>) {
            val size = 120
            val gap = 8
            val perRow = (W - 2 * M + gap) / (size + gap)
            paths.chunked(perRow).forEach { row ->
                ensure(size + gap.toFloat())
                row.forEachIndexed { i, path ->
                    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 }) ?: return@forEachIndexed
                    val dst = Rect(M + i * (size + gap), y.toInt(), M + i * (size + gap) + size, y.toInt() + size)
                    val side = minOf(bmp.width, bmp.height)
                    val src = Rect((bmp.width - side) / 2, (bmp.height - side) / 2, (bmp.width + side) / 2, (bmp.height + side) / 2)
                    canvas!!.drawBitmap(bmp, src, dst, null)
                }
                y += size + gap
            }
        }

        fun finish(out: java.io.OutputStream) {
            page?.let { doc.finishPage(it) }
            doc.writeTo(out)
            doc.close()
        }
    }
}
