package com.teoking.avsamples.ui.webrtc

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.teoking.avsamples.R
import com.teoking.common.PermissionFragment
import kotlinx.android.synthetic.main.fragment_web_rtc_intro.*

class WebRtcIntroFragment : PermissionFragment(
    perms = arrayOf(
        Manifest.permission.CAMERA
    )
) {

    private lateinit var viewModel: WebRtcIntroViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_web_rtc_intro, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(WebRtcIntroViewModel::class.java)

        descTextView.text = viewModel.descText

        viewModel.startRtcClient(surfaceViewRenderer)
    }

}
