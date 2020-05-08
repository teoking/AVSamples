package com.teoking.avsamples.imagedrawer.ui.imageview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ImageViewDrawModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Draw image with ImageView"
    }
    val text: LiveData<String> = _text

}