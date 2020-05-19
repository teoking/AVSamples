package com.teoking.avsamples.ui.image.glsurfaceview

import android.opengl.GLES20
import android.util.Log

private const val TAG = "MyGLSurfaceView"
private const val DEBUG = true

fun checkGlError(glOperation: String) {
    if (!DEBUG) {
        return
    }

    var error: Int
    while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
        Log.e(TAG, "$glOperation: glError $error")
        throw RuntimeException("$glOperation: glError $error")
    }
}