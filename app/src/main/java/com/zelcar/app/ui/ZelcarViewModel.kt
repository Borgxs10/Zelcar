package com.zelcar.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zelcar.app.BuildConfig
import com.zelcar.app.ZelcarApp
import com.zelcar.app.billing.BillingManager
import com.zelcar.app.data.CarEntity
import com.zelcar.app.data.CarSnapshot
import com.zelcar.app.data.CheckItemEntity
import com.zelcar.app.data.ExpenseEntity
import com.zelcar.app.data.NoteEntity
import com.zelcar.app.data.PhotoStore
import com.zelcar.app.data.Plan
import com.zelcar.app.data.ReminderEntity
import com.zelcar.app.data.TransferTicket
import com.zelcar.app.data.splitPaths
import com.zelcar.app.domain.FREE_CAR_LIMIT
import com.zelcar.app.domain.Period
import com.zelcar.app.domain.nkm
import com.zelcar.app.domain.parseInt2
import com.zelcar.app.domain.parseNum
import com.zelcar.app.domain.brl
import com.zelcar.app.domain.dmy
import com.zelcar.app.domain.rankReminders
import com.zelcar.app.domain.toDate
import com.zelcar.app.domain.today
import com.zelcar.app.report.ReportGenerator
import com.zelcar.app.report.ReportOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class PhotoTarget { Issue, Note }

private const val MAX_PHOTOS = 4

class ZelcarViewModel(app: Application) : AndroidViewModel(app) {
    private val zapp = app as ZelcarApp
    private val repo = zapp.repository

    private val _local = MutableStateFlow(UiLocal())

