package com.scarlett.yuedujilu.ui.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.scarlett.yuedujilu.ui.common.FormField
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader

@Composable
fun IsbnEntryScreen(vm: AppViewModel, state: AppUiState) {
  var isbn by rememberSaveable { mutableStateOf("") }
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .padding(bottom = 20.dp)
  ) {
    ScreenHeader("输入 ISBN", onBack = vm::back)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
      FormField(
        isbn,
        { isbn = it.filter { ch -> ch.isDigit() || ch.equals('X', true) } },
        "ISBN",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      )
      Spacer(Modifier.height(16.dp))
      NoticeCard(
        if (state.hasShowApiKey)
          "将使用已保存的万维易源 AppKey 查询。查询失败时会保留 ISBN，供你手填。"
        else
          "还没有填写万维易源 AppKey，查询不会发出。你仍可带着 ISBN 进入手填。"
      )
      Spacer(Modifier.height(24.dp))
      PrimaryButton(
        if (state.lookupBusy) "查询中…" else "查询并添加",
        onClick = { vm.lookupIsbn(isbn) },
        enabled = isbn.length >= 8 && !state.lookupBusy,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
