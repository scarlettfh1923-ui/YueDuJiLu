package com.scarlett.yuedujilu.domain

import kotlinx.serialization.Serializable

@Serializable
enum class WereadPartStatus { IDLE, SUCCESS, PARTIAL, INCOMPLETE, FAILED, UPGRADE }

@Serializable
data class WereadDayRecord(
  val accountHash: String,
  val date: String,
  val durationSec: Long? = null,
  val durationComplete: Boolean = false,
  val highlightCount: Int? = null,
  val thoughtCount: Int? = null,
  val notesComplete: Boolean = false,
  val updatedAtEpochMs: Long = 0,
)

/** 划线或想法的本地去重记录；书签无逐条日期，不纳入每日新增。 */
@Serializable
data class WereadNoteItem(
  val accountHash: String,
  val itemId: String,
  val kind: String, // highlight | thought
  val bookId: String,
  val date: String,
  val createdAtEpochSec: Long,
)
