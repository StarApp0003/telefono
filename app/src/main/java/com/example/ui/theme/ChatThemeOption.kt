package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class ChatThemeOption(
    val id: Int,
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val gradientColors: List<Color>,
    val receivedBubbleLight: Color,
    val receivedBubbleDark: Color,
    val onSentColor: Color = Color.White
) {
    companion object {
        val ALL_THEMES = listOf(
            ChatThemeOption(
                id = 0,
                name = "Azul Océano",
                primaryColor = Color(0xFF2563EB),
                secondaryColor = Color(0xFF1D4ED8),
                gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                receivedBubbleLight = Color(0xFFE2E8F0),
                receivedBubbleDark = Color(0xFF1E293B)
            ),
            ChatThemeOption(
                id = 1,
                name = "Verde WhatsApp",
                primaryColor = Color(0xFF059669),
                secondaryColor = Color(0xFF047857),
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                receivedBubbleLight = Color(0xFFE8F5E9),
                receivedBubbleDark = Color(0xFF132D1B)
            ),
            ChatThemeOption(
                id = 2,
                name = "Púrpura Imperial",
                primaryColor = Color(0xFF7C3AED),
                secondaryColor = Color(0xFF6D28D9),
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
                receivedBubbleLight = Color(0xFFF3E8FF),
                receivedBubbleDark = Color(0xFF281347)
            ),
            ChatThemeOption(
                id = 3,
                name = "Rosa Rubí",
                primaryColor = Color(0xFFE11D48),
                secondaryColor = Color(0xFFBE123C),
                gradientColors = listOf(Color(0xFFF43F5E), Color(0xFFE11D48)),
                receivedBubbleLight = Color(0xFFFFE4E6),
                receivedBubbleDark = Color(0xFF3B0B18)
            ),
            ChatThemeOption(
                id = 4,
                name = "Teal Turquesa",
                primaryColor = Color(0xFF0D9488),
                secondaryColor = Color(0xFF0F766E),
                gradientColors = listOf(Color(0xFF14B8A6), Color(0xFF0D9488)),
                receivedBubbleLight = Color(0xFFCCFBF1),
                receivedBubbleDark = Color(0xFF0B2E2B)
            ),
            ChatThemeOption(
                id = 5,
                name = "Ámbar Solar",
                primaryColor = Color(0xFFD97706),
                secondaryColor = Color(0xFFB45309),
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                receivedBubbleLight = Color(0xFFFEF3C7),
                receivedBubbleDark = Color(0xFF3D2005)
            ),
            ChatThemeOption(
                id = 6,
                name = "Carbón Minimal",
                primaryColor = Color(0xFF334155),
                secondaryColor = Color(0xFF1E293B),
                gradientColors = listOf(Color(0xFF475569), Color(0xFF1E293B)),
                receivedBubbleLight = Color(0xFFF1F5F9),
                receivedBubbleDark = Color(0xFF18202F)
            )
        )

        fun getById(id: Int): ChatThemeOption {
            return ALL_THEMES.find { it.id == id } ?: ALL_THEMES.first()
        }
    }
}
