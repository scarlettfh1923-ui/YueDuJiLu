package com.scarlett.yuedujilu.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

object Zones {
  val Shanghai: ZoneId = ZoneId.of("Asia/Shanghai")
}

object ReadingMath {
  /** 有效时长 = 结束-开始-已结束的暂停；进行中会话用 now。未结束的暂停算到 now。 */
  fun effectiveDurationMs(session: ReadingSession, nowEpochMs: Long): Long {
    val end = session.endedAtEpochMs ?: nowEpochMs
    if (end <= session.startedAtEpochMs) return 0L
    var pause = 0L
    for (p in session.pauses) {
      val resume = p.resumedAtEpochMs ?: end
      pause += (resume - p.pausedAtEpochMs).coerceAtLeast(0L)
    }
    return (end - session.startedAtEpochMs - pause).coerceAtLeast(0L)
  }

  fun isPaused(session: ReadingSession): Boolean {
    if (session.endedAtEpochMs != null) return false
    val last = session.pauses.lastOrNull() ?: return false
    return last.resumedAtEpochMs == null
  }

  fun pagesDelta(startPage: Int?, endPage: Int?): Int {
    if (startPage == null || endPage == null) return 0
    return (endPage - startPage).coerceAtLeast(0)
  }

  fun markFinished(book: Book, atEpochMs: Long, keepPage: Int = book.currentPage): Book {
    return book.copy(
      status = BookStatus.FINISHED,
      currentPage = keepPage,
      finishedAtEpochMs = atEpochMs,
      updatedAtEpochMs = atEpochMs,
    )
  }

  fun undoFinish(book: Book, atEpochMs: Long): Book {
    val status = if (book.currentPage > 0) BookStatus.READING else BookStatus.UNREAD
    return book.copy(
      status = status,
      finishedAtEpochMs = null,
      updatedAtEpochMs = atEpochMs,
    )
  }

  fun withFinishDate(book: Book, finishedAtEpochMs: Long, atEpochMs: Long): Book {
    require(book.status == BookStatus.FINISHED)
    return book.copy(finishedAtEpochMs = finishedAtEpochMs, updatedAtEpochMs = atEpochMs)
  }

  /**
   * @param allowDecrease 仅用于结束当前计时或修改「时间上最后一条」记录；
   * 补录历史记录时应为 false，避免把已到达的真实进度重置回去。
   */
  fun applySessionProgress(
    book: Book,
    endPage: Int?,
    atEpochMs: Long,
    markFinished: Boolean,
    allowDecrease: Boolean = false,
  ): Book {
    val page = when {
      endPage == null -> book.currentPage
      allowDecrease -> endPage
      else -> maxOf(book.currentPage, endPage)
    }
    return book.copy(
      currentPage = page,
      status = when {
        markFinished -> BookStatus.FINISHED
        page > 0 && book.status != BookStatus.FINISHED -> BookStatus.READING
        else -> book.status
      },
      finishedAtEpochMs = if (markFinished) (book.finishedAtEpochMs ?: atEpochMs) else book.finishedAtEpochMs,
      updatedAtEpochMs = atEpochMs,
    )
  }

  fun isChronologicallyLast(session: ReadingSession, all: List<ReadingSession>): Boolean {
    val latest = all.filter { it.bookId == session.bookId && it.endedAtEpochMs != null }
      .maxByOrNull { it.endedAtEpochMs ?: 0L }
    return latest?.id == session.id
  }

  /** 把有效时长按自然日切开，时区默认上海。 */
  fun splitDurationByDay(
    session: ReadingSession,
    nowEpochMs: Long,
    zone: ZoneId = Zones.Shanghai,
  ): Map<LocalDate, Long> {
    val total = effectiveDurationMs(session, nowEpochMs)
    if (total <= 0L) return emptyMap()
    val start = session.startedAtEpochMs
    val end = session.endedAtEpochMs ?: nowEpochMs
    val startDate = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
    val endDate = Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
    if (startDate == endDate) return mapOf(startDate to total)
    // 按「有效时长在日历上的比例」不够精确；改为按未暂停的时间轴切片。
    return splitAlongTimeline(session, nowEpochMs, zone)
  }

