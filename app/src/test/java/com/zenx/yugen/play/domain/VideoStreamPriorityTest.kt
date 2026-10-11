package com.zenx.yugen.play.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoStreamPriorityTest {

    @Test
    fun testServerReliabilityScoring() {
        val vidstream2 = VideoStream(
            quality = "Auto [SUB]",
            url = "https://fetch.nexabloom.top/anime/master.m3u8",
            serverName = "Vidstream-2"
        )
        val vidstream1 = VideoStream(
            quality = "Auto [SUB]",
            url = "https://fetch.nexabloom.top/anime/master.m3u8",
            serverName = "Vidstream-1"
        )
        val hd2 = VideoStream(
            quality = "Auto [SUB]",
            url = "https://ncdn.imgnex.top/anime/master.m3u8",
            serverName = "HD-2"
        )
        val standardServer = VideoStream(
            quality = "Auto [SUB]",
            url = "https://streamwish.to/master.m3u8",
            serverName = "StreamWish"
        )
        val hd1 = VideoStream(
            quality = "Auto [SUB]",
            url = "https://megap.shiora.top/master.m3u8?s=tcdn",
            serverName = "HD-1"
        )

        assertTrue(vidstream2.serverReliabilityScore() > vidstream1.serverReliabilityScore())
        assertTrue(vidstream1.serverReliabilityScore() > hd2.serverReliabilityScore())
        assertTrue(hd2.serverReliabilityScore() > standardServer.serverReliabilityScore())
        assertTrue(standardServer.serverReliabilityScore() > hd1.serverReliabilityScore())

        assertEquals(100, vidstream2.serverReliabilityScore())
        assertEquals(90, vidstream1.serverReliabilityScore())
        assertEquals(85, hd2.serverReliabilityScore())
        assertEquals(70, standardServer.serverReliabilityScore())
        assertEquals(10, hd1.serverReliabilityScore())
    }

    @Test
    fun testStreamRankingOrder() {
        val hd1 = VideoStream(
            quality = "Auto [SUB]",
            url = "https://megap.shiora.top/master.m3u8?s=tcdn",
            serverName = "HD-1"
        )
        val vidstream2 = VideoStream(
            quality = "Auto [SUB]",
            url = "https://fetch.nexabloom.top/anime/master.m3u8",
            serverName = "Vidstream-2"
        )
        val vidstream1 = VideoStream(
            quality = "Auto [SUB]",
            url = "https://fetch.nexabloom.top/anime/master.m3u8",
            serverName = "Vidstream-1"
        )
        val hd2 = VideoStream(
            quality = "Auto [SUB]",
            url = "https://ncdn.imgnex.top/anime/master.m3u8",
            serverName = "HD-2"
        )

        // Raw list matching Anikoto's scraper output where HD-1 is first
        val rawScrapedList = listOf(hd1, vidstream2, vidstream1, hd2)

        val sorted = rawScrapedList.sortedByDescending { it.priorityScore() }

        assertEquals("Vidstream-2", sorted[0].serverName)
        assertEquals("Vidstream-1", sorted[1].serverName)
        assertEquals("HD-2", sorted[2].serverName)
        assertEquals("HD-1", sorted[3].serverName)
    }

    @Test
    fun testTcdnUrlPenalizedEvenWithoutServerName() {
        val anonymousTcdnStream = VideoStream(
            quality = "Auto",
            url = "https://p16-ad-site-sign-sg.tiktokcdn.com/stream.m3u8",
            serverName = null
        )
        val anonymousCleanStream = VideoStream(
            quality = "Auto",
            url = "https://cdn.nexabloom.top/stream.m3u8",
            serverName = null
        )

        assertTrue(anonymousCleanStream.priorityScore() > anonymousTcdnStream.priorityScore())
        assertEquals(10, anonymousTcdnStream.serverReliabilityScore())
    }
}
