package com.scarlett.yuedujilu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.domain.WereadImport
import com.scarlett.yuedujilu.domain.WereadPartStatus
import com.scarlett.yuedujilu.ui.common.FormField
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.QuietButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.theme.LocalPalette

@Composable
fun WereadScreen(vm: AppViewModel, state: AppUiState) {
  val p = LocalPalette.current
  val settings = state.snapshot.settings
  var key by androidx.compose.runtime.remember { mutableStateOf("") }
  var start by rememberSaveable { mutableStateOf(settings.wereadImportStartDate.orEmpty()) }
  val connected = state.hasWereadKey && WereadImport.parseDate(settings.wereadImportStartDate) != null
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    ScreenHeader("微信读书", onBack = vm::back)
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
    ) {
      NoticeCard(
        if (connected) "已保存连接信息。导入起点固定后，每次同步都是从该日到今天，不会按最近 7 天滚动。"
        else "尚未连接微信读书。填写官方 Skill 密钥并设置导入起点后，即可测试连接并同步阅读时长。"
      )
      Spacer(Modifier.height(16.dp))
      Text(
        if (state.hasWereadKey) "已保存密钥（不会回显原文）。要更换请重新输入。" else "密钥保存在本机密钥库，不会进入备份、导出或日志。",
        color = p.textSecondary,
        fontSize = 13.sp,
        lineHeight = 20.sp,
      )
      Spacer(Modifier.height(16.dp))
      FormField(key, { key = it }, "API Key（以 wrk- 开头）", secret = true)
      Spacer(Modifier.height(16.dp))
      FormField(start, { start = it }, "导入起点（年-月-日）")
      Spacer(Modifier.height(8.dp))
      QuietButton(
        "用最近 7 天作为起点",
        onClick = { start = vm.applyWereadLast7Days(start) },
        modifier = Modifier.fillMaxWidth(),
      )
      Text(
        "最近 7 天只在还没有起点时初始化为今天往前 6 个自然日；已有起点不会被改写。",
        color = p.textTertiary,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(top = 8.dp),
      )
      Spacer(Modifier.height(16.dp))
      PrimaryButton("保存配置", onClick = { vm.saveWereadConfig(key, start) }, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(12.dp))
      Row(Modifier.fillMaxWidth()) {
        QuietButton(
          if (state.wereadBusy) "取消" else "测试连接",
          onClick = { if (state.wereadBusy) vm.cancelWereadSync() else vm.testWereadConnection() },
          modifier = Modifier.weight(1f),
        )
      }
      Spacer(Modifier.height(12.dp))
      if (connected) {
        PrimaryButton(
          if (state.wereadBusy) (state.wereadProgress ?: "同步中…") else "同步到今天",
          onClick = { if (!state.wereadBusy) vm.syncWeread() },
          modifier = Modifier.fillMaxWidth(),
          enabled = !state.wereadBusy,
        )
        Text(
          "会同步日阅读时长，以及划线与想法（按创建日）。书签没有逐条日期，不计入每日新增。未完成或失败的部分不会覆盖已有数据。",
          color = p.textTertiary,
          fontSize = 12.sp,
          lineHeight = 18.sp,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      Spacer(Modifier.height(20.dp))
      Text("当前状态", color = p.textSecondary, fontSize = 12.sp)
      Spacer(Modifier.height(6.dp))
      Text(
        statusLine(connected, settings.wereadDurationStatus, settings.wereadNotesStatus, settings.wereadLastMessage, state.wereadProgress),
        color = p.text,
        fontSize = 15.sp,
        lineHeight = 22.sp,
      )
      settings.wereadImportStartDate?.let {
        Spacer(Modifier.height(8.dp))
        Text("导入起点 $it", color = p.textSecondary, fontSize = 13.sp)
      }
      Spacer(Modifier.height(32.dp))
    }
  }
}

private fun statusLine(
  connected: Boolean,
  duration: WereadPartStatus,
  notes: WereadPartStatus,
  last: String?,
  progress: String?,
): String {
  if (progress != null) return progress
  if (!connected) return "尚未连接微信读书"
  val parts = buildList {
    add("时长：${partLabel(duration)}")
    add("划线与想法：${partLabel(notes)}")
  }
  return listOf(parts.joinToString("，"), last).filter { !it.isNullOrBlank() }.joinToString("\n")
}

private fun partLabel(status: WereadPartStatus): String = when (status) {
  WereadPartStatus.IDLE -> "尚未同步"
  WereadPartStatus.SUCCESS -> "已完成"
  WereadPartStatus.PARTIAL -> "部分完成"
  WereadPartStatus.INCOMPLETE -> "有日期缺少日明细"
  WereadPartStatus.FAILED -> "失败，未改写已有数据"
  WereadPartStatus.UPGRADE -> "需升级技能后继续"
}
