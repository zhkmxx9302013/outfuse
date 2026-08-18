package com.outfuseplayer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.outfuseplayer.ui.FileNameDisplayMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileNameText(
    text: String,
    mode: FileNameDisplayMode,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    foldedLines: Int = 1,
    expandedLines: Int = 3
) {
    val displayModifier = if (mode == FileNameDisplayMode.MARQUEE) {
        modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            animationMode = MarqueeAnimationMode.Immediately,
            repeatDelayMillis = 500,
            initialDelayMillis = 250,
            spacing = MarqueeSpacing(24.dp),
            velocity = 78.dp
        )
    } else {
        modifier
    }
    Text(
        text = text,
        style = style,
        color = color,
        modifier = displayModifier,
        maxLines = when (mode) {
            FileNameDisplayMode.ELLIPSIS -> foldedLines
            FileNameDisplayMode.MULTILINE -> expandedLines
            FileNameDisplayMode.MARQUEE -> 1
        },
        overflow = when (mode) {
            FileNameDisplayMode.MARQUEE -> TextOverflow.Clip
            else -> TextOverflow.Ellipsis
        }
    )
}
