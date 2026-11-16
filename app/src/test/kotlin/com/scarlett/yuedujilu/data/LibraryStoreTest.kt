package com.scarlett.yuedujilu.data

import com.scarlett.yuedujilu.domain.BookStatus
import com.scarlett.yuedujilu.domain.ReadingMath
import com.scarlett.yuedujilu.domain.ThemeId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class LibraryStoreTest {
  private fun repo(): LibraryRepository {
    val dir = createTempDirectory("yuedu-test").toFile()
    return LibraryRepository(JsonLibraryStore(File(dir, "library.json")))
  }

  @Test
  fun themePersistsAcrossReload() = runBlocking {
    val dir = createTempDirectory("yuedu-theme").toFile()
    val file = File(dir, "library.json")
    val first = LibraryRepository(JsonLibraryStore(file))
    first.setTheme(ThemeId.PINE_GREEN)
    assertEquals(ThemeId.PINE_GREEN, first.state.value.settings.themeId)
    val reloaded = LibraryRepository(JsonLibraryStore(file))
    assertEquals(ThemeId.PINE_GREEN, reloaded.state.value.settings.themeId)
  }

  @Test
  fun otherSettingsSurviveThemeChange() = runBlocking {
    val repo = repo()
    repo.updateSettings { it.copy(deepSeekModel = "custom-model", deepSeekTemperature = 1.2f) }
    repo.setTheme(ThemeId.SLATE_BLUE)
    val s = repo.state.value.settings
    assertEquals("custom-model", s.deepSeekModel)
    assertEquals(1.2f, s.deepSeekTemperature)
    assertEquals(ThemeId.SLATE_BLUE, s.themeId)
  }

  @Test
  fun timerPauseResumeAndFinish475() = runBlocking {
    val repo = repo()
    val now0 = 1_000_000L
    val book = repo.newBook("长夜", pageCount = 500, now = now0)
    repo.addBook(book)
    repo.startSession(book.id, startPage = 450, now = now0)
    repo.pauseActive(now0 + 10 * 60_000)
    repo.resumeActive(now0 + 20 * 60_000)
    val end = repo.endActive(endPage = 475, now = now0 + 40 * 60_000, markFinished = true).getOrThrow()
    val stored = repo.state.value.sessions.single()
    assertEquals(end.id, stored.id)
    // 有效 30 分钟（暂停 10 分钟）
    assertEquals(30 * 60_000L, ReadingMath.effectiveDurationMs(stored, now0 + 40 * 60_000))
    val updated = repo.state.value.books.single()
    assertEquals(BookStatus.FINISHED, updated.status)
    assertEquals(475, updated.currentPage)
    assertEquals(500, updated.pageCount)
    repo.undoFinish(book.id, now0 + 50 * 60_000)
    assertEquals(BookStatus.READING, repo.state.value.books.single().status)
    assertEquals(475, repo.state.value.books.single().currentPage)
    assertNull(repo.state.value.books.single().finishedAtEpochMs)
  }

  @Test
  fun duplicateIsbnRejected() = runBlocking {
    val repo = repo()
    val a = repo.newBook("A", isbn = "9787110000000", now = 1L)
    assertTrue(repo.addBook(a).isSuccess)
    val b = repo.newBook("B", isbn = "978-7-1100-00000", now = 2L)
    assertTrue(repo.addBook(b).isFailure)
  }

  @Test
  fun backfillEarlierPagesDoesNotRewindProgress() = runBlocking {
    val repo = repo()
    val now = 2_000_000L
    val book = repo.newBook("长夜", pageCount = 500, now = now)
    repo.addBook(book)
    repo.startSession(book.id, startPage = 450, now = now)
    repo.endActive(endPage = 475, now = now + 30 * 60_000, markFinished = true).getOrThrow()
    repo.addManualSession(
      bookId = book.id,
      startPage = 10,
      endPage = 20,
      startedAt = now - 3 * 24 * 3600_000L,
      endedAt = now - 3 * 24 * 3600_000L + 20 * 60_000,
      now = now + 40 * 60_000,
    ).getOrThrow()
    val after = repo.state.value.books.single()
    assertEquals(475, after.currentPage)
    assertEquals(BookStatus.FINISHED, after.status)
  }

  @Test
  fun editingOlderSessionDoesNotRewindButLatestEditCanCorrect() = runBlocking {
    val repo = repo()
    val now = 3_000_000L
    val book = repo.newBook("长夜", pageCount = 500, now = now)
    repo.addBook(book)
    repo.addManualSession(book.id, 100, 200, now, now + 10 * 60_000, now + 10 * 60_000).getOrThrow()
    repo.addManualSession(book.id, 200, 475, now + 20 * 60_000, now + 30 * 60_000, now + 30 * 60_000).getOrThrow()
    assertEquals(475, repo.state.value.books.single().currentPage)
    val older = repo.state.value.sessions.minBy { it.startedAtEpochMs }
    repo.updateSession(older.copy(endPage = 150), now + 40 * 60_000)
    assertEquals(475, repo.state.value.books.single().currentPage)
    val latest = repo.state.value.sessions.maxBy { it.endedAtEpochMs ?: 0L }
    repo.updateSession(latest.copy(endPage = 470), now + 50 * 60_000)
    assertEquals(470, repo.state.value.books.single().currentPage)
  }

  @Test
  fun finish475SurvivesReloadAndExportOmitsSecrets() = runBlocking {
    val dir = createTempDirectory("yuedu-finish").toFile()
    val file = File(dir, "library.json")
    val first = LibraryRepository(JsonLibraryStore(file))
    val book = first.newBook("长夜", pageCount = 500, now = 4_000_000L)
    first.addBook(book)
    first.markFinished(book.id, 4_100_000L)
    first.updateBook(first.state.value.books.single().copy(currentPage = 475))
    first.setTheme(ThemeId.WARM_IVORY)
    first.setTheme(ThemeId.SLATE_BLUE)
    first.setTheme(ThemeId.PINE_GREEN)
    first.setTheme(ThemeId.MIST_PLUM)
    val exported = first.exportJson()
    assertTrue("export must not contain secret field names", "showapi_appkey" !in exported && "deepseek_apikey" !in exported)
    val reloaded = LibraryRepository(JsonLibraryStore(file))
    val b = reloaded.state.value.books.single()
    assertEquals(BookStatus.FINISHED, b.status)
    assertEquals(475, b.currentPage)
    assertEquals(500, b.pageCount)
    assertEquals(ThemeId.MIST_PLUM, reloaded.state.value.settings.themeId)
  }
}
