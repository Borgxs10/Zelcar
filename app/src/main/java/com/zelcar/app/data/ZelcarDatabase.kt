package com.zelcar.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ZelcarDao {
    // ── Carros ──
    @Query("SELECT * FROM cars ORDER BY createdAt ASC")
    fun cars(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars ORDER BY createdAt ASC")
    suspend fun carsOnce(): List<CarEntity>

    @Upsert suspend fun upsertCar(car: CarEntity)
    @Delete suspend fun deleteCar(car: CarEntity)
    @Query("DELETE FROM cars WHERE id = :id") suspend fun deleteCarById(id: String)
    @Query("UPDATE cars SET km = :km WHERE id = :id") suspend fun setKm(id: String, km: Int)

    // ── Gastos ──
    @Query("SELECT * FROM expenses ORDER BY epochDay DESC, rowid DESC")
    fun expenses(): Flow<List<ExpenseEntity>>

    @Upsert suspend fun upsertExpense(e: ExpenseEntity)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun deleteExpense(id: String)

    // ── Lembretes ──
    @Query("SELECT * FROM reminders")
    fun reminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun remindersOnce(): List<ReminderEntity>

    @Upsert suspend fun upsertReminder(r: ReminderEntity)
    @Upsert suspend fun upsertReminders(list: List<ReminderEntity>)
    @Query("DELETE FROM reminders WHERE id = :id") suspend fun deleteReminder(id: String)

    // ── Notas ──
    @Query("SELECT * FROM notes ORDER BY epochDay DESC, rowid DESC")
    fun notes(): Flow<List<NoteEntity>>

    @Upsert suspend fun upsertNote(n: NoteEntity)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: String)

    // ── Checklist ──
    @Query("SELECT * FROM check_items ORDER BY position ASC")
    fun checkItems(): Flow<List<CheckItemEntity>>

    @Upsert suspend fun upsertCheckItem(c: CheckItemEntity)
    @Upsert suspend fun upsertCheckItems(list: List<CheckItemEntity>)
}

@Database(
    entities = [CarEntity::class, ExpenseEntity::class, ReminderEntity::class, NoteEntity::class, CheckItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ZelcarDatabase : RoomDatabase() {
    abstract fun dao(): ZelcarDao

    companion object {
        @Volatile private var instance: ZelcarDatabase? = null

        fun get(context: Context): ZelcarDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ZelcarDatabase::class.java, "zelcar.db")
                .build().also { instance = it }
        }
    }
}
