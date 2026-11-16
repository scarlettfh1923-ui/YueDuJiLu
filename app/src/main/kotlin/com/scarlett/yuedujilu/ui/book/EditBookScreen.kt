package com.scarlett.yuedujilu.ui.book

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.Route
import com.scarlett.yuedujilu.domain.BookSource
import com.scarlett.yuedujilu.ui.common.FormField
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader

@Composable
fun EditBookScreen(vm: AppViewModel, state: AppUiState, route: Route.EditBook) {
  val existing = route.bookId?.let { id -> state.snapshot.books.find { it.id == id } }
  var title by rememberSaveable { mutableStateOf(existing?.title ?: route.presetTitle.orEmpty()) }
  var author by rememberSaveable { mutableStateOf(existing?.author ?: route.presetAuthor.orEmpty()) }
  var isbn by rememberSaveable { mutableStateOf(existing?.isbn ?: route.presetIsbn.orEmpty()) }
  var pages by rememberSaveable { mutableStateOf((existing?.pageCount ?: route.presetPages)?.toString().orEmpty()) }
  val source = existing?.source ?: if (isbn.isNotBlank()) BookSource.ISBN else BookSource.MANUAL
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    ScreenHeader(if (existing == null) "添加书籍" else "编辑书籍", onBack = vm::back)
    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
    ) {
      FormField(title, { title = it }, "书名")
      Spacer(Modifier.height(16.dp))
      FormField(author, { author = it }, "作者")
      Spacer(Modifier.height(16.dp))
      FormField(isbn, { isbn = it }, "ISBN（可选）", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
      Spacer(Modifier.height(16.dp))
      FormField(pages, { pages = it.filter { ch -> ch.isDigit() } }, "总页数（可选）", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
      Spacer(Modifier.height(24.dp))
      PrimaryButton(
        if (existing == null) "加入书架" else "保存",
        onClick = { vm.saveBook(existing?.id, title, author, isbn, pages, source) },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(32.dp))
    }
  }
}
