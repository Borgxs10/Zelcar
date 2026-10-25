package com.zelcar.app.data

import androidx.room.withTransaction
import com.zelcar.app.domain.newChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Tudo que um carro leva consigo — usado para transferir e para desfazer a transferência. */
data class CarSnapshot(
    val car: CarEntity,
    val expenses: List<ExpenseEntity>,
    val reminders: List<ReminderEntity>,
    val notes: List<NoteEntity>,
    val checks: List<CheckItemEntity>,
)

class Repository(private val db: ZelcarDatabase, val prefs: Prefs) {
    private val dao = db.dao()

    val cars: Flow<List<CarEntity>> = dao.cars()
    val expenses: Flow<List<ExpenseEntity>> = dao.expenses()
    val reminders: Flow<List<ReminderEntity>> = dao.reminders()
    val notes: Flow<List<NoteEntity>> = dao.notes()
    val checks: Flow<List<CheckItemEntity>> = dao.checkItems()
    val settings: Flow<Settings> = prefs.settings

    // ── Carros ──
    suspend fun addOwnedCar(car: CarEntity, makeMain: Boolean) {
        db.withTransaction {
            dao.upsertCar(car)
            val s = prefs.current()
            if (s.activeCarId == null) prefs.setActiveCar(car.id)
            val hasMain = s.mainCarId != null && dao.carsOnce().any { it.id == s.mainCarId && !it.buying }
            if (makeMain || !hasMain) prefs.setMainCar(car.id)
        }
    }

    suspend fun addBuyingCar(car: CarEntity) {
        db.withTransaction {
            dao.upsertCar(car)
            dao.upsertCheckItems(newChecklist(car.id))
            if (prefs.current().activeCarId == null) prefs.setActiveCar(car.id)
        }
    }

    suspend fun setKm(carId: String, km: Int) = dao.setKm(carId, km)

    suspend fun snapshot(carId: String, cars: List<CarEntity>, expenses: List<ExpenseEntity>,
                         reminders: List<ReminderEntity>, notes: List<NoteEntity>, checks: List<CheckItemEntity>): CarSnapshot? {
        val car = cars.firstOrNull { it.id == carId } ?: return null
        return CarSnapshot(
            car,
            expenses.filter { it.carId == carId },
            reminders.filter { it.carId == carId },
            notes.filter { it.carId == carId },
            checks.filter { it.carId == carId },
        )
    }

    /** Remove o carro (e, em cascata, seus registros). Ajusta carro ativo/principal. */
    suspend fun removeCar(carId: String) {
        db.withTransaction {
            dao.deleteCarById(carId)
            val left = dao.carsOnce()
            val s = prefs.current()
            if (s.activeCarId == carId) prefs.setActiveCar(left.firstOrNull { !it.buying }?.id ?: left.firstOrNull()?.id)
            if (s.mainCarId == carId) prefs.setMainCar(left.firstOrNull { !it.buying }?.id)
        }
    }

    suspend fun restore(snap: CarSnapshot, wasActive: Boolean, wasMain: Boolean) {
        db.withTransaction {
            dao.upsertCar(snap.car)
            snap.expenses.forEach { dao.upsertExpense(it) }
            dao.upsertReminders(snap.reminders)
            snap.notes.forEach { dao.upsertNote(it) }
            dao.upsertCheckItems(snap.checks)
            if (wasActive) prefs.setActiveCar(snap.car.id)
            if (wasMain) prefs.setMainCar(snap.car.id)
        }
    }

    // ── Gastos / notas / lembretes / checklist ──
    suspend fun upsertExpense(e: ExpenseEntity) = dao.upsertExpense(e)
    suspend fun deleteExpense(id: String) = dao.deleteExpense(id)
    suspend fun upsertNote(n: NoteEntity) = dao.upsertNote(n)
    suspend fun deleteNote(id: String) = dao.deleteNote(id)
    suspend fun upsertReminder(r: ReminderEntity) = dao.upsertReminder(r)
    suspend fun upsertReminders(list: List<ReminderEntity>) = dao.upsertReminders(list)
    suspend fun deleteReminder(id: String) = dao.deleteReminder(id)
    suspend fun upsertCheckItem(c: CheckItemEntity) = dao.upsertCheckItem(c)

    // ── Dados de exemplo e reset ──
    suspend fun loadDemo(now: LocalDate) {
        val b = DemoData.build(now)
        db.withTransaction {
            b.cars.forEach { dao.upsertCar(it) }
            b.expenses.forEach { dao.upsertExpense(it) }
            dao.upsertReminders(b.reminders)
            b.notes.forEach { dao.upsertNote(it) }
            dao.upsertCheckItems(b.checks)
            prefs.setActiveCar(DemoData.CIVIC)
            prefs.setMainCar(DemoData.CIVIC)
            prefs.setOnboarded(true)
        }
    }

    /** RF10: apaga tudo do aparelho (carros, gastos, lembretes, notas) e derruba a sessão web. */
    suspend fun eraseAll() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        prefs.clearAll()
    }
}
