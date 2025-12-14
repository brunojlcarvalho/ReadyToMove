package pt.ipp.estg.readytomove.ui.screens.profile

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import pt.ipp.estg.readytomove.R
import androidx.compose.ui.res.stringResource

/**
 * A screen that implements a custom camera interface using CameraX.
 *
 * This screen allows the user to:
 * - View a live camera preview.
 * - Switch between the front and back lenses.
 * - Capture a photo, which is saved to the local cache directory.
 *
 * @param onPhotoTaken Callback function invoked when a photo is successfully captured.
 * It returns the string URI of the saved image file.
 */
@Composable
fun CameraXScreen(
    onPhotoTaken: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State to track if the camera permission is granted
    var hasCameraPermission by remember { mutableStateOf(false) }

    // State to control which lens is currently active (Back camera by default)
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    // Launcher to request camera permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    // Check for permissions as soon as the screen launches
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        // PreviewView is a standard Android View, so we need to hold its reference
        val previewView = remember { PreviewView(context) }
        val imageCapture = remember { ImageCapture.Builder().build() }

        // Re-bind the camera whenever the selected lens changes
        LaunchedEffect(lensFacing) {
            val cameraProvider = context.getCameraProvider()

            // It is essential to unbind all use cases before rebinding to a new lens
            cameraProvider.unbindAll()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                // Bind the camera lifecycle to the Compose LifecycleOwner
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    },
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.camera_error_start), Toast.LENGTH_SHORT).show()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Embed the Android View (PreviewView) into Compose
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Capture Button (Bottom Center)
            IconButton(
                onClick = {
                    takePhoto(
                        filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS",
                        imageCapture = imageCapture,
                        outputDirectory = context.cacheDir,
                        executor = ContextCompat.getMainExecutor(context),
                        onImageCaptured = { uri ->
                            onPhotoTaken(uri.toString())
                        },
                        onError = {
                            Toast.makeText(context, context.getString(R.string.camera_error_capture), Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = stringResource(R.string.camera_desc_capture),
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Switch Camera Button (Top Right)
            IconButton(
                onClick = {
                    // Toggle between Front and Back lenses
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 24.dp)
                    .size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = stringResource(R.string.camera_desc_switch),
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    } else {
        // Fallback UI if permission is denied
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.camera_permission_needed))
        }
    }
}

/**
 * Helper function to capture a photo and save it to the specified directory.
 *
 * @param filenameFormat The format pattern for the file name (e.g., date-time string).
 * @param imageCapture The CameraX ImageCapture use case instance.
 * @param outputDirectory The directory where the file will be saved.
 * @param executor The executor to run the callback operations.
 * @param onImageCaptured Callback triggered on success with the saved image URI.
 * @param onError Callback triggered on failure with the exception.
 */
private fun takePhoto(
    filenameFormat: String,
    imageCapture: ImageCapture,
    outputDirectory: File,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        outputDirectory,
        java.text.SimpleDateFormat(filenameFormat, java.util.Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }
        }
    )
}

/**
 * Extension function to retrieve the ProcessCameraProvider asynchronously using Coroutines.
 *
 * @return The initialized ProcessCameraProvider.
 */
private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        continuation.resume(cameraProviderFuture.get())
    }, ContextCompat.getMainExecutor(this))
}