package com.scarlett.yuedujilu.ui

/**
 * 底栏布局约定。三项始终等宽等高、图标在上文字在下、选中浅底铺满整格。
 * 不使用会隐藏未选项文字的 NavigationSuite 自适应模式。
 */
object TabBarSpec {
  const val ITEM_COUNT = 3
  const val ALWAYS_SHOW_LABEL = true
  const val ICON_ABOVE_LABEL = true
  const val SELECTED_FILLS_CELL = true
  const val OUTER_RADIUS_DP = 38
  const val INNER_PADDING_DP = 6
  const val SELECTED_RADIUS_DP = 32
  const val MIN_TOUCH_DP = 48
  const val LABEL_SP = 12
  const val SIDE_MARGIN_DP = 24
  const val BOTTOM_GAP_DP = 12

  fun outerRadius(heightDp: Float): Float = heightDp / 2f
  fun innerRadius(heightDp: Float): Float = outerRadius(heightDp) - INNER_PADDING_DP

  fun itemWeight(): Float = 1f / ITEM_COUNT

  fun isHorizontallySymmetric(startPaddingDp: Int, endPaddingDp: Int): Boolean =
    startPaddingDp == endPaddingDp

  fun selectedFillCoversCell(fillWidth: Float, cellWidth: Float, epsilon: Float = 0.5f): Boolean =
    cellWidth > 0f && kotlin.math.abs(fillWidth - cellWidth) <= epsilon

  fun labelsAlwaysVisible(selectedShowsLabel: Boolean, unselectedShowsLabel: Boolean): Boolean =
    ALWAYS_SHOW_LABEL && selectedShowsLabel && unselectedShowsLabel
}
