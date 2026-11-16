package com.scarlett.yuedujilu.domain

/**
 * 从条码或二维码原文提取并严格校验 ISBN。
 * ISBN-13 必须 978/979 且校验位正确；ISBN-10 必须校验位正确。
 * 允许带横线、空格和 “ISBN” 前缀的文本；拒绝普通 URL 与非 ISBN 商品码。
 */
sealed class IsbnParse {
  data class Valid(val isbn: String) : IsbnParse()
  data class Rejected(val reason: String) : IsbnParse()
}

object Isbn {
  const val REJECT_EMPTY = "未识别到内容"
  const val REJECT_URL = "这是链接，不是图书 ISBN，不会打开"
  const val REJECT_PRODUCT = "这是商品条码，不是图书 ISBN"
  const val REJECT_CHECK = "ISBN 校验位不正确"
  const val REJECT_INVALID = "未识别到有效的 ISBN"

  private val urlLike = Regex("""(?i)(https?|ftp)://|^(www\.)|[a-z0-9-]+\.(com|cn|net|org|edu|io|me|co)(/|$)""")
  private val isbnPrefix = Regex("""(?i)isbn(?:-?1[03])?[:：\s]*""")
  private val isbn13 = Regex("""(?<!\d)\d{13}(?!\d)""")
  private val isbn10 = Regex("""(?<!\d)\d{9}[\dXx](?!\d)""")

  fun extractIsbn(raw: String?): IsbnParse {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return IsbnParse.Rejected(REJECT_EMPTY)
    if (looksLikeUrl(text)) return IsbnParse.Rejected(REJECT_URL)

    val stripped = isbnPrefix.replace(text, " ")
    if (looksLikeUrl(stripped)) return IsbnParse.Rejected(REJECT_URL)

    val compact = stripped.replace(Regex("""[\s-]+"""), "")
    val candidates = linkedSetOf<String>()
    isbn13.findAll(compact).forEach { candidates += it.value }
    isbn10.findAll(compact).forEach { candidates += it.value.uppercase() }

    if (candidates.isEmpty()) {
      val digits = compact.filter { it.isDigit() || it.equals('X', true) }.uppercase()
      when (digits.length) {
        13 -> candidates += digits
        10 -> candidates += digits
        else -> return IsbnParse.Rejected(if (digits.length in 8..14) REJECT_PRODUCT else REJECT_INVALID)
      }
    }

    val thirteen = candidates.filter { it.length == 13 }
    val ten = candidates.filter { it.length == 10 }
    thirteen.firstOrNull { isValidIsbn13(it) }?.let { return IsbnParse.Valid(it) }
    if (thirteen.any { it.startsWith("978") || it.startsWith("979") }) {
      return IsbnParse.Rejected(REJECT_CHECK)
    }
    if (thirteen.isNotEmpty()) return IsbnParse.Rejected(REJECT_PRODUCT)
    ten.firstOrNull { isValidIsbn10(it) }?.let { return IsbnParse.Valid(it) }
    if (ten.isNotEmpty()) return IsbnParse.Rejected(REJECT_CHECK)
    return IsbnParse.Rejected(REJECT_INVALID)
  }

  fun looksLikeUrl(text: String): Boolean {
    val t = text.trim()
    if (t.contains("://", ignoreCase = true)) return true
    if (t.startsWith("www.", ignoreCase = true)) return true
    return urlLike.containsMatchIn(t)
  }

  fun isValidIsbn13(value: String): Boolean {
    if (value.length != 13 || value.any { !it.isDigit() }) return false
    if (!value.startsWith("978") && !value.startsWith("979")) return false
    var sum = 0
    for (i in 0 until 13) {
      val n = value[i] - '0'
      sum += n * if (i % 2 == 0) 1 else 3
    }
    return sum % 10 == 0
  }

  fun isValidIsbn10(value: String): Boolean {
    if (value.length != 10) return false
    if (!value.take(9).all { it.isDigit() }) return false
    val last = value[9].uppercaseChar()
    if (!(last.isDigit() || last == 'X')) return false
    var sum = 0
    for (i in 0..8) sum += (value[i] - '0') * (10 - i)
    val check = if (last == 'X') 10 else last - '0'
    return (sum + check) % 11 == 0
  }
}
