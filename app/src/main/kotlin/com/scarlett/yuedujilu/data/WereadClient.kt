package com.scarlett.yuedujilu.data

import com.scarlett.yuedujilu.domain.WereadDayRecord
import com.scarlett.yuedujilu.domain.WereadImport
import com.scarlett.yuedujilu.domain.WereadNoteItem
import com.scarlett.yuedujilu.domain.WereadPartStatus
import com.scarlett.yuedujilu.domain.Zones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import kotlin.coroutines.coroutineContext

data class WereadTransportResponse(val httpCode: Int, val body: String)

fun interface WereadTransport {
  suspend fun post(endpoint: String, authorization: String, jsonBody: String): WereadTransportResponse
}

class HttpWereadTransport(
  private val connectTimeoutMs: Int = 15_000,
  private val readTimeoutMs: Int = 20_000,
) : WereadTransport {
  override suspend fun post(endpoint: String, authorization: String, jsonBody: String): WereadTransportResponse =
    withContext(Dispatchers.IO) {
      val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", authorization)
      }
      try {
        conn.outputStream.use { it.write(jsonBody.toByteArray(StandardCharsets.UTF_8)) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
          ?.bufferedReader()?.readText().orEmpty()
        WereadTransportResponse(code, text)
      } finally {
        conn.disconnect()
      }
    }
}

/**
 * 真实调用官方网关。默认不发请求：没有用户密钥时直接 MissingKey。
 * 单测注入 [WereadTransport] 使用本地 fixture，不接触真实账号。
 */
