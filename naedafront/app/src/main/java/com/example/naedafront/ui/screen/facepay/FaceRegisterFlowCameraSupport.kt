package com.example.naedafront.ui.screen.facepay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.naedafront.data.remote.ApiRequestException
import com.example.naedafront.data.remote.FaceRegistrationRepository
import com.example.naedafront.data.remote.HeadPoseCheckResponseDto
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max

@Composable
internal fun FaceRegistrationCameraCard(
    modifier: Modifier,
    cameraSelector: CameraSelector,
    onFaceFrame: (ImageProxy, Face, Double) -> Unit,
    onNoFace: () -> Unit,
    overlay: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFaceFrame by rememberUpdatedState(onFaceFrame)
    val latestOnNoFace by rememberUpdatedState(onNoFace)
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            this.cameraSelector = cameraSelector
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_ANALYSIS or LifecycleCameraController.IMAGE_CAPTURE
            )
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    DisposableEffect(cameraController, lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return@setImageAnalysisAnalyzer
            }

            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val bestFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (bestFace == null) {
                        latestOnNoFace()
                        imageProxy.close()
                    } else {
                        latestOnFaceFrame(imageProxy, bestFace, estimateRegistrationLuminance(imageProxy))
                    }
                }
                .addOnFailureListener {
                    imageProxy.close()
                }
        }

        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            detector.close()
            executor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }
        )
        overlay()
    }
}

@Composable
internal fun DocumentCaptureCameraCard(
    modifier: Modifier,
    onFrame: (ImageProxy) -> Unit,
    overlay: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFrame by rememberUpdatedState(onFrame)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_ANALYSIS or LifecycleCameraController.IMAGE_CAPTURE
            )
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    DisposableEffect(cameraController, lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            latestOnFrame(imageProxy)
        }

        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            executor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }
        )
        overlay()
    }
}

@Composable
internal fun rememberDevicePostureState(): DevicePostureState {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    var postureState by remember {
        mutableStateOf(
            DevicePostureState(
                isUpright = false,
                message = "휴대폰을 세로로 똑바로 세워주세요."
            )
        )
    }

    DisposableEffect(sensorManager) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val y = event.values[1]
                val z = event.values[2]
                val upright = abs(z) < 5.5f && abs(y) > 6.5f
                postureState = DevicePostureState(
                    isUpright = upright,
                    message = if (upright) "휴대폰 자세가 정상입니다." else "휴대폰을 똑바로 세워주세요."
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return postureState
}

internal suspend fun enrollWithFallback(
    pose: String,
    payload: RegistrationFaceFramePayload
) {
    try {
        FaceRegistrationRepository.enrollFace(pose, payload.croppedFaceJpeg)
    } catch (exception: ApiRequestException) {
        if (exception.errorCode == "NO_FACE") {
            FaceRegistrationRepository.enrollFace(pose, payload.fullFrameJpeg)
        } else {
            throw exception
        }
    }
}

internal fun ensureHeadPoseMatched(
    response: HeadPoseCheckResponseDto,
    spec: FaceCaptureSpec
) {
    if (!response.matched) {
        val detected = response.detectedDirection ?: "unknown"
        val yaw = String.format("%.2f", response.yaw)
        val pitch = String.format("%.2f", response.pitch)
        throw IllegalStateException(
            "${spec.title} 자세가 감지되지 않았습니다. AI 판정=$detected (yaw=$yaw, pitch=$pitch)"
        )
    }
}


internal fun isFaceCentered(face: Face, frameWidth: Int, frameHeight: Int): Boolean {
    val box = face.boundingBox
    val centerX = box.centerX().toFloat() / frameWidth.toFloat()
    val centerY = box.centerY().toFloat() / frameHeight.toFloat()
    return centerX in 0.3f..0.7f && centerY in 0.25f..0.75f
}


internal fun resetHold(holdStartedAt: AtomicLong, updateProgress: (Float) -> Unit) {
    holdStartedAt.set(0L)
    updateProgress(0f)
}

internal class RegistrationPassiveLivenessEvaluator {
    private var frameCount = 0
    private var movementScore = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var yawMin = 0f
    private var yawMax = 0f
    private var pitchMin = 0f
    private var pitchMax = 0f
    private var blinkState = 0
    private var blinkDetected = false
    private var guide = "실제 얼굴 여부를 확인 중입니다."

    fun observe(face: Face): Boolean {
        frameCount += 1
        val box = face.boundingBox
        val centerX = (box.left + box.right) / 2f
        val centerY = (box.top + box.bottom) / 2f
        if (frameCount == 1) {
            lastX = centerX
            lastY = centerY
            yawMin = face.headEulerAngleY
            yawMax = face.headEulerAngleY
            pitchMin = face.headEulerAngleX
            pitchMax = face.headEulerAngleX
        } else {
            movementScore += abs(centerX - lastX) + abs(centerY - lastY)
            yawMin = minOf(yawMin, face.headEulerAngleY)
            yawMax = maxOf(yawMax, face.headEulerAngleY)
            pitchMin = minOf(pitchMin, face.headEulerAngleX)
            pitchMax = maxOf(pitchMax, face.headEulerAngleX)
            lastX = centerX
            lastY = centerY
        }

        val leftEye = face.leftEyeOpenProbability ?: 1f
        val rightEye = face.rightEyeOpenProbability ?: 1f
        if (leftEye < 0.35f || rightEye < 0.35f) {
            if (blinkState == 0) {
                blinkState = 1
            }
        } else if (blinkState == 1 && leftEye > 0.7f && rightEye > 0.7f) {
            blinkDetected = true
            blinkState = 2
        }

        guide = when {
            !blinkDetected && frameCount < 5 -> "눈을 한 번 깜빡이거나 얼굴을 조금 움직여주세요."
            !blinkDetected && movementScore < 20f -> "정면을 유지한 채 미세하게 움직여주세요."
            else -> "실제 얼굴로 판단되었습니다."
        }

        return blinkDetected || movementScore > 24f || (yawMax - yawMin) > 6f || (pitchMax - pitchMin) > 6f
    }

    fun guideText(): String = guide
}

private fun estimateRegistrationLuminance(imageProxy: ImageProxy): Double {
    val buffer = imageProxy.planes.firstOrNull()?.buffer ?: return 0.0
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    if (bytes.isEmpty()) return 0.0

    var total = 0L
    var count = 0
    val step = maxOf(1, bytes.size / 1500)
    var index = 0
    while (index < bytes.size) {
        total += bytes[index].toInt() and 0xFF
        count += 1
        index += step
    }
    return if (count == 0) 0.0 else total.toDouble() / count
}

internal fun imageProxyToRegistrationJpegBytes(imageProxy: ImageProxy): ByteArray {
    val nv21 = yuv420888ToNv21Registration(imageProxy)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
    val output = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 92, output)
    val jpegBytes = output.toByteArray()
    return rotateRegistrationJpeg(jpegBytes, imageProxy.imageInfo.rotationDegrees)
}

