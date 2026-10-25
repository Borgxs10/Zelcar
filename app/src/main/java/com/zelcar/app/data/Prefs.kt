package com.zelcar.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "zelcar_prefs")

enum class Plan { Free, Pro }

/** Preferências do aparelho (RF03 plano, RF10 configurações, RF12 pareamento). */
data class Settings(
    val plan: Plan = Plan.Free,
    val onboarded: Boolean = false,
    val activeCarId: String? = null,
    val mainCarId: String? = null,
    val notifReminders: Boolean = true,
    val notifSummary: Boolean = false,
    val notifSync: Boolean = true,
    val paired: Boolean = false,
    val lastReminderDay: Long = -1,
    val lastSummaryDay: Long = -1,
)

class Prefs(private val context: Context) {
    private object K {
        val plan = stringPreferencesKey("plan")
        val onboarded = booleanPreferencesKey("onboarded")
        val activeCar = stringPreferencesKey("active_car")
        val mainCar = stringPreferencesKey("main_car")
        val notifReminders = booleanPreferencesKey("notif_reminders")
        val notifSummary = booleanPreferencesKey("notif_summary")
        val notifSync = booleanPreferencesKey("notif_sync")
        val paired = booleanPreferencesKey("paired")
        val lastReminderDay = longPreferencesKey("last_reminder_day")
        val lastSummaryDay = longPreferencesKey("last_summary_day")
    }

    val settings: Flow<Settings> = context.store.data.map { p ->
        Settings(
            plan = if (p[K.plan] == "pro") Plan.Pro else Plan.Free,
            onboarded = p[K.onboarded] ?: false,
            activeCarId = p[K.activeCar],
            mainCarId = p[K.mainCar],
            notifReminders = p[K.notifReminders] ?: true,
            notifSummary = p[K.notifSummary] ?: false,
            notifSync = p[K.notifSync] ?: true,
            paired = p[K.paired] ?: false,
            lastReminderDay = p[K.lastReminderDay] ?: -1,
            lastSummaryDay = p[K.lastSummaryDay] ?: -1,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setPlan(plan: Plan) = edit { it[K.plan] = if (plan == Plan.Pro) "pro" else "free" }
    suspend fun setOnboarded(v: Boolean) = edit { it[K.onboarded] = v }
    suspend fun setActiveCar(id: String?) = edit { if (id == null) it.remove(K.activeCar) else it[K.activeCar] = id }
    suspend fun setMainCar(id: String?) = edit { if (id == null) it.remove(K.mainCar) else it[K.mainCar] = id }
    suspend fun setNotifReminders(v: Boolean) = edit { it[K.notifReminders] = v }
    suspend fun setNotifSummary(v: Boolean) = edit { it[K.notifSummary] = v }
    suspend fun setNotifSync(v: Boolean) = edit { it[K.notifSync] = v }
    suspend fun setPaired(v: Boolean) = edit { it[K.paired] = v }
    suspend fun setLastReminderDay(d: Long) = edit { it[K.lastReminderDay] = d }
    suspend fun setLastSummaryDay(d: Long) = edit { it[K.lastSummaryDay] = d }
    suspend fun clearAll() = edit { it.clear() }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.store.edit { block(it) }
    }
}
