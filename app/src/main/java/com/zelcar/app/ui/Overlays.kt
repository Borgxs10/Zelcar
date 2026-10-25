package com.zelcar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.theme.Z
import com.zelcar.app.ui.theme.bodyStyle
import com.zelcar.app.ui.theme.headingStyle
import kotlinx.coroutines.launch

private val Scrim = Color(0xFF1D1F20)

/**
 * Folha inferior (ModalBottomSheet) do Material 3, com a alça de arraste do design.
 * [content] recebe `close`, que anima o fechamento e só então chama [onClose] (uma única vez).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZSheet(
    onClose: () -> Unit,
    roundedTop: Boolean = false,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val z = Z.c
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val closed = remember { booleanArrayOf(false) }
    val once: () -> Unit = {
        if (!closed[0]) {
            closed[0] = true
            onClose()
        }
    }
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { once() } }
    ModalBottomSheet(
        onDismissRequest = once,
        sheetState = state,
        shape = if (roundedTop) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) else RectangleShape,
        containerColor = z.bg,
        contentColor = z.text,
        scrimColor = Scrim.copy(alpha = 0.32f),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 14.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .background(z.n400, RoundedCornerShape(2.dp)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) { content(close) }
    }
}

/** Diálogo de confirmação do design: quadrado, borda fina, ações à direita. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZDialog(spec: DialogSpec, onDismiss: () -> Unit) {
    val z = Z.c
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .background(z.bg)
                .border(1.dp, z.divider)
                .padding(20.dp),
        ) {
            Text(spec.title, style = headingStyle(22.sp), color = z.text)
            Text(
                spec.body, style = bodyStyle(13.sp), color = z.text.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 8.dp),
            )
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                spec.actions.forEach { a ->
                    ZButton(
                        a.label, a.act,
                        kind = when (a.kind) {
                            DialogAction.Kind.Primary -> BtnKind.Primary
                            DialogAction.Kind.Secondary -> BtnKind.Secondary
                            DialogAction.Kind.Ghost -> BtnKind.Secondary
                        },
                    )
                }
            }
        }
    }
}

/** Snackbar do design: fundo "ground" escuro, ação DESFAZER em caixa-alta. */
@Composable
fun ZSnackbar(data: SnackbarData) {
    val z = Z.c
    Row(
        Modifier
            .fillMaxWidth()
            .background(z.ground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            data.visuals.message,
            style = bodyStyle(13.5.sp),
            color = Color(0xFFF2F2F3),
            modifier = Modifier.weight(1f),
        )
        val action = data.visuals.actionLabel
        if (action != null) {
            Text(
                action.uppercase(),
                style = bodyStyle(13.sp).copy(letterSpacing = 0.06.em),
                color = Color(0xFF9FC8F1),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .clickable { data.performAction() }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            )
        }
    }
}
