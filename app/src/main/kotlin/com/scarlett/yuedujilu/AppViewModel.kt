package com.scarlett.yuedujilu

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scarlett.yuedujilu.data.AppSnapshot
import com.scarlett.yuedujilu.data.LibraryRepository
import com.scarlett.yuedujilu.data.SecretStore
import com.scarlett.yuedujilu.data.ShowApiClient
import com.scarlett.yuedujilu.data.ShowApiResult
import com.scarlett.yuedujilu.data.WereadApiResult
import com.scarlett.yuedujilu.data.WereadClient
import com.scarlett.yuedujilu.data.todayInShanghai
import com.scarlett.yuedujilu.domain.WereadImport
import com.scarlett.yuedujilu.domain.WereadPartStatus
import com.scarlett.yuedujilu.domain.Book
import com.scarlett.yuedujilu.domain.BookSource
import com.scarlett.yuedujilu.domain.BookStatus
import com.scarlett.yuedujilu.domain.ReadingMath
import com.scarlett.yuedujilu.domain.ReadingSession
import com.scarlett.yuedujilu.domain.StatsOverview
import com.scarlett.yuedujilu.domain.StatsPeriod
import com.scarlett.yuedujilu.domain.ThemeId
import com.scarlett.yuedujilu.domain.Zones
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class MainTab { SHELF, STATS, MINE }

enum class StatusFilter { ALL, READING, UNREAD, FINISHED, ABANDONED }

enum class SourceFilter { ALL, PHYSICAL, WEREAD }

sealed interface Route {
  data object Main : Route
  data class BookDetail(val bookId: String) : Route
  data class EditBook(val bookId: String?, val presetIsbn: String? = null, val presetTitle: String? = null, val presetAuthor: String? = null, val presetPages: Int? = null) : Route
  data object IsbnEntry : Route
  data object ScanIsbn : Route
  data class EndSession(val bookId: String) : Route
  data class Backfill(val bookId: String, val sessionId: String? = null) : Route
  data object Appearance : Route
  data object Weread : Route
  data object ShowApiConfig : Route
  data object AiEntry : Route
  data object DeepSeek : Route
  data object Backup : Route
  data object Export : Route
}

data class UiMessage(val text: String, val isError: Boolean = false)

data class AppUiState(
  val snapshot: AppSnapshot = AppSnapshot(),
  val now: Long = System.currentTimeMillis(),
  val tab: MainTab = MainTab.SHELF,
  val query: String = "",
  val statusFilter: StatusFilter = StatusFilter.ALL,
  val sourceFilter: SourceFilter = SourceFilter.ALL,
  val statsPeriod: StatsPeriod = StatsPeriod.MONTH,
  val statsMode: StatsMode = StatsMode.OVERVIEW,
  val calendarMonth: YearMonth = YearMonth.from(LocalDate.now(Zones.Shanghai)),
  val statsSource: SourceFilter = SourceFilter.ALL,
  val statsAnchor: LocalDate = LocalDate.now(Zones.Shanghai),
  val route: Route = Route.Main,
  val message: UiMessage? = null,
  val showPlus: Boolean = false,
  val hasShowApiKey: Boolean = false,
  val hasDeepSeekKey: Boolean = false,
  val hasWereadKey: Boolean = false,
  val lookupBusy: Boolean = false,
  val testBusy: Boolean = false,
  val wereadBusy: Boolean = false,
  val wereadProgress: String? = null,
)

enum class StatsMode { OVERVIEW, CALENDAR }

