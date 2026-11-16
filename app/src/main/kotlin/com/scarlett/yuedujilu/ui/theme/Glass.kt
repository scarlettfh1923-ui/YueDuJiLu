package com.scarlett.yuedujilu.ui.theme

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 底栏玻璃：细亮边 + 微影 + 半透明合成色。
 * 当前 Compose/SDK 没有可依赖的应用内实时背景模糊公开 API，因此不会把预乘半透明冒充为实时折射。
 * 高对比或系统减弱透明相关设置时回退为实色 surfaceRaised。
 */
fun shouldUseSolidGlass(context: Context): Boolean {
  val highContrast = Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
  val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
  val highText = runCatching {
    val m = AccessibilityManager::class.java.getMethod("isHighTextContrastEnabled")
    m.invoke(am) as? Boolean ?: false
  }.getOrDefault(false)
  return highContrast || highText
}

@Composable
fun Modifier.glassSurface(shape: Shape, solid: Boolean): Modifier {
  val p = LocalPalette.current
  val fill = if (solid) p.surfaceRaised else p.tabBar.copy(alpha = p.tabBarAlpha)
  val edge = if (p.isLight) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.16f)
  return this
    .shadow(8.dp, shape, ambientColor = p.shadow, spotColor = p.shadow)
    .clip(shape)
    .border(1.dp, edge, shape)
    .background(fill)
}
