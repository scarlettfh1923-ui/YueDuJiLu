package com.scarlett.yuedujilu.domain

import com.scarlett.yuedujilu.SourceFilter
import java.time.LocalDate

/** Shared across every calendar and selected-day detail. Missing is never an unread observation. */
enum class ReadingDayState { READ, UNREAD, UNKNOWN, FUTURE }
data class ReadingDay(
  val date: LocalDate, val state: ReadingDayState,
  val physicalMs: Long, val wereadMs: Long?,
  val physicalComplete: Boolean, val wereadComplete: Boolean,
  val physicalSessions: List<ReadingSession>,
) {
  val confirmedDurationMs: Long get() = physicalMs + (wereadMs ?: 0L)
}
object ReadingCalendar {
  fun day(date: LocalDate, today: LocalDate, now: Long, source: SourceFilter,
    books: List<Book>, sessions: List<ReadingSession>, wereadDays: List<WereadDayRecord>,
    accountHash: String?, confirmedPhysicalUnreadDates: Set<String> = emptySet(),
  ): ReadingDay {
    val wereadIds=books.filter { it.source==BookSource.WEREAD }.map { it.id }.toSet()
    val daySessions=sessions.filter { it.bookId !in wereadIds && (ReadingMath.splitDurationByDay(it,now,Zones.Shanghai)[date]?:0)>0 }
    val physical=daySessions.sumOf { ReadingMath.splitDurationByDay(it,now,Zones.Shanghai)[date]?:0L }
    // Dedup by account/date, newest wins. Never sum repeated imports.
    val wr=wereadDays.filter { !accountHash.isNullOrBlank() && it.accountHash==accountHash && it.date==date.toString() }.maxByOrNull { it.updatedAtEpochMs }
    val wComplete=wr?.durationComplete==true && wr.durationSec!=null && wr.durationSec>=0
    val w=if(wComplete) wr!!.durationSec!!*1000L else null
    val pComplete=date.toString() in confirmedPhysicalUnreadDates && physical==0L
    val includeP=source!=SourceFilter.WEREAD
    val includeW=source!=SourceFilter.PHYSICAL
    val hasReading=(includeP && physical>0)||(includeW && (wr?.durationSec?:0)>0)
    val complete=(!includeP || pComplete) && (!includeW || wComplete)
    val state=when {
      date.isAfter(today)->ReadingDayState.FUTURE
      hasReading->ReadingDayState.READ
      complete->ReadingDayState.UNREAD
      else->ReadingDayState.UNKNOWN
    }
    return ReadingDay(date,state,if(includeP)physical else 0,if(includeW)w else null,pComplete,wComplete,if(includeP)daySessions else emptyList())
  }
}
