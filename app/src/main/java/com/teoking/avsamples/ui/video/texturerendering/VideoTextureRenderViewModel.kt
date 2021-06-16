package com.teoking.avsamples.ui.video.texturerendering

import android.app.Application
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.*
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Exception

class VideoTextureRenderViewModel(private val appContext: Application) :
    AndroidViewModel(appContext), LifecycleObserver {

    private val _text = MutableLiveData<String>().apply {
        value = "[VideoTextureRender] shows play a mp4 with MediaPlayer and render pictures as textures to " +
                "the GLSurfaceView through a custom renderer."
    }
    val text: LiveData<String> = _text

    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private lateinit var videoSurfaceView: VideoSurfaceView

    fun initVideoView(container: ViewGroup) {
        var afd: AssetFileDescriptor? = null
        try {
            afd = appContext.assets.openFd(MP4_FILENAME)
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        } finally {
            if (afd != null) {
                try {
                    afd.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message, e)
                }
            }
        }

        addVideoSurfaceViewTo(container)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onLifeCycleResume() {
        videoSurfaceView.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onLifeCyclePause() {
        videoSurfaceView.onPause()
    }

    override fun onCleared() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
        super.onCleared()
    }

    fun delayedPlay() {
        viewModelScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
            }
        }
    }

    private fun addVideoSurfaceViewTo(container: ViewGroup) {
        // Add VideoSurfaceView
        videoSurfaceView = VideoSurfaceView(appContext, mediaPlayer)
        val frame = container as FrameLayout
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
        frame.addView(videoSurfaceView, params)
    }

    companion object {
        const val TAG = "VTRenderVM"
        const val MP4_FILENAME = "SampleVideo_720x480_1mb.mp4"
    }
}