package com.scarlett.yuedujilu.ui
import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.scarlett.yuedujilu.*
import com.scarlett.yuedujilu.data.*
import com.scarlett.yuedujilu.domain.*
import com.scarlett.yuedujilu.ui.theme.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Job
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.*
@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[35],qualifiers="zh-rCN-w432dp-h936dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LookupAndDetailsInteractionTest {
 @get:Rule val rule=createComposeRule()
 private fun awaitCondition(timeout:Long=10000,condition:()->Boolean){rule.waitUntil(timeout){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();condition()}}
 private fun fixture(secrets:SecretStore=MemorySecretStore(),client:ShowApiClient=ShowApiClient(),appUi:Boolean=false):AppViewModel {
  val app=ApplicationProvider.getApplicationContext<Application>();val file=File(app.cacheDir,"lookup-${System.nanoTime()}.json")
  val vm=AppViewModel(app,LibraryRepository(JsonLibraryStore(file)),secrets,client,WereadClient())
  rule.setContent { val state by vm.ui.collectAsState(); YueDuTheme { if(appUi)AppRoot(vm,state) else Text("${state.route} ${state.lookupBusy}") } }
  rule.waitForIdle();return vm
 }
 @Test fun missingKeyAndSecretFailureKeepIsbnAndClearBusy() {
  var fail=false;val base=MemorySecretStore();val secrets=object:SecretStore by base {override fun getShowApiKey():String? {if(fail)error("TEST-SECRET-MUST-NOT-LEAK");return null}}
  val vm=fixture(secrets)
  rule.runOnIdle{vm.lookupIsbn("9780306406157")};awaitCondition(5000){vm.ui.value.route is Route.EditBook};rule.waitForIdle()
  assertEquals("9780306406157",(vm.ui.value.route as Route.EditBook).presetIsbn);assertFalse(vm.ui.value.lookupBusy)
  rule.runOnIdle{vm.back();fail=true;vm.lookupIsbn("9780306406157")};awaitCondition(5000){vm.ui.value.route is Route.EditBook};rule.waitForIdle()
  assertFalse(vm.ui.value.lookupBusy);assertFalse(vm.ui.value.message?.text.orEmpty().contains("TEST-SECRET-MUST-NOT-LEAK"))
 }
 @Test fun actualHttpSuccessAndFailureNavigateToSafeEditor() {
  var ok=false;val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
  server.createContext("/isbn"){x->x.requestBody.use{it.readBytes()};val bytes=(if(ok)"""{"showapi_res_code":0,"showapi_res_body":{"ret_code":0,"data":{"title":"查询夹具","author":null,"page":null}}}""" else "invalid").toByteArray();x.sendResponseHeaders(if(ok)200 else 500,bytes.size.toLong());x.responseBody.use{it.write(bytes)}};server.start()
  try { val vm=fixture(MemorySecretStore().apply{setShowApiKey("LOCAL-FIXTURE-NOT-A-KEY")},ShowApiClient("http://127.0.0.1:${server.address.port}/isbn"))
   rule.runOnIdle{vm.go(Route.ScanIsbn);vm.lookupIsbn("9780306406157")};awaitCondition(10000){vm.ui.value.route is Route.EditBook};rule.waitForIdle();assertFalse(vm.ui.value.lookupBusy)
   rule.runOnIdle{vm.back();ok=true;vm.lookupIsbn("9780306406157")};awaitCondition(10000){(vm.ui.value.route as? Route.EditBook)?.presetTitle=="查询夹具"};rule.waitForIdle();assertFalse(vm.ui.value.lookupBusy)
  } finally { server.stop(0) }
 }
 @Test fun leavingWhileLookupIsPendingDoesNotNavigateBackOnCompletion() {
  val entered=CountDownLatch(1);val release=CountDownLatch(1);val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
  server.createContext("/isbn"){x->x.requestBody.use{it.readBytes()};entered.countDown();release.await(10,TimeUnit.SECONDS);val bytes="{}".toByteArray();x.sendResponseHeaders(200,bytes.size.toLong());x.responseBody.use{it.write(bytes)}};server.start()
  try { val vm=fixture(MemorySecretStore().apply{setShowApiKey("LOCAL-FIXTURE-NOT-A-KEY")},ShowApiClient("http://127.0.0.1:${server.address.port}/isbn"));lateinit var job:Job
   rule.runOnIdle{vm.go(Route.IsbnEntry);job=vm.lookupIsbn("9780306406157")};assertTrue(entered.await(5,TimeUnit.SECONDS))
   rule.runOnIdle{vm.back()};release.countDown();awaitCondition(10000){job.isCompleted};rule.waitForIdle();assertEquals(Route.Main,vm.ui.value.route);assertFalse(vm.ui.value.lookupBusy)
  } finally { release.countDown();server.stop(0) }
 }
 @Test fun allSixCardsOpenRealRoutesReturnAndRetainPeriodSource() {
  val vm=fixture(appUi=true);rule.runOnIdle{vm.setTab(MainTab.STATS);vm.setPeriod(StatsPeriod.WEEK);vm.setStatsAnchor(java.time.LocalDate.now(Zones.Shanghai).minusWeeks(2));vm.setStatsSource(SourceFilter.PHYSICAL)};rule.waitForIdle()
  StatsMetric.entries.forEach { metric->
   rule.onNodeWithTag("metric-${metric.name}").performScrollTo().performClick();rule.waitForIdle()
   assertEquals(Route.StatsDetail(metric),vm.ui.value.route);rule.onNodeWithTag("detail-${metric.name}").assertExists()
   if(metric==StatsMetric.DAYS){
    val dateNode=rule.onAllNodes(SemanticsMatcher("selected calendar day") { node->
     node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Selected)==true && node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription).orEmpty().any{it.matches(Regex("\\d{4}-\\d{2}-\\d{2} .*"))}
    }).fetchSemanticsNodes().single()
    val date=java.time.LocalDate.parse(dateNode.config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].first().substringBefore(' '))
    assertTrue(date in StatsOverview.resolveSpan(StatsPeriod.WEEK,vm.ui.value.statsAnchor,java.time.LocalDate.now(Zones.Shanghai))!!)
   }
   assertEquals(StatsPeriod.WEEK,vm.ui.value.statsPeriod);assertEquals(SourceFilter.PHYSICAL,vm.ui.value.statsSource)
   rule.onNodeWithContentDescription("返回统计").performClick();rule.waitForIdle();assertEquals(Route.Main,vm.ui.value.route)
  }
  rule.onNodeWithTag("metric-DURATION").performScrollTo().performClick();rule.waitForIdle();rule.onNodeWithTag("tab-MINE").performClick();rule.waitForIdle();assertEquals(Route.Main,vm.ui.value.route);assertEquals(MainTab.MINE,vm.ui.value.tab)
 }
 @Test fun metricRowsHaveEqualHeightsAndValuesShareFixedBaselineSlots() {
  val vm=fixture(appUi=true);rule.runOnIdle{vm.setTab(MainTab.STATS)};rule.waitForIdle()
  StatsMetric.entries.toList().chunked(2).forEach { pair->val a=rule.onNodeWithTag("metric-${pair[0].name}").fetchSemanticsNode().boundsInRoot;val b=rule.onNodeWithTag("metric-${pair[1].name}").fetchSemanticsNode().boundsInRoot;assertEquals(a.top,b.top,.5f);assertEquals(a.height,b.height,.5f)}
 }
 @Test fun fixedNumericBaselineSurvivesLongValueMissingValueAndLargeFont() {
  assertEquals("1.2亿",com.scarlett.yuedujilu.ui.stats.compactCardNumber("123456789"))
  assertEquals("24.7万",com.scarlett.yuedujilu.ui.stats.compactCardNumber("246913.6"))
  assertEquals("9999",com.scarlett.yuedujilu.ui.stats.compactCardNumber("9999"))
  val baselines=mutableMapOf<Int,Float>()
  rule.setContent {
   val density=LocalDensity.current.density
   CompositionLocalProvider(LocalDensity provides Density(density,1.3f)) {
    YueDuTheme {
     Column { listOf("123456789" to "天","未提供" to "","1.2" to "小时").forEachIndexed { i,(v,u)->
      com.scarlett.yuedujilu.ui.stats.MetricNumber(v,u,if(i==1)26 else 36,Modifier.width(150.dp).onGloballyPositioned { c->baselines[i]=c[FirstBaseline].toFloat() })
     } }
    }
   }
  }
  rule.waitForIdle();assertEquals(3,baselines.size);assertTrue(baselines.values.max()-baselines.values.min()<.5f)
 }

}
