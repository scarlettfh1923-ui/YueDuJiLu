package com.scarlett.yuedujilu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.theme.LocalPalette

@Composable
fun AiEntryScreen(vm: AppViewModel) {
  val p = LocalPalette.current
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(bottom = 16.dp)
  ) {
    ScreenHeader("AI 入口", onBack = vm::back)
    Column(Modifier.padding(horizontal = 20.dp)) {
      NoticeCard("用途尚未确定，所以这里不会打开对话、推荐或把阅读记录发送到任何模型。DeepSeek 的密钥可以先在上一页保存，但当前版本不会调用接口。")
      Spacer(Modifier.height(16.dp))
      Text("等你确定要它做什么之后，再在这个入口接上具体功能。", color = p.textSecondary, fontSize = 14.sp, lineHeight = 22.sp)
    }
  }
}
