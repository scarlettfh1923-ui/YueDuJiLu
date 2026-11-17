package com.scarlett.yuedujilu.ui.stats

import com.scarlett.yuedujilu.ui.theme.UiShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.Route
import com.scarlett.yuedujilu.SourceFilter
import com.scarlett.yuedujilu.StatsMode
import com.scarlett.yuedujilu.data.todayInShanghai
import com.scarlett.yuedujilu.domain.MetricValue
import com.scarlett.yuedujilu.domain.StatsOverview
import com.scarlett.yuedujilu.domain.StatsPeriod
import com.scarlett.yuedujilu.domain.WereadImport
import com.scarlett.yuedujilu.domain.Zones
import com.scarlett.yuedujilu.ui.MainBottomSpace
import com.scarlett.yuedujilu.ui.common.IconButtonCircle
import com.scarlett.yuedujilu.ui.common.SoftCard
import com.scarlett.yuedujilu.ui.common.SourceSelector
import com.scarlett.yuedujilu.ui.common.yearMonthLabel
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import com.scarlett.yuedujilu.ui.theme.pressClickable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

@Composable
fun StatsScreen(vm: AppViewModel, state: AppUiState) {
  val p = LocalPalette.current
  val today = todayInShanghai(state.now)
  val settings = state.snapshot.settings
  val hash = vm.wereadAccountHash()
  val bundle=rememberStatsBundle(state,hash)
  val report=bundle.report
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = MainBottomSpace.dp)
  ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      OverviewTabs(state.statsMode, vm::setStatsMode, Modifier.weight(1f))
      IconButtonCircle(AppIcons.Sync, if (state.wereadBusy) "取消同步" else "刷新同步", onClick = {
        if (state.hasWereadKey && settings.wereadImportStartDate != null) {
          if (state.wereadBusy) vm.cancelWereadSync() else vm.syncWeread()
        } else {
          vm.go(Route.Weread)
        }
      })
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      if(state.statsMode==StatsMode.OVERVIEW) PeriodCapsules(
        selected = state.statsPeriod,
        onSelect = vm::setPeriod,
        modifier = Modifier.weight(1f),
      )
      else Text("阅读足迹",color=p.textSecondary,fontSize=13.sp,modifier=Modifier.weight(1f))
      Spacer(Modifier.width(4.dp))
      SourceSelector(
        label = when (state.statsSource) {
          SourceFilter.ALL -> "全部来源"
          SourceFilter.PHYSICAL -> "仅实体书"
          SourceFilter.WEREAD -> "仅微信"
        },
        options = listOf(
          "全部（实体+微信）" to (state.statsSource == SourceFilter.ALL),
          "仅实体书" to (state.statsSource == SourceFilter.PHYSICAL),
          "仅微信读书" to (state.statsSource == SourceFilter.WEREAD),
        ),
        onSelect = { index ->
          vm.setStatsSource(
            when (index) {
              1 -> SourceFilter.PHYSICAL
              2 -> SourceFilter.WEREAD
              else -> SourceFilter.ALL
            },
          )
        },
      )
    }
    Spacer(Modifier.height(8.dp))
    if(state.statsMode==StatsMode.OVERVIEW) PeriodNavigator(
      period = state.statsPeriod,
      label = report.periodLabel,
      canGoForward = report.canGoForward,
      today = today,
      anchor = state.statsAnchor,
      onShift = vm::shiftStatsAnchor,
      onPick = vm::setStatsAnchor,
    )
    Spacer(Modifier.height(18.dp))
    if (state.statsMode == StatsMode.OVERVIEW) {
      Text(when(state.statsSource){SourceFilter.ALL->"实体 + 微信 · 已确认记录";SourceFilter.PHYSICAL->"实体书 · 本机记录";SourceFilter.WEREAD->"微信读书 · 同步记录"}, color=p.textSecondary,fontSize=12.sp)
      if (state.wereadBusy) {
        Spacer(Modifier.height(6.dp))
        Text(state.wereadProgress ?: "正在同步微信读书…", color = p.textSecondary, fontSize = 12.sp)
      }
      Spacer(Modifier.height(12.dp))
      OverviewCards(report,onSelect={vm.go(Route.StatsDetail(it))},source=state.statsSource)
      Spacer(Modifier.height(14.dp))
      Text("电子书仅含微信读书，精确本数暂未提供。轻点卡片查看真实明细与计算口径。",color=p.textSecondary,fontSize=12.sp,lineHeight=21.sp)
      Spacer(Modifier.height(10.dp))
      Text(report.coverageNote, color = p.textTertiary, fontSize = 12.sp, lineHeight = 18.sp)
      settings.wereadLastMessage?.let {
        Spacer(Modifier.height(6.dp))
        Text(it, color = p.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
      }
    } else {
      ReadingCalendarPanel(vm,state,hash,today)

    }
  }
}

