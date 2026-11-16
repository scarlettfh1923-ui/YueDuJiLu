package com.scarlett.yuedujilu.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.QuietButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader

@Composable
fun BackupScreen(vm: AppViewModel) {
  val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    uri?.let { vm.exportTo(it) }
  }
  val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let { vm.restoreFrom(it) }
  }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    ScreenHeader("备份与恢复", onBack = vm::back)
    Column(Modifier.padding(horizontal = 20.dp)) {
      NoticeCard("备份只包含书架、阅读记录和主题等本地设置，不包含万维易源或 DeepSeek 的密钥。恢复会覆盖当前书架数据。")
      Spacer(Modifier.height(20.dp))
      PrimaryButton("备份到文件", onClick = { export.launch("yuedujilu-backup.json") }, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(12.dp))
      QuietButton("从文件恢复", onClick = { restore.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth())
    }
  }
}
