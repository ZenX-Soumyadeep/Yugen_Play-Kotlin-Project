package com.zenx.yugen.play.ui.tv

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TvSpacing {
    val overscanH: Dp = 48.dp
    val overscanV: Dp = 32.dp
    val swimlaneGap: Dp = 28.dp
    val itemGap: Dp = 16.dp
    val chipGap: Dp = 12.dp

    val billboardHeight: Dp = 420.dp
    val billboardTextWidth: Dp = 660.dp

    val cardWidth: Dp = 160.dp
    val cardHeight: Dp = 226.dp

    val continueCardWidth: Dp = 220.dp
    val continueCardHeight: Dp = 124.dp

    val heroContentPaddingH: Dp = 48.dp
    val heroContentPaddingTop: Dp = 80.dp
    val episodeCardWidth: Dp = 196.dp
    val episodeCardHeight: Dp = 110.dp

    val panelWidth: Dp = 420.dp
    val progressBarH: Dp = 7.dp
    val seekThumbSize: Dp = 18.dp
}

object TvType {
    val billboardTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp
    )

    val billboardDescription = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val billboardMeta = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
    )

    val swimlaneHeader = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    val cardTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )

    val cardSubtitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val badge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )

    val chip = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp
    )

    val button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    val detailTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp
    )

    val detailSectionHeader = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )

    val detailBodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val detailBadge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    )

    val detailButtonLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    val detailTopBarTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )

    val playerTimestamp = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp
    )

    val playerPillLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    )

    val playerPillBadge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
}

