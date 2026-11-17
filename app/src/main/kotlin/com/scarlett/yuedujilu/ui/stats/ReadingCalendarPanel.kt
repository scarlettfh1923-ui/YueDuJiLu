package com.scarlett.yuedujilu.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.*
import com.scarlett.yuedujilu.domain.*
import com.scarlett.yuedujilu.ui.common.*
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun ReadingCalendarPanel(vm:AppViewModel,state:AppUiState,hash:String?,today:LocalDate) {
  val p=LocalPalette.current
  val month=state.calendarMonth
  var selectedRaw by rememberSaveable { mutableStateOf(today.toString()) }
  val selected=LocalDate.parse(selectedRaw)
  val snapshot=state.snapshot
  fun day(date:LocalDate)=ReadingCalendar.day(date,today,state.now,state.statsSource,snapshot.books,snapshot.sessions,snapshot.wereadDays,hash,snapshot.confirmedPhysicalUnreadDates)
  fun label(d:ReadingDayState)=when(d){ReadingDayState.READ->"已阅读";ReadingDayState.UNREAD->"确认未读";ReadingDayState.UNKNOWN->"数据未知";ReadingDayState.FUTURE->"未来"}
  SoftCard(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      IconButtonCircle(AppIcons.ChevronLeft,"上个月",onClick={vm.shiftMonth(-1)})
      Text("${month.year} 年 ${month.monthValue} 月",color=p.text,fontWeight=FontWeight.SemiBold,fontSize=18.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
      Box(Modifier.size(52.dp),contentAlignment=Alignment.Center) {
        if(month<YearMonth.from(today)) IconButtonCircle(AppIcons.ChevronRight,"下个月",onClick={vm.shiftMonth(1)})
      }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth()) { listOf("一","二","三","四","五","六","日").forEach { Text(it,color=p.textSecondary,fontSize=12.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.weight(1f)) } }
    Spacer(Modifier.height(10.dp))
    val offset=month.atDay(1).dayOfWeek.value-1
    val count=((offset+month.lengthOfMonth()+6)/7)*7
    (0 until count step 7).forEach { row ->
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
        (row until row+7).forEach { index ->
          val number=index-offset+1
          Box(Modifier.weight(1f).height(48.dp),contentAlignment=Alignment.Center) {
            if(number in 1..month.lengthOfMonth()) {
              val date=month.atDay(number); val d=day(date);val chosen=date==selected
              val fill=when(d.state){ReadingDayState.READ->p.primary;ReadingDayState.UNREAD->p.primarySoft;else->Color.Transparent}
              Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(fill)
                .then(if(chosen)Modifier.border(1.5f.dp,p.primary,CircleShape) else Modifier)
                .pressClickable { selectedRaw=date.toString() }
                .semantics { contentDescription="$date ${label(d.state)}"; this.selected=chosen },contentAlignment=Alignment.Center) {
                Text(number.toString(),color=when(d.state){ReadingDayState.READ->p.onPrimary;ReadingDayState.FUTURE->p.textTertiary.copy(alpha=.4f);else->p.text},fontSize=14.sp)
                if(d.state==ReadingDayState.UNKNOWN) Box(Modifier.align(Alignment.BottomCenter).padding(bottom=3.dp).size(3.dp).background(p.textTertiary,CircleShape))
              }
            }
          }
        }
      }
    }
    Spacer(Modifier.height(14.dp))
    // Two rows work at narrow width and1.3x without squeezing four labels into one line.
    listOf(listOf(ReadingDayState.READ,ReadingDayState.UNREAD),listOf(ReadingDayState.UNKNOWN,ReadingDayState.FUTURE)).forEach { row ->
      Row(Modifier.fillMaxWidth().padding(vertical=4.dp)) { row.forEach { st ->
        Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically) {
          Box(Modifier.size(9.dp).clip(CircleShape).background(when(st){ReadingDayState.READ->p.primary;ReadingDayState.UNREAD->p.primarySoft;ReadingDayState.UNKNOWN->p.textTertiary;else->p.stroke.copy(alpha=.35f)}))
          Text(label(st),color=p.textSecondary,fontSize=11.sp,modifier=Modifier.padding(start=7.dp))
        }
      } }
    }
  }
  Spacer(Modifier.height(16.dp))
  val d=day(selected)
  Text("${selected.monthValue} 月 ${selected.dayOfMonth} 日 · ${label(d.state)}",color=p.text,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
  Spacer(Modifier.height(10.dp))
  SoftCard(Modifier.fillMaxWidth()) {
    if(d.state==ReadingDayState.FUTURE) Text("未来日期，不判定是否阅读。",color=p.textSecondary,fontSize=13.sp)
    else {
      d.physicalSessions.forEach { session ->
        val book=snapshot.books.firstOrNull{it.id==session.bookId}
        val ms=ReadingMath.splitDurationByDay(session,state.now,Zones.Shanghai)[selected]?:0L
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp).pressClickable{vm.go(Route.BookDetail(session.bookId))}) {
          Column(Modifier.weight(1f)) {
            Text(book?.title?:"实体书记录",color=p.text,fontSize=15.sp,fontWeight=FontWeight.Medium)
            Text("实体书 · ${formatDurationZh(ms)}",color=p.textSecondary,fontSize=12.sp)
          }
          Icon(AppIcons.ChevronRight,null,tint=p.primary,modifier=Modifier.size(18.dp))
        }
      }
      if(state.statsSource!=SourceFilter.PHYSICAL) {
        val wr=snapshot.wereadDays.filter{it.accountHash==hash && it.date==selected.toString()}.maxByOrNull{it.updatedAtEpochMs}
        Text(if(wr?.durationSec!=null) "微信读书 · ${formatDurationZh(wr.durationSec*1000)}${if(wr.durationComplete)"" else "（记录未完整）"}" else "微信读书 · 该日尚无完整同步记录",color=p.textSecondary,fontSize=13.sp,lineHeight=21.sp,modifier=Modifier.padding(vertical=6.dp))
      }
      if(d.state==ReadingDayState.UNKNOWN) Text("记录不全，不能把空白当作没读。实体记录与微信日数据按日期合并。",color=p.textSecondary,fontSize=12.sp,lineHeight=20.sp)
      if(d.state==ReadingDayState.UNREAD) Text("所选来源均有明确的零阅读依据。",color=p.textSecondary,fontSize=12.sp,lineHeight=20.sp)
      if(state.statsSource!=SourceFilter.WEREAD && d.physicalMs==0L) {
        var confirm by remember { mutableStateOf(false) }
        Spacer(Modifier.height(8.dp))
        QuietButton(if(d.physicalComplete)"撤销实体未读确认" else "确认当天未读实体书",{confirm=true},Modifier.fillMaxWidth())
        if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text(if(d.physicalComplete)"撤销确认？" else "确认当天没有读实体书？")},
          text={Text("仅作用于 $selected 的实体书。不会改动微信数据或删除任何阅读记录；之后可撤销。")},
          confirmButton={TextButton(onClick={vm.confirmPhysicalUnread(selected,!d.physicalComplete);confirm=false}){Text("确认")}},dismissButton={TextButton(onClick={confirm=false}){Text("取消")}})
      }
    }
  }
  Spacer(Modifier.height(12.dp))
  Text("实心为有记录；浅色为确认未读；带点空白为未知；淡色为未来。缺失同步不是零阅读。",color=p.textSecondary,fontSize=12.sp,lineHeight=20.sp)
}
