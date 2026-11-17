package com.scarlett.yuedujilu.ui.book

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.ui.common.FormField
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
fun BackfillScreen(vm: AppViewModel, state: AppUiState, bookId: String, sessionId: String?) {
  val p = LocalPalette.current
  val book = state.snapshot.books.find { it.id == bookId }
  val existing = sessionId?.let { id -> state.snapshot.sessions.find { it.id == id } }
  if (book == null) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
      ScreenHeader("补录", onBack = vm::back)
    }
    return
  }
  val defaultStart = existing?.startedAtEpochMs?.let { epochToText(it) }
    ?: LocalDateTime.now(zone).minusHours(1).format(fmt)
  val defaultEnd = existing?.endedAtEpochMs?.let { epochToText(it) }
    ?: LocalDateTime.now(zone).format(fmt)
  var startText by rememberSaveable { mutableStateOf(defaultStart) }
  var endText by rememberSaveable { mutableStateOf(defaultEnd) }
  var startPage by rememberSaveable { mutableStateOf((existing?.startPage ?: book.currentPage.takeIf { it > 0 })?.toString().orEmpty()) }
  var endPage by rememberSaveable { mutableStateOf(existing?.endPage?.toString().orEmpty()) }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    ScreenHeader(if (existing == null) "补录阅读" else "修改记录", onBack = vm::back)
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
    ) {
      Text(book.title, color = p.text, fontSize = 16.sp)
      Spacer(Modifier.height(16.dp))
      FormField(startText, { startText = it }, "开始时间 · 年-月-日 时:分")
      Spacer(Modifier.height(16.dp))
      FormField(endText, { endText = it }, "结束时间 · 年-月-日 时:分")
      Spacer(Modifier.height(16.dp))
      FormField(startPage, { startPage = it.filter { ch -> ch.isDigit() } }, "起始页", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
      Spacer(Modifier.height(16.dp))
      FormField(endPage, { endPage = it.filter { ch -> ch.isDigit() } }, "结束页", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
      Spacer(Modifier.height(16.dp))
      NoticeCard("时间使用上海时区（UTC+8）。有效时长按起止时间计算，跨日记录自动分配到对应日期。")
      Spacer(Modifier.height(24.dp))
      PrimaryButton(
        "保存",
        onClick = {
          val start = parseTime(startText)
          val end = parseTime(endText)
          if (start == null || end == null) {
            vm.toast("时间格式应为 yyyy-MM-dd HH:mm", true)
          } else {
            vm.saveManualSession(book.id, existing?.id, startPage.toIntOrNull(), endPage.toIntOrNull(), start, end)
          }
        },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(32.dp))
    }
  }
}

private fun epochToText(ms: Long): String =
  java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime().format(fmt)

private fun parseTime(text: String): Long? = runCatching {
  LocalDateTime.parse(text.trim(), fmt).atZone(zone).toInstant().toEpochMilli()
}.getOrNull()
