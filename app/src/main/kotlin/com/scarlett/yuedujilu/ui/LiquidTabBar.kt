package com.scarlett.yuedujilu.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.MainTab
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.theme.*

/** One continuous, interruptible indicator. The same 50% shape clips all glass/ripple edges.
 * Outer H/2 and inset inner (H-12)/2 share their Y centre; no arbitrary corner constants.
 */
@Composable
fun LiquidTabBar(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  val motion = LocalMotion.current
  val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
  val outerHeight = (76f + 36f * (scale - 1f)).dp
  val shape = UiShape.Capsule
  val animatedPosition by animateFloatAsState(selected.ordinal.toFloat(),
    animationSpec = if (motion.weakenMotion) snap() else spring(dampingRatio=1f, stiffness=600f), label="nav-indicator")
  val position=if(motion.weakenMotion) selected.ordinal.toFloat() else animatedPosition
  BoxWithConstraints(modifier.fillMaxWidth().height(outerHeight).glassSurface(shape).testTag("liquid-nav")) {
    val inset=6.dp
    val cellWidth=(maxWidth-inset*2)/3
    Box(Modifier.padding(inset).offset(x=cellWidth*position).width(cellWidth).fillMaxHeight()
      .clip(shape).background(p.text.copy(alpha=0.075f))
      .background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.30f),Color.White.copy(alpha=.04f))))
      .border(.7f.dp,Color.White.copy(alpha=.65f),shape).testTag("nav-indicator"))
    Row(Modifier.fillMaxSize().padding(inset)) {
      val icons=listOf(AppIcons.Shelf,AppIcons.Stats,AppIcons.Mine)
      val labels=listOf("书架","统计","我的")
      MainTab.entries.forEachIndexed { index, tab ->
        val active=selected==tab
        val ink by animateColorAsState(p.text,
          animationSpec=if(motion.weakenMotion) snap() else androidx.compose.animation.core.tween(140),label="nav-ink")
        Column(Modifier.weight(1f).fillMaxHeight().clip(shape)
          .selectable(active, role=Role.Tab, interactionSource=remember { MutableInteractionSource() },
            indication=ripple(), onClick={onSelect(tab)}).testTag("tab-${tab.name}"),
          horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
          Icon(icons[index],null,tint=ink,modifier=Modifier.size(25.dp))
          Spacer(Modifier.height(3.dp))
          Text(labels[index],color=ink,fontSize=12.sp,lineHeight=16.sp,
            fontWeight=if(active)FontWeight.SemiBold else FontWeight.Medium,maxLines=1)
        }
      }
    }
  }
}
