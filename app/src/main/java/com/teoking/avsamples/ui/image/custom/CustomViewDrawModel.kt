package com.teoking.avsamples.ui.image.custom

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teoking.avsamples.R

class CustomViewDrawModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Draw image with Custom View"
    }
    val text: LiveData<String> = _text

    private val _image = MutableLiveData<@DrawableRes Int>().apply {
        value = R.drawable.james
    }
    val image: LiveData<Int> = _image
}