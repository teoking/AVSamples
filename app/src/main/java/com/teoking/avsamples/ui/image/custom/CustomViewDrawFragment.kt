package com.teoking.avsamples.ui.image.custom

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

class CustomViewDrawFragment : Fragment() {

    private lateinit var customViewDrawModel: CustomViewDrawModel

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        customViewDrawModel =
            ViewModelProvider(this).get(CustomViewDrawModel::class.java)
        val root = inflater.inflate(R.layout.fragment_custom_view, container, false)
        val textView: TextView = root.findViewById(R.id.text_custom)
        customViewDrawModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        val myImageView: MyImageView = root.findViewById(R.id.my_image_view)
        customViewDrawModel.image.observe(viewLifecycleOwner, Observer {
            myImageView.setImageResource(it)
        })
        return root
    }
}
