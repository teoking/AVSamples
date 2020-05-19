package com.teoking.avsamples.ui.image.glsurfaceview

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class MyGLSurfaceImageView(context: Context): GLSurfaceView(context) {

    private val renderer: MyGL20Renderer

    constructor(context: Context, attributeSet: AttributeSet) : this(context) {
        // Do nothing
    }

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = MyGL20Renderer(context)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data.
        // To allow the triangle to rotate automatically, this line is commented out:
        renderMode = RENDERMODE_WHEN_DIRTY

        // Set debug flags
        debugFlags = DEBUG_CHECK_GL_ERROR or DEBUG_LOG_GL_CALLS
    }

}