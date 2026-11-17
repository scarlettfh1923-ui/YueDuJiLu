package com.scarlett.yuedujilu.domain

import kotlinx.serialization.Serializable

@Serializable
enum class BookSource { MANUAL, ISBN, WEREAD }

@Serializable
enum class BookStatus { UNREAD, READING, FINISHED, ABANDONED }

@Serializable
enum class SessionOrigin { TIMER, MANUAL }

@Serializable
enum class ThemeId {
  MIST_PLUM,   // 雾紫（保留既有选择）
  PINE_GREEN,  // 青瓷青绿（新安装默认）
  SLATE_BLUE,  // 灰蓝
  WARM_IVORY;  // 暖白

  val displayName: String
    get() = when (this) {
      MIST_PLUM -> "雾紫"
      PINE_GREEN -> "青绿"
      SLATE_BLUE -> "灰蓝"
      WARM_IVORY -> "暖白"
    }
}

@Serializable
data class Book(
  val id: String,
  val title: String,
  val author: String = "",
  val isbn: String? = null,
  val source: BookSource = BookSource.MANUAL,
  val pageCount: Int? = null,
  val currentPage: Int = 0,
  val status: BookStatus = BookStatus.UNREAD,
  val finishedAtEpochMs: Long? = null,
  val coverHint: Int = 0,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
)

@Serializable
data class PauseSpan(
  val pausedAtEpochMs: Long,
  val resumedAtEpochMs: Long? = null,
)

@Serializable
data class ReadingSession(
  val id: String,
  val bookId: String,
  val origin: SessionOrigin = SessionOrigin.TIMER,
  val startedAtEpochMs: Long,
  val endedAtEpochMs: Long? = null,
  val pauses: List<PauseSpan> = emptyList(),
  val startPage: Int? = null,
  val endPage: Int? = null,
  val note: String? = null,
)

enum class StatsPeriod { WEEK, MONTH, YEAR, ALL }

data class PeriodStats(
  val durationMs: Long,
  val readingDays: Int,
  val booksRead: Int,
  val booksFinished: Int,
)
