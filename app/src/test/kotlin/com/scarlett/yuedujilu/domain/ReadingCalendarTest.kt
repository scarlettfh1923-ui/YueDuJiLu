package com.scarlett.yuedujilu.domain

import com.scarlett.yuedujilu.SourceFilter
import com.scarlett.yuedujilu.data.*
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDate
import kotlin.io.path.createTempDirectory

class ReadingCalendarTest {
  private val today=LocalDate.of(2026,11,17)
  private val now=today.atTime(18,0).atZone(Zones.Shanghai).toInstant().toEpochMilli()
  private fun day(source:SourceFilter=SourceFilter.ALL,rec:WereadDayRecord?=null,confirmed:Set<String> = emptySet(),date:LocalDate=today)=ReadingCalendar.day(date,today,now,source,emptyList(),emptyList(),listOfNotNull(rec),"fixture",confirmed)
  @Test fun absenceIsUnknownForEverySource() { SourceFilter.entries.forEach { assertEquals(ReadingDayState.UNKNOWN,day(it).state) } }
  @Test fun explicitCompleteWechatZeroIsUnreadOnlyForThatSource() {
    val rec=WereadDayRecord("fixture",today.toString(),0,true)
    assertEquals(ReadingDayState.UNREAD,day(SourceFilter.WEREAD,rec).state)
    assertEquals(ReadingDayState.UNKNOWN,day(SourceFilter.ALL,rec).state)
    assertEquals(ReadingDayState.UNREAD,day(SourceFilter.ALL,rec,setOf(today.toString())).state)
  }
  @Test fun nullAndPartialZeroAreUnknown() {
    assertEquals(ReadingDayState.UNKNOWN,day(SourceFilter.WEREAD,WereadDayRecord("fixture",today.toString(),null,true)).state)
    assertEquals(ReadingDayState.UNKNOWN,day(SourceFilter.WEREAD,WereadDayRecord("fixture",today.toString(),0,false)).state)
  }
  @Test fun futureWinsEvenWithBadImportedRecord() { assertEquals(ReadingDayState.FUTURE,day(SourceFilter.WEREAD,WereadDayRecord("fixture",today.plusDays(1).toString(),60,true),date=today.plusDays(1)).state) }
  @Test fun positivePartialRecordProvesReadingButNotCompleteDuration() {
    val d=day(SourceFilter.WEREAD,WereadDayRecord("fixture",today.toString(),60,false))
    assertEquals(ReadingDayState.READ,d.state);assertFalse(d.wereadComplete)
  }
  @Test fun newestUpsertWinsNeverAccumulates() {
    val rs=listOf(WereadDayRecord("fixture",today.toString(),60,true,updatedAtEpochMs=1),WereadDayRecord("fixture",today.toString(),120,true,updatedAtEpochMs=2))
    val d=ReadingCalendar.day(today,today,now,SourceFilter.WEREAD,emptyList(),emptyList(),rs,"fixture")
    assertEquals(120_000L,d.wereadMs)
  }
  @Test fun physicalPositiveOverridesOldUnreadConfirmation() {
    val b=Book("b","真实记录",createdAtEpochMs=now,updatedAtEpochMs=now)
    val s=ReadingSession("s","b",startedAtEpochMs=now-60_000,endedAtEpochMs=now)
    val d=ReadingCalendar.day(today,today,now,SourceFilter.ALL,listOf(b),listOf(s),emptyList(),null,setOf(today.toString()))
    assertEquals(ReadingDayState.READ,d.state);assertEquals(60_000L,d.physicalMs)
  }
  @Test fun defaultCeladonDoesNotReplaceExplicitOldTheme() = runBlocking {
    assertEquals(ThemeId.PINE_GREEN,AppSettings().themeId)
    val file=File(createTempDirectory("yuedu-migration").toFile(),"library.json")
    file.writeText("""{"settings":{"themeId":"MIST_PLUM"}}""")
    val repo=LibraryRepository(JsonLibraryStore(file));assertEquals(ThemeId.MIST_PLUM,repo.state.value.settings.themeId)
    repo.confirmPhysicalUnread(today.toString(),true)
    assertTrue(today.toString() in LibraryRepository(JsonLibraryStore(file)).state.value.confirmedPhysicalUnreadDates)
    repo.confirmPhysicalUnread(today.toString(),false);assertTrue(repo.state.value.confirmedPhysicalUnreadDates.isEmpty())
  }
  @Test fun partialPositiveDayAgreesWithCombinedStatistics() {
    val rec=WereadDayRecord("fixture",today.toString(),60,false)
    val r=StatsOverview.overview(emptyList(),emptyList(),listOf(rec),StatsPeriod.MONTH,today,today,now,SourceFilter.ALL,"fixture",today.withDayOfMonth(1),today)
    assertEquals(ReadingDayState.READ,day(SourceFilter.ALL,rec).state)
    assertEquals(1L,(r.days as MetricValue.Known).value)
    assertTrue(r.dailyAverageMs is MetricValue.Missing)
  }
  @Test fun completeCurrentWindowCannotInventUncoveredPreviousComparison() {
    val records=(1..17).map { WereadDayRecord("fixture",today.withDayOfMonth(it).toString(),if(it==17)60 else 0,true) }
    val r=StatsOverview.overview(emptyList(),emptyList(),records,StatsPeriod.MONTH,today,today,now,SourceFilter.ALL,"fixture",today.withDayOfMonth(1),today)
    assertEquals(60_000L,(r.durationMs as MetricValue.Known).value)
    assertTrue((r.durationMs as MetricValue.Known).compare?.contains("暂不对比")==true)
  }

}
