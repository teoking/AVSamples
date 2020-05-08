package com.teoking.avsamples.imagedrawer.ui.surfaceview

import android.graphics.Canvas
import android.view.SurfaceView
import java.lang.IllegalStateException

class RenderThread(view: SurfaceView) : Thread() {
    var myView: SurfaceView = view
    private var running = false

    fun setRunning(run: Boolean) {
        running = run
    }

    override fun run() {
        while (running) {
            try {
                val canvas: Canvas = myView.holder.lockCanvas()
                synchronized(myView.holder) { myView.draw(canvas) }
                myView.holder.unlockCanvasAndPost(canvas)
            } catch (e: IllegalStateException) {
                // Ignore
            }

            // 这里不sleep退出时会崩溃，为什么？
            // java.lang.IllegalStateException: myView.holder.lockCanvas() must not be null
//            try {
//                sleep(30)
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
        }
    }
}