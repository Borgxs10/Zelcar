package com.zelcar.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zelcar.app.ZelcarApp
import com.zelcar.app.domain.MONTH_FULL
import com.zelcar.app.domain.RankedReminder
import com.zelcar.app.domain.brl
import com.zelcar.app.domain.monthTotal
import com.zelcar.app.domain.rankReminders
import com.zelcar.app.domain.today
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Roda a cada 12 h (RNF07). Gera no máximo UM aviso agrupado por dia (RF19/RF20) com os lembretes
 * que vencem em até 14 dias ou já venceram, e — no dia 1º, se ligado — o resumo mensal de gastos.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ZelcarApp
        val repo = app.repository
        val settings = repo.settings.first()
        val now = today()
        val day = now.toEpochDay()
        val cars = repo.cars.first().filter { !it.buying }
        if (cars.isEmpty()) return Result.success()

        if (settings.notifReminders && settings.lastReminderDay != day) {
            val reminders = repo.reminders.first()
            val due = cars.flatMap { car ->
                rankReminders(reminders.filter { it.carId == car.id }, car.km, now)
                    .filter { it.days <= 14 }
                    .map { car.name to it }
            }
            if (due.isNotEmpty()) {
                val text = if (due.size == 1) "${due[0].second.reminder.label} · ${due[0].second.tag}"
                else "${due.size} lembretes de manutenção pedem atenção"
                val big = due.joinToString("\n") { (car, r) -> "${r.reminder.label} (${car.substringBefore(' ')}) — ${describe(r)}" }
                Notifications.post(applicationContext, Notifications.CH_REMINDERS, Notifications.ID_REMINDERS, "Zelcar", text, big)
                repo.prefs.setLastReminderDay(day)
            }
        }

        if (settings.notifSummary && now.dayOfMonth == 1 && settings.lastSummaryDay != day) {
            val prev = now.minusMonths(1)
            val expenses = repo.expenses.first()
            val main = cars.firstOrNull { it.id == settings.mainCarId } ?: cars.first()
            val total = monthTotal(expenses.filter { it.carId == main.id }, prev.year, prev.monthValue)
            Notifications.post(
                applicationContext, Notifications.CH_SUMMARY, Notifications.ID_SUMMARY,
                "Resumo de ${MONTH_FULL[prev.monthValue - 1]}", "${main.name}: ${brl(total)} em gastos.",
            )
            repo.prefs.setLastSummaryDay(day)
        }
        return Result.success()
    }

    private fun describe(r: RankedReminder): String =
        if (r.days < 0) "atrasado" else if (r.isKm) "faltam ${r.remainKm} km" else "em ${r.days} dias"

    companion object {
        private const val NAME = "zelcar-reminders"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
