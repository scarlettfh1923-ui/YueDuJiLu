package com.scarlett.yuedujilu.ui.shelf

import com.scarlett.yuedujilu.ui.theme.UiShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.Route
import com.scarlett.yuedujilu.SourceFilter
import com.scarlett.yuedujilu.StatusFilter
import com.scarlett.yuedujilu.domain.Book
import com.scarlett.yuedujilu.domain.BookSource
import com.scarlett.yuedujilu.ui.MainBottomSpace
import com.scarlett.yuedujilu.ui.common.EmptyState
import com.scarlett.yuedujilu.ui.common.FilterChip
import com.scarlett.yuedujilu.ui.common.IconButtonCircle
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.SearchField
import com.scarlett.yuedujilu.ui.common.StatusTabs
import com.scarlett.yuedujilu.ui.common.SourceSelector
import com.scarlett.yuedujilu.ui.common.progressLabel
import com.scarlett.yuedujilu.ui.common.sourceLabel
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import com.scarlett.yuedujilu.ui.theme.LocalRadii
import com.scarlett.yuedujilu.ui.theme.pressClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(vm: AppViewModel, state: AppUiState) {
  val p = LocalPalette.current
  val books = vm.visibleBooks(state)
  val active = vm.active()
  Column(
    Modifier
      .fillMaxSize()
      .statusBarsPadding()
  ) {
    Row(
      Modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SearchField(state.query, vm::setQuery, Modifier.weight(1f))
      Spacer(Modifier.width(10.dp))
      IconButtonCircle(AppIcons.Plus, "添加", emphasized = false, onClick = { vm.openPlus(true) })
    }
    val statuses = listOf(
      StatusFilter.ALL to "全部",
      StatusFilter.READING to "在读",
      StatusFilter.UNREAD to "想读",
      StatusFilter.FINISHED to "已读",
      StatusFilter.ABANDONED to "弃读",
    )
    val sources = listOf(
      SourceFilter.ALL to "全部来源",
      SourceFilter.PHYSICAL to "实体书",
      SourceFilter.WEREAD to "微信读书",
    )
    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp)) {
      StatusTabs(
        labels = statuses.map { it.second to (state.statusFilter == it.first) },
        onSelect = { vm.setStatus(statuses[it].first) },
        modifier = Modifier.fillMaxWidth(),
      )
      Row(Modifier.fillMaxWidth().padding(top=6.dp),verticalAlignment=Alignment.CenterVertically) {
      Text("${books.size} 本书",color=p.textSecondary,fontSize=12.sp,modifier=Modifier.weight(1f))
      SourceSelector(
        label = sources.first { it.first == state.sourceFilter }.second,
        options = sources.map { it.second to (state.sourceFilter == it.first) },
        onSelect = { vm.setSource(sources[it].first) },
      )
      }
    }
    if (books.isEmpty()) {
      val emptyTitle: String
      val emptySub: String
      when {
        state.snapshot.books.isEmpty() -> {
          emptyTitle = "书架还是空的"
          emptySub = "点右上角加号，扫描或输入 ISBN，也可以完全手填。这里不会预置示例书。"
        }
        state.sourceFilter == SourceFilter.WEREAD -> {
          emptyTitle = "还没有微信读书记录"
          emptySub = "微信日数据不会伪造成书架图书。前往「我的」设置同步，在统计查看真实阅读记录。"
        }
        else -> {
          emptyTitle = "没有符合条件的书"
          emptySub = "试试更换筛选，或清空搜索。"
        }
      }
      Column(Modifier.weight(1f).padding(top=52.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(AppIcons.Shelf,null,tint=p.primary.copy(alpha=.65f),modifier=Modifier.size(64.dp))
        EmptyState(emptyTitle,emptySub)
        PrimaryButton("添加第一本书",{vm.openPlus(true)})
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = MainBottomSpace.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        itemsIndexed(books, key = { _, b -> b.id }, contentType = { _, _ -> "book" }) { _, book ->
          BookRow(
            book = book,
            timerRunningOnThis = active?.bookId == book.id,
            onOpen = { vm.go(Route.BookDetail(book.id)) },
            onAction = { vm.startOrOpenTimer(book) },
          )
        }
      }
    }
  }
  if (state.showPlus) {
    ModalBottomSheet(
      onDismissRequest = { vm.openPlus(false) },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = p.surface,
      tonalElevation = 0.dp,
      shape = UiShape.Sheet,
    ) {
      Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("添加到书架", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("扫描与手输都会先查重 ISBN。查询依赖万维易源，未配置时仍可手填。", color = p.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(16.dp))
        PlusItem(AppIcons.Barcode, "扫描 ISBN", "使用本机相机识别条码") { vm.go(Route.ScanIsbn) }
        PlusItem(AppIcons.Keyboard, "输入 ISBN", "粘贴或手打 13 位号码") { vm.go(Route.IsbnEntry) }
        PlusItem(AppIcons.Manual, "手动填写", "没有条码时直接写书名") { vm.go(Route.EditBook(null)) }
        Spacer(Modifier.height(28.dp))
      }
    }
  }
}

