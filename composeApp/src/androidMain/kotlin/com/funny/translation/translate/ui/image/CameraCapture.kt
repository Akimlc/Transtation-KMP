package com.funny.translation.translate.ui.image

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.net.Uri
import android.os.Build
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.funny.translation.helper.CacheManager
import com.funny.translation.kmp.painterDrawableRes
import com.funny.translation.strings.ResStrings
import com.funny.translation.translate.utils.executor
import com.funny.translation.ui.FixedSizeIcon
import com.funny.translation.ui.Permission
import com.funny.translation.ui.touchToScale
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@Composable
fun CameraCapture(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onSavedImageFile: (Uri) -> Unit = { },
    onError: (Exception) -> Unit = {},
    startChooseImage: () -> Unit,
) {
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()

    // 进入页面时隐藏底部栏
    DisposableEffect(key1 = systemUiController) {
        systemUiController.isSystemBarsVisible = false
        onDispose {
            systemUiController.isSystemBarsVisible = true
        }
    }

    Permission(
        permission = Manifest.permission.CAMERA,
        description = ResStrings.need_camera_permission_tip,
    ) {
        Box(modifier = modifier) {
            val coroutineScope = rememberCoroutineScope()
            val cameraState: MutableState<Camera?> = remember {
                mutableStateOf(null)
            }

            val imageCaptureUseCase by remember {
                mutableStateOf(
                    ImageCapture.Builder()
                        .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                )
            }

            val controlLineContent: (@Composable () -> Unit) = remember {
                {
                    FlashlightButton(cameraControl = cameraState.value?.cameraControl)
                    CapturePictureButton(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(16.dp),
                        onClick = {
                            coroutineScope.launch {
                                imageCaptureUseCase.takePicture(
                                    OutputFileOptions.Builder(photoCachePath).build(),
                                    context.executor,
                                    object : OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            outputFileResults.savedUri?.let { onSavedImageFile(it) }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            onError(exception)
                                        }
                                    }
                                )
                            }
                        }
                    )
                    FixedSizeIcon(
                        painter = painterDrawableRes("ic_album"),
                        contentDescription = ResStrings.album,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .touchToScale(onClick = startChooseImage)
                            .size(40.dp)
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                var currentRotation by remember {
                    mutableStateOf(Surface.ROTATION_0)
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner){
                    val orientationEventListener by lazy {
                        object : OrientationEventListener(context) {
                            override fun onOrientationChanged(orientation: Int) {
                                if (orientation == ORIENTATION_UNKNOWN) {
                                    return
                                }
                                currentRotation = when (orientation) {
                                    in 45 until 135 -> Surface.ROTATION_270
                                    in 135 until 225 -> Surface.ROTATION_180
                                    in 225 until 315 -> Surface.ROTATION_90
                                    else -> Surface.ROTATION_0
                                }
                            }
                        }
                    }
                    val lifecycleObserver = LifecycleEventObserver { _, event ->
                        when(event){
                            Lifecycle.Event.ON_RESUME -> orientationEventListener.enable()
                            Lifecycle.Event.ON_STOP   -> orientationEventListener.disable()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                    }
                }

                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    imageCaptureUseCase = imageCaptureUseCase,
                    cameraSelector = cameraSelector,
                    cameraState = cameraState,
                    rotationProvider = { currentRotation }
                )
                // 竖屏
                if (currentRotation == Surface.ROTATION_0 || currentRotation == Surface.ROTATION_180) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        controlLineContent()
                    }
                }
                // 横屏
                else {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd),
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        controlLineContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashlightButton(
    cameraControl: CameraControl?
) {
    val context = LocalContext.current
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    var flashlightEnabled by remember {
        mutableStateOf(false)
    }
    val torchCallback = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    flashlightEnabled = enabled
                }
            }
        } else {
            null
        }
    }

    DisposableEffect(cameraManager) {
        torchCallback?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.registerTorchCallback(it, null)
            }
        }
        onDispose {
            torchCallback?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.unregisterTorchCallback(it)
                }
            }
        }
    }

    IconToggleButton(
        checked = flashlightEnabled,
        onCheckedChange = {
            flashlightEnabled = it
            cameraControl?.enableTorch(it)
        }
    ) {
        FixedSizeIcon(if (flashlightEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
            ResStrings.flashlight, modifier = Modifier.size(36.dp), tint = Color.White)
    }
}

private val photoCachePath = CacheManager.cacheDir.resolve("temp_captured_photo.jpeg")