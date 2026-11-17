package com.scarlett.yuedujilu.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 万维易源 ISBN 查询，依据公开文档：
 * https://www.showapi.com/guides/isbn-book-quickstart-1626
 * POST/GET https://route.showapi.com/1626-1?appKey={appKey}
 * 表单字段 isbn；成功当 showapi_res_body.ret_code == 0。
 *
 * 不会在没有用户填写的 AppKey 时发出请求。
 */
data class ShowApiBook(
  val title: String,
  val author: String,
  val publisher: String,
  val pubdate: String,
  val page: Int?,
  val isbn: String,
  val img: String?,
)

sealed class ShowApiResult {
  data class Ok(val book: ShowApiBook) : ShowApiResult()
  data class NotFound(val message: String) : ShowApiResult()
  data class Failed(val message: String) : ShowApiResult()
  data object MissingKey : ShowApiResult()
}

class ShowApiClient(
  private val endpoint: String = "https://route.showapi.com/1626-1",
) {
  suspend fun lookup(appKey: String, isbn: String): ShowApiResult = withContext(Dispatchers.IO) {
    if (appKey.isBlank()) return@withContext ShowApiResult.MissingKey
    val cleaned = isbn.filter { it.isDigit() || it.equals('X', true) }
    if (cleaned.isEmpty()) return@withContext ShowApiResult.Failed("ISBN 为空")
    var conn:HttpURLConnection?=null
    try {
    val url = URL("$endpoint?appKey=${URLEncoder.encode(appKey, "UTF-8")}")
    val active = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 12_000
      readTimeout = 15_000
      doOutput = true
      setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    }
    conn=active
      val body = "isbn=${URLEncoder.encode(cleaned, "UTF-8")}"
      active.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
      val text = (if (active.responseCode in 200..299) active.inputStream else active.errorStream)
        ?.bufferedReader()?.readText().orEmpty()
      parse(text, active.responseCode)
    } catch(e:kotlinx.coroutines.CancellationException) { throw e
    } catch (e: Exception) {
      ShowApiResult.Failed("网络请求失败：${sanitizeNetworkError(e)}")
    } finally {
      conn?.disconnect()
    }
  }

  /** 仅探测凭证是否被接口接受，不把书籍当成功样本缓存。 */
  suspend fun testConnection(appKey: String): ShowApiResult {
    // 文档未提供专用“测试连接”接口；用一个合法但极不可能存在的 ISBN 探测鉴权。
    // 若返回鉴权失败则失败；若返回业务层 ret_code/未找到则视为凭证可用。
    return lookup(appKey, "9780000000002")
  }

  internal fun parse(text: String, httpCode: Int): ShowApiResult {
    if (text.isBlank()) return ShowApiResult.Failed("空响应（HTTP $httpCode）")
    return try {
      val root = Json.parseToJsonElement(text).jsonObject
      val resCode = root["showapi_res_code"]?.jsonPrimitive?.intOrNull ?: -1
      if (resCode != 0) {
        val msg = root["showapi_res_error"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return ShowApiResult.Failed(msg.ifBlank { "接口错误码 $resCode" })
      }
      val body = root["showapi_res_body"]?.jsonObject
        ?: return ShowApiResult.Failed("响应缺少 showapi_res_body")
      val ret = body["ret_code"]?.jsonPrimitive?.intOrNull ?: -1
      if (ret != 0) {
        val msg = body["msg"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return ShowApiResult.NotFound(msg.ifBlank { "未找到图书（ret_code=$ret）" })
      }
      val data = body["data"]?.jsonObject ?: return ShowApiResult.NotFound("响应无图书数据")
      fun field(key: String): String = data[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
      val title = field("title")
      if (title.isEmpty()) return ShowApiResult.NotFound("图书数据不完整")
      val pageRaw = field("page")
      ShowApiResult.Ok(
        ShowApiBook(
          title = title,
          author = field("author"),
          publisher = field("publisher"),
          pubdate = field("pubdate"),
          page = pageRaw.toIntOrNull()?.takeIf { it > 0 } ?: data["page"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 },
          isbn = field("isbn"),
          img = field("img").takeIf { it.isNotEmpty() },
        )
      )
    } catch (e: Exception) {
      ShowApiResult.Failed("无法解析响应")
    }
  }
}

/** 去掉可能含 appKey 的 URL 或查询串，避免把密钥回显到界面或日志。 */
internal fun sanitizeNetworkError(error: Throwable): String {
  val raw = error.message ?: error.javaClass.simpleName
  return redactSecrets(raw)
}

internal fun redactSecrets(raw: String): String {
  var text = raw
  text = APPKEY_QUERY.replace(text, "$1***")
  text = URL_IN_TEXT.replace(text, "[endpoint]")
  return text
}
private val APPKEY_QUERY = Regex("(?i)(appKey=)[^& ?#\\t\\r\\n]*")
private val URL_IN_TEXT = Regex("https?://[^ \\t\\r\\n]+")

