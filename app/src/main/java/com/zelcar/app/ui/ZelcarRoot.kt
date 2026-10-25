package com.zelcar.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.screens.CarScreen
import com.zelcar.app.ui.screens.ChecklistScreen
import com.zelcar.app.ui.screens.FinanceScreen
import com.zelcar.app.ui.screens.GarageScreen
import com.zelcar.app.ui.screens.LogbookScreen
import com.zelcar.app.ui.screens.OnboardingScreen
import com.zelcar.app.ui.screens.PairingScreen
import com.zelcar.app.ui.screens.PaywallScreen
import com.zelcar.app.ui.screens.RemindersScreen
import com.zelcar.app.ui.screens.SettingsScreen
import com.zelcar.app.ui.sheets.AddCarSheet
import com.zelcar.app.ui.sheets.AddExpenseSheet
import com.zelcar.app.ui.sheets.AddNoteSheet
import com.zelcar.app.ui.sheets.CheckPickSheet
import com.zelcar.app.ui.sheets.IssueSheet
import com.zelcar.app.ui.sheets.KmSheet
import com.zelcar.app.ui.sheets.ReportSheet
import com.zelcar.app.ui.sheets.TransferSheet
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.ZColors
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import kotlinx.coroutines.launch

@Composable
fun ZelcarRoot(vm: ZelcarViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (!state.ui.loaded) {
        Box(Modifier.fillMaxSize().background(Z.c.canvas))
    } else {
        ZelcarScaffold(vm, state)
    }
}

/** Fundo do app: cor base + brilho radial azul, como nas classes .zlight/.zdark do protótipo. */
private fun Modifier.zelcarBackground(z: ZColors): Modifier = this
    .background(z.canvas)
    .drawBehind {
        if (z.dark) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF3B8FF0).copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(size.width * 0.85f, -size.height * 0.08f), radius = size.width * 1.1f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF2F6FD0).copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(-size.width * 0.1f, size.height), radius = size.width * 0.95f,
                ),
            )
        } else {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF4F9AEB).copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.9f, -size.height * 0.06f), radius = size.width * 0.95f,
                ),
            )
        }
    }

