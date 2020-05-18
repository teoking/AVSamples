package com.teoking.avsamples.ui.image.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap

class MyImageView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val mMatrix = Matrix()
    private var mBitmap: Bitmap? = null
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var currentAngle = 360f

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setImageResource(@DrawableRes res: Int) {
        mBitmap = context.getDrawable(res)!!.run {
            toBitmap(intrinsicWidth / 2, intrinsicHeight / 2)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mBitmap == null) {
            return
        }

        centerX = (width / 2).toFloat()
        centerY = (height / 2).toFloat()
        currentAngle = (--currentAngle).run {
            if (this < 0f) 360f else this
        }
        mMatrix.setRotate(currentAngle, centerX, centerY)


        canvas.save()

        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(mBitmap!!, mMatrix, null)

        canvas.restore()

        invalidate()
    }

}