package com.zelcar.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Veículo. `buying = true` é um carro em fase de compra (só checklist, sem placa/km). RF01, RF02. */
@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey val id: String,
    val name: String,
    val year: Int,
    val plate: String = "—",
    val color: String = "—",
    val km: Int = 0,
    val buying: Boolean = false,
    val price: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Gasto lançado em um carro (RF08). `epochDay` = LocalDate.toEpochDay(). */
@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(CarEntity::class, ["id"], ["carId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("carId")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val cat: String,
    val title: String,
    val epochDay: Long,
    val km: Int,
    val value: Double,
    val extra: String = "",
)

/**
 * Lembrete de manutenção (RF09, RF19, RF20).
 * type = "km"  -> usa [everyKm] e [lastKm]
 * type = "date" -> usa [dueEpochDay]
 * [everyMonths] vale para os dois: dispara no que vencer primeiro.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(CarEntity::class, ["id"], ["carId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("carId")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val label: String,
    val type: String,
    val everyKm: Int = 0,
    val lastKm: Int = 0,
    val everyMonths: Int = 12,
    val dueEpochDay: Long = 0,
)

/** Nota do diário de bordo (RF18). */
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(CarEntity::class, ["id"], ["carId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("carId")],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val epochDay: Long,
    val km: Int,
    val title: String,
    val body: String,
    /** Contagem de marcadores sem arquivo (dados de exemplo). Fotos reais ficam em [photoPaths]. */
    val photos: Int = 0,
    /** Caminhos dos arquivos em filesDir/photos, separados por \n. */
    val photoPaths: String = "",
)

/** Item do checklist de compra (RF05, RF22–RF28). state: ok | issue | unset. severity: leve | atencao | grave. */
@Entity(
    tableName = "check_items",
    foreignKeys = [ForeignKey(CarEntity::class, ["id"], ["carId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("carId")],
)
data class CheckItemEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val grp: String,
    val label: String,
    val state: String = "unset",
    val note: String = "",
    val severity: String = "",
    val cost: Double = 0.0,
    val photos: Int = 0,
    val photoPaths: String = "",
    val position: Int = 0,
)

fun splitPaths(s: String): List<String> = s.split('\n').filter { it.isNotBlank() }
fun photoCount(count: Int, paths: String): Int = splitPaths(paths).size.takeIf { it > 0 } ?: count
