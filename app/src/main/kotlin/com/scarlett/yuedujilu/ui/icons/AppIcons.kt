package com.scarlett.yuedujilu.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 统一图标系统：24dp 视口、2dp 描边、圆线帽、圆转角、光学留白约 3dp。
 * 底栏三项提供描边/填充两种，底栏选中只变色，始终使用同一描边路径，避免混用其他图标库或 emoji。
 */
object AppIcons {
  val Shelf = stroke("Shelf") {
    moveTo(5.5f, 19f); lineTo(5.5f, 8.5f); lineTo(9.5f, 8.5f); lineTo(9.5f, 19f)
    moveTo(10.5f, 19f); lineTo(10.5f, 5.5f); lineTo(14.5f, 5.5f); lineTo(14.5f, 19f)
    moveTo(15.5f, 19f); lineTo(15.5f, 9.5f); lineTo(19.5f, 9.5f); lineTo(19.5f, 19f)
    moveTo(4.5f, 19f); lineTo(19.5f, 19f)
  }
  val ShelfFilled = filled("ShelfFilled") {
    moveTo(5.25f, 19f); lineTo(5.25f, 8.25f); lineTo(9.25f, 8.25f); lineTo(9.25f, 19f); close()
    moveTo(10.25f, 19f); lineTo(10.25f, 5.25f); lineTo(14.25f, 5.25f); lineTo(14.25f, 19f); close()
    moveTo(15.25f, 19f); lineTo(15.25f, 9.25f); lineTo(19.25f, 9.25f); lineTo(19.25f, 19f); close()
  }
  val Stats = stroke("Stats") {
    moveTo(6.5f, 18.5f); lineTo(6.5f, 12f)
    moveTo(12f, 18.5f); lineTo(12f, 6.5f)
    moveTo(17.5f, 18.5f); lineTo(17.5f, 10f)
    moveTo(4.5f, 18.5f); lineTo(19.5f, 18.5f)
  }
  val StatsFilled = filled("StatsFilled") {
    moveTo(5.4f, 18.6f); lineTo(7.6f, 18.6f); lineTo(7.6f, 11.4f); lineTo(5.4f, 11.4f); close()
    moveTo(10.9f, 18.6f); lineTo(13.1f, 18.6f); lineTo(13.1f, 5.9f); lineTo(10.9f, 5.9f); close()
    moveTo(16.4f, 18.6f); lineTo(18.6f, 18.6f); lineTo(18.6f, 9.4f); lineTo(16.4f, 9.4f); close()
  }
  val Mine = stroke("Mine") {
    circle(12f, 8.2f, 2.7f)
    moveTo(6.4f, 18.5f)
    arcTo(6.2f, 5.2f, 0f, false, true, 17.6f, 18.5f)
  }
  val MineFilled = ImageVector.Builder("MineFilled", 24.dp, 24.dp, 24f, 24f)
    .path(
      fill = SolidColor(Color.Black),
      stroke = null,
    ) {
      moveTo(12f, 11.2f)
      arcToRelative(3.2f, 3.2f, 0f, true, false, 0f, -6.4f)
      arcToRelative(3.2f, 3.2f, 0f, false, false, 0f, 6.4f)
      close()
      moveTo(6.1f, 19.2f)
      arcToRelative(5.9f, 5.2f, 0f, false, true, 11.8f, 0f)
      close()
    }.build()

