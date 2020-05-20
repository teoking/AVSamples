package com.teoking.avsamples.ui.image.image2texture

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import androidx.lifecycle.AndroidViewModel
import com.android.grafika.*
import com.teoking.avsamples.R
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

class DrawImageSprite2dViewModel(application: Application) : AndroidViewModel(application),
    SurfaceHolder.Callback, Choreographer.FrameCallback{

    val descText = "This sample shows how to draw an image to SurfaceView with Sprite2d (from `grafika` project)"

    companion object {
        private const val TAG = "MyImageTextureActivity"

        // [ This used to have "a few thoughts about app life cycle and SurfaceView".  These
        //   are now at http://source.android.com/devices/graphics/architecture.html in
        //   Appendix B. ]
        //
        // This Activity uses approach #2 (Surface-driven).

        // [ This used to have "a few thoughts about app life cycle and SurfaceView".  These
        //   are now at http://source.android.com/devices/graphics/architecture.html in
        //   Appendix B. ]
        //
        // This Activity uses approach #2 (Surface-driven).
        // Indexes into the data arrays.
        private const val SURFACE_SIZE_TINY = 0
        private const val SURFACE_SIZE_SMALL = 1
        private const val SURFACE_SIZE_MEDIUM = 2
        private const val SURFACE_SIZE_FULL = 3

        private val SURFACE_DIM = intArrayOf(64, 240, 480, -1)
        private val SURFACE_LABEL = arrayOf(
            "tiny", "small", "medium", "full"
        )

        private val SAMPLE_IMAGE = R.drawable.james
    }

    private var mSelectedSize = 0
    private var mFullViewWidth = 0
    private var mFullViewHeight = 0
    private val mWindowWidthHeight: Array<IntArray> = Array(SURFACE_DIM.size) {IntArray(2) {0} }

    // Rendering code runs on this thread.  The thread's life span is tied to the Surface.
    private lateinit var mRenderThread: RenderThread
    private lateinit var mSurfaceHolder: SurfaceHolder

    fun onActivityCreated(holder: SurfaceHolder) {
        mSelectedSize = SURFACE_SIZE_FULL
        mFullViewWidth = 512
        mFullViewHeight = 512

        mSurfaceHolder = holder
        mSurfaceHolder.addCallback(this)
    }

    fun onResume() {
        // If we already have a Surface, we just need to resume the frame notifications.
        if (::mRenderThread.isInitialized) {
            Log.d(TAG, "onResume re-hooking choreographer")
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun onPause() {
        // If the callback was posted, remove it.  This stops the notifications.  Ideally we
        // would send a message to the thread letting it know, so when it wakes up it can
        // reset its notion of when the previous Choreographer event arrived.
        Log.d(TAG, "onPause unhooking choreographer")
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated holder=$holder")

        // Grab the view's width.  It's not available before now.
        val size = holder.surfaceFrame
        mFullViewWidth = size.width()
        mFullViewHeight = size.height()

        // Configure our fixed-size values.  We want to configure it so that the narrowest
        // dimension (e.g. width when device is in portrait orientation) is equal to the
        // value in SURFACE_DIM, and the other dimension is sized to maintain the same
        // aspect ratio.
        val windowAspect = mFullViewHeight.toFloat() / mFullViewWidth.toFloat()
        for (i in SURFACE_DIM.indices) {
            when {
                i == SURFACE_SIZE_FULL -> {
                    // special-case for full size
                    mWindowWidthHeight[i][0] = mFullViewWidth
                    mWindowWidthHeight[i][1] = mFullViewHeight
                }
                mFullViewWidth < mFullViewHeight -> {
                    // portrait
                    mWindowWidthHeight[i][0] = SURFACE_DIM[i]
                    mWindowWidthHeight[i][1] = (SURFACE_DIM[i] * windowAspect).toInt()
                }
                else -> {
                    // landscape
                    mWindowWidthHeight[i][0] = (SURFACE_DIM[i] / windowAspect).toInt()
                    mWindowWidthHeight[i][1] = SURFACE_DIM[i]
                }
            }
        }

        mRenderThread = RenderThread(getApplication(), mSurfaceHolder)
        mRenderThread.name = "HardwareScaler GL render"
        mRenderThread.start()
        mRenderThread.waitUntilReady()

        val rh: RenderHandler = mRenderThread.getHandler()
        rh.sendSurfaceCreated()

        // start the draw events
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                " holder=" + holder)

        val rh: RenderHandler = mRenderThread.getHandler()
        rh.sendSurfaceChanged(format, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.d(TAG, "surfaceDestroyed holder=$holder")

        // We need to wait for the render thread to shut down before continuing because we
        // don't want the Surface to disappear out from under it mid-render.  The frame
        // notifications will have been stopped back in onPause(), but there might have
        // been one in progress.
        val rh: RenderHandler = mRenderThread.getHandler()
        rh.sendShutdown()
        try {
            mRenderThread.join()
        } catch (ie: InterruptedException) {
            // not expected
            throw java.lang.RuntimeException("join was interrupted", ie)
        }

        Log.d(TAG, "surfaceDestroyed complete")
    }

    override fun doFrame(frameTimeNanos: Long) {
        val rh: RenderHandler = mRenderThread.getHandler()
        Choreographer.getInstance().postFrameCallback(this)
        rh.sendDoFrame(frameTimeNanos)
    }

    /**
     * This class handles all OpenGL rendering.
     * <p>
     * We use Choreographer to coordinate with the device vsync.  We deliver one frame
     * per vsync.  We can't actually know when the frame we render will be drawn, but at
     * least we get a consistent frame interval.
     * <p>
     * Start the render thread after the Surface has been created.
     */
    private class RenderThread(context: Context, holder: SurfaceHolder): Thread() {

        private val mContextWeakRef = WeakReference(context)

        // Object must be created on render thread to get correct Looper, but is used from
        // UI thread, so we need to declare it volatile to ensure the UI thread sees a fully
        // constructed object.
        private lateinit var mHandler: RenderHandler

        // Used to wait for the thread to start.
        private val mStartLock = ReentrantLock()
        private val mCondition = mStartLock.newCondition()
        private var mReady = false

        private val mSurfaceHolder: SurfaceHolder = holder
        private lateinit var mEglCore: EglCore
        private lateinit var mWindowSurface: WindowSurface
        private lateinit var mTexProgram: Texture2dProgram
        private var mPngTexture: Int = 0

        // Orthographic projection matrix.
        private val mDisplayProjectionMatrix = FloatArray(16)

        private val mRectDrawable = Drawable2d(Drawable2d.Prefab.RECTANGLE)

        // One rectangle
        private val mRect = Sprite2d(mRectDrawable)

        // velocity, in viewport units per second
        private var mRectVelX = 0f
        private  var mRectVelY = 0f
        private var mInnerLeft = 0f
        private  var mInnerTop = 0f
        private  var mInnerRight = 0f
        private  var mInnerBottom = 0f

        private val mIdentityMatrix = FloatArray(16).apply {
            Matrix.setIdentityM(this, 0)
        }

        // Previous frame time.
        private var mPrevTimeNanos: Long = 0

        /**
         * Thread entry point.
         * <p>
         * The thread should not be started until the Surface associated with the SurfaceHolder
         * has been created.  That way we don't have to wait for a separate "surface created"
         * message to arrive.
         */
        override fun run() {
            Looper.prepare()
            mHandler = RenderHandler(this)
            mEglCore = EglCore(null, 0)
            // Like synchronized(object)
            mStartLock.withLock {
                mReady = true
                // Like object.notify()
                // Signal waitUntilReady()
                mCondition.signal()
            }

            Looper.loop()

            Log.d(TAG, "looper quit")
            releaseGl()
            mEglCore.release()

            mStartLock.withLock {
                mReady = false
            }
        }

        /**
         * Waits until the render thread is ready to receive messages.
         *
         *
         * Call from the UI thread.
         */
        fun waitUntilReady() {
            mStartLock.withLock {
                while (!mReady) {
                    try {
                        mCondition.await()
                    } catch (ie: InterruptedException) {
                        /* not expected */
                    }
                }
            }
        }

        /**
         * Shuts everything down.
         */
        fun shutdown() {
            Log.d(TAG, "shutdown")
            Looper.myLooper()?.quit()
        }

        /**
         * Returns the render thread's Handler.  This may be called from any thread.
         */
        fun getHandler(): RenderHandler {
            return mHandler
        }

        /**
         * Prepares the surface.
         */
        fun surfaceCreated() {
            val surface = mSurfaceHolder.surface
            prepareGl(surface)
        }

        /**
         * Prepares window surface and GL state.
         */
        private fun prepareGl(surface: Surface) {
            Log.d(TAG, "prepareGl")
            if (mContextWeakRef.get() == null) {
                throw IllegalStateException("Context cannot be null!")
            }

            mWindowSurface = WindowSurface(mEglCore, surface, false)
            mWindowSurface.makeCurrent()

            // Programs used for drawing onto the screen.
            mTexProgram = Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D)
            // FIXME testing now
            mPngTexture = loadTexture(mContextWeakRef.get()!!, SAMPLE_IMAGE)

            // Set the background color.
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

            // Disable depth testing -- we're 2D only.
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)

            // Don't need backface culling.  (If you're feeling pedantic, you can turn it on to
            // make sure we're defining our shapes correctly.)
            GLES20.glDisable(GLES20.GL_CULL_FACE)
        }

        /**
         * Handles changes to the size of the underlying surface.  Adjusts viewport as needed.
         * Must be called before we start drawing.
         * (Called from RenderHandler.)
         */
        fun surfaceChanged(width: Int, height: Int) {
            // This method is called when the surface is first created, and shortly after the
            // call to setFixedSize().  The tricky part is that this is called when the
            // drawing surface is *about* to change size, not when it has *already* changed
            // size.  A query on the EGL surface will confirm that the surface dimensions
            // haven't yet changed.  If you re-query after the next swapBuffers() call,
            // you will see the new dimensions.
            //
            // To have a smooth transition, we should continue to draw at the old size until the
            // surface query tells us that the size of the underlying buffers has actually
            // changed.  I don't really expect a "normal" app will want to call setFixedSize()
            // dynamically though, so in practice this situation shouldn't arise, and it's
            // just not worth the hassle of doing it right.
            Log.d(TAG, "surfaceChanged " + width + "x" + height)

            // Use full window.

            // Use full window.
            GLES20.glViewport(0, 0, width, height)

            // Simple orthographic projection, with (0,0) in lower-left corner.
            Matrix.orthoM(mDisplayProjectionMatrix, 0, 0f, width.toFloat(), 0f, height.toFloat(), -1f, 1f)

            val smallDim = min(width, height)

            // Set initial shape size / position / velocity based on window size.  Movement
            // has the same "feel" on all devices, but the actual path will vary depending
            // on the screen proportions.  We do it here, rather than defining fixed values
            // and tweaking the projection matrix, so that our squares are square.
            mRect.setColor(0.9f, 0.1f, 0.1f)
            mRect.setTexture(mPngTexture)
//            mRect.setScale(smallDim / 5.0f, smallDim / 5.0f)
//            mRect.setPosition(width / 2.0f, height / 2.0f)
            mRect.setScale(smallDim / 1.0f, smallDim / 1.0f)
            mRect.setPosition(width / 2.0f, height / 2.0f)

            Log.d(TAG, "mRect: $mRect")
        }

        /**
         * Releases most of the GL resources we currently hold.
         * <p>
         * Does not release EglCore.
         */
        private fun releaseGl() {
            GlUtil.checkGlError("releaseGl start")

            mWindowSurface.release()
            mTexProgram.release()
            GlUtil.checkGlError("releaseGl done")

            mEglCore.makeNothingCurrent()
        }

        /**
         * Handles the frame update.  Runs when Choreographer signals.
         */
        fun doFrame(timeStampNanos: Long) {
            // Log.d(TAG, "doFrame $timeStampNanos")

            // If we're not keeping up 60fps -- maybe something in the system is busy, maybe
            // recording is too expensive, maybe the CPU frequency governor thinks we're
            // not doing and wants to drop the clock frequencies -- we need to drop frames
            // to catch up.  The "timeStampNanos" value is based on the system monotonic
            // clock, as is System.nanoTime(), so we can compare the values directly.
            //
            // Our clumsy collision detection isn't sophisticated enough to deal with large
            // time gaps, but it's nearly cost-free, so we go ahead and do the computation
            // either way.
            //
            // We can reduce the overhead of recording, as well as the size of the movie,
            // by recording at ~30fps instead of the display refresh rate.  As a quick hack
            // we just record every-other frame, using a "recorded previous" flag.
            update(timeStampNanos)
            val diff = (System.nanoTime() - timeStampNanos) / 1000000
            if (diff > 15) {
                // too much, drop a frame
                Log.d(TAG, "diff is $diff, skipping render")
                return
            }
            draw()
            mWindowSurface.swapBuffers()
        }

        /**
         * Advances animation state.
         *
         * We use the time delta from the previous event to determine how far everything
         * moves.  Ideally this will yield identical animation sequences regardless of
         * the device's actual refresh rate.
         */
        private fun update(timeStampNanos: Long) {
            // Not implement now.
        }

        /**
         * Draws the scene.
         */
        private fun draw() {
            GlUtil.checkGlError("draw start")

            // Clear to a non-black color to make the content easily differentiable from
            // the pillar-/letter-boxing.
            GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            // Textures may include alpha, so turn blending on.
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            mRect.draw(mTexProgram, mDisplayProjectionMatrix)
            GLES20.glDisable(GLES20.GL_BLEND)
            GlUtil.checkGlError("draw done")
        }
    }

    /**
     * Handler for RenderThread.  Used for messages sent from the UI thread to the render thread.
     *
     *
     * The object is created on the render thread, and the various "send" methods are called
     * from the UI thread.
     */
    private class RenderHandler(rt: RenderThread) : Handler() {
        // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
        // but no real harm in it.
        private val mWeakRenderThread: WeakReference<RenderThread> = WeakReference(rt)

        /**
         * Sends the "surface created" message.
         *
         *
         * Call from UI thread.
         */
        fun sendSurfaceCreated() {
            sendMessage(obtainMessage(MSG_SURFACE_CREATED))
        }

        /**
         * Sends the "surface changed" message, forwarding what we got from the SurfaceHolder.
         *
         *
         * Call from UI thread.
         */
        fun sendSurfaceChanged(format: Int, width: Int,
                               height: Int) {
            // ignore format
            sendMessage(obtainMessage(MSG_SURFACE_CHANGED, width, height))
        }

        /**
         * Sends the "do frame" message, forwarding the Choreographer event.
         *
         *
         * Call from UI thread.
         */
        fun sendDoFrame(frameTimeNanos: Long) {
            sendMessage(obtainMessage(MSG_DO_FRAME,
                (frameTimeNanos shr 32).toInt(), frameTimeNanos.toInt()))
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         *
         *
         * Call from UI thread.
         */
        fun sendShutdown() {
            sendMessage(obtainMessage(MSG_SHUTDOWN))
        }

        // runs on RenderThread
        override fun handleMessage(msg: Message) {
            val what = msg.what
            //Log.d(TAG, "RenderHandler [" + this + "]: what=" + what);
            val renderThread = mWeakRenderThread.get()
            if (renderThread == null) {
                Log.w(TAG, "RenderHandler.handleMessage: weak ref is null")
                return
            }
            when (what) {
                MSG_SURFACE_CREATED -> renderThread.surfaceCreated()
                MSG_SURFACE_CHANGED -> renderThread.surfaceChanged(msg.arg1, msg.arg2)
                MSG_DO_FRAME -> {
                    val timestamp = msg.arg1.toLong() shl 32 or
                            (msg.arg2.toLong() and 0xffffffffL)
                    renderThread.doFrame(timestamp)
                }
                MSG_SHUTDOWN -> renderThread.shutdown()
                else -> throw java.lang.RuntimeException("unknown message $what")
            }
        }

        companion object {
            private const val MSG_SURFACE_CREATED = 0
            private const val MSG_SURFACE_CHANGED = 1
            private const val MSG_DO_FRAME = 2
            private const val MSG_FLAT_SHADING = 3
            private const val MSG_SHUTDOWN = 5
        }

    }
}

fun loadTexture(context: Context, resourceId: Int): Int {
    val textureHandle = IntArray(1)
    GLES20.glGenTextures(1, textureHandle, 0)
    if (textureHandle[0] != 0) {
        val options = BitmapFactory.Options()
        options.inScaled = false // No pre-scaling

        // Read in the resource
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle()
    }
    if (textureHandle[0] == 0) {
        throw java.lang.RuntimeException("Error loading texture.")
    }
    return textureHandle[0]
}
