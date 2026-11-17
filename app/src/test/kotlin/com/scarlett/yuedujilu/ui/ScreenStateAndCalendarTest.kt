package com.scarlett.yuedujilu.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.scarlett.yuedujilu.*
import com.scarlett.yuedujilu.data.*
import com.scarlett.yuedujilu.domain.*
import com.scarlett.yuedujilu.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[35],qualifiers="zh-rCN-w432dp-h936dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenStateAndCalendarTest {
 @get:Rule val rule=createComposeRule()
 private fun vm():AppViewModel {
   val app=ApplicationProvider.getApplicationContext<Application>()
   return AppViewModel(app,LibraryRepository(JsonLibraryStore(File(app.cacheDir,"state-fixture-${System.nanoTime()}.json"))),MemorySecretStore(),ShowApiClient(),WereadClient())
 }
 @Test fun tabContentNeverKeepsOutgoingPageDuringIndicatorAnimation() {
   val vm=vm()
   var state by mutableStateOf(AppUiState())
   rule.setContent { YueDuTheme { AppRoot(vm,state) } }
   rule.waitForIdle();rule.mainClock.autoAdvance=false
   val pageLabels=mapOf(MainTab.SHELF to "搜索书名、作者或 ISBN",MainTab.STATS to "概览",MainTab.MINE to "阅读连接")
   for(tab in listOf(MainTab.MINE,MainTab.SHELF,MainTab.STATS,MainTab.MINE,MainTab.SHELF)) {
     rule.runOnIdle { state=state.copy(tab=tab) }
     for(delta in listOf(16L,16L,32L,76L)) {
       rule.mainClock.advanceTimeBy(delta);rule.waitForIdle()
       // Before the state commits a frame may still show the old page; never both.
       val pageCount=pageLabels.values.sumOf { label ->
         rule.onAllNodesWithText(label,useUnmergedTree=true).fetchSemanticsNodes().size
       }
       Assert.assertEquals("Exactly one page at every frame",1,pageCount)
     }
     for((page,label) in pageLabels) {
       rule.onAllNodesWithText(label,useUnmergedTree=true).assertCountEquals(if(page==tab) 1 else 0)
     }
   }
 }
 @Test fun tabAndRouteChangesPreserveShelfScroll(): Unit = runBlocking {
   val vm=vm();val now=System.currentTimeMillis()
   repeat(20) { vm.library.addBook(vm.library.newBook("测试书目${it.toString().padStart(2,'0')}",now=now+it).copy(id="b$it")) }
   var state by mutableStateOf(AppUiState(snapshot=vm.library.state.value,now=now))
   rule.setContent { YueDuTheme { AppRoot(vm,state) } }
   rule.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
   val visible=vm.visibleBooks(state)[10].title
   rule.onNodeWithText(visible).assertIsDisplayed()
   rule.runOnIdle { state=state.copy(tab=MainTab.STATS) };rule.waitForIdle()
   rule.runOnIdle { state=state.copy(tab=MainTab.SHELF) };rule.waitForIdle()
   rule.onNodeWithText(visible).assertIsDisplayed()
   rule.runOnIdle { state=state.copy(route=Route.BookDetail("b10")) };rule.waitForIdle()
   rule.runOnIdle { state=state.copy(route=Route.Main) };rule.waitForIdle()
   rule.onNodeWithText(visible).assertIsDisplayed()
 }
 @Test fun calendarSelectedUnknownAndFutureNeverMasqueradeAsUnread() {
   val vm=vm();val today=LocalDate.of(2026,11,17)
   val now=today.atTime(12,0).atZone(Zones.Shanghai).toInstant().toEpochMilli()
   val state=AppUiState(now=now,tab=MainTab.STATS,statsMode=StatsMode.CALENDAR,calendarMonth=YearMonth.from(today),statsAnchor=today)
   rule.setContent { YueDuTheme { AppRoot(vm,state) } }
   rule.onNodeWithContentDescription("2026-11-14 数据未知").performClick()
   rule.onNodeWithText("11 月 14 日 · 数据未知").assertExists()
   rule.onNodeWithContentDescription("2026-11-18 未来").performClick()
   rule.onNodeWithText("未来日期，不判定是否阅读。").assertExists()
   rule.onNodeWithText("确认当天未读实体书").assertDoesNotExist()
 }
}
