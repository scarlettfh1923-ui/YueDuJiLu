package com.scarlett.yuedujilu

import android.app.Application
import android.content.Context
import com.scarlett.yuedujilu.data.JsonLibraryStore
import com.scarlett.yuedujilu.data.KeystoreSecretStore
import com.scarlett.yuedujilu.data.LibraryRepository
import com.scarlett.yuedujilu.data.SecretStore
import com.scarlett.yuedujilu.data.ShowApiClient
import com.scarlett.yuedujilu.ui.theme.ThemeCatalog
import java.io.File

class YueDuApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}

class AppContainer(context: Context) {
  val library = LibraryRepository(JsonLibraryStore(File(context.filesDir, "library.json")))
  val secrets: SecretStore = KeystoreSecretStore(context)
  val showApi = ShowApiClient()
  val themeCatalog: ThemeCatalog = ThemeCatalog.load(context)
}
