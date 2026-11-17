package com.scarlett.yuedujilu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.ui.common.FormField
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.QuietButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.theme.LocalPalette

@Composable
fun ShowApiConfigScreen(vm: AppViewModel, state: AppUiState) {
  val p = LocalPalette.current
  var key by androidx.compose.runtime.remember { mutableStateOf("") }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    ScreenHeader("图书查询配置", onBack = vm::back)
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
    ) {
      NoticeCard("万维易源 · ISBN 图书查询。填写 AppKey 后可查书，未配置仍可手动添加。")
      Spacer(Modifier.height(16.dp))
      Text(if (state.hasShowApiKey) "已保存 AppKey（不会回显原文）。要更换请重新输入并保存。" else "尚未填写 AppKey。未填写时不会发出查询请求。", color = p.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
      Spacer(Modifier.height(16.dp))
      FormField(
        value = key,
        onValueChange = { key = it },
        label = "AppKey",
        secret = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      )
      Spacer(Modifier.height(20.dp))
      PrimaryButton("保存", onClick = { vm.saveShowApiKey(key) }, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(12.dp))
      QuietButton(
        if (state.testBusy) "测试中…" else "测试连接",
        onClick = { vm.testShowApi() },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(12.dp))
      if (state.hasShowApiKey) {
        QuietButton("清除 AppKey", onClick = { vm.saveShowApiKey("") }, modifier = Modifier.fillMaxWidth())
      }
      Spacer(Modifier.height(12.dp))
      Text("测试连接只会在已保存密钥后发起，用于确认凭证是否被接口接受，不代表已经添加了某本书。", color = p.textTertiary, fontSize = 12.sp, lineHeight = 18.sp)
      Spacer(Modifier.height(32.dp))
    }
  }
}
