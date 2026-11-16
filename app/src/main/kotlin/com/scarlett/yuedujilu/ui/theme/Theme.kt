package com.scarlett.yuedujilu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.scarlett.yuedujilu.domain.ThemeId

val LocalPalette = staticCompositionLocalOf { Palettes.MistPlum }
val LocalThemeCatalog = staticCompositionLocalOf { ThemeCatalog.builtin() }
val LocalRadii = staticCompositionLocalOf { RadiiTokens() }
val LocalSpaces = staticCompositionLocalOf { SpaceTokens() }

@Composable
fun YueDuTheme(
  themeId: ThemeId = ThemeId.MIST_PLUM,
  catalog: ThemeCatalog = ThemeCatalog.builtin(),
  content: @Composable () -> Unit,
) {
  val palette = catalog.palette(themeId)
  val context = LocalContext.current
  val fontScale = LocalConfiguration.current.fontScale
  val motion = remember(fontScale, context) {
    MotionPrefs(
      reduceMotion = readReduceMotion(context),
      largeText = fontScale >= 1.3f,
    )
  }
  val scheme = lightColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    background = palette.background,
    surface = palette.surface,
    onBackground = palette.text,
    onSurface = palette.text,
    secondary = palette.primary,
    onSecondary = palette.onPrimary,
    surfaceVariant = palette.surfaceMuted,
    outline = palette.stroke,
    error = palette.danger,
    scrim = Color(0x661C1216),
  )
  CompositionLocalProvider(
    LocalPalette provides palette,
    LocalThemeCatalog provides catalog,
    LocalRadii provides catalog.radii,
    LocalSpaces provides catalog.spaces,
    LocalMotion provides motion,
    LocalGlassTreatment provides remember(context) { resolveGlassTreatment(context) },
  ) {
    MaterialTheme(colorScheme = scheme, typography = YueDuTypography, content = content)
  }
}
