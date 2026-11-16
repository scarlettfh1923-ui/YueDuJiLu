package com.scarlett.yuedujilu.ui.theme

import androidx.compose.ui.graphics.toArgb
import com.scarlett.yuedujilu.domain.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {
  @Test
  fun parsesFileAndOverridesBuiltinColor() {
    val json = """
      {
        "version": 1,
        "radiiDp": {"container": 20, "inner": 14, "chip": 10, "search": 22, "pill": 26},
        "spacesDp": {"xs": 6, "sm": 10, "md": 16, "lg": 22},
        "themes": [
          {
            "id": "MIST_PLUM",
            "displayName": "雾紫",
            "subtitle": "交接稿覆盖",
            "background": "#112233",
            "surface": "#FFFFFF",
            "surfaceMuted": "#EEEEEE",
            "primary": "#AABBCC",
            "onPrimary": "#000000",
            "primarySoft": "#CCDDEE",
            "text": "#010101",
            "textSecondary": "#222222",
            "textTertiary": "#333333",
            "stroke": "#1A000000",
            "danger": "#FF0000",
            "success": "#00FF00",
            "shadow": "#1A000000",
            "isLight": true
          }
        ]
      }
    """.trimIndent()
    val catalog = ThemeCatalog.parse(json, "test")
    assertEquals(20, catalog.radii.container)
    assertEquals(6, catalog.spaces.xs)
    assertEquals("交接稿覆盖", catalog.subtitle(ThemeId.MIST_PLUM))
    assertEquals(0xFF112233.toInt(), catalog.palette(ThemeId.MIST_PLUM).background.toArgb())
    assertEquals(Palettes.PineGreen.primary.toArgb(), catalog.palette(ThemeId.PINE_GREEN).primary.toArgb())
  }

  @Test
  fun parseColorAcceptsRgbAndArgb() {
    assertEquals(0xFF112233.toInt(), parseColor("#112233").toArgb())
    assertEquals(0x80AABBCC.toInt(), parseColor("#80AABBCC").toArgb())
  }

  @Test
  fun defaultAssetJsonParsesAllFourThemes() {
    val stream = javaClass.classLoader?.getResourceAsStream("theme-tokens.json")
    val text = stream?.bufferedReader()?.readText()
      ?: java.io.File("src/main/assets/theme-tokens.json").takeIf { it.exists() }?.readText()
      ?: java.io.File("app/src/main/assets/theme-tokens.json").takeIf { it.exists() }?.readText()
      ?: error("theme-tokens.json not found on the test classpath or as a project file")
    val catalog = ThemeCatalog.parse(text, "assets-file")
    assertEquals("assets-file", catalog.source)
    ThemeId.entries.forEach { id ->
      val pal = catalog.palette(id)
      assertEquals(id, pal.id)
      assertTrue(pal.primary.toArgb() != pal.background.toArgb())
    }
    assertEquals(24, catalog.radii.container)
  }
}
