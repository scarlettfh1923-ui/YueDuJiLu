package com.scarlett.yuedujilu.domain
import com.scarlett.yuedujilu.SourceFilter
import com.scarlett.yuedujilu.data.AppSnapshot
import com.scarlett.yuedujilu.data.AppSettings
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class StatsDetailsTest {
 private val today=LocalDate.of(2026,11,17)
 private fun at(day:LocalDate,h:Int=12)=day.atTime(h,0).atZone(Zones.Shanghai).toInstant().toEpochMilli()
 private val now=at(today,18)
 private val book=Book("b","真实测试夹具",pageCount=500,currentPage=475,status=BookStatus.READING,createdAtEpochMs=now,updatedAtEpochMs=now)
 private fun session(date:LocalDate,ms:Long=60000)=ReadingSession(id=date.toString(),bookId=book.id,startedAtEpochMs=at(date),endedAtEpochMs=at(date)+ms)
 private fun wr(date:LocalDate,sec:Long?,complete:Boolean=true,update:Long=1)=WereadDayRecord("acct",date.toString(),sec,complete,updatedAtEpochMs=update)
 private fun snapshot(days:List<WereadDayRecord> = emptyList(),sessions:List<ReadingSession> = emptyList())=AppSnapshot(books=listOf(book),sessions=sessions,wereadDays=days,settings=AppSettings(wereadImportStartDate="2026-11-01",wereadLastSyncAt="2026-11-17T01:00:00Z"))
 private fun project(s:AppSnapshot,source:SourceFilter=SourceFilter.ALL,period:StatsPeriod=StatsPeriod.MONTH,anchor:LocalDate=today)=StatsDetails.project(s,period,anchor,today,now,source,"acct")
 @Test fun unionAndPartialDayDoNotInventCompleteDuration(){val d=project(snapshot(listOf(wr(today,10,false)),listOf(session(today))));assertEquals(1,d.knownDays);assertEquals(60000L,d.knownMs);assertFalse(d.durationComplete);assertTrue(d.averageMs is MetricValue.Missing);assertEquals(ReadingDayState.READ,d.days.last().state)}
 @Test fun newerDuplicateZeroReplacesOldPositive(){val d=project(snapshot(listOf(wr(today,300,true,1),wr(today,0,true,2))));assertEquals(0,d.knownDays);assertEquals(0L,d.wereadMs);assertEquals(ReadingDayState.UNKNOWN,d.days.last().state)}
 @Test fun explicitZeroNeedsAllSelectedSources(){val s=snapshot(listOf(wr(today,0))).copy(confirmedPhysicalUnreadDates=setOf(today.toString()));assertEquals(ReadingDayState.UNREAD,project(s).days.last().state);assertEquals(ReadingDayState.UNKNOWN,project(s.copy(confirmedPhysicalUnreadDates=emptySet())).days.last().state)}
 @Test fun nullCompleteDurationIsStillUnknown(){val d=project(snapshot(listOf(wr(today,null))),SourceFilter.WEREAD);assertNull(d.days.last().wereadMs);assertFalse(d.durationComplete);assertTrue(d.averageMs is MetricValue.Missing)}
 @Test fun naturalAndReadingAveragesUseDifferentDenominators(){val dates=(1..17).map{LocalDate.of(2026,11,it)};val d=project(snapshot(dates.map{wr(it,if(it==today)1700 else 0)}),SourceFilter.WEREAD);assertEquals(100000L,(d.averageMs as MetricValue.Known).value);assertEquals(1700000L,(d.readingDayAverageMs as MetricValue.Known).value)}
 @Test fun unknownEndpointIsNotZeroStreak(){val d=project(snapshot(listOf(wr(today.minusDays(1),60))),SourceFilter.WEREAD);assertTrue(d.runMetric is MetricValue.Missing);assertEquals(1L,d.longestRun!!.days)}
 @Test fun unknownBreaksConfirmedRunWithoutBecomingUnread(){val d=project(snapshot(listOf(wr(today,60),wr(today.minusDays(1),60),wr(today.minusDays(3),60))),SourceFilter.WEREAD);assertEquals(2L,d.currentRun!!.days);assertEquals(2L,d.longestRun!!.days);assertEquals(today.minusDays(1),d.currentRun!!.start)}
 @Test fun crossMonthSplitsAndPreservesCurrentProgress(){val start=LocalDate.of(2026,10,31).atTime(23,30).atZone(Zones.Shanghai).toInstant().toEpochMilli();val d=project(snapshot(sessions=listOf(ReadingSession(id="cross",bookId="b",startedAtEpochMs=start,endedAtEpochMs=start+60*60000))),SourceFilter.PHYSICAL);assertEquals(30*60000L,d.physicalMs);assertEquals(475,d.physicalBooks.single().book.currentPage)}
 @Test fun explicitFinishWithoutTimerAppearsInIntervalList(){val s=snapshot().copy(books=listOf(book.copy(status=BookStatus.FINISHED,finishedAtEpochMs=at(today))));val d=project(s,SourceFilter.PHYSICAL);assertTrue(d.physicalBooks.single().finishedInSpan);assertEquals(475,d.physicalBooks.single().book.currentPage)}
 @Test fun notesMissingFieldsAreNotInventedZeros(){val d=project(snapshot(listOf(wr(today,60).copy(notesComplete=true,highlightCount=null,thoughtCount=null))));assertNull(d.highlights);assertNull(d.thoughts);assertTrue(d.notesIncomplete)}
 @Test fun sourceFilterExcludesPhysicalBooksFromWechatDetail(){val d=project(snapshot(sessions=listOf(session(today))),SourceFilter.WEREAD);assertTrue(d.physicalBooks.isEmpty());assertEquals(0L,d.physicalMs)}
 @Test fun futureIntervalHasNoAverageOrReadingDays(){val d=project(snapshot(),SourceFilter.PHYSICAL,anchor=today.plusMonths(1));assertTrue(d.days.isEmpty());assertTrue(d.averageMs is MetricValue.Missing)}
 @Test fun batchCalendarMatchesSingleDayIncludingFuture(){val s=snapshot(listOf(wr(today,60)),listOf(session(today)));val dates=ReadingCalendar.range(today.minusDays(2),today.plusDays(1),today,now,SourceFilter.ALL,s.books,s.sessions,s.wereadDays,"acct");dates.forEach{assertEquals(it,ReadingCalendar.day(it.date,today,now,SourceFilter.ALL,s.books,s.sessions,s.wereadDays,"acct"))};assertEquals(ReadingDayState.FUTURE,dates.last().state)}
 @Test fun comparisonFormatsMillisecondsAsChineseDuration(){assertEquals("4小时59分44秒",StatsOverview.durationText(17984000));val old=session(LocalDate.of(2026,10,10),17984000);val report=StatsOverview.overview(listOf(old),listOf(book),emptyList(),StatsPeriod.MONTH,today,today,now,SourceFilter.PHYSICAL,null,null,null);val c=(report.durationMs as MetricValue.Known).compare!!;assertTrue(c.contains("-4小时59分44秒"));assertFalse(c.contains("17984000"));assertEquals(LocalDate.of(2026,10,17),report.previousSpan!!.end)}
 @Test fun positiveAndZeroComparisonsKeepUnitsAndNeverExposeRawMillis(){
  fun report(s:List<ReadingSession>)=StatsOverview.overview(s,listOf(book),emptyList(),StatsPeriod.MONTH,today,today,now,SourceFilter.PHYSICAL,null,null,null)
  val plus=(report(listOf(session(today,17984000))).durationMs as MetricValue.Known).compare!!;assertTrue(plus.contains("+4小时59分44秒"))
  val zero=(report(emptyList()).durationMs as MetricValue.Known).compare!!;assertTrue(zero.contains("持平"))
  assertTrue((report(listOf(session(today))).booksRead as MetricValue.Known).compare!!.contains("+1本"))
 }

}
