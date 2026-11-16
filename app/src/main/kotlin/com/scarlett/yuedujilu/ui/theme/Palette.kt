package com.scarlett.yuedujilu.ui.theme

import androidx.compose.ui.graphics.Color
import com.scarlett.yuedujilu.domain.ThemeId

data class YueDuPalette(
  val id: ThemeId,
  val background: Color,
  val surface: Color,
  val surfaceMuted: Color,
  val primary: Color,
  val onPrimary: Color,
  val primarySoft: Color,
  val text: Color,
  val textSecondary: Color,
  val textTertiary: Color,
  val stroke: Color,
  val danger: Color,
  val success: Color,
  val shadow: Color,
  val isLight: Boolean = true,
)

object Palettes {
  val MistPlum = YueDuPalette(
    id = ThemeId.MIST_PLUM,
    background = Color(0xFFF3EEF4),
    surface = Color(0xFFFFFBFE),
    surfaceMuted = Color(0xFFE8DFE9),
    primary = Color(0xFF6B2E45),
    onPrimary = Color(0xFFFFF8FB),
    primarySoft = Color(0xFFE9D4DE),
    text = Color(0xFF2A1C22),
    textSecondary = Color(0xFF746069),
    textTertiary = Color(0xFF9A8E95),
    stroke = Color(0x1A2A1C22),
    danger = Color(0xFF9B2C3B),
    success = Color(0xFF2E6B52),
    shadow = Color(0x1A3A2A40),
  )
  val PineGreen = YueDuPalette(
    id = ThemeId.PINE_GREEN,
    background = Color(0xFFEEF4F1),
    surface = Color(0xFFF8FBFA),
    surfaceMuted = Color(0xFFDCE8E3),
    primary = Color(0xFF1E5A48),
    onPrimary = Color(0xFFF4FBF8),
    primarySoft = Color(0xFFD0E4DB),
    text = Color(0xFF17241F),
    textSecondary = Color(0xFF5A6B64),
    textTertiary = Color(0xFF8A9892),
    stroke = Color(0x1A17241F),
    danger = Color(0xFF9B2C3B),
    success = Color(0xFF1E5A48),
    shadow = Color(0x1A1A3A30),
  )
  val SlateBlue = YueDuPalette(
    id = ThemeId.SLATE_BLUE,
    background = Color(0xFFEEF1F5),
    surface = Color(0xFFF8FAFC),
    surfaceMuted = Color(0xFFDDE3EB),
    primary = Color(0xFF2C4A66),
    onPrimary = Color(0xFFF5F8FB),
    primarySoft = Color(0xFFD4DEE8),
    text = Color(0xFF1B2430),
    textSecondary = Color(0xFF5B6673),
    textTertiary = Color(0xFF8B95A1),
    stroke = Color(0x1A1B2430),
    danger = Color(0xFF9B2C3B),
    success = Color(0xFF2E6B52),
    shadow = Color(0x1A1E2A3A),
  )
  val WarmIvory = YueDuPalette(
    id = ThemeId.WARM_IVORY,
    background = Color(0xFFF6F1EA),
    surface = Color(0xFFFFFBF6),
    surfaceMuted = Color(0xFFE9DFD2),
    primary = Color(0xFF6A4A32),
    onPrimary = Color(0xFFFFF8F1),
    primarySoft = Color(0xFFE8D5C4),
    text = Color(0xFF2A221C),
    textSecondary = Color(0xFF74675C),
    textTertiary = Color(0xFFA09082),
    stroke = Color(0x1A2A221C),
    danger = Color(0xFF9B2C3B),
    success = Color(0xFF3D6B45),
    shadow = Color(0x1A3A2E22),
  )

  fun of(id: ThemeId): YueDuPalette = builtins.getValue(id)

  val builtins: Map<ThemeId, YueDuPalette> = mapOf(
    ThemeId.MIST_PLUM to MistPlum,
    ThemeId.PINE_GREEN to PineGreen,
    ThemeId.SLATE_BLUE to SlateBlue,
    ThemeId.WARM_IVORY to WarmIvory,
  )

  val defaultSubtitles: Map<ThemeId, String> = mapOf(
    ThemeId.MIST_PLUM to "默认 · 雾紫底，深梅强调",
    ThemeId.PINE_GREEN to "青绿底，松柏强调",
    ThemeId.SLATE_BLUE to "灰蓝底，青石强调",
    ThemeId.WARM_IVORY to "暖白底，赭石强调",
  )
}

object Radii {
  val container = 24
  val inner = 16
  val chip = 12
  val search = 24
  val pill = 28
}

object Spaces {
  val xs = 8
  val sm = 12
  val md = 20
  val lg = 24
}