  private fun splitAlongTimeline(
    session: ReadingSession,
    nowEpochMs: Long,
    zone: ZoneId,
  ): Map<LocalDate, Long> {
    val end = session.endedAtEpochMs ?: nowEpochMs
    val pauses = session.pauses.map { it.pausedAtEpochMs to (it.resumedAtEpochMs ?: end) }
    val acc = mutableMapOf<LocalDate, Long>()
    var cursor = session.startedAtEpochMs
    while (cursor < end) {
      val zdt = Instant.ofEpochMilli(cursor).atZone(zone)
      val nextMidnight = zdt.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
      val sliceEnd = minOf(end, nextMidnight)
      val overlapPause = pauses.sumOf { (p, r) -> overlap(cursor, sliceEnd, p, r) }
      val gain = (sliceEnd - cursor - overlapPause).coerceAtLeast(0L)
      if (gain > 0) {
        val day = zdt.toLocalDate()
        acc[day] = (acc[day] ?: 0L) + gain
      }
      cursor = sliceEnd
    }
    return acc
  }

  private fun overlap(a0: Long, a1: Long, b0: Long, b1: Long): Long {
    val s = maxOf(a0, b0)
    val e = minOf(a1, b1)
    return (e - s).coerceAtLeast(0L)
  }

  fun periodRange(period: StatsPeriod, today: LocalDate, zone: ZoneId = Zones.Shanghai): Pair<Long, Long>? {
    return when (period) {
      StatsPeriod.ALL -> null
      StatsPeriod.WEEK -> {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(7)
        start.atStartOfDay(zone).toInstant().toEpochMilli() to
          end.atStartOfDay(zone).toInstant().toEpochMilli()
      }
      StatsPeriod.MONTH -> {
        val ym = YearMonth.from(today)
        ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() to
          ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
      }
      StatsPeriod.YEAR -> {
        val start = LocalDate.of(today.year, 1, 1)
        start.atStartOfDay(zone).toInstant().toEpochMilli() to
          start.plusYears(1).atStartOfDay(zone).toInstant().toEpochMilli()
      }
    }
  }

  fun aggregate(
    sessions: List<ReadingSession>,
    books: List<Book>,
    period: StatsPeriod,
    nowEpochMs: Long,
    zone: ZoneId = Zones.Shanghai,
  ): PeriodStats {
    val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
    val range = periodRange(period, today, zone)
    val dayMap = mutableMapOf<LocalDate, Long>()
    val booksTouched = mutableSetOf<String>()
    var duration = 0L
    for (s in sessions) {
      val slices = splitDurationByDay(s, nowEpochMs, zone)
      for ((day, ms) in slices) {
        if (range != null) {
          val t = day.atStartOfDay(zone).toInstant().toEpochMilli()
          if (t < range.first || t >= range.second) continue
        }
        dayMap[day] = (dayMap[day] ?: 0L) + ms
        duration += ms
        if (ms > 0) booksTouched.add(s.bookId)
      }
    }
    val finished = books.count { book ->
      val at = book.finishedAtEpochMs ?: return@count false
      if (book.status != BookStatus.FINISHED) return@count false
      if (range == null) true else at >= range.first && at < range.second
    }
    return PeriodStats(
      durationMs = duration,
      readingDays = dayMap.count { it.value > 0 },
      booksRead = booksTouched.size,
      booksFinished = finished,
    )
  }

  fun readingDaysInMonth(
    sessions: List<ReadingSession>,
    yearMonth: YearMonth,
    nowEpochMs: Long,
    zone: ZoneId = Zones.Shanghai,
  ): Set<LocalDate> {
    val days = mutableSetOf<LocalDate>()
    for (s in sessions) {
      for ((day, ms) in splitDurationByDay(s, nowEpochMs, zone)) {
        if (ms > 0 && YearMonth.from(day) == yearMonth) days.add(day)
      }
    }
    return days
  }

  fun formatDuration(ms: Long): Pair<String, String> {
    val totalMin = (ms / 60000L)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
      h > 0 -> "${h}小时${m}分" to "阅读时长"
      else -> "${m}分钟" to "阅读时长"
    }
  }
}
