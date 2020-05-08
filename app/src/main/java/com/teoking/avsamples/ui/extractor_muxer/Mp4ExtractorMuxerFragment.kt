package com.teoking.avsamples.ui.extractor_muxer

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.teoking.avsamples.R
import com.teoking.common.PermissionFragment
import kotlinx.android.synthetic.main.fragment_extractor_muxer.*

class Mp4ExtractorMuxerFragment : PermissionFragment(perms = arrayOf(
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)) {
    private lateinit var extractorMuxerViewModel: Mp4ExtractorMuxerViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        extractorMuxerViewModel =
            ViewModelProvider(this).get(Mp4ExtractorMuxerViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_extractor_muxer, container, false)
        val textView: TextView = root.findViewById(R.id.sample_info_text)
        extractorMuxerViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        val resultTextView: TextView = root.findViewById(R.id.resultTextView)
        extractorMuxerViewModel.resultText.observe(viewLifecycleOwner, Observer {
            resultTextView.text = it
        })

        val extractMp4Button: Button = root.findViewById(R.id.extractMp4Button)
        extractMp4Button.setOnClickListener {
            extractorMuxerViewModel.showMediaInfo()
        }

        val muxerMp4Button: Button = root.findViewById(R.id.muxerMp4Button)
        muxerMp4Button.setOnClickListener {
            extractorMuxerViewModel.doMuxingSampleMp4()
        }

        return root
    }
}
