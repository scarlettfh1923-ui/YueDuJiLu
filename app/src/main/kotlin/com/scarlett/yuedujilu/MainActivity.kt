package com.scarlett.yuedujilu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scarlett.yuedujilu.ui.AppRoot
import com.scarlett.yuedujilu.ui.theme.YueDuTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)
    val app = application as YueDuApp
    val vm: AppViewModel by viewModels { AppViewModelFactory(app, app.container) }
    setContent {
      val state by vm.ui.collectAsStateWithLifecycle()
      val catalog = app.container.themeCatalog
      val palette = catalog.palette(state.snapshot.settings.themeId)
      val color = palette.background.toArgb()
      SideEffect {
        enableEdgeToEdge(
          statusBarStyle = SystemBarStyle.light(color, color),
          navigationBarStyle = SystemBarStyle.light(color, color),
        )
      }
      YueDuTheme(themeId = state.snapshot.settings.themeId, catalog = catalog) {
        AppRoot(vm, state)
      }
    }
  }
}
