package com.teoking.avsamples.ui.video.hwdecode

import android.view.Surface

interface HwDecoderSurfaceViewCallback {
    fun onSurfaceCreated(surface: Surface)
}