package com.teoking.avsamples.ui.codec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.teoking.avsamples.R
import kotlinx.android.synthetic.main.fragment_media_code_sample.*

class AudioCodecFragment : Fragment() {

    companion object {
        fun newInstance() = AudioCodecFragment()
    }

    private lateinit var viewModel: AudioCodecViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media_code_sample, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AudioCodecViewModel::class.java)

        descTextView.text = viewModel.descText

        viewModel.recordingStateText.observe(viewLifecycleOwner, Observer {
            recordAndEncodeButton.text = it
        })

        viewModel.stateText.observe(viewLifecycleOwner, Observer {
            stateTextView.text = it
        })

        recordAndEncodeButton.setOnClickListener {
            viewModel.recordAndEncodeAudioToAac()
        }

        playEncodedButton.setOnClickListener {
            viewModel.playAacWithMediaPlayer()
        }

        decodedAndPlayButton.setOnClickListener {
            viewModel.decodeAacAndPlayWithAudioTrack()
        }
    }

}
