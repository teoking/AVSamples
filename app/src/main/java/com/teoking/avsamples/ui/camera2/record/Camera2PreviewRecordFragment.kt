package com.teoking.avsamples.ui.camera2.record

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.teoking.avsamples.R

class Camera2PreviewRecordFragment : Fragment() {

    companion object {
        fun newInstance() = Camera2PreviewRecordFragment()
    }

    private lateinit var viewModel: Camera2PreviewRecordViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera2_preview_record_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(Camera2PreviewRecordViewModel::class.java)
        // TODO: Use the ViewModel
    }

}