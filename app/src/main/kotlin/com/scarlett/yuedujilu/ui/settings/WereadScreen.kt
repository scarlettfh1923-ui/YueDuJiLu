package com.scarlett.yuedujilu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.common.SoftCard
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.ui.theme.LocalPalette

@Composable
fun WereadScreen(vm: AppViewModel) {
  val p = LocalPalette.current
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    ScreenHeader("微信读书接入", onBack = vm::back)
    Column(Modifier.padding(horizontal = 20.dp)) {
      NoticeCard("目前没有可用的微信读书 skill 或官方接口接入。这里只保留入口与数据模型，不会伪造同步成功，也不会生成示范书籍。")
      Spacer(Modifier.height(16.dp))
      SoftCard {
        Text("当前状态", color = p.textSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text("待配置", color = p.text, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("接入之后，统计页的同步按钮才会真正拉取数据。自动后台同步在小米 HyperOS 上也不能保证准时。", color = p.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
      }
    }
  }
}
