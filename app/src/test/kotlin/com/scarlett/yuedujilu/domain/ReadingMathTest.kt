package com.scarlett.yuedujilu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReadingMathTest {
  private val zone: ZoneId = Zones.Shanghai

  private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
    ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

  @Test
  fun pauseAndResume_excludesPausedTime() {
    val start = at(2026, 11, 16, 10, 0)
    val pause = at(2026, 11, 16, 10, 30)
    val resume = at(2026, 11, 16, 11, 0)
    val end = at(2026, 11, 16, 11, 20)
    val session = ReadingSession(
      id = "s1",
      bookId = "b1",
      startedAtEpochMs = start,
      endedAtEpochMs = end,
      pauses = listOf(PauseSpan(pause, resume)),
    )
    // 10:00-11:20 共 80 分钟，暂停 30 分钟，有效 50 分钟
    assertEquals(50 * 60_000L, ReadingMath.effectiveDurationMs(session, end))
    assertFalse(ReadingMath.isPaused(session))
  }

  @Test
  fun openPause_countsUntilNow() {
    val start = at(2026, 11, 16, 10, 0)
    val pause = at(2026, 11, 16, 10, 10)
    val now = at(2026, 11, 16, 10, 40)
    val session = ReadingSession(
      id = "s1",
      bookId = "b1",
      startedAtEpochMs = start,
      pauses = listOf(PauseSpan(pause, null)),
    )
    assertTrue(ReadingMath.isPaused(session))
    // 有效仅 10 分钟
    assertEquals(10 * 60_000L, ReadingMath.effectiveDurationMs(session, now))
  }

  @Test
  fun crossDay_splitsByShanghaiCalendar() {
    val start = at(2026, 11, 16, 23, 30)
    val end = at(2026, 11, 17, 0, 20)
    val session = ReadingSession(
      id = "s1",
      bookId = "b1",
      startedAtEpochMs = start,
      endedAtEpochMs = end,
    )
    val split = ReadingMath.splitDurationByDay(session, end, zone)
    assertEquals(30 * 60_000L, split[LocalDate.of(2026, 11, 16)])
    assertEquals(20 * 60_000L, split[LocalDate.of(2026, 11, 17)])
  }

  @Test
  fun crossDay_withPauseAcrossMidnight() {
    val start = at(2026, 11, 16, 23, 0)
    val pause = at(2026, 11, 16, 23, 40)
    val resume = at(2026, 11, 17, 0, 10)
    val end = at(2026, 11, 17, 0, 30)
    val session = ReadingSession(
      id = "s1",
      bookId = "b1",
      startedAtEpochMs = start,
      endedAtEpochMs = end,
      pauses = listOf(PauseSpan(pause, resume)),
    )
    val split = ReadingMath.splitDurationByDay(session, end, zone)
    // 16日 23:00-23:40 = 40 分钟；17日 00:10-00:30 = 20 分钟
    assertEquals(40 * 60_000L, split[LocalDate.of(2026, 11, 16)])
    assertEquals(20 * 60_000L, split[LocalDate.of(2026, 11, 17)])
  }

  @Test
  fun finishAt475_keepsCurrentPage() {
    val now = at(2026, 11, 16, 12, 0)
    val book = Book(
      id = "b1",
      title = "示例",
      pageCount = 500,
      currentPage = 475,
      status = BookStatus.READING,
      createdAtEpochMs = now,
      updatedAtEpochMs = now,
    )
    val finished = ReadingMath.markFinished(book, now)
    assertEquals(BookStatus.FINISHED, finished.status)
    assertEquals(475, finished.currentPage)
    assertEquals(500, finished.pageCount)
    assertEquals(now, finished.finishedAtEpochMs)
    val undone = ReadingMath.undoFinish(finished, now + 1000)
    assertEquals(BookStatus.READING, undone.status)
    assertEquals(475, undone.currentPage)
    assertEquals(null, undone.finishedAtEpochMs)
  }

  @Test
  fun pagesDelta_neverNegative() {
    assertEquals(0, ReadingMath.pagesDelta(100, 80))
    assertEquals(25, ReadingMath.pagesDelta(450, 475))
  }

  @Test
  fun aggregate_weekIgnoresOutsideAndSplitsCrossDay() {
    val mon = at(2026, 11, 16, 10, 0) // Monday
    val sunNight = at(2026, 11, 15, 23, 50) // previous Sunday
    val monMorning = at(2026, 11, 16, 0, 20)
    val now = at(2026, 11, 16, 18, 0)
    val cross = ReadingSession(
      id = "cross",
      bookId = "b1",
      startedAtEpochMs = sunNight,
      endedAtEpochMs = monMorning,
    )
    val weekOnly = ReadingSession(
      id = "in",
      bookId = "b2",
      startedAtEpochMs = mon,
      endedAtEpochMs = mon + 60 * 60_000L,
    )
    val books = listOf(
      Book("b1", "A", createdAtEpochMs = mon, updatedAtEpochMs = mon, status = BookStatus.READING, currentPage = 10),
      Book("b2", "B", createdAtEpochMs = mon, updatedAtEpochMs = mon, status = BookStatus.READING, currentPage = 20),
      Book(
        "b3", "C", createdAtEpochMs = mon, updatedAtEpochMs = mon,
        status = BookStatus.FINISHED, currentPage = 475, pageCount = 500,
        finishedAtEpochMs = mon,
      ),
    )
    val stats = ReadingMath.aggregate(listOf(cross, weekOnly), books, StatsPeriod.WEEK, now, zone)
    // 跨天会话只计周一 20 分钟 + 周一 60 分钟
    assertEquals((20 + 60) * 60_000L, stats.durationMs)
    assertEquals(1, stats.readingDays)
    assertEquals(2, stats.booksRead)
    assertEquals(1, stats.booksFinished)
  }
}
