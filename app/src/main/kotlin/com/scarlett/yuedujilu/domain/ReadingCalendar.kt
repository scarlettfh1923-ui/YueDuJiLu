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
    return range(date,date,today,now,source,books,sessions,wereadDays,accountHash,confirmedPhysicalUnreadDates).single()
  }
  /** Split each physical session once, not once per calendar cell or day of a long interval. */
  fun range(start:LocalDate,end:LocalDate,today:LocalDate,now:Long,source:SourceFilter,
    books:List<Book>,sessions:List<ReadingSession>,wereadDays:List<WereadDayRecord>,accountHash:String?,
    confirmedPhysicalUnreadDates:Set<String> = emptySet()):List<ReadingDay> {
    if(start.isAfter(end))return emptyList()
    val wereadIds=books.filter{it.source==BookSource.WEREAD}.map{it.id}.toSet()
    val perDay=mutableMapOf<LocalDate,MutableList<ReadingSession>>()
    val totals=mutableMapOf<LocalDate,Long>()
    sessions.filter{it.bookId !in wereadIds}.forEach { session->
      ReadingMath.splitDurationByDay(session,now,Zones.Shanghai).forEach { (date,ms)->
        if(ms>0 && !date.isBefore(start)&&!date.isAfter(end)) {
          perDay.getOrPut(date){mutableListOf()}.add(session);totals[date]=(totals[date]?:0L)+ms
        }
      }
    }
    val byDate=wereadDays.filter{!accountHash.isNullOrBlank()&&it.accountHash==accountHash}.groupBy{it.date}.mapValues{it.value.maxByOrNull{r->r.updatedAtEpochMs}}
    return generateSequence(start){it.plusDays(1)}.takeWhile{!it.isAfter(end)}.map { date->
    val daySessions=perDay[date].orEmpty()
    val physical=totals[date]?:0L
    val wr=byDate[date.toString()]
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
    ReadingDay(date,state,if(includeP)physical else 0,if(includeW)w else null,pComplete,wComplete,if(includeP)daySessions else emptyList())
    }.toList()
  }
}
