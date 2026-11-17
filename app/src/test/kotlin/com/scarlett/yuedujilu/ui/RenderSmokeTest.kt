package com.scarlett.yuedujilu.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.scarlett.yuedujilu.ui.theme.*
import com.scarlett.yuedujilu.domain.ThemeId
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.platform.ViewRootForTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "zh-rCN-w432dp-h936dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderSmokeTest {
 @Test fun launcherNameAndPackagePreserveUpgradeIdentity() {
  val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>()
  org.junit.Assert.assertEquals("书蠹",app.getString(com.scarlett.yuedujilu.R.string.app_name))
  org.junit.Assert.assertEquals("书蠹",app.applicationInfo.loadLabel(app.packageManager).toString())
  org.junit.Assert.assertEquals("com.scarlett.yuedujilu",app.packageName)
 }

 @get:Rule val rule = createComposeRule()
 @Test fun rendersNativeCompose() {
  System.setProperty("robolectric.pixelCopyRenderMode","hardware")
  rule.setContent { YueDuTheme(themeId=ThemeId.PINE_GREEN) {
   val backdrop=rememberLayerBackdrop()
   CompositionLocalProvider(LocalBackdrop provides backdrop) {
    Box(Modifier.fillMaxSize()) {
     Column(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color(0xFFF3F7F5))) {
      repeat(26) { Text("阅读记录 · 真正的 Compose 渲染 · $it", Modifier.padding(8.dp)) }
     }
     Box(Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth().height(76.dp).glassSurface(RoundedCornerShape(50))) { Text("书架          统计          我的",Modifier.align(Alignment.Center)) }
    }
   }
  } }
  rule.waitForIdle()
  val view = (rule.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
  val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
  var result=-1
  rule.runOnIdle {
    var ctx:android.content.Context=view.context
    while(ctx !is android.app.Activity && ctx is android.content.ContextWrapper) ctx=ctx.baseContext
    val activity=ctx as android.app.Activity
    android.view.PixelCopy.request(activity.window,b,{result=it},android.os.Handler(android.os.Looper.getMainLooper()))
  }
  rule.waitForIdle()
  check(result==android.view.PixelCopy.SUCCESS) { "PixelCopy=$result" }
  File("../ui-audit/screenshots").mkdirs()
  File("../ui-audit/screenshots/glass-prototype.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) }
  check(b.getPixel(b.width/2,b.height/2) != android.graphics.Color.TRANSPARENT)
 }
}
