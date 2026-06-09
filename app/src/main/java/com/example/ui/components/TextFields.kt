package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTypography
import com.example.ui.theme.GradientStart
import com.example.ui.theme.LocalAppShapes
import com.example.ui.theme.LocalAppSpacing
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = LocalAppShapes.current.large
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = AppTypography.bodyLarge.copy(color = TextPrimary),
        cursorBrush = SolidColor(GradientStart),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(SurfaceElevated, shape)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), shape)
                    .padding(horizontal = LocalAppSpacing.current.large, vertical = LocalAppSpacing.current.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Box(modifier = Modifier.padding(end = LocalAppSpacing.current.small))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = AppTypography.bodyLarge, color = TextMuted)
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Box(modifier = Modifier.padding(start = LocalAppSpacing.current.small))
                    trailingIcon()
                }
            }
        }
    )
}
