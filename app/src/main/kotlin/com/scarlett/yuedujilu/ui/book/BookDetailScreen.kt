package com.scarlett.yuedujilu.ui.book

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.Route
import com.scarlett.yuedujilu.domain.BookStatus
import com.scarlett.yuedujilu.domain.ReadingMath
import com.scarlett.yuedujilu.domain.SessionOrigin
import com.scarlett.yuedujilu.ui.common.EmptyState
import com.scarlett.yuedujilu.ui.common.IconButtonCircle
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.QuietButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.common.SoftCard
import com.scarlett.yuedujilu.ui.common.formatDate
import com.scarlett.yuedujilu.ui.common.formatDateTime
import com.scarlett.yuedujilu.ui.common.formatHms
import com.scarlett.yuedujilu.ui.common.progressLabel
import com.scarlett.yuedujilu.ui.common.sourceLabel
import com.scarlett.yuedujilu.ui.common.statusLabel
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.shelf.CoverPlate
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(vm: AppViewModel, state: AppUiState, bookId: String) {
  val p = LocalPalette.current
  val book = state.snapshot.books.find { it.id == bookId }
  if (book == null) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
      ScreenHeader("书籍", onBack = vm::back)
      EmptyState("找不到这本书", "它可能已被删除。")
    }
    return
  }
  val sessions = vm.sessionsOf(bookId)
  val active = vm.active()
  val runningHere = active?.bookId == bookId
  var showDate by remember { mutableStateOf(false) }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    ScreenHeader(title = "书籍详情", onBack = vm::back) {
      IconButtonCircle(AppIcons.Edit, "编辑", onClick = { vm.go(Route.EditBook(book.id)) })
    }
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
      Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CoverPlate(book, width = 100.dp)
          Spacer(Modifier.width(16.dp))
          Column(Modifier.weight(1f)) {
            Text(book.title, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (book.author.isNotBlank()) Text(book.author, color = p.textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("${book.sourceLabel()} · ${book.statusLabel()} · ${book.progressLabel()}", color = p.textSecondary, fontSize = 13.sp)
            book.isbn?.takeIf { it.isNotBlank() }?.let {
              Text("ISBN $it", color = p.textTertiary, fontSize = 12.sp)
            }
            if (book.status == BookStatus.FINISHED && book.finishedAtEpochMs != null) {
              Text("完读于 ${formatDate(book.finishedAtEpochMs)}", color = p.primary, fontSize = 13.sp)
            }
          }
        }
        if (book.isPhysicalBook()) {
          Spacer(Modifier.height(16.dp))
          val action = when {
            runningHere -> "查看计时"
            book.currentPage > 0 -> "继续阅读"
            else -> "开始阅读"
          }
          PrimaryButton(action, onClick = { vm.startOrOpenTimer(book) }, modifier = Modifier.fillMaxWidth())
          Spacer(Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuietButton("补录记录", onClick = { vm.go(Route.Backfill(book.id)) },modifier=Modifier.weight(1f))
            if (book.status != BookStatus.FINISHED) {
              QuietButton("标记读完", onClick = { vm.markFinished(book.id) },modifier=Modifier.weight(1f))
            } else {
              QuietButton("撤销读完", onClick = { vm.undoFinish(book.id) },modifier=Modifier.weight(1f))
            }
          }
          if (book.status == BookStatus.FINISHED) {
            Spacer(Modifier.height(8.dp))
            QuietButton("修改完读日期", onClick = { showDate = true })
          }
          if (book.status != BookStatus.ABANDONED) {
            Spacer(Modifier.height(8.dp))
            QuietButton("移入弃读", onClick = { vm.setAbandoned(book.id, true) })
          } else {
            Spacer(Modifier.height(8.dp))
            QuietButton("取消弃读", onClick = { vm.setAbandoned(book.id, false) })
          }
          Text(
            "读完由你决定，不强制翻到最后一页，保留真实阅读进度。",
            color = p.textTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
            lineHeight = 18.sp,
          )
        }
      }
      Spacer(Modifier.height(20.dp))
      Text("阅读记录", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(10.dp))
      if (sessions.isEmpty()) {
        SoftCard { Text("还没有阅读记录。开始计时或补录一次即可。", color = p.textSecondary, fontSize = 14.sp) }
      } else {
        sessions.forEach { s ->
          SoftCard(onClick = {
            if (s.endedAtEpochMs != null) vm.go(Route.Backfill(book.id, s.id))
            else vm.go(Route.EndSession(book.id))
          }) {
            val dur = ReadingMath.effectiveDurationMs(s, state.now)
            val pages = if (s.startPage != null && s.endPage != null) "${s.startPage}–${s.endPage} 页" else "未记页码"
            val origin = if (s.origin == SessionOrigin.MANUAL) "补录" else "计时"
            Text("$origin · ${formatHms(dur)}", color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("${formatDateTime(s.startedAtEpochMs)} 起", color = p.textSecondary, fontSize = 13.sp)
            s.endedAtEpochMs?.let { Text("至 ${formatDateTime(it)} · $pages", color = p.textSecondary, fontSize = 13.sp) }
              ?: Text(if (ReadingMath.isPaused(s)) "进行中（已暂停） · $pages" else "进行中 · $pages", color = p.primary, fontSize = 13.sp)
          }
          Spacer(Modifier.height(10.dp))
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
  if (showDate) {
    val picker = rememberDatePickerState(
      initialSelectedDateMillis = book.finishedAtEpochMs ?: state.now,
    )
    DatePickerDialog(
      onDismissRequest = { showDate = false },
      confirmButton = {
        TextButton(onClick = {
          picker.selectedDateMillis?.let { vm.setFinishDate(book.id, it) }
          showDate = false
        }) { Text("确定") }
      },
      dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } },
    ) { DatePicker(picker) }
  }
}

private fun com.scarlett.yuedujilu.domain.Book.isPhysicalBook() =
  source != com.scarlett.yuedujilu.domain.BookSource.WEREAD
