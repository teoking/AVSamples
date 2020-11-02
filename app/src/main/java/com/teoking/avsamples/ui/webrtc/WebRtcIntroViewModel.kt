package com.teoking.avsamples.ui.webrtc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.teoking.avsamples.logic.webrtc.RTCClient
import org.webrtc.SurfaceViewRenderer


class WebRtcIntroViewModel(application: Application) : AndroidViewModel(application) {

    val descText = "This demo shows how to use WebRTC api for camera preview."

    private lateinit var rtcClient: RTCClient

    fun startRtcClient(renderer: SurfaceViewRenderer) {
        rtcClient = RTCClient(getApplication(), renderer)
        rtcClient.startLocalVideoCapture()
    }

    override fun onCleared() {
        rtcClient.shutdown()
    }
}
