package com.zelcar.app.ui.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zelcar.app.ui.components.BtnKind
import com.zelcar.app.ui.components.Muted
import com.zelcar.app.ui.components.PhotoThumb
import com.zelcar.app.ui.components.ZButton
import com.zelcar.app.ui.components.ZIcon
import com.zelcar.app.ui.components.ZIcons
import com.zelcar.app.ui.theme.Z

/** Rodapé padrão das folhas: Cancelar / ação principal. */
@Composable
fun SheetButtons(saveLabel: String, onCancel: () -> Unit, modifier: Modifier = Modifier, onSave: () -> Unit) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ZButton("Cancelar", onCancel, Modifier.weight(1f), BtnKind.Secondary)
        ZButton(saveLabel, onSave, Modifier.weight(1f), BtnKind.Primary)
    }
}

/** Botão "+" tracejado, miniaturas das fotos já anexadas e o seletor de fotos do sistema (Photo Picker). */
@Composable
fun PhotoRow(paths: List<String>, onPicked: (List<Uri>) -> Unit, onRemove: (String) -> Unit, max: Int = 4) {
    val z = Z.c
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(max)) { uris ->
        if (uris.isNotEmpty()) onPicked(uris)
    }
    Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (paths.size < max) {
            Box(
                Modifier.size(54.dp).border(1.dp, z.a400).clickable {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                contentAlignment = Alignment.Center,
            ) { ZIcon(ZIcons.Plus, tint = z.accent) }
        }
        paths.forEach { p -> PhotoThumb(p, onRemove = { onRemove(p) }) }
        if (paths.isEmpty()) Muted("Anexar fotos", size = 12.5f, alpha = 0.7f)
        else Muted(if (paths.size == 1) "1 foto anexada" else "${paths.size} fotos anexadas", size = 12.5f, alpha = 0.7f)
    }
}