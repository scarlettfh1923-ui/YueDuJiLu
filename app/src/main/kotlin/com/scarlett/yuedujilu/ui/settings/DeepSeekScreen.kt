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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.ui.common.FormField
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import kotlin.math.roundToInt

@Composable
fun DeepSeekScreen(vm: AppViewModel, state: AppUiState) {
  val p = LocalPalette.current
  val s = state.snapshot.settings
  var key by androidx.compose.runtime.remember { mutableStateOf("") }
  var model by rememberSaveable { mutableStateOf(s.deepSeekModel) }
  var base by rememberSaveable { mutableStateOf(s.deepSeekBaseUrl) }
  var thinking by rememberSaveable { mutableStateOf(s.deepSeekDeepThinking) }
  var temp by rememberSaveable { mutableFloatStateOf(s.deepSeekTemperature) }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    ScreenHeader("DeepSeek 配置", onBack = vm::back)
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
    ) {
      NoticeCard("仅保存配置，不自动调用 AI。密钥加密存于本机，不进入备份或日志。")
      Spacer(Modifier.height(16.dp))
      Text(if (state.hasDeepSeekKey) "已保存密钥（不会回显原文）。要更换请重新输入。" else "尚未填写密钥。", color = p.textSecondary, fontSize = 13.sp)
      Spacer(Modifier.height(16.dp))
      FormField(key, { key = it }, "API Key", secret=true)
      Spacer(Modifier.height(16.dp))
      FormField(base, { base = it }, "接口地址")
      Spacer(Modifier.height(16.dp))
      FormField(model, { model = it }, "模型名")
      Spacer(Modifier.height(20.dp))
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text("深度思考", color = p.text, fontSize = 15.sp)
          Text("默认关闭。开启后温度不可调。", color = p.textSecondary, fontSize = 12.sp)
        }
        Switch(checked = thinking, onCheckedChange = { thinking = it })
      }
      Spacer(Modifier.height(12.dp))
      Text("温度  ${"%.1f".format(temp)}（0–2，默认 0.7）", color = if (thinking) p.textTertiary else p.text, fontSize = 13.sp)
      Slider(
        value = temp,
        onValueChange = { temp = (it * 10).roundToInt() / 10f },
        valueRange = 0f..2f,
        steps = 19,
        enabled = !thinking,
      )
      Spacer(Modifier.height(16.dp))
      PrimaryButton(
        "保存配置",
        onClick = { vm.saveDeepSeek(key, if (thinking) s.deepSeekTemperature else temp, thinking, model, base) },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(32.dp))
    }
  }
}
