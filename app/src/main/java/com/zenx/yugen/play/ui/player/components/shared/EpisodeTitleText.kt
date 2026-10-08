package com.zenx.yugen.play.ui.player.components.shared

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun EpisodeTitleText(
    number: Float,
    rawTitle: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    maxLines: Int = 1,
    isCardFormat: Boolean = false
) {
    val text = if (isCardFormat) {
        EpisodeTitleFormatter.formatCardTitle(number, rawTitle)
    } else {
        EpisodeTitleFormatter.formatDisplayTitle(number, rawTitle)
    }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
