package com.scarlett.yuedujilu.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.StatsMode
import com.scarlett.yuedujilu.domain.ReadingMath
import com.scarlett.yuedujilu.domain.StatsPeriod
import com.scarlett.yuedujilu.domain.Zones
import com.scarlett.yuedujilu.ui.MainBottomSpace
import com.scarlett.yuedujilu.ui.common.FilterChip
import com.scarlett.yuedujilu.ui.common.IconButtonCircle
import com.scarlett.yuedujilu.ui.common.SoftCard
import com.scarlett.yuedujilu.ui.common.formatDurationZh
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
  val stats = ReadingMath.aggregate(
    state.snapshot.sessions,
    state.snapshot.books,
    state.statsPeriod,
    state.now,
    Zones.Shanghai,
  )
  val today = LocalDate.now(Zones.Shanghai)
  val monthLabel = when (state.statsPeriod) {
    StatsPeriod.MONTH -> yearMonthLabel(YearMonth.from(today))
    StatsPeriod.WEEK -> "本周"
    StatsPeriod.YEAR -> "${today.year}年"
    StatsPeriod.ALL -> "全部时间"
  }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = MainBottomSpace.dp)
  ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      ModeToggle(state.statsMode, vm::setStatsMode, Modifier.weight(1f))
      IconButtonCircle(AppIcons.Sync, "同步", onClick = {
        vm.toast("微信读书尚未接入，没有可同步的数据")
      })
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
      FilterChip("周", state.statsPeriod == StatsPeriod.WEEK) { vm.setPeriod(StatsPeriod.WEEK) }
      FilterChip("月", state.statsPeriod == StatsPeriod.MONTH) { vm.setPeriod(StatsPeriod.MONTH) }
      FilterChip("年", state.statsPeriod == StatsPeriod.YEAR) { vm.setPeriod(StatsPeriod.YEAR) }
      FilterChip("全部", state.statsPeriod == StatsPeriod.ALL) { vm.setPeriod(StatsPeriod.ALL) }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(monthLabel, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
      Spacer(Modifier.width(10.dp))
      Text("全部来源", color = p.textSecondary, fontSize = 13.sp)
    }
    Spacer(Modifier.height(16.dp))
    if (state.statsMode == StatsMode.OVERVIEW) {
      SoftCard(elevated = true) {
        Text("阅读时长", color = p.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(
          formatDurationZh(stats.durationMs),
          color = p.text,
          fontSize = 44.sp,
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Medium,
          lineHeight = 52.sp,
        )
        if (state.snapshot.sessions.isEmpty()) {
          Spacer(Modifier.height(8.dp))
          Text("还没有阅读记录。在书架里开始一次计时后，时长会显示在这里。", color = p.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        }
      }
      Spacer(Modifier.height(16.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCell("阅读天数", "${stats.readingDays}天", Modifier.weight(1f))
        MetricCell("阅读本数", "${stats.booksRead}本", Modifier.weight(1f))
        MetricCell("读完本数", "${stats.booksFinished}本", Modifier.weight(1f))
      }
      Spacer(Modifier.height(16.dp))
      SoftCard {
        Text("微信读书 · 新增笔记", color = p.textSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("暂无数据", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text("来源尚未接入，不会显示占位数字。", color = p.textTertiary, fontSize = 12.sp)
      }
    } else {
      val ym = state.calendarMonth
      val marked = ReadingMath.readingDaysInMonth(state.snapshot.sessions, ym, state.now, Zones.Shanghai)
      SoftCard(elevated = true) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Icon(AppIcons.ChevronLeft, "上个月", tint = p.text, modifier = Modifier
            .size(28.dp)
            .pressClickable { vm.shiftMonth(-1) })
          Text(yearMonthLabel(ym), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
          Icon(AppIcons.ChevronRight, "下个月", tint = p.text, modifier = Modifier
            .size(28.dp)
            .pressClickable { vm.shiftMonth(1) })
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
          listOf("一", "二", "三", "四", "五", "六", "日").forEach {
            Text(it, color = p.textTertiary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
          }
        }
        Spacer(Modifier.height(8.dp))
        val first = ym.atDay(1)
        val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var day = start
        repeat(6) {
          Row(Modifier.fillMaxWidth()) {
            repeat(7) {
              val inMonth = YearMonth.from(day) == ym
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
                      else -> androidx.compose.ui.graphics.Color.Transparent
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
        Text("有阅读记录的日期会以主题色点出。时长按上海时区自然日切开。", color = p.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
      }
    }
  }
}

@Composable
private fun ModeToggle(mode: StatsMode, onChange: (StatsMode) -> Unit, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  val shape = RoundedCornerShape(18.dp)
  Row(
    modifier
      .clip(shape)
      .background(p.surface)
      .padding(4.dp)
  ) {
    TogglePill("概览", mode == StatsMode.OVERVIEW) { onChange(StatsMode.OVERVIEW) }
    TogglePill("日历", mode == StatsMode.CALENDAR) { onChange(StatsMode.CALENDAR) }
  }
}

@Composable
private fun TogglePill(text: String, selected: Boolean, onClick: () -> Unit) {
  val p = LocalPalette.current
  Box(
    Modifier
      .clip(RoundedCornerShape(14.dp))
      .background(if (selected) p.primary else androidx.compose.ui.graphics.Color.Transparent)
      .pressClickable(onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 8.dp)
  ) {
    Text(text, color = if (selected) p.onPrimary else p.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  Column(
    modifier
      .clip(RoundedCornerShape(20.dp))
      .background(p.surface)
      .padding(14.dp)
  ) {
    Text(label, color = p.textSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    Text(value, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
  }
}
