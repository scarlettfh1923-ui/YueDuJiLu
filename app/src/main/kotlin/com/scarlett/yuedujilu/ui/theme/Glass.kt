package com.scarlett.yuedujilu.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * 玻璃材质的实际能力（不是物理折射，也未在真机上验证观感）：
 *
 * - [GlassTreatment.BLUR]：API 31+ 且系统允许时，用 Haze 1.7.2（Apache 2.0）
 *   对已登记的 Compose 内容做采样模糊（RenderEffect），再叠一层浅色染。
 *   这是背景采样，不是 [Modifier.blur]（后者会把自身文字图标糊掉，这里不用）。
 *   细亮边是画上去的高光，不是透镜折射。
 * - [GlassTreatment.TINT]：API 30 及以下，或模糊被关闭时，只用预乘半透明合成色 + 细亮边。
 *   不是实时模糊，也不声称是玻璃折射。
 * - [GlassTreatment.SOLID]：高对比、减弱透明、低内存设备上的近实色底板，保证文字对比。
 *
 * 计时条和正文统计卡保持实色，不整页透明。弹层若在独立窗口，无法采样主界面背景，退回合成色。
 */
enum class GlassTreatment { BLUR, TINT, SOLID }

val LocalGlassTreatment = staticCompositionLocalOf { GlassTreatment.TINT }
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

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
  if (!HazeDefaults.blurEnabled()) return GlassTreatment.TINT
  return GlassTreatment.BLUR
}

@Composable
fun Modifier.glassSurface(shape: Shape, solid: Boolean? = null): Modifier {
  val p = LocalPalette.current
  val treatment = when {
    solid == true -> GlassTreatment.SOLID
    else -> LocalGlassTreatment.current
  }
  val hazeState = LocalHazeState.current
  val edge = if (p.isLight) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.16f)
  val base = this
    .shadow(6.dp, shape, ambientColor = p.shadow, spotColor = p.shadow)
    .clip(shape)
  return when {
    treatment == GlassTreatment.BLUR && hazeState != null -> base
      .hazeEffect(
        state = hazeState,
        style = HazeStyle(
          backgroundColor = p.tabBar,
          tints = listOf(HazeTint(p.tabBar.copy(alpha = 0.58f))),
          blurRadius = 24.dp,
          noiseFactor = 0.05f,
        ),
      )
      .border(1.dp, edge, shape)
    treatment == GlassTreatment.SOLID -> base
      .border(1.dp, edge, shape)
      .background(p.surfaceRaised)
    else -> base
      .border(1.dp, edge, shape)
      .background(p.tabBar.copy(alpha = p.tabBarAlpha))
  }
}
