package com.zelcar.app.domain

import com.zelcar.app.data.CarEntity
import com.zelcar.app.data.CheckItemEntity
import com.zelcar.app.data.ExpenseEntity
import com.zelcar.app.data.ReminderEntity
import com.zelcar.app.data.photoCount
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

// ─────────────────────────────────────────────────────────────
// Formatação (pt-BR)
// ─────────────────────────────────────────────────────────────
private val PT_BR = Locale("pt", "BR")

private fun nf(minFrac: Int, maxFrac: Int) = NumberFormat.getNumberInstance(PT_BR).apply {
    minimumFractionDigits = minFrac
    maximumFractionDigits = maxFrac
}

fun brl(v: Double): String = "R$ " + nf(2, 2).format(v)
fun brl0(v: Double): String = "R$ " + nf(0, 0).format(Math.round(v))
fun nkm(v: Int): String = nf(0, 0).format(v)

val MONTH_SHORT = listOf("JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ")
val MONTH_FULL = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
)

fun LocalDate.dmy(): String = "%02d/%02d".format(dayOfMonth, monthValue)
fun LocalDate.dmyy(): String = dmy() + "/" + year
fun Long.toDate(): LocalDate = LocalDate.ofEpochDay(this)

/** "1.234,50" -> 1234.5 (entrada no padrão pt-BR). */
fun parseNum(s: String): Double {
    val clean = s.replace(".", "").replace(",", ".").replace(Regex("[^0-9.\\-]"), "")
    return clean.toDoubleOrNull() ?: 0.0
}

fun parseInt2(s: String): Int = s.filter { it.isDigit() }.toIntOrNull() ?: 0

fun today(): LocalDate = LocalDate.now()

// ─────────────────────────────────────────────────────────────
// Lembretes (RF09, RF19, RF20, RN04)
// ─────────────────────────────────────────────────────────────
data class RankedReminder(
    val reminder: ReminderEntity,
    val days: Int,
    val sub: String,
    val tag: String,
    val remainKm: Int?,
    val pct: Int,
) {
    val isKm get() = reminder.type == "km"
    val level: Level get() = when {
        days < 0 -> Level.Overdue
        days <= 14 -> Level.Soon
        days <= 45 -> Level.Near
        else -> Level.Ok
    }

    enum class Level { Overdue, Soon, Near, Ok }
}

/** ~30 km/dia é a estimativa do protótipo para converter km em dias. */
private const val KM_PER_DAY = 30

fun rankReminders(list: List<ReminderEntity>, carKm: Int, now: LocalDate = today()): List<RankedReminder> =
    list.map { r ->
        val days: Int
        val sub: String
        val pct: Double
        var remainKm: Int? = null
        if (r.type == "km") {
            val next = r.lastKm + r.everyKm
            val remain = next - carKm
            remainKm = remain
            days = Math.round(remain / KM_PER_DAY.toDouble()).toInt()
            sub = "a cada ${nkm(r.everyKm)} km · " +
                (if (remain >= 0) "faltam ${nkm(remain)}" else "${nkm(-remain)} km atrasado")
            pct = ((carKm - r.lastKm).toDouble() / r.everyKm.coerceAtLeast(1) * 100).coerceIn(0.0, 100.0)
        } else {
            val due = r.dueEpochDay.toDate()
            days = ChronoUnit.DAYS.between(now, due).toInt()
            sub = "data fixa · ${due.dmyy()}"
            val span = (r.everyMonths.takeIf { it > 0 } ?: 12) * 30
            pct = ((span - days).toDouble() / span * 100).coerceIn(0.0, 100.0)
        }
        val tag = when {
            days < 0 -> "Atrasado"
            days <= 14 -> "$days dias"
            days <= 45 -> "Perto"
            else -> "Em dia"
        }
        RankedReminder(r, days, sub, tag, remainKm, Math.round(pct).toInt())
    }.sortedBy { it.days }

// ─────────────────────────────────────────────────────────────
// Gastos (RF08, RF27)
// ─────────────────────────────────────────────────────────────
enum class Period(val label: String) { Month("Mês"), Quarter("Trimestre"), Year("Ano") }

val EXPENSE_CATEGORIES = listOf("Combustível", "Manutenção", "Documentos", "Estética", "Seguro", "Outros")