class WereadClient(
  private val transport: WereadTransport = HttpWereadTransport(),
  private val endpoint: String = WereadGateway.ENDPOINT,
  private val paceMs: Long = 300L,
) {
  private var lastCallAt = 0L

  suspend fun testConnection(apiKey: String): WereadApiResult {
    if (apiKey.isBlank()) return WereadApiResult.MissingKey
    return call(apiKey, WereadGateway.API_LIST)
  }

  suspend fun fetchReadDetail(apiKey: String, mode: String, baseTime: Long): WereadApiResult {
    if (apiKey.isBlank()) return WereadApiResult.MissingKey
    return call(apiKey, WereadGateway.API_READDATA, mapOf("mode" to mode, "baseTime" to baseTime))
  }

  suspend fun fetchNotebooks(apiKey: String, count: Int, lastSort: Long?): WereadApiResult {
    if (apiKey.isBlank()) return WereadApiResult.MissingKey
    val params = mutableMapOf<String, Any?>("count" to count)
    if (lastSort != null) params["lastSort"] = lastSort
    return call(apiKey, WereadGateway.API_NOTEBOOKS, params)
  }

  suspend fun fetchBookmarks(apiKey: String, bookId: String): WereadApiResult {
    if (apiKey.isBlank()) return WereadApiResult.MissingKey
    return call(apiKey, WereadGateway.API_BOOKMARKS, mapOf("bookId" to bookId))
  }

  suspend fun fetchReviews(apiKey: String, bookId: String, synckey: Long?, count: Int = 20): WereadApiResult {
    if (apiKey.isBlank()) return WereadApiResult.MissingKey
    val params = mutableMapOf<String, Any?>("bookid" to bookId, "count" to count)
    if (synckey != null) params["synckey"] = synckey
    return call(apiKey, WereadGateway.API_REVIEWS, params)
  }

  /**
   * 按固定起点到今天，遍历年/月/必要时周，用日桶精确筛选。
   * 无日明细的周期记为不完整，不用 totalReadTime 冒充。
   */
  suspend fun syncDailyDuration(
    apiKey: String,
    start: LocalDate,
    today: LocalDate,
    existing: List<WereadDayRecord>,
    nowMs: Long,
    onProgress: suspend (String) -> Unit = {},
  ): Pair<WereadSyncReport, List<WereadDayRecord>?> {
    if (apiKey.isBlank()) {
      return report(WereadPartStatus.FAILED, WereadPartStatus.IDLE, "尚未填写微信读书密钥") to null
    }
    val account = WereadGateway.accountPartition(apiKey)
    val collected = linkedMapOf<LocalDate, Long>()
    val incomplete = mutableListOf<String>()
    try {
      for (year in WereadGateway.yearsOverlapping(start, today)) {
        coroutineContext.ensureActive()
        onProgress("正在读取 $year 年阅读时长")
        val yearStart = LocalDate.of(year, 1, 1)
        val annual = fetchReadDetail(apiKey, "annually", WereadGateway.shanghaiEpochSecond(yearStart))
        when (annual) {
          is WereadApiResult.UpgradeRequired ->
            return report(WereadPartStatus.UPGRADE, WereadPartStatus.IDLE, annual.message, incomplete) to null
          is WereadApiResult.MissingKey ->
            return report(WereadPartStatus.FAILED, WereadPartStatus.IDLE, "尚未填写微信读书密钥") to null
          is WereadApiResult.Error, is WereadApiResult.Failed -> {
            if (!fillYearByMonths(apiKey, year, start, today, collected, incomplete, onProgress)) {
              return abortDuration(annual, incomplete) to null
            }
          }
          is WereadApiResult.Ok -> {
            val buckets = WereadGateway.extractDailySeconds("annually", annual.body)
            if (buckets.complete) {
              val yearEnd = LocalDate.of(year, 12, 31)
              collected += WereadGateway.materializePeriodDays(
                yearStart, yearEnd, start, today,
                WereadGateway.filterDays(buckets.secondsByTimestamp, start, today),
                complete = true,
              )
            } else if (!fillYearByMonths(apiKey, year, start, today, collected, incomplete, onProgress)) {
              return abortDuration(annual, incomplete) to null
            }
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      return report(
        WereadPartStatus.FAILED,
        WereadPartStatus.IDLE,
        "同步时长失败：${WereadGateway.redact(e.message ?: e.javaClass.simpleName)}",
        incomplete,
      ) to null
    }
    val written = WereadGateway.mergeDurationDays(existing, account, collected, nowMs)
    val status = if (incomplete.isEmpty()) WereadPartStatus.SUCCESS else WereadPartStatus.INCOMPLETE
    val message = if (incomplete.isEmpty()) {
      "已同步 ${collected.size} 天阅读时长"
    } else {
      "已同步 ${collected.size} 天阅读时长，但 ${incomplete.joinToString("、")} 没有日明细，这些日期未改写，也没有用整月合计代替"
    }
    return WereadSyncReport(
      durationStatus = status,
      notesStatus = WereadPartStatus.IDLE,
      durationDaysWritten = collected.size,
      incompletePeriods = incomplete,
      message = message,
    ) to written
  }

  private suspend fun fillYearByMonths(
    apiKey: String,
    year: Int,
    start: LocalDate,
    today: LocalDate,
    collected: MutableMap<LocalDate, Long>,
    incomplete: MutableList<String>,
    onProgress: suspend (String) -> Unit,
  ): Boolean {
    val months = WereadGateway.monthsOverlapping(start, today).filter { it.year == year }
    for (monthStart in months) {
      coroutineContext.ensureActive()
      val ym = YearMonth.from(monthStart)
      onProgress("正在读取 ${ym.year}年${ym.monthValue}月 阅读时长")
      val result = fetchReadDetail(apiKey, "monthly", WereadGateway.shanghaiEpochSecond(monthStart))
      when (result) {
        is WereadApiResult.UpgradeRequired, is WereadApiResult.MissingKey,
        is WereadApiResult.Error, is WereadApiResult.Failed -> return false
        is WereadApiResult.Ok -> {
          val buckets = WereadGateway.extractDailySeconds("monthly", result.body)
          val monthEnd = ym.atEndOfMonth()
          if (buckets.complete) {
            collected += WereadGateway.materializePeriodDays(
              monthStart, monthEnd, start, today,
              WereadGateway.filterDays(buckets.secondsByTimestamp, start, today),
              complete = true,
            )
          } else if (!fillMonthByWeeks(apiKey, monthStart, monthEnd, start, today, collected, incomplete, onProgress)) {
            return false
          }
        }
      }
    }
    return true
  }

  private suspend fun fillMonthByWeeks(
    apiKey: String,
    monthStart: LocalDate,
    monthEnd: LocalDate,
    start: LocalDate,
    today: LocalDate,
    collected: MutableMap<LocalDate, Long>,
    incomplete: MutableList<String>,
    onProgress: suspend (String) -> Unit,
  ): Boolean {
    val weeks = WereadGateway.mondaysOverlapping(maxOf(monthStart, start), minOf(monthEnd, today))
    var any = false
    for (monday in weeks) {
      coroutineContext.ensureActive()
      onProgress("正在读取周起始 $monday 的阅读时长")
      val result = fetchReadDetail(apiKey, "weekly", WereadGateway.shanghaiEpochSecond(monday))
      when (result) {
        is WereadApiResult.UpgradeRequired, is WereadApiResult.MissingKey,
        is WereadApiResult.Error, is WereadApiResult.Failed -> return false
        is WereadApiResult.Ok -> {
          val buckets = WereadGateway.extractDailySeconds("weekly", result.body)
          val weekEnd = monday.plusDays(6)
          if (buckets.complete) {
            any = true
            collected += WereadGateway.materializePeriodDays(
              monday, weekEnd, maxOf(start, monthStart), minOf(today, monthEnd),
              WereadGateway.filterDays(buckets.secondsByTimestamp, start, today),
              complete = true,
            )
          }
        }
      }
    }
    if (!any) incomplete += "${monthStart.year}年${monthStart.monthValue}月"
    return true
  }

  private fun abortDuration(result: WereadApiResult, incomplete: List<String>): WereadSyncReport {
    val message = when (result) {
      is WereadApiResult.UpgradeRequired -> result.message
      is WereadApiResult.Error -> result.message
      is WereadApiResult.Failed -> result.message
      WereadApiResult.MissingKey -> "尚未填写微信读书密钥"
      is WereadApiResult.Ok -> "同步中断"
    }
    val status = if (result is WereadApiResult.UpgradeRequired) WereadPartStatus.UPGRADE else WereadPartStatus.FAILED
    return report(status, WereadPartStatus.IDLE, message, incomplete)
  }

  suspend fun syncNotes(
    apiKey: String,
    start: LocalDate,
    today: LocalDate,
    nowEpochSec: Long,
    existingDays: List<WereadDayRecord>,
    existingNotes: List<WereadNoteItem>,
    nowMs: Long,
    onProgress: suspend (String) -> Unit = {},
  ): Pair<WereadSyncReport, Pair<List<WereadDayRecord>, List<WereadNoteItem>>?> {
    if (apiKey.isBlank()) {
      return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, "尚未填写微信读书密钥") to null
    }
    val account = WereadGateway.accountPartition(apiKey)
    val allNotes = mutableListOf<ParsedNote>()
    try {
      val books = mutableListOf<NotebookSummary>()
      var lastSort: Long? = null
      var page = 0
      do {
        coroutineContext.ensureActive()
        page += 1
        onProgress("正在获取笔记本第 $page 页")
        when (val result = fetchNotebooks(apiKey, count = 100, lastSort = lastSort)) {
          is WereadApiResult.UpgradeRequired ->
            return report(WereadPartStatus.IDLE, WereadPartStatus.UPGRADE, result.message) to null
          is WereadApiResult.MissingKey ->
            return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, "尚未填写微信读书密钥") to null
          is WereadApiResult.Error ->
            return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, result.message) to null
          is WereadApiResult.Failed ->
            return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, result.message) to null
          is WereadApiResult.Ok -> {
            val parsed = WereadGateway.parseNotebooksPage(result.body)
            books += parsed.books
            if (!parsed.hasMore) break
            val next = parsed.lastSort ?: break
            if (lastSort != null && next == lastSort) {
              return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, "笔记本分页未前进，已停止以免把第一页当成全部") to null
            }
            lastSort = next
          }
        }
      } while (true)

      val uniqueBooks = books.distinctBy { it.bookId }
      uniqueBooks.forEachIndexed { index, book ->
        coroutineContext.ensureActive()
        onProgress("正在同步划线与想法 ${index + 1}/${uniqueBooks.size}")
        when (val marks = fetchBookmarks(apiKey, book.bookId)) {
          is WereadApiResult.UpgradeRequired ->
            return report(WereadPartStatus.IDLE, WereadPartStatus.UPGRADE, marks.message) to null
          is WereadApiResult.Ok -> allNotes += WereadGateway.parseHighlights(marks.body)
          is WereadApiResult.MissingKey ->
            return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, "尚未填写微信读书密钥") to null
          is WereadApiResult.Error, is WereadApiResult.Failed ->
            return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, "获取划线失败，未写入笔记") to null
        }
        var synckey: Long? = 0
        do {
          coroutineContext.ensureActive()
          when (val reviews = fetchReviews(apiKey, book.bookId, synckey)) {
            is WereadApiResult.UpgradeRequired ->
              return report(WereadPartStatus.IDLE, WereadPartStatus.UPGRADE, reviews.message) to null
            is WereadApiResult.Ok -> {
              val parsed = WereadGateway.parseReviewsPage(reviews.body)
              allNotes += parsed.items
              if (!parsed.hasMore) break
              val next = parsed.synckey
              if (next == null || next == synckey) {
                return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, "想法分页未前进，未写入笔记") to null
              }
              synckey = next
            }
            is WereadApiResult.MissingKey ->
              return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, "尚未填写微信读书密钥") to null
            is WereadApiResult.Error, is WereadApiResult.Failed ->
              return report(WereadPartStatus.IDLE, WereadPartStatus.FAILED, "获取想法失败，未写入笔记") to null
          }
        } while (true)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      return report(
        WereadPartStatus.IDLE,
        WereadPartStatus.FAILED,
        "同步笔记失败：${WereadGateway.redact(e.message ?: e.javaClass.simpleName)}",
      ) to null
    }

    val (items, counts) = WereadGateway.notesToDayCounts(account, allNotes, start, today, nowEpochSec)
    val merged = WereadGateway.mergeNoteDays(existingDays, existingNotes, account, start, today, items, counts, nowMs)
    val highlight = items.count { it.kind == "highlight" }
    val thought = items.count { it.kind == "thought" }
    return WereadSyncReport(
      durationStatus = WereadPartStatus.IDLE,
      notesStatus = WereadPartStatus.SUCCESS,
      notesItemsWritten = items.size,
      message = "已同步划线 $highlight 条、想法 $thought 条（按创建日，不含无日期书签）",
    ) to merged
  }

  private suspend fun call(apiKey: String, apiName: String, params: Map<String, Any?> = emptyMap()): WereadApiResult {
    val body = WereadGateway.buildBody(apiName, params)
    val authorization = "Bearer $apiKey"
    return try {
      pace()
      val raw = transport.post(endpoint, authorization, body)
      WereadGateway.parseResponse(raw.body, raw.httpCode).let { parsed ->
        if (parsed is WereadApiResult.Failed) {
          parsed.copy(message = WereadGateway.redact(parsed.message))
        } else parsed
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      WereadApiResult.Failed("网络请求失败：${WereadGateway.redact(e.message ?: e.javaClass.simpleName)}")
    }
  }

  private suspend fun pace() {
    val wait = paceMs - (System.currentTimeMillis() - lastCallAt)
    if (wait > 0) delay(wait)
    lastCallAt = System.currentTimeMillis()
  }

  private fun report(
    duration: WereadPartStatus,
    notes: WereadPartStatus,
    message: String,
    incomplete: List<String> = emptyList(),
  ) = WereadSyncReport(
    durationStatus = duration,
    notesStatus = notes,
    incompletePeriods = incomplete,
    message = WereadGateway.redact(message),
  )
}

fun todayInShanghai(nowEpochMs: Long = System.currentTimeMillis()): LocalDate =
  InstantOf(nowEpochMs)

private fun InstantOf(nowEpochMs: Long): LocalDate =
  java.time.Instant.ofEpochMilli(nowEpochMs).atZone(Zones.Shanghai).toLocalDate()
