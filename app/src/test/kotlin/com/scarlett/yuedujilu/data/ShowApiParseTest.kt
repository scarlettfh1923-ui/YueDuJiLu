package com.scarlett.yuedujilu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowApiParseTest {
  private val client = ShowApiClient()

  @Test
  fun parsesSuccessfulDocumentShape() {
    val json = """
      {"showapi_res_code":0,"showapi_res_error":"","showapi_res_body":{
        "ret_code":0,
        "data":{"title":"三体","author":"刘慈欣","publisher":"重庆出版社","pubdate":"2008-01","page":"302","isbn":"9787536693968","img":"https://example.com/a.jpg"}
      }}
    """.trimIndent()
    val result = client.parse(json, 200)
    val ok = result as ShowApiResult.Ok
    assertEquals("三体", ok.book.title)
    assertEquals("刘慈欣", ok.book.author)
    assertEquals(302, ok.book.page)
    assertEquals("9787536693968", ok.book.isbn)
  }

  @Test
  fun notFoundWhenRetCodeNonZero() {
    val json = """{"showapi_res_code":0,"showapi_res_body":{"ret_code":1,"msg":"未找到"}}"""
    assertTrue(client.parse(json, 200) is ShowApiResult.NotFound)
  }

  @Test
  fun failedWhenGatewayError() {
    val json = """{"showapi_res_code":-1,"showapi_res_error":"appKey无效"}"""
    val r = client.parse(json, 200) as ShowApiResult.Failed
    assertEquals("appKey无效", r.message)
  }

  @Test
  fun networkErrorsDoNotEchoAppKeyUrl() {
    val raw = "failed to connect to https://route.showapi.com/1626-1?appKey=SUPER_SECRET_VALUE&x=1"
    val redacted = redactSecrets(raw)
    assertTrue("secret leaked: $redacted", "SUPER_SECRET_VALUE" !in redacted)
    assertTrue("raw url leaked: $redacted", "route.showapi.com" !in redacted)
    assertTrue(redacted.contains("[endpoint]") || redacted.contains("appKey=***"))
    val onlyQuery = redactSecrets("status for appKey=SUPER_SECRET_VALUE was 401")
    assertTrue("SUPER_SECRET_VALUE" !in onlyQuery)
    assertTrue(onlyQuery.contains("appKey=***"))
  }
}
