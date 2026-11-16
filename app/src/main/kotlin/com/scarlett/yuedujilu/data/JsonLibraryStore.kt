package com.scarlett.yuedujilu.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

class JsonLibraryStore(private val file: File) {
  private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
  }
  private val mutex = Mutex()
  private val _state = MutableStateFlow(load())
  val state: StateFlow<AppSnapshot> = _state.asStateFlow()

  private fun load(): AppSnapshot {
    if (!file.exists()) return AppSnapshot()
    return runCatching {
      json.decodeFromString(AppSnapshot.serializer(), file.readText())
    }.getOrElse { AppSnapshot() }
  }

  suspend fun update(transform: (AppSnapshot) -> AppSnapshot): AppSnapshot = mutex.withLock {
    val next = transform(_state.value)
    file.parentFile?.mkdirs()
    val tmp = File(file.parentFile, file.name + ".tmp")
    tmp.writeText(json.encodeToString(AppSnapshot.serializer(), next))
    if (!tmp.renameTo(file)) {
      tmp.copyTo(file, overwrite = true)
      tmp.delete()
    }
    _state.value = next
    next
  }

  fun exportJson(): String = json.encodeToString(AppSnapshot.serializer(), _state.value)

  suspend fun restoreFrom(text: String) {
    val parsed = json.decodeFromString(AppSnapshot.serializer(), text)
    update { parsed }
  }
}
