package com.scarlett.yuedujilu.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Android 约定优先：保留系统 ripple；按下立刻缩放反馈。
 * 系统「移除动画」或字体放大到 1.3 及以上时减弱动效（不再缩放，只保留 ripple）。
 */
data class MotionPrefs(
  val reduceMotion: Boolean,
  val largeText: Boolean,
) {
  val weakenMotion: Boolean get() = reduceMotion || largeText
}

val LocalMotion = staticCompositionLocalOf { MotionPrefs(reduceMotion = false, largeText = false) }

fun readReduceMotion(context: android.content.Context): Boolean {
  fun scale(name: String): Float =
    runCatching { Settings.Global.getFloat(context.contentResolver, name, 1f) }.getOrDefault(1f)
  return scale(Settings.Global.ANIMATOR_DURATION_SCALE) == 0f ||
    scale(Settings.Global.TRANSITION_ANIMATION_SCALE) == 0f
}

@Composable
fun Modifier.pressClickable(
  enabled: Boolean = true,
  onClick: () -> Unit,
): Modifier {
  val motion = LocalMotion.current
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (pressed && enabled && !motion.weakenMotion) 0.97f else 1f,
    animationSpec = if (motion.weakenMotion) snap() else tween(durationMillis = 100),
    label = "pressScale",
  )
  return this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .clickable(
      enabled = enabled,
      interactionSource = interaction,
      indication = ripple(bounded = true),
      onClick = onClick,
    )
}
