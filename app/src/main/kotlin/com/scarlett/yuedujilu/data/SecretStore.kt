package com.scarlett.yuedujilu.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

interface SecretStore {
  fun getShowApiKey(): String?
  fun setShowApiKey(value: String?)
  fun getDeepSeekKey(): String?
  fun setDeepSeekKey(value: String?)
}

class KeystoreSecretStore(context: Context) : SecretStore {
  private val prefs = EncryptedSharedPreferences.create(
    "secret_prefs",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
  )

  override fun getShowApiKey(): String? = prefs.getString(KEY_SHOWAPI, null)?.takeIf { it.isNotBlank() }
  override fun setShowApiKey(value: String?) {
    prefs.edit().putString(KEY_SHOWAPI, value?.trim().orEmpty()).apply()
  }
  override fun getDeepSeekKey(): String? = prefs.getString(KEY_DEEPSEEK, null)?.takeIf { it.isNotBlank() }
  override fun setDeepSeekKey(value: String?) {
    prefs.edit().putString(KEY_DEEPSEEK, value?.trim().orEmpty()).apply()
  }

  companion object {
    private const val KEY_SHOWAPI = "showapi_appkey"
    private const val KEY_DEEPSEEK = "deepseek_apikey"
  }
}

class MemorySecretStore : SecretStore {
  private var show: String? = null
  private var deep: String? = null
  override fun getShowApiKey(): String? = show
  override fun setShowApiKey(value: String?) { show = value?.trim()?.takeIf { it.isNotEmpty() } }
  override fun getDeepSeekKey(): String? = deep
  override fun setDeepSeekKey(value: String?) { deep = value?.trim()?.takeIf { it.isNotEmpty() } }
}
