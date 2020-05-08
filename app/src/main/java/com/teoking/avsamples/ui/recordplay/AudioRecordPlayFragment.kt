package com.teoking.avsamples.ui.recordplay

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.teoking.avsamples.R
import com.teoking.common.PermissionFragment
import kotlinx.android.synthetic.main.fragment_audio_record_play.*

class AudioRecordPlayFragment : PermissionFragment(
    perms = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )
) {

    companion object {
        fun newInstance() = AudioRecordPlayFragment()
    }

    private lateinit var viewModel: AudioRecordPlayViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_audio_record_play, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AudioRecordPlayViewModel::class.java)

        descTextView.text = viewModel.descText

        viewModel.stateText.observe(viewLifecycleOwner, Observer {
            statusTextView.text = it
        })

        recordStartButton.setOnClickListener {
            viewModel.recordPcmStart()
        }

        recordStopButton.setOnClickListener {
            viewModel.recordPcmStop()
        }

        audioPlayButton.setOnClickListener {
            viewModel.playPcm()
        }
    }

}
