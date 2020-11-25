package com.teoking.avsamples.ui.camerax.preivew

import android.app.Application
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraXPreviewViewModel(application: Application) : AndroidViewModel(application) {

    val descText = "This sample shows how to using CameraX API to implement Camera Preview/Capture"

    private lateinit var _cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var _cameraProvider: ProcessCameraProvider

    /** Blocking camera operations are performed using this executor */
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Request previewing with a SurfaceView */
    fun requestWithSurfaceView(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        isFrontLens: Boolean
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            val cameraProvider = fetchCameraProvider()
            unbindUseCase(cameraProvider)
            bindCameraUseCases(
                lifecycleOwner,
                previewView,
                PreviewView.ImplementationMode.SURFACE_VIEW,
                cameraProvider,
                isFrontLens
            )
        }
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        implementationMode: PreviewView.ImplementationMode,
        cameraProvider: ProcessCameraProvider,
        isFrontLens: Boolean
    ) {
        previewView.preferredImplementationMode = implementationMode

        val preview: Preview = Preview.Builder()
            .setTargetResolution(RESOLUTION_SIZE)
            .build()

        // ImageAnalysis
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(RESOLUTION_SIZE)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                    // Values returned from our analyzer are passed to the attached listener
                    // We log image analysis results here - you should do something useful
                    // instead!
                    Log.d(TAG, "Average luminosity: $luma")
                })
            }

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(if (isFrontLens) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
            .build()

        val camera =
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)

        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.cameraInfo))
    }

    private suspend fun fetchCameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
        _cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())

        // Check for CameraProvider availability
        _cameraProviderFuture.addListener(Runnable {
            _cameraProvider = _cameraProviderFuture.get()
            cont.resume(_cameraProvider)
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    private fun unbindUseCase(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
    }

    /** Request previewing with a TextureView */
    fun requestWithTextureView(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        isFrontLens: Boolean
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            val cameraProvider = fetchCameraProvider()
            unbindUseCase(cameraProvider)
            bindCameraUseCases(
                lifecycleOwner,
                previewView,
                PreviewView.ImplementationMode.TEXTURE_VIEW,
                cameraProvider,
                isFrontLens
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    companion object {
        private val RESOLUTION_SIZE = Size(640, 480)
        private val TAG = "CameraXPreviewViewModel"
    }
}
