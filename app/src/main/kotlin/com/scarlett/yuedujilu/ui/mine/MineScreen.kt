package com.scarlett.yuedujilu.ui.mine

import com.scarlett.yuedujilu.ui.theme.UiShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.Route
import com.scarlett.yuedujilu.ui.MainBottomSpace
import com.scarlett.yuedujilu.ui.common.SettingsRow
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import com.scarlett.yuedujilu.ui.theme.LocalRadii

@Composable
fun MineScreen(vm: AppViewModel, state: AppUiState) {
  val p = LocalPalette.current
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = MainBottomSpace.dp)
  ) {
    Group("阅读连接") {
      SettingsRow(
        title = "微信读书",
        subtitle = when {
          state.hasWereadKey && state.snapshot.settings.wereadImportStartDate != null -> "已保存连接，可同步阅读时长与划线想法"
          else -> "尚未连接微信读书"
        },
        icon = AppIcons.Link,
        onClick = { vm.go(Route.Weread) },
      )
      SettingsRow(
        title = "图书查询配置",
        subtitle = if (state.hasShowApiKey) "万维易源 · 已填写 AppKey" else "万维易源 · AppKey",
        icon = AppIcons.Barcode,
        onClick = { vm.go(Route.ShowApiConfig) },
      )
    }
    Spacer(Modifier.height(20.dp))
    Group("外观") {
      SettingsRow(
        title = "外观主题",
        subtitle = if(state.snapshot.settings.themeId==com.scarlett.yuedujilu.domain.ThemeId.PINE_GREEN) "青瓷 · 青绿" else state.snapshot.settings.themeId.displayName,
        icon = AppIcons.Spark,
        onClick = { vm.go(Route.Appearance) },
      )
    }
    Spacer(Modifier.height(20.dp))
    Group("AI") {
      SettingsRow(
        title = "AI 入口",
        subtitle = "用途待定，暂不提供对话或推荐",
        icon = AppIcons.Spark,
        onClick = { vm.go(Route.AiEntry) },
      )
      SettingsRow(
        title = "DeepSeek 配置",
        subtitle = if (state.hasDeepSeekKey) "已保存密钥 · 不会自动调用" else "自行填写，密钥仅存本机",
        icon = AppIcons.Key,
        onClick = { vm.go(Route.DeepSeek) },
      )
    }
    Spacer(Modifier.height(20.dp))
    Group("数据管理") {
      SettingsRow(
        title = "备份与恢复",
        subtitle = "导出或读回本地记录，不含密钥",
        icon = AppIcons.Backup,
        onClick = { vm.go(Route.Backup) },
      )
      SettingsRow(
        title = "导出阅读记录",
        subtitle = "另存为 JSON 文件",
        icon = AppIcons.Export,
        onClick = { vm.go(Route.Export) },
      )
    }
  }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
  val p = LocalPalette.current
  Text(title, color = p.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
  Column(
    Modifier
      .fillMaxWidth()
      .clip(UiShape.Regular)
      .background(p.surface, UiShape.Regular)
  ) { content() }
}
