package com.scarlett.yuedujilu.ui.scan

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
import com.scarlett.yuedujilu.ui.common.NoticeCard
import com.scarlett.yuedujilu.ui.common.PrimaryButton
import com.scarlett.yuedujilu.ui.common.ScreenHeader
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScanIsbnScreen(vm: AppViewModel) {
  val p = LocalPalette.current
  val context = LocalContext.current
  var granted by remember {
    mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
  }
  var asked by remember { mutableStateOf(false) }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    granted = it
    asked = true
  }
  LaunchedEffect(Unit) {
    if (!granted) launcher.launch(Manifest.permission.CAMERA)
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
        Box(Modifier.weight(1f).fillMaxWidth()) {
          CameraIsbnPreview(onIsbn = { code -> vm.lookupIsbn(code) })
        }
        Text(
          "将条码置于画面中。识别使用应用内的 ML Kit，不需要再装其他扫码应用。真机相机在本构建环境中未实测。",
          color = p.textSecondary,
          fontSize = 12.sp,
          modifier = Modifier.padding(20.dp),
          lineHeight = 18.sp,
        )
      }
      else -> {
        Column(Modifier.padding(20.dp)) {
          NoticeCard("扫描 ISBN 需要相机权限，仅在你点扫描时申请，用于识别封底条码。未授权时仍可手输 ISBN 或手动填书。")
          Spacer(Modifier.height(16.dp))
          PrimaryButton("授予相机权限", onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth())
          if (asked && !granted) {
            Spacer(Modifier.height(12.dp))
            Text("权限被拒绝。你可以到系统设置中开启，或改用手输 ISBN。", color = p.textSecondary, fontSize = 13.sp, lineHeight = 20.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun CameraIsbnPreview(onIsbn: (String) -> Unit) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val context = LocalContext.current
  val taken = remember { AtomicBoolean(false) }
  val executor = remember { Executors.newSingleThreadExecutor() }
  DisposableEffect(Unit) {
    onDispose { executor.shutdown() }
  }
  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      PreviewView(ctx).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
      }
    },
    update = { previewView ->
      val providerFuture = ProcessCameraProvider.getInstance(context)
      providerFuture.addListener({
        val provider = providerFuture.get()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val options = BarcodeScannerOptions.Builder()
          .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
          )
          .build()
        val scanner = BarcodeScanning.getClient(options)
        val analysis = ImageAnalysis.Builder()
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
          .setResolutionSelector(
            ResolutionSelector.Builder()
              .setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
              .build()
          )
          .build()
        analysis.setAnalyzer(
          executor,
          MlKitAnalyzer(listOf(scanner), androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED, executor) { result ->
            val codes = result.getValue(scanner) ?: return@MlKitAnalyzer
            val raw = codes.firstOrNull()?.rawValue ?: return@MlKitAnalyzer
            val cleaned = raw.filter { it.isDigit() || it.equals('X', true) }
            if (cleaned.length in 8..13 && taken.compareAndSet(false, true)) {
              previewView.post { onIsbn(cleaned) }
            }
          },
        )
        try {
          provider.unbindAll()
          provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        } catch (_: Exception) {
          // 无相机或绑定失败时保持画面空白，文案已提示未实测。
        }
      }, ContextCompat.getMainExecutor(context))
    },
  )
}
