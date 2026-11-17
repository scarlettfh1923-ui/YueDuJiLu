package com.scarlett.yuedujilu.ui.scan

import com.scarlett.yuedujilu.ui.theme.UiShape
import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.scarlett.yuedujilu.AppViewModel
import com.scarlett.yuedujilu.Route
import com.scarlett.yuedujilu.domain.Isbn
import com.scarlett.yuedujilu.domain.IsbnParse
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.QuietButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay

@Composable
fun ScanIsbnScreen(vm: AppViewModel) {
  val p = LocalPalette.current
  val context = LocalContext.current
  var granted by remember {
    mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
  }
  var asked by remember { mutableStateOf(false) }
  var hint by remember { mutableStateOf<String?>(null) }
  var offerManual by remember { mutableStateOf(false) }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    granted = it
    asked = true
  }
  LaunchedEffect(Unit) {
    if (!granted) launcher.launch(Manifest.permission.CAMERA)
  }
  LaunchedEffect(granted) {
    if (!granted) return@LaunchedEffect
    delay(15_000)
    offerManual = true
  }
  Column(
    Modifier
      .fillMaxSize()
      .background(p.background)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    ScreenHeader("扫描 ISBN", onBack = vm::back)
    when {
      granted -> {
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal=24.dp).clip(UiShape.Regular)) {
          CameraIsbnPreview(
            onIsbn = { code -> vm.lookupIsbn(code) },
            onHint = { hint = it },
          )
          Box(Modifier.align(Alignment.Center).fillMaxWidth(.82f).height(140.dp).border(1.5f.dp,Color.White.copy(alpha=.8f),UiShape.Regular))
        }
        Column(Modifier.padding(24.dp).heightIn(max=280.dp).verticalScroll(rememberScrollState())) {
          Text(
            "对准书背或封底上的 ISBN 条码。也可以扫描写有 ISBN 的二维码；普通链接或商品条码不会打开，只会给出提示。",
            color = p.textSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
          )
          hint?.let {
            Spacer(Modifier.height(10.dp))
            NoticeCard(it)
          }
          if (offerManual) {
            Spacer(Modifier.height(14.dp))
            Text("还没有识别到 ISBN，可以改用手填。", color = p.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            QuietButton("改为手输 ISBN", onClick = { vm.go(Route.IsbnEntry) }, modifier = Modifier.fillMaxWidth())
          }
        }
      }
      else -> {
        Column(Modifier.padding(24.dp).heightIn(max=280.dp).verticalScroll(rememberScrollState())) {
          NoticeCard("扫描书背或封底上的 ISBN 条码需要相机权限，仅在你点扫描时申请。未授权时仍可手输 ISBN 或手动填书。")
          Spacer(Modifier.height(16.dp))
          PrimaryButton("授予相机权限", onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth())
          if (asked && !granted) {
            Spacer(Modifier.height(12.dp))
            Text("权限被拒绝。你可以到系统设置中开启，或改用手输 ISBN。", color = p.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(12.dp))
            QuietButton("改为手输 ISBN", onClick = { vm.go(Route.IsbnEntry) }, modifier = Modifier.fillMaxWidth())
          }
        }
      }
    }
  }
}

@Composable
private fun CameraIsbnPreview(
  onIsbn: (String) -> Unit,
  onHint: (String) -> Unit,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val context = LocalContext.current
  var previewView by remember { mutableStateOf<PreviewView?>(null) }
  val taken = remember { AtomicBoolean(false) }

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      PreviewView(ctx).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
      }
    },
    update = { view ->
      if (previewView !== view) previewView = view
    },
  )

  DisposableEffect(lifecycleOwner, previewView) {
    val view = previewView
    if (view == null) {
      return@DisposableEffect onDispose { }
    }
    val executor = Executors.newSingleThreadExecutor()
    val options = BarcodeScannerOptions.Builder()
      .setBarcodeFormats(
        Barcode.FORMAT_EAN_13,
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_UPC_A,
        Barcode.FORMAT_UPC_E,
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_QR_CODE,
      )
      .build()
    val scanner = BarcodeScanning.getClient(options)
    val disposed = AtomicBoolean(false)
    val lastHint = AtomicReference<String?>(null)
    val mainExec = ContextCompat.getMainExecutor(context)
    var provider: ProcessCameraProvider? = null
    val analysis = ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      .setResolutionSelector(
        ResolutionSelector.Builder()
          .setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
          .build()
      )
      .build()
    // 手绑 Preview + ImageAnalysis 时必须用 ORIGINAL。VIEW_REFERENCED 依赖
    // CameraController.updateTransform，在 CameraX 1.5.2 下手绑会导致每帧直接 close。
    // 本页不绘制条码框，不需要视图坐标。
    analysis.setAnalyzer(
      executor,
      MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, mainExec) { result ->
        if (disposed.get() || taken.get()) return@MlKitAnalyzer
        val err = result.getThrowable(scanner)
        if (err != null) {
          val msg = "识别暂时失败：${err.message ?: err.javaClass.simpleName}"
          if (lastHint.getAndSet(msg) != msg) {
            view.post { if (!disposed.get()) onHint(msg) }
          }
          return@MlKitAnalyzer
        }
        val codes = result.getValue(scanner).orEmpty()
        val raw = codes.firstOrNull()?.rawValue ?: return@MlKitAnalyzer
        when (val parsed = Isbn.extractIsbn(raw)) {
          is IsbnParse.Valid -> {
            if (taken.compareAndSet(false, true)) {
              view.post { if (!disposed.get()) onIsbn(parsed.isbn) }
            }
          }
          is IsbnParse.Rejected -> {
            if (lastHint.getAndSet(parsed.reason) != parsed.reason) {
              view.post { if (!disposed.get()) onHint(parsed.reason) }
            }
          }
        }
      },
    )
    val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
      if (disposed.get()) return@addListener
      try {
        val p = future.get()
        if (disposed.get()) return@addListener
        provider = p
        p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
      } catch (e: Exception) {
        val msg = "无法打开相机：${e.message ?: e.javaClass.simpleName}。请改用手输 ISBN。"
        view.post { if (!disposed.get()) onHint(msg) }
      }
    }, mainExec)

    onDispose {
      disposed.set(true)
      runCatching { analysis.clearAnalyzer() }
      runCatching { provider?.unbind(preview, analysis) }
      runCatching { scanner.close() }
      // Only the frame worker stops. MLKit completion callbacks use mainExec and may
      // still close an in-flight ImageProxy after this composition is disposed.
      executor.shutdown()
    }
  }
}
