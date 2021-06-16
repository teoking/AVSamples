package com.teoking.avsamples.ui.camera2.record

import android.view.Surface
import javax.microedition.khronos.opengles.GL10

interface Camera2PreviewViewListener {
    fun onSurfaceCreated(surface: Surface)
    fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    fun onSurfaceDestroy()
}