@Composable
private fun ZelcarScaffold(vm: ZelcarViewModel, state: AppState) {
    val z = Z.c
    val ui = state.ui
    val screen = ui.screen
    val context = LocalContext.current

    // Snackbar com "Desfazer" real (o ViewModel manda a ação de desfazer junto da mensagem).
    val snackHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        vm.snacks.collect { s ->
            snackHost.currentSnackbarData?.dismiss()
            scope.launch {
                val r = snackHost.showSnackbar(
                    message = s.message,
                    actionLabel = if (s.undo != null) "Desfazer" else null,
                    duration = SnackbarDuration.Short,
                )
                if (r == SnackbarResult.ActionPerformed) s.undo?.invoke()
            }
        }
    }

    // Permissão de notificações (Android 13+), pedida uma vez depois do onboarding.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.settings.onboarded) {
        if (state.settings.onboarded && !asked && Build.VERSION.SDK_INT >= 33 && state.settings.notifReminders) {
            asked = true
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = ui.dialog != null || ui.pickId != null || (screen != Dest.Garage && screen != Dest.Onboarding)) {
        when {
            ui.dialog != null -> vm.closeDialog()
            ui.pickId != null -> vm.closePick()
            else -> vm.back()
        }
    }

    // Folha aberta mostra, por baixo, a tela que a abriu.
    val under = if (screen.sheet) (ui.stack.lastOrNull() ?: Dest.rootOf(screen.tab)) else screen
    val onboarding = screen == Dest.Onboarding

    Box(Modifier.fillMaxSize().zelcarBackground(z)) {
        Column(Modifier.fillMaxSize().imePadding().then(if (onboarding) Modifier.systemBarsPadding() else Modifier)) {
            if (!onboarding) {
                ZTopBar(
                    title = screen.appBarTitle,
                    back = screen.backIcon,
                    syncing = ui.syncing,
                    onNav = { vm.back() },
                    onSync = { vm.sync() },
                    onMore = { vm.go(Dest.Settings) },
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ScreenHost(under, state, vm)
                val fab = if (screen.sheet) null else fabFor(under)
                if (fab != null) {
                    ZFab(
                        label = fab,
                        onClick = {
                            when (under) {
                                Dest.Garage -> vm.openAddCar(CarMode.Owned)
                                Dest.Finance -> vm.go(Dest.AddExpense, push = true)
                                Dest.Logbook -> vm.go(Dest.AddNote, push = true)
                                else -> vm.addReminder()
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    )
                }
                SnackbarHost(
                    snackHost,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    snackbar = { ZSnackbar(it) },
                )
            }
            if (!onboarding) {
                ZBottomBar(selected = screen.tab, onSelect = { vm.go(Dest.rootOf(it)) })
            }
        }

        // Folhas, seletor do checklist e diálogos ficam por cima de tudo.
        when (screen) {
            Dest.AddExpense -> AddExpenseSheet(state, vm)
            Dest.AddNote -> AddNoteSheet(state, vm)
            Dest.Issue -> IssueSheet(state, vm)
            Dest.Km -> KmSheet(state, vm)
            Dest.AddCar -> AddCarSheet(state, vm)
            Dest.Transfer -> TransferSheet(state, vm)
            Dest.Report -> ReportSheet(state, vm)
            else -> Unit
        }
        if (ui.pickId != null) CheckPickSheet(state, vm)
        ui.dialog?.let { ZDialog(it, onDismiss = { vm.closeDialog() }) }
    }
}

private fun fabFor(dest: Dest): String? = when (dest) {
    Dest.Garage -> "Novo carro"
    Dest.Finance -> "Gasto"
    Dest.Logbook -> "Nota"
    Dest.Reminders -> "Lembrete"
    else -> null
}

@Composable
private fun ScreenHost(dest: Dest, state: AppState, vm: ZelcarViewModel) {
    when (dest) {
        Dest.Onboarding -> OnboardingScreen(vm)
        Dest.Car -> CarScreen(state, vm)
        Dest.Checklist -> ChecklistScreen(state, vm)
        Dest.Finance -> FinanceScreen(state, vm)
        Dest.Reminders -> RemindersScreen(state, vm)
        Dest.Logbook -> LogbookScreen(state, vm)
        Dest.Settings -> SettingsScreen(state, vm)
        Dest.Pairing -> PairingScreen(state, vm)
        Dest.Paywall -> PaywallScreen(state, vm)
        else -> GarageScreen(state, vm)
    }
}

// ─────────────────────────────────────────────────────────────
// Top app bar (56dp), navigation bar (80dp) e FAB estendido
// ─────────────────────────────────────────────────────────────
@Composable
private fun ZTopBar(
    title: String,
    back: Boolean,
    syncing: Boolean,
    onNav: () -> Unit,
    onSync: () -> Unit,
    onMore: () -> Unit,
) {
    val z = Z.c
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .drawBehind {
                drawLine(z.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonBox(onNav) { ZIcon(if (back) ZIcons.Back else ZIcons.Menu, size = 22.dp, tint = z.text) }
        Text(
            title, style = headingStyle(20.sp, 0.01f), color = z.text,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1,
        )
        IconButtonBox(onSync) { SyncIcon(syncing) }
        IconButtonBox(onMore) { ZIcon(ZIcons.Dots, size = 22.dp, tint = z.text) }
    }
}

@Composable
private fun IconButtonBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun SyncIcon(syncing: Boolean) {
    val z = Z.c
    if (syncing) {
        val t = rememberInfiniteTransition(label = "sync")
        val angle by t.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "syncAngle",
        )
        ZIcon(ZIcons.Sync, Modifier.rotate(angle), size = 22.dp, tint = z.accent)
    } else {
        ZIcon(ZIcons.Sync, size = 22.dp, tint = z.text)
    }
}

@Composable
private fun ZBottomBar(selected: Tab?, onSelect: (Tab) -> Unit) {
    val z = Z.c
    Column(
        Modifier
            .fillMaxWidth()
            .background(z.surface)
            .navigationBarsPadding()
            .drawBehind { drawLine(z.divider, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) },
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp)) {
            Tab.entries.forEach { tab ->
                val on = tab == selected
                val color = if (on) z.a900 else z.text.copy(alpha = 0.5f)
                Column(
                    Modifier.weight(1f).clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier
                            .width(64.dp).height(32.dp)
                            .background(if (on) z.a200 else Color.Transparent, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) { ZIcon(tab.icon, size = 24.dp, tint = color) }
                    Text(tab.label, style = bodyStyle(12.sp), color = color, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ZFab(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val z = Z.c
    Row(
        modifier
            .shadowMd()
            .background(z.accent, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        ZIcon(ZIcons.Plus, size = 21.dp, tint = z.bg)
        Text(label, style = headingStyle(16.sp, 0.02f), color = z.bg)
    }
}

private fun Modifier.shadowMd(): Modifier = this.then(
    Modifier.drawBehind {
        // Sombra suave sob o FAB (--shadow-md), sem depender de elevação do Material.
        drawRect(Color.Black.copy(alpha = 0.14f), topLeft = Offset(0f, 3.dp.toPx()), size = size)
    },
)
