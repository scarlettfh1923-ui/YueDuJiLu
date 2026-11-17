package com.scarlett.yuedujilu.ui.book

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.scarlett.yuedujilu.ui.theme.*
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
    Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
      IconButtonCircle(AppIcons.ChevronLeft,"返回",onClick=vm::back)
      Text("书籍详情",color=p.text,fontSize=16.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center,modifier=Modifier.weight(1f))
      IconButtonCircle(AppIcons.Edit,"编辑",onClick={vm.go(Route.EditBook(book.id))})
    }
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
      Row(Modifier.fillMaxWidth().porcelainSurface(UiShape.Regular,p.surface,p.text).padding(18.dp),verticalAlignment=Alignment.Top) {
        CoverPlate(book,width=84.dp)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
          Text(book.title,color=p.text,fontSize=21.sp,lineHeight=29.sp,fontWeight=FontWeight.SemiBold)
          if(book.author.isNotBlank())Text(book.author,color=p.textSecondary,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.padding(top=4.dp))
          Text("${book.sourceLabel()} · ${book.statusLabel()}",color=p.textSecondary,fontSize=11.sp,lineHeight=18.sp,modifier=Modifier.padding(top=10.dp))
          Text(book.progressLabel(),color=p.textSecondary,fontSize=13.sp,lineHeight=20.sp,modifier=Modifier.padding(top=8.dp))
          val progress=if((book.pageCount?:0)>0)(book.currentPage.toFloat()/book.pageCount!!).coerceIn(0f,1f) else 0f
          Box(Modifier.padding(top=7.dp).fillMaxWidth().height(3.dp).clip(UiShape.Capsule).background(p.track)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(p.primary.copy(alpha=.65f)))
          }
          book.isbn?.takeIf{it.isNotBlank()}?.let { Text("ISBN $it",color=p.textTertiary,fontSize=11.sp,lineHeight=17.sp,modifier=Modifier.padding(top=8.dp)) }
          if(book.status==BookStatus.FINISHED&&book.finishedAtEpochMs!=null)Text("完读于 ${formatDate(book.finishedAtEpochMs)}",color=p.primary,fontSize=12.sp,lineHeight=18.sp)
        }
      }
      if(book.isPhysicalBook()) {
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(UiShape.Regular).background(p.groupBase).padding(16.dp)) {
          DetailAction(when {runningHere->"查看计时";book.currentPage>0->"继续阅读";else->"开始阅读"},true,{vm.startOrOpenTimer(book)},Modifier.fillMaxWidth())
          Spacer(Modifier.height(10.dp))
          Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DetailAction("补录记录",false,{vm.go(Route.Backfill(book.id))},Modifier.weight(1f).fillMaxHeight())
            DetailAction(if(book.status==BookStatus.FINISHED)"撤销读完" else "标记读完",false,{
              if(book.status==BookStatus.FINISHED)vm.undoFinish(book.id) else vm.markFinished(book.id)
            },Modifier.weight(1f).fillMaxHeight())
            DetailAction(if(book.status==BookStatus.ABANDONED)"取消弃读" else "移入弃读",false,{vm.setAbandoned(book.id,book.status!=BookStatus.ABANDONED)},Modifier.weight(1f).fillMaxHeight())
          }
          if(book.status==BookStatus.FINISHED) {
            Spacer(Modifier.height(10.dp));DetailAction("修改完读日期",false,{showDate=true},Modifier.fillMaxWidth())
          }
          Text("读完由你决定，保留真实阅读进度。",color=p.textSecondary,fontSize=11.sp,lineHeight=18.sp,modifier=Modifier.padding(top=10.dp))
        }
      }
      Spacer(Modifier.height(20.dp))
      Text("阅读记录 · ${sessions.size}", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun DetailAction(text:String,primary:Boolean,onClick:()->Unit,modifier:Modifier=Modifier) {
 val p=LocalPalette.current
 Box(modifier.defaultMinSize(minHeight=if(primary)52.dp else 48.dp)
  .porcelainSurface(if(primary)UiShape.Capsule else UiShape.Regular,if(primary)p.primarySoft else p.surfaceRaised,p.text,if(primary)PorcelainLevel.CONTROL else PorcelainLevel.SUBTLE)
  .pressClickable(onClick=onClick).padding(horizontal=6.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
   Text(text,color=if(primary)p.primary else p.textSecondary,fontSize=if(primary)14.sp else 12.sp,lineHeight=18.sp,fontWeight=if(primary)FontWeight.SemiBold else FontWeight.Medium,textAlign=TextAlign.Center,maxLines=2)
 }
}
