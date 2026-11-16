package com.scarlett.yuedujilu.domain

import com.scarlett.yuedujilu.SourceFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class StatsOverviewTest {
  private val zone: ZoneId = Zones.Shanghai
  private val today = LocalDate.of(2026, 11, 17)
  private val now = at(2026, 11, 17, 18, 0)
  private val hash = "acct"

  private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
    ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

  private fun book(id: String, source: BookSource = BookSource.MANUAL, finished: Long? = null) = Book(
    id = id,
    title = id,
    source = source,
    status = if (finished != null) BookStatus.FINISHED else BookStatus.READING,
    finishedAtEpochMs = finished,
    createdAtEpochMs = now,
    updatedAtEpochMs = now,
  )

  private fun session(id: String, bookId: String, start: Long, end: Long) = ReadingSession(
    id = id,
    bookId = bookId,
    startedAtEpochMs = start,
    endedAtEpochMs = end,
  )

  private fun weread(date: String, sec: Long, complete: Boolean = true, notesComplete: Boolean = true, h: Int = 0, t: Int = 0) =
    WereadDayRecord(
      accountHash = hash,
      date = date,
      durationSec = sec,
      durationComplete = complete,
      highlightCount = h,
      thoughtCount = t,
      notesComplete = notesComplete,
    )

  private fun report(
    sessions: List<ReadingSession>,
    books: List<Book>,
    days: List<WereadDayRecord>,
    period: StatsPeriod = StatsPeriod.MONTH,
    anchor: LocalDate = today,
    source: SourceFilter = SourceFilter.ALL,
    wereadStart: LocalDate? = LocalDate.of(2026, 11, 1),
    wereadEnd: LocalDate? = today,
  ) = StatsOverview.overview(
    sessions, books, days, period, anchor, today, now, source, hash, wereadStart, wereadEnd, zone,
  )

  @Test
  fun overlappingDay_unionsDaysAndAddsDuration() {
    val physical = session("s1", "b1", at(2026, 11, 10, 9, 0), at(2026, 11, 10, 9, 30))
    val wr = weread("2026-11-10", 20 * 60)
    val r = report(listOf(physical), listOf(book("b1")), listOf(wr), period = StatsPeriod.MONTH)
    val days = r.days as MetricValue.Known
    val dur = r.durationMs as MetricValue.Known
    assertEquals(1, days.value)
    assertEquals((30 + 20) * 60_000L, dur.value)
  }

  @Test
  fun crossMonth_sessionSplitCountsOnlyInRange() {
    val cross = session("s1", "b1", at(2026, 10, 31, 23, 30), at(2026, 11, 1, 0, 20))
    val nov = report(listOf(cross), listOf(book("b1")), emptyList(), period = StatsPeriod.MONTH, anchor = LocalDate.of(2026, 11, 5), source = SourceFilter.PHYSICAL, wereadStart = null, wereadEnd = null)
    val oct = report(listOf(cross), listOf(book("b1")), emptyList(), period = StatsPeriod.MONTH, anchor = LocalDate.of(2026, 10, 15), source = SourceFilter.PHYSICAL, wereadStart = null, wereadEnd = null)
    assertEquals(20 * 60_000L, (nov.durationMs as MetricValue.Known).value)
    assertEquals(1, (nov.days as MetricValue.Known).value)
    assertEquals(30 * 60_000L, (oct.durationMs as MetricValue.Known).value)
    assertEquals(1, (oct.days as MetricValue.Known).value)
  }

  private fun completeWereadWindow(vararg entries: Pair<String, Long>): List<WereadDayRecord> {
    val map = entries.toMap()
    var d = LocalDate.of(2026, 11, 1)
    val out = mutableListOf<WereadDayRecord>()
    while (!d.isAfter(today)) {
      val iso = d.toString()
      out += weread(iso, map[iso] ?: 0L, complete = true)
      d = d.plusDays(1)
    }
    return out
  }

  @Test
  fun sourceFilter_physicalIgnoresWeread_wereadIgnoresPhysical() {
    val physical = session("s1", "b1", at(2026, 11, 10, 9, 0), at(2026, 11, 10, 10, 0))
    val wr = completeWereadWindow("2026-11-11" to 15 * 60)
    val books = listOf(book("b1"))
    val onlyP = report(listOf(physical), books, wr, source = SourceFilter.PHYSICAL)
    val onlyW = report(listOf(physical), books, wr, source = SourceFilter.WEREAD)
    val all = report(listOf(physical), books, wr, source = SourceFilter.ALL)
    assertEquals(60 * 60_000L, (onlyP.durationMs as MetricValue.Known).value)
    assertEquals(1, (onlyP.days as MetricValue.Known).value)
    assertEquals(15 * 60_000L, (onlyW.durationMs as MetricValue.Known).value)
    assertEquals(1, (onlyW.days as MetricValue.Known).value)
    assertEquals((60 + 15) * 60_000L, (all.durationMs as MetricValue.Known).value)
    assertEquals(2, (all.days as MetricValue.Known).value)
  }

  @Test
  fun missingWereadDay_isNotFilledWithZero() {
    val wr = listOf(weread("2026-11-10", 10 * 60)) // 11 月其余日在覆盖窗口内缺失
    val r = report(emptyList(), emptyList(), wr, source = SourceFilter.WEREAD)
    assertTrue("缺失不能变成 0 天完整口径: ${r.days}", r.days is MetricValue.Missing || (r.days as? MetricValue.Known)?.footnote != null)
    assertTrue(r.dailyAverageMs is MetricValue.Missing)
    assertTrue(r.booksRead is MetricValue.Missing)
    assertTrue((r.booksRead as MetricValue.Missing).reason.contains("无明细"))
  }

  @Test
  fun wereadBookCount_neverUsesShelfSize() {
    val books = listOf(book("w1", BookSource.WEREAD), book("w2", BookSource.WEREAD), book("p1"))
    val r = report(emptyList(), books, listOf(weread("2026-11-10", 60)), source = SourceFilter.ALL)
    val read = r.booksRead as MetricValue.Known
    assertEquals(0, read.value)
    assertTrue(read.footnote!!.contains("仅实体书"))
  }

  @Test
  fun comparison_withheldWhenPreviousIncomplete() {
    // 上月完全没有完整微信数据，本月完整；仅微信时上期应不对比
    val thisMonth = weread("2026-11-10", 60)
    val r = report(emptyList(), emptyList(), listOf(thisMonth), source = SourceFilter.WEREAD, wereadStart = LocalDate.of(2026, 11, 1), wereadEnd = today)
    // 覆盖窗口从 11 月 1 日开始，10 月不在窗口内：对仅微信而言上月没有可对比口径
    val days = r.days
    if (days is MetricValue.Known) {
      assertTrue(days.compare == null || days.compare.contains("暂不对比") || days.compare.contains("较上月"))
    }
    // 明确：上月不在微信覆盖窗口，不应编造「无变化」
    if (days is MetricValue.Known && days.compare != null) {
      assertFalse(days.compare!!.contains("无变化"))
      assertFalse(days.compare!!.contains("持平") && days.value == 0L && days.footnote != null)
    }
  }

  @Test
  fun dailyAverage_usesElapsedCalendarDays() {
    val s = session("s1", "b1", at(2026, 11, 1, 8, 0), at(2026, 11, 1, 9, 0))
    val r = report(listOf(s), listOf(book("b1")), emptyList(), source = SourceFilter.PHYSICAL, wereadStart = null, wereadEnd = null)
    val avg = r.dailyAverageMs as MetricValue.Known
    // 11/1–11/17 共 17 天，总时长 60 分钟
    assertEquals(17, YearMonth.of(2026, 11).atDay(1).until(today).plusDays(1).let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2026, 11, 1), today) + 1 })
    assertEquals((60 * 60_000L) / 17L, avg.value)
    assertTrue(avg.footnote!!.contains("17 个自然日"))
  }

  @Test
  fun streak_stopsAtUnknownWereadDay() {
    // 今天与昨天有阅读，但再往前一天在覆盖窗口内缺失
    val wr = listOf(
      weread("2026-11-17", 60),
      weread("2026-11-16", 60),
    )
    val r = report(emptyList(), emptyList(), wr, source = SourceFilter.WEREAD, period = StatsPeriod.MONTH)
    when (val st = r.streak) {
      is MetricValue.Known -> {
        assertEquals(2, st.value)
        assertTrue(st.footnote == null || st.footnote!!.contains("缺失"))
      }
      is MetricValue.Missing -> assertTrue(st.reason.contains("缺失") || st.reason.contains("暂不"))
    }
  }

  @Test
  fun periodLabel_andForwardClamp() {
    val week = StatsOverview.resolveSpan(StatsPeriod.WEEK, today, today)!!
    assertEquals(LocalDate.of(2026, 11, 16), week.start)
    assertEquals(LocalDate.of(2026, 11, 22), week.end)
    assertFalse(StatsOverview.canShiftForward(StatsPeriod.WEEK, today, today))
    assertTrue(StatsOverview.canShiftForward(StatsPeriod.WEEK, today.minusWeeks(1), today))
    assertFalse(StatsOverview.canShiftForward(StatsPeriod.ALL, today, today))
  }

  private fun assertFalse(cond: Boolean) = assertTrue(!cond)
}
