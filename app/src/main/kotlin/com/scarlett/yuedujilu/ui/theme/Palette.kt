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
  val surfaceRaised: Color,
  val track: Color,
  val tabBar: Color,
  val tabBarAlpha: Float = 0.92f,
  val isLight: Boolean = true,
)

object Palettes {
  // 内置色与设计交接 theme-tokens.json 默认雾紫等四套对齐，避免解析失败时整份回退到旧色。
  val MistPlum = YueDuPalette(
    id = ThemeId.MIST_PLUM,
    background = Color(0xFFF1EEF4),
    surface = Color(0xFFFAF8FB),
    surfaceMuted = Color(0xFFE8E2E6),
    primary = Color(0xFF5E314C),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFECE6EA),
    text = Color(0xFF1B161F),
    textSecondary = Color(0xFF58515F),
    textTertiary = Color(0xFF58515F),
    stroke = Color(0xFFCBC4D0),
    danger = Color(0xFF9B2C3B),
    success = Color(0xFF2E6B52),
    shadow = Color(0x14000000),
    surfaceRaised = Color(0xFFFFFFFF),
    track = Color(0xFFE8E2E6),
    tabBar = Color(0xFFFEFEFE),
    tabBarAlpha = 0.92f,
  )
  val PineGreen = YueDuPalette(
    id = ThemeId.PINE_GREEN,
    background = Color(0xFFEEF3F1),
    surface = Color(0xFFF7FBFA),
    surfaceMuted = Color(0xFFDFE8E6),
    primary = Color(0xFF1B5A50),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFE4EBEA),
    text = Color(0xFF141C1A),
    textSecondary = Color(0xFF4C5B56),
    textTertiary = Color(0xFF4C5B56),
    stroke = Color(0xFFC3CEC9),
    danger = Color(0xFF9B2C3B),
    success = Color(0xFF1B5A50),
    shadow = Color(0x14000000),
    surfaceRaised = Color(0xFFFFFFFF),
    track = Color(0xFFDFE8E6),
    tabBar = Color(0xFFFEFEFE),
    tabBarAlpha = 0.92f,
  )
  val SlateBlue = YueDuPalette(
    id = ThemeId.SLATE_BLUE,
    background = Color(0xFFEEF1F5),
    surface = Color(0xFFF7F9FC),
    surfaceMuted = Color(0xFFE3E6EA),
    primary = Color(0xFF3A4E66),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFE7EAED),
    text = Color(0xFF15181E),
    textSecondary = Color(0xFF515965),
    textTertiary = Color(0xFF515965),
    stroke = Color(0xFFC4CAD3),
    danger = Color(0xFF9B2C3B),
    success = Color(0xFF2E6B52),
    shadow = Color(0x14000000),
    surfaceRaised = Color(0xFFFFFFFF),
    track = Color(0xFFE3E6EA),
    tabBar = Color(0xFFFEFEFE),
    tabBarAlpha = 0.92f,
  )
  val WarmIvory = YueDuPalette(
    id = ThemeId.WARM_IVORY,
    background = Color(0xFFF3EFE6),
    surface = Color(0xFFFAF7F0),
    surfaceMuted = Color(0xFFEAE2DD),
    primary = Color(0xFF6A3A2E),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFEDE6E1),
    text = Color(0xFF1A1713),
    textSecondary = Color(0xFF5F584F),
    textTertiary = Color(0xFF5F584F),
    stroke = Color(0xFFD2CABE),
    danger = Color(0xFF9B2C3B),
    success = Color(0xFF3D6B45),
    shadow = Color(0x14000000),
    surfaceRaised = Color(0xFFFFFDF9),
    track = Color(0xFFEAE2DD),
    tabBar = Color(0xFFFEFCF7),
    tabBarAlpha = 0.92f,
  )

  fun of(id: ThemeId): YueDuPalette = builtins.getValue(id)

  val builtins: Map<ThemeId, YueDuPalette> = mapOf(
    ThemeId.MIST_PLUM to MistPlum,
    ThemeId.PINE_GREEN to PineGreen,
    ThemeId.SLATE_BLUE to SlateBlue,
    ThemeId.WARM_IVORY to WarmIvory,
  )

  val defaultSubtitles: Map<ThemeId, String> = mapOf(
    ThemeId.MIST_PLUM to "默认 · 雾紫",
    ThemeId.PINE_GREEN to "青绿",
    ThemeId.SLATE_BLUE to "灰蓝",
    ThemeId.WARM_IVORY to "暖白",
  )
}

object Radii {
  val container = 24
  val inner = 16
  val chip = 12
  val search = 24
  val pill = 12
  val sm = 8
}

object Spaces {
  val xs = 8
  val sm = 12
  val md = 20
  val lg = 24
}