@Composable
private fun OverviewTabs(mode: StatsMode, onChange: (StatsMode) -> Unit, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  Row(modifier, verticalAlignment = Alignment.Bottom) {
    OverviewTab("概览", mode == StatsMode.OVERVIEW) { onChange(StatsMode.OVERVIEW) }
    Spacer(Modifier.width(20.dp))
    OverviewTab("日历", mode == StatsMode.CALENDAR) { onChange(StatsMode.CALENDAR) }
  }
}

@Composable
private fun OverviewTab(text: String, selected: Boolean, onClick: () -> Unit) {
  val p = LocalPalette.current
  Column(
    Modifier
      .defaultMinSize(minHeight = 48.dp)
      .pressClickable(onClick = onClick)
      .padding(top = 8.dp, end = 4.dp),
  ) {
    Text(
      text,
      color = if (selected) p.text else p.textTertiary,
      fontSize = 22.sp,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
    )
    Spacer(Modifier.height(6.dp))
    Box(
      Modifier
        .width(22.dp)
        .height(3.dp)
        .clip(UiShape.Capsule)
        .background(if (selected) p.primary else Color.Transparent),
    )
  }
}

@Composable
private fun PeriodCapsules(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  val shape = UiShape.Capsule
  Row(
    modifier
      .clip(shape)
      .background(p.surfaceMuted)
      .padding(3.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    listOf(
      StatsPeriod.WEEK to "周",
      StatsPeriod.MONTH to "月",
      StatsPeriod.YEAR to "年",
      StatsPeriod.ALL to "全部",
    ).forEach { (period, label) ->
      val on = selected == period
      Box(
        Modifier
          .weight(1f)
          .defaultMinSize(minHeight = 42.dp)
          .clip(UiShape.Capsule)
          .background(if (on) p.surfaceRaised else Color.Transparent)
          .pressClickable { onSelect(period) },
        contentAlignment = Alignment.Center,
      ) {
        Text(
          label,
          color = if (on) p.text else p.textSecondary,
          fontSize = 13.sp,
          fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
        )
      }
    }
  }
}

@Composable
private fun PeriodNavigator(
  period: StatsPeriod,
  label: String,
  canGoForward: Boolean,
  today: LocalDate,
  anchor: LocalDate,
  onShift: (Long) -> Unit,
  onPick: (LocalDate) -> Unit,
) {
  val p = LocalPalette.current
  if (period == StatsPeriod.ALL) {
    Text(label, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    return
  }
  var open by remember { mutableStateOf(false) }
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .pressClickable { onShift(-1) },
      contentAlignment = Alignment.Center,
    ) {
      Icon(AppIcons.ChevronLeft, "上一区间", tint = p.text, modifier = Modifier.size(22.dp))
    }
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
      Row(
        Modifier
          .defaultMinSize(minHeight = 48.dp)
          .pressClickable { open = true }
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(label, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(4.dp))
        Icon(AppIcons.ChevronDown, "选择区间", tint = p.textSecondary, modifier = Modifier.size(16.dp))
      }
      DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = p.surfaceRaised) {
        periodChoices(period, today, anchor).forEach { (itemLabel, date) ->
          DropdownMenuItem(
            text = { Text(itemLabel, color = p.text, fontSize = 14.sp) },
            onClick = {
              open = false
              onPick(date)
            },
          )
        }
      }
    }
    Box(
      Modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .pressClickable(enabled = canGoForward) { onShift(1) },
      contentAlignment = Alignment.Center,
    ) {
      Icon(AppIcons.ChevronRight, "下一区间", tint = if (canGoForward) p.text else p.textTertiary.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
    }
  }
}

