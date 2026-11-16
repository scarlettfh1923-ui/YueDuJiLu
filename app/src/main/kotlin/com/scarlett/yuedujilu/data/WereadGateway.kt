package com.scarlett.yuedujilu.data

import com.scarlett.yuedujilu.domain.WereadDayRecord
import com.scarlett.yuedujilu.domain.WereadImport
import com.scarlett.yuedujilu.domain.WereadNoteItem
import com.scarlett.yuedujilu.domain.Zones
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * 微信读书官方 Agent Gateway（skill 1.0.5）请求构造与回包解析。
 * 文档：https://github.com/Tencent/WeChatReading（skills/SKILL.md, readdata.md, notes.md）
 * 不发明 endpoint，不把业务参数包进 params/data，不传 vid。
 */
object WereadGateway {
  const val ENDPOINT = "https://i.weread.qq.com/api/agent/gateway"
  const val SKILL_VERSION = "1.0.5"
  const val API_LIST = "/_list"
  const val API_READDATA = "/readdata/detail"
  const val API_NOTEBOOKS = "/user/notebooks"
  const val API_BOOKMARKS = "/book/bookmarklist"
  const val API_REVIEWS = "/review/list/mine"

  private val jsonPretty = Json { encodeDefaults = false }

  fun buildBody(apiName: String, params: Map<String, Any?> = emptyMap()): String {
    val obj = buildJsonObject {
      put("api_name", apiName)
      put("skill_version", SKILL_VERSION)
      for ((k, v) in params) {
        if (k == "api_name" || k == "skill_version" || k == "vid" || k == "params" || k == "data" || k == "body") continue
        when (v) {
          null -> Unit
          is Int -> put(k, v)
          is Long -> put(k, v)
          is Boolean -> put(k, v)
          is Number -> put(k, v.toLong())
          else -> {
            val text = v.toString()
            if (text.isNotEmpty()) put(k, text)
          }
        }
      }
    }
    return jsonPretty.encodeToString(JsonObject.serializer(), obj)
  }

  fun parseResponse(text: String, httpCode: Int): WereadApiResult {
    if (text.isBlank()) return WereadApiResult.Failed("空响应（HTTP $httpCode）")
    val root = try {
      Json.parseToJsonElement(text).jsonObject
    } catch (_: Exception) {
      return WereadApiResult.Failed("响应不是 JSON（HTTP $httpCode）")
    }
    val upgrade = root["upgrade_info"]
    if (upgrade != null && upgrade !is kotlinx.serialization.json.JsonNull) {
      val message = upgradeMessage(upgrade) ?: "技能需要升级后才能继续"
      return WereadApiResult.UpgradeRequired(message)
    }
    val err = root["errcode"]?.jsonPrimitive?.intOrNull
    if (err != null && err != 0) {
      val msg = root["errmsg"]?.jsonPrimitive?.contentOrNull
        ?: root["message"]?.jsonPrimitive?.contentOrNull
        ?: "请求失败"
      return WereadApiResult.Error(err, msg)
    }
    if (httpCode !in 200..299) {
      return WereadApiResult.Failed("HTTP $httpCode")
    }
    return WereadApiResult.Ok(root)
  }

  private fun upgradeMessage(element: JsonElement): String? {
    val obj = element as? JsonObject ?: return element.jsonPrimitive.contentOrNull
    return obj["message"]?.jsonPrimitive?.contentOrNull
      ?: obj["msg"]?.jsonPrimitive?.contentOrNull
  }

