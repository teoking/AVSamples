package com.teoking.avsamples.ui.image.surfaceview

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.teoking.avsamples.R

class SurfaceDrawFragment : Fragment() {

    private lateinit var surfaceViewDrawModel: SurfaceViewDrawModel

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        surfaceViewDrawModel =
            ViewModelProvider(this).get(SurfaceViewDrawModel::class.java)
        val root = inflater.inflate(R.layout.fragment_surface_view, container, false)
        val textView: TextView = root.findViewById(R.id.text_surface_view)
        surfaceViewDrawModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        val imageSurfaceView: ImageSurfaceView = root.findViewById(R.id.image_surface_view)
        surfaceViewDrawModel.image.observe(viewLifecycleOwner, Observer {
            imageSurfaceView.setImage(it)
        })
        return root
    }
}
