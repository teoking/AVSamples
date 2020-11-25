package com.teoking.avsamples.ui.extractor_muxer

import android.app.Application
import android.media.MediaCodec
import android.media.MediaFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.teoking.avsamples.logic.extractor_muxer.MediaExtractorProxy
import com.teoking.avsamples.logic.extractor_muxer.MediaMuxerProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class Mp4ExtractorMuxerViewModel(application: Application) : AndroidViewModel(application) {

    private val _text = MutableLiveData<String>().apply {
        value = "This sample shows usage of MediaExtractor & MediaMuxer with a mp4 file:\n" +
                "* Using MediaExtractor to retrieve media data from a mp4 file\n" +
                "* Using MediaMuxer to muxing a new mp4 file from media data read from MediaExtractor"
    }
    val text: LiveData<String> = _text

    private val _resultText = MutableLiveData<String>().apply {
        value = ""
    }
    val resultText: LiveData<String> = _resultText

    fun showMediaInfo() {
        viewModelScope.launch {
            val proxy = createMediaExtractorProxy()
            _resultText.value = proxy.extractMp4MediaInfo()
            proxy.release()
        }
    }

    private suspend fun createMediaExtractorProxy(): MediaExtractorProxy {
        return withContext(Dispatchers.IO) {
            MediaExtractorProxy(
                getApplication<Application>().assets.openFd(
                    MP4_FILENAME2
                )
            )
        }
    }

    fun doMuxingSampleMp4() {
        viewModelScope.launch {
            _resultText.value = "Starting to muxing a sample mp4 file..."

            muxingTracks()

            _resultText.value = "Muxing finished. File path is $outputFilePath"
        }
    }

    private suspend fun muxingTracks() {
        withContext(Dispatchers.IO) {
            val muxer = createMediaMuxerProxy()
            val extractor = createMediaExtractorProxy()

            var audioTrackIndex = -1
            var videoTrackIndex = -1
            for (i in 0 until extractor.getTrackCount()) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video")) {
                    videoTrackIndex = muxer.addTrack(format)
                } else if (mime.startsWith("audio")) {
                    audioTrackIndex = muxer.addTrack(format)
                }
            }

            val byteBuffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            muxer.start()

            for (i in 0 until extractor.getTrackCount()) {
                var finished = false
                while(!finished) {
                    // getInputBuffer() will fill the inputBuffer with one frame of encoded
                    // sample from either MediaCodec or MediaExtractor, set isAudioSample to
                    // true when the sample is audio data, set up all the fields of bufferInfo,
                    // and return true if there are no more samples.
                    val result = extractor.getInputBuffer(i, byteBuffer, bufferInfo)
                    finished = result.first == -1
                    if (!finished) {
                        val currentTrackIndex = if (result.second) audioTrackIndex else videoTrackIndex
                        muxer.writeSampleData(currentTrackIndex, byteBuffer, bufferInfo)
                        byteBuffer.rewind()
                    }
                }
            }

            muxer.stop()
            muxer.release()
        }
    }

    private suspend fun createMediaMuxerProxy(): MediaMuxerProxy = withContext(Dispatchers.Default) {
        MediaMuxerProxy(outputFilePath)
    }

    override fun onCleared() {
    }

    private fun getCachePath(): String {
        return getApplication<Application>().run {
            this.externalCacheDir?.absolutePath ?: this.cacheDir.absolutePath
        }
    }

    private val outputFilePath = getCachePath() + File.separator + "temp.mp4"

    companion object {
        const val MP4_FILENAME = "file_example_MP4_480_1_5MG.mp4"
        const val MP4_FILENAME2 = "SampleVideo_720x480_1mb.mp4"
    }

}