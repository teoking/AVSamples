package com.teoking.avsamples.ui.video.texturerendering

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.teoking.avsamples.R
import kotlinx.android.synthetic.main.video_texture_renderer_fragment.*

class VideoTextureRenderFragment : Fragment() {

    companion object {
        fun newInstance() = VideoTextureRenderFragment()
    }

    private lateinit var viewModel: VideoTextureRenderViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.video_texture_renderer_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(VideoTextureRenderViewModel::class.java)
        viewModel.text.observe(viewLifecycleOwner, Observer {
            infoTextView.text = it
        })

        viewModel.initVideoView(container)
        lifecycle.addObserver(viewModel)

        playButton.setOnClickListener {
            viewModel.delayedPlay()
        }
    }
}