class AppViewModel(
  application: Application,
  val library: LibraryRepository,
  val secrets: SecretStore,
  val showApi: ShowApiClient,
  val weread: WereadClient,
) : AndroidViewModel(application) {

  private val _tab = MutableStateFlow(MainTab.SHELF)
  private val _query = MutableStateFlow("")
  private val _status = MutableStateFlow(StatusFilter.ALL)
  private val _source = MutableStateFlow(SourceFilter.ALL)
  private val _period = MutableStateFlow(StatsPeriod.MONTH)
  private val _statsMode = MutableStateFlow(StatsMode.OVERVIEW)
  private val _calMonth = MutableStateFlow(YearMonth.from(LocalDate.now(Zones.Shanghai)))
  private val _statsSource = MutableStateFlow(SourceFilter.ALL)
  private val _statsAnchor = MutableStateFlow(LocalDate.now(Zones.Shanghai))
  private val _route = MutableStateFlow<Route>(Route.Main)
  private val _message = MutableStateFlow<UiMessage?>(null)
  private val _showPlus = MutableStateFlow(false)
  private val _now = MutableStateFlow(System.currentTimeMillis())
  private val _hasShow = MutableStateFlow(secrets.getShowApiKey() != null)
  private val _hasDeep = MutableStateFlow(secrets.getDeepSeekKey() != null)
  private val _hasWeread = MutableStateFlow(secrets.getWereadKey() != null)
  private val _lookupBusy = MutableStateFlow(false)
  private val _testBusy = MutableStateFlow(false)
  private val _wereadBusy = MutableStateFlow(false)
  private val _wereadProgress = MutableStateFlow<String?>(null)
  private var wereadJob: Job? = null
  private val backStack = ArrayDeque<Route>()

  val ui: StateFlow<AppUiState> = combine(
    combine(library.state, _now, _tab, _query) { snap, now, tab, q -> Quad(snap, now, tab, q) },
    combine(_status, _source, _period, _statsMode) { a, b, c, d -> Quad(a, b, c, d) },
    combine(_calMonth, _route, _message, _showPlus) { a, b, c, d -> Quad(a, b, c, d) },
    combine(_hasShow, _hasDeep, _hasWeread, combine(_lookupBusy, _testBusy, _wereadBusy, _wereadProgress) { w, x, y, z -> Quad(w, x, y, z) }) { a, b, c, d ->
      FlagPack(a, b, c, d.a, d.b, d.c, d.d)
    },
    combine(_statsSource, _statsAnchor) { s, a -> s to a },
  ) { a, b, c, d, e ->
    AppUiState(
      snapshot = a.a,
      now = a.b,
      tab = a.c,
      query = a.d,
      statusFilter = b.a,
      sourceFilter = b.b,
      statsPeriod = b.c,
      statsMode = b.d,
      calendarMonth = c.a,
      statsSource = e.first,
      statsAnchor = e.second,
      route = c.b,
      message = c.c,
      showPlus = c.d,
      hasShowApiKey = d.hasShow,
      hasDeepSeekKey = d.hasDeep,
      hasWereadKey = d.hasWeread,
      lookupBusy = d.lookupBusy,
      testBusy = d.testBusy,
      wereadBusy = d.wereadBusy,
      wereadProgress = d.wereadProgress,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

  init {
    viewModelScope.launch {
      while (isActive) {
        _now.value = System.currentTimeMillis()
        delay(1000)
      }
    }
  }

  fun now(): Long = System.currentTimeMillis()

  fun confirmPhysicalUnread(date: LocalDate, confirmed: Boolean) {
    if(date.isAfter(LocalDate.now(Zones.Shanghai))) return
    viewModelScope.launch { library.confirmPhysicalUnread(date.toString(), confirmed) }
  }

  fun setTab(tab: MainTab) { _tab.value = tab }
  fun setQuery(q: String) { _query.value = q }
  fun setStatus(f: StatusFilter) { _status.value = f }
  fun setSource(f: SourceFilter) { _source.value = f }
  fun setPeriod(p: StatsPeriod) { _period.value = p }
  fun setStatsMode(m: StatsMode) { _statsMode.value = m }
  fun setStatsSource(f: SourceFilter) { _statsSource.value = f }
  fun setStatsAnchor(date: LocalDate) {
    _statsAnchor.value = date
    _calMonth.value = YearMonth.from(date)
  }
  fun shiftStatsAnchor(delta: Long) {
    val today = LocalDate.now(Zones.Shanghai)
    val period = _period.value
    if (period == StatsPeriod.ALL) return
    if (delta > 0 && !StatsOverview.canShiftForward(period, _statsAnchor.value, today)) return
    val next = StatsOverview.shiftAnchor(period, _statsAnchor.value, delta)
    _statsAnchor.value = next
    _calMonth.value = YearMonth.from(next)
  }
  fun shiftMonth(delta: Long) {
    val today = LocalDate.now(Zones.Shanghai)
    val next = _calMonth.value.plusMonths(delta)
    if (delta > 0 && next.isAfter(YearMonth.from(today))) return
    _calMonth.value = next
    val day = _statsAnchor.value.dayOfMonth.coerceAtMost(next.lengthOfMonth())
    _statsAnchor.value = next.atDay(day)
  }
  fun openPlus(open: Boolean) { _showPlus.value = open }
  fun clearMessage() { _message.value = null }
  fun toast(text: String, error: Boolean = false) { _message.value = UiMessage(text, error) }

  fun go(route: Route) {
    val current = _route.value
    if (current != Route.Main) backStack.addLast(current)
    else if (route != Route.Main) backStack.clear().also { backStack.addLast(Route.Main) }
    _route.value = route
    _showPlus.value = false
  }

  fun back() {
    _route.value = if (backStack.isNotEmpty()) backStack.removeLast() else Route.Main
  }

  fun visibleBooks(state: AppUiState): List<Book> {
    val q = state.query.trim()
    return state.snapshot.books
      .filter { book ->
        when (state.statusFilter) {
          StatusFilter.ALL -> true
          StatusFilter.READING -> book.status == BookStatus.READING
          StatusFilter.UNREAD -> book.status == BookStatus.UNREAD
          StatusFilter.FINISHED -> book.status == BookStatus.FINISHED
          StatusFilter.ABANDONED -> book.status == BookStatus.ABANDONED
        }
      }
      .filter { book ->
        when (state.sourceFilter) {
          SourceFilter.ALL -> true
          SourceFilter.PHYSICAL -> book.source != BookSource.WEREAD
          SourceFilter.WEREAD -> book.source == BookSource.WEREAD
        }
      }
      .filter { book ->
        if (q.isEmpty()) true
        else listOf(book.title, book.author, book.isbn.orEmpty()).any { it.contains(q, ignoreCase = true) }
      }
      .sortedByDescending { it.updatedAtEpochMs }
  }

  fun book(id: String): Book? = library.state.value.books.find { it.id == id }
  fun sessionsOf(bookId: String): List<ReadingSession> =
    library.state.value.sessions.filter { it.bookId == bookId }.sortedByDescending { it.startedAtEpochMs }
  fun active(): ReadingSession? = library.activeSession()

  fun setTheme(id: ThemeId) = viewModelScope.launch { library.setTheme(id) }

  fun saveBook(
    bookId: String?,
    title: String,
    author: String,
    isbn: String,
    pageCount: String,
    source: BookSource,
  ) = viewModelScope.launch {
    val titleTrim = title.trim()
    if (titleTrim.isEmpty()) {
      toast("请填写书名", true)
      return@launch
    }
    val pages = pageCount.trim().toIntOrNull()?.takeIf { it > 0 }
    val isbnTrim = isbn.trim().ifEmpty { null }
    if (isbnTrim != null) {
      val existed = library.findByIsbn(isbnTrim)
      if (existed != null && existed.id != bookId) {
        toast("已存在相同 ISBN 的书，不会重复添加", true)
        return@launch
      }
    }
    val t = now()
    if (bookId == null) {
      val book = library.newBook(titleTrim, author, isbnTrim, source, pages, t)
      library.addBook(book).onFailure { toast(it.message ?: "添加失败", true); return@launch }
      toast("已加入书架")
      back()
    } else {
      val old = book(bookId) ?: return@launch
      library.updateBook(old.copy(title = titleTrim, author = author.trim(), isbn = isbnTrim, pageCount = pages, updatedAtEpochMs = t))
      toast("已保存")
      back()
    }
  }

  fun startOrOpenTimer(book: Book) {
    val active = active()
    when {
      active != null && active.bookId == book.id -> go(Route.EndSession(book.id))
      active != null -> toast("请先结束或暂停当前正在计时的书", true)
      else -> viewModelScope.launch {
        val startPage = book.currentPage.takeIf { it > 0 }
        library.startSession(book.id, startPage, now())
          .onSuccess { toast("已开始计时") }
          .onFailure { toast(it.message ?: "无法开始", true) }
      }
    }
  }

  fun pauseOrResume() = viewModelScope.launch {
    val s = active() ?: return@launch
    if (ReadingMath.isPaused(s)) library.resumeActive(now()) else library.pauseActive(now())
  }

  fun endSession(endPage: Int?, markFinished: Boolean) = viewModelScope.launch {
    library.endActive(endPage, now(), markFinished)
      .onSuccess {
        toast(if (markFinished) "已结束计时，并记为读完" else "已保存本次阅读")
        _route.value = Route.Main
        backStack.clear()
      }
      .onFailure { toast(it.message ?: "结束失败", true) }
  }

  fun markFinished(bookId: String) = viewModelScope.launch {
    library.markFinished(bookId, now())
    toast("已记为读完（页数保持不变）")
  }

  fun undoFinish(bookId: String) = viewModelScope.launch {
    library.undoFinish(bookId, now())
    toast("已撤销读完")
  }

  fun setFinishDate(bookId: String, epochMs: Long) = viewModelScope.launch {
    runCatching { library.setFinishDate(bookId, epochMs, now()) }
      .onSuccess { toast("已更新完读日期") }
      .onFailure { toast(it.message ?: "无法修改", true) }
  }

  fun setAbandoned(bookId: String, abandoned: Boolean) = viewModelScope.launch {
    val b = book(bookId) ?: return@launch
    library.updateBook(
      b.copy(
        status = if (abandoned) BookStatus.ABANDONED else if (b.currentPage > 0) BookStatus.READING else BookStatus.UNREAD,
        updatedAtEpochMs = now(),
      )
    )
  }

  fun saveManualSession(bookId: String, sessionId: String?, startPage: Int?, endPage: Int?, startAt: Long, endAt: Long) =
    viewModelScope.launch {
      if (sessionId == null) {
        library.addManualSession(bookId, startPage, endPage, startAt, endAt, now())
          .onSuccess { toast("已补录"); back() }
          .onFailure { toast(it.message ?: "补录失败", true) }
      } else {
        val old = library.state.value.sessions.find { it.id == sessionId } ?: return@launch
        library.updateSession(
          old.copy(startPage = startPage, endPage = endPage, startedAtEpochMs = startAt, endedAtEpochMs = endAt),
          now(),
        )
        toast("已修改记录")
        back()
      }
    }

  fun lookupIsbn(isbn: String, onFoundOpenEdit: Boolean = true) = viewModelScope.launch {
    val key = secrets.getShowApiKey()
    if (key.isNullOrBlank()) {
      toast("尚未填写万维易源 AppKey，可先手填书目，或到「我的」里配置后再查", true)
      go(Route.EditBook(null, presetIsbn = isbn))
      return@launch
    }
    val existed = library.findByIsbn(isbn)
    if (existed != null) {
      toast("书架里已有这本书")
      go(Route.BookDetail(existed.id))
      return@launch
    }
    _lookupBusy.value = true
    when (val r = showApi.lookup(key, isbn)) {
      is ShowApiResult.Ok -> {
        toast("已查询到《${r.book.title}》")
        if (onFoundOpenEdit) {
          go(
            Route.EditBook(
              null,
              presetIsbn = r.book.isbn.ifBlank { isbn },
              presetTitle = r.book.title,
              presetAuthor = r.book.author,
              presetPages = r.book.page,
            )
          )
        }
      }
      is ShowApiResult.NotFound -> {
        toast("未查到书目，已保留 ISBN，请手填其余信息")
        go(Route.EditBook(null, presetIsbn = isbn))
      }
      is ShowApiResult.Failed -> {
        toast("查询失败：${r.message}。已保留 ISBN，可手填", true)
        go(Route.EditBook(null, presetIsbn = isbn))
      }
      ShowApiResult.MissingKey -> {
        toast("尚未填写 AppKey", true)
        go(Route.EditBook(null, presetIsbn = isbn))
      }
    }
    _lookupBusy.value = false
  }

  fun saveShowApiKey(key: String) {
    secrets.setShowApiKey(key.ifBlank { null })
    _hasShow.value = secrets.getShowApiKey() != null
    toast(if (_hasShow.value) "已保存 AppKey（仅存本机，加密，不会进入备份）" else "已清除 AppKey")
  }

  fun testShowApi() = viewModelScope.launch {
    val key = secrets.getShowApiKey()
    if (key.isNullOrBlank()) {
      toast("请先填写并保存 AppKey", true)
      return@launch
    }
    _testBusy.value = true
    val r = showApi.testConnection(key)
    toast(
      when (r) {
        is ShowApiResult.Ok -> "连接成功，接口已返回数据"
        is ShowApiResult.NotFound -> "凭证已被接口接受（探测 ISBN 无对应书目，这是预期结果）"
        is ShowApiResult.Failed -> "测试未通过：${r.message}"
        ShowApiResult.MissingKey -> "缺少 AppKey"
      },
      error = r is ShowApiResult.Failed || r is ShowApiResult.MissingKey,
    )
    _testBusy.value = false
  }

  fun saveDeepSeek(key: String, temperature: Float, deepThinking: Boolean, model: String, baseUrl: String) {
    if (key.isNotBlank()) secrets.setDeepSeekKey(key)
    _hasDeep.value = secrets.getDeepSeekKey() != null
    viewModelScope.launch {
      library.updateSettings {
        it.copy(
          deepSeekTemperature = temperature.coerceIn(0f, 2f),
          deepSeekDeepThinking = deepThinking,
          deepSeekModel = model.trim().ifEmpty { "deepseek-chat" },
          deepSeekBaseUrl = baseUrl.trim().ifEmpty { "https://api.deepseek.com" },
        )
      }
      toast("配置已保存。当前不会调用接口，用途待定")
    }
  }


  fun wereadAccountHash(): String? =
    secrets.getWereadKey()?.let { com.scarlett.yuedujilu.data.WereadGateway.accountPartition(it) }

  fun saveWereadConfig(key: String, importStartDate: String?) {
    if (key.isNotBlank()) secrets.setWereadKey(key.trim())
    _hasWeread.value = secrets.getWereadKey() != null
    val start = WereadImport.parseDate(importStartDate)
    viewModelScope.launch {
      library.updateSettings {
        it.copy(
          wereadImportStartDate = start?.let(WereadImport::format),
          wereadConfigured = secrets.getWereadKey() != null && start != null,
        )
      }
      toast(
        when {
          secrets.getWereadKey() == null -> "请填写密钥后再保存"
          start == null -> "请设置导入起点"
          else -> "已保存密钥和导入起点（起点不会自动滚动）"
        },
        error = secrets.getWereadKey() == null || start == null,
      )
    }
  }

  fun applyWereadLast7Days(existingIso: String?): String {
    val today = todayInShanghai(now())
    val existing = WereadImport.parseDate(existingIso)
    val start = WereadImport.applyLast7DaysShortcut(today, existing)
    if (existing != null) {
      toast("起点已固定为 ${WereadImport.format(existing)}，不会按最近 7 天滚动。如需调整请直接修改日期。")
    }
    return WereadImport.format(start)
  }

  fun testWereadConnection() {
    val key = secrets.getWereadKey()
    if (key.isNullOrBlank()) {
      toast("请先填写并保存密钥", true)
      return
    }
    if (_wereadBusy.value) return
    wereadJob?.cancel()
    wereadJob = viewModelScope.launch {
      _wereadBusy.value = true
      _wereadProgress.value = "正在测试连接"
      when (val r = weread.testConnection(key)) {
        is WereadApiResult.Ok -> toast("连接成功，接口可用")
        is WereadApiResult.UpgradeRequired -> toast("需要先升级微信读书技能：${r.message}", true)
        is WereadApiResult.Error -> toast("测试未通过：${r.message}", true)
        is WereadApiResult.Failed -> toast("测试未通过：${r.message}", true)
        WereadApiResult.MissingKey -> toast("请先填写并保存密钥", true)
      }
      _wereadBusy.value = false
      _wereadProgress.value = null
    }
  }

  fun cancelWereadSync() {
    wereadJob?.cancel()
    wereadJob = null
    _wereadBusy.value = false
    _wereadProgress.value = null
    toast("已取消")
  }

  fun syncWeread() {
    val key = secrets.getWereadKey()
    val start = WereadImport.parseDate(library.state.value.settings.wereadImportStartDate)
    if (key.isNullOrBlank() || start == null) {
      toast("请先保存密钥和导入起点", true)
      return
    }
    if (_wereadBusy.value) return
    wereadJob?.cancel()
    wereadJob = viewModelScope.launch {
      _wereadBusy.value = true
      val today = todayInShanghai(now())
      val nowMs = now()
      val snap0 = library.state.value
      var durationMsg = ""
      var notesMsg = ""
      var durationStatus = WereadPartStatus.IDLE
      var notesStatus = WereadPartStatus.IDLE
      try {
        _wereadProgress.value = "正在同步阅读时长"
        val (durationReport, newDays) = weread.syncDailyDuration(
          apiKey = key,
          start = start,
          today = today,
          existing = snap0.wereadDays,
          nowMs = nowMs,
          onProgress = { _wereadProgress.value = it },
        )
        durationStatus = durationReport.durationStatus
        durationMsg = durationReport.message
        if (newDays != null && durationStatus != WereadPartStatus.FAILED && durationStatus != WereadPartStatus.UPGRADE) {
          library.replaceWereadDuration(newDays)
        }
        if (durationStatus == WereadPartStatus.UPGRADE) {
          notesStatus = WereadPartStatus.IDLE
          notesMsg = "已暂停，未同步笔记"
        } else {
          _wereadProgress.value = "正在同步划线与想法"
          val snap1 = library.state.value
          val (notesReport, notesWritten) = weread.syncNotes(
            apiKey = key,
            start = start,
            today = today,
            nowEpochSec = nowMs / 1000,
            existingDays = snap1.wereadDays,
            existingNotes = snap1.wereadNotes,
            nowMs = nowMs,
            onProgress = { _wereadProgress.value = it },
          )
          notesStatus = notesReport.notesStatus
          notesMsg = notesReport.message
          if (notesWritten != null && notesStatus == WereadPartStatus.SUCCESS) {
            library.replaceWereadNotes(notesWritten.first, notesWritten.second)
          }
        }
        val combined = listOf(durationMsg, notesMsg).filter { it.isNotBlank() }.joinToString("。")
        library.updateSettings {
          it.copy(
            wereadLastSyncAt = java.time.Instant.ofEpochMilli(nowMs).toString(),
            wereadDurationStatus = durationStatus,
            wereadNotesStatus = notesStatus,
            wereadLastMessage = combined,
          )
        }
        val failed = durationStatus == WereadPartStatus.FAILED || durationStatus == WereadPartStatus.UPGRADE ||
          notesStatus == WereadPartStatus.FAILED || notesStatus == WereadPartStatus.UPGRADE
        val partial = durationStatus == WereadPartStatus.INCOMPLETE || notesStatus == WereadPartStatus.PARTIAL ||
          (durationStatus == WereadPartStatus.SUCCESS && notesStatus != WereadPartStatus.SUCCESS)
        toast(combined.ifBlank { "同步结束" }, error = failed || partial)
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {
        toast("同步失败", true)
      } finally {
        _wereadBusy.value = false
        _wereadProgress.value = null
      }
    }
  }

  fun exportTo(uri: Uri) = viewModelScope.launch {
    runCatching {
      val text = library.exportJson()
      getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
        it.write(text.toByteArray(Charsets.UTF_8))
      } ?: error("无法写入所选位置")
    }.onSuccess { toast("已导出阅读记录（不含密钥）") }
      .onFailure { toast(it.message ?: "导出失败", true) }
  }

  fun restoreFrom(uri: Uri) = viewModelScope.launch {
    runCatching {
      val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
        it.readBytes().toString(Charsets.UTF_8)
      } ?: error("无法读取所选文件")
      library.restoreJson(text)
    }.onSuccess { toast("已恢复数据（密钥不会出现在备份里，需另行填写）") }
      .onFailure { toast("恢复失败：${it.message}", true) }
  }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private data class FlagPack(
  val hasShow: Boolean,
  val hasDeep: Boolean,
  val hasWeread: Boolean,
  val lookupBusy: Boolean,
  val testBusy: Boolean,
  val wereadBusy: Boolean,
  val wereadProgress: String?,
)

class AppViewModelFactory(
  private val app: Application,
  private val container: AppContainer,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return AppViewModel(app, container.library, container.secrets, container.showApi, container.weread) as T
  }
}
