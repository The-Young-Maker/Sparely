package com.example.sparely.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sparely.ui.theme.ExpressiveShapes

/**
 * Small set of reusable expressive components for consistent cards and surfaces.
 * These keep a consistent shape / elevation / spacing and support an optional gradient.
 */
@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = ExpressiveShapes.large,
    tonalElevation: Dp = 6.dp,
    shadowElevation: Dp = 1.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
