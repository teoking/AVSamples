package com.teoking.avsamples.ui.image.gl30surfaceview

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_gl_surface_view_draw.*

class GL30Tex2DFragment : Fragment() {

    private lateinit var mGLSurfaceView: GLSurfaceView

    companion object {
        val CONTEXT_CLIENT_VERSION = 3
        fun newInstance() = GL30Tex2DFragment()
    }

    private lateinit var viewModel: GL30Tex2DViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mGLSurfaceView = GLSurfaceView(context)

        if (detectOpenGLES30()) {
            mGLSurfaceView.setEGLContextClientVersion(CONTEXT_CLIENT_VERSION)
            mGLSurfaceView.setRenderer(MyGL30Renderer(requireContext()))
        }

        return mGLSurfaceView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GL30Tex2DViewModel::class.java)
    }

    private fun detectOpenGLES30(): Boolean {
        val am = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = am.deviceConfigurationInfo
        return info.reqGlEsVersion >= 0x30000
    }

    override fun onResume() {
        super.onResume()
        mGLSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mGLSurfaceView.onPause()
    }
}