internal fun createRegistrationFaceFramePayload(
    imageProxy: ImageProxy,
    faceBounds: Rect
): RegistrationFaceFramePayload {
    val fullJpeg = imageProxyToRegistrationJpegBytes(imageProxy)
    return RegistrationFaceFramePayload(
        fullFrameJpeg = fullJpeg,
        croppedFaceJpeg = cropRegistrationFaceJpeg(fullJpeg, faceBounds)
    )
}

private fun rotateRegistrationJpeg(jpegBytes: ByteArray, rotationDegrees: Int): ByteArray {
    if (rotationDegrees == 0) return jpegBytes
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val output = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, 92, output)
    bitmap.recycle()
    rotated.recycle()
    return output.toByteArray()
}

private fun cropRegistrationFaceJpeg(jpegBytes: ByteArray, faceBounds: Rect): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val cropRect = expandedRegistrationFaceRect(faceBounds, bitmap.width, bitmap.height)
    if (cropRect.width() <= 0 || cropRect.height() <= 0) {
        bitmap.recycle()
        return jpegBytes
    }

    val croppedBitmap = Bitmap.createBitmap(
        bitmap,
        cropRect.left,
        cropRect.top,
        cropRect.width(),
        cropRect.height()
    )
    bitmap.recycle()

    val resizedBitmap = resizeRegistrationBitmapIfNeeded(croppedBitmap, 720)
    if (resizedBitmap !== croppedBitmap) {
        croppedBitmap.recycle()
    }

    val output = ByteArrayOutputStream()
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
    resizedBitmap.recycle()
    return output.toByteArray()
}

private fun expandedRegistrationFaceRect(faceBounds: Rect, imageWidth: Int, imageHeight: Int): Rect {
    val centerX = faceBounds.centerX().toFloat()
    val centerY = faceBounds.centerY().toFloat()
    val targetSize = (max(faceBounds.width(), faceBounds.height()) * 1.8f).toInt().coerceAtLeast(1)
    var left = (centerX - targetSize / 2f).toInt()
    var top = (centerY - targetSize / 2f).toInt()
    var right = left + targetSize
    var bottom = top + targetSize

    if (left < 0) {
        right = (right - left).coerceAtMost(imageWidth)
        left = 0
    }
    if (top < 0) {
        bottom = (bottom - top).coerceAtMost(imageHeight)
        top = 0
    }
    if (right > imageWidth) {
        val delta = right - imageWidth
        left = (left - delta).coerceAtLeast(0)
        right = imageWidth
    }
    if (bottom > imageHeight) {
        val delta = bottom - imageHeight
        top = (top - delta).coerceAtLeast(0)
        bottom = imageHeight
    }

    return Rect(left, top, right, bottom)
}

private fun resizeRegistrationBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val currentMax = max(bitmap.width, bitmap.height)
    if (currentMax <= maxDimension) {
        return bitmap
    }

    val scale = maxDimension / currentMax.toFloat()
    val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
}

private fun yuv420888ToNv21Registration(image: ImageProxy): ByteArray {
    val width = image.width
    val height = image.height
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val yBuffer = yPlane.buffer.duplicate()
    val uBuffer = uPlane.buffer.duplicate()
    val vBuffer = vPlane.buffer.duplicate()

    val nv21 = ByteArray(width * height * 3 / 2)
    var outputOffset = 0

    for (row in 0 until height) {
        val rowStart = row * yPlane.rowStride
        for (col in 0 until width) {
            val index = rowStart + col * yPlane.pixelStride
            nv21[outputOffset++] = yBuffer.get(index)
        }
    }

    val chromaWidth = width / 2
    val chromaHeight = height / 2
    for (row in 0 until chromaHeight) {
        val uRowStart = row * uPlane.rowStride
        val vRowStart = row * vPlane.rowStride
        for (col in 0 until chromaWidth) {
            nv21[outputOffset++] = vBuffer.get(vRowStart + col * vPlane.pixelStride)
            nv21[outputOffset++] = uBuffer.get(uRowStart + col * uPlane.pixelStride)
        }
    }

    return nv21
}






