package com.scarlett.yuedujilu.ui

import com.scarlett.yuedujilu.ui.theme.UiShape
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.AppUiState
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.MainTab
import com.scarlett.yuedujilu.Route
import com.scarlett.yuedujilu.domain.ReadingMath
import com.scarlett.yuedujilu.ui.book.BackfillScreen
import com.scarlett.yuedujilu.ui.book.BookDetailScreen
import com.scarlett.yuedujilu.ui.book.EditBookScreen
import com.scarlett.yuedujilu.ui.book.EndSessionScreen
import com.scarlett.yuedujilu.ui.common.formatHms
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.mine.MineScreen
import com.scarlett.yuedujilu.ui.scan.IsbnEntryScreen
import com.scarlett.yuedujilu.ui.scan.ScanIsbnScreen
import com.scarlett.yuedujilu.ui.settings.AiEntryScreen
import com.scarlett.yuedujilu.ui.settings.AppearanceScreen
import com.scarlett.yuedujilu.ui.settings.BackupScreen
import com.scarlett.yuedujilu.ui.settings.DeepSeekScreen
import com.scarlett.yuedujilu.ui.settings.ExportScreen
import com.scarlett.yuedujilu.ui.settings.ShowApiConfigScreen
import com.scarlett.yuedujilu.ui.settings.WereadScreen
import com.scarlett.yuedujilu.ui.shelf.ShelfScreen
import com.scarlett.yuedujilu.ui.stats.StatsScreen
import com.scarlett.yuedujilu.ui.theme.LocalMotion
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import com.scarlett.yuedujilu.ui.theme.LocalRadii
import com.scarlett.yuedujilu.ui.theme.GlassTreatment
import com.scarlett.yuedujilu.ui.theme.LocalGlassTreatment
import com.scarlett.yuedujilu.ui.theme.LocalBackdrop
import com.scarlett.yuedujilu.ui.theme.glassSurface
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.scarlett.yuedujilu.ui.theme.pressClickable

@Composable
fun AppRoot(vm: AppViewModel, state: AppUiState) {
  val p = LocalPalette.current
  val snack = remember { SnackbarHostState() }
  val routeState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
  LaunchedEffect(state.message) {
    val msg = state.message ?: return@LaunchedEffect
    snack.showSnackbar(msg.text)
    vm.clearMessage()
  }
  BackHandler(enabled = state.route != Route.Main) { vm.back() }

  Box(
    Modifier
      .fillMaxSize()
      .background(p.background)
  ) {
    routeState.SaveableStateProvider(state.route.toString()) {
    when (val route = state.route) {
      Route.Main, is Route.StatsDetail -> MainTabs(vm, state)
      is Route.BookDetail -> BookDetailScreen(vm, state, route.bookId)
      is Route.EditBook -> EditBookScreen(vm, state, route)
      Route.IsbnEntry -> IsbnEntryScreen(vm, state)
      Route.ScanIsbn -> ScanIsbnScreen(vm)
      is Route.EndSession -> EndSessionScreen(vm, state, route.bookId)
      is Route.Backfill -> BackfillScreen(vm, state, route.bookId, route.sessionId)
      Route.Appearance -> AppearanceScreen(vm, state)
      Route.Weread -> WereadScreen(vm, state)
      Route.ShowApiConfig -> ShowApiConfigScreen(vm, state)
      Route.AiEntry -> AiEntryScreen(vm)
      Route.DeepSeek -> DeepSeekScreen(vm, state)
      Route.Backup -> BackupScreen(vm)
      Route.Export -> ExportScreen(vm)
    }
    }
    SnackbarHost(
      snack,
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 8.dp),
    )
  }
}

@Composable
private fun MainTabs(vm: AppViewModel, state: AppUiState) {
  val active = vm.active()
  val activeBook = active?.let { s -> state.snapshot.books.find { it.id == s.bookId } }
  val treatment = LocalGlassTreatment.current
  val hazeState = rememberLayerBackdrop()
  val tabState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
  val scale=androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
  val navHeight=(76f+36f*(scale-1f)).dp
  val timerHeight=if(active!=null&&activeBook!=null)(maxOf(48f,40f*scale)+36f).dp else 0.dp
  val bottomSpace=navHeight+timerHeight+36.dp+androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  CompositionLocalProvider(LocalBackdrop provides hazeState,LocalMainBottomSpace provides bottomSpace) {
  Box(Modifier.fillMaxSize()) {
    Box(
      Modifier
        .fillMaxSize()
        .then(if (treatment == GlassTreatment.BLUR) Modifier.layerBackdrop(hazeState) else Modifier)
        .background(LocalPalette.current.background)
    ) {
      // Commit exactly one page. Crossfade superimposed outgoing/incoming text.
      // The independent liquid-nav indicator and press feedback still animate.
      tabState.SaveableStateProvider(state.tab.name) {
        if(state.route is Route.StatsDetail) com.scarlett.yuedujilu.ui.stats.StatsDetailScreen(vm,state,state.route.metric) else when (state.tab) {
          MainTab.SHELF -> ShelfScreen(vm, state)
          MainTab.STATS -> StatsScreen(vm, state)
          MainTab.MINE -> MineScreen(vm, state)
        }
      }
    }
    Column(
      Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val motion = LocalMotion.current
      AnimatedVisibility(
        visible = active != null && activeBook != null,
        enter = if (motion.weakenMotion) fadeIn(tween(200)) else fadeIn(tween(180)) + expandVertically(),
        exit = if (motion.weakenMotion) fadeOut(tween(200)) else shrinkVertically() + fadeOut(tween(180)),
      ) {
        if (active != null && activeBook != null) {
          Column {
            TimerBar(
              title = activeBook.title,
              elapsed = formatHms(ReadingMath.effectiveDurationMs(active, state.now)),
              paused = ReadingMath.isPaused(active),
              onToggle = { vm.pauseOrResume() },
              onOpen = { vm.go(Route.EndSession(active.bookId)) },
            )
            Spacer(Modifier.height(12.dp))
          }
        }
      }
      LiquidTabBar(selected = state.tab, onSelect = vm::setTab)
    }
  }
  }
}

@Composable
private fun TimerBar(
  title: String,
  elapsed: String,
  paused: Boolean,
  onToggle: () -> Unit,
  onOpen: () -> Unit,
) {
  val p = LocalPalette.current
  val shape = UiShape.Capsule
  Row(
    Modifier
      .fillMaxWidth()
      .glassSurface(shape)
      .then(Modifier.testTag("active-timer"))
      .pressClickable(onClick = onOpen)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(AppIcons.Clock, null, tint = p.text, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
      Text(title, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
      Text(elapsed, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
    Box(
      Modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .clip(UiShape.Capsule)
        .background(p.primarySoft)
        .pressClickable(onClick = onToggle)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (paused) AppIcons.Play else AppIcons.Pause, if (paused) "继续" else "暂停", tint = p.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (paused) "继续" else "暂停", color = p.primary, fontSize = 13.sp)
      }
    }
  }
}

val LocalMainBottomSpace = androidx.compose.runtime.staticCompositionLocalOf { 200.dp }