fun periodExpenses(all: List<ExpenseEntity>, period: Period, now: LocalDate = today()): List<ExpenseEntity> =
    all.filter { e ->
        val d = e.epochDay.toDate()
        when (period) {
            Period.Year -> d.year == now.year
            Period.Month -> d.year == now.year && d.monthValue == now.monthValue
            Period.Quarter -> d.year == now.year && d.monthValue > now.monthValue - 3 && d.monthValue <= now.monthValue
        }
    }.sortedByDescending { it.epochDay }

fun monthTotal(all: List<ExpenseEntity>, year: Int, month: Int): Double =
    all.filter { val d = it.epochDay.toDate(); d.year == year && d.monthValue == month }.sumOf { it.value }

/** Custo por km nos últimos 3 meses (RF27). */
fun costPerKm(all: List<ExpenseEntity>, carKm: Int, now: LocalDate = today()): Double {
    val q = periodExpenses(all, Period.Quarter, now)
    val total = q.sumOf { it.value }
    val minKm = q.minOfOrNull { it.km } ?: carKm
    val span = (carKm - minOf(minKm, carKm)).coerceAtLeast(1)
    return total / span
}

data class MonthBar(val month: Int, val label: String, val value: Double, val current: Boolean)

/** Últimos 6 meses terminando no mês atual (meses de anos anteriores são tratados como valor 0). */
fun lastSixMonths(all: List<ExpenseEntity>, now: LocalDate = today()): List<MonthBar> =
    (5 downTo 0).map { i ->
        val ref = now.withDayOfMonth(1).minusMonths(i.toLong())
        MonthBar(ref.monthValue, MONTH_SHORT[ref.monthValue - 1], monthTotal(all, ref.year, ref.monthValue), i == 0)
    }

// ─────────────────────────────────────────────────────────────
// Checklist de compra (RF05, RF22–RF28)
// ─────────────────────────────────────────────────────────────
val CHECK_GROUPS = listOf(
    "doc" to "Documentação",
    "mec" to "Mecânica",
    "est" to "Estrutura e lataria",
    "bur" to "Burocracia de transferência",
)

val CHECK_TEMPLATE: List<Pair<String, String>> = listOf(
    "doc" to "Débitos de IPVA e multas",
    "doc" to "Alienação fiduciária",
    "doc" to "Histórico de sinistro, leilão e roubo",
    "doc" to "CRLV e CRV em nome do vendedor",
    "doc" to "Recall pendente no fabricante",
    "doc" to "Chassi e motor batendo com o documento",
    "mec" to "Partida com o motor frio",
    "mec" to "Nível e cor do óleo",
    "mec" to "Ruído na suspensão",
    "mec" to "Câmbio e embreagem",
    "mec" to "Freios e pastilhas",
    "mec" to "Ar-condicionado gelando",
    "mec" to "Bateria e alternador",
    "est" to "Alinhamento das frestas",
    "est" to "Pintura e sinais de repintura",
    "est" to "Assoalho e caixa de roda",
    "est" to "Pneus e desgaste",
    "est" to "Vidros com o mesmo código",
    "bur" to "Vistoria veicular",
    "bur" to "Taxa de transferência no DETRAN",
    "bur" to "Comunicação de venda",
    "bur" to "Reconhecimento de firma no CRV",
    "bur" to "Seguro transferido",
)

fun newChecklist(carId: String): List<CheckItemEntity> = CHECK_TEMPLATE.mapIndexed { i, (g, label) ->
    CheckItemEntity(id = "$carId-c$i", carId = carId, grp = g, label = label, position = i)
}

fun severityLabel(s: String) = when (s) { "leve" -> "Leve"; "grave" -> "Grave"; else -> "Atenção" }

fun issueLine(c: CheckItemEntity): String {
    if (c.state != "issue") return ""
    val parts = mutableListOf(severityLabel(c.severity))
    if (c.note.isNotBlank()) parts += c.note
    val photos = photoCount(c.photos, c.photoPaths)
    if (photos > 0) parts += photos.toString() + if (photos > 1) " fotos" else " foto"
    if (c.cost > 0) parts += "~" + brl0(c.cost)
    if (c.note.isBlank() && photos == 0) parts += "toque para descrever"
    return parts.joinToString(" · ")
}

// ─────────────────────────────────────────────────────────────
// Planos (RF03, RN07)
// ─────────────────────────────────────────────────────────────
const val FREE_CAR_LIMIT = 2

fun ownedCars(cars: List<CarEntity>) = cars.filter { !it.buying }
