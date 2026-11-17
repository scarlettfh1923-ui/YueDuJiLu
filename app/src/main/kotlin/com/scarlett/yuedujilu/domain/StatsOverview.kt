package com.scarlett.yuedujilu.domain

import com.scarlett.yuedujilu.SourceFilter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** 闭区间自然日。 */
data class DateSpan(val start: LocalDate, val end: LocalDate) {
  init { require(!end.isBefore(start)) }
  operator fun contains(day: LocalDate): Boolean = !day.isBefore(start) && !day.isAfter(end)
  fun elapsedEnd(today: LocalDate): LocalDate = if (end.isAfter(today)) today else end
  fun elapsedDays(today: LocalDate): Int = ChronoUnit.DAYS.between(start, elapsedEnd(today)).toInt() + 1
}

/**
 * 指标值。缺失必须用 [Missing]，不得用 0 填空。
 * 真正零（口径完整且确无阅读）才用 [Known] 且 value=0。
 */
sealed class MetricValue {
  data class Known(
    val value: Long,
    val footnote: String? = null,
    val compare: String? = null,
  ) : MetricValue()
  data class Missing(val reason: String) : MetricValue()
}

data class OverviewReport(
  val span: DateSpan?,
  val previousSpan: DateSpan?,
  val periodLabel: String,
  val canGoForward: Boolean,
  val days: MetricValue,
  val durationMs: MetricValue,
  val booksRead: MetricValue,
  val booksFinished: MetricValue,
  val streak: MetricValue,
  val dailyAverageMs: MetricValue,
  val notes: MetricValue,
  val coverageNote: String,
  val markedDays: Set<LocalDate>,
)

object StatsOverview {

