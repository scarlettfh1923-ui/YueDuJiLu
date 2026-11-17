package com.scarlett.yuedujilu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.scarlett.yuedujilu.domain.ThemeId

val LocalPalette = staticCompositionLocalOf { Palettes.PineGreen }
val LocalThemeCatalog = staticCompositionLocalOf { ThemeCatalog.builtin() }
val LocalRadii = staticCompositionLocalOf { RadiiTokens() }
val LocalSpaces = staticCompositionLocalOf { SpaceTokens() }

@Composable
fun YueDuTheme(
  themeId: ThemeId = ThemeId.PINE_GREEN,
  catalog: ThemeCatalog = ThemeCatalog.builtin(),
  content: @Composable () -> Unit,
) {
  val palette = catalog.palette(themeId)
  val context = LocalContext.current
  val fontScale = LocalConfiguration.current.fontScale
  var accessibilityRevision by remember { mutableIntStateOf(0) }
  DisposableEffect(context) {
    val observer=object:android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
      override fun onChange(selfChange:Boolean) { accessibilityRevision++ }
    }
    listOf(android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
      android.provider.Settings.Global.WINDOW_ANIMATION_SCALE).forEach { context.contentResolver.registerContentObserver(android.provider.Settings.Global.getUriFor(it),false,observer) }
    listOf("high_text_contrast_enabled","accessibility_reduce_transparency","reduce_transparency").forEach {
      context.contentResolver.registerContentObserver(android.provider.Settings.Secure.getUriFor(it),false,observer)
      context.contentResolver.registerContentObserver(android.provider.Settings.Global.getUriFor(it),false,observer)
    }
    onDispose { context.contentResolver.unregisterContentObserver(observer) }
  }
  val motion = remember(fontScale, context, accessibilityRevision) {
    MotionPrefs(
      reduceMotion = readReduceMotion(context),
      largeText = fontScale >= 1.3f,
    )
  }
  val scheme = lightColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primarySoft,
    onPrimaryContainer = palette.text,
    secondaryContainer = palette.primarySoft,
    onSecondaryContainer = palette.text,
    tertiary = palette.primary,
    onTertiary = palette.onPrimary,
    tertiaryContainer = palette.primarySoft,
    onTertiaryContainer = palette.text,
    onSurfaceVariant = palette.textSecondary,
    surfaceContainerLowest = palette.surfaceRaised,
    surfaceContainerLow = palette.surface,
    surfaceContainer = palette.surface,
    surfaceContainerHigh = palette.surfaceMuted,
    surfaceContainerHighest = palette.surfaceMuted,
    surfaceDim = palette.surfaceMuted,
    surfaceBright = palette.surfaceRaised,
    outlineVariant = palette.stroke,
    inverseSurface = palette.text,
    inverseOnSurface = palette.surface,
    inversePrimary = palette.primarySoft,
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
    LocalRadii provides RadiiTokens(),
    LocalSpaces provides catalog.spaces,
    LocalMotion provides motion,
    LocalGlassTreatment provides remember(context, accessibilityRevision) { resolveGlassTreatment(context) },
  ) {
    MaterialTheme(colorScheme = scheme, typography = YueDuTypography, shapes = androidx.compose.material3.Shapes(
      extraSmall=UiShape.Regular, small=UiShape.Regular,
      medium=UiShape.Regular, large=UiShape.Regular, extraLarge=UiShape.Regular), content = content)
  }
}
