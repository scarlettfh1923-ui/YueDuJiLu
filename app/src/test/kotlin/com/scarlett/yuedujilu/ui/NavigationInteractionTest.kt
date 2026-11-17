package com.scarlett.yuedujilu.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalDensity
import com.scarlett.yuedujilu.MainTab
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.theme.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[35],qualifiers="zh-rCN-w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationInteractionTest {
  @get:Rule val rule=createComposeRule()
  @Test fun releaseCommitsAndSlideOutCancels() {
    var selected by mutableStateOf(MainTab.SHELF)
    rule.setContent { YueDuTheme { LiquidTabBar(selected,{selected=it}) } }
    rule.onNodeWithTag("tab-STATS").performTouchInput { down(center) }
    rule.runOnIdle { Assert.assertEquals(MainTab.SHELF,selected) }
    rule.onNodeWithTag("tab-STATS").performTouchInput { moveTo(Offset(center.x,-120f));up() }
    rule.runOnIdle { Assert.assertEquals(MainTab.SHELF,selected) }
    rule.onNodeWithTag("tab-STATS").performTouchInput { down(center);advanceEventTime(20);up() }
    rule.runOnIdle { Assert.assertEquals(MainTab.STATS,selected) }
  }
  @Test fun interruptedIndicatorRetargetsAndSettlesWithoutInputLock() {
    var selected by mutableStateOf(MainTab.SHELF)
    rule.setContent { YueDuTheme { LiquidTabBar(selected,{selected=it}) } }
    rule.mainClock.autoAdvance=false
    rule.onNodeWithTag("tab-STATS").performClick();rule.waitForIdle();rule.mainClock.advanceTimeBy(96);rule.waitForIdle()
    val middle=rule.onNodeWithTag("nav-indicator").fetchSemanticsNode().boundsInRoot.left
    rule.onNodeWithTag("tab-MINE").performClick();rule.waitForIdle();rule.mainClock.advanceTimeBy(64);rule.waitForIdle()
    rule.onNodeWithTag("tab-SHELF").performClick();rule.waitForIdle();rule.mainClock.advanceTimeBy(1000);rule.waitForIdle()
    val end=rule.onNodeWithTag("nav-indicator").fetchSemanticsNode().boundsInRoot.left
    println("indicator middle=$middle end=$end selected=$selected")
    Assert.assertTrue("indicator must move before interruption",middle>end+1f)
    Assert.assertEquals(MainTab.SHELF,selected)
    listOf("书架","统计","我的").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
  }
  @Test fun reducedMotionSnapsAndThreeTargetsStayEqualAtLargeFont() {
    var selected by mutableStateOf(MainTab.SHELF)
    rule.setContent {
      val density=LocalDensity.current.density
      CompositionLocalProvider(LocalDensity provides Density(density,1.3f)) {
        YueDuTheme { CompositionLocalProvider(LocalMotion provides MotionPrefs(true,true)) { LiquidTabBar(selected,{selected=it}) } }
      }
    }
    rule.mainClock.autoAdvance=false
    rule.onNodeWithTag("tab-MINE").performClick();rule.waitForIdle();rule.mainClock.advanceTimeByFrame();rule.waitForIdle()
    val widths=MainTab.entries.map { rule.onNodeWithTag("tab-${it.name}").fetchSemanticsNode().boundsInRoot.width }
    Assert.assertTrue(widths.max()-widths.min()<1.1f)
    val bottoms=listOf("书架","统计","我的").map { rule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.bottom }
    Assert.assertTrue(bottoms.max()-bottoms.min()<1.1f)
    val indicator=rule.onNodeWithTag("nav-indicator").fetchSemanticsNode().boundsInRoot
    val target=rule.onNodeWithTag("tab-MINE").fetchSemanticsNode().boundsInRoot
    Assert.assertEquals(target.left,indicator.left,1.1f)
  }
  @Test fun buttonDoesNotSubmitOnPressOrCancel() {
    var count=0
    rule.setContent { YueDuTheme { PrimaryButton("保存配置",{count++}) } }
    rule.onNodeWithText("保存配置").performTouchInput { down(center) }
    rule.runOnIdle { Assert.assertEquals(0,count) }
    rule.onNodeWithText("保存配置").performTouchInput { cancel() }
    rule.runOnIdle { Assert.assertEquals(0,count) }
    rule.onNodeWithText("保存配置").performTouchInput { down(center);advanceEventTime(20);up() }
    rule.runOnIdle { Assert.assertEquals(1,count) }
  }
}
