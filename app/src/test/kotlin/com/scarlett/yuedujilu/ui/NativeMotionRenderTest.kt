package com.scarlett.yuedujilu.ui

import android.app.Application
import android.graphics.Bitmap
import android.view.PixelCopy
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.scarlett.yuedujilu.*
import com.scarlett.yuedujilu.data.*
import com.scarlett.yuedujilu.ui.theme.YueDuTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

/** Deterministic native animation frames, not a device screen recording or FPS measurement. */
@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[35],qualifiers="zh-rCN-w432dp-h936dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeMotionRenderTest {
 @get:Rule val rule=createComposeRule()
 @Test fun captureActualNavigationFrames() = runBlocking {
  System.setProperty("robolectric.pixelCopyRenderMode","hardware")
  val app=ApplicationProvider.getApplicationContext<Application>()
  val repo=LibraryRepository(JsonLibraryStore(File(app.cacheDir,"motion-fixture.json").apply { delete() }))
  repo.addBook(repo.newBook("九诗心",author="黄晓丹",pageCount=320,now=System.currentTimeMillis()))
  val vm=AppViewModel(app,repo,MemorySecretStore(),ShowApiClient(),WereadClient())
  rule.setContent { YueDuTheme { val state by vm.ui.collectAsState();AppRoot(vm,state) } }
  rule.waitForIdle();rule.mainClock.autoAdvance=false
  val out=File("../ui-audit/animation-frames").apply { mkdirs() }
  repeat(64) { frame ->
   when(frame) {
    6->rule.onNodeWithTag("tab-STATS").performTouchInput { down(center) }
    8->rule.onNodeWithTag("tab-STATS").performTouchInput { up() }
    10->rule.onNodeWithTag("tab-MINE").performClick()
    12->rule.onNodeWithTag("tab-SHELF").performClick()
    24->rule.onNodeWithTag("tab-STATS").performClick()
    40->rule.onNodeWithTag("tab-MINE").performClick()
   }
   rule.waitForIdle();rule.mainClock.advanceTimeBy(32);rule.waitForIdle()
   val view=(rule.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
   val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
   var status=-1
   rule.runOnIdle { PixelCopy.request(PixelCopy.Request.Builder.ofWindow(view).setDestinationBitmap(b).setSourceRect(android.graphics.Rect(0,0,view.width,view.height)).build(),java.util.concurrent.Executor { it.run() }) {status=it.status} }
   rule.waitForIdle();Assert.assertEquals(PixelCopy.SUCCESS,status)
   File(out,"frame-${frame.toString().padStart(3,'0')}.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) }
  }
 }
}
