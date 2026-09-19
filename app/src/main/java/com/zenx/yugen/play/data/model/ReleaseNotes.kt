package com.zenx.yugen.play.data.model

enum class ReleaseCategory(
    val displayName: String,
    val defaultTag: String,
    val accentColor: Long,
    val secondaryColor: Long
) {
    PLAYBACK(
        displayName = "Playback & Streams",
        defaultTag = "Playback",
        accentColor = 0xFF8B5CF6,    // Electric Violet
        secondaryColor = 0xFFA78BFA
    ),
    UI_UX(
        displayName = "UI & Ergonomics",
        defaultTag = "UI & UX",
        accentColor = 0xFF06B6D4,    // Vibrant Cyan
        secondaryColor = 0xFF22D3EE
    ),
    TV(
        displayName = "Android TV",
        defaultTag = "Android TV",
        accentColor = 0xFF3B82F6,    // Royal Blue
        secondaryColor = 0xFF60A5FA
    ),
    FIXES_SECURITY(
        displayName = "Fixes & Hardening",
        defaultTag = "Fixes",
        accentColor = 0xFFF43F5E,    // Vibrant Rose
        secondaryColor = 0xFFFB7185
    ),
    OFFLINE(
        displayName = "Downloads & Offline",
        defaultTag = "Downloads",
        accentColor = 0xFF10B981,    // Emerald
        secondaryColor = 0xFF34D399
    ),
    SETTINGS(
        displayName = "Preferences & Settings",
        defaultTag = "Settings",
        accentColor = 0xFFF59E0B,    // Warm Amber
        secondaryColor = 0xFFFBBF24
    ),
    HIGHLIGHTS(
        displayName = "Highlights & Features",
        defaultTag = "Featured",
        accentColor = 0xFFEC4899,    // Neon Pink
        secondaryColor = 0xFFF472B6
    )
}

data class ReleaseItem(
    val title: String,
    val description: String = "",
    val bullets: List<String> = emptyList(),
    val category: ReleaseCategory = ReleaseCategory.HIGHLIGHTS,
    val tag: String? = null
)

data class ReleaseSection(
    val title: String,
    val items: List<ReleaseItem>,
    val category: ReleaseCategory = ReleaseCategory.HIGHLIGHTS
)

data class ReleaseNotes(
    val version: String,
    val overview: String,
    val sections: List<ReleaseSection>
)
