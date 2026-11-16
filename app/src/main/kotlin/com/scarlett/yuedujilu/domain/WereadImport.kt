package com.scarlett.yuedujilu.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 微信读书同步语义（在官方接口文档确认前，只处理本地配置与合并规则，不发明 endpoint）。
 *
 * - importStartDate 持久固定；「最近 7 天」只在尚未设置时初始化起点（含今天共 7 个自然日）。
 * - 之后每次同步范围都是同一起点 → 今天，不是滚动 7 天，也不会自动改起点。
 * - 按账户 + 来源日期 + 远端记录 id upsert，不重复插入。
 */
object WereadImport {
  private val ISO = DateTimeFormatter.ISO_LOCAL_DATE

  fun parseDate(raw: String?): LocalDate? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    return try {
      LocalDate.parse(text, ISO)
    } catch (_: DateTimeParseException) {
      null
    }
  }

  fun format(date: LocalDate): String = date.format(ISO)

  /** 最近 7 天的起点：今天往前共 7 个自然日（含今天）。 */
  fun last7DaysStart(today: LocalDate): LocalDate = today.minusDays(6)

  /**
   * 快捷选择「最近 7 天」。已有起点时原样返回，绝不按当天滚动改写。
   */
  fun applyLast7DaysShortcut(today: LocalDate, existingStart: LocalDate?): LocalDate =
    existingStart ?: last7DaysStart(today)

  data class SyncWindow(val start: LocalDate, val end: LocalDate) {
    init {
      require(!end.isBefore(start)) { "同步窗口结束不能早于起点" }
    }
  }

  fun syncWindow(start: LocalDate, today: LocalDate): SyncWindow {
    val from = if (start.isAfter(today)) today else start
    return SyncWindow(from, today)
  }

  enum class Readiness { NOT_READY, CONFIGURED }

  fun readiness(hasKey: Boolean, start: LocalDate?): Readiness =
    if (hasKey && start != null) Readiness.CONFIGURED else Readiness.NOT_READY

  data class RemoteRecord(
    val accountId: String,
    val sourceDate: LocalDate,
    val remoteId: String,
    val payload: String = "",
  )

  fun upsertKey(record: RemoteRecord): String =
    "${record.accountId}|${format(record.sourceDate)}|${record.remoteId}"

  /** 按账户 + 来源日期 + 远端 id 更新或插入，保持原有顺序，新记录追加在末尾。 */
  fun upsertByAccountAndSourceDate(
    existing: List<RemoteRecord>,
    incoming: List<RemoteRecord>,
  ): List<RemoteRecord> {
    val index = existing.mapIndexed { i, r -> upsertKey(r) to i }.toMap().toMutableMap()
    val out = existing.toMutableList()
    for (item in incoming) {
      val key = upsertKey(item)
      val at = index[key]
      if (at != null) {
        out[at] = item
      } else {
        index[key] = out.size
        out += item
      }
    }
    return out
  }

  fun periodRange(period: StatsPeriod, today: java.time.LocalDate): ClosedRange<java.time.LocalDate> {
    val start = when (period) {
      StatsPeriod.WEEK -> today.minusDays((today.dayOfWeek.value - 1).toLong())
      StatsPeriod.MONTH -> today.withDayOfMonth(1)
      StatsPeriod.YEAR -> java.time.LocalDate.of(today.year, 1, 1)
      StatsPeriod.ALL -> java.time.LocalDate.of(1970, 1, 1)
    }
    return start..today
  }

  fun durationSecInRange(days: List<com.scarlett.yuedujilu.domain.WereadDayRecord>, accountHash: String?, range: ClosedRange<java.time.LocalDate>): Long {
    if (accountHash.isNullOrBlank()) return 0L
    return days.asSequence()
      .filter { it.accountHash == accountHash && it.durationComplete }
      .mapNotNull { rec -> parseDate(rec.date)?.let { it to (rec.durationSec ?: 0L) } }
      .filter { it.first in range }
      .sumOf { it.second }
  }

  fun notesInRange(days: List<com.scarlett.yuedujilu.domain.WereadDayRecord>, accountHash: String?, range: ClosedRange<java.time.LocalDate>): Pair<Int, Int> {
    if (accountHash.isNullOrBlank()) return 0 to 0
    var h = 0
    var th = 0
    for (rec in days) {
      if (rec.accountHash != accountHash || !rec.notesComplete) continue
      val date = parseDate(rec.date) ?: continue
      if (date !in range) continue
      h += rec.highlightCount ?: 0
      th += rec.thoughtCount ?: 0
    }
    return h to th
  }
}
