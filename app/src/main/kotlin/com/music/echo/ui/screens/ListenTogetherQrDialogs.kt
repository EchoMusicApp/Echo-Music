package echo.music.iad1tya.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import echo.music.iad1tya.R
import echo.music.iad1tya.listentogether.ListenTogetherInvite
import echo.music.iad1tya.ui.component.DefaultDialog
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

@Composable
internal fun ListenTogetherRoomQrDialog(roomCode: String, onDismiss: () -> Unit) {
  val qr =
    remember(roomCode) {
      val matrix =
        QRCodeWriter().encode(ListenTogetherInvite.link(roomCode), BarcodeFormat.QR_CODE, 0, 0)
      val pixels =
        IntArray(matrix.width * matrix.height) { i ->
          if (matrix[i % matrix.width, i / matrix.width]) android.graphics.Color.BLACK
          else android.graphics.Color.WHITE
        }
      Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        .asImageBitmap()
    }

  DefaultDialog(
    onDismiss = onDismiss,
    title = { Text(stringResource(R.string.listen_together_room_qr)) },
    buttons = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
  ) {
    Image(
      bitmap = qr,
      contentDescription = stringResource(R.string.listen_together_room_qr_code, roomCode),
      modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(1f),
      filterQuality = FilterQuality.None,
    )
    Spacer(Modifier.height(16.dp))
    Text(
      stringResource(R.string.listen_together_room_qr_help),
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
internal fun ListenTogetherQrScannerDialog(onScanned: (String) -> Unit, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var invalidCode by remember { mutableStateOf(false) }
  var cameraError by remember { mutableStateOf(false) }
  val permission =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      hasPermission = it
    }

  DefaultDialog(
    onDismiss = onDismiss,
    title = { Text(stringResource(R.string.listen_together_scan_qr)) },
    buttons = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
      if (hasCamera && !hasPermission) {
        TextButton(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
          Text(stringResource(R.string.listen_together_allow_camera))
        }
      }
    },
  ) {
    if (hasCamera && hasPermission && !cameraError) {
      ListenTogetherQrPreview(
        onScanned = onScanned,
        onInvalidCode = { invalidCode = true },
        onError = { cameraError = true },
      )
      Spacer(Modifier.height(12.dp))
    }

    Text(
      stringResource(
        when {
          !hasCamera || cameraError -> R.string.listen_together_camera_unavailable
          !hasPermission -> R.string.listen_together_camera_permission
          invalidCode -> R.string.listen_together_invalid_qr
          else -> R.string.listen_together_scan_qr_help
        }
      ),
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun ListenTogetherQrPreview(
  onScanned: (String) -> Unit,
  onInvalidCode: () -> Unit,
  onError: () -> Unit,
) {
  val context = LocalContext.current
  val owner = LocalLifecycleOwner.current
  val currentOnScanned by rememberUpdatedState(onScanned)
  val currentOnInvalidCode by rememberUpdatedState(onInvalidCode)
  val currentOnError by rememberUpdatedState(onError)
  val view =
    remember(context) {
      PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FIT_CENTER
      }
    }

  DisposableEffect(view, owner) {
    val mainExecutor = ContextCompat.getMainExecutor(context)
    val active = AtomicBoolean(true)
    val decoded = AtomicBoolean(false)
    val executor = Executors.newSingleThreadExecutor()
    val reader = QRCodeReader()
    var rejectedPayload: String? = null
    var provider: ProcessCameraProvider? = null
    val preview = Preview.Builder().build().apply { setSurfaceProvider(view.surfaceProvider) }
    val analysis =
      ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    analysis.setAnalyzer(executor) { image ->
      val payload =
        image.use { frame ->
          if (active.get() && !decoded.get()) frame.readQrCode(reader) else null
        } ?: return@setAnalyzer
      val roomCode = ListenTogetherInvite.roomCodeFromQr(payload)
      if (roomCode != null && decoded.compareAndSet(false, true)) {
        mainExecutor.execute { if (active.get()) currentOnScanned(roomCode) }
      } else if (roomCode == null && rejectedPayload != payload) {
        rejectedPayload = payload
        mainExecutor.execute { if (active.get()) currentOnInvalidCode() }
      }
    }

    val future = ProcessCameraProvider.getInstance(context)
    future.addListener(
      {
        if (active.get()) {
          try {
            val camera = future.get()
            provider = camera
            val selector =
              if (camera.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                CameraSelector.DEFAULT_BACK_CAMERA
              } else CameraSelector.DEFAULT_FRONT_CAMERA
            camera.bindToLifecycle(owner, selector, preview, analysis)
          } catch (e: Exception) {
            Timber.w(e, "Could not start the Listen Together QR scanner")
            currentOnError()
          }
        }
      },
      mainExecutor,
    )

    onDispose {
      active.set(false)
      analysis.clearAnalyzer()
      provider?.unbind(preview, analysis)
      executor.shutdown()
    }
  }

  AndroidView(
    factory = { view },
    modifier =
      Modifier.widthIn(max = 280.dp)
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.Black),
  )
}

private fun ImageProxy.readQrCode(reader: QRCodeReader): String? {
  val plane = planes.firstOrNull() ?: return null
  val buffer = plane.buffer.duplicate()
  val start = buffer.position()
  if (plane.pixelStride != 1 || start + (height - 1) * plane.rowStride + width > buffer.limit()) {
    return null
  }
  val luminance = ByteArray(width * height)
  for (row in 0 until height) {
    buffer.position(start + row * plane.rowStride)
    buffer.get(luminance, row * width, width)
  }
  val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
  return try {
    reader.decode(BinaryBitmap(HybridBinarizer(source))).text
  } catch (_: ReaderException) {
    null
  }
}
