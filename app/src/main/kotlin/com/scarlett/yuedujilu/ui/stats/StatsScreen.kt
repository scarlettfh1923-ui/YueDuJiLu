package com.scarlett.yuedujilu.ui.stats

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
      .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = MainBottomSpace.dp)
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
      PeriodCapsules(
        selected = state.statsPeriod,
        onSelect = vm::setPeriod,
        modifier = Modifier.weight(1f),
      )
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
    PeriodNavigator(
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
      Text("数据总览", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
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
      CalendarBoard(
        month = state.calendarMonth,
        marked = report.markedDays,
        today = today,
        onShift = vm::shiftMonth,
      )
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
      fontSize = if (selected) 22.sp else 18.sp,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
    )
    Spacer(Modifier.height(6.dp))
    Box(
      Modifier
        .width(22.dp)
        .height(3.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(if (selected) p.primary else Color.Transparent),
    )
  }
}

@Composable
private fun PeriodCapsules(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  val shape = RoundedCornerShape(20.dp)
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
          .defaultMinSize(minHeight = 36.dp)
          .clip(RoundedCornerShape(17.dp))
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
private fun OverviewCards(report: com.scarlett.yuedujilu.domain.OverviewReport) {
  val p = LocalPalette.current
  val rows = listOf(
    listOf(
      CardSpec("阅读天数", AppIcons.Stats, p.primary, report.days, ::unitDays),
      CardSpec("阅读时长", AppIcons.Clock, p.success, report.durationMs, ::unitDuration),
    ),
    listOf(
      CardSpec("阅读本数", AppIcons.Shelf, p.primary.copy(alpha = 0.85f), report.booksRead, ::unitBooks),
      CardSpec("读完本数", AppIcons.Check, p.success, report.booksFinished, ::unitBooks),
    ),
    listOf(
      CardSpec("连续阅读", AppIcons.Calendar, p.primary, report.streak, ::unitDays),
      CardSpec("日均阅读时长", AppIcons.Spark, p.success, report.dailyAverageMs, ::unitDuration),
    ),
  )
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    rows.forEach { row ->
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        row.forEach { spec ->
          MetricCard(spec, Modifier.weight(1f))
        }
      }
    }
  }
}

private data class CardSpec(
  val label: String,
  val icon: ImageVector,
  val tint: Color,
  val metric: MetricValue,
  val format: (Long) -> Pair<String, String>,
)

@Composable
private fun MetricCard(spec: CardSpec, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  val shape = RoundedCornerShape(24.dp)
  val (value, unit, foot, compare) = present(spec.metric, spec.format)
  Column(
    modifier
      .clip(shape)
      .background(p.surfaceMuted)
      .defaultMinSize(minHeight = 124.dp)
      .padding(horizontal = 14.dp, vertical = 14.dp)
  ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(spec.label, color = p.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
      Icon(spec.icon, contentDescription = null, tint = spec.tint, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        value,
        color = p.text,
        fontSize = if (value.length > 6) 22.sp else 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
        maxLines = 2,
        lineHeight = 30.sp,
        modifier = Modifier.weight(1f, fill = false),
      )
      if (unit.isNotEmpty()) {
        Spacer(Modifier.width(4.dp))
        Text(unit, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 3.dp))
      }
    }
    Spacer(Modifier.height(8.dp))
    Text(
      compare ?: foot ?: " ",
      color = p.textTertiary,
      fontSize = 11.sp,
      lineHeight = 15.sp,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun NotesStrip(metric: MetricValue) {
  val p = LocalPalette.current
  val shape = RoundedCornerShape(24.dp)
  val (value, unit, foot, compare) = present(metric, ::unitNotes)
  Column(
    Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(p.surfaceMuted)
      .padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("新增笔记", color = p.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
      Icon(AppIcons.Edit, contentDescription = null, tint = p.primary, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(value, color = p.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
      if (unit.isNotEmpty()) {
        Spacer(Modifier.width(4.dp))
        Text(unit, color = p.text, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
      }
    }
    Spacer(Modifier.height(6.dp))
    Text(compare ?: foot ?: "口径为微信划线与想法，书签无逐条日期，不计入。", color = p.textTertiary, fontSize = 11.sp, lineHeight = 16.sp)
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
private fun unitDuration(ms: Long): Pair<String, String> {
  val totalMin = (ms / 60_000L).coerceAtLeast(0L)
  val h = totalMin / 60
  val m = totalMin % 60
  return when {
    h > 0 && m > 0 -> "${h}小时${m}分" to ""
    h > 0 -> h.toString() to "小时"
    else -> m.toString() to "分"
  }
}

@Composable
private fun CalendarBoard(
  month: YearMonth,
  marked: Set<LocalDate>,
  today: LocalDate,
  onShift: (Long) -> Unit,
) {
  val p = LocalPalette.current
  SoftCard(elevated = true, radius = 24) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Box(
        Modifier
          .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
          .pressClickable { onShift(-1) },
        contentAlignment = Alignment.Center,
      ) {
        Icon(AppIcons.ChevronLeft, "上个月", tint = p.text, modifier = Modifier.size(22.dp))
      }
      Text(yearMonthLabel(month), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
      val canFwd = month.isBefore(YearMonth.from(today))
      Box(
        Modifier
          .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
          .pressClickable(enabled = canFwd) { onShift(1) },
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          AppIcons.ChevronRight,
          "下个月",
          tint = if (month.isBefore(YearMonth.from(today))) p.text else p.textTertiary.copy(alpha = 0.4f),
          modifier = Modifier.size(22.dp),
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
      listOf("一", "二", "三", "四", "五", "六", "日").forEach {
        Text(it, color = p.textTertiary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
      }
    }
    Spacer(Modifier.height(8.dp))
    val first = month.atDay(1)
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    var day = start
    repeat(6) {
      Row(Modifier.fillMaxWidth()) {
        repeat(7) {
          val inMonth = YearMonth.from(day) == month
          val has = marked.contains(day)
          val isToday = day == today
          Box(
            Modifier
              .weight(1f)
              .aspectRatio(1f)
              .padding(3.dp)
              .clip(CircleShape)
              .background(
                when {
                  has && inMonth -> p.primary
                  isToday && inMonth -> p.primarySoft
                  else -> Color.Transparent
                }
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              day.dayOfMonth.toString(),
              color = when {
                !inMonth -> p.textTertiary.copy(alpha = 0.4f)
                has -> p.onPrimary
                else -> p.text
              },
              fontSize = 13.sp,
              fontWeight = if (has || isToday) FontWeight.SemiBold else FontWeight.Normal,
            )
          }
          day = day.plusDays(1)
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "有阅读记录的日期会以主题色点出。实体与微信按自然日合并；微信缺失的日期不会标成已读。时长按上海时区自然日切开。",
      color = p.textSecondary,
      fontSize = 12.sp,
      lineHeight = 18.sp,
    )
  }
}
