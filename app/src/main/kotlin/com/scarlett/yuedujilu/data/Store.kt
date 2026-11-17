package com.scarlett.yuedujilu.data

import com.scarlett.yuedujilu.domain.Book
import com.scarlett.yuedujilu.domain.ReadingSession
import com.scarlett.yuedujilu.domain.ThemeId
import com.scarlett.yuedujilu.domain.WereadDayRecord
import com.scarlett.yuedujilu.domain.WereadNoteItem
import com.scarlett.yuedujilu.domain.WereadPartStatus
import kotlinx.serialization.Serializable

@Serializable
data class AppSnapshot(
  val books: List<Book> = emptyList(),
  val sessions: List<ReadingSession> = emptyList(),
  val settings: AppSettings = AppSettings(),
  val wereadDays: List<WereadDayRecord> = emptyList(),
  val wereadNotes: List<WereadNoteItem> = emptyList(),
  val confirmedPhysicalUnreadDates: Set<String> = emptySet(),
)

@Serializable
data class AppSettings(
  val themeId: ThemeId = ThemeId.PINE_GREEN,
  val deepSeekBaseUrl: String = "https://api.deepseek.com",
  val deepSeekModel: String = "deepseek-chat",
  val deepSeekTemperature: Float = 0.7f,
  val deepSeekDeepThinking: Boolean = false,
  val wereadConfigured: Boolean = false,
  val wereadImportStartDate: String? = null,
  val wereadLastSyncAt: String? = null,
  val wereadDurationStatus: WereadPartStatus = WereadPartStatus.IDLE,
  val wereadNotesStatus: WereadPartStatus = WereadPartStatus.IDLE,
  val wereadLastMessage: String? = null,
)
