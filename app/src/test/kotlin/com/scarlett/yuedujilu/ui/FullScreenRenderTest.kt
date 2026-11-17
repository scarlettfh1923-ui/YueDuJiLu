package com.scarlett.yuedujilu.ui

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.PixelCopy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.core.app.ApplicationProvider
import com.scarlett.yuedujilu.*
import com.scarlett.yuedujilu.data.*
import com.scarlett.yuedujilu.domain.*
import com.scarlett.yuedujilu.ui.stats.OverviewCards
import com.scarlett.yuedujilu.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.*

/** Real production composables, host-native HWUI/Skia PixelCopy. Fixtures never enter main source/APK.
 * Not a physical-device camera or performance test. Raw PNGs are intentionally unedited.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[35],qualifiers="zh-rCN-w432dp-h936dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FullScreenRenderTest {
  @get:Rule val rule=createComposeRule()
  private val today=LocalDate.of(2026,11,17)
  private val now=today.atTime(10,0).atZone(Zones.Shanghai).toInstant().toEpochMilli()
  private data class Case(val name:String,val state:AppUiState,val width:Int=432,val font:Float=1f,val theme:ThemeId=ThemeId.PINE_GREEN,val stress:Boolean=false)

  @Test fun rendersAllProductionScreensAndBoundaryCases() = runBlocking {
    System.setProperty("robolectric.pixelCopyRenderMode","hardware")
    val app=ApplicationProvider.getApplicationContext<Application>()
    val file=File(app.cacheDir,"render-fixture/library.json").also { it.parentFile!!.mkdirs();it.delete() }
    val repo=LibraryRepository(JsonLibraryStore(file))
    val book=repo.newBook("九诗心",author="黄晓丹",pageCount=320,now=now-86_400_000).copy(id="b1",currentPage=76,status=BookStatus.READING)
    val book2=repo.newBook("我与地坛",author="史铁生",pageCount=384,now=now-86_400_000).copy(id="b2",currentPage=124,status=BookStatus.READING)
    repo.addBook(book);repo.addBook(book2)
    repo.addManualSession(book.id,20,50,now-86_400_000,now-84_600_000,now)
    val secrets=MemorySecretStore().apply { setWereadKey("local-render-fixture-not-a-real-key") }
    val vm=AppViewModel(app,repo,secrets,ShowApiClient(),WereadClient())
    repo.updateSettings { it.copy(wereadConfigured=true,wereadImportStartDate="2026-11-01",wereadLastSyncAt="2026-11-17T01:00:00Z") }
    val hash=WereadGateway.accountPartition(secrets.getWereadKey()!!)
    repo.replaceWereadDuration((1..17).filter { it!=14 }.map {
      WereadDayRecord(hash,LocalDate.of(2026,11,it).toString(),if(it==15)2400 else 0,true,updatedAtEpochMs=now)
    })
    repo.confirmPhysicalUnread("2026-11-13",true)
    val base=AppUiState(snapshot=repo.state.value,now=now,statsAnchor=today,calendarMonth=YearMonth.from(today),hasWereadKey=true)
    val cases=mutableListOf(
      Case("01-shelf",base),
      Case("02-stats",base.copy(tab=MainTab.STATS)),
      Case("03-calendar",base.copy(tab=MainTab.STATS,statsMode=StatsMode.CALENDAR)),
      Case("04-mine",base.copy(tab=MainTab.MINE)),
      Case("05-detail",base.copy(route=Route.BookDetail(book.id))),
      Case("06-timer",base.copy(route=Route.EndSession(book.id))),
      Case("07-end",base.copy(route=Route.EndSession(book.id))),
      Case("08-edit",base.copy(route=Route.EditBook(book.id))),
      Case("09-isbn",base.copy(route=Route.IsbnEntry)),
      Case("10-scan-permission",base.copy(route=Route.ScanIsbn)),
      Case("11-backfill",base.copy(route=Route.Backfill(book.id))),
      Case("12-appearance",base.copy(route=Route.Appearance)),
      Case("13-showapi",base.copy(route=Route.ShowApiConfig)),
      Case("14-weread",base.copy(route=Route.Weread)),
      Case("15-ai",base.copy(route=Route.AiEntry)),
      Case("16-deepseek",base.copy(route=Route.DeepSeek)),
      Case("17-backup",base.copy(route=Route.Backup)),
      Case("18-export",base.copy(route=Route.Export)),
      Case("19-empty",base.copy(snapshot=AppSnapshot())),
      Case("20-add-sheet",base.copy(showPlus=true)),
      Case("21-active-timer-bar",base),
      Case("22-narrow-large-timer",base.copy(route=Route.EndSession(book.id)),360,1.3f),
      Case("23-narrow-large-end",base.copy(route=Route.EndSession(book.id)),360,1.3f),
      Case("24-narrow-large-empty",base.copy(snapshot=AppSnapshot()),360,1.3f)
    )
    listOf(MainTab.SHELF,MainTab.STATS,MainTab.MINE).forEach {
      cases+=Case("narrow-large-${it.name}",base.copy(tab=it),360,1.3f)
    }
    cases+=Case("narrow-large-calendar",base.copy(tab=MainTab.STATS,statsMode=StatsMode.CALENDAR),360,1.3f)
    cases+=Case("narrow-large-detail",base.copy(route=Route.BookDetail(book.id)),360,1.3f)
    cases+=Case("narrow-large-form",base.copy(route=Route.Backfill(book.id)),360,1.3f)
    cases+=Case("narrow-large-long-title",base.copy(snapshot=base.snapshot.copy(books=listOf(book.copy(title="一本有着非常长非常长的书名与副标题的书：关于阅读、时间和记忆的故事",author="一位名字也非常长的作者与译者")))),360,1.3f)
    ThemeId.entries.forEach { cases+=Case("theme-${it.name}",base.copy(tab=MainTab.STATS),theme=it) }
    cases+=Case("narrow-large-metric-stress",base.copy(tab=MainTab.STATS),360,1.3f,stress=true)
    var current by mutableStateOf(cases.first())
    rule.setContent {
      val c=current
      val configuration=Configuration(LocalConfiguration.current).apply { fontScale=c.font }
      val density=LocalDensity.current.density
      CompositionLocalProvider(LocalDensity provides Density(density,c.font),LocalConfiguration provides configuration) {
        YueDuTheme(themeId=c.theme) {
          Box(Modifier.width(c.width.dp).fillMaxHeight().background(LocalPalette.current.background)) {
            key(c.name) {
              if(c.stress) {
                val report=StatsOverview.overview(emptyList(),emptyList(),emptyList(),StatsPeriod.MONTH,today,today,now,SourceFilter.PHYSICAL,null,null,null)
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                  OverviewCards(report.copy(days=MetricValue.Known(123456789),durationMs=MetricValue.Known(888888888888L),booksRead=MetricValue.Missing("缺少明细，不使用书架存量冒充区间本数"),booksFinished=MetricValue.Known(999999),streak=MetricValue.Known(999),dailyAverageMs=MetricValue.Known(88888888,"按已过自然日（含未阅读日），这是一条用于大字排版的长注释")))
                }
              } else AppRoot(vm,c.state)
            }
          }
        }
      }
    }
    val out=File("../ui-audit/screenshots/native").apply { mkdirs() }
    val metadata=mutableListOf<String>()
    for(c in cases) {
      if(c.name=="06-timer" || c.name=="21-active-timer-bar" || c.name=="22-narrow-large-timer") repo.startSession(book.id,76,now-25*60_000)
      rule.runOnIdle { current=c }
      rule.waitForIdle()
      if(c.name=="07-end" || c.name=="23-narrow-large-end") { rule.onNodeWithText("结束阅读").performClick();rule.waitForIdle() }
      val nodes=rule.onAllNodes(isRoot()).fetchSemanticsNodes()
      val view=(nodes.last().root as ViewRootForTest).view
      val widthPx=(c.width*view.resources.displayMetrics.density).toInt().coerceAtMost(view.width)
      val bitmap=Bitmap.createBitmap(widthPx,view.height,Bitmap.Config.ARGB_8888)
      var result=-1
      rule.runOnIdle {
        val request=PixelCopy.Request.Builder.ofWindow(view).setDestinationBitmap(bitmap)
          .setSourceRect(Rect(0,0,widthPx,view.height)).build()
        PixelCopy.request(request,java.util.concurrent.Executor { it.run() }) { result=it.status }
      }
      rule.waitForIdle()
      Assert.assertEquals("PixelCopy ${c.name}",PixelCopy.SUCCESS,result)
      File(out,"${c.name}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
      if(c.name=="07-end" || c.name=="21-active-timer-bar" || c.name=="23-narrow-large-end") repo.endActive(76,now,false)
      metadata+="${c.name},${c.width},${c.font},${c.theme},${bitmap.width},${bitmap.height},HWUI-host-native,TEST-FIXTURE-NOT-LIVE"
    }
    File(out,"capture-matrix.csv").writeText("name,width_dp,font_scale,theme,png_width,png_height,renderer,data\n"+metadata.joinToString("\n"))
  }
}
