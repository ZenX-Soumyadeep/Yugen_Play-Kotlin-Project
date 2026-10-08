package com.zenx.yugen.play.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object YugenShape {
    val xs = RoundedCornerShape(4.dp)
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(20.dp)
    val full = CircleShape

    // Semantic shapes
    val card = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(20.dp)
    val tab = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(100.dp)
    val dialog = RoundedCornerShape(24.dp)
}
