package com.scarlett.yuedujilu.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** v7 opaque porcelain. Glass stays in navigation; highlights follow the same clipped shape.
 * A short contact shadow, not a blurred duplicate of the content. Cached outline/brush per size.
 */
enum class PorcelainLevel { CONTENT, CONTROL, SUBTLE }
fun Modifier.porcelainSurface(shape:Shape,fill:Color,ink:Color,level:PorcelainLevel=PorcelainLevel.CONTENT):Modifier {
 val elevation=when(level){PorcelainLevel.CONTENT->4.dp;PorcelainLevel.CONTROL->5.dp;PorcelainLevel.SUBTLE->2.dp}
 val opacity=when(level){PorcelainLevel.CONTENT->.20f;PorcelainLevel.CONTROL->.28f;PorcelainLevel.SUBTLE->.15f}
 return shadow(elevation,shape,clip=false,ambientColor=ink.copy(alpha=opacity),spotColor=ink.copy(alpha=opacity))
  .clip(shape).background(fill).drawWithCache {
   val outline=shape.createOutline(size,layoutDirection,this)
   val light=Brush.linearGradient(listOf(Color.White.copy(alpha=.8f),Color.White.copy(alpha=.12f),Color.Transparent),Offset.Zero,Offset(size.width,size.height))
   val edge=Stroke(1.dp.toPx())
   onDrawWithContent {drawContent();drawOutline(outline,light,style=edge)}
  }
}
val YueDuPalette.groupBase:Color get()=if(id==com.scarlett.yuedujilu.domain.ThemeId.PINE_GREEN)Color(0xFFD5E2D8) else lerp(background,primarySoft,.65f)
val YueDuPalette.toolBase:Color get()=if(id==com.scarlett.yuedujilu.domain.ThemeId.PINE_GREEN)Color(0xFFEDF4EE) else lerp(background,surface,.55f)
