package com.teoking.avsamples.ui.video.hwdecode

import android.app.Application
import android.view.Gravity
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.*

class HardwareDecoderViewModel(private val appContext: Application) :
    AndroidViewModel(appContext), LifecycleObserver, HwDecoderSurfaceViewCallback {

    private val _text = MutableLiveData<String>().apply {
        value = "This sample shows decoding a video file with MediaCodec API and " +
                "output frame to the Surface."
    }
    val text: LiveData<String> = _text

    private lateinit var decoder: HwVideoDecoder

    fun initVideoView(container: FrameLayout) {
        addVideoSurfaceViewTo(container)
    }

    override fun onCleared() {
        super.onCleared()
        decoder.stop()
    }

    override fun onSurfaceCreated(surface: Surface) {
        val afd = appContext.assets.openFd(MP4_FILENAME)
        decoder = HwVideoDecoder(afd, surface)
        decoder.start()
    }

    fun playOrPause() {
        decoder.playOrPause()
    }

    private fun addVideoSurfaceViewTo(container: ViewGroup) {
        // Add HardwareDecoderSurfaceView
        val surfaceView = HwDecoderSurfaceView(appContext, this)
        val frame = container as FrameLayout
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
        frame.addView(surfaceView, params)
    }

    companion object {
        const val TAG = "HwDecoderVM"
        const val MP4_FILENAME = "SampleVideo_720x480_1mb.mp4"
    }
}