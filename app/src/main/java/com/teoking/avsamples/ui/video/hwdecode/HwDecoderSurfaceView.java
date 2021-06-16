package com.teoking.avsamples.ui.video.hwdecode;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.teoking.common.TextureRender;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class HwDecoderSurfaceView extends GLSurfaceView {
    private static final String TAG = "VideoSurfaceView";
    private static final int SLEEP_TIME_MS = 1000;

    private VideoRender mRenderer;

    public HwDecoderSurfaceView(Context context, HwDecoderSurfaceViewCallback callback) {
        super(context);

        setEGLContextClientVersion(2);
        mRenderer = new VideoRender(context, callback);
        setRenderer(mRenderer);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * A GLSurfaceView implementation that wraps TextureRender.  Used to render frames from a
     * video decoder to a View.
     */
    private static class VideoRender
            implements Renderer, SurfaceTexture.OnFrameAvailableListener {
        private static String TAG = "VideoRender";

        private TextureRender mTextureRender;
        private SurfaceTexture mSurfaceTexture;
        private HwDecoderSurfaceViewCallback mCallback;
        private boolean updateSurface = false;

        public VideoRender(Context context, HwDecoderSurfaceViewCallback callback) {
            mTextureRender = new TextureRender();
            mCallback = callback;
        }

        public void onDrawFrame(GL10 glUnused) {
            synchronized (this) {
                if (updateSurface) {
                    mSurfaceTexture.updateTexImage();
                    updateSurface = false;
                }
            }

            mTextureRender.drawFrame(mSurfaceTexture);
        }

        public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        }

        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
            mTextureRender.surfaceCreated();

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the callback
             */
            mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
            mSurfaceTexture.setOnFrameAvailableListener(this);

            Surface surface = new Surface(mSurfaceTexture);
            mCallback.onSurfaceCreated(surface);
            surface.release();

            synchronized (this) {
                updateSurface = false;
            }
        }

        synchronized public void onFrameAvailable(SurfaceTexture surface) {
            updateSurface = true;
        }
    }  // End of class VideoRender.
} // End of class VideoSurfaceView