private fun periodChoices(period: StatsPeriod, today: LocalDate, anchor: LocalDate): List<Pair<String, LocalDate>> {
  return when (period) {
    StatsPeriod.MONTH -> (0..17).map { YearMonth.from(today).minusMonths(it.toLong()).atDay(1) }.map {
      StatsOverview.periodLabel(period, StatsOverview.resolveSpan(period, it, today), today) to it
    }
    StatsPeriod.YEAR -> (0..7).map { LocalDate.of(today.year - it, 1, 1) }.map {
      StatsOverview.periodLabel(period, StatsOverview.resolveSpan(period, it, today), today) to it
    }
    StatsPeriod.WEEK -> (0..15).map { today.minusWeeks(it.toLong()) }.map {
      StatsOverview.periodLabel(period, StatsOverview.resolveSpan(period, it, today), today) to it
    }
    StatsPeriod.ALL -> emptyList()
  }
}

@Composable
internal fun OverviewCards(report:com.scarlett.yuedujilu.domain.OverviewReport,onSelect:(com.scarlett.yuedujilu.domain.StatsMetric)->Unit={},source:SourceFilter=SourceFilter.ALL) {
  val p=LocalPalette.current
  val bases=listOf(Color(0xFFDAEAE1),Color(0xFFDFE8F2),Color(0xFFF0E8DA),Color(0xFFE8E3F0),Color(0xFFE2EDE7),Color(0xFFE5ECF3))
  val colors=bases.map { if(p.id==com.scarlett.yuedujilu.domain.ThemeId.PINE_GREEN)it else androidx.compose.ui.graphics.lerp(it,p.primarySoft,.22f) }
  val metrics=com.scarlett.yuedujilu.domain.StatsMetric.entries
  val values=listOf(report.durationMs,report.days,report.booksRead,
    MetricValue.Missing(if(source==SourceFilter.PHYSICAL)"当前筛选不含电子书" else "微信读书未提供本期精确书单/本数"),report.streak,report.dailyAverageMs)
  Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
    metrics.chunked(2).forEach { row->
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        row.forEach { metric-> MetricCard(metric,values[metric.ordinal],colors[metric.ordinal],Modifier.weight(1f)){onSelect(metric)} }
      }
    }
  }
}
@Composable
private fun MetricCard(metric:com.scarlett.yuedujilu.domain.StatsMetric,value:MetricValue,background:Color,modifier:Modifier=Modifier,onClick:()->Unit) {
  val p=LocalPalette.current;val density=androidx.compose.ui.platform.LocalDensity.current
  val format=when(metric){com.scarlett.yuedujilu.domain.StatsMetric.DURATION,com.scarlett.yuedujilu.domain.StatsMetric.AVERAGE->::unitDuration;com.scarlett.yuedujilu.domain.StatsMetric.PHYSICAL,com.scarlett.yuedujilu.domain.StatsMetric.EBOOK->::unitBooks;else->::unitDays}
  val(v,u,foot,compare)=present(value,format)
  Box(modifier.height((144f+100f*(density.fontScale.coerceAtLeast(1f)-1)).dp).clip(UiShape.Regular).background(background)
    .pressClickable(onClick=onClick).then(Modifier.semantics {contentDescription="${metric.label}：$v$u"})
    .testTag("metric-${metric.name}").padding(18.dp)) {
    Column(Modifier.fillMaxSize()) {
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(metric.label,color=p.text,fontSize=14.sp,lineHeight=21.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f),maxLines=1)
        Icon(androidx.compose.ui.res.painterResource(statsIcon(metric)),null,tint=p.primary.copy(alpha=.8f),modifier=Modifier.size(22.dp))
      }
      Spacer(Modifier.weight(1f))
      MetricNumber(if(value is MetricValue.Known)compactCardNumber(v) else v,u,if(value is MetricValue.Missing)26 else 36)
    }
    Icon(AppIcons.ChevronRight,null,tint=p.primary.copy(alpha=.55f),modifier=Modifier.align(Alignment.BottomEnd).size(15.dp))
  }
}
/** Very large stress/long-lived totals use a readable compact card value; full value stays in semantics/detail. */
internal fun compactCardNumber(text:String):String {
 val n=text.toDoubleOrNull()?:return text
 val (divisor,suffix)=when { n>=100_000_000->100_000_000.0 to "亿";n>=10_000->10_000.0 to "万";else->return text }
 return java.math.BigDecimal.valueOf(n/divisor).setScale(1,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()+suffix
}
@Composable
internal fun MetricNumber(value:String,unit:String,size:Int=36,modifier:Modifier=Modifier) {
 val p=LocalPalette.current;val density=androidx.compose.ui.platform.LocalDensity.current
 androidx.compose.ui.layout.Layout(content={
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   androidx.compose.foundation.text.BasicText(value,style=androidx.compose.ui.text.TextStyle(color=p.text,fontSize=size.sp,fontWeight=FontWeight.Medium,fontFeatureSettings="tnum",letterSpacing=(-.5).sp),
    maxLines=1,overflow=TextOverflow.Ellipsis,autoSize=androidx.compose.foundation.text.TextAutoSize.StepBased(16.sp,size.sp,1.sp),modifier=Modifier.weight(1f,fill=false).alignByBaseline())
   if(unit.isNotEmpty())Text(unit,color=p.text,fontSize=12.sp,maxLines=1,modifier=Modifier.padding(start=3.dp,end=10.dp).alignByBaseline())
  }
 },modifier=modifier.fillMaxWidth().height(with(density){52.sp.toDp()})) { measurables,c->
  val child=measurables.single().measure(c.copy(minHeight=0));val baseline=child[androidx.compose.ui.layout.FirstBaseline];val target=38.sp.roundToPx()
  layout(c.maxWidth,c.maxHeight,alignmentLines=mapOf(androidx.compose.ui.layout.FirstBaseline to target)){child.placeRelative(0,target-baseline)}
 }
}
internal fun statsIcon(metric:com.scarlett.yuedujilu.domain.StatsMetric):Int=when(metric) {
 com.scarlett.yuedujilu.domain.StatsMetric.DURATION->com.scarlett.yuedujilu.R.drawable.stat_clock
 com.scarlett.yuedujilu.domain.StatsMetric.DAYS->com.scarlett.yuedujilu.R.drawable.stat_calendar
 com.scarlett.yuedujilu.domain.StatsMetric.PHYSICAL->com.scarlett.yuedujilu.R.drawable.stat_physical_book
 com.scarlett.yuedujilu.domain.StatsMetric.EBOOK->com.scarlett.yuedujilu.R.drawable.stat_e_reader
 com.scarlett.yuedujilu.domain.StatsMetric.STREAK->com.scarlett.yuedujilu.R.drawable.stat_date_chain
 com.scarlett.yuedujilu.domain.StatsMetric.AVERAGE->com.scarlett.yuedujilu.R.drawable.stat_average_clock
}
@Composable
private fun NotesStrip(metric:MetricValue) {
  val p=LocalPalette.current
  val (value,unit,foot,compare)=present(metric,::unitNotes)
  Row(Modifier.fillMaxWidth().clip(UiShape.Regular).background(p.surface).padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
    Icon(AppIcons.Edit,null,tint=p.primary,modifier=Modifier.size(23.dp))
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
      Text("新增笔记",color=p.textSecondary,fontSize=12.sp)
      Row(verticalAlignment=Alignment.CenterVertically) {
        Text(value,color=p.text,fontSize=24.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.alignByBaseline())
        Text(unit,color=p.text,fontSize=14.sp,modifier=Modifier.alignByBaseline())
      }
      if(foot!=null) Text(foot,color=p.textSecondary,fontSize=11.sp,lineHeight=16.sp)
    }
  }
}

private data class Presented(val value: String, val unit: String, val foot: String?, val compare: String?)

private fun present(metric: MetricValue, format: (Long) -> Pair<String, String>): Presented {
  return when (metric) {
    is MetricValue.Known -> {
      val (v, u) = format(metric.value)
      Presented(v, u, metric.footnote, metric.compare)
    }
    is MetricValue.Missing -> Presented("未提供", "", metric.reason, null)
  }
}

private fun unitDays(v: Long) = v.toString() to "天"
private fun unitBooks(v: Long) = v.toString() to "本"
private fun unitNotes(v: Long) = v.toString() to "条"
private fun unitDuration(ms: Long): Pair<String,String> = if(ms>=3_600_000L) {
  String.format(java.util.Locale.ROOT,"%.1f",ms/3_600_000.0) to "小时"
} else (ms/60_000L).coerceAtLeast(0L).toString() to "分"
