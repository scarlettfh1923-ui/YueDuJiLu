package com.scarlett.yuedujilu.domain

import com.scarlett.yuedujilu.SourceFilter
import com.scarlett.yuedujilu.data.AppSnapshot
import java.time.LocalDate
import java.time.Instant
import java.time.temporal.ChronoUnit

/** UI and every drill-down share the same selected-period projection, never example values. */
enum class StatsMetric(val label:String) { DURATION("阅读时长"),DAYS("阅读天数"),PHYSICAL("实体书"),EBOOK("电子书"),STREAK("连续阅读"),AVERAGE("日均阅读") }
data class BookInterval(val book:Book,val durationMs:Long,val finishedInSpan:Boolean)
data class ReadingRun(val start:LocalDate,val end:LocalDate) { val days:Long get()=ChronoUnit.DAYS.between(start,end)+1 }
data class StatsDetailData(val span:DateSpan,val days:List<ReadingDay>,val physicalBooks:List<BookInterval>,
 val physicalMs:Long,val wereadMs:Long,val durationComplete:Boolean,val knownDays:Int,
 val currentRun:ReadingRun?,val longestRun:ReadingRun?,val runMetric:MetricValue,
 val averageMs:MetricValue,val readingDayAverageMs:MetricValue,val highlights:Int?,val thoughts:Int?,val notesIncomplete:Boolean,
 val includesPhysical:Boolean,val includesWeread:Boolean,val wereadConnected:Boolean) {
 val knownMs:Long get()=physicalMs+wereadMs
 val unknownDays:Int get()=days.count { it.state==ReadingDayState.UNKNOWN }
}
object StatsDetails {
 fun project(snapshot:AppSnapshot,period:StatsPeriod,anchor:LocalDate,today:LocalDate,now:Long,source:SourceFilter,hash:String?):StatsDetailData {
  val includeP=source!=SourceFilter.WEREAD;val includeW=source!=SourceFilter.PHYSICAL
  val settings=snapshot.settings
  val wStart=WereadImport.parseDate(settings.wereadImportStartDate)
  val wEnd=StatsOverview.parseSyncDate(settings.wereadLastSyncAt)
  val connected=!hash.isNullOrBlank()&&wStart!=null&&wEnd!=null&&!wEnd.isBefore(wStart)
  val records=if(!connected) emptyList() else snapshot.wereadDays.filter { it.accountHash==hash &&
   WereadImport.parseDate(it.date)?.let { d->!d.isBefore(wStart)&&!d.isAfter(wEnd) }==true }
   .groupBy { it.date }.values.mapNotNull { it.maxByOrNull { r->r.updatedAtEpochMs } }
  val byBook=snapshot.books.associateBy { it.id }
  val perSession=snapshot.sessions.filter { byBook[it.bookId]?.source!=BookSource.WEREAD }.associateWith { ReadingMath.splitDurationByDay(it,now,Zones.Shanghai) }
  val first=listOfNotNull(if(includeP)perSession.values.flatMap { it.keys }.minOrNull() else null,if(includeW&&connected)wStart else null).minOrNull()?:today
  val requested=StatsOverview.resolveSpan(period,anchor,today)
  val start=requested?.start?:first;val end=minOf(requested?.end?:today,today)
  val span=DateSpan(start,maxOf(start,end))
  val days=ReadingCalendar.range(start,end,today,now,source,snapshot.books,snapshot.sessions,records,hash,snapshot.confirmedPhysicalUnreadDates)
  val durationByBook=mutableMapOf<String,Long>()
  if(includeP)perSession.forEach { (session,values)->
   val ms=values.filterKeys{it in span&&!it.isAfter(end)}.values.sum()
   if(ms>0)durationByBook[session.bookId]=(durationByBook[session.bookId]?:0L)+ms
  }
  val physical=if(!includeP)emptyList() else snapshot.books.filter{it.source!=BookSource.WEREAD}.mapNotNull { book->
   val duration=durationByBook[book.id]?:0L
   val finish=book.status==BookStatus.FINISHED&&book.finishedAtEpochMs?.let { Instant.ofEpochMilli(it).atZone(Zones.Shanghai).toLocalDate() in span }==true
   if(duration>0||finish)BookInterval(book,duration,finish) else null
  }.sortedByDescending{it.durationMs}
  val pMs=days.sumOf{it.physicalMs};val wMs=days.sumOf{it.wereadMs?:0L}
  val full=days.isNotEmpty()&&(!includeW || (!connected&&includeP) || (connected&&days.all{it.wereadComplete}))
  val denom=days.size
  val total=pMs+wMs
  val average=if(full&&denom>0)MetricValue.Known(total/denom,"分母为已过的${denom}个自然日，含未阅读日；${if(includeW&&!connected)"微信未连接，仅按本机实体记录计算" else "仅按已保存/同步记录计算"}") else MetricValue.Missing("来源覆盖不完整，暂不计算日均")
  val readCount=days.count{it.state==ReadingDayState.READ}
  val readAverage=if(full&&readCount>0)MetricValue.Known(total/readCount,"已确认总时长÷${readCount}个阅读日；不等同自然日日均") else MetricValue.Missing(if(readCount==0)"没有可作分母的阅读日" else "时长覆盖不全，不用部分合计伪算阅读日均")
  val runs=mutableListOf<ReadingRun>();var runStart:LocalDate?=null
  for(day in days) {
   if(day.state==ReadingDayState.READ) { if(runStart==null)runStart=day.date }
   else if(runStart!=null){runs+=ReadingRun(runStart,day.date.minusDays(1));runStart=null}
  }
  if(runStart!=null)runs+=ReadingRun(runStart,days.last().date)
  val current=runs.lastOrNull()?.takeIf{it.end==end}
  val longest=runs.maxByOrNull{it.days}
  val runMetric=when {
   days.isEmpty()->MetricValue.Missing("区间尚无已过日期")
   days.last().state==ReadingDayState.UNKNOWN->MetricValue.Missing("末日来源未知，暂不判定当前连续")
   current!=null->MetricValue.Known(current.days,if(days.any{it.state==ReadingDayState.UNKNOWN})"已确认连续天数；未知日期中止回溯，不视为未读" else "所选区间内截至末日的连续记录")
   else->MetricValue.Known(0,"末日有明确未读依据")
  }
  val noteRecords=records.filter{WereadImport.parseDate(it.date)?.let{d->d in span&&!d.isAfter(end)}==true}
  val validNotes=noteRecords.filter{it.notesComplete&&it.highlightCount!=null&&it.thoughtCount!=null}
  val notesPartial=noteRecords.size!=validNotes.size||(includeW&&noteRecords.size<denom)
  return StatsDetailData(span,days,physical,pMs,wMs,full,readCount,current,longest,runMetric,average,readAverage,
   if(includeW&&validNotes.isNotEmpty())validNotes.sumOf{it.highlightCount!!} else null,
   if(includeW&&validNotes.isNotEmpty())validNotes.sumOf{it.thoughtCount!!} else null,notesPartial,includeP,includeW,connected)
 }
}
