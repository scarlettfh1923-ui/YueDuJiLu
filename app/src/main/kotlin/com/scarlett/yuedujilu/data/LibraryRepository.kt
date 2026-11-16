package com.scarlett.yuedujilu.data

import com.scarlett.yuedujilu.domain.Book
import com.scarlett.yuedujilu.domain.BookSource
import com.scarlett.yuedujilu.domain.BookStatus
import com.scarlett.yuedujilu.domain.PauseSpan
import com.scarlett.yuedujilu.domain.ReadingMath
import com.scarlett.yuedujilu.domain.ReadingSession
import com.scarlett.yuedujilu.domain.SessionOrigin
import com.scarlett.yuedujilu.domain.ThemeId
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class LibraryRepository(private val store: JsonLibraryStore) {
  val state: StateFlow<AppSnapshot> = store.state

  suspend fun setTheme(id: ThemeId) = store.update { it.copy(settings = it.settings.copy(themeId = id)) }

  suspend fun updateSettings(transform: (AppSettings) -> AppSettings) =
    store.update { it.copy(settings = transform(it.settings)) }

  fun findByIsbn(isbn: String): Book? {
    val cleaned = isbn.filter { it.isDigit() || it.equals('X', true) }
    if (cleaned.isEmpty()) return null
    return state.value.books.find { it.isbn?.filter { c -> c.isDigit() || c.equals('X', true) } == cleaned }
  }

  suspend fun addBook(book: Book): Result<Book> {
    if (!book.isbn.isNullOrBlank() && findByIsbn(book.isbn) != null) {
      return Result.failure(IllegalStateException("已存在相同 ISBN 的书，不会重复添加"))
    }
    store.update { it.copy(books = it.books + book) }
    return Result.success(book)
  }

  suspend fun updateBook(book: Book) {
    store.update { snap ->
      snap.copy(books = snap.books.map { if (it.id == book.id) book else it })
    }
  }

  suspend fun deleteBook(id: String) {
    store.update { snap ->
      snap.copy(
        books = snap.books.filterNot { it.id == id },
        sessions = snap.sessions.filterNot { it.bookId == id },
      )
    }
  }

  fun activeSession(): ReadingSession? =
    state.value.sessions.firstOrNull { it.endedAtEpochMs == null }

  suspend fun startSession(bookId: String, startPage: Int?, now: Long): Result<ReadingSession> {
    if (activeSession() != null) return Result.failure(IllegalStateException("已有进行中的阅读计时"))
    val session = ReadingSession(
      id = UUID.randomUUID().toString(),
      bookId = bookId,
      origin = SessionOrigin.TIMER,
      startedAtEpochMs = now,
      startPage = startPage,
    )
    store.update { snap ->
      val books = snap.books.map {
        if (it.id == bookId && it.status == BookStatus.UNREAD)
          it.copy(status = BookStatus.READING, updatedAtEpochMs = now)
        else it
      }
      snap.copy(sessions = snap.sessions + session, books = books)
    }
    return Result.success(session)
  }

  suspend fun pauseActive(now: Long) {
    store.update { snap ->
      snap.copy(sessions = snap.sessions.map { s ->
        if (s.endedAtEpochMs == null && !ReadingMath.isPaused(s)) {
          s.copy(pauses = s.pauses + PauseSpan(pausedAtEpochMs = now))
        } else s
      })
    }
  }

  suspend fun resumeActive(now: Long) {
    store.update { snap ->
      snap.copy(sessions = snap.sessions.map { s ->
        if (s.endedAtEpochMs == null && ReadingMath.isPaused(s)) {
          val last = s.pauses.last()
          s.copy(pauses = s.pauses.dropLast(1) + last.copy(resumedAtEpochMs = now))
        } else s
      })
    }
  }

  suspend fun endActive(endPage: Int?, now: Long, markFinished: Boolean): Result<ReadingSession> {
    val current = activeSession() ?: return Result.failure(IllegalStateException("没有进行中的计时"))
    var ended: ReadingSession? = null
    store.update { snap ->
      val sessions = snap.sessions.map { s ->
        if (s.id != current.id) s else {
          val pauses = if (ReadingMath.isPaused(s)) {
            val last = s.pauses.last()
            s.pauses.dropLast(1) + last.copy(resumedAtEpochMs = now)
          } else s.pauses
          s.copy(endedAtEpochMs = now, endPage = endPage, pauses = pauses).also { ended = it }
        }
      }
      val books = snap.books.map { b ->
        if (b.id == current.bookId) ReadingMath.applySessionProgress(b, endPage, now, markFinished, allowDecrease = true) else b
      }
      snap.copy(sessions = sessions, books = books)
    }
    return Result.success(ended!!)
  }

  suspend fun addManualSession(
    bookId: String,
    startPage: Int?,
    endPage: Int?,
    startedAt: Long,
    endedAt: Long,
    now: Long,
  ): Result<ReadingSession> {
    if (endedAt <= startedAt) return Result.failure(IllegalArgumentException("结束时间须晚于开始时间"))
    val session = ReadingSession(
      id = UUID.randomUUID().toString(),
      bookId = bookId,
      origin = SessionOrigin.MANUAL,
      startedAtEpochMs = startedAt,
      endedAtEpochMs = endedAt,
      startPage = startPage,
      endPage = endPage,
    )
    store.update { snap ->
      val books = snap.books.map { b ->
        if (b.id == bookId) ReadingMath.applySessionProgress(b, endPage, now, markFinished = false, allowDecrease = false) else b
      }
      snap.copy(sessions = snap.sessions + session, books = books)
    }
    return Result.success(session)
  }

  suspend fun updateSession(session: ReadingSession, now: Long) {
    store.update { snap ->
      val sessions = snap.sessions.map { if (it.id == session.id) session else it }
      val allowDecrease = ReadingMath.isChronologicallyLast(session, sessions)
      val books = snap.books.map { b ->
        if (b.id == session.bookId) ReadingMath.applySessionProgress(b, session.endPage, now, markFinished = false, allowDecrease = allowDecrease)
        else b
      }
      snap.copy(sessions = sessions, books = books)
    }
  }

  suspend fun markFinished(bookId: String, at: Long) {
    store.update { snap ->
      snap.copy(books = snap.books.map {
        if (it.id == bookId) ReadingMath.markFinished(it, at) else it
      })
    }
  }

  suspend fun undoFinish(bookId: String, at: Long) {
    store.update { snap ->
      snap.copy(books = snap.books.map {
        if (it.id == bookId) ReadingMath.undoFinish(it, at) else it
      })
    }
  }

  suspend fun setFinishDate(bookId: String, finishedAt: Long, now: Long) {
    store.update { snap ->
      snap.copy(books = snap.books.map {
        if (it.id == bookId) ReadingMath.withFinishDate(it, finishedAt, now) else it
      })
    }
  }

  suspend fun replaceAll(snapshot: AppSnapshot) {
    store.update { snapshot }
  }

  fun exportJson(): String = store.exportJson()

  suspend fun restoreJson(text: String) = store.restoreFrom(text)

  fun newBookId(): String = UUID.randomUUID().toString()

  fun newBook(
    title: String,
    author: String = "",
    isbn: String? = null,
    source: BookSource = BookSource.MANUAL,
    pageCount: Int? = null,
    now: Long,
  ) = Book(
    id = newBookId(),
    title = title.trim(),
    author = author.trim(),
    isbn = isbn?.trim()?.takeIf { it.isNotEmpty() },
    source = source,
    pageCount = pageCount,
    currentPage = 0,
    status = BookStatus.UNREAD,
    createdAtEpochMs = now,
    updatedAtEpochMs = now,
  )

  suspend fun replaceWereadDuration(days: List<com.scarlett.yuedujilu.domain.WereadDayRecord>) =
    store.update { it.copy(wereadDays = days) }

  suspend fun replaceWereadNotes(
    days: List<com.scarlett.yuedujilu.domain.WereadDayRecord>,
    notes: List<com.scarlett.yuedujilu.domain.WereadNoteItem>,
  ) = store.update { it.copy(wereadDays = days, wereadNotes = notes) }
}
