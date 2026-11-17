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
  val wereadStart = WereadImport.parseDate(settings.wereadImportStartDate)
  val wereadEnd = StatsOverview.parseSyncDate(settings.wereadLastSyncAt)
  val report = StatsOverview.overview(
    sessions = state.snapshot.sessions,
    books = state.snapshot.books,
    wereadDays = state.snapshot.wereadDays,
    period = state.statsPeriod,
    anchor = state.statsAnchor,
    today = today,
    nowEpochMs = state.now,
    source = state.statsSource,
    wereadAccountHash = hash,
    wereadCoverageStart = wereadStart,
    wereadCoverageEnd = wereadEnd,
    zone = Zones.Shanghai,
    calendarMonth = state.calendarMonth,
  )
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
      OverviewCards(report)
      Spacer(Modifier.height(14.dp))
      NotesStrip(report.notes)
      Spacer(Modifier.height(12.dp))
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
internal fun OverviewCards(report: com.scarlett.yuedujilu.domain.OverviewReport) {
  val p=LocalPalette.current
  // Same low-saturation families in all four themes, gently mixed with the active theme.
  val bases=listOf(Color(0xFFDFEDE7),Color(0xFFE5EBF6),Color(0xFFF3E9DD),Color(0xFFE9E4F0))
  val colors=bases.map { androidx.compose.ui.graphics.lerp(it,p.primarySoft,.16f) }
  val cards=listOf(
    CardSpec("阅读天数",AppIcons.Calendar,colors[0],report.days,::unitDays),
    CardSpec("阅读时长",AppIcons.Clock,colors[1],report.durationMs,::unitDuration),
    CardSpec(if((report.booksRead as? MetricValue.Known)?.footnote?.contains("仅实体")==true) "实体阅读本数" else "阅读本数",AppIcons.Shelf,colors[2],report.booksRead,::unitBooks),
    CardSpec(if((report.booksFinished as? MetricValue.Known)?.footnote?.contains("仅实体")==true) "实体读完本数" else "读完本数",AppIcons.Check,colors[3],report.booksFinished,::unitBooks),
    CardSpec("连续阅读",AppIcons.Stats,colors[0],report.streak,::unitDays),
    CardSpec("日均阅读",AppIcons.Clock,colors[1],report.dailyAverageMs,::unitDuration))
  Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
    cards.chunked(2).forEach { row ->
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        row.forEach { MetricCard(it,Modifier.weight(1f)) }
      }
    }
  }
}
private data class CardSpec(val label:String,val icon:ImageVector,val background:Color,val metric:MetricValue,val format:(Long)->Pair<String,String>)

@Composable
private fun MetricCard(spec:CardSpec,modifier:Modifier=Modifier) {
  val p=LocalPalette.current
  val density=androidx.compose.ui.platform.LocalDensity.current
  val labelH=with(density){22.sp.toDp()}
  val valueH=with(density){46.sp.toDp()}
  val noteH=with(density){40.sp.toDp()}
  val (value,unit,foot,compare)=present(spec.metric,spec.format)
  var explain by remember { mutableStateOf(false) }
  Column(modifier.clip(UiShape.Regular).background(spec.background)
    .pressClickable { explain=true }.padding(horizontal=16.dp,vertical=14.dp)
    .then(Modifier.semantics { contentDescription="${spec.label}：$value$unit。${compare?:foot.orEmpty()}" })) {
    Row(Modifier.fillMaxWidth().height(labelH),verticalAlignment=Alignment.CenterVertically) {
      Text(spec.label,color=p.textSecondary,fontSize=13.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f),maxLines=1)
      if(density.fontScale<1.2f) Icon(spec.icon,null,tint=p.primary,modifier=Modifier.size(18.dp))
    }
    androidx.compose.ui.layout.Layout(content={
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      androidx.compose.foundation.text.BasicText(value,style=androidx.compose.ui.text.TextStyle(color=p.text,fontSize=30.sp,fontWeight=FontWeight.SemiBold,
        fontFeatureSettings="tnum",letterSpacing=(-.5).sp),maxLines=1,overflow=TextOverflow.Ellipsis,
        autoSize=androidx.compose.foundation.text.TextAutoSize.StepBased(16.sp,30.sp,1.sp),modifier=Modifier.weight(1f,fill=false).alignByBaseline())
      if(unit.isNotEmpty()) Text(unit,color=p.text,fontSize=12.sp,modifier=Modifier.padding(start=3.dp).alignByBaseline(),maxLines=1)
    }
    },modifier=Modifier.fillMaxWidth().height(valueH)) { measurables,constraints ->
      val placeable=measurables.single().measure(constraints.copy(minHeight=0))
      val baseline=placeable[androidx.compose.ui.layout.FirstBaseline]
      val target=34.sp.roundToPx()
      layout(constraints.maxWidth,constraints.maxHeight,alignmentLines=mapOf(androidx.compose.ui.layout.FirstBaseline to target)) {
        placeable.placeRelative(0,target-baseline)
      }
    }
    Box(Modifier.fillMaxWidth().height(noteH),contentAlignment=Alignment.BottomStart) {
      Text(foot?:compare?:"已确认记录",color=p.textSecondary,fontSize=11.sp,lineHeight=16.sp,minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis)
    }
  }
  if(explain) androidx.compose.material3.AlertDialog(onDismissRequest={explain=false},title={Text(spec.label)},
    text={Text("$value $unit\n\n${foot?:"依据已有记录统计"}${compare?.let{"\n$it"}.orEmpty()}")},
    confirmButton={androidx.compose.material3.TextButton(onClick={explain=false}){Text("知道了")}})
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
