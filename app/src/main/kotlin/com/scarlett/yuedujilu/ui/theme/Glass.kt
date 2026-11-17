package com.scarlett.yuedujilu.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Background sampling uses Backdrop 1.0.4 (Apache-2.0). API33+ adds an AGSL edge lens.
 * This approximates liquid glass, not Apple's proprietary optical/gesture implementation.
 * Text is drawn after the backdrop and is never blurred. API31/32 blur only; older/high-contrast solid fallback.
 */
enum class GlassTreatment { BLUR, TINT, SOLID }

val LocalGlassTreatment = staticCompositionLocalOf { GlassTreatment.TINT }
val LocalBackdrop = staticCompositionLocalOf<com.kyant.backdrop.Backdrop?> { null }

fun shouldUseSolidGlass(context: Context): Boolean {
  val highContrast = Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
  val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
  val highText = runCatching {
    val m = AccessibilityManager::class.java.getMethod("isHighTextContrastEnabled")
    m.invoke(am) as? Boolean ?: false
  }.getOrDefault(false)
  return highContrast || highText || reduceTransparency(context) || isLowRam(context)
}

fun reduceTransparency(context: Context): Boolean {
  val names = listOf("accessibility_reduce_transparency", "reduce_transparency")
  return names.any { name ->
    runCatching { Settings.Secure.getInt(context.contentResolver, name, 0) }.getOrDefault(0) == 1 ||
      runCatching { Settings.Global.getInt(context.contentResolver, name, 0) }.getOrDefault(0) == 1
  }
}

fun isLowRam(context: Context): Boolean {
  val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
  return am?.isLowRamDevice == true
}

fun resolveGlassTreatment(context: Context): GlassTreatment {
  if (shouldUseSolidGlass(context)) return GlassTreatment.SOLID
  if (Build.VERSION.SDK_INT < 31) return GlassTreatment.TINT
  return GlassTreatment.BLUR
}

@Composable
fun Modifier.glassSurface(shape: Shape, solid: Boolean? = null): Modifier {
  val p = LocalPalette.current
  val backdrop = LocalBackdrop.current
  val treatment = LocalGlassTreatment.current
  val enabled = solid != true && treatment == GlassTreatment.BLUR && backdrop != null
  return if (enabled) {
    this.drawBackdrop(
      backdrop = backdrop!!,
      shape = { shape },
      effects = {
        vibrancy()
        blur(12.dp.toPx())
        lens(10.dp.toPx(), 16.dp.toPx(), depthEffect = true)
      },
      highlight = { com.kyant.backdrop.highlight.Highlight(width = 1.dp, alpha = 0.75f) },
      shadow = { com.kyant.backdrop.shadow.Shadow(radius = 10.dp, color = p.text.copy(alpha = 0.10f)) },
      innerShadow = { com.kyant.backdrop.shadow.InnerShadow(radius = 2.dp, offset = androidx.compose.ui.unit.DpOffset(0.dp, 1.dp), color = p.text.copy(alpha = 0.08f)) },
      onDrawSurface = { drawRect(p.surfaceRaised.copy(alpha = 0.46f)) },
    ).clip(shape)
  } else {
    this.shadow(6.dp, shape, ambientColor = p.shadow, spotColor = p.shadow)
      .clip(shape).background(p.surfaceRaised)
      .border(1.dp, p.stroke.copy(alpha = 0.65f), shape)
  }
}
