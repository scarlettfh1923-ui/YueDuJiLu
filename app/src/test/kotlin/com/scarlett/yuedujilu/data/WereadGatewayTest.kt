package com.scarlett.yuedujilu.data

import com.scarlett.yuedujilu.domain.WereadDayRecord
import com.scarlett.yuedujilu.domain.WereadImport
import com.scarlett.yuedujilu.ui.TabBarSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WereadGatewayTest {
  @Test
  fun buildBodyIsFlatWithVersionAndNoVid() {
    val raw = WereadGateway.buildBody("/readdata/detail", mapOf("mode" to "monthly", "baseTime" to 1704067200L, "vid" to "should-drop"))
    val obj = Json.parseToJsonElement(raw).jsonObject
    assertEquals("/readdata/detail", obj["api_name"]?.jsonPrimitive?.content)
    assertEquals(WereadGateway.SKILL_VERSION, obj["skill_version"]?.jsonPrimitive?.content)
    assertEquals("monthly", obj["mode"]?.jsonPrimitive?.content)
    assertFalse(obj.containsKey("params"))
    assertFalse(obj.containsKey("data"))
    assertFalse(obj.containsKey("vid"))
    assertTrue(obj["baseTime"] != null)
  }

  @Test
  fun parseErrorAndUpgrade() {
    val err = WereadGateway.parseResponse("""{"errcode":401,"errmsg":"unauthorized"}""", 200)
    assertTrue(err is WereadApiResult.Error)
    val up = WereadGateway.parseResponse("""{"errcode":0,"upgrade_info":{"message":"请升级到 1.0.6"}}""", 200)
    assertTrue(up is WereadApiResult.UpgradeRequired)
    assertEquals("请升级到 1.0.6", (up as WereadApiResult.UpgradeRequired).message)
  }

  @Test
  fun annuallyMonthlyBucketsAreNotTreatedAsDaily() {
    // 2024-01-01 and 2024-02-01 00:00 Asia/Shanghai
    val jan = WereadGateway.shanghaiEpochSecond(LocalDate.of(2024, 1, 1))
    val feb = WereadGateway.shanghaiEpochSecond(LocalDate.of(2024, 2, 1))
    val body = Json.parseToJsonElement("""{"totalReadTime":9000,"readTimes":{"$jan":5000,"$feb":4000}}""").jsonObject
    val extracted = WereadGateway.extractDailySeconds("annually", body)
    assertFalse(extracted.complete)
    assertTrue(extracted.secondsByTimestamp.isEmpty())
  }

  @Test
  fun monthlyDailyBucketsAreFilteredByRange() {
    val start = LocalDate.of(2024, 1, 31)
    val d30 = WereadGateway.shanghaiEpochSecond(LocalDate.of(2024, 1, 30))
    val d31 = WereadGateway.shanghaiEpochSecond(LocalDate.of(2024, 1, 31))
    val d1 = WereadGateway.shanghaiEpochSecond(LocalDate.of(2024, 2, 1))
    val body = Json.parseToJsonElement(
      """{"readTimes":{"$d30":60,"$d31":120,"$d1":180},"totalReadTime":360}"""
    ).jsonObject
    val extracted = WereadGateway.extractDailySeconds("monthly", body)
    assertTrue(extracted.complete)
    val filtered = WereadGateway.filterDays(extracted.secondsByTimestamp, start, LocalDate.of(2024, 2, 1))
    assertEquals(setOf(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 1)), filtered.keys)
    assertEquals(120L, filtered[LocalDate.of(2024, 1, 31)])
    // 不得把整月 totalReadTime 当成截断范围
    assertTrue(filtered.values.sum() != 360L)
  }

  @Test
  fun last7DaysOnlyInitializesAndDoesNotRoll() {
    val today = LocalDate.of(2026, 11, 17)
    val first = WereadImport.applyLast7DaysShortcut(today, null)
    assertEquals(LocalDate.of(2026, 11, 11), first)
    val later = WereadImport.applyLast7DaysShortcut(today.plusDays(10), first)
    assertEquals(first, later)
  }

  @Test
  fun upsertDoesNotAccumulateAndFailedMergeKeepsOld() {
    val a = WereadGateway.accountPartition("wrk-fixture-aaaa")
    val b = WereadGateway.accountPartition("wrk-fixture-bbbb")
    assertTrue(a != b)
    val old = listOf(WereadDayRecord(a, "2026-11-11", durationSec = 100, durationComplete = true))
    val merged = WereadGateway.mergeDurationDays(old, a, mapOf(LocalDate.of(2026, 11, 11) to 40L), 1L)
    assertEquals(1, merged.size)
    assertEquals(40L, merged[0].durationSec)
    // 失败不写库：调用方不传入新数据时保持原值
    assertEquals(100L, old[0].durationSec)
    val mixed = WereadGateway.mergeDurationDays(old + WereadDayRecord(b, "2026-11-11", durationSec = 9, durationComplete = true), a, mapOf(LocalDate.of(2026, 11, 12) to 5L), 2L)
    assertTrue(mixed.any { it.accountHash == b && it.durationSec == 9L })
    assertTrue(mixed.any { it.accountHash == a && it.date == "2026-11-11" && it.durationSec == 100L })
  }

  @Test
  fun notesSplitByCreateTimeAndDedup() {
    val start = LocalDate.of(2026, 11, 11)
    val today = LocalDate.of(2026, 11, 17)
    val t1 = WereadGateway.shanghaiEpochSecond(LocalDate.of(2026, 11, 12)) + 3600
    val t2 = WereadGateway.shanghaiEpochSecond(LocalDate.of(2026, 11, 12)) + 7200
    val notes = listOf(
      ParsedNote("h1", "highlight", "book-1", t1),
      ParsedNote("h1", "highlight", "book-1", t1),
      ParsedNote("r1", "thought", "book-1", t2),
    )
    val (_, counts) = WereadGateway.notesToDayCounts("abc", notes, start, today, t2 + 10)
    assertEquals(1 to 1, counts[LocalDate.of(2026, 11, 12)])
  }

  @Test
  fun notebooksPaginationAndRedaction() {
    val page = WereadGateway.parseNotebooksPage(
      Json.parseToJsonElement("""{"hasMore":1,"books":[{"bookId":"b1","sort":111},{"bookId":"b2","sort":222}]}""").jsonObject
    )
    assertTrue(page.hasMore)
    assertEquals(222L, page.lastSort)
    val redacted = WereadGateway.redact("Authorization: Bearer wrk-supersecretvalue and https://i.weread.qq.com/api/agent/gateway")
    assertFalse(redacted.contains("wrk-supersecretvalue"))
    assertFalse(redacted.contains("Bearer wrk-"))
    assertFalse(redacted.contains("i.weread.qq.com"))
  }

  @Test
  fun tabBarContractIsEqualAndAlwaysLabeled() {
    assertEquals(3, TabBarSpec.ITEM_COUNT)
    assertTrue(TabBarSpec.ALWAYS_SHOW_LABEL)
    assertTrue(TabBarSpec.ICON_ABOVE_LABEL)
    assertTrue(TabBarSpec.SELECTED_FILLS_CELL)
    assertEquals(TabBarSpec.itemWeight() * 3f, 1f, 0.0001f)
    assertTrue(TabBarSpec.isHorizontallySymmetric(20, 20))
    assertFalse(TabBarSpec.isHorizontallySymmetric(20, 16))
    assertTrue(TabBarSpec.selectedFillCoversCell(120f, 120f))
    assertTrue(TabBarSpec.labelsAlwaysVisible(selectedShowsLabel = true, unselectedShowsLabel = true))
    assertFalse(TabBarSpec.labelsAlwaysVisible(true, false))
  }
}

class FakeWereadTransportTest {
  @Test
  fun testConnectionUsesListAndDoesNotWriteOnFailure() = kotlinx.coroutines.runBlocking {
    var posted = ""
    val transport = WereadTransport { _, _, body ->
      posted = body
      WereadTransportResponse(200, """{"errcode":500,"errmsg":"boom"}""")
    }
    val client = WereadClient(transport = transport, paceMs = 0)
    val result = client.testConnection("wrk-fixture-only")
    assertTrue(result is WereadApiResult.Error)
    assertTrue(posted.contains("\"api_name\":\"/_list\""))
    assertTrue(posted.contains("\"skill_version\":\"1.0.5\""))

    val today = LocalDate.of(2026, 11, 17)
    val (report, written) = client.syncDailyDuration(
      apiKey = "wrk-fixture-only",
      start = today.minusDays(6),
      today = today,
      existing = listOf(WereadDayRecord("keep", "2026-11-11", durationSec = 77, durationComplete = true)),
      nowMs = 1L,
    )
    assertEquals(com.scarlett.yuedujilu.domain.WereadPartStatus.FAILED, report.durationStatus)
    assertTrue(written == null)
  }
}
