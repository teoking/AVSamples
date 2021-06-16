package com.teoking.avsamples.ui.camera2.record

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import com.teoking.common.AutoFitGlSurfaceView
import com.teoking.common.TextureRender
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Camera2PreviewView(
    context: Context,
    attrs: AttributeSet? = null
) : AutoFitGlSurfaceView(context, attrs) {

    private val mRenderer: Camera2PreviewRender
    private var mPreviewListener: Camera2PreviewViewListener? = null

    init {
        setEGLContextClientVersion(2)
        mRenderer = Camera2PreviewRender(context)
        setRenderer(mRenderer)
    }

    fun setSurfaceListener(listener: Camera2PreviewViewListener) {
        mPreviewListener = listener
    }

    fun onDestroy() {
        mRenderer.onDestroy()
    }

    inner class Camera2PreviewRender(context: Context) : Renderer,
        SurfaceTexture.OnFrameAvailableListener {

        private val mTextureRender: TextureRender = TextureRender()
        private lateinit var mSurfaceTexture: SurfaceTexture
        private lateinit var mSurface: Surface
        private var updateSurface = false

        // 当surface被创建或重建时调用
        // 也就是说在渲染线程启动和EGL context丢失时调用。由于这个方法会在渲染一开始调用，所以这里很适合
        // 用来创建渲染需要的资源，比如创建纹理。另外要注意的是，EGL context丢失时，与之相关的资源都会被
        // 自动释放, 所以不需要手动调用glDelete（如glDeleteTextures）这类方法来释放资源。
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            mTextureRender.surfaceCreated()

            /*
             * Create the SurfaceTexture that will feed this textureID
             */
            mSurfaceTexture = SurfaceTexture(mTextureRender.textureId)
            mSurfaceTexture.setOnFrameAvailableListener(this)

            mSurface = Surface(mSurfaceTexture)
            post {
                mPreviewListener?.onSurfaceCreated(mSurface)
            }
        }

        // 当OpenGL ES surface刚创建或者大小有变化时被调用
        // 通常需要在这个方法里设置viewport
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            mPreviewListener?.onSurfaceChanged(gl, width, height)
        }

        // 在当前帧需要绘制时被调用
        // 通常需要在这个方法里各种绘制
        override fun onDrawFrame(gl: GL10?) {
            synchronized(this) {
                if (updateSurface) {
                    mSurfaceTexture.updateTexImage()
                    updateSurface = false
                }
            }
            mTextureRender.drawFrame(mSurfaceTexture)
        }

        // 当一个流式帧可用时，该回调方法被调用
        override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
            synchronized(this) {
                updateSurface = true
            }
        }

        fun onDestroy() {
            mSurface.release()
        }
    }
}