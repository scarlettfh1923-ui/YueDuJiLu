package com.scarlett.yuedujilu.ui.common

import com.scarlett.yuedujilu.domain.Book
import com.scarlett.yuedujilu.domain.BookSource
import com.scarlett.yuedujilu.domain.BookStatus
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

fun formatHms(ms: Long): String {
  val totalSec = (ms / 1000L).coerceAtLeast(0L)
  val h = totalSec / 3600
  val m = (totalSec % 3600) / 60
  val s = totalSec % 60
  return "%02d:%02d:%02d".format(h, m, s)
}

fun formatDurationZh(ms: Long): String {
  val totalMin = (ms / 60_000L).coerceAtLeast(0L)
  val h = totalMin / 60
  val m = totalMin % 60
  return if (h > 0) "${h}小时${m}分" else "${m}分钟"
}

fun formatDate(epochMs: Long): String {
  val d = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
  return d.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
}

fun formatDateTime(epochMs: Long): String {
  return Instant.ofEpochMilli(epochMs).atZone(zone)
    .format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
}

fun yearMonthLabel(ym: YearMonth): String = "${ym.year}年${ym.monthValue}月"

fun today(): LocalDate = LocalDate.now(zone)

fun Book.statusLabel(): String = when (status) {
  BookStatus.UNREAD -> "想读"
  BookStatus.READING -> "在读"
  BookStatus.FINISHED -> "已读"
  BookStatus.ABANDONED -> "弃读"
}

fun Book.sourceLabel(): String = when (source) {
  BookSource.WEREAD -> "微信读书"
  BookSource.ISBN, BookSource.MANUAL -> "实体书"
}

fun Book.progressLabel(): String {
  return if (source == BookSource.WEREAD) {
    ""
  } else {
    val total = pageCount?.toString() ?: "?"
    "$currentPage/$total 页"
  }
}

fun Book.isPhysical(): Boolean = source != BookSource.WEREAD
