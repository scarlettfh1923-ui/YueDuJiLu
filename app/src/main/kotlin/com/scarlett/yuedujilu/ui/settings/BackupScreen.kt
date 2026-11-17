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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.material3.*
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
  var pending by remember { mutableStateOf<android.net.Uri?>(null) }
  val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let { pending=it }
  }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    ScreenHeader("备份与恢复", onBack = vm::back)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
      NoticeCard("备份书架、实体与微信记录、主题设置。不含任何服务密钥；恢复前会再次确认覆盖。")
      Spacer(Modifier.height(20.dp))
      PrimaryButton("备份到文件", onClick = { export.launch("yuedujilu-backup.json") }, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(12.dp))
      QuietButton("从文件恢复", onClick = { restore.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth())
    }
  }
  pending?.let { uri ->
    AlertDialog(onDismissRequest={pending=null},title={Text("恢复会覆盖当前记录")},
      text={Text("请先备份现有数据。确认后读取所选文件并替换本机书架与阅读记录；服务密钥不变。")},
      confirmButton={TextButton(onClick={vm.restoreFrom(uri);pending=null}){Text("确认恢复")}},
      dismissButton={TextButton(onClick={pending=null}){Text("取消")}})
  }
}
