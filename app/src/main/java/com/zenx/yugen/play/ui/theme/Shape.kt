package com.zenx.yugen.play.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object YugenShape {
    val xs = RoundedCornerShape(8.dp)
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(20.dp)
    val xl = RoundedCornerShape(24.dp)
    val xxl = RoundedCornerShape(28.dp)
    val full = CircleShape

    // Semantic shapes
    val card = RoundedCornerShape(16.dp)
    val cardLg = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(14.dp)
    val chip = RoundedCornerShape(100.dp)
    val tab = RoundedCornerShape(14.dp)
    val pill = RoundedCornerShape(100.dp)
    val dialog = RoundedCornerShape(24.dp)
}