  fun resolveSpan(period: StatsPeriod, anchor: LocalDate, today: LocalDate): DateSpan? {
    return when (period) {
      StatsPeriod.WEEK -> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        DateSpan(start, start.plusDays(6))
      }
      StatsPeriod.MONTH -> {
        val ym = YearMonth.from(anchor)
        DateSpan(ym.atDay(1), ym.atEndOfMonth())
      }
      StatsPeriod.YEAR -> DateSpan(LocalDate.of(anchor.year, 1, 1), LocalDate.of(anchor.year, 12, 31))
      StatsPeriod.ALL -> null
    }
  }

  fun previousSpan(period: StatsPeriod, span: DateSpan): DateSpan? {
    return when (period) {
      StatsPeriod.WEEK -> DateSpan(span.start.minusDays(7), span.end.minusDays(7))
      StatsPeriod.MONTH -> {
        val prev = YearMonth.from(span.start).minusMonths(1)
        DateSpan(prev.atDay(1), prev.atEndOfMonth())
      }
      StatsPeriod.YEAR -> DateSpan(span.start.minusYears(1), span.end.minusYears(1))
      StatsPeriod.ALL -> null
    }
  }

  fun periodLabel(period: StatsPeriod, span: DateSpan?, today: LocalDate): String {
    return when (period) {
      StatsPeriod.ALL -> "全部时间"
      StatsPeriod.MONTH -> {
        val s = requireNotNull(span)
        "${s.start.year}年${s.start.monthValue}月"
      }
      StatsPeriod.YEAR -> "${requireNotNull(span).start.year}年"
      StatsPeriod.WEEK -> {
        val s = requireNotNull(span)
        val a = s.start
        val b = s.end
        if (a.year == b.year && a.month == b.month) {
          "${a.year}年${a.monthValue}月${a.dayOfMonth}日–${b.dayOfMonth}日"
        } else if (a.year == b.year) {
          "${a.year}年${a.monthValue}月${a.dayOfMonth}日–${b.monthValue}月${b.dayOfMonth}日"
        } else {
          "${a.year}年${a.monthValue}月${a.dayOfMonth}日–${b.year}年${b.monthValue}月${b.dayOfMonth}日"
        }
      }
    }
  }

  fun parseSyncDate(raw: String?, zone: ZoneId = Zones.Shanghai): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return runCatching { Instant.parse(raw).atZone(zone).toLocalDate() }.getOrNull()
      ?: WereadImport.parseDate(raw)
  }

  /**
   * @param wereadCoverageStart 微信导入起点（含）
   * @param wereadCoverageEnd 最后一次成功同步所在上海自然日（含）；尚未同步则为 null
   */
  fun overview(
    sessions: List<ReadingSession>,
    books: List<Book>,
    wereadDays: List<WereadDayRecord>,
    period: StatsPeriod,
    anchor: LocalDate,
    today: LocalDate,
    nowEpochMs: Long,
    source: SourceFilter,
    wereadAccountHash: String?,
    wereadCoverageStart: LocalDate?,
    wereadCoverageEnd: LocalDate?,
    zone: ZoneId = Zones.Shanghai,
    calendarMonth: YearMonth? = null,
  ): OverviewReport {
    val span = resolveSpan(period, anchor, today)
    val prev = span?.let { current -> previousSpan(period,current)?.let { full ->
      if(current.end.isAfter(today)) DateSpan(full.start,minOf(full.end,full.start.plusDays((current.elapsedDays(today)-1).coerceAtLeast(0).toLong()))) else full
    } }
    val latestWeread=wereadDays.groupBy { it.accountHash to it.date }.values.mapNotNull { it.maxByOrNull { r->r.updatedAtEpochMs } }
    val queryEnd = span?.elapsedEnd(today) ?: today
    val queryStart: LocalDate? = span?.start
    val current = compute(sessions, books, latestWeread, queryStart, queryEnd, today, nowEpochMs, source, wereadAccountHash, wereadCoverageStart, wereadCoverageEnd, zone)
    val previous = if (prev != null) {
      compute(sessions, books, latestWeread, prev.start, prev.elapsedEnd(today), today, nowEpochMs, source, wereadAccountHash, wereadCoverageStart, wereadCoverageEnd, zone)
    } else null
    val cmpLabel = previousCompareLabel(period)?.let { if(span?.end?.isAfter(today)==true) it+"同期" else it }
    val markedMonth = calendarMonth ?: YearMonth.from(anchor)
    val marked = markedDays(sessions, latestWeread, markedMonth, nowEpochMs, source, wereadAccountHash, wereadCoverageStart, wereadCoverageEnd, zone)
    return OverviewReport(
      span = span,
      previousSpan = prev,
      periodLabel = periodLabel(period, span, today),
      canGoForward = canShiftForward(period, anchor, today),
      days = withCompare(current.days, previous?.days, cmpLabel, MetricUnit.DAYS),
      durationMs = withCompare(current.durationMs, previous?.durationMs, cmpLabel, MetricUnit.DURATION),
      booksRead = withCompare(current.booksRead, previous?.booksRead, cmpLabel, MetricUnit.BOOKS),
      booksFinished = withCompare(current.booksFinished, previous?.booksFinished, cmpLabel, MetricUnit.BOOKS),
      streak = withCompare(current.streak, previous?.streak, cmpLabel, MetricUnit.DAYS),
      dailyAverageMs = withCompare(current.dailyAverageMs, previous?.dailyAverageMs, cmpLabel, MetricUnit.DURATION),
      notes = withCompare(current.notes, previous?.notes, cmpLabel, MetricUnit.NOTES),
      coverageNote = current.coverageNote,
      markedDays = marked,
    )
  }

  fun canShiftForward(period: StatsPeriod, anchor: LocalDate, today: LocalDate): Boolean {
    if (period == StatsPeriod.ALL) return false
    val current = resolveSpan(period, anchor, today) ?: return false
    val nextAnchor = shiftAnchor(period, anchor, 1)
    val next = resolveSpan(period, nextAnchor, today) ?: return false
    return next.start.isAfter(current.start) && !next.start.isAfter(resolveSpan(period, today, today)!!.start)
  }

  fun shiftAnchor(period: StatsPeriod, anchor: LocalDate, delta: Long): LocalDate {
    return when (period) {
      StatsPeriod.WEEK -> anchor.plusWeeks(delta)
      StatsPeriod.MONTH -> anchor.plusMonths(delta)
      StatsPeriod.YEAR -> anchor.plusYears(delta)
      StatsPeriod.ALL -> anchor
    }
  }

  private data class Slice(
    val days: MetricValue,
    val durationMs: MetricValue,
    val booksRead: MetricValue,
    val booksFinished: MetricValue,
    val streak: MetricValue,
    val dailyAverageMs: MetricValue,
    val notes: MetricValue,
    val coverageNote: String,
  )

  private fun compute(
    sessions: List<ReadingSession>,
    books: List<Book>,
    wereadDays: List<WereadDayRecord>,
    start: LocalDate?,
    end: LocalDate,
    today: LocalDate,
    nowEpochMs: Long,
    source: SourceFilter,
    hash: String?,
    wereadStart: LocalDate?,
    wereadEnd: LocalDate?,
    zone: ZoneId,
  ): Slice {
    if(start!=null && start.isAfter(end)) {
      val missing=MetricValue.Missing("未来区间暂无已过日期")
      return Slice(missing,missing,missing,missing,missing,missing,missing,"未来区间不计零、不计算环比")
    }
    val includePhysical = source != SourceFilter.WEREAD
    val includeWeread = source != SourceFilter.PHYSICAL
    val wereadConnected = !hash.isNullOrBlank() && wereadStart != null && wereadEnd != null && !wereadEnd.isBefore(wereadStart)

    val bookById = books.associateBy { it.id }
    val physicalMs = mutableMapOf<LocalDate, Long>()
    val physicalBooks = mutableSetOf<String>()
    if (includePhysical) {
      for (s in sessions) {
        val book = bookById[s.bookId]
        if (book?.source == BookSource.WEREAD) continue
        for ((day, ms) in ReadingMath.splitDurationByDay(s, nowEpochMs, zone)) {
          if (ms <= 0L) continue
          if (start != null && (day.isBefore(start) || day.isAfter(end))) continue
          if (start == null && day.isAfter(end)) continue
          physicalMs[day] = (physicalMs[day] ?: 0L) + ms
          physicalBooks += s.bookId
        }
      }
    }

    val wereadMs = mutableMapOf<LocalDate, Long>()
    var wereadUnknownInRange = includeWeread && wereadConnected && (
      (start?.isBefore(wereadStart!!) == true) || end.isAfter(wereadEnd!!) ||
        (start==null && physicalMs.keys.any { it.isBefore(wereadStart) })
    )
    if (includeWeread && wereadConnected) {
      val byDate = wereadDays.asSequence()
        .filter { it.accountHash == hash }
        .mapNotNull { rec -> WereadImport.parseDate(rec.date)?.let { it to rec } }
        .groupBy({ it.first }, { it.second })
      var cursor = maxOf(wereadStart!!, start ?: wereadStart)
      val last = minOf(wereadEnd!!, end)
      if (!cursor.isAfter(last)) {
        while (!cursor.isAfter(last)) {
          val recs = byDate[cursor].orEmpty()
          val complete = listOfNotNull(recs.maxByOrNull { it.updatedAtEpochMs }).filter { it.durationComplete && it.durationSec!=null && it.durationSec>=0L }
          if (complete.isEmpty()) {
            wereadUnknownInRange = true
          } else {
            wereadMs[cursor] = complete.sumOf { it.durationSec ?: 0L } * 1000L
          }
          cursor = cursor.plusDays(1)
        }
      }
    }

    val unionDays = sortedSetOf<LocalDate>()
    physicalMs.filter { it.value > 0 }.keys.forEach { unionDays += it }
    wereadMs.filter { it.value > 0 }.keys.forEach { unionDays += it }
    // A positive partial day proves reading happened, even when its full duration is unknown.
    // Calendar and day-count must agree; incomplete duration still cannot become a complete total.
    if(includeWeread && wereadConnected) wereadDays.filter {
      it.accountHash==hash && (it.durationSec?:0L)>0L
    }.mapNotNull { WereadImport.parseDate(it.date) }.filter {
      !it.isAfter(end) && (start==null || !it.isBefore(start)) &&
        !it.isBefore(wereadStart) && !it.isAfter(wereadEnd)
    }.forEach { unionDays += it }

    var duration = 0L
    val dayIterStart = start ?: run {
      val all = unionDays
      if (all.isEmpty()) end else all.first()
    }
    var d = dayIterStart
    while (!d.isAfter(end)) {
      val p = if (includePhysical) physicalMs[d] ?: 0L else 0L
      val w = if (includeWeread) wereadMs[d] else null
      duration += p + (w ?: 0L)
      d = d.plusDays(1)
    }

    val coverageBits = mutableListOf<String>()
    when {
      source == SourceFilter.WEREAD && !wereadConnected ->
        coverageBits += "尚未同步微信读书，区间内没有可用的微信日数据"
      source == SourceFilter.ALL && !wereadConnected ->
        coverageBits += "尚未连接或同步微信读书，当前仅统计实体书"
      source == SourceFilter.ALL && wereadUnknownInRange ->
        coverageBits += "微信部分日期未同步完整，时长与天数只计入已确认的记录，缺失日不按 0 补"
      source == SourceFilter.WEREAD && wereadUnknownInRange ->
        coverageBits += "部分日期没有完整的微信日数据，已从合计中排除，不会按 0 补"
    }
    if (includePhysical && includeWeread && wereadConnected) {
      coverageBits += "时长按来源相加，阅读天数按自然日去重合并"
    }

    val durationComplete = when {
      source == SourceFilter.WEREAD && !wereadConnected -> false
      includeWeread && wereadConnected && wereadUnknownInRange -> false
      source == SourceFilter.WEREAD && wereadConnected && !wereadUnknownInRange -> true
      source == SourceFilter.PHYSICAL -> true
      source == SourceFilter.ALL && !wereadConnected -> true // 仅实体，已标注
      source == SourceFilter.ALL && wereadConnected && !wereadUnknownInRange -> true
      else -> false
    }

    val daysValue: MetricValue = when {
      source == SourceFilter.WEREAD && !wereadConnected ->
        MetricValue.Missing("未提供（尚未同步微信读书）")
      source == SourceFilter.WEREAD && wereadUnknownInRange ->
        MetricValue.Missing("数据不完整，暂不按缺失日计 0")
      else -> MetricValue.Known(
        unionDays.size.toLong(),
        footnote = when {
          source == SourceFilter.ALL && !wereadConnected -> "仅实体书"
          source == SourceFilter.ALL && wereadUnknownInRange -> "仅含已确认日期，缺失日未计入"
          else -> null
        },
      )
    }

    val durationValue: MetricValue = when {
      source == SourceFilter.WEREAD && !wereadConnected ->
        MetricValue.Missing("未提供（尚未同步微信读书）")
      includeWeread && wereadConnected && wereadUnknownInRange ->
        MetricValue.Known(duration, footnote = "仅含已同步完整的日期，可能少计")
      source == SourceFilter.ALL && !wereadConnected ->
        MetricValue.Known(duration, footnote = "仅实体书")
      else -> MetricValue.Known(duration)
    }

    // An explicit finish is an interval reading record too, even without a timer.
    if(includePhysical)books.filter { book->book.source!=BookSource.WEREAD&&book.status==BookStatus.FINISHED&&book.finishedAtEpochMs?.let{at->
      val day=Instant.ofEpochMilli(at).atZone(zone).toLocalDate();!day.isAfter(end)&&(start==null||!day.isBefore(start))
    }==true }.forEach{physicalBooks+=it.id}
    val booksValue: MetricValue = when {
      source == SourceFilter.WEREAD ->
        MetricValue.Missing("未提供（微信读书区间本数无明细，不会用书架存量代替）")
      else -> MetricValue.Known(
        physicalBooks.size.toLong(),
        footnote = if (source == SourceFilter.ALL) "仅实体书（微信读书区间本数无明细）" else null,
      )
    }

    val finishedCount = if (!includePhysical) 0 else books.count { book ->
      if (book.source == BookSource.WEREAD) return@count false
      if (book.status != BookStatus.FINISHED) return@count false
      val at = book.finishedAtEpochMs ?: return@count false
      val day = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
      if (day.isAfter(end)) return@count false
      start == null || !day.isBefore(start)
    }
    val finishedValue: MetricValue = when {
      source == SourceFilter.WEREAD ->
        MetricValue.Missing("未提供（微信读书读完本数无区间明细）")
      else -> MetricValue.Known(
        finishedCount.toLong(),
        footnote = if (source == SourceFilter.ALL) "仅实体书" else null,
      )
    }

    val denom = if (start == null) {
      val first = unionDays.minOrNull() ?: end
      ChronoUnit.DAYS.between(first, end).toInt() + 1
    } else {
      DateSpan(start, if (end.isAfter(today)) today else end).let {
        // if start after end (future empty) at least 1? shouldn't happen
        ChronoUnit.DAYS.between(it.start, it.end).toInt() + 1
      }
    }.coerceAtLeast(1)

    val avgValue: MetricValue = when {
      durationValue is MetricValue.Missing ->
        MetricValue.Missing(durationValue.reason)
      source == SourceFilter.WEREAD && wereadUnknownInRange ->
        MetricValue.Missing("数据不完整，暂不计算日均")
      includeWeread && wereadConnected && wereadUnknownInRange ->
        MetricValue.Missing("数据不完整，暂不计算日均")
      durationValue is MetricValue.Known ->
        MetricValue.Known(
          durationValue.value / denom,
          footnote = if (start == null) {
            "分母为首次有记录之日至区间末日的 $denom 个自然日（含未阅读日）"
          } else {
            "分母为区间内已过的 $denom 个自然日（含未阅读日）"
          },
        )
      else -> MetricValue.Missing("暂不计算日均")
    }

    val streakValue = streak(
      end = end,
      includePhysical = includePhysical,
      includeWeread = includeWeread,
      wereadConnected = wereadConnected,
      wereadStart = wereadStart,
      wereadEnd = wereadEnd,
      physicalMs = physicalMs,
      wereadMs = wereadMs,
    )

    val notesValue = notesMetric(
      includeWeread = includeWeread,
      wereadConnected = wereadConnected,
      hash = hash,
      wereadDays = wereadDays.filter { rec->WereadImport.parseDate(rec.date)?.let { date->wereadStart!=null&&wereadEnd!=null&&!date.isBefore(wereadStart)&&!date.isAfter(wereadEnd) }==true },
      start = start,
      end = end,
    )

    val coverageNote = coverageBits.joinToString("。").ifBlank {
      when (source) {
        SourceFilter.PHYSICAL -> "仅统计实体书的计时与读完记录"
        SourceFilter.WEREAD -> "仅统计已同步完整的微信读书日数据"
        SourceFilter.ALL -> "时长按来源相加，阅读天数按自然日去重合并"
      }
    }

    // silence unused
    durationComplete
    return Slice(daysValue, durationValue, booksValue, finishedValue, streakValue, avgValue, notesValue, coverageNote)
  }

  /**
   * 连续阅读：从区间末日（未结束的区间已在调用方收束到今天）向前，
   * 连续有阅读记录的自然日天数。来源并集。
   * 遇到微信覆盖窗口内日数据缺失则停止计算，不把缺失当作未读。
   */
  private fun streak(
    end: LocalDate,
    includePhysical: Boolean,
    includeWeread: Boolean,
    wereadConnected: Boolean,
    wereadStart: LocalDate?,
    wereadEnd: LocalDate?,
    physicalMs: Map<LocalDate, Long>,
    wereadMs: Map<LocalDate, Long>,
  ): MetricValue {
    if (includeWeread && !includePhysical && !wereadConnected) {
      return MetricValue.Missing("未提供（尚未同步微信读书）")
    }
    var count = 0L
    var cursor = end
    // safety cap
    var steps = 0
    while (steps < 40000) {
      steps++
      val inWereadWindow = includeWeread && wereadConnected &&
        wereadStart != null && wereadEnd != null &&
        !cursor.isBefore(wereadStart) && !cursor.isAfter(wereadEnd)
      val physicalKnown = includePhysical
      val pRead = includePhysical && (physicalMs[cursor] ?: 0L) > 0L
      val wKnown = when {
        !includeWeread -> true
        !wereadConnected -> true // 不把未连接当成缺失
        inWereadWindow -> wereadMs.containsKey(cursor)
        else -> true
      }
      val wRead = includeWeread && (wereadMs[cursor] ?: 0L) > 0L
      val known = when {
        includeWeread && includePhysical && inWereadWindow && !wKnown && !pRead -> false
        includeWeread && !includePhysical && inWereadWindow && !wKnown -> false
        else -> physicalKnown || wKnown || !includeWeread
      }
      if (!known) {
        return if (count == 0L) MetricValue.Missing("遇缺失日，连续天数暂不计算")
        else MetricValue.Known(count, footnote = "回溯在缺失日中止，不把缺失当作未读")
      }
      val read = pRead || wRead
      if (!read) {
        return MetricValue.Known(count)
      }
      count++
      cursor = cursor.minusDays(1)
    }
    return MetricValue.Known(count)
  }

  private fun notesMetric(
    includeWeread: Boolean,
    wereadConnected: Boolean,
    hash: String?,
    wereadDays: List<WereadDayRecord>,
    start: LocalDate?,
    end: LocalDate,
  ): MetricValue {
    if (!includeWeread) return MetricValue.Missing("未提供（仅统计微信划线与想法）")
    if (!wereadConnected || hash.isNullOrBlank()) return MetricValue.Missing("未提供（尚未同步微信读书）")
    var highlights = 0
    var thoughts = 0
    var anyComplete = false
    var incomplete = false
    val completeDates=mutableSetOf<LocalDate>()
    for (rec in wereadDays) {
      if (rec.accountHash != hash) continue
      val date = WereadImport.parseDate(rec.date) ?: continue
      if (date.isAfter(end)) continue
      if (start != null && date.isBefore(start)) continue
      if (!rec.notesComplete || rec.highlightCount==null || rec.thoughtCount==null) {
        incomplete = true
        continue
      }
      anyComplete = true
      completeDates+=date
      highlights += rec.highlightCount
      thoughts += rec.thoughtCount
    }
    if(start!=null && completeDates.size < ChronoUnit.DAYS.between(start,end)+1)incomplete=true
    if (!anyComplete) {
      return MetricValue.Missing(if (incomplete) "笔记数据不完整，暂不计数" else "未提供（此区间没有完整的划线与想法）")
    }
    return MetricValue.Known(
      (highlights + thoughts).toLong(),
      footnote = buildString {
        append("微信划线 ${highlights} 条、想法 ${thoughts} 条；书签无逐条日期，不计入")
        if (incomplete) append("。部分日期未完整同步，已排除")
      },
    )
  }

  private fun withCompare(current: MetricValue, previous: MetricValue?, label: String?, unit: MetricUnit): MetricValue {
    val cur = current as? MetricValue.Known ?: return current
    val prev = previous as? MetricValue.Known
    if (prev == null || label == null) return cur
    // 两期都要是完整口径：任一带「可能少计 / 不完整」脚注则不对比
    if (looksIncomplete(cur.footnote) || looksIncomplete(prev.footnote)) {
      return cur.copy(compare = "上期数据不足，暂不对比")
    }
    val delta = cur.value - prev.value
    val compare = when {
      delta > 0L -> "$label +${formatDelta(unit, delta)}"
      delta < 0L -> "$label -${formatDelta(unit, -delta)}"
      else -> "$label 持平"
    }
    return cur.copy(compare = compare)
  }

  private fun looksIncomplete(note: String?): Boolean {
    if (note == null) return false
    return note.contains("少计") || note.contains("不完整") || note.contains("缺失") || note.contains("未完整")
  }

  private enum class MetricUnit { DURATION,DAYS,BOOKS,NOTES }
  private fun formatDelta(unit:MetricUnit, abs:Long):String=when(unit) {
    MetricUnit.DURATION->durationText(abs)
    MetricUnit.DAYS->"${abs}天";MetricUnit.BOOKS->"${abs}本";MetricUnit.NOTES->"${abs}条"
  }
  fun durationText(ms:Long):String {
    val seconds=ms.coerceAtLeast(0L)/1000;val h=seconds/3600;val m=seconds%3600/60;val sec=seconds%60
    return buildString { if(h>0)append("${h}小时");if(m>0)append("${m}分");if(sec>0 || (h==0L&&m==0L))append("${sec}秒") }
  }

  private fun previousCompareLabel(period: StatsPeriod): String? = when (period) {
    StatsPeriod.WEEK -> "较上周"
    StatsPeriod.MONTH -> "较上月"
    StatsPeriod.YEAR -> "较去年"
    StatsPeriod.ALL -> null
  }

  fun markedDays(
    sessions: List<ReadingSession>,
    wereadDays: List<WereadDayRecord>,
    yearMonth: YearMonth,
    nowEpochMs: Long,
    source: SourceFilter,
    hash: String?,
    wereadStart: LocalDate?,
    wereadEnd: LocalDate?,
    zone: ZoneId = Zones.Shanghai,
  ): Set<LocalDate> {
    val includePhysical = source != SourceFilter.WEREAD
    val includeWeread = source != SourceFilter.PHYSICAL
    val days = mutableSetOf<LocalDate>()
    if (includePhysical) {
      days += ReadingMath.readingDaysInMonth(sessions, yearMonth, nowEpochMs, zone)
    }
    if (includeWeread && !hash.isNullOrBlank() && wereadStart != null && wereadEnd != null) {
      for (rec in wereadDays.groupBy { it.accountHash to it.date }.values.mapNotNull { it.maxByOrNull { r -> r.updatedAtEpochMs } }) {
        if (rec.accountHash != hash) continue
        val date = WereadImport.parseDate(rec.date) ?: continue
        if (YearMonth.from(date) != yearMonth) continue
        if (date.isBefore(wereadStart) || date.isAfter(wereadEnd)) continue
        if ((rec.durationSec ?: 0L) > 0L) days += date
      }
    }
    return days
  }
}
