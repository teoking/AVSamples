package com.teoking.avsamples.ui.image.image2texture

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider

import com.teoking.avsamples.R
import kotlinx.android.synthetic.main.fragment_image_sprite2d.*

class DrawImageSprite2dFragment : Fragment() {

    companion object {
        fun newInstance() = DrawImageSprite2dFragment()
    }

    private lateinit var viewModel: DrawImageSprite2dViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_sprite2d, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DrawImageSprite2dViewModel::class.java)

        descTextView.text = viewModel.descText

        viewModel.onActivityCreated(surfaceView.holder)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}
