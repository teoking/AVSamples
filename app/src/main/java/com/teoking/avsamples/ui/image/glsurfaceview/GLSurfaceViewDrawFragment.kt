package com.teoking.avsamples.ui.image.glsurfaceview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.teoking.avsamples.R
import kotlinx.android.synthetic.main.fragment_gl_surface_view_draw.*

class GLSurfaceViewDrawFragment : Fragment() {

    companion object {
        fun newInstance() = GLSurfaceViewDrawFragment()
    }

    private lateinit var viewModel: GLSurfaceViewDrawViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gl_surface_view_draw, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GLSurfaceViewDrawViewModel::class.java)

        descTextView.text = viewModel.descText
    }

}
