package com.scarlett.yuedujilu.ui
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.core.app.ApplicationProvider
import com.scarlett.yuedujilu.*
import com.scarlett.yuedujilu.data.*
import com.scarlett.yuedujilu.domain.*
import com.scarlett.yuedujilu.ui.shelf.BookRow
import com.scarlett.yuedujilu.ui.theme.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[35],qualifiers="zh-rCN-w432dp-h936dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class V7LayoutRegressionTest {
 @get:Rule val rule=createComposeRule()
 private fun book()=Book(id="compact-test",title="九诗心",author="黄晓丹",pageCount=500,currentPage=475,status=BookStatus.READING,createdAtEpochMs=1,updatedAtEpochMs=1)
 @Test fun ordinaryBookIsCompactButActionHasFullTouchTarget() {
  var density=1f
  rule.setContent { density=LocalDensity.current.density;YueDuTheme {Box(Modifier.width(432.dp).padding(24.dp)){BookRow(book(),false,{}, {})}} }
  rule.waitForIdle()
  val row=rule.onNodeWithTag("book-compact-test").fetchSemanticsNode().boundsInRoot
  val action=rule.onNode(hasText("继续阅读") and hasClickAction()).fetchSemanticsNode().boundsInRoot
  assertTrue("Compact ordinary row: ${row.height/density}dp",row.height/density in 172f..180f)
  assertTrue(action.height>=48*density-.5f);assertTrue(action.width>=48*density-.5f)
 }
 @Test fun narrowLargeTypeGrowsInsteadOfShrinkingTouchOrClipping() {
  var density=1f
  rule.setContent {density=LocalDensity.current.density;CompositionLocalProvider(LocalDensity provides Density(density,1.5f)) {YueDuTheme {Box(Modifier.width(360.dp).padding(24.dp)){BookRow(book().copy(title="一本有着非常长非常长的书名与副标题的书",author="一位名字也非常长的作者与译者"),false,{}, {})}}}}
  rule.waitForIdle();val row=rule.onNodeWithTag("book-compact-test").fetchSemanticsNode().boundsInRoot
  val action=rule.onNode(hasText("继续阅读") and hasClickAction()).fetchSemanticsNode().boundsInRoot
  assertTrue(row.height>172*density);assertTrue(action.height>=48*density-.5f);assertTrue(action.bottom<=row.bottom)
 }
 @Test fun groupedSecondaryActionsRemainAlignedAndFinishingKeeps475Of500() {
  val app=ApplicationProvider.getApplicationContext<Application>();val repo=LibraryRepository(JsonLibraryStore(File(app.cacheDir,"v7-actions-${System.nanoTime()}.json")))
  kotlinx.coroutines.runBlocking {repo.addBook(book())};val vm=AppViewModel(app,repo,MemorySecretStore(),ShowApiClient(),WereadClient());vm.go(Route.BookDetail(book().id))
  rule.setContent {val state by vm.ui.collectAsState();val density=LocalDensity.current.density;CompositionLocalProvider(LocalDensity provides Density(density,1.5f)){YueDuTheme {Box(Modifier.width(360.dp)){AppRoot(vm,state)}}}}
  rule.waitForIdle()
  val bounds=listOf("补录记录","标记读完","移入弃读").map {rule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot}
  assertTrue(bounds.maxOf{it.top}-bounds.minOf{it.top}<.5f);assertTrue(bounds.maxOf{it.height}-bounds.minOf{it.height}<.5f)
  rule.onNodeWithText("标记读完").performClick();rule.waitForIdle()
  val saved=vm.ui.value.snapshot.books.single();assertEquals(475,saved.currentPage);assertEquals(500,saved.pageCount);assertEquals(BookStatus.FINISHED,saved.status)
  rule.onNodeWithText("修改完读日期").assertExists();rule.onNodeWithText("撤销读完").performClick();rule.waitForIdle();assertEquals(475,vm.ui.value.snapshot.books.single().currentPage)
 }
 @Test fun narrowLargeCompactHoursKeepMagnitudeSuffixWithoutEllipsis() {
  var result:androidx.compose.ui.text.TextLayoutResult?=null
  rule.setContent {val density=LocalDensity.current.density;CompositionLocalProvider(LocalDensity provides Density(density,1.5f)){YueDuTheme {
   com.scarlett.yuedujilu.ui.stats.MetricNumber("24.7万","小时",modifier=Modifier.width(114.dp),onNumberLayout={result=it})
  }}}
  rule.waitForIdle();val r=checkNotNull(result);assertFalse("Magnitude suffix must not be ellipsized",r.isLineEllipsized(0));assertFalse(r.hasVisualOverflow)
 }

}
