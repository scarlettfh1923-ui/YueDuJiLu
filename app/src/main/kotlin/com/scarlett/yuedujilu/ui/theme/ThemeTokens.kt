package com.scarlett.yuedujilu.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.scarlett.yuedujilu.domain.ThemeId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.pow

/**
 * 可替换主题令牌。
 * 同时识别两套 JSON：
 * 1. 设计交接稿（meta / themes 对象 / contrast / radiusDp），id 为 mist_purple 等
 * 2. 本项目早期列表格式（themes: [ { id: MIST_PLUM, ... } ]）
 * 不能把交接稿直接当列表解析，否则会整份回退到内置色。
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
  val container: Int = 22,
  val inner: Int = 22,
  val chip: Int = 22,
  val search: Int = 22,
  val pill: Int = 22,
  val sm: Int = 22,
)

@Serializable
data class SpaceTokens(
  val xs: Int = 8,
  val sm: Int = 22,
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
  val surfaceMuted: String? = null,
  val surfaceRaised: String? = null,
  val primary: String,
  val onPrimary: String,
  val primarySoft: String? = null,
  val primaryContainer: String? = null,
  val text: String,
  val textSecondary: String? = null,
  val secondary: String? = null,
  val textTertiary: String? = null,
  val stroke: String? = null,
  val outline: String? = null,
  val track: String? = null,
  val tabBarComposite: String? = null,
  val tabBarAlpha: Float? = null,
  val danger: String? = null,
  val success: String? = null,
  val shadow: String? = null,
  val isLight: Boolean = true,
)

data class ThemeCatalog(
  val radii: RadiiTokens = RadiiTokens(),
  val spaces: SpaceTokens = SpaceTokens(),
  val palettes: Map<ThemeId, YueDuPalette> = Palettes.builtins,
  val subtitles: Map<ThemeId, String> = Palettes.defaultSubtitles,
  val source: String = "builtin",
  val schema: String = "builtin",
) {
  fun palette(id: ThemeId): YueDuPalette = palettes[id] ?: Palettes.builtins.getValue(id)
  fun subtitle(id: ThemeId): String = subtitles[id] ?: Palettes.defaultSubtitles.getValue(id)

  companion object {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun builtin(): ThemeCatalog = ThemeCatalog()

    fun parse(text: String, source: String): ThemeCatalog {
      val root = json.parseToJsonElement(text).jsonObject
      val themesEl = root["themes"]
      val looksLikeHandoff = root["meta"] != null || root["radiusDp"] != null ||
        (themesEl is JsonObject)
      return if (looksLikeHandoff && themesEl is JsonObject) {
        parseHandoff(root, source)
      } else {
        parseLegacy(text, source)
      }
    }

    private fun parseLegacy(text: String, source: String): ThemeCatalog {
      val file = json.decodeFromString(ThemeTokenFile.serializer(), text)
      val map = linkedMapOf<ThemeId, YueDuPalette>()
      val subs = linkedMapOf<ThemeId, String>()
      for (token in file.themes) {
        val id = mapThemeId(token.id) ?: continue
        map[id] = token.toPalette(id)
        token.subtitle?.let { subs[id] = it }
        token.displayName?.let { if (subs[id] == null) subs[id] = it }
      }
      if (map.isEmpty()) error("列表格式主题令牌没有可识别的 id")
      return ThemeCatalog(
        radii = file.radiiDp,
        spaces = file.spacesDp,
        palettes = Palettes.builtins + map,
        subtitles = Palettes.defaultSubtitles + subs,
        source = source,
        schema = "legacy-list",
      )
    }

    private fun parseHandoff(root: JsonObject, source: String): ThemeCatalog {
      val themesObj = root["themes"]!!.jsonObject
      val map = linkedMapOf<ThemeId, YueDuPalette>()
      val subs = linkedMapOf<ThemeId, String>()
      for ((key, value) in themesObj) {
        val obj = value.jsonObject
        val rawId = obj.str("id") ?: key
        val id = mapThemeId(rawId) ?: mapThemeId(key) ?: continue
        val token = PaletteTokens(
          id = rawId,
          displayName = obj.str("label"),
          subtitle = obj.str("label"),
          background = obj.str("background") ?: continue,
          surface = obj.str("surface") ?: continue,
          surfaceRaised = obj.str("surfaceRaised"),
          surfaceMuted = obj.str("track"),
          primary = obj.str("primary") ?: continue,
          onPrimary = obj.str("onPrimary") ?: continue,
          primaryContainer = obj.str("primaryContainer"),
          primarySoft = obj.str("primaryContainer"),
          text = obj.str("text") ?: continue,
          secondary = obj.str("secondary"),
          textSecondary = obj.str("secondary"),
          outline = obj.str("outline"),
          stroke = obj.str("outline"),
          track = obj.str("track"),
          tabBarComposite = obj.str("tabBarComposite"),
          tabBarAlpha = obj.float("tabBarAlpha"),
          isLight = true,
        )
        map[id] = token.toPalette(id)
        val label = obj.str("label")
        val isDefault = obj["isDefault"]?.jsonPrimitive?.booleanOrNull == true
        subs[id] = when {
          isDefault && label != null -> "默认 · $label"
          label != null -> label
          else -> Palettes.defaultSubtitles.getValue(id)
        }
      }
      if (map.size < ThemeId.entries.size) {
        error("交接稿未能映射全部四套主题，已识别：${map.keys}")
      }
      val radii = root["radiusDp"]?.jsonObject
      val radiiTokens = RadiiTokens(
        container = radii?.int("xl") ?: 24,
        inner = radii?.int("lg") ?: 16,
        chip = radii?.int("md") ?: 12,
        search = radii?.int("xl") ?: 24,
        pill = radii?.int("md") ?: 12,
        sm = radii?.int("sm") ?: 8,
      )
      return ThemeCatalog(
        radii = radiiTokens,
        spaces = SpaceTokens(),
        palettes = Palettes.builtins + map,
        subtitles = Palettes.defaultSubtitles + subs,
        source = source,
        schema = "handoff-meta-themes",
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

fun mapThemeId(raw: String): ThemeId? = when (raw.trim()) {
  "mist_purple", "MIST_PLUM", "雾紫" -> ThemeId.MIST_PLUM
  "pine_teal", "PINE_GREEN", "青绿" -> ThemeId.PINE_GREEN
  "slate_blue", "SLATE_BLUE", "灰蓝" -> ThemeId.SLATE_BLUE
  "warm_paper", "WARM_IVORY", "暖白" -> ThemeId.WARM_IVORY
  else -> runCatching { ThemeId.valueOf(raw.trim()) }.getOrNull()
}

fun PaletteTokens.toPalette(id: ThemeId): YueDuPalette {
  val fallback = Palettes.builtins.getValue(id)
  val textSec = parseColor(textSecondary ?: secondary ?: colorHex(fallback.textSecondary))
  val raised = parseColor(surfaceRaised ?: surface)
  val muted = parseColor(track ?: surfaceMuted ?: colorHex(fallback.surfaceMuted))
  val container = parseColor(primaryContainer ?: primarySoft ?: colorHex(fallback.primarySoft))
  return YueDuPalette(
    id = id,
    background = parseColor(background),
    surface = parseColor(surface),
    surfaceMuted = muted,
    primary = parseColor(primary),
    onPrimary = parseColor(onPrimary),
    primarySoft = container,
    text = parseColor(text),
    textSecondary = textSec,
    textTertiary = parseColor(textTertiary ?: textSecondary ?: secondary ?: colorHex(fallback.textSecondary)),
    stroke = parseColor(stroke ?: outline ?: colorHex(fallback.stroke)),
    danger = parseColor(danger ?: colorHex(fallback.danger)),
    success = parseColor(success ?: colorHex(fallback.success)),
    shadow = parseColor(shadow ?: "14000000"),
    surfaceRaised = raised,
    track = muted,
    tabBar = parseColor(tabBarComposite ?: surfaceRaised ?: surface),
    tabBarAlpha = tabBarAlpha ?: 0.92f,
    isLight = isLight,
  )
}

fun colorHex(color: Color): String = "%08X".format(color.toArgb())

/** 接受 #RGB、#RRGGBB、#AARRGGBB，或无 # 的同样位数。 */
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

/** WCAG 2.x 相对亮度对比度 (L1+0.05)/(L2+0.05)。 */
fun contrastRatio(a: Color, b: Color): Double {
  val l1 = relativeLuminance(a)
  val l2 = relativeLuminance(b)
  val hi = maxOf(l1, l2)
  val lo = minOf(l1, l2)
  return (hi + 0.05) / (lo + 0.05)
}

fun relativeLuminance(color: Color): Double {
  fun lin(c: Float): Double {
    val v = c.toDouble()
    return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
  }
  return 0.2126 * lin(color.red) + 0.7152 * lin(color.green) + 0.0722 * lin(color.blue)
}

private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
private fun JsonObject.float(key: String): Float? = this[key]?.jsonPrimitive?.floatOrNull
