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
import com.scarlett.yuedujilu.ui.common.ScreenHeader

@Composable
fun ExportScreen(vm: AppViewModel) {
  val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    uri?.let { vm.exportTo(it) }
  }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    ScreenHeader("导出阅读记录", onBack = vm::back)
    Column(Modifier.padding(horizontal = 20.dp)) {
      NoticeCard("将本地书架和阅读记录另存为 JSON。文件里不含任何 API 密钥。")
      Spacer(Modifier.height(20.dp))
      PrimaryButton("选择保存位置", onClick = { export.launch("yuedujilu-records.json") }, modifier = Modifier.fillMaxWidth())
    }
  }
}
