package com.scarlett.yuedujilu.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.scarlett.yuedujilu.domain.ThemeId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 可替换主题令牌。
 * 优先读 filesDir/theme-tokens.json（设计稿交接后可直接覆盖），
 * 否则读 assets/theme-tokens.json，再否则用内置默认值。
 * 更换 JSON 即可改色与圆角间距，不必改界面代码。
 */
@Serializable
data class ThemeTokenFile(
  val version: Int = 1,
  val radiiDp: RadiiTokens = RadiiTokens(),
  val spacesDp: SpaceTokens = SpaceTokens(),
  val themes: List<PaletteTokens> = emptyList(),
)

@Serializable
data class RadiiTokens(
  val container: Int = 24,
  val inner: Int = 16,
  val chip: Int = 12,
  val search: Int = 24,
  val pill: Int = 28,
)

@Serializable
data class SpaceTokens(
  val xs: Int = 8,
  val sm: Int = 12,
  val md: Int = 20,
  val lg: Int = 24,
)

@Serializable
data class PaletteTokens(
  val id: String,
  val displayName: String? = null,
  val subtitle: String? = null,
  val background: String,
  val surface: String,
  val surfaceMuted: String,
  val primary: String,
  val onPrimary: String,
  val primarySoft: String,
  val text: String,
  val textSecondary: String,
  val textTertiary: String,
  val stroke: String,
  val danger: String,
  val success: String,
  val shadow: String,
  val isLight: Boolean = true,
)

data class ThemeCatalog(
  val radii: RadiiTokens = RadiiTokens(),
  val spaces: SpaceTokens = SpaceTokens(),
  val palettes: Map<ThemeId, YueDuPalette> = Palettes.builtins,
  val subtitles: Map<ThemeId, String> = Palettes.defaultSubtitles,
  val source: String = "builtin",
) {
  fun palette(id: ThemeId): YueDuPalette = palettes[id] ?: Palettes.builtins.getValue(id)
  fun subtitle(id: ThemeId): String = subtitles[id] ?: Palettes.defaultSubtitles.getValue(id)

  companion object {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun builtin(): ThemeCatalog = ThemeCatalog()

    fun parse(text: String, source: String): ThemeCatalog {
      val file = json.decodeFromString(ThemeTokenFile.serializer(), text)
      val map = linkedMapOf<ThemeId, YueDuPalette>()
      val subs = linkedMapOf<ThemeId, String>()
      for (token in file.themes) {
        val id = runCatching { ThemeId.valueOf(token.id) }.getOrNull() ?: continue
        map[id] = token.toPalette(id)
        token.subtitle?.let { subs[id] = it }
      }
      return ThemeCatalog(
        radii = file.radiiDp,
        spaces = file.spacesDp,
        palettes = Palettes.builtins + map,
        subtitles = Palettes.defaultSubtitles + subs,
        source = source,
      )
    }

    fun load(context: Context): ThemeCatalog {
      val override = File(context.filesDir, "theme-tokens.json")
      val fromOverride = override.takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }
      if (!fromOverride.isNullOrBlank()) {
        return runCatching { parse(fromOverride, "filesDir") }.getOrElse { builtin() }
      }
      val fromAssets = runCatching {
        context.assets.open("theme-tokens.json").bufferedReader().use { it.readText() }
      }.getOrNull()
      if (!fromAssets.isNullOrBlank()) {
        return runCatching { parse(fromAssets, "assets") }.getOrElse { builtin() }
      }
      return builtin()
    }
  }
}

fun PaletteTokens.toPalette(id: ThemeId): YueDuPalette = YueDuPalette(
  id = id,
  background = parseColor(background),
  surface = parseColor(surface),
  surfaceMuted = parseColor(surfaceMuted),
  primary = parseColor(primary),
  onPrimary = parseColor(onPrimary),
  primarySoft = parseColor(primarySoft),
  text = parseColor(text),
  textSecondary = parseColor(textSecondary),
  textTertiary = parseColor(textTertiary),
  stroke = parseColor(stroke),
  danger = parseColor(danger),
  success = parseColor(success),
  shadow = parseColor(shadow),
  isLight = isLight,
)

/** 接受 #RGB、#RRGGBB、#AARRGGBB。 */
fun parseColor(raw: String): Color {
  val h = raw.trim().removePrefix("#")
  val value = when (h.length) {
    3 -> h.map { "$it$it" }.joinToString("").toLong(16) or 0xFF000000L
    6 -> h.toLong(16) or 0xFF000000L
    8 -> h.toLong(16)
    else -> error("不支持的颜色：$raw")
  }
  return Color(value.toInt())
}
