package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppTypography
import com.example.ui.theme.Background
import com.example.ui.theme.LocalAppShapes
import com.example.ui.theme.LocalAppSpacing
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceElevated

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .padding(LocalAppSpacing.current.extraLarge)
                .fillMaxWidth()
                .clip(LocalAppShapes.current.extraLarge)
                .background(Surface)
                .border(1.dp, Color.White.copy(alpha = 0.05f), LocalAppShapes.current.extraLarge)
                .padding(LocalAppSpacing.current.extraLarge)
        ) {
            Column {
                Text(text = title, style = AppTypography.titleLarge)
                Spacer(modifier = Modifier.height(LocalAppSpacing.current.large))
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Background,
        shape = LocalAppShapes.current.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalAppSpacing.current.extraLarge)
                .padding(bottom = LocalAppSpacing.current.extraLarge)
        ) {
            content()
        }
    }
}
