package com.zenx.yugen.play.util

import com.zenx.yugen.play.data.model.ReleaseCategory
import com.zenx.yugen.play.data.model.ReleaseItem
import com.zenx.yugen.play.data.model.ReleaseNotes
import com.zenx.yugen.play.data.model.ReleaseSection

object ReleaseNotesParser {

    private val SKIP_SECTIONS = listOf(
        "installation",
        "download",
        "assets",
        "full changelog",
        "installing"
    )

    fun parse(markdown: String, targetVersion: String): ReleaseNotes {
        val lines = markdown.lines()
        val overviewLines = mutableListOf<String>()
        val sections = mutableListOf<ReleaseSection>()

        var currentSectionTitle: String? = null
        var currentSectionItems = mutableListOf<ReleaseItem>()
        var currentItemTitle: String? = null
        var currentItemBullets = mutableListOf<String>()
        var inOverview = true

        fun flushCurrentItem() {
            val title = currentItemTitle?.trim()
            if (!title.isNullOrBlank()) {
                val cleanedTitle = cleanMarkdown(title)
                val cleanedBullets = currentItemBullets.map { cleanMarkdown(it) }.filter { it.isNotBlank() }
                val category = determineCategory("$currentSectionTitle $cleanedTitle ${cleanedBullets.joinToString(" ")}")
                currentSectionItems.add(
                    ReleaseItem(
                        title = cleanedTitle,
                        bullets = cleanedBullets,
                        category = category,
                        tag = category.defaultTag
                    )
                )
            }
            currentItemTitle = null
            currentItemBullets.clear()
        }

        fun flushCurrentSection() {
            flushCurrentItem()
            val sectionTitle = currentSectionTitle?.trim()
            if (!sectionTitle.isNullOrBlank() && currentSectionItems.isNotEmpty()) {
                val category = determineCategory(sectionTitle)
                sections.add(
                    ReleaseSection(
                        title = sectionTitle,
                        items = currentSectionItems.toList(),
                        category = category
                    )
                )
            }
            currentSectionTitle = null
            currentSectionItems = mutableListOf()
        }

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // Skip top-level version header # 🚀 YugenPlay v1.3.2
            if (line.startsWith("# ") && !line.startsWith("### ")) {
                continue
            }

            // Divider marks end of overview
            if (line.startsWith("---") || line.startsWith("***")) {
                inOverview = false
                continue
            }

            // Section Header (### or ##)
            if (line.startsWith("### ") || line.startsWith("## ")) {
                inOverview = false
                flushCurrentSection()

                val secTitle = line.replace(Regex("^#+\\s*"), "").trim()
                val lower = secTitle.lowercase()
                if (SKIP_SECTIONS.any { lower.contains(it) }) {
                    currentSectionTitle = null
                    continue
                }

                currentSectionTitle = secTitle
                continue
            }

            // Collect overview text before first section or divider
            if (inOverview) {
                if (!line.startsWith("*") && !line.startsWith("-")) {
                    overviewLines.add(cleanMarkdown(line))
                    continue
                } else {
                    inOverview = false
                }
            }

            // If we are currently inside a skipped section (currentSectionTitle is null), skip line
            if (currentSectionTitle == null) {
                // Check if this is a bullet without section
                if (line.startsWith("* ") || line.startsWith("- ")) {
                    currentSectionTitle = "Updates & Highlights"
                } else {
                    continue
                }
            }

            // Skip changelog / compare link lines
            if (line.contains("compare/", ignoreCase = true) || line.contains("Full Changelog", ignoreCase = true)) {
                continue
            }

            // Bullet with bold title: * **Title**: description or - **Title**: description
            val boldMatch = Regex("^(\\*|\\-)\\s+\\*\\*(.*?)\\*\\*:?\\s*(.*)$").find(line)
            if (boldMatch != null) {
                flushCurrentItem()
                val itemTitle = boldMatch.groupValues[2].trim()
                val inlineDesc = boldMatch.groupValues[3].trim()
                currentItemTitle = itemTitle
                if (inlineDesc.isNotBlank()) {
                    currentItemBullets.add(inlineDesc)
                }
                continue
            }

            // Standard bullet point: * Item or - Item
            if (line.startsWith("* ") || line.startsWith("- ")) {
                val bulletText = line.replace(Regex("^(\\*|\\-)\\s*"), "").trim()
                // If it was indented in the raw markdown, treat as sub-bullet
                val isIndented = rawLine.startsWith("  ") || rawLine.startsWith("\t")
                if (isIndented && currentItemTitle != null) {
                    currentItemBullets.add(bulletText)
                } else {
                    flushCurrentItem()
                    currentItemTitle = bulletText
                }
                continue
            }

            // Continuation text of previous item
            if (currentItemTitle != null) {
                currentItemBullets.add(line)
            }
        }

