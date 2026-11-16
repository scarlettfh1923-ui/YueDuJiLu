package com.scarlett.yuedujilu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsbnTest {
  // 公开示例：ISBN-13 978-0-306-40615-7，ISBN-10 0-306-40615-2
  private val isbn13 = "9780306406157"
  private val isbn10 = "0306406152"

  @Test
  fun validIsbn13_plainAndHyphenated() {
    assertEquals(IsbnParse.Valid(isbn13), Isbn.extractIsbn("978-0-306-40615-7"))
    assertEquals(IsbnParse.Valid(isbn13), Isbn.extractIsbn(isbn13))
    assertTrue(Isbn.isValidIsbn13(isbn13))
  }

  @Test
  fun validIsbn13_withIsbnPrefix() {
    assertEquals(IsbnParse.Valid(isbn13), Isbn.extractIsbn("ISBN 978-0-306-40615-7"))
    assertEquals(IsbnParse.Valid(isbn13), Isbn.extractIsbn("ISBN-13: 9780306406157"))
  }

  @Test
  fun validIsbn10_plainAndHyphenatedAndX() {
    assertEquals(IsbnParse.Valid(isbn10), Isbn.extractIsbn("0-306-40615-2"))
    assertEquals(IsbnParse.Valid(isbn10), Isbn.extractIsbn(isbn10))
    assertTrue(Isbn.isValidIsbn10("043942089X") || Isbn.isValidIsbn10("043942089x"))
    when (val r = Isbn.extractIsbn("043942089X")) {
      is IsbnParse.Valid -> assertEquals("043942089X", r.isbn)
      is IsbnParse.Rejected -> throw AssertionError(r.reason)
    }
  }

  @Test
  fun isbn13_mustBe978or979() {
    assertFalse(Isbn.isValidIsbn13("1234567890128"))
    when (val r = Isbn.extractIsbn("6901234567892")) {
      is IsbnParse.Rejected -> assertEquals(Isbn.REJECT_PRODUCT, r.reason)
      is IsbnParse.Valid -> throw AssertionError("商品码不应通过：${r.isbn}")
    }
  }

  @Test
  fun isbn13_badCheckDigit() {
    when (val r = Isbn.extractIsbn("9780306406158")) {
      is IsbnParse.Rejected -> assertEquals(Isbn.REJECT_CHECK, r.reason)
      is IsbnParse.Valid -> throw AssertionError("错误校验位不应通过")
    }
  }

  @Test
  fun isbn10_badCheckDigit() {
    when (val r = Isbn.extractIsbn("0306406153")) {
      is IsbnParse.Rejected -> assertEquals(Isbn.REJECT_CHECK, r.reason)
      is IsbnParse.Valid -> throw AssertionError("错误校验位不应通过")
    }
  }

  @Test
  fun rejectUrlAndDoNotTreatAsIsbn() {
    when (val r = Isbn.extractIsbn("https://example.com/book?q=9780306406157")) {
      is IsbnParse.Rejected -> assertEquals(Isbn.REJECT_URL, r.reason)
      is IsbnParse.Valid -> throw AssertionError("URL 不应被当作 ISBN")
    }
    when (val r = Isbn.extractIsbn("www.example.com")) {
      is IsbnParse.Rejected -> assertEquals(Isbn.REJECT_URL, r.reason)
      is IsbnParse.Valid -> throw AssertionError("www 链接不应通过")
    }
  }

  @Test
  fun rejectShortProductCodes() {
    when (val r = Isbn.extractIsbn("12345678")) {
      is IsbnParse.Rejected -> assertTrue(r.reason == Isbn.REJECT_PRODUCT || r.reason == Isbn.REJECT_INVALID)
      is IsbnParse.Valid -> throw AssertionError("8 位商品码不应通过")
    }
  }

  @Test
  fun emptyAndGarbage() {
    assertEquals(IsbnParse.Rejected(Isbn.REJECT_EMPTY), Isbn.extractIsbn("  "))
    assertEquals(IsbnParse.Rejected(Isbn.REJECT_EMPTY), Isbn.extractIsbn(null))
    when (val r = Isbn.extractIsbn("hello world")) {
      is IsbnParse.Rejected -> assertEquals(Isbn.REJECT_INVALID, r.reason)
      is IsbnParse.Valid -> throw AssertionError("乱码不应通过")
    }
  }
}
