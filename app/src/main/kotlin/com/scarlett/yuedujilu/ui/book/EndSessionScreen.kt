package com.scarlett.yuedujilu.ui.book

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.domain.ReadingMath
import com.scarlett.yuedujilu.ui.common.FormField
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.QuietButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.common.SoftCard
import com.scarlett.yuedujilu.ui.common.formatHms
import com.scarlett.yuedujilu.ui.theme.LocalPalette

@Composable
fun EndSessionScreen(vm: AppViewModel, state: AppUiState, bookId: String) {
  val p = LocalPalette.current
  val book = state.snapshot.books.find { it.id == bookId }
  val session = vm.active()?.takeIf { it.bookId == bookId }
  if (book == null || session == null) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
      ScreenHeader("本次阅读", onBack = vm::back)
      Text("没有进行中的计时。", modifier = Modifier.padding(20.dp), color = p.textSecondary)
    }
    return
  }
  var endPage by rememberSaveable { mutableStateOf("") }
  var mark by rememberSaveable { mutableStateOf(false) }
  val startPage = session.startPage ?: book.currentPage.takeIf { it > 0 }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    ScreenHeader("本次阅读", onBack = vm::back)
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
    ) {
      SoftCard(elevated = true) {
        Text(book.title, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(formatHms(ReadingMath.effectiveDurationMs(session, state.now)), color = p.primary, fontSize = 32.sp, fontWeight = FontWeight.Medium)
        Text(if (ReadingMath.isPaused(session)) "已暂停" else "计时中", color = p.textSecondary, fontSize = 13.sp)
        if (startPage != null) Text("起始页 $startPage", color = p.textSecondary, fontSize = 13.sp)
      }
      Spacer(Modifier.height(16.dp))
      QuietButton(if (ReadingMath.isPaused(session)) "继续计时" else "暂停", onClick = { vm.pauseOrResume() }, modifier = Modifier.fillMaxWidth())
      Spacer(Modifier.height(20.dp))
      FormField(
        value = endPage,
        onValueChange = { endPage = it.filter { ch -> ch.isDigit() } },
        label = "结束页码",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      )
      Spacer(Modifier.height(16.dp))
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
          Text("同时记为读完", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
          Text("即使没读到最后一页，也可以记为读完，页数仍按结束页保存。", color = p.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Switch(checked = mark, onCheckedChange = { mark = it })
      }
      Spacer(Modifier.height(16.dp))
      NoticeCard("下一趟阅读的起始页会默认接上这次的结束页。")
      Spacer(Modifier.height(24.dp))
      PrimaryButton(
        "结束并保存",
        onClick = { vm.endSession(endPage.toIntOrNull(), mark) },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(32.dp))
    }
  }
}