    val state: StateFlow<AppState> = combine(
        combine(repo.cars, repo.expenses, repo.reminders, repo.notes, repo.checks) { c, e, r, n, k ->
            DataSet(c, e, r, n, k)
        },
        repo.settings,
        _local,
    ) { data, settings, ui -> AppState(data, settings, ui, today()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    private val _snacks = MutableSharedFlow<Snack>(extraBufferCapacity = 8)
    val snacks: SharedFlow<Snack> = _snacks.asSharedFlow()

    /** Pedido de compra (true = anual). A Activity escuta e abre o fluxo do Google Play. */
    private val _purchaseRequests = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val purchaseRequests: SharedFlow<Boolean> = _purchaseRequests.asSharedFlow()

    val prices get() = zapp.billing.prices

    init {
        viewModelScope.launch {
            val s = repo.prefs.current()
            _local.update { it.copy(loaded = true, screen = if (s.onboarded) Dest.Garage else Dest.Onboarding) }
        }
        viewModelScope.launch {
            zapp.billing.events.collect { e ->
                when (e) {
                    BillingManager.Event.Purchased -> {
                        toast("Zelcar Pro ativo.")
                        if (state.value.ui.screen == Dest.Paywall) back()
                    }
                    BillingManager.Event.Cancelled -> Unit
                    is BillingManager.Event.Failed -> toast(e.message)
                }
            }
        }
    }

    // ───────────────────────── helpers ─────────────────────────
    private val s: AppState get() = state.value
    private fun ui(f: (UiLocal) -> UiLocal) = _local.update(f)
    fun form(f: (Form) -> Form) = _local.update { it.copy(form = f(it.form)) }

    fun toast(msg: String, undo: (() -> Unit)? = null) {
        _snacks.tryEmit(Snack(msg, undo))
    }

    fun ask(title: String, body: String, actions: List<DialogAction>) = ui { it.copy(dialog = DialogSpec(title, body, actions)) }
    fun closeDialog() = ui { it.copy(dialog = null) }

    private fun newId() = UUID.randomUUID().toString()

    // ───────────────────────── navegação ─────────────────────────
    fun go(dest: Dest, push: Boolean = false) {
        val st = s
        val car = st.activeCar
        val carKm = (car?.km ?: 0).toString()
        ui { l ->
            var f = l.form
            when (dest) {
                Dest.Km -> f = f.copy(kmDraft = carKm)
                Dest.AddExpense -> f = f.copy(value = "", title = "", km = carKm, cat = "Combustível")
                Dest.AddNote -> f = f.copy(noteTitle = "", noteBody = "", noteKm = carKm, notePhotos = emptyList())
                Dest.AddCar -> f = f.copy(
                    carBrand = "", carColor = "", carName = "", carYear = "", carPlate = "",
                    carKmNew = "", carPrice = "", carMain = st.owned.isEmpty(),
                )
                else -> Unit
            }
            l.copy(
                screen = dest,
                dialog = null,
                stack = if (push) l.stack + l.screen else emptyList(),
                form = f,
                pdf = if (dest == Dest.Report) PdfState.Idle else l.pdf,
                reportFile = if (dest == Dest.Report) null else l.reportFile,
                scanning = if (dest == Dest.Pairing) l.scanning else false,
            )
        }
        if (dest == Dest.Reminders) syncReminderForm(st.ui.selReminderId)
    }

    fun back() {
        ui { l ->
            val next = l.stack.lastOrNull() ?: Dest.Garage
            l.copy(screen = next, stack = l.stack.dropLast(1), dialog = null, scanning = false)
        }
    }

    // ───────────────────────── onboarding / garagem ─────────────────────────
    fun finishOnboarding(buying: Boolean) {
        viewModelScope.launch {
            repo.prefs.setOnboarded(true)
            val st = s
            ui { it.copy(screen = Dest.Garage, stack = emptyList()) }
            if (buying) {
                if (st.buyingCar != null) go(Dest.Checklist, push = true) else openAddCar(CarMode.Buying)
            } else if (st.owned.isEmpty()) {
                openAddCar(CarMode.Owned)
            }
        }
    }

    fun openAddCar(mode: CarMode) {
        go(Dest.AddCar, push = true)
        ui { it.copy(carMode = mode) }
    }

    fun setGarageFilter(f: GarageFilter) = ui { it.copy(garageFilter = f) }

    fun openCar(c: CarEntity) {
        viewModelScope.launch { repo.prefs.setActiveCar(c.id) }
        ui { it.copy(screen = Dest.Car, stack = listOf(Dest.Garage), dialog = null) }
    }

    fun openBuyingChecklist() = go(Dest.Checklist, push = true)

    fun askCarActions(c: CarEntity) {
        ask(
            c.name, "O que fazer com este carro?",
            buildList {
                if (!c.buying) add(DialogAction("Transferir", DialogAction.Kind.Primary) {
                    viewModelScope.launch { repo.prefs.setActiveCar(c.id) }
                    go(Dest.Transfer, push = true)
                })
                add(DialogAction("Excluir", if (c.buying) DialogAction.Kind.Primary else DialogAction.Kind.Secondary) { askDeleteCar(c) })
                add(DialogAction("Cancelar", DialogAction.Kind.Ghost) { closeDialog() })
            },
        )
    }

    private fun askDeleteCar(c: CarEntity) {
        ask("Excluir ${c.name}?", "Gastos, lembretes e notas deste carro também são apagados deste aparelho.", listOf(
            DialogAction("Excluir", DialogAction.Kind.Primary) {
                closeDialog()
                removeCarWithUndo(c, "${c.name} excluído.")
            },
            DialogAction("Manter", DialogAction.Kind.Ghost) { closeDialog() },
        ))
    }

    private fun removeCarWithUndo(c: CarEntity, message: String) {
        viewModelScope.launch {
            val st = s
            val snap: CarSnapshot = repo.snapshot(
                c.id, st.data.cars, st.data.expenses, st.data.reminders, st.data.notes, st.data.checks,
            ) ?: return@launch
            val wasActive = st.settings.activeCarId == c.id
            val wasMain = st.settings.mainCarId == c.id
            repo.removeCar(c.id)
            ui { it.copy(screen = Dest.Garage, stack = emptyList(), dialog = null) }
            toast(message) { viewModelScope.launch { repo.restore(snap, wasActive, wasMain) } }
        }
    }

    // ───────────────────────── novo carro ─────────────────────────
    fun setCarMode(m: CarMode) = ui { it.copy(carMode = m) }

    fun saveCar() {
        val st = s
        val f = st.ui.form
        if (f.carName.isBlank()) return toast("Informe o modelo do carro.")
        if (st.settings.plan == Plan.Free && st.data.cars.size >= FREE_CAR_LIMIT) return go(Dest.Paywall, push = true)
        val buying = st.ui.carMode == CarMode.Buying
        val car = CarEntity(
            id = newId(),
            name = (f.carBrand.trim() + " " + f.carName.trim()).trim(),
            year = parseInt2(f.carYear).takeIf { it > 0 } ?: st.today.year,
            plate = if (buying) "—" else f.carPlate.ifBlank { "—" },
            color = f.carColor.trim().ifBlank { "—" },
            km = if (buying) 0 else parseInt2(f.carKmNew),
            buying = buying,
            price = if (buying) parseInt2(f.carPrice) else 0,
        )
        viewModelScope.launch {
            if (buying) {
                repo.addBuyingCar(car)
                ui { it.copy(screen = Dest.Checklist, stack = listOf(Dest.Garage), checkFilter = "todos") }
                toast("${car.name} está em compra. Comece o checklist.")
            } else {
                repo.addOwnedCar(car, f.carMain)
                ui { it.copy(screen = Dest.Garage, stack = emptyList()) }
                toast("${car.name} entrou na garagem.") { removeCarQuiet(car.id) }
            }
        }
    }

    private fun removeCarQuiet(id: String) = viewModelScope.launch { repo.removeCar(id) }

    // ───────────────────────── checklist ─────────────────────────
    fun setCheckFilter(k: String) = ui { it.copy(checkFilter = k) }
    fun pickCheck(id: String) = ui { it.copy(pickId = id) }
    fun closePick() = ui { it.copy(pickId = null) }

    fun choose(state: String) {
        val id = s.ui.pickId ?: return
        closePick()
        if (state == "issue") openIssue(id)
        else patchCheck(id) { it.copy(state = state, note = "", severity = "", cost = 0.0, photos = 0, photoPaths = "") }
    }

    private fun patchCheck(id: String, f: (CheckItemEntity) -> CheckItemEntity) {
        val c = s.data.checks.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { repo.upsertCheckItem(f(c)) }
    }

    private fun openIssue(id: String) {
        val c = s.data.checks.firstOrNull { it.id == id } ?: return
        ui { l ->
            l.copy(
                selCheckId = id,
                form = l.form.copy(
                    issueNote = c.note,
                    issueSev = c.severity.ifBlank { "atencao" },
                    issueCost = if (c.cost > 0) c.cost.toLong().toString() else "",
                    issuePhotos = splitPaths(c.photoPaths),
                ),
                screen = Dest.Issue, stack = l.stack + l.screen, dialog = null,
            )
        }
    }

    fun saveIssue() {
        val st = s
        val f = st.ui.form
        val id = st.ui.selCheckId ?: return
        if (f.issueNote.isBlank() && f.issuePhotos.isEmpty()) return toast("Descreva o problema ou anexe uma foto.")
        patchCheck(id) {
            it.copy(
                state = "issue", note = f.issueNote.trim(), severity = f.issueSev, cost = parseNum(f.issueCost),
                photos = f.issuePhotos.size, photoPaths = f.issuePhotos.joinToString("\n"),
            )
        }
        back()
        toast("Problema registrado no checklist.")
    }

    fun issueSet(state: String) {
        val id = s.ui.selCheckId ?: return
        patchCheck(id) { it.copy(state = state, note = "", severity = "", cost = 0.0, photos = 0, photoPaths = "") }
        back()
        toast(if (state == "ok") "Item marcado como OK." else "Marcação removida.")
    }

    // ───────────────────────── fotos ─────────────────────────
    fun attachPhotos(target: PhotoTarget, uris: List<Uri>) {
        viewModelScope.launch {
            val saved = uris.mapNotNull { PhotoStore.import(getApplication(), it) }
            if (saved.isEmpty()) return@launch toast("Não foi possível anexar a foto.")
            form { f ->
                when (target) {
                    PhotoTarget.Issue -> f.copy(issuePhotos = (f.issuePhotos + saved).take(MAX_PHOTOS))
                    PhotoTarget.Note -> f.copy(notePhotos = (f.notePhotos + saved).take(MAX_PHOTOS))
                }
            }
        }
    }

    fun removePhoto(target: PhotoTarget, path: String) = form { f ->
        when (target) {
            PhotoTarget.Issue -> f.copy(issuePhotos = f.issuePhotos - path)
            PhotoTarget.Note -> f.copy(notePhotos = f.notePhotos - path)
        }
    }

    // ───────────────────────── quilometragem ─────────────────────────
    fun saveKm() {
        val car = s.activeCar ?: return
        val v = parseInt2(s.ui.form.kmDraft)
        if (v <= 0) return toast("Informe uma quilometragem válida.")
        if (v < car.km) {
            ask(
                "Km menor que o anterior",
                "Você registrou ${nkm(car.km)} km. Salvar ${nkm(v)} km recalcula tudo para baixo.",
                listOf(
                    DialogAction("Salvar assim", DialogAction.Kind.Primary) { commitKm(car, v) },
                    DialogAction("Corrigir", DialogAction.Kind.Ghost) { closeDialog() },
                ),
            )
            return
        }
        commitKm(car, v)
    }

    private fun commitKm(car: CarEntity, v: Int) {
        viewModelScope.launch { repo.setKm(car.id, v) }
        closeDialog()
        toast("Km atualizado para ${nkm(v)}. Lembretes recalculados.")
        back()
    }

    fun kmStep(delta: Int) = form { f -> f.copy(kmDraft = (parseInt2(f.kmDraft) + delta).coerceAtLeast(0).toString()) }

    // ───────────────────────── gastos ─────────────────────────
    fun setPeriod(p: Period) = ui { it.copy(period = p) }

    fun saveExpense() {
        val car = s.activeCar ?: return toast("Adicione um carro antes de lançar gastos.")
        val f = s.ui.form
        val v = parseNum(f.value)
        if (v <= 0) return toast("Informe o valor do gasto.")
        val e = ExpenseEntity(
            id = newId(), carId = car.id, cat = f.cat, title = f.title.ifBlank { f.cat },
            epochDay = today().toEpochDay(), km = parseInt2(f.km).takeIf { it > 0 } ?: car.km, value = v,
        )
        viewModelScope.launch { repo.upsertExpense(e) }
        ui { it.copy(screen = Dest.Finance, period = Period.Month, stack = emptyList()) }
        toast("${brl(v)} em ${e.cat} lançado.") { viewModelScope.launch { repo.deleteExpense(e.id) } }
    }

    fun askDeleteExpense(e: ExpenseEntity) {
        ask(
            "Apagar lançamento?", "${e.title} · ${brl(e.value)} · ${e.epochDay.toDate().dmy()}",
            listOf(
                DialogAction("Apagar", DialogAction.Kind.Primary) {
                    closeDialog()
                    viewModelScope.launch { repo.deleteExpense(e.id) }
                    toast("Lançamento apagado.") { viewModelScope.launch { repo.upsertExpense(e) } }
                },
                DialogAction("Manter", DialogAction.Kind.Ghost) { closeDialog() },
            ),
        )
    }

    // ───────────────────────── diário ─────────────────────────
    fun saveNote() {
        val car = s.activeCar ?: return toast("Adicione um carro antes de criar notas.")
        val f = s.ui.form
        if (f.noteTitle.isBlank()) return toast("Dê um título para a nota.")
        val n = NoteEntity(
            id = newId(), carId = car.id, epochDay = today().toEpochDay(),
            km = parseInt2(f.noteKm).takeIf { it > 0 } ?: car.km,
            title = f.noteTitle.trim(), body = f.noteBody.trim(),
            photos = f.notePhotos.size, photoPaths = f.notePhotos.joinToString("\n"),
        )
        viewModelScope.launch { repo.upsertNote(n) }
        ui { it.copy(screen = Dest.Logbook, stack = emptyList()) }
        toast("Nota salva no diário.") { viewModelScope.launch { repo.deleteNote(n.id) } }
    }

    // ───────────────────────── lembretes ─────────────────────────
    private fun syncReminderForm(preferred: String?) {
        val st = s
        val car = st.activeCar ?: return
        val ranked = rankReminders(st.carReminders, car.km, st.today)
        val r = ranked.firstOrNull { it.reminder.id == preferred }?.reminder ?: ranked.firstOrNull()?.reminder
        ui { l ->
            l.copy(
                selReminderId = r?.id,
                form = l.form.copy(
                    reminderLabel = r?.label ?: "",
                    intervalKm = r?.everyKm?.takeIf { it > 0 }?.toString() ?: "",
                    intervalMonths = r?.everyMonths?.takeIf { it > 0 }?.toString() ?: "",
                ),
            )
        }
    }

    fun selectReminder(id: String) = syncReminderForm(id)

    fun doneNext() {
        val st = s
        val car = st.activeCar ?: return
        val r = rankReminders(st.carReminders, car.km, st.today).firstOrNull()?.reminder ?: return
        val updated = if (r.type == "km") r.copy(lastKm = car.km)
        else r.copy(dueEpochDay = r.dueEpochDay.toDate().plusMonths((r.everyMonths.takeIf { it > 0 } ?: 12).toLong()).toEpochDay())
        viewModelScope.launch { repo.upsertReminder(updated) }
        toast("${r.label} marcado como feito.") { viewModelScope.launch { repo.upsertReminder(r) } }
    }

    fun askPostpone() {
        val st = s
        val car = st.activeCar ?: return
        val r = rankReminders(st.carReminders, car.km, st.today).firstOrNull()?.reminder ?: return
        ask(
            "Adiar ${r.label}", "Adiar não apaga o lembrete — só empurra o vencimento.",
            listOf(
                DialogAction("+ 7 dias", DialogAction.Kind.Primary) { postpone(r, days = 7) },
                DialogAction("+ 30 dias", DialogAction.Kind.Secondary) { postpone(r, days = 30) },
                DialogAction("+ 1.000 km", DialogAction.Kind.Secondary) { postpone(r, km = 1000) },
                DialogAction("Cancelar", DialogAction.Kind.Ghost) { closeDialog() },
            ),
        )
    }

    /** Adiar por dias (~30 km/dia nos lembretes por km) ou por km (~30 km/dia nos de data). */
    private fun postpone(r: ReminderEntity, days: Int = 0, km: Int = 0) {
        val updated = if (r.type == "km") r.copy(lastKm = r.lastKm + days * 30 + km)
        else r.copy(dueEpochDay = r.dueEpochDay + days + km / 30)
        viewModelScope.launch { repo.upsertReminder(updated) }
        closeDialog()
        toast("${r.label} adiado.") { viewModelScope.launch { repo.upsertReminder(r) } }
    }

    fun saveInterval() {
        val st = s
        val id = st.ui.selReminderId ?: return
        val r = st.data.reminders.firstOrNull { it.id == id } ?: return
        val f = st.ui.form
        val k = parseInt2(f.intervalKm)
        val m = parseInt2(f.intervalMonths)
        viewModelScope.launch {
            repo.upsertReminder(
                r.copy(
                    label = f.reminderLabel.trim().ifBlank { r.label },
                    everyKm = if (k > 0) k else r.everyKm,
                    everyMonths = if (m > 0) m else r.everyMonths,
                ),
            )
        }
        toast("Intervalo salvo: " + (if (k > 0) "${nkm(k)} km" else "sem km") + " ou " + (if (m > 0) "$m" else "—") + " meses.")
    }

    fun addReminder() {
        val car = s.activeCar ?: return toast("Adicione um carro antes de criar lembretes.")
        val n = ReminderEntity(newId(), car.id, "Novo lembrete", "km", everyKm = 10000, lastKm = car.km, everyMonths = 12)
        viewModelScope.launch { repo.upsertReminder(n) }
        ui { l ->
            l.copy(selReminderId = n.id, form = l.form.copy(reminderLabel = n.label, intervalKm = "10000", intervalMonths = "12"))
        }
        toast("Lembrete criado.") { viewModelScope.launch { repo.deleteReminder(n.id) } }
    }

    fun askDeleteSelectedReminder() {
        val id = s.ui.selReminderId ?: return
        val r = s.data.reminders.firstOrNull { it.id == id } ?: return
        ask("Excluir lembrete?", r.label, listOf(
            DialogAction("Excluir", DialogAction.Kind.Primary) {
                closeDialog()
                viewModelScope.launch { repo.deleteReminder(r.id) }
                ui { it.copy(selReminderId = null) }
                toast("Lembrete excluído.") { viewModelScope.launch { repo.upsertReminder(r) } }
            },
            DialogAction("Manter", DialogAction.Kind.Ghost) { closeDialog() },
        ))
    }

    // ───────────────────────── transferência ─────────────────────────
    fun toggleTransfer(key: String) = ui { l -> l.copy(transfer = if (key in l.transfer) l.transfer - key else l.transfer + key) }

    suspend fun createTicket(carId: String, blocks: Set<String>): TransferTicket = zapp.transfer.createTicket(carId, blocks)

    fun askTransfer() {
        val car = s.activeCar ?: return
        val n = s.ui.transfer.size
        ask(
            "Transferir o ${car.name}?",
            "O carro sai da sua garagem, $n blocos de dados vão para o comprador e os lembretes param.",
            listOf(
                DialogAction("Transferir", DialogAction.Kind.Primary) { closeDialog(); removeCarWithUndo(car, "${car.name} transferido.") },
                DialogAction("Cancelar", DialogAction.Kind.Ghost) { closeDialog() },
            ),
        )
    }

    // ───────────────────────── relatório PDF ─────────────────────────
    fun setReportRange(r: ReportRange) = ui { it.copy(reportRange = r, pdf = PdfState.Idle, reportFile = null) }

    fun toggleReport(key: String) = ui { l ->
        l.copy(report = if (key in l.report) l.report - key else l.report + key, pdf = PdfState.Idle, reportFile = null)
    }

    fun generatePdf() {
        val st = s
        if (!st.isPro) return go(Dest.Paywall, push = true)
        if (st.ui.pdf == PdfState.Generating) return
        val car = st.activeCar ?: return toast("Adicione um carro para gerar o relatório.")
        ui { it.copy(pdf = PdfState.Generating) }
        viewModelScope.launch {
            try {
                val file = ReportGenerator.generate(
                    getApplication(), car,
                    st.data.expenses.filter { it.carId == car.id },
                    st.data.reminders.filter { it.carId == car.id },
                    st.data.notes.filter { it.carId == car.id },
                    ReportOptions(st.ui.report, st.ui.reportRange == ReportRange.SixMonths),
                    st.today,
                )
                ui { it.copy(pdf = PdfState.Ready, reportFile = file) }
                toast("PDF pronto. Abrindo o compartilhamento.")
            } catch (e: Exception) {
                ui { it.copy(pdf = PdfState.Idle) }
                toast("Não foi possível gerar o PDF.")
            }
        }
    }

    // ───────────────────────── pareamento web ─────────────────────────
    fun startScan() = ui { it.copy(scanning = true) }
    fun stopScan() = ui { it.copy(scanning = false) }

    fun onPairCode(raw: String): Boolean {
        val ok = raw.startsWith("zelcar://pair") || raw.contains("zelcar.app")
        if (!ok) {
            toast("Este QR Code não é do Zelcar.")
            return false
        }
        viewModelScope.launch { repo.prefs.setPaired(true) }
        ui { it.copy(scanning = false) }
        toast("Navegador vinculado.")
        return true
    }

    fun askRevoke() {
        ask("Revogar a sessão web?", "O navegador para de ver seus carros na hora.", listOf(
            DialogAction("Revogar", DialogAction.Kind.Primary) {
                closeDialog()
                viewModelScope.launch { repo.prefs.setPaired(false) }
                toast("Sessão web revogada.") { viewModelScope.launch { repo.prefs.setPaired(true) } }
            },
            DialogAction("Manter", DialogAction.Kind.Ghost) { closeDialog() },
        ))
    }

    // ───────────────────────── assinatura ─────────────────────────
    fun setPlanPick(p: PlanPick) = ui { it.copy(planPick = p) }

    fun subscribe() {
        val st = s
        if (st.isPro) {
            toast("Você já está no Pro.")
            return back()
        }
        if (BuildConfig.DEBUG && !zapp.billing.isAvailable) {
            viewModelScope.launch { repo.prefs.setPlan(Plan.Pro) }
            toast("Zelcar Pro ativo (build de debug).") { viewModelScope.launch { repo.prefs.setPlan(Plan.Free) } }
            return back()
        }
        _purchaseRequests.tryEmit(st.ui.planPick == PlanPick.Annual)
    }

    // ───────────────────────── sync / ajustes ─────────────────────────
    fun sync() {
        if (s.ui.syncing) return
        ui { it.copy(syncing = true) }
        viewModelScope.launch {
            val r = zapp.sync.sync()
            ui { it.copy(syncing = false) }
            toast(if (r.isSuccess) "Sincronizado com a nuvem." else "Não foi possível sincronizar agora.")
        }
    }

    fun setNotif(key: String, on: Boolean) {
        viewModelScope.launch {
            when (key) {
                "lembretes" -> repo.prefs.setNotifReminders(on)
                "resumo" -> repo.prefs.setNotifSummary(on)
                "sync" -> repo.prefs.setNotifSync(on)
            }
        }
    }

    fun askReset() {
        ask("Apagar tudo deste aparelho?", "Carros, gastos, lembretes e notas somem. A sessão web também cai.", listOf(
            DialogAction("Apagar tudo", DialogAction.Kind.Primary) {
                closeDialog()
                viewModelScope.launch {
                    repo.eraseAll()
                    ui { it.copy(screen = Dest.Onboarding, stack = emptyList()) }
                    toast("Dados apagados.")
                }
            },
            DialogAction("Cancelar", DialogAction.Kind.Ghost) { closeDialog() },
        ))
    }

    fun loadDemo() {
        viewModelScope.launch {
            repo.loadDemo(today())
            ui { it.copy(screen = Dest.Garage, stack = emptyList()) }
            toast("Dados de exemplo carregados.")
        }
    }
}