@Composable
private fun PlusItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, onClick: () -> Unit) {
  val p = LocalPalette.current
  Row(
    Modifier
      .fillMaxWidth()
      .clip(UiShape.Regular)
      .pressClickable(onClick = onClick)
      .padding(vertical = 12.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier
        .size(44.dp)
        .clip(UiShape.Regular)
        .background(p.primarySoft),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, null, tint = p.primary, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
      Text(title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
      Text(sub, color = p.textSecondary, fontSize = 12.sp)
    }
    Icon(AppIcons.ChevronRight, null, tint = p.textTertiary, modifier = Modifier.size(18.dp))
  }
}

@Composable
fun BookRow(
  book: Book,
  timerRunningOnThis: Boolean,
  onOpen: () -> Unit,
  onAction: () -> Unit,
) {
  val p = LocalPalette.current
  Row(Modifier.fillMaxWidth().shadow(7.dp,UiShape.Regular,ambientColor=p.text.copy(alpha=.24f),spotColor=p.text.copy(alpha=.24f))
    .clip(UiShape.Regular).background(p.surfaceRaised).pressClickable(onClick=onOpen).padding(20.dp),verticalAlignment=Alignment.Top) {
    CoverPlate(book,width=104.dp)
    Spacer(Modifier.width(20.dp))
    Column(Modifier.weight(1f)) {
      Text(book.title,color=p.text,fontSize=20.sp,lineHeight=27.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
      if(book.author.isNotBlank()) Text(book.author,color=p.textSecondary,fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=5.dp))
      Text(book.sourceLabel(),color=p.textSecondary,fontSize=11.sp,
        modifier=Modifier.padding(top=12.dp).clip(UiShape.Capsule).background(p.primarySoft.copy(alpha=.55f)).padding(horizontal=9.dp,vertical=4.dp))
      if(book.source!=BookSource.WEREAD) {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
          Text(book.progressLabel(),color=p.text,fontSize=13.sp,modifier=Modifier.weight(1f))
          if((book.pageCount?:0)>0)Text("${(book.currentPage.toLong()*100/(book.pageCount?:1).coerceAtLeast(1)).coerceIn(0,100)}%",color=p.textSecondary,fontSize=11.sp)
        }
        val progress=if((book.pageCount?:0)>0)(book.currentPage.toFloat()/book.pageCount!!).coerceIn(0f,1f) else 0f
        Box(Modifier.padding(top=8.dp).fillMaxWidth().height(3.dp).clip(UiShape.Capsule).background(p.track)) {
          Box(Modifier.fillMaxWidth(progress).height(3.dp).background(p.primary.copy(alpha=.65f)))
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.defaultMinSize(minHeight=48.dp).pressClickable(onClick=onAction),contentAlignment=Alignment.CenterStart) {
          Row(Modifier.clip(UiShape.Capsule).background(p.primarySoft).padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(AppIcons.Play,null,tint=p.primary,modifier=Modifier.size(13.dp))
            Text(when {timerRunningOnThis->"查看计时";book.currentPage>0->"继续阅读";else->"开始阅读"},color=p.primary,fontWeight=FontWeight.SemiBold,fontSize=12.sp,modifier=Modifier.padding(start=5.dp))
          }
        }
      } else Text("查看记录",color=p.primary,fontSize=13.sp,modifier=Modifier.padding(top=20.dp))
    }
  }
}

/** Honest local placeholder: no fictional cover art or first-letter gradient. */
@Composable
fun CoverPlate(book: Book, width: androidx.compose.ui.unit.Dp) {
  val p=LocalPalette.current
  val shape=UiShape.Regular
  Box(Modifier.width(width).height(width*1.52f).shadow(4.dp,shape,ambientColor=p.shadow,spotColor=p.shadow)
    .clip(shape).background(p.surfaceMuted)) {
    Box(Modifier.align(Alignment.CenterEnd).width(5.dp).height(width*1.52f).background(p.stroke))
    Box(Modifier.align(Alignment.CenterEnd).padding(end=2.dp).width(1.dp).height(width*1.52f).background(p.surface))
    Box(Modifier.padding(start=7.dp).width(1.dp).height(width*1.48f).background(p.primary.copy(alpha=.2f)))
    Column(Modifier.fillMaxSize().padding(horizontal=14.dp,vertical=20.dp),verticalArrangement=Arrangement.SpaceBetween) {
      Text(book.title,color=p.text,fontSize=if(width.value>85)14.sp else 12.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium,maxLines=4,overflow=TextOverflow.Ellipsis)
      Text("暂无封面",color=p.textSecondary,fontSize=10.sp)
    }
  }
}
