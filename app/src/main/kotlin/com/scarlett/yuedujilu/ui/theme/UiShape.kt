package com.scarlett.yuedujilu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** One curve language. All regular cards/fields/buttons use22dp.
 * Capsules use50% of actual measured height, so accessibility growth keeps round ends.
 * Circular swatches/markers are squares using the same half-height geometry.
 * App launcher masks and platform camera/document/date-picker surfaces belong to the OS.
 */
object UiShape {
  val Regular=RoundedCornerShape(22.dp)
  val Capsule=RoundedCornerShape(50)
  val Sheet=RoundedCornerShape(topStart=22.dp,topEnd=22.dp)
}
