package com.nexus.core.ui.theme

import androidx.compose.ui.graphics.Color

// Palette shared with IRSO BLOG (Tailwind's gray and amber scales): neutral surfaces,
// near-black primary actions and an amber accent for highlights.

val Gray50 = Color(0xFFF9FAFB)
val Gray100 = Color(0xFFF3F4F6)
val Gray200 = Color(0xFFE5E7EB)
val Gray300 = Color(0xFFD1D5DB)
val Gray400 = Color(0xFF9CA3AF)
val Gray500 = Color(0xFF6B7280)
val Gray600 = Color(0xFF4B5563)
val Gray700 = Color(0xFF374151)
val Gray800 = Color(0xFF1F2937)
val Gray900 = Color(0xFF111827)
val Gray950 = Color(0xFF030712)

val Amber50 = Color(0xFFFFFBEB)
val Amber100 = Color(0xFFFEF3C7)
val Amber300 = Color(0xFFFCD34D)
val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)
val Amber600 = Color(0xFFD97706)
val Amber700 = Color(0xFFB45309)
val Amber800 = Color(0xFF92400E)

val Emerald500 = Color(0xFF10B981)
val Emerald600 = Color(0xFF059669)
val Red500 = Color(0xFFEF4444)
val Red600 = Color(0xFFDC2626)

// Brand names used across the app, now pointing at the shared palette
val NexusPurple = Amber500
val NexusPurpleLight = Amber300
val NexusPurpleDark = Amber700
val NexusAccentTeal = Emerald500
val NexusAccentTealDark = Emerald600

// Dark theme surfaces
val NexusDarkBg = Gray950
val NexusDarkSurface = Gray900
val NexusDarkSurfaceVariant = Gray800
val NexusDarkOnSurface = Gray100
val NexusDarkOnSurfaceVariant = Gray400

// Light theme surfaces
val NexusLightBg = Color.White
val NexusLightSurface = Color.White
val NexusLightSurfaceVariant = Gray100
val NexusLightOnSurface = Gray900
val NexusLightOnSurfaceVariant = Gray500

// Semantic
val NexusError = Red500
val NexusErrorContainer = Color(0xFF3B1212)
val NexusSuccess = Emerald500
val NexusWarning = Amber400

/**
 * Colors a circle can take (circles/{id}.colorIndex, 0-7), the same set IRSO BLOG uses for
 * avatars: amber, indigo, emerald, rose, sky, violet, orange and teal.
 */
val CirclePalette = listOf(
    Color(0xFFF59E0B),
    Color(0xFF6366F1),
    Color(0xFF10B981),
    Color(0xFFF43F5E),
    Color(0xFF0EA5E9),
    Color(0xFF8B5CF6),
    Color(0xFFF97316),
    Color(0xFF14B8A6),
)

fun circleColor(index: Int): Color = CirclePalette[index.mod(CirclePalette.size)]

/** Same color for the same name every time, like the blog's initials avatars. */
fun avatarColor(seed: String): Color = circleColor(seed.hashCode())
