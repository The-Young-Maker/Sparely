package com.example.sparely.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun SingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign? = null,
    minFontSize: Float = 10f
) {
    BoxWithConstraints(modifier = modifier) {
        val textMeasurer = rememberTextMeasurer()
        var fontSize by remember(text, maxWidth) { mutableStateOf(style.fontSize.value) }
        
        // Measure and adjust font size to fit
        val measuredWidth = textMeasurer.measure(
            text = text,
            style = style.copy(fontSize = fontSize.sp)
        ).size.width
        
        val targetWidth = constraints.maxWidth
        
        if (measuredWidth > targetWidth && fontSize > minFontSize) {
            fontSize = (fontSize * targetWidth / measuredWidth).coerceAtLeast(minFontSize)
        }
        
        Text(
            text = text,
            style = style.copy(fontSize = fontSize.sp),
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = textAlign
        )
    }
}
