package com.zelcar.app.ui

import com.zelcar.app.data.CarEntity
import com.zelcar.app.data.CheckItemEntity
import com.zelcar.app.data.ExpenseEntity
import com.zelcar.app.data.NoteEntity
import com.zelcar.app.data.Plan
import com.zelcar.app.data.ReminderEntity
import com.zelcar.app.data.Settings
import com.zelcar.app.domain.Period
import com.zelcar.app.ui.components.ZIcons
import java.io.File
import java.time.LocalDate

enum class Tab(val label: String, val icon: List<String>) {
    Garage("Garagem", ZIcons.TabGarage),
    Finance("Gastos", ZIcons.TabFinance),
    Reminders("Lembretes", ZIcons.Bell),
    Logbook("Diário", ZIcons.TabLogbook),
    Settings("Ajustes", ZIcons.TabSettings),
}

/** As 17 telas do protótipo. [sheet] = ModalBottomSheet sobre a tela anterior. */
enum class Dest(val screenName: String, val appBarTitle: String, val backIcon: Boolean, val tab: Tab?, val sheet: Boolean = false) {
    Onboarding("Entrada sem login", "Zelcar", false, null),
    Garage("Garagem", "Zelcar", false, Tab.Garage),
    Car("Ficha do carro", "Ficha do carro", true, Tab.Garage),
    Checklist("Checklist de compra", "Checklist de compra", true, Tab.Garage),
    Finance("Gastos", "Gastos", false, Tab.Finance),
    AddExpense("Novo gasto", "Gastos", true, Tab.Finance, sheet = true),
    Reminders("Lembretes", "Lembretes", false, Tab.Reminders),
    Logbook("Diário de bordo", "Diário de bordo", false, Tab.Logbook),
    AddNote("Nova nota", "Diário de bordo", true, Tab.Logbook, sheet = true),
    Issue("Detalhar problema", "Checklist de compra", true, Tab.Garage, sheet = true),
    Km("Atualizar quilometragem", "Quilometragem", true, Tab.Logbook, sheet = true),
    AddCar("Adicionar carro", "Novo carro", true, Tab.Garage, sheet = true),
    Transfer("Transferência via QR", "Transferir", true, Tab.Garage, sheet = true),
    Pairing("Pareamento com a Web", "Vincular navegador", true, Tab.Settings),
    Report("Relatório em PDF", "Relatório", true, Tab.Garage, sheet = true),
    Paywall("Assinatura", "Zelcar Pro", true, Tab.Settings),
    Settings("Configurações", "Configurações", true, Tab.Settings),
    ;

    /** Tela "de fundo" quando esta é uma folha aberta sem pilha. */
    companion object {
        fun rootOf(tab: Tab?): Dest = when (tab) {
            Tab.Finance -> Finance
            Tab.Reminders -> Reminders
            Tab.Logbook -> Logbook
            Tab.Settings -> Settings
            else -> Garage
        }
    }
}

enum class GarageFilter(val label: String) { All("Todos"), Mine("Meus carros"), Buying("Em compra") }
enum class CarMode(val label: String) { Owned("Já tenho"), Buying("Estou comprando") }
enum class PlanPick { Monthly, Annual }
enum class ReportRange(val label: String) { SixMonths("Últimos 6 meses"), All("Tudo") }
enum class PdfState { Idle, Generating, Ready }

val REPORT_SECTIONS = listOf(
    "ficha" to "Ficha do veículo",
    "manut" to "Histórico de manutenção",
    "gastos" to "Gastos por categoria",
    "fotos" to "Fotos do diário de bordo",
    "custo" to "Custo por km e projeções",
)

/** Blocos que acompanham a transferência (RF14). O rótulo "histórico" é montado na tela, com a contagem. */
val TRANSFER_KEYS = listOf("dados", "historico", "gastos", "notas")

class DialogAction(val label: String, val kind: Kind, val act: () -> Unit) {
    enum class Kind { Primary, Secondary, Ghost }
}

class DialogSpec(val title: String, val body: String, val actions: List<DialogAction>)

class Snack(val message: String, val undo: (() -> Unit)? = null)

data class Form(
    // km
    val kmDraft: String = "",
    // gasto
    val value: String = "", val title: String = "", val km: String = "", val cat: String = "Combustível",
    // nota
    val noteTitle: String = "", val noteBody: String = "", val noteKm: String = "", val notePhotos: List<String> = emptyList(),
    // problema do checklist
    val issueNote: String = "", val issueSev: String = "atencao", val issueCost: String = "",
    val issuePhotos: List<String> = emptyList(),
    // carro
    val carBrand: String = "", val carColor: String = "", val carName: String = "", val carYear: String = "",
    val carPlate: String = "", val carKmNew: String = "", val carPrice: String = "", val carMain: Boolean = false,
    // lembrete
    val reminderLabel: String = "", val intervalKm: String = "", val intervalMonths: String = "",
)

/** Estado de UI que não é persistido. */
data class UiLocal(
    val loaded: Boolean = false,
    val screen: Dest = Dest.Garage,
    val stack: List<Dest> = emptyList(),
    val period: Period = Period.Month,
    val garageFilter: GarageFilter = GarageFilter.All,
    val checkFilter: String = "todos",
    val carMode: CarMode = CarMode.Owned,
    val planPick: PlanPick = PlanPick.Annual,
    val reportRange: ReportRange = ReportRange.All,
    val report: Set<String> = setOf("ficha", "manut", "gastos"),
    val transfer: Set<String> = setOf("dados", "historico", "notas"),
    val pdf: PdfState = PdfState.Idle,
    val reportFile: File? = null,
    val syncing: Boolean = false,
    val scanning: Boolean = false,
    val pickId: String? = null,
    val selCheckId: String? = null,
    val selReminderId: String? = null,
    val dialog: DialogSpec? = null,
    val form: Form = Form(),
)

data class DataSet(
    val cars: List<CarEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val checks: List<CheckItemEntity> = emptyList(),
)

/** Instantâneo completo que as telas leem. */
data class AppState(
    val data: DataSet = DataSet(),
    val settings: Settings = Settings(),
    val ui: UiLocal = UiLocal(),
    val today: LocalDate = LocalDate.now(),
) {
    val isPro get() = settings.plan == Plan.Pro
    val owned get() = data.cars.filter { !it.buying }

    /** Carro em uso nas abas Gastos/Lembretes/Diário/Ficha: o ativo (se for da garagem) ou o primeiro. */
    val activeCar: CarEntity? get() =
        data.cars.firstOrNull { it.id == settings.activeCarId && !it.buying } ?: owned.firstOrNull()

    val mainCarId: String? get() = settings.mainCarId?.takeIf { id -> owned.any { it.id == id } } ?: owned.firstOrNull()?.id

    /** Carro em compra cujo checklist está aberto: o último adicionado. */
    val buyingCar: CarEntity? get() = data.cars.lastOrNull { it.buying }

    val carReminders get() = activeCar?.let { c -> data.reminders.filter { it.carId == c.id } }.orEmpty()
    val carExpenses get() = activeCar?.let { c -> data.expenses.filter { it.carId == c.id } }.orEmpty()
    val carNotes get() = activeCar?.let { c -> data.notes.filter { it.carId == c.id } }.orEmpty()
    val checkItems get() = buyingCar?.let { c -> data.checks.filter { it.carId == c.id } }.orEmpty()
}
