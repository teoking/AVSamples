package com.teoking.avsamples.ui.graphic.ogles2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider

import com.teoking.avsamples.R
import kotlinx.android.synthetic.main.fragment_gles2_shapes.*

class Gles2ShapesFragment : Fragment() {

    companion object {
        fun newInstance() = Gles2ShapesFragment()
    }

    private lateinit var viewModel: Gles2ShapesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gles2_shapes, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(Gles2ShapesViewModel::class.java)

        descTextView.text = viewModel.descText
    }

}
