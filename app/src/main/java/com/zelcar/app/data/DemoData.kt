package com.zelcar.app.data

import com.zelcar.app.domain.CHECK_TEMPLATE
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Dados de exemplo idênticos aos do protótipo (Honda Civic, Fiat Argo, Toyota Corolla em compra).
 * As datas do protótipo são ancoradas em 07/09/2026 e deslocadas para hoje, para nada ficar no futuro.
 */
object DemoData {
    private val REF = LocalDate.of(2026, 9, 7)

    class Bundle(
        val cars: List<CarEntity>,
        val expenses: List<ExpenseEntity>,
        val reminders: List<ReminderEntity>,
        val notes: List<NoteEntity>,
        val checks: List<CheckItemEntity>,
    )

    const val CIVIC = "civic"
    const val ARGO = "argo"
    const val COROLLA = "corolla"

    fun build(now: LocalDate): Bundle {
        val shift = ChronoUnit.DAYS.between(REF, now)
        fun d(y: Int, m: Int, day: Int): Long = LocalDate.of(y, m, day).toEpochDay() + shift
        var n = 0L
        val t0 = System.currentTimeMillis()

        val cars = listOf(
            CarEntity(CIVIC, "Honda Civic EXL", 2019, "QRZ-4E17", "Prata", 78412, createdAt = t0 + n++),
            CarEntity(ARGO, "Fiat Argo Drive", 2022, "RFX-1A09", "Branco", 31980, createdAt = t0 + n++),
            CarEntity(COROLLA, "Toyota Corolla 2020", 2020, "—", "Preto", 64300, buying = true, createdAt = t0 + n++),
        )

        fun e(id: String, cat: String, title: String, y: Int, m: Int, day: Int, km: Int, v: Double, extra: String = "") =
            ExpenseEntity("demo-$id", CIVIC, cat, title, d(y, m, day), km, v, extra)

        val expenses = listOf(
            e("e1", "Combustível", "Shell BR-116", 2026, 9, 6, 78320, 287.4, "41,2 L"),
            e("e2", "Combustível", "Ipiranga Av. Brasil", 2026, 9, 1, 77980, 396.8, "52,0 L"),
            e("e3", "Manutenção", "Pastilhas de freio · Auto Center Vila", 2026, 9, 4, 78100, 340.0),
            e("e4", "Documentos", "Licenciamento 2026", 2026, 9, 2, 77990, 148.6),
            e("e5", "Estética", "Lavagem completa", 2026, 9, 3, 78000, 68.0),
            e("e6", "Combustível", "Shell BR-116", 2026, 8, 22, 77140, 312.0, "40,8 L"),
            e("e7", "Manutenção", "Alinhamento e balanceamento", 2026, 8, 14, 77100, 180.0),
            e("e8", "Combustível", "Petrobras Centro", 2026, 8, 5, 76600, 289.9, "38,1 L"),
            e("e9", "Combustível", "Shell BR-116", 2026, 7, 28, 76300, 301.2, "39,5 L"),
            e("e10", "Manutenção", "Troca de óleo e filtro", 2026, 7, 10, 76010, 420.0),
            e("e11", "Seguro", "Parcela 6 de 12", 2026, 6, 15, 75200, 310.0),
            e("e12", "Combustível", "Ipiranga Av. Brasil", 2026, 6, 9, 74900, 295.0, "38,8 L"),
            e("e13", "Combustível", "Shell BR-116", 2026, 5, 18, 73800, 288.0, "37,9 L"),
            e("e14", "Combustível", "Petrobras Centro", 2026, 4, 12, 72400, 276.5, "36,4 L"),
            e("e15", "Manutenção", "Revisão de 70.000 km", 2026, 4, 3, 71900, 200.0),
            e("e16", "Documentos", "IPVA 2026 · cota única", 2026, 1, 20, 66400, 1840.0),
        )

        val reminders = listOf(
            ReminderEntity("demo-r1", CIVIC, "Troca de óleo e filtro", "km", 10000, 69000, 12),
            ReminderEntity("demo-r2", CIVIC, "Filtro de ar", "km", 15000, 70000, 24),
            ReminderEntity("demo-r3", CIVIC, "Alinhamento e balanceamento", "km", 10000, 69800, 12),
            ReminderEntity("demo-r4", CIVIC, "Licenciamento 2026", "date", everyMonths = 12, dueEpochDay = d(2026, 10, 12)),
            ReminderEntity("demo-r5", CIVIC, "Correia dentada", "km", 60000, 28000, 60),
            ReminderEntity("demo-r6", CIVIC, "Revisão da apólice do seguro", "date", everyMonths = 12, dueEpochDay = d(2026, 11, 3)),
        )

        val notes = listOf(
            NoteEntity(
                "demo-n1", CIVIC, d(2026, 8, 29), 77980, "Barulho na suspensão dianteira",
                "Estalo ao passar em lombada, lado esquerdo. Mecânico suspeita de bandeja. Orçamento de R$ 620 na Auto Center Vila.", 2,
            ),
            NoteEntity(
                "demo-n2", CIVIC, d(2026, 8, 14), 77140, "Pneus rodados no eixo traseiro",
                "Rodízio feito junto com o alinhamento. Pressão calibrada em 32 psi.", 0,
            ),
            NoteEntity(
                "demo-n3", CIVIC, d(2026, 7, 30), 76010, "Consumo caiu para 9,1 km/L",
                "Só rodando na cidade neste mês. Verificar filtro de ar na próxima revisão.", 0,
            ),
        )

        // Estados iniciais do protótipo, na mesma ordem de CHECK_TEMPLATE.
        val states = listOf(
            "ok", "issue", "unset", "ok", "unset", "ok",
            "ok", "ok", "issue", "unset", "ok", "ok", "unset",
            "ok", "issue", "unset", "ok", "unset",
            "ok", "unset", "unset", "unset", "unset",
        )
        val issueNotes = mapOf(
            1 to "Restrição ativa no Banco Itaú",
            8 to "Estalo no lado esquerdo em lombada",
            14 to "Porta traseira direita com tom diferente",
        )
        val checks = CHECK_TEMPLATE.mapIndexed { i, (g, label) ->
            val st = states[i]
            CheckItemEntity(
                id = "$COROLLA-c$i", carId = COROLLA, grp = g, label = label, state = st,
                note = issueNotes[i] ?: "",
                severity = if (st == "issue") (if (g == "doc") "grave" else "atencao") else "",
                cost = if (g == "mec" && st == "issue") 620.0 else 0.0,
                photos = if (i == 1) 2 else 0,
                position = i,
            )
        }
        return Bundle(cars, expenses, reminders, notes, checks)
    }
}