  val Search = stroke("Search") {
    circle(11f, 11f, 5.25f)
    moveTo(15.1f, 15.1f); lineTo(19f, 19f)
  }
  val Plus = stroke("Plus") {
    moveTo(12f, 6.5f); lineTo(12f, 17.5f)
    moveTo(6.5f, 12f); lineTo(17.5f, 12f)
  }
  val Close = stroke("Close") {
    moveTo(7f, 7f); lineTo(17f, 17f)
    moveTo(17f, 7f); lineTo(7f, 17f)
  }
  val Check = stroke("Check") {
    moveTo(6f, 12.2f); lineTo(10.2f, 16.5f); lineTo(18.2f, 7.5f)
  }
  val ChevronRight = stroke("ChevronRight") {
    moveTo(9.5f, 6.5f); lineTo(16f, 12f); lineTo(9.5f, 17.5f)
  }
  val ChevronLeft = stroke("ChevronLeft") {
    moveTo(14.5f, 6.5f); lineTo(8f, 12f); lineTo(14.5f, 17.5f)
  }
  val ChevronDown = stroke("ChevronDown") {
    moveTo(6.5f, 9.5f); lineTo(12f, 15f); lineTo(17.5f, 9.5f)
  }
  val Pause = stroke("Pause") {
    moveTo(9f, 7f); lineTo(9f, 17f)
    moveTo(15f, 7f); lineTo(15f, 17f)
  }
  val Play = stroke("Play") {
    moveTo(9f, 7.5f)
    lineTo(17.2f, 12f)
    lineTo(9f, 16.5f)
    close()
  }
  val Clock = stroke("Clock") {
    circle(12f, 12f, 7f)
    moveTo(12f, 8.2f); lineTo(12f, 12.2f); lineTo(15.2f, 14f)
  }
  val Barcode = stroke("Barcode") {
    moveTo(5.5f, 6.5f); lineTo(5.5f, 17.5f)
    moveTo(8.5f, 6.5f); lineTo(8.5f, 17.5f)
    moveTo(11f, 6.5f); lineTo(11f, 17.5f)
    moveTo(14.5f, 6.5f); lineTo(14.5f, 17.5f)
    moveTo(16.5f, 6.5f); lineTo(16.5f, 17.5f)
    moveTo(18.5f, 6.5f); lineTo(18.5f, 17.5f)
  }
  val Keyboard = stroke("Keyboard") {
    moveTo(5.5f, 7.5f)
    lineTo(18.5f, 7.5f)
    lineTo(18.5f, 16.5f)
    lineTo(5.5f, 16.5f)
    close()
    moveTo(8f, 10.5f); lineTo(8.01f, 10.5f)
    moveTo(12f, 10.5f); lineTo(12.01f, 10.5f)
    moveTo(16f, 10.5f); lineTo(16.01f, 10.5f)
    moveTo(8.5f, 13.5f); lineTo(15.5f, 13.5f)
  }
  val Edit = stroke("Edit") {
    moveTo(15.2f, 6.3f)
    lineTo(17.7f, 8.8f)
    lineTo(9.4f, 17.1f)
    lineTo(6.5f, 17.5f)
    lineTo(6.9f, 14.6f)
    close()
  }
  val Calendar = stroke("Calendar") {
    moveTo(6.5f, 7.5f)
    lineTo(17.5f, 7.5f)
    lineTo(17.5f, 18.5f)
    lineTo(6.5f, 18.5f)
    close()
    moveTo(6.5f, 11f); lineTo(17.5f, 11f)
    moveTo(9.5f, 5.5f); lineTo(9.5f, 8.5f)
    moveTo(14.5f, 5.5f); lineTo(14.5f, 8.5f)
  }
  val Sync = stroke("Sync") {
    moveTo(7.4f, 9.4f)
    arcTo(5.6f, 5.6f, 0f, false, true, 17.6f, 11.2f)
    moveTo(17.6f, 8.2f); lineTo(17.6f, 11.6f); lineTo(14.4f, 11.6f)
    moveTo(16.6f, 14.6f)
    arcTo(5.6f, 5.6f, 0f, false, true, 6.4f, 12.8f)
    moveTo(6.4f, 15.8f); lineTo(6.4f, 12.4f); lineTo(9.6f, 12.4f)
  }
  val Spark = stroke("Spark") {
    moveTo(12f, 5f)
    lineTo(13.3f, 10.7f)
    lineTo(19f, 12f)
    lineTo(13.3f, 13.3f)
    lineTo(12f, 19f)
    lineTo(10.7f, 13.3f)
    lineTo(5f, 12f)
    lineTo(10.7f, 10.7f)
    close()
  }
  val Backup = stroke("Backup") {
    moveTo(8.2f, 16.5f)
    lineTo(6.5f, 16.5f)
    lineTo(6.5f, 10f)
    arcTo(5.5f, 5.5f, 0f, false, true, 17.5f, 10.4f)
    lineTo(17.5f, 16.5f)
    lineTo(15.8f, 16.5f)
    moveTo(12f, 12f); lineTo(12f, 19.2f)
    moveTo(9.3f, 14.6f); lineTo(12f, 12f); lineTo(14.7f, 14.6f)
  }
  val Export = stroke("Export") {
    moveTo(6.5f, 18.5f); lineTo(17.5f, 18.5f)
    moveTo(12f, 5.5f); lineTo(12f, 15f)
    moveTo(8.4f, 9.1f); lineTo(12f, 5.5f); lineTo(15.6f, 9.1f)
  }
  val Link = stroke("Link") {
    moveTo(10f, 14.2f); lineTo(8.6f, 14.2f)
    arcToRelative(3.2f, 3.2f, 0f, true, true, 0f, -6.4f)
    lineTo(12.2f, 7.8f)
    moveTo(14f, 9.8f); lineTo(15.4f, 9.8f)
    arcToRelative(3.2f, 3.2f, 0f, true, true, 0f, 6.4f)
    lineTo(11.8f, 16.2f)
  }
  val Key = stroke("Key") {
    circle(8.5f, 12f, 3.2f)
    moveTo(11.6f, 12f); lineTo(19f, 12f); lineTo(19f, 14.8f)
    moveTo(16.4f, 12f); lineTo(16.4f, 14.4f)
  }
  val Info = stroke("Info") {
    circle(12f, 12f, 7f)
    moveTo(12f, 11f); lineTo(12f, 16.4f)
    moveTo(12f, 8f); lineTo(12.01f, 8f)
  }
  val Manual = stroke("Manual") {
    moveTo(7f, 5.5f); lineTo(14.2f, 5.5f); lineTo(17.5f, 8.8f); lineTo(17.5f, 18.5f); lineTo(7f, 18.5f); close()
    moveTo(14.2f, 5.5f); lineTo(14.2f, 8.8f); lineTo(17.5f, 8.8f)
    moveTo(9.5f, 12f); lineTo(14.5f, 12f)
    moveTo(9.5f, 15.2f); lineTo(13.2f, 15.2f)
  }
}

private fun stroke(name: String, block: PathBuilder.() -> Unit): ImageVector =
  ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
    fill = SolidColor(Color.Transparent),
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
  ) { block() }.build()

private fun filled(name: String, block: PathBuilder.() -> Unit): ImageVector =
  ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
    fill = SolidColor(Color.Black),
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 0.6f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
  ) { block() }.build()

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
  moveTo(cx + r, cy)
  arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
  arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
}
