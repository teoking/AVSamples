package com.teoking.avsamples.ui.camerax.preivew

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider

import com.teoking.avsamples.R
import com.teoking.common.PermissionFragment
import kotlinx.android.synthetic.main.fragment_camera_x_preview.*

class CameraXPreviewFragment : PermissionFragment(
    perms = arrayOf(
        Manifest.permission.CAMERA
    )
) {

    companion object {
        fun newInstance() = CameraXPreviewFragment()
    }

    private lateinit var viewModel: CameraXPreviewViewModel
    private var isFrontLensSelected = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera_x_preview, container, false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // TODO To be supported.
        // A good start to refer for this task:
        // https://github.com/android/camera-samples/blob/master/CameraXBasic/app/src/main/java/com/android/example/cameraxbasic/fragments/CameraFragment.kt
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraXPreviewViewModel::class.java)
        descTextView.text = viewModel.descText

        previewSelectGroup.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.surfaceViewButton -> {
                    viewModel.requestWithSurfaceView(this, preview_view, isFrontLensSelected)
                }
                R.id.textureViewButton -> {
                    viewModel.requestWithTextureView(this, preview_view, isFrontLensSelected)
                }
                else -> {
                    Toast.makeText(context, "Not implemented!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cameraSelectGroup.setOnCheckedChangeListener { _, checkedId ->
            isFrontLensSelected = when(checkedId) {
                R.id.frontCameraButton -> {
                    true
                }
                else -> {
                    false
                }
            }
        }
    }
}
