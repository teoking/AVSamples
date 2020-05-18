package com.teoking.avsamples.ui.image.imageview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.teoking.avsamples.R

class ImageViewDrawFragment : Fragment() {

    private lateinit var imageViewDrawModel: ImageViewDrawModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        imageViewDrawModel =
            ViewModelProvider(this).get(ImageViewDrawModel::class.java)
        val root = inflater.inflate(R.layout.fragment_image_view, container, false)
        val textView: TextView = root.findViewById(R.id.text_image_view)
        imageViewDrawModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        return root
    }
}
