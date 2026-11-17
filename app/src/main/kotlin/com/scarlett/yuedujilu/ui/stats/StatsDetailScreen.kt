package com.scarlett.yuedujilu.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.*
import com.scarlett.yuedujilu.data.todayInShanghai
import com.scarlett.yuedujilu.domain.*
import com.scarlett.yuedujilu.ui.LocalMainBottomSpace
import com.scarlett.yuedujilu.ui.common.*
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.theme.*
import java.time.LocalDate

internal data class StatsBundle(val report:OverviewReport,val detail:StatsDetailData)
@Composable internal fun rememberStatsBundle(state:AppUiState,hash:String?):StatsBundle {
 val today=todayInShanghai(state.now)
 val active=state.snapshot.sessions.any{it.endedAtEpochMs==null}
 return remember(state.snapshot,state.statsPeriod,state.statsAnchor,state.statsSource,hash,today,if(active)state.now else 0L) {
  val d=StatsDetails.project(state.snapshot,state.statsPeriod,state.statsAnchor,today,state.now,state.statsSource,hash)
  val s=state.snapshot.settings
  val report=StatsOverview.overview(sessions=state.snapshot.sessions,books=state.snapshot.books,wereadDays=state.snapshot.wereadDays,period=state.statsPeriod,anchor=state.statsAnchor,today=today,nowEpochMs=state.now,source=state.statsSource,wereadAccountHash=hash,wereadCoverageStart=WereadImport.parseDate(s.wereadImportStartDate),wereadCoverageEnd=StatsOverview.parseSyncDate(s.wereadLastSyncAt))
  val days=if(state.statsSource==SourceFilter.WEREAD&&!d.wereadConnected)MetricValue.Missing("尚未连接微信读书")
   else if(d.knownDays==0&&d.unknownDays>0)MetricValue.Missing("区间存在未知日期")
   else MetricValue.Known(d.knownDays.toLong(),if(d.unknownDays>0)"仅计有记录的日期并集，未知不作零" else "实体与微信日期去重")
  StatsBundle(report.copy(days=days,streak=d.runMetric,dailyAverageMs=d.averageMs),d)
 }
}
@Composable fun StatsDetailScreen(vm:AppViewModel,state:AppUiState,metric:StatsMetric) {
 val p=LocalPalette.current;val bundle=rememberStatsBundle(state,vm.wereadAccountHash());val d=bundle.detail;val report=bundle.report
 val selectedValue=when(metric){StatsMetric.DURATION->report.durationMs;StatsMetric.DAYS->report.days;StatsMetric.PHYSICAL->report.booksRead;StatsMetric.EBOOK->MetricValue.Missing("微信接口未提供精确书单或本数");StatsMetric.STREAK->d.runMetric;StatsMetric.AVERAGE->d.averageMs}
 val today=todayInShanghai(state.now)
 Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=LocalMainBottomSpace.current).testTag("detail-${metric.name}")) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   IconButtonCircle(AppIcons.ChevronLeft,"返回统计",onClick=vm::back)
   Text(metric.label,color=p.text,fontSize=19.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f).padding(start=8.dp))
   Icon(androidx.compose.ui.res.painterResource(statsIcon(metric)),null,tint=p.primary,modifier=Modifier.padding(12.dp).size(24.dp))
  }
  Spacer(Modifier.height(22.dp))
  Text("${report.periodLabel} · ${sourceLabel(state.statsSource)}",color=p.textSecondary,fontSize=13.sp,lineHeight=21.sp)
  Text(if(d.days.isEmpty())"未来区间 · 尚无已过日期" else "${d.span.start} 至 ${minOf(d.span.end,today)}",color=p.textSecondary,fontSize=12.sp,modifier=Modifier.padding(top=5.dp))
  Spacer(Modifier.height(20.dp))
  val pair=when(val v=selectedValue){is MetricValue.Missing->"未提供" to "";is MetricValue.Known->when(metric){StatsMetric.DURATION,StatsMetric.AVERAGE->durationPair(v.value);StatsMetric.PHYSICAL,StatsMetric.EBOOK->v.value.toString() to "本";else->v.value.toString() to "天"}}
  MetricNumber(pair.first,pair.second,if(selectedValue is MetricValue.Missing)32 else 46)
  Text(when(selectedValue){is MetricValue.Known->selectedValue.footnote.orEmpty();is MetricValue.Missing->selectedValue.reason},color=p.textSecondary,fontSize=12.sp,lineHeight=21.sp)
  if(selectedValue is MetricValue.Known)selectedValue.compare?.let { Text(it,color=p.primary,fontSize=13.sp,modifier=Modifier.padding(top=8.dp)) }
  Spacer(Modifier.height(24.dp))
  when(metric) {
   StatsMetric.DURATION->{
    var chosen by rememberSaveable(state.statsPeriod,state.statsAnchor.toString(),state.statsSource){mutableStateOf(d.days.lastOrNull{it.state==ReadingDayState.READ}?.date?.toString()?:d.days.lastOrNull()?.date?.toString())}
    val trendMax=d.days.maxOfOrNull{it.physicalMs+(it.wereadMs?:0)}?.coerceAtLeast(1)?:1L
    val trendState=androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex=(d.days.indexOfFirst{it.date.toString()==chosen}-4).coerceAtLeast(0))
    SoftCard {
     SectionLabel("每日时长")
     if(d.days.isEmpty()) DetailNote("本区间尚无已过日期。") else {
      androidx.compose.foundation.lazy.LazyRow(state=trendState,horizontalArrangement=Arrangement.spacedBy(3.dp),modifier=Modifier.fillMaxWidth().height(145.dp)) {
       items(d.days.size,key={d.days[it].date.toString()}) { index->val day=d.days[index];val ms=day.physicalMs+(day.wereadMs?:0)
        Column(Modifier.width(45.dp).fillMaxHeight().clip(UiShape.Capsule).pressClickable{chosen=day.date.toString()}.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
         Box(Modifier.weight(1f).width(13.dp),contentAlignment=Alignment.BottomCenter) {
          Box(Modifier.fillMaxWidth().height((if(ms>0)80f*ms/trendMax else 3f).coerceAtLeast(3f).dp).clip(UiShape.Capsule).background(if(day.date.toString()==chosen)p.primary else if(ms>0)p.primary.copy(alpha=.45f) else p.stroke))
         }
         Text("${day.date.monthValue}/${day.date.dayOfMonth}",color=p.textSecondary,fontSize=10.sp,modifier=Modifier.padding(top=10.dp))
         Text(if(day.state==ReadingDayState.UNKNOWN)"?" else " ",color=p.textTertiary,fontSize=10.sp,lineHeight=16.sp)
        }
       }
      }
      Text("单位：分钟 · 可横滑；? 为来源不全，空白不代表未读。",color=p.textSecondary,fontSize=11.sp,lineHeight=20.sp)
     }
    }
    Spacer(Modifier.height(16.dp))
    SoftCard {
     SectionLabel("来源拆分")
     if(d.knownMs>0) Row(Modifier.fillMaxWidth().height(4.dp).clip(UiShape.Capsule)) {
      if(d.physicalMs>0)Box(Modifier.weight(d.physicalMs.toFloat()/d.knownMs).fillMaxHeight().background(p.primary.copy(alpha=.65f)))
      if(d.wereadMs>0)Box(Modifier.weight(d.wereadMs.toFloat()/d.knownMs).fillMaxHeight().background(androidx.compose.ui.graphics.Color(0xFF8FAEC2)))
     }
     if(d.includesPhysical)DetailLine("实体书 · 本机记录",formatDurationZh(d.physicalMs))
     if(d.includesWeread)DetailLine("微信 · 已确认时长",if(d.wereadConnected)formatDurationZh(d.wereadMs) else "未连接")
     if(!d.durationComplete)Text("微信覆盖不完整；以上为已确认部分，不代表完整合计。",color=p.textSecondary,fontSize=12.sp,lineHeight=20.sp)
    }
    d.days.firstOrNull{it.date.toString()==chosen}?.let{day->Spacer(Modifier.height(20.dp));SectionLabel("${day.date} · 当日记录");DayRows(vm,state,day)}
   }
   StatsMetric.DAYS->ReadingCalendarPanel(vm,state,vm.wereadAccountHash(),today,d.span)
   StatsMetric.PHYSICAL->{
    SectionLabel("本期实体书")
    DetailNote("只列所选区间有计时记录或主动完读的实体书；进度显示当前已保存值，不伪造历史页数。")
    Spacer(Modifier.height(12.dp))
    if(!d.includesPhysical)DetailNote("当前来源仅微信读书，未包含实体书。") else if(d.physicalBooks.isEmpty())DetailNote("本区间没有实体书阅读或完读记录。")
    d.physicalBooks.forEach { item->
     SoftCard(Modifier.padding(bottom=12.dp),onClick={vm.go(Route.BookDetail(item.book.id))}) {
      Text(item.book.title,color=p.text,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
      Text(item.book.author,color=p.textSecondary,fontSize=12.sp,modifier=Modifier.padding(top=5.dp))
      Spacer(Modifier.height(14.dp));DetailLine("本期阅读",formatDurationZh(item.durationMs));DetailLine("当前进度",item.book.progressLabel())
      DetailLine("完读",if(item.finishedInSpan)"本期主动完读" else if(item.book.status==BookStatus.FINISHED)"已读完 · 非本期" else "未完读")
     }
    }
   }
   StatsMetric.EBOOK->{
    SectionLabel("微信读书")
    SoftCard {
     if(!d.includesWeread)Text("当前来源仅实体书，未包含电子书。",color=p.textSecondary,fontSize=14.sp)
     else if(!d.wereadConnected)Text("尚未建立完整同步区间，请在我的 → 微信读书中设置固定起点并同步。",color=p.textSecondary,fontSize=14.sp,lineHeight=23.sp)
     else { DetailLine("已确认阅读",formatDurationZh(d.wereadMs));DetailLine("划线",d.highlights?.let{"${it} 条"}?:"未提供");DetailLine("想法",d.thoughts?.let{"${it} 条"}?:"未提供")
      Text(if(d.notesIncomplete)"划线/想法仅为已返回部分，不能推算完整总数。" else "划线和想法按接口实际返回统计。",color=p.textSecondary,fontSize=12.sp,lineHeight=20.sp)
     }
    }
    Spacer(Modifier.height(16.dp));DetailNote("当前官方同步接口未提供本期精确书单与本数。不用书架存量或示例数据冒充电子书阅读本数。")
   }
   StatsMetric.STREAK->{
    SectionLabel("当前连续")
    SoftCard { DetailLine("已确认范围",d.currentRun?.let{"${it.start} 至 ${it.end}"}?:"暂无可确认范围");Text("仅回溯所选区间。末日未知时不判定为零；未知日期中止回溯，不等于未读。",color=p.textSecondary,fontSize=12.sp,lineHeight=21.sp) }
    Spacer(Modifier.height(18.dp));SectionLabel("本期最长连续")
    SoftCard { DetailLine("最长记录",d.longestRun?.let{"${it.days} 天"}?:if(d.unknownDays>0)"尚不能确认" else "0 天");d.longestRun?.let{Text("${it.start} 至 ${it.end}",color=p.primary,fontSize=14.sp)}
     if(d.unknownDays>0)Text("区间仍有 ${d.unknownDays} 个未知日期，最长记录是已确认下限。",color=p.textSecondary,fontSize=12.sp,lineHeight=20.sp)
    }
   }
   StatsMetric.AVERAGE->{
    SectionLabel("自然日日均")
    SoftCard { DetailLine("已确认时长",formatDurationZh(d.knownMs));DetailLine("已过自然日","${d.days.size} 天");Text(if(d.durationComplete)"${formatDurationZh(d.knownMs)} ÷ ${d.days.size} 天。包含未阅读日；仅实体来源时按本机已保存计时记录计算。" else "所选来源覆盖不完整，部分合计不能当作完整分子，因此暂不计算。",color=p.textSecondary,fontSize=13.sp,lineHeight=22.sp) }
    Spacer(Modifier.height(20.dp));SectionLabel("阅读日日均 · 另一种口径")
    SoftCard { DetailLine("已确认阅读日","${d.knownDays} 天");DetailLine("阅读日日均",when(val v=d.readingDayAverageMs){is MetricValue.Known->formatDurationZh(v.value);is MetricValue.Missing->"未提供"});Text("总时长 ÷ 有阅读记录的日期并集；不含未阅读日，因此不等同上方自然日日均。覆盖不全时同样不计算。",color=p.textSecondary,fontSize=13.sp,lineHeight=22.sp) }
   }
  }
 }
}
private fun sourceLabel(s:SourceFilter)=when(s){SourceFilter.ALL->"实体 + 微信";SourceFilter.PHYSICAL->"仅实体书";SourceFilter.WEREAD->"仅微信读书"}
private fun durationPair(ms:Long):Pair<String,String> {val minutes=ms/60000;return if(minutes>=60)String.format(java.util.Locale.ROOT,"%.1f",minutes/60.0) to "小时" else minutes.toString() to "分钟"}
@Composable private fun SectionLabel(text:String){Text(text,color=LocalPalette.current.text,fontSize=17.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(bottom=12.dp))}
@Composable private fun DetailNote(text:String){Text(text,color=LocalPalette.current.textSecondary,fontSize=13.sp,lineHeight=23.sp)}
@Composable private fun DetailLine(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.Top){Text(label,color=LocalPalette.current.textSecondary,fontSize=13.sp,modifier=Modifier.weight(1f));Text(value,color=LocalPalette.current.text,fontSize=14.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1.2f).padding(start=12.dp))}}
@Composable private fun DayRows(vm:AppViewModel,state:AppUiState,day:ReadingDay){
 val p=LocalPalette.current
 SoftCard {
  day.physicalSessions.forEach { session->val book=state.snapshot.books.firstOrNull{it.id==session.bookId};val ms=ReadingMath.splitDurationByDay(session,state.now,Zones.Shanghai)[day.date]?:0L
   Column(Modifier.fillMaxWidth().pressClickable{vm.go(Route.BookDetail(session.bookId))}.padding(vertical=8.dp)){Text(book?.title?:"实体书记录",color=p.text,fontSize=15.sp,fontWeight=FontWeight.Medium);Text("实体 · ${formatDurationZh(ms)}",color=p.textSecondary,fontSize=12.sp)}
  }
  if(state.statsSource!=SourceFilter.PHYSICAL)Text("微信 · "+(day.wereadMs?.let{formatDurationZh(it)}?:"尚无完整日记录"),color=p.textSecondary,fontSize=13.sp,modifier=Modifier.padding(vertical=8.dp))
  if(day.physicalSessions.isEmpty()&&day.wereadMs==null)Text("暂无可展示记录；未知不代表未读。",color=p.textSecondary,fontSize=13.sp)
 }
}
