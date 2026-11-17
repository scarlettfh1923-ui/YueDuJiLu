package com.scarlett.yuedujilu.ui.settings

import com.scarlett.yuedujilu.ui.theme.UiShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.domain.ThemeId
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import com.scarlett.yuedujilu.ui.theme.LocalRadii
import com.scarlett.yuedujilu.ui.theme.LocalThemeCatalog
import com.scarlett.yuedujilu.ui.theme.pressClickable

@Composable
fun AppearanceScreen(vm: AppViewModel, state: AppUiState) {
  val p = LocalPalette.current
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    val catalog = LocalThemeCatalog.current
    ScreenHeader("外观主题", onBack = vm::back)
    Column(
      Modifier
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
      Text("选择一套底色。背景、卡片、文字、图标、按钮和系统栏会一起切换，并在下次打开时保持。", color = p.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
      Spacer(Modifier.height(16.dp))
      listOf(ThemeId.PINE_GREEN,ThemeId.MIST_PLUM,ThemeId.SLATE_BLUE,ThemeId.WARM_IVORY).forEach { id ->
        val pal = catalog.palette(id)
        val selected = state.snapshot.settings.themeId == id
        val shape = UiShape.Regular
        Row(
          Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(if(selected) 1.5f.dp else 0.dp, if(selected) pal.primary else androidx.compose.ui.graphics.Color.Transparent, shape)
            .clip(shape)
            .background(p.surface)
            .pressClickable { vm.setTheme(id) }
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(pal.background)
              .border(3.dp, pal.primary, CircleShape)
          )
          Spacer(Modifier.width(14.dp))
          Column(Modifier.weight(1f)) {
            Text(if(id==ThemeId.PINE_GREEN) "青瓷 · 青绿" else id.displayName, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(catalog.subtitle(id), color = p.textSecondary, fontSize = 12.sp)
          }
          if (selected) {
            Icon(AppIcons.Check, "已选择", tint = pal.primary, modifier = Modifier.size(22.dp))
          }
        }
      }
    }
  }
}