  fun accountPartition(apiKey: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(apiKey.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { b -> "%02x".format(b) }
  }

  fun shanghaiEpochSecond(date: LocalDate): Long =
    date.atStartOfDay(Zones.Shanghai).toEpochSecond()

  fun bucketDate(epochSec: Long): LocalDate =
    Instant.ofEpochSecond(epochSec).atZone(Zones.Shanghai).toLocalDate()

  fun monthsOverlapping(start: LocalDate, end: LocalDate): List<LocalDate> {
    var ym = YearMonth.from(start)
    val last = YearMonth.from(end)
    val out = mutableListOf<LocalDate>()
    while (!ym.isAfter(last)) {
      out += ym.atDay(1)
      ym = ym.plusMonths(1)
    }
    return out
  }

  fun yearsOverlapping(start: LocalDate, end: LocalDate): IntRange = start.year..end.year

  fun mondaysOverlapping(start: LocalDate, end: LocalDate): List<LocalDate> {
    var day = start.minusDays(((start.dayOfWeek.value - 1).toLong()))
    val out = mutableListOf<LocalDate>()
    while (!day.isAfter(end)) {
      out += day
      day = day.plusWeeks(1)
    }
    return out
  }

  /**
   * 从 /readdata/detail 提取日桶（秒）。
   * annually 的 readTimes 是月桶，不能当日记；优先 dailyReadTimes。
   * weekly/monthly 的 readTimes 按文档通常是日桶。
   * 无日明细时 complete=false，不得用 totalReadTime 冒充截断区间。
   */
  fun extractDailySeconds(mode: String, body: JsonObject): DailyBuckets {
    val dailyReadTimes = timestampMap(body["dailyReadTimes"])
    val readTimes = timestampMap(body["readTimes"])
    return when (mode) {
      "annually" -> {
        if (dailyReadTimes.isNotEmpty()) DailyBuckets(dailyReadTimes, complete = true, source = "dailyReadTimes")
        else DailyBuckets(emptyMap(), complete = false, source = "annually-no-daily")
      }
      "weekly", "monthly" -> {
        when {
          readTimes.isNotEmpty() -> DailyBuckets(readTimes, complete = true, source = "readTimes")
          dailyReadTimes.isNotEmpty() -> DailyBuckets(dailyReadTimes, complete = true, source = "dailyReadTimes")
          else -> DailyBuckets(emptyMap(), complete = false, source = "$mode-no-daily")
        }
      }
      else -> DailyBuckets(emptyMap(), complete = false, source = "unsupported-mode")
    }
  }

  fun filterDays(
    buckets: Map<Long, Long>,
    start: LocalDate,
    end: LocalDate,
  ): Map<LocalDate, Long> {
    val acc = linkedMapOf<LocalDate, Long>()
    for ((ts, sec) in buckets) {
      val date = bucketDate(ts)
      if (!date.isBefore(start) && !date.isAfter(end)) {
        acc[date] = (acc[date] ?: 0L) + sec.coerceAtLeast(0L)
      }
    }
    return acc
  }

  /** 完整周期内缺省的日视为 0 秒；不完整周期不补 0，以免覆盖已有精确数据。 */
  fun materializePeriodDays(
    periodStart: LocalDate,
    periodEnd: LocalDate,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    known: Map<LocalDate, Long>,
    complete: Boolean,
  ): Map<LocalDate, Long> {
    val from = maxOf(periodStart, rangeStart)
    val to = minOf(periodEnd, rangeEnd)
    if (from.isAfter(to)) return emptyMap()
    if (!complete) return known.filterKeys { !it.isBefore(from) && !it.isAfter(to) }
    val out = linkedMapOf<LocalDate, Long>()
    var d = from
    while (!d.isAfter(to)) {
      out[d] = known[d] ?: 0L
      d = d.plusDays(1)
    }
    return out
  }

  fun timestampMap(element: JsonElement?): Map<Long, Long> {
    val obj = element as? JsonObject ?: return emptyMap()
    val out = linkedMapOf<Long, Long>()
    for ((k, v) in obj) {
      val ts = k.toLongOrNull() ?: continue
      val sec = when (v) {
        is JsonPrimitive -> v.longOrNull ?: v.contentOrNull?.toLongOrNull()
        else -> null
      } ?: continue
      out[ts] = sec
    }
    return out
  }

  fun parseHasMore(element: JsonElement?): Boolean {
    val p = element?.jsonPrimitive ?: return false
    return p.booleanOrNull == true || p.intOrNull == 1 || p.contentOrNull == "1" || p.contentOrNull == "true"
  }

  fun parseNotebooksPage(body: JsonObject): NotebooksPage {
    val booksEl = body["books"]?.jsonArray
    val books = booksEl?.mapNotNull { el ->
      val o = el as? JsonObject ?: return@mapNotNull null
      val bookId = o["bookId"]?.jsonPrimitive?.contentOrNull
        ?: (o["book"] as? JsonObject)?.get("bookId")?.jsonPrimitive?.contentOrNull
        ?: return@mapNotNull null
      val sort = o["sort"]?.jsonPrimitive?.longOrNull ?: 0L
      NotebookSummary(bookId = bookId, sort = sort)
    }.orEmpty()
    return NotebooksPage(
      books = books,
      hasMore = parseHasMore(body["hasMore"]),
      lastSort = books.lastOrNull()?.sort,
    )
  }

  fun parseHighlights(body: JsonObject): List<ParsedNote> {
    val arr = body["updated"]?.jsonArray ?: return emptyList()
    return arr.mapNotNull { el ->
      val o = el as? JsonObject ?: return@mapNotNull null
      val id = o["bookmarkId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
      val bookId = o["bookId"]?.jsonPrimitive?.contentOrNull.orEmpty()
      val created = o["createTime"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
      ParsedNote(id = id, kind = "highlight", bookId = bookId, createdAtEpochSec = created)
    }
  }

  fun parseReviewsPage(body: JsonObject): ReviewsPage {
    val arr = body["reviews"]?.jsonArray ?: emptyList()
    val items = arr.mapNotNull { el ->
      val wrapper = el as? JsonObject ?: return@mapNotNull null
      val review = (wrapper["review"] as? JsonObject) ?: wrapper
      val id = review["reviewId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
      val bookId = review["bookId"]?.jsonPrimitive?.contentOrNull
        ?: wrapper["bookId"]?.jsonPrimitive?.contentOrNull
        ?: ""
      val created = review["createTime"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
      ParsedNote(id = id, kind = "thought", bookId = bookId, createdAtEpochSec = created)
    }
    val synckey = body["synckey"]?.jsonPrimitive?.longOrNull
    return ReviewsPage(items = items, hasMore = parseHasMore(body["hasMore"]), synckey = synckey)
  }

  fun notesToDayCounts(
    accountHash: String,
    notes: List<ParsedNote>,
    start: LocalDate,
    endInclusive: LocalDate,
    nowEpochSec: Long,
  ): Pair<List<WereadNoteItem>, Map<LocalDate, Pair<Int, Int>>> {
    val startSec = shanghaiEpochSecond(start)
    val items = mutableListOf<WereadNoteItem>()
    val seen = mutableSetOf<String>()
    for (n in notes) {
      if (n.createdAtEpochSec < startSec || n.createdAtEpochSec > nowEpochSec) continue
      val date = bucketDate(n.createdAtEpochSec)
      if (date.isAfter(endInclusive)) continue
      val key = "${n.kind}:${n.id}"
      if (!seen.add(key)) continue
      items += WereadNoteItem(
        accountHash = accountHash,
        itemId = n.id,
        kind = n.kind,
        bookId = n.bookId,
        date = WereadImport.format(date),
        createdAtEpochSec = n.createdAtEpochSec,
      )
    }
    val counts = linkedMapOf<LocalDate, Pair<Int, Int>>()
    for (item in items) {
      val date = WereadImport.parseDate(item.date) ?: continue
      val cur = counts[date] ?: (0 to 0)
      counts[date] = if (item.kind == "highlight") (cur.first + 1) to cur.second else cur.first to (cur.second + 1)
    }
    return items to counts
  }

  fun mergeDurationDays(
    existing: List<WereadDayRecord>,
    accountHash: String,
    incoming: Map<LocalDate, Long>,
    nowMs: Long,
  ): List<WereadDayRecord> {
    val byKey = existing.associateBy { it.accountHash to it.date }.toMutableMap()
    for ((date, sec) in incoming) {
      val key = accountHash to WereadImport.format(date)
      val old = byKey[key]
      byKey[key] = (old ?: WereadDayRecord(accountHash, key.second)).copy(
        durationSec = sec,
        durationComplete = true,
        updatedAtEpochMs = nowMs,
      )
    }
    return byKey.values.sortedBy { it.date }
  }

  fun mergeNoteDays(
    existingDays: List<WereadDayRecord>,
    existingNotes: List<WereadNoteItem>,
    accountHash: String,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    newItems: List<WereadNoteItem>,
    dayCounts: Map<LocalDate, Pair<Int, Int>>,
    nowMs: Long,
  ): Pair<List<WereadDayRecord>, List<WereadNoteItem>> {
    val keptNotes = existingNotes.filter { it.accountHash != accountHash }
    val otherAccountDays = existingDays.filter { it.accountHash != accountHash }
    val mine = existingDays.filter { it.accountHash == accountHash }.associateBy { it.date }.toMutableMap()
    var d = rangeStart
    while (!d.isAfter(rangeEnd)) {
      val iso = WereadImport.format(d)
      val pair = dayCounts[d] ?: (0 to 0)
      val old = mine[iso] ?: WereadDayRecord(accountHash, iso)
      mine[iso] = old.copy(
        highlightCount = pair.first,
        thoughtCount = pair.second,
        notesComplete = true,
        updatedAtEpochMs = nowMs,
      )
      d = d.plusDays(1)
    }
    return (otherAccountDays + mine.values).sortedBy { it.date } to (keptNotes + newItems)
  }

  fun redact(text: String): String {
    var out = text
    out = WRK_KEY.replace(out, "wrk-***")
    out = BEARER.replace(out, "Bearer ***")
    out = out.replace(ENDPOINT, "[weread-gateway]")
    return out
  }

  private val WRK_KEY = Regex("wrk-[A-Za-z0-9_-]+")
  private val BEARER = Regex("Bearer\\s+\\S+", RegexOption.IGNORE_CASE)
}

data class DailyBuckets(
  val secondsByTimestamp: Map<Long, Long>,
  val complete: Boolean,
  val source: String,
)

data class NotebookSummary(val bookId: String, val sort: Long)
data class NotebooksPage(val books: List<NotebookSummary>, val hasMore: Boolean, val lastSort: Long?)
data class ParsedNote(val id: String, val kind: String, val bookId: String, val createdAtEpochSec: Long)
data class ReviewsPage(val items: List<ParsedNote>, val hasMore: Boolean, val synckey: Long?)

sealed class WereadApiResult {
  data class Ok(val body: JsonObject) : WereadApiResult()
  data class Error(val errcode: Int, val message: String) : WereadApiResult()
  data class UpgradeRequired(val message: String) : WereadApiResult()
  data class Failed(val message: String) : WereadApiResult()
  data object MissingKey : WereadApiResult()
}

data class WereadSyncReport(
  val durationStatus: com.scarlett.yuedujilu.domain.WereadPartStatus,
  val notesStatus: com.scarlett.yuedujilu.domain.WereadPartStatus,
  val durationDaysWritten: Int = 0,
  val notesItemsWritten: Int = 0,
  val incompletePeriods: List<String> = emptyList(),
  val message: String,
)