        flushCurrentSection()

        val finalOverview = if (overviewLines.isNotEmpty()) {
            overviewLines.joinToString(" ")
        } else {
            "Here is what's newly added, refined, and upgraded in YugenPlay v$targetVersion."
        }

        // Fallback if parsing yielded 0 sections
        if (sections.isEmpty()) {
            return fallbackReleaseNotes(targetVersion)
        }

        return ReleaseNotes(
            version = targetVersion,
            overview = finalOverview,
            sections = sections
        )
    }

    private fun determineCategory(text: String): ReleaseCategory {
        val lower = text.lowercase()
        return when {
            lower.contains("tv") || lower.contains("leanback") || lower.contains("remote") || lower.contains("dpad") || lower.contains("d-pad") -> ReleaseCategory.TV
            lower.contains("playback") || lower.contains("player") || lower.contains("stream") || lower.contains("video") ||
                    lower.contains("audio") || lower.contains("hls") || lower.contains("cdn") || lower.contains("scraper") ||
                    lower.contains("anikoto") || lower.contains("megaplay") || lower.contains("404") || lower.contains("host") -> ReleaseCategory.PLAYBACK
            lower.contains("ui") || lower.contains("ux") || lower.contains("ergonomics") || lower.contains("bottom bar") ||
                    lower.contains("design") || lower.contains("layout") || lower.contains("theme") || lower.contains("bar") ||
                    lower.contains("animation") || lower.contains("touch") || lower.contains("thumbnail") || lower.contains("search") -> ReleaseCategory.UI_UX
            lower.contains("fix") || lower.contains("bug") || lower.contains("security") || lower.contains("token") ||
                    lower.contains("resolved") || lower.contains("patch") || lower.contains("hardening") || lower.contains("crash") -> ReleaseCategory.FIXES_SECURITY
            lower.contains("download") || lower.contains("offline") || lower.contains("storage") -> ReleaseCategory.OFFLINE
            lower.contains("setting") || lower.contains("preference") || lower.contains("subtitle") || lower.contains("sync") ||
                    lower.contains("airing") || lower.contains("schedule") -> ReleaseCategory.SETTINGS
            else -> ReleaseCategory.HIGHLIGHTS
        }
    }

    private fun cleanMarkdown(input: String): String {
        return input
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // [text](url) -> text
            .replace("**", "")                            // bold
            .replace("`", "")                             // inline code
            .replace(Regex("^#+\\s*"), "")                 // heading markers
            .replace(Regex("^(\\-|\\*)\\s*"), "")          // bullet points
            .trim()
    }

    fun fallbackReleaseNotes(version: String): ReleaseNotes {
        return ReleaseNotes(
            version = version,
            overview = "A critical hotfix and UX polish update addressing playback segment loading across Mobile and Android TV, integrating updated MegaPlay stream token signatures, and improving bottom bar reachability.",
            sections = listOf(
                ReleaseSection(
                    title = "🎬 Critical Playback & Scraper Fixes",
                    category = ReleaseCategory.PLAYBACK,
                    items = listOf(
                        ReleaseItem(
                            title = "Resolved Segment 404 Playback Failures",
                            bullets = listOf(
                                "Fixed an issue where HLS media segment requests were incorrectly rewritten to an index-only host, causing immediate 404 playback errors.",
                                "Preserved direct authoritative segment routing (bb.akirax.buzz), restoring instant video playback across both Phone and Android TV."
                            ),
                            category = ReleaseCategory.PLAYBACK,
                            tag = "Playback"
                        ),
                        ReleaseItem(
                            title = "MegaPlay HMAC-SHA256 Token Signing",
                            bullets = listOf(
                                "Synced with the latest Anikoto updates by adding HMAC-SHA256 signature verification for decrypted MegaPlay streams, preventing token expiration."
                            ),
                            category = ReleaseCategory.PLAYBACK,
                            tag = "Scraper"
                        )
                    )
                ),
                ReleaseSection(
                    title = "📱 UI & Ergonomics",
                    category = ReleaseCategory.UI_UX,
                    items = listOf(
                        ReleaseItem(
                            title = "Thumb-Sized Floating Bottom Bar",
                            bullets = listOf(
                                "Increased touch target height from 42dp to 52dp for effortless one-handed thumb interaction.",
                                "Increased icon size to 24dp and optimized vertical padding (11dp) while maintaining the compact horizontal pill width."
                            ),
                            category = ReleaseCategory.UI_UX,
                            tag = "UI & UX"
                        )
                    )
                )
            )
        )
    }
}
