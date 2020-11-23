package com.teoking.avsamples.ui.image.surfaceview

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.teoking.avsamples.R
import kotlinx.android.synthetic.main.fragment_surface_view.*

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

        surfaceViewDrawModel.text.observe(viewLifecycleOwner, Observer {
            text_surface_view.text = it
        })
        surfaceViewDrawModel.image.observe(viewLifecycleOwner, Observer {
            image_surface_view.setImage(it)
        })
        return root
    }

    override fun onResume() {
        super.onResume()
        image_surface_view.resume()
    }

    override fun onPause() {
        super.onPause()
        image_surface_view.pause()
    }
}
