package com.teoking.avsamples.ui.image.surfaceview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import kotlin.system.measureTimeMillis

class ImageSurfaceView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs),
    SurfaceHolder.Callback {

    private lateinit var mBitmap: Bitmap
    private val mRenderThread: RenderThread

    private val mMatrix = Matrix()
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var currentAngle = 0f

    init {
        mRenderThread = RenderThread(this)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setImage(@DrawableRes imageRes: Int) {
        val drawable = context.getDrawable(imageRes)!!
        val width = drawable.intrinsicWidth / 2
        val height = drawable.intrinsicHeight / 2
        mBitmap = drawable.toBitmap(width, height)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (this::mBitmap.isInitialized) {
            currentAngle = ++currentAngle % 360
            mMatrix.setRotate(currentAngle, centerX, centerY)

            canvas.save()

            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(mBitmap, mMatrix, null)

            canvas.restore()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        var retry = true
        mRenderThread.setRunning(false)
        while (retry) {
            try {
                mRenderThread.join()
                retry = false
            } catch (e: InterruptedException) {
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        centerX = (width / 2).toFloat()
        centerY = (height / 2).toFloat()

        mRenderThread.setRunning(true)
        mRenderThread.start()
    }

    fun resume() {
        holder.addCallback(this)
        mRenderThread.setRunning(true)
    }

    fun pause() {
        holder.removeCallback(this)
        mRenderThread.setRunning(false)
    }
}