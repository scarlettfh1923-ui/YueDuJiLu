package com.scarlett.yuedujilu.data

import com.scarlett.yuedujilu.domain.Book
import com.scarlett.yuedujilu.domain.ReadingSession
import com.scarlett.yuedujilu.domain.ThemeId
import kotlinx.serialization.Serializable

@Serializable
data class AppSnapshot(
  val books: List<Book> = emptyList(),
  val sessions: List<ReadingSession> = emptyList(),
  val settings: AppSettings = AppSettings(),
)

@Serializable
data class AppSettings(
  val themeId: ThemeId = ThemeId.MIST_PLUM,
  val deepSeekBaseUrl: String = "https://api.deepseek.com",
  val deepSeekModel: String = "deepseek-chat",
  val deepSeekTemperature: Float = 0.7f,
  val deepSeekDeepThinking: Boolean = false,
  val wereadConfigured: Boolean = false,
)
